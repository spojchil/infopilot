# 项目演化日志

> 记录架构决策、技术选型的理由、踩过的坑和反思。不是日记，只写"为什么"。
> 格式参考：**日期 · 决策/发现 · 一句话理由**

---

## 2026-05-24 · 项目初始化

### 决定：参考 JeecgBoot 的 AI 模块而非基于它开发

- **理由**：JeecgBoot 是完整的企业低代码平台，作为学习项目太重。取其 AI 模块（LangChain4j + pgvector）的实现思路作为参考，独立搭建轻量学习项目。

### 决定：学习路线采用 `xuexulux-v3.md` 的六阶段分层架构

- **理由**：LLM 基础 → Prompt → 上下文工程 → Tools → RAG → Agent → Harness，每一层是下一层的前提，跳层会导致"知道概念但不明白为什么"。

### 决定：技术栈选 Spring AI + PostgreSQL/pgvector

- **理由**：Java 后端生态原生支持，PGVector 运维成本最低。Spring AI 的 `ChatModel` 统一接口让模型可替换。
- **→ 2026-05-26 更新：已切到 LangChain4j，见下文**

### 发现：DeepSeek 不提供独立的 Embedding API

- 嵌入模型需另选（当前用智谱 embedding-3）。

### 发现：RAG 纯向量检索有结构性弱点

- 数学论文测试中，检索只拿到了目录和摘要（10-15%），推导细节丢失。这引出了后续阶段需要关注混合检索（向量 + BM25）的问题。

---

## 2026-05-26 · 框架切换 + 策略调整

### 决定：AI 框架从 Spring AI 2.0 切到 LangChain4j 1.15.0

- **理由**：
  1. **市场占有率**：LangChain4j 开发者采用率 68% vs Spring AI 52%（JetBrains 2025 Q1 调研）
  2. **Agent 成熟度**：LangChain4j 有完整的 ReAct / Plan-Execute / 多 Agent 协作，Spring AI 2.0 Agent 仍较基础。简历上写 Agent 能力，LangChain4j 能撑住
  3. **国产模型支持**：LangChain4j 对 DeepSeek、智谱、通义千问等有 50+ 模型适配（含社区模块），Spring AI 仅 20+
  4. **知识可迁移**：LangChain4j 的概念体系与 Python LangChain 一脉相承，不局限于 Java 生态
  5. **有现成参考**：JeecgBoot 的 AI 模块就是 LangChain4j 实现，源码可对照学习

### 决定：放弃按部就班学习，先出 MVP 应对招聘会

- **理由**：原计划按 6 阶段逐步学习，但招聘会在即。策略调整为"先搭骨架写简历，招聘会后补细节"
- **影响**：简历上 InfoPilot 项目从"研读 JeecgBoot"改为"独立开发中"，先上线核心链路（对话 + RAG + 工具调用），高级特性标注"规划中"

### 决定：简历项目顺序调整为 AI 优先

- **理由**：投递 AI Agent 岗位，面试官第一眼应看到 InfoPilot（AI）而非 Hawkeye Cloud（微服务）
- 技术栈已同步更新：Spring AI → LangChain4j

### 依赖清单（当前版本）

| 组件 | 坐标 | 版本 |
|------|------|------|
| Spring Boot | `spring-boot-starter-parent` | 4.0.6 |
| LangChain4j BOM | `langchain4j-bom` | 1.15.0 |
| OpenAI 集成（DeepSeek） | `langchain4j-open-ai` | BOM 托管 |
| 向量存储 | `langchain4j-pgvector` | BOM 托管 |
| 智谱嵌入 | `langchain4j-community-zhipu-ai` | 1.0.0-beta5 |
| MyBatis-Plus | `mybatis-plus-spring-boot4-starter` | 3.5.15 |

### 补充：学习路线文档同步更新

- `doc/learning-roadmap.md` 中 ~17 处 Spring AI 引用全部补充了 LangChain4j 对应方案，保持两个框架并列展示，本项目选用的 LangChain4j 标注"本项目选用"
