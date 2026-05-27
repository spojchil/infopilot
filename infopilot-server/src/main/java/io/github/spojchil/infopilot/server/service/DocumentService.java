package io.github.spojchil.infopilot.server.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;

import static dev.langchain4j.data.document.splitter.DocumentSplitters.recursive;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    public DocumentService(EmbeddingModel embeddingModel, EmbeddingStore<TextSegment> embeddingStore) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    /**
     * 摄入文档：解析 → 切分 → 向量化 → 存储
     */
    public int ingest(InputStream inputStream, String fileName) {
        log.info("开始摄入文档: {}", fileName);

        Document document = new ApacheTikaDocumentParser().parse(inputStream);
        log.info("解析完成: {} 字符", document.text().length());

        var ingestor = EmbeddingStoreIngestor.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .documentSplitter(recursive(500, 50))
                .build();

        ingestor.ingest(document);

        int estimatedChunks = document.text().length() / 500 + 1;
        log.info("摄入完成: {}, 字符数={}, 估算片段数~{}", fileName, document.text().length(), estimatedChunks);
        return estimatedChunks;
    }
}
