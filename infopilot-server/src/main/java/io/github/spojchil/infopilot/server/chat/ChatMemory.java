package io.github.spojchil.infopilot.server.chat;

import dev.langchain4j.data.message.ChatMessage;
import java.util.List;

public interface ChatMemory {

  List<ChatMessage> loadHistory(String sessionId);

  void saveExchange(String sessionId, String userMessage, String assistantMessage);
}
