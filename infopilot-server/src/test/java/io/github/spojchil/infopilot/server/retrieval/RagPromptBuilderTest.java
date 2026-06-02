package io.github.spojchil.infopilot.server.retrieval;

import static org.junit.jupiter.api.Assertions.*;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RagPromptBuilder 提示词构建单元测试")
class RagPromptBuilderTest {

    private final RagPromptBuilder builder = new RagPromptBuilder();

    @Test
    @DisplayName("buildSystemPrompt — 包含角色定义、参考文档、用户问题")
    void containsAllSections() {
        var match = createMatch("handbook.pdf", "GPA 3.5 以上", 0.95);
        var matches = List.of(match);

        String prompt = builder.buildSystemPrompt("推免生条件", matches);

        assertTrue(prompt.contains("企业文档智能助手"), "应包含角色定义");
        assertTrue(prompt.contains("参考文档"), "应包含参考文档段");
        assertTrue(prompt.contains("handbook.pdf"), "应包含来源文件名");
        assertTrue(prompt.contains("0.95"), "应包含相似度");
        assertTrue(prompt.contains("GPA 3.5 以上"), "应包含片段文本");
        assertTrue(prompt.contains("## 用户问题"), "应包含用户问题段");
        assertTrue(prompt.contains("推免生条件"), "应包含用户问题原文");
    }

    @Test
    @DisplayName("buildSystemPrompt — 多片段用分隔线区分")
    void multipleChunksSeparated() {
        var m1 = createMatch("a.pdf", "内容 A", 0.9);
        var m2 = createMatch("b.pdf", "内容 B", 0.7);
        var matches = List.of(m1, m2);

        String prompt = builder.buildSystemPrompt("测试", matches);

        assertTrue(prompt.contains("---"));
        assertTrue(prompt.contains("内容 A"));
        assertTrue(prompt.contains("内容 B"));
    }

    @Test
    @DisplayName("buildSystemPrompt — 无 fileName 时使用默认值")
    void missingFileNameUsesDefault() {
        var segment = TextSegment.from("内容");
        // 不设 fileName metadata
        var match = new EmbeddingMatch<>(0.5, "id-1", new Embedding(new float[2048]), segment);
        var matches = List.of(match);

        String prompt = builder.buildSystemPrompt("测试", matches);

        assertTrue(prompt.contains("未知文档"));
    }

    private static EmbeddingMatch<TextSegment> createMatch(
            String fileName, String text, double score) {
        var segment = TextSegment.from(text);
        segment.metadata().put("fileName", fileName);
        return new EmbeddingMatch<>(score, "id-1", new Embedding(new float[2048]), segment);
    }
}
