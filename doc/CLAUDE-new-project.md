# CLAUDE.md — AI 应用开发学习项目

> 本项目遵循 `xuexulux-v3.md` 学习路线，目标是端到端掌握 AI 应用开发的六个阶段。

## 项目定位

- **身份**：Java 后端工程师的 AI 应用开发学习项目
- **主线项目**：企业内部文档问答助手
- **技术栈**：Java 17+ / Spring Boot 3 / Spring AI / PostgreSQL + pgvector
- **当前阶段**：前期规划 + 参考项目研究，**尚未进入架构设计**

## 参考项目

| 项目 | 路径 | 参考价值 |
|------|------|---------|
| JeecgBoot 3.9.2 | `C:\d\xiangmu\JeecgBoot` | AI 模块（LangChain4j + pgvector RAG）、前后端架构 |
| hawkeye-cloud | `C:\d\hawkeye-cloud` | 微服务架构、Docker Compose 编排 |

## 开发环境（已就绪）

- MySQL 8.0 → localhost:3306 (root/root)
- Redis → localhost:6379 (密码 123456)
- PostgreSQL 18.4 + pgvector 0.8.2 → localhost:5432 (postgres/postgres)
- Java 21+ (IDEA)
- Node.js v24 + pnpm v11

## 已配置的 AI 模型

| 用途 | 模型 | 环境变量 |
|------|------|---------|
| 对话 | deepseek-v4-flash | DEEPSEEKJEECG |
| 嵌入 | 智谱 embedding-3 | QRZHIPUKEY |

## 学习阶段总览

```
阶段 1：LLM 基本概念     → Java SDK 调通 API，理解 token/temperature/流式
阶段 2：Prompt Engineering → System Prompt 设计，格式约束，边界规则
阶段 2.5：上下文工程      → 多轮对话管理，Token Budget，历史裁剪
阶段 3：Tools & 结构化输出 → Function Calling，JSON Schema，SSE 流式
阶段 4：RAG              → 文档切片 → 向量化 → 检索 → 注入 Prompt
阶段 5：Agent             → ReAct 循环，工具编排，多步推理
阶段 6：Harness 工程化    → 可观测性，Eval，成本管控，安全边界
```

## 交互约定

- 用中文交流
- 每进入一个新阶段，告知 Claude 当前阶段和目标
- 遇到架构决策时，先讨论再写代码
- 代码以理解原理优先，不过早引入生产级复杂度
- JeecgBoot 仅作参考，不直接修改其代码

## 相关资料

- 学习路线详情：`xuexulux-v3.md`（在 JeecgBoot 目录或新项目目录）
- 上一会话的完整上下文：`session-transfer.md`
- JeecgBoot AI 模块入口：`EmbeddingHandler.java` / `AIChatHandler.java`
