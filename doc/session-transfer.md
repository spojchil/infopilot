# 项目历史上下文 — 给新对话的交接文档

> 这份文档记录了上一个 Claude Code 会话中完成的所有工作。新对话开始时，将本文档粘贴给 Claude，即可无缝继续。

---

## 1. 你是谁

Java 后端工程师，正在学习 AI 应用开发。学习路线见 `xuexulux-v3.md`（6 阶段：LLM 基础 → Prompt → 上下文工程 → Tools → RAG → Agent → Harness）。

## 2. 当前进度

- **阶段**：前期规划 + 参考项目研究，尚未进入架构设计
- **学习主线项目**：企业文档问答助手（待新建）
- **参考项目**：JeecgBoot 3.9.2（已克隆到 `C:\d\xiangmu\JeecgBoot`）

## 3. 已搭建的开发环境

| 服务 | 地址             | 账号/密码 | 用途 |
|------|----------------|-----------|------|
| MySQL 8.0 | localhost:3306 | root/root | 主数据库 |
| Redis 7.x | localhost:6379 | 密码 123456 | 缓存/会话 |
| PostgreSQL 18.4 + pgvector 0.8.2 | localhost:5432 | postgres/postgres 或 20695(无密码) | 向量存储 |
| Node.js | v24.14.0       | — | 前端（Vite 项目用） |
| Java | 21             | — | 后端 |
| pnpm | v11.3.0        | — | 前端包管理 |

启动方式（已在 hawkeye-cloud 项目中有脚本）：
- MySQL：Windows 服务 `MySQL80`，`net start MySQL80`（需管理员权限）
- Redis：`C:\d\Redis-x64-3.2.100\redis-server.exe C:\d\Redis-x64-3.2.100\redis.windows.conf`
- PostgreSQL：`C:\d\changku\start-pg.bat`

## 4. 已配置的 API Key（Windows 用户环境变量）

| 用途 | 模型 | 环境变量名 | Key 前缀 |
|------|------|-----------|---------|
| AI 对话 | deepseek-v4-flash | DEEPSEEKJEECG | sk-f4a9... |
| 嵌入向量 | 智谱 embedding-3 | QRZHIPUKEY | ee63d5... |
| AI 语音 | 智谱 glm-tts | 同上 | 同上 |

> API Key 存储在 Windows 用户环境变量中（非系统变量）。读取方式：`[System.Environment]::GetEnvironmentVariable('KEY', 'User')`

## 5. JeecgBoot 参考项目关键信息

- **路径**：`C:\d\xiangmu\JeecgBoot`
- **后端**：Spring Boot 3.5.5 + MyBatis-Plus + Shiro + JWT
- **前端**：Vue3 + Vite + Ant Design Vue + TypeScript + pnpm
- **AI 核心模块**：`jeecg-boot/jeecg-boot-module/jeecg-boot-module-airag`
  - `EmbeddingHandler.java` — 向量化+检索（LangChain4j + pgvector）
  - `AIChatHandler.java` — 聊天+RAG注入
  - `AiragChatServiceImpl.java` — 对话流程编排
- **AI 外部依赖**：`jeecg-boot-starter-ai`（Jeecg 封装的模型工厂，支持 OpenAI/ZHIPU/QWEN/DEEPSEEK/OLLAMA 等）

## 6. 已验证的 RAG 链路

在 JeecgBoot 上端到端验证通过：
```
用户提问 → 智谱 embedding-3 向量化 → pgvector 检索 → 注入 prompt → DeepSeek v4-flash 回答
```

## 7. 本会话中的关键发现和讨论

### 技术发现
- DeepSeek **不提供**独立的 Embedding API（只有 Chat/Reasoning）
- 智谱 embedding-3 的 `apiHost` 需设为 `https://open.bigmodel.cn`（SDK 自动拼接 `/api/paas/v4/embeddings`）
- JeecgBoot 启动类需加 `static { System.setProperty("langchain4j.http.clientBuilderFactory", "...JdkHttpClientBuilderFactory"); }` 解决 LangChain4j HTTP 客户端冲突
- Redis 密码在 `application-dev.yml` 中两处需要修改：`data.redis.password` 和 `redisson.password`

### 架构讨论
- RAG 检索时，向量搜索偏向语义"主题"，丢失了数学论文中的推导细节（因为 LaTeX 公式向量语义弱）
- 混合检索（向量 + BM25/全文）理论上更好，但 JeecgBoot 只实现了纯向量检索
- Agent + Tool 模式比 RAG 单次检索更强（可多次迭代搜索），但代价是延迟×token×复杂度
- 数学论文案例揭示了 RAG 的经典弱点：检索到的只有目录+摘要（10-15%），LLM 凭训练知识补全了推导

### 代码修改记录
- `application-dev.yml`：Redis 密码、嵌入模型改为智谱、DeepSeek 模型名、API Key
- `JeecgSystemApplication.java`：添加 HTTP 客户端 factory 的 static 初始化块

## 8. JeecgBoot 配置要点

- `jeecg-boot/jeecg-module-system/jeecg-system-start/src/main/resources/application-dev.yml`
- 后端端口 8080，context-path: `/jeecg-boot`
- 前端端口 3100，代理到后端
- 数据库 `jeecg-boot`（134 张表），初始化 SQL: `jeecg-boot/db/jeecgboot-mysql-5.7.sql`
- AI 应用的**知识库关联**在前端「AI应用管理」→ 点击应用卡片 → 设置窗口 → 知识库区域 → `+ 添加`

---

> 新对话可以直接说：「请阅读 session-transfer.md 了解项目背景，然后帮我...」
