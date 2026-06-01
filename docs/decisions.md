# 架构决策记录

> 只记"为什么"，不记流水账。每条标注日期。

## 2026-05-24 · 项目初始化

**背景**：需要一个能快速搭建 RAG 问答系统的启动框架。
**决策**：Java 21 + Spring Boot 4.0.6 + Maven，独立搭建而非基于现有低代码平台。
**原因**：

- Java 21 是 LTS，Spring Boot 4.x 原生支持 virtual thread，适合 I/O 密集型（LLM API 并发调用 + 文档处理）。
- 初期功能集中，单模块够用。后续如果拆前端或独立检索服务，再拆多模块。
- 放弃 Spring Cloud 微服务架构：项目本质是单点服务，不需要服务发现/网关。

**影响**：所有代码在 `infopilot-server` 下，包结构按职责拆分（chat / document / retrieval / config）。

---

## 2026-05-26 · LLM 框架选型

**背景**：需要同时调用多个不同提供商的模型（通过 OpenAI 兼容接口统一对接）。
**决策**：LangChain4j 1.15 + open-ai 兼容模块，替代 Spring AI。
**原因**：

- **市场占有率**：LangChain4j 开发者采用率 68% vs Spring AI 52%（JetBrains 2025 Q1）。
- **Agent 成熟度**：LangChain4j 有完整的 ReAct / Plan-Execute / 多 Agent 协作，Spring AI 2.0 Agent 仍较基础。
- **国产模型支持**：LangChain4j 对 DeepSeek、智谱、Qwen 等有 50+ 模型适配，Spring AI 约 20+。
- **知识可迁移**：LangChain4j 概念体系与 Python LangChain 一脉相承。

**影响**：所有 LLM 调用走统一的 `ChatLanguageModel` 接口。

---

## 2026-05-26 · MVP 优先策略

**背景**：完整实现全部规划功能需要 3-4 个月。
**决策**：先搭骨架——对话 + RAG + 工具调用——让项目尽早可运行、可演示。高级特性（混合检索、Eval、Harness）标注"规划中"。
**原因**：可运行的 MVP 比一份完美的设计文档更有说服力。

**影响**：核心链路（对话 + RAG）优先实现，高级特性在后续迭代中补全。

---

## 2026-05-27 · 上下文管理方案

**背景**：多轮对话需要上下文管理，LangChain4j 内置了 `ChatMemory` 但不够灵活。
**决策**：手动管理上下文 + Redis 持久化，不用 LangChain4j 内置 ChatMemory。
**原因**：

- **原理透明**：手动实现能理解每一步细节，遇到边界场景（超窗口、Token 预算）可以定制。
- **旁路摘要不阻塞主链路**：大厂方案（Google ADK、Claude Code）都把压缩放在异步旁路，内置 ChatMemory 的同步模式不支持。
- **文档问答场景不同**：文档问答场景 3-8 轮即结束，不同于编程 Agent 的 20-50+ 轮长会话，上下文压力小很多。

**影响**：V1 纯滑动窗口 + Token 计数，V2 再加摘要压缩。

---

## 2026-05-27 · RAG 方案设计

**背景**：需要确定文档问答的核心架构。
**决策**：递归切分 + 纯向量检索，不做混合检索和重排。
**原因**：

- 递归切分是所有来源一致推荐的最佳起始基线——够好、简单、不需要额外 LLM 调用。
- 混合检索（向量+BM25）是公认最佳实践，但 MVP 阶段先验证纯向量效果，用 Eval 数据判断是否需要引入。
- Cross-encoder 重排精度提升 10-40%，但延迟增加 9 倍。文档问答对延迟敏感，先不做。

**影响**：切分参数 `recursive(500, 50)`，检索 top 3 + minScore 0.6，后续 Eval 校准。

---

## 2026-05-27 · System Prompt 安全加固

**背景**：安全测试中，英文 prompt injection 和中文角色劫持均成功突破 V1 版 System Prompt。
**决策**：XML 标签分隔 + 指令层级声明 + 安全边界段（防注入/防泄露/防角色劫持）。
**原因**：

- XML 标签在 LLM 训练数据中大量存在，模型天然理解标签边界（Anthropic 第一最佳实践）。
- 指令层级（System > Developer > User）显式声明后，用户输入中的"忽略之前指令"类攻击成功率大幅下降。
- 防御放在代码层（用户输入包裹 `<user_message>` 标签）和 Prompt 层（System Prompt 安全边界段）两层。

**影响**：System Prompt 成为独立设计模块，每次改动需过安全测试。

---

## 2026-06-01 · 项目重建

**背景**：第一版代码停留在 legacy 分支，工程规范（PR 模板、CI/CD、代码风格）不完整。
**决策**：在 main 分支重建项目骨架，参照 proverlap 的工程规范，吸收 legacy 分支的文档资产。
**原因**：空白分支重建比在旧代码上修修补补更干净。legacy 的设计文档是真实思考过程的记录，值得保留。

**影响**：main 从零开始，包结构保持 legacy 的设计意图（chat / document / retrieval），文档从 `doc/` 迁移到 `docs/`。
