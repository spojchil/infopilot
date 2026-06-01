# CLAUDE.md — InfoPilot 企业文档智能助手

## 项目概述

基于 RAG + Agent 架构的企业级文档问答系统。上传文档，自然语言提问，带来源引用的精准回答。

- GitHub: https://github.com/spojchil/infopilot
- **当前阶段**：MVP 核心链路已验证通过，持续迭代中

## 技术栈

| 类别 | 技术 |
|------|------|
| 框架 | Spring Boot 4.0.6 · LangChain4j 1.15.0 |
| 数据库 | PostgreSQL 18.4 + PGVector · MySQL 8.0 · Redis 7.x |
| AI 模型 | DeepSeek（对话）· 智谱 Embedding-3（向量化） |
| 工具 | MyBatis-Plus 3.5.15 · Knife4j · Apache Tika |
| 前端 | 单 HTML + SSE 流式响应 |

## 环境变量

| 用途 | 模型 | 环境变量 |
|------|------|---------|
| 对话 | deepseek-v4-flash | `DEEPSEEKJEECG` |
| 嵌入 | 智谱 embedding-3 | `QRZHIPUKEY` |

## 项目结构

```
infopilot/
├── infopilot-common/          # 公共模块（实体、工具类）
├── infopilot-server/          # 服务端
│   └── config/                #   LangChain4j、Knife4j、CORS、Jackson
│   └── controller/            #   ChatController / DocController
│   └── service/               #   ChatService / DocumentService / RetrievalService
├── frontend/                  # 前端（SSE 流式对话）
├── doc/                       # 设计文档（RAG、Prompt、上下文工程等）
└── docs/                      # 面试准备 & 简历
```

## 核心链路

```
文档摄入：上传 → Tika 解析 → Recursive 切片(500) → 智谱 Embedding-3(2048维) → PGVector
问答检索：提问 → 向量化 → PGVector top-3检索(minScore过滤) → Prompt组装 → DeepSeek生成
多轮对话：Redis 滑动窗口(最近10条) + XML标签包裹 + Session TTL
安全防御：System Prompt 指令层级 + XML分隔 + 防注入/防泄露/防角色劫持
```

## 分支策略（Git Flow）

```
main                     ← 稳定版本，只从 develop 合并
  └── develop            ← 开发主线
        ├── feature/xxx  ← 功能分支
        └── fix/xxx      ← 修复分支
```

- 永远不在 main 上直接提交
- 新功能从 develop 切 feature 分支，完成后提 PR 合并
- `mvn test` 通过后再合并

## Commit 规范（Conventional Commits）

```
feat: 新功能     → feat: RAG 管道实现 — 文档摄入 + 检索问答
fix: 修 bug      → fix: 历史消息包裹 XML 标签，堵住注入漏洞
docs: 文档       → docs: 新增 Prompt Engineering 实践指南
security: 安全   → security: System Prompt 加固
refactor: 重构
chore: 工程配置
```

## 关键文档

| 文档 | 说明 |
|------|------|
| `doc/rag-design.md` | RAG 检索方案设计 |
| `doc/prompt-engineering.md` | System Prompt 设计与注入防御 |
| `doc/context-engineering.md` | 多轮对话上下文管理 |
| `doc/architecture.md` | 系统架构 |
| `doc/project-evolution.md` | 架构决策记录（ADR） |
| `doc/learning-roadmap.md` | 六阶段学习路线 |

## AI 协作约定

- 始终中文交流
- 架构决策先讨论再写代码
- 每个功能完成后自动 `git commit`（遵循 Conventional Commits）
- 不提交含 API Key 的配置文件
- 复杂任务结束后自动 commit
