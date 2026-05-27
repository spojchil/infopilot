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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LangChain4jConfig {

    // ==================== DeepSeek 对话模型 ====================

    @Value("${infopilot.deepseek.api-key}")
    private String deepseekApiKey;

    @Value("${infopilot.deepseek.base-url}")
    private String deepseekBaseUrl;

    @Value("${infopilot.deepseek.model-name}")
    private String deepseekModelName;

    @Value("${infopilot.deepseek.temperature:0.3}")
    private double temperature;

    @Bean
    public ChatModel chatModel() {
        return OpenAiChatModel.builder()
                .apiKey(deepseekApiKey)
                .baseUrl(deepseekBaseUrl)
                .modelName(deepseekModelName)
                .temperature(temperature)
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    @Bean
    public StreamingChatModel streamingChatModel() {
        return OpenAiStreamingChatModel.builder()
                .apiKey(deepseekApiKey)
                .baseUrl(deepseekBaseUrl)
                .modelName(deepseekModelName)
                .temperature(temperature)
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    // ==================== 智谱 Embedding 模型 ====================

    @Value("${infopilot.embedding.api-key}")
    private String embeddingApiKey;

    @Value("${infopilot.embedding.model-name:embedding-3}")
    private String embeddingModelName;

    @Bean
    public EmbeddingModel embeddingModel() {
        return ZhipuAiEmbeddingModel.builder()
                .apiKey(embeddingApiKey)
                .model(embeddingModelName)
                .build();
    }

    // ==================== PGVector 向量存储 ====================

    @Value("${infopilot.vector-store.host}")
    private String vectorStoreHost;

    @Value("${infopilot.vector-store.port}")
    private int vectorStorePort;

    @Value("${infopilot.vector-store.database}")
    private String vectorStoreDatabase;

    @Value("${infopilot.vector-store.username}")
    private String vectorStoreUsername;

    @Value("${infopilot.vector-store.password}")
    private String vectorStorePassword;

    @Value("${infopilot.vector-store.table:embeddings}")
    private String vectorStoreTable;

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return PgVectorEmbeddingStore.builder()
                .host(vectorStoreHost)
                .port(vectorStorePort)
                .database(vectorStoreDatabase)
                .user(vectorStoreUsername)
                .password(vectorStorePassword)
                .table(vectorStoreTable)
                .build();
    }
}
