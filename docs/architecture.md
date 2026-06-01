# InfoPilot 架构设计

> 面向 MVP 阶段的系统设计，随项目演进持续更新。

---

## 1. 系统分层架构

```
┌─────────────────────────────────────────────────────────────┐
│                        客户端（浏览器 / API）                 │
└──────────────────────┬──────────────────────────────────────┘
                       │ HTTP/SSE
┌──────────────────────┴──────────────────────────────────────┐
│                    Spring Boot 容器层                         │
│  ┌─────────────────────────────────────────────────────┐    │
│  │               chat/ — 对话问答                        │    │
│  │    ChatController · ChatService · ContextManager     │    │
│  └──────────────────────┬──────────────────────────────┘    │
│  ┌──────────────────────┴──────────────────────────────┐    │
│  │             document/ — 文档摄入与处理                │    │
│  │    DocController · DocumentService · DocumentParser  │    │
│  └──────────────────────┬──────────────────────────────┘    │
│  ┌──────────────────────┴──────────────────────────────┐    │
│  │            retrieval/ — RAG 检索管道                  │    │
│  │    RetrievalService · ChunkStrategy                  │    │
│  └──────────────────────┬──────────────────────────────┘    │
│                         │                                    │
│  ┌──────────────────────┴──────────────────────────────┐    │
│  │              LangChain4j 集成层                      │    │
│  │  ChatModel · StreamingChatModel · EmbeddingModel    │    │
│  │  PgVectorEmbeddingStore · AiServices                │    │
│  └───────────────────┬────────────────────────────────┘    │
└──────────────────────┼──────────────────────────────────────┘
                       │
       ┌───────────────┼───────────────┐
       │               │               │
┌──────┴──────┐ ┌──────┴──────┐ ┌──────┴──────┐
│   DeepSeek  │ │  智谱 AI    │ │  PGVector   │
│   (对话)    │ │  (嵌入)     │ │  (向量库)   │
└─────────────┘ └─────────────┘ └─────────────┘

┌──────────────┐ ┌──────────────┐
│  PostgreSQL  │ │    Redis     │
│  (业务库+向量)│ │   (缓存)     │
└──────────────┘ └──────────────┘
```

---

## 2. 模块结构

```
infopilot (父 POM, pom 打包)
│
└── infopilot-server (核心业务, jar + Spring Boot)
    └── src/main/java/io/github/spojchil/infopilot/server/
        ├── chat/          # 对话会话管理、多轮上下文
        ├── document/      # 文档上传、解析、分块、向量化
        ├── retrieval/     # 向量检索、重排序、上下文组装
        ├── config/        # LLM 配置、数据源配置、异步配置
        └── model/         # DTO、Entity、Enum
```

**单模块决策**：MVP 阶段功能集中，拆模块的收益（独立版本、并行编译）远小于成本（模块间 API 契约维护）。

---

## 3. 请求处理流程

### 3.1 普通对话

```
POST /api/chat
    │
    ▼
chat.ChatController
    │
    ▼
chat.ChatService
    ├─→ ContextManager.load(sessionId)     ← Redis 加载对话历史
    ├─→ PromptManager.build(system, hist)   ← 组装 Prompt
    ├─→ ChatModel.chat(messages)            ← LangChain4j → DeepSeek
    └─→ ContextManager.save(sessionId)      ← Redis 保存对话历史
    │
    ▼
SSE 流式响应（Flux<String>）
```

### 3.2 RAG 问答

```
POST /api/document/search
    │
    ▼
document.DocController
    │
    ▼
document.DocumentService
    ├─→ EmbeddingModel.embed(query)         ← 嵌入模型向量化查询
    ├─→ EmbeddingStore.search(embedding)    ← PGVector 相似度检索
    ├─→ ContextManager.build(hist, chunks)  ← 检索结果注入 Prompt
    └─→ ChatModel.chat(messages)            ← LLM 生成回答
    │
    ▼
SSE 流式响应
```

### 3.3 文档摄入

```
POST /api/document/upload  (multipart)
    │
    ▼
document.DocController
    │
    ▼
document.DocumentService
    ├─→ DocumentParser.parse(inputStream)   ← Tika 解析文件
    ├─→ ChunkStrategy.split(text)           ← 递归切分
    ├─→ EmbeddingModel.embedAll(segments)   ← 批量向量化
    └─→ EmbeddingStore.addAll(embeddings)   ← PGVector 存储
```

---

## 4. 包结构（infopilot-server）

```
io.github.spojchil.infopilot.server
│
├── InfoPilotApplication.java          — 启动入口
│
├── chat/                              — 对话问答
│   ├── ChatController.java            — 对话接口
│   ├── ChatService.java               — 对话核心逻辑
│   └── ContextManager.java            — 对话上下文管理（Redis）
│
├── document/                          — 文档摄入与处理
│   ├── DocController.java             — 文档接口（上传、查询、删除）
│   ├── DocumentService.java           — 文档加载、切分、向量化
│   └── DocumentParser.java            — 文件解析（PDF/Word/Text）
│
├── retrieval/                         — RAG 检索管道
│   ├── RetrievalService.java          — 检索管道（搜索+组装注入）
│   └── ChunkStrategy.java             — 切片策略
│
├── config/                            — 配置类
│   ├── LangChain4jConfig.java         — ChatModel / Embedding / EmbeddingStore Bean
│   ├── MybatisPlusConfig.java         — MapperScan 配置
│   └── MybatisMetaObjectHandler.java  — 自动填充
│
└── model/                             — 数据模型
    ├── entity/                        — 数据库实体
    ├── dto/                           — 数据传输对象
    └── enums/                         — 枚举
```

---

## 5. 数据模型概览

```
┌──────────────────────────────────────────────────┐
│              PostgreSQL + PGVector                │
├──────────────────────────────────────────────────┤
│ 对话会话     │ chat_session (id, user_id, title)  │
│ 对话消息     │ chat_message (id, session_id, ...) │
│ 文档记录     │ document (id, name, type, status)  │
│ embeddings 表 — 文档切片向量，含元数据过滤字段     │
│   (id, embedding(vector), text, knowledge_id,     │
│    doc_id, tenant_id, user_id, create_time)       │
└──────────────────────────────────────────────────┘
```

---

> 详细设计方案见 `docs/rag-design.md`
