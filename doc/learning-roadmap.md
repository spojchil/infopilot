# AI 应用开发学习路线 v3

> **面向 Java 后端工程师** · 聚焦应用层，跳过研究层 · 每阶段附精选学习资料

---

## 暂时跳过 — 研究者才需要，应用开发者不用

`反向传播算法` `注意力机制数学推导` `深度学习网络结构` `模型预训练流程` `CUDA / GPU 编程` `Fine-tuning 模型微调`

---

## 架构总览 — 先看懂这张图再开始

六个阶段不是平行的知识点，而是有依赖关系的分层架构：

```
┌──────────────────────────────────────────┐
│         阶段 6：Harness 工程化             │  ← 权限 / 可观测性 / 成本 / 兜底
├──────────────────────────────────────────┤
│         阶段 5：Agent（编排层）            │  ← 多步推理 / 工具编排 / 自主决策
├──────────────────────────────────────────┤
│         阶段 4：RAG（知识注入层）          │  ← 检索 / 重排 / 上下文组装
├──────────────────────────────────────────┤
│      阶段 3：Tools API + 结构化输出        │  ← JSON 输出 / 工具调用 / 流式响应
├──────────────────────────────────────────┤
│      阶段 2.5：上下文工程                 │  ← 对话管理 / Token Budget / 上下文裁剪
├──────────────────────────────────────────┤
│      阶段 2：Prompt Engineering           │  ← 指令控制 / 格式约束 / 稳定性
├──────────────────────────────────────────┤
│      阶段 1：LLM 基本概念（模型层）        │  ← Token / 温度 / 上下文窗口 / 模型选型
└──────────────────────────────────────────┘
```

**为什么是这个顺序？** 每一层都是下一层的前提：Prompt 决定模型行为 → 上下文工程管理 Prompt 所处的环境（多轮对话、长文本）→ Tools 把行为结构化（`tool_use block` 的回传机制是理解 Agent 循环的前置条件）→ RAG 给 Tools 提供动态知识 → Agent 把 Tools + RAG 编排成多步任务 → Harness 让 Agent 在生产环境可信赖地运行。跳层学习会导致"知道概念但不明白为什么"。

---

## 贯穿项目 — 一条主线串联六个阶段

> 推荐项目：**企业内部文档问答助手**（可替换为你团队真实的业务场景）

| 阶段 | 在项目中完成的模块 |
|---|---|
| 阶段 1 | 用 Java SDK 调通 API，写一个 CLI 聊天接口，观察不同 temperature 对回答风格的影响 |
| 阶段 2 | 设计 System Prompt：角色定义 + 回答格式 + 拒绝范围 + 负向示例，测试 10 个边界问题 |
| 阶段 2.5 | 为项目加入多轮对话支持，实现对话历史裁剪和 Token Budget 控制，20 轮对话不超窗口 |
| 阶段 3 | 加入工具调用：让模型能查询数据库、返回结构化 JSON 结果，映射到 Java DTO；流式 SSE 接口 |
| 阶段 4 | 接入企业文档库：PDF 切片 → 向量化 → PGVector 存储 → 检索注入 Prompt |
| 阶段 5 | 升级为 Agent：自动判断「查文档」还是「查数据库」，多步骤完成「查询→分析→生成报告」 |
| 阶段 6 | 加上 Eval 指标、链路追踪、Token 成本告警、权限控制，达到生产可用标准 |

---

## 阶段 1：LLM 基本概念

**关键词：** Token · 上下文窗口 · Temperature · API 消息结构 · 模型选型

**时间：** 1~2 周（有 REST API 经验可压缩至 3~5 天）

### 模型选型（别纠结，先选一个跑起来）

从学习到生产，你大概需要两档模型。不需要第一天就搞懂所有模型，但需要知道选哪个开始：

- **日常调试 + 高频调用：DeepSeek** — 极低价格，OpenAI 兼容接口，调试阶段放心刷，不心疼 Token。注册即用，有免费额度
- **正式开发 + 复杂任务：Claude Sonnet** — Agent / 复杂 Prompt / 长文档分析场景的当前首选，Tool Use 最成熟
- **通用 + 性价比：GPT-4o-mini** — 翻译、摘要、格式转换等中低难度任务，成本约为 Sonnet 的 1/20

核心原则：**不要写死某个厂商。** LangChain4j（本项目选用）和 Spring AI 都提供统一的模型接口，换模型只需改配置。从第一天就习惯"模型是可替换的"。

### 核心概念

`Token 与生成原理` `Context Window` `Temperature / Top-P` `System / User / Assistant 消息` `max_tokens` `流式输出基础`

### 学习资料

- **🔧 [OpenAI Tokenizer Playground](https://platform.openai.com/tokenizer)** — 输入任意文字，直观看到 token 切分方式。理解 token 最快的方式，5 分钟建立直觉
- **📄 [Anthropic — Models Overview](https://docs.anthropic.com/en/docs/about-claude/models/overview)** — 各模型的 context window、能力差异对比。API 调用的起点，直接看这里选模型
- **🎬 [Andrej Karpathy — Intro to LLMs（1小时）](https://www.youtube.com/watch?v=zjkBMFhNj_g)** — 前 OpenAI 核心成员讲解 LLM 是什么。只看前 30 分钟建立直觉，不需要懂数学。这阶段唯一推荐的视频
- **📄 [LangChain4j — Chat Language Model](https://docs.langchain4j.dev/tutorials/chat-language-model)** — Java 生态起点（本项目选用）。`ChatLanguageModel.generate()` 同步调用，`StreamingChatLanguageModel` 流式调用，马上能跑起来
- **📄 [Spring AI — Chat Model API](https://docs.spring.io/spring-ai/reference/api/chatmodel.html)** — 另一主流选择。同步调用（`call()`）和流式调用（`stream()` → `Flux<String>`）两种模式都在这里

### 本地模型替代方案（可选，但强烈建议了解）

企业级场景常有数据合规要求，完全依赖云端 API 不现实。Ollama 让你在本地跑开源模型（Llama、Qwen、DeepSeek），LangChain4j 和 Spring AI 只需修改 `baseUrl` 配置即可无缝切换：

- **🔧 [Ollama 官网](https://ollama.com)** — 本地运行大模型，类似 Docker 管理镜像的方式管理模型，`ollama run llama3` 一行命令启动
- **📄 [LangChain4j — Ollama 集成](https://docs.langchain4j.dev/integrations/language-models/ollama)** — 配置 `baseUrl` 后，代码层面与调用 OpenAI 完全一致，无需改业务逻辑
- **📄 [Spring AI — Ollama 官方文档](https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html)** — 配置 `spring.ai.ollama.base-url` 后，同样无缝切换

### ⚠️ 成本意识（从第一天开始）

API 调用按 token 计费，从现在起养成三个习惯：查看每次响应的 `usage` 字段、设置 `max_tokens` 上限、非创意场景把 `temperature` 设在 0.3 以下。Token 消耗不关注，阶段 4~5 的调试成本会超出预期。

### 阶段目标

能用 Java SDK 或 curl 调通 API，理解 System Prompt 的作用，能调整 temperature 观察效果变化；能用 `stream()` 返回流式响应而不是阻塞等待

---

## 阶段 2：Prompt Engineering

**关键词：** Few-shot · CoT · System Prompt 设计 · XML 结构化

**时间：** 2~3 周

### 核心概念

`Few-shot 示例` `Chain of Thought` `System Prompt 设计` `XML 标签控制格式` `负向示例` `指令优先级`

### 学习资料（以官方文档为主，视频已过时）

- **📄 ⭐ [Anthropic Prompt Engineering Guide](https://docs.anthropic.com/en/docs/build-with-claude/prompt-engineering/overview)** — 业内最好的 Prompt Engineering 文档之一。有明确技巧 + 正反例对比，直接从这里开始，不要看视频
- **📄 [OpenAI Prompt Engineering Guide](https://platform.openai.com/docs/guides/prompt-engineering)** — 六大策略框架，与 Anthropic 文档互为补充。两个一起看，覆盖面更全，不同模型思路有差异
- **📦 ⭐ [anthropics/courses — 官方课程 Notebook](https://github.com/anthropics/courses)** — Anthropic 官方出品，Jupyter Notebook 形式，边跑代码边学。Prompt Engineering 系列直接动手练，比看视频强

### 💡 Eval 意识前置（从写 Prompt 开始就要有）

Prompt 写完不是凭感觉判断好不好，而是用测试用例验证：对 10 个典型输入，输出是否符合预期格式？边界条件是否被正确拒绝？这个习惯在阶段 4（RAG）会救你大量调试时间。阶段 6 会系统化，但意识现在就建立。

### 阶段目标

能为一个真实业务场景写出稳定、可预期的 System Prompt，包含角色定义 + 格式约束 + 边界规则；能用 10 个测试用例验证 Prompt 的稳定性

---

## 阶段 2.5：上下文工程（Context Engineering）

**关键词：** 对话历史管理 · 上下文裁剪 · Token Budget · 分层记忆

**时间：** 1 周（和阶段 2~3 在实践中交替学习）

### 为什么在 Prompt 之后、Tools 之前？

学完 Prompt Engineering 之后，你马上会遇到一个问题：单轮问答很简单，但三轮对话后模型就开始"忘事"了——因为历史消息塞满了上下文窗口。

上下文工程解决的是"给模型看什么信息"的问题，比 Prompt Engineering 高半层：Prompt 决定指令怎么写，上下文工程决定指令周围放什么。把它放在 Tools 和 RAG 之前，是因为后续每个阶段都会向上下文里塞更多东西——工具调用结果、检索片段、Agent 中间推理——你需要先建立上下文管理的框架，再往里填充内容。

### 核心概念

`对话历史裁剪` `Summary Memory（摘要压缩）` `Sliding Window` `Token Budget 分配`
`RAG Context Packing（检索结果排布）` `Context 去重与优先级`

### 实践要点

- **历史裁剪**：超过窗口时，优先保留 System Prompt + 最近 N 轮 + 关键工具结果，中间轮次压缩为摘要。不要等 API 报 context length exceeded 才处理，要提前裁剪
- **Token Budget**：给 System Prompt、历史消息、检索结果、模型输出各分配上限，任何一块超限就触发裁剪逻辑。这个习惯从多轮对话开始就要有，到 RAG 阶段会自然扩展
- **RAG 注入排布**（阶段 4 会深入）：检索回来的片段，相关性最高的放靠近问题的位置。模型对上下文位置敏感——长上下文的中间部分最容易被忽略（Lost in the Middle 问题）。此处先知道概念，阶段 4 实操

### 阶段目标

能处理 20 轮以上的多轮对话而不超窗口；RAG 检索结果超过 context 限制时有明确的裁剪策略

---

## 阶段 3：Tools API & 结构化输出

**关键词：** Function Calling · JSON Schema · tool_use · 流式输出

**时间：** 1~2 周

### 核心概念

`tools 参数声明` `tool_use block` `tool_result 回传` `JSON Schema 定义` `response_format` `多工具路由` `SSE 流式响应` `Flux<String>`

### 学习资料（这阶段必须看文档，视频全部过时）

- **📄 ⭐ [Anthropic — Tool Use Overview](https://docs.anthropic.com/en/docs/build-with-claude/tool-use/overview)** — 完整讲解 tools 参数如何声明、模型返回 tool_use block、如何回传 tool_result。这是核心，反复读到能默写流程
- **📄 ⭐ [OpenAI — Structured Outputs](https://platform.openai.com/docs/guides/structured-outputs)** — response_format + JSON Schema 严格模式的完整文档。完全替代「靠 prompt 让模型输出 JSON」的旧做法
- **📦 [anthropics/courses — Tool Use 系列 Notebook](https://github.com/anthropics/courses/tree/master/tool_use)** — 配套练习，从单工具到多工具、到工具链，循序渐进。跑完这个系列，概念就全通了
- **📄 ⭐ [LangChain4j — Streaming](https://docs.langchain4j.dev/tutorials/streaming)** — `StreamingChatLanguageModel.generate()` 通过 `TokenStream` 回调逐 token 推送，配合 Spring WebFlux 的 `SseEmitter` 实现 SSE。本项目选用此方案
- **📄 [Spring AI — Streaming Chat Client](https://docs.spring.io/spring-ai/reference/api/chatclient.html)** — `chatClient.stream().content()` 返回 `Flux<String>`，配合 Spring WebFlux 实现 SSE。另一主流方案

### 为什么流式输出在这里学？

LLM 响应慢（平均 3~10 秒），阻塞等待会让用户体验极差。`stream()` + SSE 让第一个 token 在 200ms 内就到达前端。LangChain4j 的 `StreamingChatLanguageModel` 通过回调逐 token 推送，Spring AI 的 `StreamingChatModel` 返回 `Flux<ChatResponse>`，两者都支持与 WebMVC 共存——加 `spring-boot-starter-webflux` 依赖即可，不需要整体迁移到响应式。

### Java 生态对照

| 能力 | Anthropic/OpenAI API | LangChain4j 实现（本项目） | Spring AI 实现 |
|---|---|---|---|
| 工具调用声明 | `tools` 参数 | `@Tool` 注解方法 | `@Tool` 注解 / `FunctionCallback` |
| 结构化输出 | JSON Schema | `AiServices` + 返回类型约束 | `BeanOutputConverter<YourDTO>` |
| 流式响应 | SSE stream | `StreamingChatLanguageModel` + 回调 | `chatClient.stream().content()` → `Flux<String>` |

### 阶段目标

能用 tools 参数让模型返回严格 JSON 并映射到 Java 对象，不再依赖 prompt 拼凑 + 手工解析；能用 `stream()` 实现流式 SSE 接口

---

## 阶段 4：RAG — 检索增强生成

**关键词：** Embedding · 向量数据库 · 切片策略 · 检索优化 · RAG Eval

**时间：** 3~4 周（跑通原型）+ 2 周（调到可信赖）

### 核心概念

`Embedding 向量` `余弦相似度` `Chunk 切片策略` `向量检索` `Rerank 重排` `HyDE` `混合检索（向量 + BM25）` `元数据过滤` `RAG Evaluation`

### 学习资料

- **📦 ⭐ [langchain-ai/rag-from-scratch](https://github.com/langchain-ai/rag-from-scratch)** — LangChain 官方出品，每个概念一个独立 Notebook，从 Naive RAG 到高级优化。教程用 Python 编写，**重点是理解流程和思路，后续用 LangChain4j 或 Spring AI 在 Java 中实现**，不要陷入 Python 框架细节
- **📄 ⭐ [LangChain4j — Embedding Store](https://docs.langchain4j.dev/tutorials/embedding-store)** — LangChain4j 向量存储接口，支持 PGVector、Chroma、Milvus 等主流向量库，本项目选用 PGVector
- **📄 [Spring AI — Vector Database 文档](https://docs.spring.io/spring-ai/reference/api/vectordbs.html)** — Spring AI 向量库接口，同样支持 PGVector 等，另一主流选择
- **📦 [Spring AI — RAG with Docling 实战](https://github.com/spring-ai-community/awesome-spring-ai)** — Spring AI Community 维护的资料汇总，含 Docling 解析 PDF/Word、RAG 端到端 Java 实现等多个实战教程
- **📦 [LangChain4j — RAG 教程](https://docs.langchain4j.dev/tutorials/rag)** — LangChain4j 官方 RAG 文档，含文档加载、切分、嵌入、检索全流程
- **📦 ⭐ [confident-ai/deepeval](https://github.com/confident-ai/deepeval)** — RAG 评测框架，类似 Pytest 的用法。提供 Faithfulness（忠实度）、Contextual Recall（检索召回率）、Answer Relevancy 等核心指标。**没有 Eval，chunk 策略和 rerank 调优就是在盲飞**

### 向量库选型（Java 后端视角）

| 向量库 | 适合场景 | 上手成本 |
|---|---|---|
| **PGVector** | 已有 PostgreSQL，推荐首选 | 低（加一个扩展） |
| **Chroma** | 本地原型验证 | 低 |
| **Milvus / Weaviate** | 亿级向量，生产大规模 | 高 |

> 建议从 PGVector 开始，它的运维成本最低，LangChain4j 和 Spring AI 都有完善支持。

### Embedding 模型选型

| 场景 | 推荐模型 | 说明 |
|---|---|---|
| 中文为主 + 快速启动 | `text-embedding-3-small` | OpenAI，按量计费，接入简单 |
| 多语言 + 数据隐私 | `BAAI/bge-m3` | 开源，支持本地部署，100+ 语言 |
| 中英混合 + 高质量 | `text-embedding-3-large` | OpenAI，成本略高但效果更好 |

> 经验：先用 `text-embedding-3-small` 跑通流程，再根据检索质量决定是否升级。

### 混合检索（向量 + BM25）：为什么重要，以及当下怎么做

纯向量检索擅长语义匹配，但关键词精确匹配（产品编号、错误码、人名）不如传统全文检索。工程上公认的最佳实践是**混合检索**——向量检索做语义召回，BM25 做关键词召回，再把两路结果融合排序。

LangChain4j 和 Spring AI 的向量检索支持都很成熟，但对 BM25 的原生集成还在早期阶段。目前有两个务实路径：

- **轻量方案**：用 PGVector 的全文搜索能力（PostgreSQL 内置 `tsvector`），与向量检索在同一查询中联合，走 SQL 层面的融合。学习成本最低，Java 后端直接用 JPA/Native Query
- **完整方案**：独立部署 Elasticsearch 做 BM25 检索，向量库用 PGVector / Milvus，在应用层做两路结果融合和重排。检索质量更高，但架构多了一个组件

> 建议：原型阶段先用纯向量检索跑通流程，检索质量瓶颈确认在关键词匹配后，再引入 BM25 做混合。

### ⚠️ 多租户场景必须加元数据过滤

纯向量检索没有"范围"概念，用户问"我昨天的订单出了什么问题"，
检索会把所有用户的订单记录都算进候选集。没有元数据过滤的 RAG
在多租户场景下存在数据泄露风险，这不是性能问题，是正确性问题。

解法：向量检索前加结构化过滤条件（`user_id`、`tenant_id`、时间范围等），
先缩小候选集，再在子集里做相似度计算。LangChain4j 的 `EmbeddingStore` 接口和 Spring AI 的 `FilterExpressionBuilder` 都支持元数据过滤，PGVector 底层是 SQL WHERE 条件，对 Java 后端几乎没有学习成本。

### ⚠️ RAG 调优必须有 Eval 指标

没有量化指标的调优等于猜测。在开始调 chunk 大小、rerank 策略前，先用 DeepEval 或 RAGAS 建立基准分数。每次调整后对比分数变化，才能确认改动有效。

### 阶段目标

独立完成文档问答系统：PDF 切片 → 向量化 → 存入 PGVector → 检索 → 注入 Prompt → 生成回答，端到端跑通；**用 DeepEval 跑一轮基准评估，拿到 Faithfulness 和 Contextual Recall 的基准分**

---

## 阶段 5：Agent

**关键词：** ReAct 循环 · 工具编排 · 多步骤推理 · MCP 协议（附录）

**时间：** 3~4 周（跑通）+ 持续打磨

### 核心概念

`ReAct 推理循环` `工具注册与路由` `多步骤规划` `Agent 记忆管理` `错误恢复` `Human-in-the-Loop`

### MCP 协议的位置

MCP（Model Context Protocol）是工具接入的标准化协议——让不同 Agent 框架用统一的方式对接工具。它是一个重要方向，但不是当前必须精通的核心能力。懂 Function Calling + 工具编排之后，MCP 自然就理解了。建议在学完 Agent 基础后，用 1~2 小时浏览 [MCP 官方规范文档](https://modelcontextprotocol.io/introduction)，知道它解决什么问题即可。后续如果团队需要跨框架工具复用，再深入。

### ⚠️ Agent 执行安全：不可逆操作的边界设计

Agent 能调用工具意味着它能造成真实影响。业务 Agent 的安全边界和编码 Agent 本质相同，
只是威胁形态不同：不是 `rm -rf`，而是误删订单、向全量用户发邮件、用错误参数调了支付接口。

**三个必须回答的设计问题：**

1. **哪些工具是不可逆的？** 读操作（查数据库、查订单）和写操作（发邮件、修改记录、
   调外部 API）要显式分类。不可逆工具要单独对待。

2. **不可逆操作走什么流程？** 两种策略：
   - Human-in-the-Loop：Agent 生成操作计划，人确认后再执行（适合低频、高风险）
   - Dry-run 模式：先模拟执行并展示结果，用户确认后再真正提交（适合批量操作）

3. **工具失败怎么回滚？** Agent 多步推理中，步骤 3 成功、步骤 4 失败时，
   步骤 3 的副作用怎么撤销？设计工具时要同时设计对应的撤销接口。

**Java 生态落地：** 在 LangChain4j 的 `@Tool` 方法或 Spring AI 的 Tool 定义里加 `@Confirmable`（或自定义注解），拦截不可逆操作跳转确认流程；用 Spring AOP 统一记录工具调用审计日志，出问题可回放整条执行链路。

### 学习资料

- **📄 ⭐ [Anthropic — Building Effective Agents](https://www.anthropic.com/research/building-effective-agents)** — Agent 设计的五种核心模式（Prompt Chaining / Routing / Parallelization / Orchestrator-Worker / Evaluator-Optimizer）。比「ReAct 原始论文」更实用，直接告诉你什么时候用哪种模式
- **📄 ⭐ [LangChain4j — AI Services（Java 生态）](https://docs.langchain4j.dev/tutorials/ai-services)** — Java 版 Agent 框架（本项目选用），用 `@Tool` 注解定义工具，`AiServices` 声明式构建 Agent，与 Spring 无缝集成。注意：LangChain4j API 迭代较快，以官方文档为准，不要依赖旧博客
- **📄 [Spring AI — Building Effective Agents（Java 实现）](https://docs.spring.io/spring-ai/reference/api/effective-agents.html)** — Spring 团队基于 Anthropic 的五种模式，提供 Java 代码实现。Anthropic 文档讲原理，Spring AI 文档给 Java 落地，两个配套阅读

### Agent 框架选型（Java 生态）

| 框架 | 适合场景 | 稳定性 |
|---|---|---|
| **LangChain4j** | Agent 能力成熟，国产模型支持好，不绑定 Spring（本项目选用） | 中高（迭代较快，开发者采用率 68%） |
| **Spring AI** | Spring Boot 团队，Spring 生态原生集成 | 高，Spring 生态背书 |
| 两者不互斥 | 可按需组合使用，Spring 做基础架构，LangChain4j 做 Agent 编排 | — |

### 阶段目标

写一个能自主调用 2~3 个工具完成多步任务的 Agent，例如：查文档 → 查数据库 → 计算 → 生成报告；理解什么时候用 Workflow（确定性流程），什么时候用 Agent（需要模型自主决策）

---

## 阶段 6：Harness 工程化

**关键词：** 可观测性 · Eval 体系 · 成本管控 · 权限边界 · 失败兜底

**时间：** 持续积累（每个子方向各需 1~2 周专项投入）

> 这是最容易被低估的阶段。Demo 能跑和生产可用之间，Harness 是那道墙。

### 核心概念

`渐进披露` `权限边界设计` `Prompt Injection 防御` `Evaluation 评测体系` `Tracing 链路追踪` `语义缓存` `成本告警` `失败兜底 / 模型降级`

### 学习资料

**可观测性 & 链路追踪**

- **📄 ⭐ [Langfuse — LangChain4j 集成](https://langfuse.com/integrations/frameworks/langchain4j)** — LLM 链路追踪工具，开源可自托管（MIT 协议）。LangChain4j 通过 `LangChain4j` 回调直接集成，Spring AI 通过 Actuator + Micrometer + OpenTelemetry 接入，无需改业务代码，即可在 Langfuse UI 中看到每次 LLM 调用的 Prompt、Response、Token 消耗、耗时。**从 Agent 开始就应该接入，否则调试是灾难**
- **📄 [Langfuse — Spring AI 集成文档](https://langfuse.com/integrations/frameworks/spring-ai)** — Spring AI 的 Langfuse 集成方式
- **📦 [arize-ai/phoenix](https://github.com/Arize-ai/phoenix)** — 另一个开源 LLM 可观测性工具（Elastic License），更侧重 Agent 评估和实验分析。Langfuse 偏生产运维，Phoenix 偏开发调试，两者定位互补

**Evaluation 体系**

- **📄 ⭐ [Anthropic — Evaluation Overview](https://docs.anthropic.com/en/docs/test-and-evaluate/eval-overview)** — 如何系统评测 AI 应用效果，不凭感觉判断。覆盖 Prompt Eval、RAG Eval、Agent Eval 三个层次
- **📦 [confident-ai/deepeval](https://github.com/confident-ai/deepeval)** — 在阶段 4 已引入，这里做体系化：把 Eval 集成进 CI/CD，每次 Prompt 变更自动跑评测，防止 Prompt 退化

**工程化实践**

- **📦 ⭐ [langchain4j/langchain4j](https://github.com/langchain4j/langchain4j)** — LangChain4j 官方仓库，含示例模块 `langchain4j-examples`，覆盖 RAG、Agent、工具调用等全场景
- **📦 [spring-ai-community/awesome-spring-ai](https://github.com/spring-ai-community/awesome-spring-ai)** — Spring AI 生态资料汇总，持续更新。含 MCP 集成、多模型路由、RAG 进阶、可观测性等各个方向的一手教程
- **📦 [ai-boost/awesome-harness-engineering](https://github.com/ai-boost/awesome-harness-engineering)** — Harness Engineering 最全资料汇总。含工具、架构模式、Eval 框架、MCP 最新规范、可观测性方案
- **📝 [Simon Willison's Blog](https://simonwillison.net)** — 资深开发者视角，专写「用 LLM 做工程」的实战经验。Prompt Injection 防御、安全边界、成本控制都有深度文章

### 六个工程化专项（逐步完成，不要一次全做）

**1. 链路追踪**：Langfuse 接入，确保每次 LLM 调用可查询、可回放

**2. Eval 体系**：DeepEval 集成 CI，Prompt 变更自动触发评测，设定质量门禁

**3. 成本管控**

- **语义缓存**：普通缓存用字符串精确匹配，"今天天气怎么样"和"今天的天气如何"
  是两个不同 key，各自调一次 API。语义缓存把问题向量化后和历史问题算相似度，
  超过阈值直接返回缓存结果。高频问答产品中，重复问题通常占 30%~40% 请求量，
  这部分成本几乎可以清零。实现：Redis 存向量，请求进来先查缓存再走 LLM

- **小模型路由**：翻译、摘要、格式转换等简单任务不需要大模型，Haiku 或
  GPT-4o-mini 完全够用，成本差距 10~20 倍。路由策略：用规则（问题长度、
  是否含代码、是否需要推理）或用极便宜的小模型先分类，再决定派给哪个模型

- **两者组合**：先过语义缓存，命中直接返回；未命中走路由，简单任务给小模型，
  复杂任务给大模型。日均万次调用的系统，实际成本可压缩到未优化时的 20%~30%

- Token 配额和月度成本告警保持不变

**4. 模型降级**：主模型（如 Claude / GPT-4o）不可用时，自动切换到备用模型（如 DeepSeek / Haiku）；Resilience4j 做熔断限流

**5. 权限边界**：工具调用添加表 / 字段级权限控制；用 Spring Security 管控哪些用户能触发哪些工具

**6. Prompt Injection 防御**：输入过滤、Prompt 模板固化、输出内容合规检查

### 阶段目标

你的 Agent 满足以下条件才算达标：链路可追踪（Langfuse 能看到完整调用链）、质量可量化（DeepEval 有基准分且在 CI 中运行）、成本可控（有 Token 配额和月度告警）、失败可恢复（有降级策略和兜底回复）

---

## 附录：Java 后端特有的坑

| 阶段 | 常见坑 | 避坑建议 |
|---|---|---|
| 阶段 1 | 忘看 `usage`，token 悄悄累积 | 每次响应打印 `usage.totalTokens` |
| 阶段 3 | 阻塞等待导致接口超时 | 优先用 `stream()` + SSE，超时设 30s |
| 阶段 3 | JSON Schema 定义不严谨导致解析失败 | 用 `BeanOutputConverter` 替代手工解析 |
| 阶段 4 | Chunk 过大 / 过小导致检索精度低 | 先按语义边界切（如 PDF 章节），再用 Eval 验证 |
| 阶段 4 | 没有 Eval 就调优 | 先跑基准分，再改策略，再对比分数 |
| 阶段 5 | Agent 工具路由混乱 | 工具描述要精准，加 "何时不应该调用此工具" |
| 阶段 5 | 多步推理无失败兜底 | 加最大迭代次数限制，失败时回退到人工介入 |
| 阶段 6 | 没有链路追踪，调试靠猜 | 从 Agent 阶段就接入 Langfuse |

---

> *更新时间：2026 · 面向应用开发者，不面向研究者*  
> *资料以官方文档 + 一手仓库为主，视频仅第一阶段推荐*  
> *⭐ = 强烈推荐优先阅读*  
> *时间估算说明：「跑通原型」和「调到生产可用」是两回事，阶段 4~5 实际可能各需 2 倍时间，做好预期管理*
