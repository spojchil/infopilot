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

/**
 * {@link ChatMemory} 的 Redis 滑动窗口实现。
 *
 * <p>每条会话在 Redis 中存为一个 List（键 {@code session:{id}:history}），值是 JSON 序列化的 {@link
 * ChatMessageRecord}。通过 {@code trim} 保持窗口大小，通过 {@code expire} 设置 TTL 防止 内存泄漏。Redis
 * 异常被捕获并降级——加载失败返回空列表，保存失败仅记录日志，不影响主对话流程。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SlidingWindowChatMemory implements ChatMemory {

    private static final int MAX_HISTORY = 10;

    private static final int SESSION_TTL_HOURS = 1;

    private static final String KEY_PREFIX = "session:";

    private final StringRedisTemplate redis;

    /**
     * 直接创建而非注入 Bean，因为 Spring Boot 4.0.6 不再自动配置 {@link ObjectMapper} Bean（Jackson 3
     * 迁移），这里只需默认配置即可满足 JSON 序列化需求。
     */
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
                    ChatMessageRecord record =
                            objectMapper.readValue(json, ChatMessageRecord.class);
                    messages.add(record.toChatMessage());
                } catch (JsonProcessingException e) {
                    log.warn("解析历史消息失败: sessionId={}, {}", sessionId, e.getMessage());
                }
            }
            return messages;
        } catch (Exception e) {
            // Redis 不可用时降级：返回空历史，对话继续进行，只是丢失上下文
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
                    objectMapper.writeValueAsString(
                            new ChatMessageRecord("assistant", assistantMessage));

            // rightPushAll 一次性写入两条消息，trim 裁剪超出窗口的旧消息
            redis.opsForList().rightPushAll(key, userJson, assistantJson);
            redis.opsForList().trim(key, -MAX_HISTORY, -1);
            redis.expire(key, SESSION_TTL_HOURS, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            log.error("序列化消息失败: sessionId={}", sessionId, e);
        } catch (Exception e) {
            // Redis 不可用时降级：丢失本轮历史，但不阻断对话
            log.error("保存会话历史失败: sessionId={}", sessionId, e);
        }
    }

    /** Redis JSON 序列化载体。{@link #toChatMessage()} 根据 role 字段还原为 LangChain4j 的消息类型。 */
    record ChatMessageRecord(String role, String content) {
        ChatMessage toChatMessage() {
            return "user".equals(role) ? UserMessage.from(content) : AiMessage.from(content);
        }
    }
}
