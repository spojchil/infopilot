package io.github.spojchil.infopilot.server.retrieval;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("RetrievalService RAG 检索单元测试")
@ExtendWith(MockitoExtension.class)
class RetrievalServiceTest {

    @Mock private EmbeddingModel embeddingModel;

    @Mock private EmbeddingStore<TextSegment> embeddingStore;

    @Mock private ChatModel chatModel;

    private RetrievalService service;

    @BeforeEach
    void setUp() {
        var promptBuilder = new RagPromptBuilder();
        service = new RetrievalService(embeddingModel, embeddingStore, chatModel, promptBuilder);

        when(embeddingModel.embed(anyString()))
                .thenReturn(Response.from(new Embedding(new float[2048])));
    }

    @Test
    @DisplayName("search — 有匹配结果时返回 LLM 回答")
    void searchWithMatches() {
        var match = createMatch("handbook.pdf", "推免生申请条件：GPA 3.5 以上", 0.95);
        when(embeddingStore.search(any(EmbeddingSearchRequest.class)))
                .thenReturn(new EmbeddingSearchResult<>(List.of(match)));
        when(chatModel.chat(anyList()))
                .thenReturn(
                        ChatResponse.builder()
                                .aiMessage(
                                        AiMessage.from("根据文档，推免生需要 GPA 3.5 以上。"))
                                .build());

        String result = service.search("推免生申请条件");

        assertTrue(result.contains("GPA"));
        verify(embeddingModel).embed("推免生申请条件");
        verify(embeddingStore).search(any(EmbeddingSearchRequest.class));
        verify(chatModel).chat(anyList());
    }

    @Test
    @DisplayName("search — 无匹配结果时返回友好提示")
    void searchNoMatches() {
        when(embeddingStore.search(any(EmbeddingSearchRequest.class)))
                .thenReturn(new EmbeddingSearchResult<>(List.of()));

        String result = service.search("不存在的内容");

        assertEquals("未在文档中找到相关信息。请尝试上传相关文档，或换个问法。", result);
        verify(chatModel, never()).chat(anyList());
    }

    @Test
    @DisplayName("search — Prompt 包含来源文件名和相似度")
    void searchPromptContainsSource() {
        var match = createMatch("handbook.pdf", "GPA 3.5 以上", 0.95);
        when(embeddingStore.search(any(EmbeddingSearchRequest.class)))
                .thenReturn(new EmbeddingSearchResult<>(List.of(match)));
        when(chatModel.chat(anyList()))
                .thenReturn(ChatResponse.builder().aiMessage(AiMessage.from("OK")).build());

        service.search("条件");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ChatMessage>> captor = ArgumentCaptor.forClass(List.class);
        verify(chatModel).chat(captor.capture());
        String system = ((SystemMessage) captor.getValue().get(0)).text();
        assertTrue(system.contains("handbook.pdf"));
        assertTrue(system.contains("0.95"));
    }

    @Test
    @DisplayName("search — 嵌入失败时异常向上传播")
    void embeddingFailure() {
        when(embeddingModel.embed(anyString())).thenThrow(new RuntimeException("API 限流"));

        assertThrows(RuntimeException.class, () -> service.search("测试"));
    }

    @Test
    @DisplayName("search — LLM 调用失败时异常向上传播")
    void chatFailure() {
        var match = createMatch("a.pdf", "content", 0.8);
        when(embeddingStore.search(any(EmbeddingSearchRequest.class)))
                .thenReturn(new EmbeddingSearchResult<>(List.of(match)));
        when(chatModel.chat(anyList())).thenThrow(new RuntimeException("LLM 超时"));

        assertThrows(RuntimeException.class, () -> service.search("测试"));
    }

    private static EmbeddingMatch<TextSegment> createMatch(
            String fileName, String text, double score) {
        var segment = TextSegment.from(text);
        segment.metadata().put("fileName", fileName);
        return new EmbeddingMatch<>(score, "id-1", new Embedding(new float[2048]), segment);
    }
}
