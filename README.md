# InfoPilot

AI 驱动的信息领航平台 —— 文档摄入、向量检索、智能问答。

[![Java](https://img.shields.io/badge/Java-21-blue)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-green)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

## 快速开始

```bash
# 1. 克隆
git clone https://github.com/spojchil/infopilot.git
cd infopilot

# 2. 配置环境变量
cp .env.example .env
# 编辑 .env：填入模型 API Key

# 3. 一键启动
docker compose up -d
```

## 核心特性

- **多模型接入** — 统一 OpenAI 兼容接口，支持 DeepSeek / 智谱 / Qwen 等任意模型
- **RAG 管道** — 文档摄入 → 向量化 → 语义检索 → 上下文增强问答
- **多格式文档** — 支持 PDF、DOCX、Markdown、纯文本等常见格式
- **多轮对话** — 上下文感知的对话管理，支持追问和澄清

## 技术栈

Java 21 · Spring Boot 4.0.6 · LangChain4j 1.15 · PostgreSQL 16 + PGVector · Redis · Docker Compose

## 项目结构

```
├── infopilot-server/              # 主服务模块
│   └── src/main/java/.../infopilot/server/
│       ├── chat/                 # 对话问答
│       ├── document/             # 文档摄入与处理
│       ├── retrieval/            # RAG 检索管道
│       ├── config/               # LLM / 数据源配置
│       └── model/                # DTO / 实体 / 枚举
├── docs/
│   ├── architecture.md           # 系统架构设计
│   ├── project-design.md         # 项目设计
│   ├── rag-design.md             # RAG 检索方案
│   └── decisions.md              # 架构决策记录
├── docker-compose.yml
└── README.md
```

## 文档

| 文档 | 说明 |
|------|------|
| [架构设计](docs/architecture.md) | 系统分层、模块结构、请求流程 |
| [RAG 方案](docs/rag-design.md) | 文档摄入、检索管道、Eval 策略 |
| [项目设计](docs/project-design.md) | 核心链路与 API 设计 |
| [架构决策](docs/decisions.md) | 关键选型理由与演化记录 |
| [贡献指南](CONTRIBUTING.md) | 分支策略、Commit 规范、PR 流程 |

## 许可证

[MIT License](LICENSE)
