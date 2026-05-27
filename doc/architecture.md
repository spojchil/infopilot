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
│  │                Controller（接口层）                    │    │
│  │    ChatController    DocController    AgentController │    │
│  └──────────────────────┬──────────────────────────────┘    │
│                         │                                    │
│  ┌──────────────────────┴──────────────────────────────┐    │
│  │                Service（业务层）                       │    │
│  │    ChatService      DocService       AgentService     │    │
│  │    ContextManager   PromptManager                     │    │
│  └──────┬───────────────┬──────────────┬────────────────┘    │
│         │               │              │                      │
│  ┌──────┴───┐  ┌────────┴───┐  ┌──────┴──────────┐          │
│  │  对话引擎  │  │  RAG 管道  │  │  Agent 编排     │          │
│  │ (LLM)    │  │  (检索)    │  │  (工具+推理)    │          │
│  └──────┬───┘  └────┬──────┘  └──────┬──────────┘          │
│         │           │              │                         │
│  ┌──────┴───────────┴──────────────┴──────────────────┐    │
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
│   MySQL      │ │    Redis     │
│   (业务库)   │ │   (缓存)     │
└──────────────┘ └──────────────┘
```

---

## 2. 模块依赖关系

```
infopilot (父 POM, pom 打包)
│
├── infopilot-common (公共模块, jar)
│   ├── entity/          — 数据库实体
│   ├── dto/             — 数据传输对象
│   ├── utils/           — 通用工具类
│   └── config/          — 通用配置（MyBatis-Plus 等）
│
└── infopilot-server (核心业务, jar + Spring Boot)
    ├── controller/      — REST/SSE 接口
    ├── service/         — 业务逻辑
    │   ├── chat/        — 对话服务
    │   ├── document/    — 文档管理
    │   ├── rag/         — RAG 检索管道
    │   └── agent/       — Agent 编排
    ├── config/          — Bean 配置（LangChain4j 等）
    └── tools/           — @Tool 工具定义
```

**依赖方向：** server → common（common 不依赖 server）

---

## 3. 请求处理流程

### 3.1 普通对话

```
POST /api/chat
    │
    ▼
ChatController
    │
    ▼
ChatService
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
POST /api/doc/chat
    │
    ▼
DocController
    │
    ▼
DocService
    ├─→ EmbeddingModel.embed(query)         ← 智谱 embedding-3 向量化查询
    ├─→ EmbeddingStore.search(embedding)    ← PGVector 相似度检索
    ├─→ ContextManager.build(hist, chunks)  ← 检索结果注入 Prompt
    └─→ ChatModel.chat(messages)            ← LLM 生成回答
    │
    ▼
SSE 流式响应
```

### 3.3 Agent 工具调用

```
POST /api/agent/run
    │
    ▼
AgentController
    │
    ▼
AgentService
    ├─→ AiServices.create(AgentClass, model)
    │       .tools(searchTool, queryTool, ...)
    │       .chatMemory(window)
    │       .build()
    │
    ├─→ Agent.chat(task)                    ← LLM 自主决策调用哪些工具
    │       │
    │       ├─→ searchKnowledge("订单查询")  → PGVector 检索
    │       └─→ queryDatabase("2024-05")    → MyBatis 查数据库
    │
    ▼
SSE 流式响应
```

---

## 4. 服务层包结构（infopilot-server）

```
io.github.spojchil.infopilot.server
│
├── InfoPilotApplication.java          — 启动入口
│
├── config/                            — 配置类
│   └── LangChain4jConfig.java         — ChatModel/Embedding/EmbeddingStore Bean
│
├── controller/                        — REST 控制器
│   ├── ChatController.java            — 对话接口（GET /chat, GET /chat/stream）
│   ├── DocController.java             — 文档接口（上传、查询、RAG 问答）
│   └── AgentController.java           — Agent 接口
│
├── service/                           — 业务服务
│   ├── chat/
│   │   ├── ChatService.java           — 对话核心逻辑
│   │   └── ContextManager.java        — 对话上下文管理（Redis）
│   ├── document/
│   │   ├── DocumentService.java       — 文档加载、切分、向量化
│   │   └── DocumentParser.java        — 文件解析（PDF/Word/Text）
│   ├── rag/
│   │   ├── RetrievalService.java      — 检索管道（搜索+重排+注入）
│   │   └── ChunkStrategy.java         — 切片策略
│   └── agent/
│       ├── AgentService.java          — Agent 编排
│       └── AgentMemory.java           — Agent 记忆管理
│
└── tools/                             — @Tool 定义
    ├── KnowledgeSearchTool.java       — 知识库检索工具
    ├── DatabaseQueryTool.java         — 数据库查询工具
    └── CalculatorTool.java            — 计算工具（示例）
```

---

## 5. 数据模型概览

```
┌──────────────────────────────────────────────────┐
│                  MySQL 业务表                      │
├─────────────┬────────────────────────────────────┤
│ 对话会话     │ chat_session (id, user_id, title)  │
│ 对话消息     │ chat_message (id, session_id, ...)  │
│ 文档记录     │ document (id, name, type, status)  │
│ 知识库       │ knowledge_base (id, name, ...)      │
├─────────────┴────────────────────────────────────┤
│              PostgreSQL + PGVector                │
├──────────────────────────────────────────────────┤
│ embeddings 表 — 文档切片向量，含元数据过滤字段     │
│   (id, embedding(vector), text, knowledge_id,     │
│    doc_id, tenant_id, user_id, create_time)       │
└──────────────────────────────────────────────────┘
```

---

## 6. MVP 开发路线

```
Week 1（阶段 1）         Week 2（阶段 2-3）        Week 3（阶段 4）         Week 4（阶段 5）
┌────────────────┐    ┌────────────────┐    ┌────────────────┐    ┌────────────────┐
│ ✅ 架构搭建     │    │ ○ Prompt 设计   │    │ ○ 文档加载      │    │ ○ Agent 编排   │
│ ✅ Bean 配置    │    │ ○ 上下文管理    │    │ ○ 切片 & 向量化 │    │ ○ 工具路由     │
│ ○ 对话接口     │    │ ○ 工具调用      │    │ ○ PGVector 检索 │    │ ○ 多步推理     │
│ ○ 流式 SSE     │    │ ○ SSE 流式接口  │    │ ○ 检索注入      │    │ ○ 安全边界     │
└────────────────┘    └────────────────┘    └────────────────┘    └────────────────┘
```

---

> *与 `learning-roadmap.md` 互补阅读——roadmap 讲"学什么"，architecture 讲"怎么搭"*
