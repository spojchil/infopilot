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

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(LangChain4jProperties.class)
public class LangChain4jConfig {

  private final LangChain4jProperties props;

  @PostConstruct
  void init() {
    log.info(
        "LangChain4j 模型已就绪: chat={}, embedding={}",
        props.getChat().getModelName(),
        props.getEmbedding().getModelName());
  }

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
