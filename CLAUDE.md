# CLAUDE.md — InfoPilot 企业文档智能助手

## 项目定位

- Java 后端工程师的 AI 应用开发项目
- 基于 RAG + Agent 架构的企业级文档问答系统
- **当前阶段**：搭建 MVP 应对招聘会，核心链路优先，高级特性标注"规划中"

## 技术栈

- Spring Boot 4.0.6、LangChain4j 1.15.0
- PostgreSQL + PGVector（向量存储）
- DeepSeek（对话，OpenAI 兼容接口）、智谱 embedding-3（文档向量化）
- MyBatis-Plus 3.5.15、Knife4j、Redis

## 开发环境

- MySQL 8.0、Redis、PostgreSQL 18.4 + pgvector（统一 localhost，凭据见本地配置）
- Java 21+、Node.js v24、pnpm v11

## AI 模型环境变量

| 用途 | 模型 | 环境变量 |
|------|------|---------|
| 对话 | deepseek-v4-flash | `DEEPSEEKJEECG` |
| 嵌入 | 智谱 embedding-3 | `QRZHIPUKEY` |

## 关键文档

| 文档 | 说明 |
|------|------|
| `doc/learning-roadmap.md` | 六阶段学习路线 |
| `doc/project-evolution.md` | 架构决策记录 |

## 参考项目

- JeecgBoot 3.9.2 — AI 模块使用 LangChain4j + pgvector，本地克隆后可对照学习

## 交互约定

- 始终中文交流
- 架构决策先讨论再写代码
- 代码以理解原理优先，不过早引入生产级复杂度
- 复杂任务结束后自动 commit
