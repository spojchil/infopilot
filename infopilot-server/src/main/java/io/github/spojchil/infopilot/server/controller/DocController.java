package io.github.spojchil.infopilot.server.controller;

import io.github.spojchil.infopilot.server.service.DocumentService;
import io.github.spojchil.infopilot.server.service.RetrievalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Tag(name = "文档", description = "文档上传与 RAG 检索问答")
@RestController
@RequestMapping("/api/doc")
public class DocController {

    private final DocumentService documentService;
    private final RetrievalService retrievalService;

    public DocController(DocumentService documentService, RetrievalService retrievalService) {
        this.documentService = documentService;
        this.retrievalService = retrievalService;
    }

    @Operation(summary = "上传文档", description = "上传 PDF/Word/Text 等文件，自动解析、切分、向量化并存储")
    @PostMapping("/upload")
    public String upload(
            @Parameter(description = "文档文件") @RequestParam("file") MultipartFile file) throws IOException {
        int chunks = documentService.ingest(file.getInputStream(), file.getOriginalFilename());
        return "上传完成: " + file.getOriginalFilename() + ", 切分为 " + chunks + " 个片段";
    }

    @Operation(summary = "文档检索问答", description = "基于已上传文档进行 RAG 检索增强回答")
    @GetMapping("/search")
    public String search(
            @Parameter(description = "查询问题") @RequestParam String q) {
        return retrievalService.search(q);
    }
}
