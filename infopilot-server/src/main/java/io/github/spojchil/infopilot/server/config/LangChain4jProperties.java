package io.github.spojchil.infopilot.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "infopilot")
public class LangChain4jProperties {

    private ChatConfig chat = new ChatConfig();
    private EmbeddingConfig embedding = new EmbeddingConfig();
    private VectorStoreConfig vectorStore = new VectorStoreConfig();

    @Data
    public static class ChatConfig {
        private String baseUrl;
        private String apiKey;
        private String modelName;
        private double temperature = 0.3;
        private int maxTokens = 16384;
        private int timeoutSeconds = 600;
    }

    @Data
    public static class EmbeddingConfig {
        private String baseUrl;
        private String apiKey;
        private String modelName = "embedding-3";
    }

    @Data
    public static class VectorStoreConfig {
        private String host = "localhost";
        private int port = 5432;
        private String database = "infopilot";
        private String username = "infopilot";
        private String password = "infopilot";
        private String table = "embeddings";
        private int dimension = 2048;
    }
}
