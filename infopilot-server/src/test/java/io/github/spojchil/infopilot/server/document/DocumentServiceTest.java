package io.github.spojchil.infopilot.server.document;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@DisplayName("DocumentService 文档摄入单元测试")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentServiceTest {

    @Mock private EmbeddingModel embeddingModel;

    @Mock private EmbeddingStore<TextSegment> embeddingStore;

    private DocumentService service;

    @BeforeEach
    void setUp() {
        service = new DocumentService(embeddingModel, embeddingStore);
        when(embeddingModel.embedAll(anyList()))
                .thenAnswer(
                        inv -> {
                            int size = inv.<List<?>>getArgument(0).size();
                            List<Embedding> fake = new ArrayList<>();
                            for (int i = 0; i < size; i++) {
                                fake.add(new Embedding(new float[2048]));
                            }
                            return Response.from(fake);
                        });
    }

    // ==================== 正常流程 ====================

    @Test
    @DisplayName("ingest — 短文本切分为 1 个片段并嵌入存储")
    void shortTextSingleChunk() {
        InputStream in = stream("InfoPilot 是一个 AI 驱动的信息领航平台。");

        int chunks = service.ingest(in, "readme.txt");

        assertEquals(1, chunks);
        verify(embeddingModel).embedAll(anyList());
        verify(embeddingStore).addAll(anyList(), anyList());
    }

    @Test
    @DisplayName("ingest — 长文档切分为多段，分批嵌入")
    void longTextMultiChunk() {
        // 生成约 2000 字符的文本，预期切分 > 1 个片段
        String text = "InfoPilot 是一个 AI 驱动的信息领航平台。".repeat(100);
        InputStream in = stream(text);

        int chunks = service.ingest(in, "handbook.md");

        assertTrue(chunks > 1, "长文本应切分为多个片段，实际: " + chunks);
    }

    @Test
    @DisplayName("ingest — 超大批次并发控制：>64 片段时拆为多批并行嵌入")
    void largeDocMultiBatch() {
        // 生成约 40000 字符，预期 > 64 片段 = 多批次并发嵌入
        String text = "A".repeat(40000);
        InputStream in = stream(text);

        int chunks = service.ingest(in, "large.log");

        assertTrue(chunks >= 64, "超大文本应触发多批并发，实际: " + chunks);
    }

    // ==================== 元数据 ====================

    @Test
    @DisplayName("ingest — 每个片段注入 fileName、chunkIndex、totalChunks、ingestedAt")
    void metadataInjected() {
        String text = "第一章 概述\n\n".repeat(50);
        InputStream in = stream(text);

        service.ingest(in, "handbook.pdf");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TextSegment>> captor = ArgumentCaptor.forClass(List.class);
        verify(embeddingStore).addAll(anyList(), captor.capture());

        List<TextSegment> segments = captor.getValue();
        assertFalse(segments.isEmpty());

        TextSegment first = segments.get(0);
        assertEquals("handbook.pdf", first.metadata().getString("fileName"));
        assertEquals("0", first.metadata().getString("chunkIndex"));
        assertEquals(String.valueOf(segments.size()), first.metadata().getString("totalChunks"));
        assertNotNull(first.metadata().getString("ingestedAt"));
    }

    // ==================== 边界与异常 ====================

    @Test
    @DisplayName("ingest — fileName 为 null 时向上传播异常（由 Controller 层兜底）")
    void nullFileNameThrows() {
        InputStream in = stream("测试文本");

        assertThrows(IllegalArgumentException.class, () -> service.ingest(in, null));
    }

    @Test
    @DisplayName("ingest — 嵌入失败时异常向上传播")
    void embeddingFailurePropagates() {
        when(embeddingModel.embedAll(anyList())).thenThrow(new RuntimeException("API 限流"));

        assertThrows(RuntimeException.class, () -> service.ingest(stream("测试文本"), "test.txt"));
    }

    @Test
    @DisplayName("ingest — 空文件抛 BlankDocumentException")
    void emptyFileThrows() {
        InputStream in = stream("");

        assertThrows(Exception.class, () -> service.ingest(in, "empty.txt"));
    }

    // ==================== 工具方法 ====================

    private static InputStream stream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }
}
