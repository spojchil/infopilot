package io.github.spojchil.infopilot.server.chat;

import dev.langchain4j.data.message.ChatMessage;
import java.util.List;

/** 会话记忆抽象。解耦 ChatService 与具体的记忆存储策略，方便后续替换为摘要压缩或语义检索等实现。 */
public interface ChatMemory {

    /** 加载会话的历史消息，按时间顺序排列。返回空列表而非 {@code null}。 */
    List<ChatMessage> loadHistory(String sessionId);

    /** 保存一轮对话（用户消息 + AI 回复）。实现类负责持久化与容量控制。 */
    void saveExchange(String sessionId, String userMessage, String assistantMessage);
}
