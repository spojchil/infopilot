package io.github.spojchil.infopilot.server.document;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 文档摄入服务。编排 Tika 解析 → 递归切分 → 向量嵌入 → PGVector 存储的完整管道。
 *
 * <p>使用手动编排而非 {@code EmbeddingStoreIngestor} 一步完成，是为了在每步之间注入自定义元数据 （文件名、片段索引、摄入时间），支撑后续检索时的来源引用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    /** 每个片段最大字符数。中文约合 250 token，在嵌入模型推荐范围内。 */
    private static final int CHUNK_SIZE = 500;

    /** 相邻片段重叠字符数，防止关键句卡在片段边界。 */
    private static final int CHUNK_OVERLAP = 50;

    /** 智谱 embedding-3 API 单次最多 64 条文本。 */
    private static final int EMBED_BATCH_SIZE = 64;

    /** 智谱 API 并发限制（V0 为 50），取 32 留安全余量。 */
    private static final int EMBED_CONCURRENCY = 32;

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    /**
     * 摄入单个文档。解析 → 切分 → 嵌入 → 存储，返回切分出的片段数。
     *
     * @param inputStream 文件输入流
     * @param fileName 原始文件名，作为元数据写入每个片段
     * @return 切分后的片段总数
     */
    public int ingest(InputStream inputStream, String fileName) {
        // 1. 解析：Tika 自动识别文件格式，提取纯文本
        Document document = new ApacheTikaDocumentParser().parse(inputStream);

        // 2. 递归切分：段落 → 句子 → 词，逐级回退
        List<TextSegment> segments =
                DocumentSplitters.recursive(CHUNK_SIZE, CHUNK_OVERLAP).split(document);

        // 3. 注入元数据：每个片段记录来源文件和位置
        String ingestedAt = Instant.now().toString();
        int totalChunks = segments.size();
        for (int i = 0; i < segments.size(); i++) {
            segments.get(i).metadata().put("fileName", fileName);
            segments.get(i).metadata().put("chunkIndex", String.valueOf(i));
            segments.get(i).metadata().put("totalChunks", String.valueOf(totalChunks));
            segments.get(i).metadata().put("ingestedAt", ingestedAt);
        }

        // 4. 并行分批向量化：每批 ≤64 条，并发 ≤32，用虚拟线程 + 信号量控速
        List<List<TextSegment>> batches = new ArrayList<>();
        for (int i = 0; i < segments.size(); i += EMBED_BATCH_SIZE) {
            int end = Math.min(i + EMBED_BATCH_SIZE, segments.size());
            batches.add(segments.subList(i, end));
        }

        List<Embedding> embeddings = new ArrayList<>(totalChunks);
        Semaphore semaphore = new Semaphore(EMBED_CONCURRENCY);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<List<Embedding>>> futures = new ArrayList<>();
            for (List<TextSegment> batch : batches) {
                futures.add(
                        executor.submit(
                                () -> {
                                    semaphore.acquire();
                                    try {
                                        return embeddingModel.embedAll(batch).content();
                                    } finally {
                                        semaphore.release();
                                    }
                                }));
            }
            for (Future<List<Embedding>> future : futures) {
                try {
                    embeddings.addAll(future.get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("嵌入任务被中断", e);
                } catch (ExecutionException e) {
                    throw new RuntimeException("嵌入任务执行失败", e.getCause());
                }
            }
        }

        // 5. 存入 PGVector
        embeddingStore.addAll(embeddings, segments);

        log.info(
                "文档摄入完成: fileName={}, chunks={}, batches={}",
                fileName,
                totalChunks,
                batches.size());
        return totalChunks;
    }
}
