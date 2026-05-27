package io.github.spojchil.infopilot.server.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RetrievalService {

    private static final Logger log = LoggerFactory.getLogger(RetrievalService.class);

    private static final int MAX_RESULTS = 3;
    private static final double MIN_SCORE = 0.6;
    private static final String RAG_PROMPT = """
            你是 InfoPilot，一个企业文档智能助手。
            基于以下参考文档片段回答用户问题。

            规则：
            - 如果文档中有相关信息，请引用具体内容并标注来源
            - 如果文档中没有相关信息，直接说"未在文档中找到相关信息"
            - 不要编造文档中没有的内容
            - 回答应简洁、准确
            """;

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final ChatModel chatModel;

    public RetrievalService(EmbeddingModel embeddingModel,
                            EmbeddingStore<TextSegment> embeddingStore,
                            ChatModel chatModel) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.chatModel = chatModel;
    }

    /**
     * RAG 检索 — 完整链路：embed(query) → 检索 → 组装 Prompt → LLM 生成
     */
    public String search(String query) {
        log.info("RAG 检索: query={}", query);

        Embedding queryEmbedding = embeddingModel.embed(query).content();

        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(
                EmbeddingSearchRequest.builder()
                        .queryEmbedding(queryEmbedding)
                        .maxResults(MAX_RESULTS)
                        .minScore(MIN_SCORE)
                        .build()
        ).matches();

        if (matches.isEmpty()) {
            log.info("无匹配片段: query={}, minScore={}", query, MIN_SCORE);
            return "未在文档中找到相关信息。请尝试上传相关文档，或换个问法。";
        }

        log.info("检索到 {} 个片段", matches.size());
        for (var m : matches) {
            String fileName = m.embedded().metadata().getString("fileName");
            String preview = m.embedded().text().substring(0, Math.min(80, m.embedded().text().length()));
            log.debug("  score={}, source={}, text={}...", m.score(), fileName, preview);
        }

        String context = buildContext(matches);
        String augmentedPrompt = RAG_PROMPT + "\n\n" + context + "\n\n## 用户问题\n" + query;

        var response = chatModel.chat(List.of(
                SystemMessage.from(augmentedPrompt),
                UserMessage.from(query)
        ));

        return response.aiMessage().text();
    }

    private String buildContext(List<EmbeddingMatch<TextSegment>> matches) {
        StringBuilder sb = new StringBuilder("## 参考文档\n\n");
        for (var m : matches) {
            var meta = m.embedded().metadata();
            String source = meta.getString("fileName") != null ? meta.getString("fileName") : "未知文档";
            sb.append("[来源: %s, 相似度: %.2f]\n".formatted(source, m.score()));
            sb.append(m.embedded().text()).append("\n\n---\n\n");
        }
        return sb.toString();
    }
}
