package io.github.spojchil.infopilot.server.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlidingWindowChatMemory implements ChatMemory {

  private static final int MAX_HISTORY = 10;
  private static final int SESSION_TTL_HOURS = 1;
  private static final String KEY_PREFIX = "session:";

  private final StringRedisTemplate redis;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public List<ChatMessage> loadHistory(String sessionId) {
    if (sessionId == null) return Collections.emptyList();

    try {
      String key = KEY_PREFIX + sessionId + ":history";
      List<String> jsonList = redis.opsForList().range(key, 0, -1);
      if (jsonList == null || jsonList.isEmpty()) return Collections.emptyList();

      List<ChatMessage> messages = new ArrayList<>();
      for (String json : jsonList) {
        try {
          ChatMessageRecord record = objectMapper.readValue(json, ChatMessageRecord.class);
          messages.add(record.toChatMessage());
        } catch (JsonProcessingException e) {
          log.warn("解析历史消息失败: sessionId={}, {}", sessionId, e.getMessage());
        }
      }
      return messages;
    } catch (Exception e) {
      log.error("加载会话历史失败: sessionId={}", sessionId, e);
      return Collections.emptyList();
    }
  }

  @Override
  public void saveExchange(String sessionId, String userMessage, String assistantMessage) {
    if (sessionId == null) return;

    try {
      String key = KEY_PREFIX + sessionId + ":history";
      String userJson =
          objectMapper.writeValueAsString(new ChatMessageRecord("user", userMessage));
      String assistantJson =
          objectMapper.writeValueAsString(new ChatMessageRecord("assistant", assistantMessage));

      redis.opsForList().rightPushAll(key, userJson, assistantJson);
      redis.opsForList().trim(key, -MAX_HISTORY, -1);
      redis.expire(key, SESSION_TTL_HOURS, TimeUnit.HOURS);
    } catch (JsonProcessingException e) {
      log.error("序列化消息失败: sessionId={}", sessionId, e);
    } catch (Exception e) {
      log.error("保存会话历史失败: sessionId={}", sessionId, e);
    }
  }

  record ChatMessageRecord(String role, String content) {
    ChatMessage toChatMessage() {
      return "user".equals(role) ? UserMessage.from(content) : AiMessage.from(content);
    }
  }
}
