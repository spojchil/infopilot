package io.github.spojchil.infopilot.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * LangChain4j 配置属性，映射 {@code application.yml} 中 {@code infopilot.*} 命名空间。
 *
 * <p>三个子配置对应三种基础设施：对话模型（OpenAI 兼容接口）、嵌入模型（智谱社区模块）、向量存储 （PGVector）。默认值覆盖本地开发场景，生产环境通过环境变量注入。
 */
@Data
@ConfigurationProperties(prefix = "infopilot")
public class LangChain4jProperties {

    private ChatConfig chat = new ChatConfig();
    private EmbeddingConfig embedding = new EmbeddingConfig();
    private VectorStoreConfig vectorStore = new VectorStoreConfig();

    @Data
    public static class ChatConfig {
        /** OpenAI 兼容 API 地址，例如 {@code https://api.deepseek.com/v1}。 */
        private String baseUrl;

        private String apiKey;

        /** 模型名称，例如 {@code deepseek-v4-flash}。 */
        private String modelName;

        /** 生成温度，0-2，越低越确定。 */
        private double temperature = 0.3;

        private int maxTokens = 16384;

        private int timeoutSeconds = 600;

        /** 是否打印请求日志，调试时开启，生产务必关闭以防 API Key 泄露。 */
        private boolean logRequests = false;

        /** 是否打印响应日志，同上。 */
        private boolean logResponses = false;

        /** SSE 连接超时秒数，应略大于 {@code timeoutSeconds}。 */
        private int sseTimeoutSeconds = 600;
    }

    @Data
    public static class EmbeddingConfig {
        private String baseUrl;

        private String apiKey;

        /** 当前使用智谱 {@code embedding-3}（2048 维）。 */
        private String modelName = "embedding-3";
    }

    @Data
    public static class VectorStoreConfig {
        /** 默认本地开发环境。 */
        private String host = "localhost";

        private int port = 5432;

        private String database = "infopilot";

        private String username = "infopilot";

        /** 生产环境须通过环境变量覆盖默认值。 */
        private String password = "infopilot";

        private String table = "embeddings";

        /** 需与嵌入模型输出维度一致。智谱 embedding-3 为 2048。 */
        private int dimension = 2048;
    }
}
