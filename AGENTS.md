# AGENTS.md

本文件是 AI 编码助手的**唯一真相源**。`CLAUDE.md` 内容为 `@AGENTS.md`，不再单独维护。

## 项目概述

InfoPilot — AI 驱动的信息领航平台。接入多模型，通过 RAG 管道实现对文档/知识的智能问答。核心能力：文档摄入 → 向量检索 → 上下文增强 → 多模型问答。

## 技术栈

| 类别 | 技术 | 备注 |
|------|------|------|
| 语言 | Java 21 | 主技术栈 |
| 框架 | Spring Boot 4.0.6 | REST API + 依赖注入 |
| LLM 集成 | LangChain4j 1.15 | OpenAI 兼容接口，支持任意模型提供商 |
| 向量存储 | PostgreSQL 16 + PGVector | 向量检索 + 业务数据同库 |
| ORM | MyBatis-Plus 3.5.15 | 替代 JPA，内置 CRUD |
| 缓存 | Redis | 会话缓存、查询缓存 |
| 部署 | Docker Compose | Java 服务 + PostgreSQL + Redis 一键启动 |

## 项目结构

```
infopilot/
├── infopilot-server/              # 主服务模块
│   └── src/main/java/io/github/spojchil/infopilot/server/
│       ├── InfoPilotApplication.java
│       ├── chat/                 # 对话问答
│       ├── document/             # 文档摄入与处理
│       ├── retrieval/            # RAG 检索管道
│       ├── config/               # LLM / 数据源 / 异步配置
│       └── model/entity|dto|enums/
├── docs/
│   ├── project-design.md         # 完整设计（Mermaid 图 + 组件详设）
│   └── decisions.md              # 架构决策记录
├── .github/
│   ├── workflows/ci.yml
│   └── pull_request_template.md
├── docker-compose.yml
├── pom.xml
├── .gitignore
├── .gitmessage
├── AGENTS.md
├── CLAUDE.md
├── CONTRIBUTING.md
├── LICENSE
└── README.md
```

## 常用命令

```bash
# 构建（跳过测试）
./mvnw clean package -DskipTests

# 测试
./mvnw test

# 启动（开发环境，需要先启动 PostgreSQL + Redis）
docker compose up -d postgres redis
./mvnw spring-boot:run -pl infopilot-server

# 全部启动
docker compose up -d

# 代码风格
./mvnw spotless:check
./mvnw spotless:apply
```

## 代码风格

- Java 代码遵循 Google Java Style Guide（通过 Spotless + google-java-format 自动格式化）
- MyBatis-Plus 的 BaseMapper 提供内置 CRUD，无需 XML。Entity 继承 BaseEntity（createTime / updateTime / deletedAt 自动填充）
- 包结构按**职责**（chat / document / retrieval 等），而非按**层**（controller / service）
- 配置类放在 `config/`，业务代码各司其职

### 注释

注释是负债——代码改了注释没改就会说谎。写之前先问：

> **删掉这行注释，一个合格的开发者读代码会困惑吗？** 不会，就不写。

| 该写 | 不该写 |
|------|--------|
| WHY — 为什么选这个方案而非另一个 | WHAT — 方法名已经说清楚的事 |
| 反直觉的行为（"这个排序是反向的，因为……"） | 注解已表达的信息（`@Configuration` 不需要 "配置类"） |
| 非显而易见的约束（"调用方负责关闭流"） | 分隔线 `// ====` — 说明类太长，应该拆 |
| public API 的 `@throws` 和边界条件 | 每个类/方法都来一句 Javadoc 凑数 |

Javadoc 只在被调方不看源码就不知道该怎么用时才需要——IDE 自动提示里能看到的信息，不需要再写一遍。

## 测试规范

- 单元测试覆盖核心逻辑（文档解析、检索管道、问答链路）
- 集成测试使用 Testcontainers（PostgreSQL + Redis）
- LLM 调用层使用 WireMock 做 mock，不依赖外部服务

## Commit 规范

格式：`type(scope): 中文描述`

| type | 用途 |
|------|------|
| `feat` | 新功能 |
| `fix` | 修 Bug |
| `refactor` | 重构（功能不变） |
| `docs` | 文档 |
| `test` | 测试 |
| `chore` | 依赖、配置、脚本 |
| `perf` | 性能优化 |

- 一个 commit 只做一件事
- 描述用中文，说清楚做了什么

## PR 流程

- feature/fix/refactor → develop（日常功能，Squash merge）
- develop → main（集中发布，Merge commit）
- hotfix → main + cherry-pick 回 develop（紧急修复）
- 合并前自查 diff，确认无调试代码、无密钥泄露

## 个人本地规则

如果仓库根目录存在 `AGENTS.local.md`，在会话开始时读取一次，其内容作为个人规则的**扩展**（不覆盖团队规则）。
