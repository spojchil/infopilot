package io.github.spojchil.infopilot.server.retrieval;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import io.github.spojchil.infopilot.server.common.log.LogExecutionTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * RAG 检索服务。编排查询向量化 → PGVector 搜索 → Prompt 组装 → LLM 生成的完整管道。
 *
 * <p>搜索参数和 RAG 系统提示词分别由常量定义和 {@link RagPromptBuilder} 管理。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetrievalService {

    private static final int MAX_RESULTS = 3;
    private static final double MIN_SCORE = 0.6;

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final ChatModel chatModel;
    private final RagPromptBuilder promptBuilder;

    /**
     * 根据查询检索文档并生成回答。
     *
     * @param query 用户问题
     * @return 带来源引用的回答，无匹配时返回友好提示
     */
    @LogExecutionTime
    public String search(String query) {
        // 1. 向量化查询
        Embedding queryEmbedding = embeddingModel.embed(query).content();

        // 2. PGVector 语义搜索
        List<EmbeddingMatch<TextSegment>> matches =
                embeddingStore
                        .search(
                                EmbeddingSearchRequest.builder()
                                        .queryEmbedding(queryEmbedding)
                                        .maxResults(MAX_RESULTS)
                                        .minScore(MIN_SCORE)
                                        .build())
                        .matches();

        if (matches.isEmpty()) {
            return "未在文档中找到相关信息。请尝试上传相关文档，或换个问法。";
        }

        // 3. 组装增强 Prompt
        String systemPrompt = promptBuilder.buildSystemPrompt(query, matches);

        // 4. LLM 生成回答
        var response =
                chatModel.chat(List.of(SystemMessage.from(systemPrompt), UserMessage.from(query)));
        return response.aiMessage().text();
    }
}
