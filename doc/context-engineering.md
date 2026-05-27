# 上下文工程设计方案

> InfoPilot 多轮对话的上下文管理策略，含业界调研、设计取舍与实现路线。

---

## 1. 问题定义

LLM 每次请求是**无状态**的。多轮对话需要通过"注入历史消息"来模拟记忆：

```
第3轮实际发送: [
  SystemMessage("你是助手"),
  UserMessage("我叫张三"),           ← 第1轮
  AiMessage("好的张三"),             ← 第1轮
  UserMessage("我喜欢吃什么"),       ← 第2轮
  AiMessage("你有什么忌口吗"),       ← 第2轮
  UserMessage("不太辣就行")           ← 当前
]
```

上下文窗口有限（DeepSeek 128K），无节制地堆积历史会触发三个问题：

| 问题 | 后果 |
|---|---|
| **Token 浪费** | 早期无关聊天占据窗口，有用信息被挤出 |
| **注意力衰减** | LLM 对中间位置内容关注度最低（Lost in the Middle） |
| **成本累积** | 每次请求为冗余历史付费，20 轮后 input token 可达首轮的 10 倍 |

### 1.1 场景差异：文档问答 vs 编程 Agent

InfoPilot 是文档问答助手，与编程 Agent（Claude Code、Cursor、Aider 等）的上下文压力来源完全不同，设计方案不能照搬。

| 维度 | 编程 Agent | InfoPilot（文档问答） |
|---|---|---|
| 典型轮次 | 20-50+ 轮（持续修复、重构） | 3-8 轮（提问→回答→追问→结束） |
| Token 增长主因 | 工具输出、源码片段、错误堆栈 | RAG 检索片段（多个文档 chunk） |
| 长会话场景 | 复杂功能开发（数小时同一任务） | 罕见——用户通常新问题开新会话 |
| 上下文瓶颈 | 对话历史积压 + 文件内容膨胀 | **文档片段注入量**（历史本身不长） |
| 压缩需求 | 强需求——不压缩很快溢出 | 弱需求——3-8 轮远不到窗口上限 |
| 核心挑战 | 如何在 100K token 中保持注意力 | **RAG 片段的质量和数量控制** |

**InfoPilot 的真正上下文挑战不在对话历史，而在 RAG 阶段。** 当检索返回 5 个文档 chunk、每个 500 token 时，Prompt 已经增加 2500 token——远超 10 轮简短对话。因此：

- **V1 对话历史用简单滑动窗口即可**，不需要摘要压缩
- **上下文预算的重点是 RAG 片段注入量**（阶段 4 时设计）
- 保留业界调研作为参考，但标注"编程 Agent 场景，InfoPilot 不完全适用"

---

## 2. 业界调研

### 2.1 Claude Code

7 层渐进式记忆架构，核心原则：**成本递增、能力递增——廉价层处理 90%，昂贵层只用于 10% 场景**。

对 InfoPilot 有参考价值的要点：

- **60% 规则**：上下文占用 60% 时主动压缩，不等 95% 被动触发——因为模型越"饱和"摘要质量越差
- **Rewind 优于纠正**：失败尝试不留在上下文里，回到失败前重新来——上下文干净比"多一轮经验"更重要
- **子代理隔离**：中间过程只返回结论给主会话，中间细节丢弃
- **CLAUDE.md 作为持久记忆**：不在对话里口头约定，写入文件跨会话生效

来源：[Claude Code — Session Management & 1M Context](https://claude.com/blog/using-claude-code-session-management-and-1m-context)

### 2.2 Cursor

动态上下文发现，核心理念：**Agent 主动拉取所需，而非 push 所有上下文**。

对 InfoPilot 有参考价值的要点：

- 长工具输出写文件而非塞上下文 —— Agent 通过 `tail`/`grep` 按需读取
- MCP 工具描述存文件夹而非注入上下文 —— 省 46.9% token
- 对话历史完整存于文件，仅语义摘要放入 prompt —— 两份独立，可互查

来源：[Cursor — Dynamic Context Discovery for Production Coding Agents](https://www.zenml.io/llmops-database/dynamic-context-discovery-for-production-coding-agents)

### 2.3 Google ADK

四层上下文架构：

| 层 | 职责 | InfoPilot 对应 |
|---|---|---|
| Working Context | 每次调用的临时 prompt | Service 层组装的消息列表 |
| Session | 结构化事件流（用户消息、工具调用、错误） | Redis 中的会话历史 |
| Memory | 跨会话可搜索的知识 | 用户偏好 KV（后阶段） |
| Artifacts | 大文件（PDF 等）用引用代替内容 | RAG 检索结果（阶段 4） |

核心思想：**Storage ≠ Presentation** —— 持久状态（Session）和每次调用的视图（Working Context）独立演进。

来源：[Google — Context-Aware Multi-Agent Framework](https://developers.googleblog.com/en/architecting-efficient-context-aware-multi-agent-framework-for-production/)

### 2.4 生产级通用模式

来自阿里、字节、Redis 官方等实践总结：

- **分层记忆**：工作记忆（Redis List 滑动窗口）→ 情节记忆（摘要 + Redis Hash）→ 语义记忆（向量库，生产阶段）
- **摘要压缩与原始消息并存**：不等摘要完全替代历史，而是作为历史的前缀/补充。丢失细节时可回查 Redis 完整记录
- **异步旁路更新**：压缩/摘要生成不阻塞主链路，走后台任务
- **强事实走 KV**：用户偏好、禁忌等事实数据存 Redis Hash 直接读写，不走不可靠的向量召回

来源：[Redis — AI Agent Memory & State Infrastructure](https://redis.io/blog/long-horizon-ai-agents-memory-state-infrastructure/)

---

## 3. 方案设计

### 3.1 整体架构

```
┌──────────────────────────────────────────────────┐
│                  ChatService                      │
│                                                   │
│  buildContext(sessionId, message)                 │
│    ├── Redis HGET summary        → 摘要(早期)     │
│    ├── Redis LRANGE history 0 -1 → 最近10条完整   │
│    ├── 组装: SystemPrompt + 摘要 + 历史 + 当前     │
│    └── Token计数 → 超70%触发裁剪                  │
│                                                   │
│  chat(sessionId, message)                         │
│    ├── buildContext()            → 消息列表        │
│    ├── chatModel.chat()          → LLM 调用       │
│    └── Redis RPUSH history       → 追加本轮        │
│                                                   │
│  compactHistory(sessionId)  [异步]                 │
│    ├── 最早10条 → LLM摘要                         │
│    ├── Redis HSET summary        → 更新摘要       │
│    └── Redis LTRIM history 0 9   → 保留最近10条   │
└──────────────────────────────────────────────────┘
```

### 3.2 数据模型（Redis）

```
session:{id}:history    → List   [msg1, msg2, ...]  最近10条完整消息
session:{id}:summary    → String  "早期对话摘要..."   压缩后的摘要文本
session:{id}:meta       → Hash   {created, updated, messageCount}
session:{id}:ttl        → 1 小时自动过期
```

### 3.3 Token Budget 策略

| 水位 | 动作 |
|---|---|
| < 50% | 正常，仅日志记录用量 |
| 50-70% | WARN 日志，不干预 |
| > 70% | 裁剪最早 5 条消息，触发异步摘要压缩 |
| > 90% | 裁剪至只保留最近 5 条，ERROR 日志 |

---

## 4. 分步落地计划

| 步骤 | 内容 | 触发阶段 | 复杂度 |
|---|---|---|---|
| **V1（当前）** | 滑动窗口 10 条 + Redis 持久化 + Token 计数日志 | 阶段 2.5 | 低 |
| **V2** | 异步摘要压缩（超 20 条触发）+ 压缩后回查能力 | 阶段 4 RAG 后 | 中 |
| **V3** | 用户偏好 KV（Redis Hash）+ 跨会话记忆 | 阶段 6 Harness | 高 |

## 5. 设计取舍

| 选择 | 理由 | 放弃的方案 |
|---|---|---|
| 手动 Service 层管理，不用 AiServices 内置 ChatMemory | 面试能讲清原理；能自定义 Token 预算和裁剪逻辑 | `MessageWindowChatMemory.withMaxMessages(10)` 一行搞定但黑盒 |
| V1 先做纯滑动窗口，不做摘要 | 快速跑通链路，摘要压缩需要多一次 LLM 调用，增加延迟和复杂度 | 一步到位的完整摘要方案 |
| Redis 而非内存 Map | 服务重启不丢对话；为后续分布式做准备 | `ConcurrentHashMap` 开发更简单但重启即失 |
| 先不做语义记忆 | 向量检索在阶段 4 才引入；用户偏好需要积累足够数据才有价值 | 一步到位的三层记忆架构 |
| 70% 主动裁剪而非等报错 | 来自 Claude Code 60% 规则的启发——模型越饱和输出越差 | 等 API 报 context length exceeded 再处理 |

---

## 6. 参考来源

- [Claude Code — Using Claude Code: Session Management and 1M Context](https://claude.com/blog/using-claude-code-session-management-and-1m-context)
- [Cursor — Dynamic Context Discovery for Production Coding Agents](https://www.zenml.io/llmops-database/dynamic-context-discovery-for-production-coding-agents)
- [Google — Context-Aware Multi-Agent Framework for Production](https://developers.googleblog.com/en/architecting-efficient-context-aware-multi-agent-framework-for-production/)
- [Redis — Long-Horizon AI Agents: Memory & State Infrastructure](https://redis.io/blog/long-horizon-ai-agents-memory-state-infrastructure/)
- [SparkCo — Agent Context Management: Ephemeral vs Durable Classification](https://sparkco.ai/blog/agent-context-management-ephemeral-vs-durable-classification-async-messaging-and-persistent-entity-graphs)
- [Agno — Automatic Tool Output Compression](https://www.agno.com/changelog/keep-runs-within-context-limits-with-automatic-tool-output-compression)
- [ContextBudget: Budget-Aware Context Management for Long-Horizon Search Agents (BAAI 2025)](https://hub.baai.ac.cn/paper/f696dd55-b8c6-4925-8938-403da07c689c)
