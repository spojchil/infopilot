package io.github.spojchil.infopilot.server.config;

import dev.langchain4j.community.model.zhipu.ZhipuAiEmbeddingModel;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * LangChain4j Bean 配置。
 *
 * <p>手动构建而非使用 Spring Boot Starter 自动配置，原因是：
 *
 * <ol>
 *   <li>统一管理——ChatModel 和 EmbeddingModel 在同一处配置，方便全局了解模型拓扑
 *   <li>完全控制——构建参数（temperature、maxTokens、超时）可精细调整，不受 Starter 约束
 *   <li>混合供应商——DeepSeek 对话走 OpenAI 兼容接口，智谱嵌入走社区模块，Starter 无法同时覆盖
 * </ol>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(LangChain4jProperties.class)
public class LangChain4jConfig {

    private final LangChain4jProperties props;

    /** 启动时打印当前激活的模型，方便排查配置错误。 */
    @PostConstruct
    void init() {
        log.info(
                "LangChain4j 模型已就绪: chat={}, embedding={}",
                props.getChat().getModelName(),
                props.getEmbedding().getModelName());
    }

    /** 同步对话模型。通过 {@code baseUrl} 切换 OpenAI / DeepSeek / 其他兼容提供商。 */
    @Bean
    public ChatModel chatModel() {
        var chat = props.getChat();
        return OpenAiChatModel.builder()
                .baseUrl(chat.getBaseUrl())
                .apiKey(chat.getApiKey())
                .modelName(chat.getModelName())
                .temperature(chat.getTemperature())
                .maxTokens(chat.getMaxTokens())
                .timeout(Duration.ofSeconds(chat.getTimeoutSeconds()))
                .logRequests(chat.isLogRequests())
                .logResponses(chat.isLogResponses())
                .build();
    }

    /** 流式对话模型，用于 SSE 逐字推送。配置与同步模型保持一致。 */
    @Bean
    public StreamingChatModel streamingChatModel() {
        var chat = props.getChat();
        return OpenAiStreamingChatModel.builder()
                .baseUrl(chat.getBaseUrl())
                .apiKey(chat.getApiKey())
                .modelName(chat.getModelName())
                .temperature(chat.getTemperature())
                .maxTokens(chat.getMaxTokens())
                .timeout(Duration.ofSeconds(chat.getTimeoutSeconds()))
                .logRequests(chat.isLogRequests())
                .logResponses(chat.isLogResponses())
                .build();
    }

    /** 嵌入模型，用于文档和查询的向量化。当前使用智谱 embedding-3（2048 维）。{@link Lazy} 注解确保 PostgreSQL 未就绪时不会阻塞应用启动。 */
    @Bean
    @Lazy
    public EmbeddingModel embeddingModel() {
        var emb = props.getEmbedding();
        return ZhipuAiEmbeddingModel.builder()
                .baseUrl(emb.getBaseUrl())
                .apiKey(emb.getApiKey())
                .model(emb.getModelName())
                .build();
    }

    /** PGVector 向量存储，连接与业务数据库相同的 PostgreSQL 实例。{@link Lazy} 原因同上。 */
    @Bean
    @Lazy
    public EmbeddingStore<TextSegment> embeddingStore() {
        var vs = props.getVectorStore();
        return PgVectorEmbeddingStore.builder()
                .host(vs.getHost())
                .port(vs.getPort())
                .database(vs.getDatabase())
                .user(vs.getUsername())
                .password(vs.getPassword())
                .table(vs.getTable())
                .dimension(vs.getDimension())
                .build();
    }
}
