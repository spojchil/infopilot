# InfoPilot 项目设计

> 持续更新，与代码保持同步。

## 概述

InfoPilot 是 AI 驱动的信息领航平台。用户上传文档，系统通过 RAG 管道实现智能问答。

## 核心链路

```
文档上传 → 解析提取 → 分块 → 向量化 → 存入 PGVector
                                          ↓
用户提问 → 向量化 → 相似度检索 → 上下文拼接 → LLM 生成回答
```

## 包结构

```
infopilot-server/src/main/java/.../infopilot/server/
├── chat/          # 对话会话管理、多轮上下文
├── document/      # 文档上传、解析、分块、向量化
├── retrieval/     # 向量检索、重排序、上下文组装
├── config/        # LLM 配置、数据源配置、异步配置
└── model/         # DTO、Entity、Enum
```

## 数据模型

- **Document** — 文档元信息（文件名、类型、大小、状态）
- **Chunk** — 文档分块（内容、向量、位置）
- **Conversation** — 对话会话
- **Message** — 对话消息

## API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/chat` | 发送消息，返回 SSE 流式回答 |
| POST | `/api/document/upload` | 上传文档 |
| GET | `/api/document` | 文档列表 |
| DELETE | `/api/document/{id}` | 删除文档 |

## 技术选型

| 层 | 技术 |
|----|------|
| 框架 | Spring Boot 4.0.6 |
| LLM | LangChain4j 1.15 |
| 向量存储 | PostgreSQL 16 + PGVector |
| ORM | MyBatis-Plus 3.5.15 |
| 缓存 | Redis 7 |
| 部署 | Docker Compose |
