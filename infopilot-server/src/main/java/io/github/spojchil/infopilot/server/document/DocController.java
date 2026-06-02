package io.github.spojchil.infopilot.server.document;

import io.github.spojchil.infopilot.server.common.response.ApiResponse;
import io.github.spojchil.infopilot.server.retrieval.RetrievalService;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 文档管理接口。上传 → 摄入 → 检索问答。 */
@RestController
@RequestMapping("/api/document")
@RequiredArgsConstructor
public class DocController {

    private final DocumentService documentService;
    private final RetrievalService retrievalService;

    /**
     * 上传文档并触发摄入管道。支持 PDF、DOCX、TXT 等常见格式，Tika 自动识别。
     *
     * @param file 上传的文件
     * @return 摄入结果
     */
    @PostMapping("/upload")
    public ApiResponse<String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        String fileName =
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown";
        int chunks = documentService.ingest(file.getInputStream(), fileName);
        return ApiResponse.success("上传完成: " + fileName + ", 切分为 " + chunks + " 个片段");
    }

    /**
     * 文档检索问答。在已摄入文档中搜索相关内容，由 LLM 生成带来源引用的回答。
     *
     * @param q 用户问题
     * @return 带来源引用的回答
     */
    @GetMapping("/search")
    public ApiResponse<String> search(@RequestParam String q) {
        return ApiResponse.success(retrievalService.search(q));
    }
}
