# RAG 检索增强生成 — 设计方案

> InfoPilot 文档问答的核心能力，涵盖文档摄入、向量检索、上下文增强、LLM 生成的全链路设计。

---

## 1. 问题定义

LLM 的权重是"冻结的知识快照"。企业文档（内部手册、技术规范、会议纪要）不在训练数据中，LLM 凭"猜测"回答会导致幻觉。

RAG 解决三件事：

| 问题 | RAG 解法 |
|---|---|
| **知识盲区** — 模型没见过企业文档 | 检索相关片段 → 注入 Prompt → LLM 基于原文回答 |
| **时效性** — 文档更新了但模型不知道 | 重新向量化新文档，旧向量删除 |
| **可追溯** — "这个结论从哪来的" | 检索结果附带来源引用 |

---

## 2. 业界调研

### 2.1 RAG 流水线共识

2025-2026 业界标准 RAG 管道：

```
文档加载 → 清洗 → 切分(Chunk) → 向量化(Embed) → 存储(Store)
                                                      ↓
用户提问 → 向量化 → 相似度检索 → 重排(Rerank) → 注入 Prompt → LLM 生成
```

来源：[Redis — 10 Techniques to Improve RAG Accuracy](https://redis.io/blog/10-techniques-to-improve-rag-accuracy/)、[Anthropic — Contextual Retrieval](https://www.anthropic.com/news/contextual-retrieval)

### 2.2 切分策略

| 策略 | 做法 | 适合场景 |
|---|---|---|
| **固定大小** | 按 token 数等量切 | 原型验证 |
| **递归切分**（推荐起始） | 段落→句子→词，逐级回退 | 通用文档 |
| **语义切分** | 计算句子相似度，断在语义边界 | 技术文档 |
| **结构感知** | 按标题/章节切 | 手册、规范 |
| **命题切分**（高端） | LLM 把文档打散为原子事实再重组 | 法律/医疗 |

**InfoPilot 选择：递归切分起手。** 这是所有来源一致推荐的最佳起始基线——够好、简单、不需要额外 LLM 调用。后续根据 Eval 指标决定是否升级。

来源：[Milvus — Max-Min Semantic Chunking](https://milvus.io/blog/embedding-first-chunking-second-smarter-rag-retrieval-with-max-min-semantic-chunking)、[LangChain4j RAG Tutorials](https://docs.langchain4j.dev/tutorials/rag)

### 2.3 混合检索（向量 + BM25）

| 检索方式 | 擅长 | 弱点 |
|---|---|---|
| **向量检索**（语义） | "如何优化数据库性能" | 搜不到确切的产品编号 |
| **BM25**（关键词） | "ERR-5001"、"V2.3.4" | 搜不到同义表达 |

工程共识：**向量 + BM25 混合**是最佳实践，但不是 MVP 必须——先做纯向量，用 Eval 数据判断是否需要 BM25。

来源：[Blended RAG](https://arxiv.org/abs/2309.09966)

### 2.4 重排（Rerank）

检索返回的 top-N 结果是按向量相似度排序的，不是按"对回答问题的有用程度"排序的。Cross-encoder 重排器对每个 chunk 打分，精度提升 10-40%，但增加约 9 倍延迟。

**InfoPilot：MVP 不做重排。** 在检索质量不足时再引入。

### 2.5 LangChain4j 的 RAG 支持

```
EmbeddingStoreIngestor      — 一行代码搞定 切分+向量化+存储
EmbeddingStoreContentRetriever — 语义检索，支持 minScore/maxResults/动态过滤
DefaultRetrievalAugmentor   — 可选的重排+查询转换+内容注入管道
@ContentRetriever           — 通过 @AiService 注解绑定检索器
```

来源：[LangChain4j — RAG Tutorial](https://docs.langchain4j.dev/tutorials/rag)

---

## 3. 方案设计

### 3.1 整体架构

```
┌─────────────────────────────────────────────────────────┐
│                    DocController                         │
│  POST /api/doc/upload    ← 上传文档                      │
│  GET  /api/doc/search    ← RAG 问答                      │
└──────────┬──────────────────────────────┬───────────────┘
           │                              │
┌──────────┴──────────┐    ┌──────────────┴───────────────┐
│   DocumentService    │    │     RetrievalService          │
│                      │    │                              │
│ 1. Tika 解析文档      │    │ 1. embedding(query)          │
│ 2. Recursive 切分    │    │ 2. store.search(embedding)   │
│ 3. embedding 向量化   │    │ 3. 组装 context + 来源引用    │
│ 4. store.addAll()    │    │ 4. ChatModel 生成最终回答     │
└──────────┬──────────┘    └──────────────┬───────────────┘
           │                              │
┌──────────┴──────────────────────────────┴───────────────┐
│                  LangChain4j 集成层                       │
│  EmbeddingModel  ·  EmbeddingStore  ·  ChatModel         │
└────────────────────────┬────────────────────────────────┘
                         │
          ┌──────────────┼──────────────┐
          │              │              │
   ┌──────┴──────┐ ┌─────┴─────┐ ┌─────┴─────┐
   │ 智谱 emb-3  │ │ PGVector  │ │ DeepSeek  │
   │ (向量化)    │ │ (存储)    │ │ (生成)    │
   └─────────────┘ └───────────┘ └───────────┘
```

### 3.2 文档摄入流程

```
POST /api/doc/upload  (multipart file)
    │
    ▼
DocumentService.ingest(inputStream, fileName)
    │
    ├─→ TikaDocumentParser.parse(inputStream)
    │       → Document(text, metadata{fileName})
    │
    ├─→ DocumentSplitters.recursive(500, 50)
    │       → List<TextSegment>  (每个 ~500 字符, 重叠 50)
    │       元数据: fileName, chunkIndex, totalChunks
    │
    ├─→ EmbeddingModel.embedAll(segments)
    │       → List<Embedding>
    │
    └─→ EmbeddingStore.addAll(embeddings, segments)
            → PGVector 存储（自动建表 + IVFFlat 索引）
```

**切分参数选择：**
- `maxSegmentSize=500` — 字符数。中文文档 ~250 token/500 字，在 128-512 的推荐范围内
- `overlap=50` — 10% 重叠。防止关键句子正好卡在边界上
- 后续根据 Eval（阶段 6）调整

### 3.3 检索流程

```
GET /api/doc/search?q=如何配置数据库连接
    │
    ▼
RetrievalService.search(query)
    │
    ├─→ EmbeddingModel.embed(query)
    │       → queryVector
    │
    ├─→ EmbeddingStore.search(
    │       EmbeddingSearchRequest.builder()
    │           .queryEmbedding(queryVector)
    │           .maxResults(3)       ← 取 top 3
    │           .minScore(0.6)       ← 相似度阈值
    │           .build()
    │   )
    │       → List<EmbeddingMatch<TextSegment>>
    │
    ├─→ 组装增强 Prompt:
    │   """
    │   基于以下文档片段回答用户问题。
    │   如果文档中没有相关信息，请直接说"未找到相关信息"。
    │
    │   ## 参考文档
    │   [来源: fileName, 片段 1/5]
    │   chunk1.text
    │
    │   [来源: fileName, 片段 3/5]
    │   chunk3.text
    │
    │   ## 用户问题
    │   {query}
    │   """
    │
    └─→ ChatModel.chat(augmentedPrompt)
            → 返回回答 + 来源引用
```

### 3.4 数据模型（PGVector）

```sql
-- 由 PgVectorEmbeddingStore 自动创建
CREATE TABLE IF NOT EXISTS embeddings (
    embedding_id UUID PRIMARY KEY,
    embedding    vector(1024),   -- 智谱 embedding-3 维度
    text         TEXT,            -- 片段原文
    metadata     JSONB            -- {fileName, chunkIndex, totalChunks}
);

-- IVFFlat 索引（自动创建）
CREATE INDEX ON embeddings USING ivfflat (embedding vector_cosine_ops);
```

### 3.5 元数据设计

每个 chunk 携带的元数据：

```json
{
    "fileName": "数据库配置手册.pdf",
    "chunkIndex": "3",
    "totalChunks": "15",
    "ingestedAt": "2026-05-27T23:00:00Z"
}
```

用途：
- **来源引用**：回答中标注"根据《数据库配置手册》第 3/15 片段"
- **过滤**：按文档删除/更新（`metadataKey("fileName").isEqualTo("xxx")`）
- **多租户**：阶段 6 添加 `tenantId` 字段

---

## 4. 混合检索（规划中，V2）

当前 V1 仅做纯向量检索。V2 引入 BM25 关键词检索：

```
Query → EmbeddingModel.embed() → PGVector ANN search  → top 10
Query → tsvector @@ to_tsquery  → PG 全文搜索          → top 10
                              ↓
                    RRF 融合排序 → top 5 → LLM
```

PostgreSQL 原生支持 `tsvector` 中文分词（需装 `zhparser` 扩展），Java 端无需额外组件。这是学习路线中提到的"轻量方案"。

V1 先用纯向量跑通流程，检索精度不够时再升级。

---

## 5. Eval 策略（阶段 6 系统化）

在没有 Eval 的情况下调 RAG 参数（chunk 大小、minScore、topN）等于盲飞。至少需要两个指标：

| 指标 | 含义 | 目标 |
|---|---|---|
| **Faithfulness**（忠实度） | 回答是否基于检索到的文档，有没有编造 | > 0.8 |
| **Contextual Recall**（召回率） | 相关文档片段是否被检索到了 | > 0.7 |

工具选 [DeepEval](https://github.com/confident-ai/deepeval)，Pytest 风格 API。阶段 6 集成 CI，每次 Prompt 或参数变更自动跑。

---

## 6. MVP 实现清单

| 步骤 | 内容 | 依赖 |
|---|---|---|
| 1 | pom.xml 添加 Tika 依赖 | ✅ 已加 |
| 2 | `DocumentService` — 解析 + 切分 + 向量化 + 存储 | 无 |
| 3 | `RetrievalService` — query embedding + 检索 + Prompt 组装 | 2 |
| 4 | `DocController` — 上传 + 搜索接口 | 2, 3 |
| 5 | 测试文档上传 + 问答 | 4 |

---

## 7. 参考来源

- [Redis — 10 Techniques to Improve RAG Accuracy](https://redis.io/blog/10-techniques-to-improve-rag-accuracy/)
- [Anthropic — Contextual Retrieval](https://www.anthropic.com/news/contextual-retrieval)
- [Milvus — Max-Min Semantic Chunking](https://milvus.io/blog/embedding-first-chunking-second-smarter-rag-retrieval-with-max-min-semantic-chunking)
- [LangChain4j — RAG Tutorial](https://docs.langchain4j.dev/tutorials/rag)
- [LangChain4j — RAG Examples](https://github.com/langchain4j/langchain4j-examples/tree/main/rag-examples)
- [Blended RAG (IBM)](https://arxiv.org/abs/2309.09966)
