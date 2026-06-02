package io.github.spojchil.infopilot.server.document;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 文档管理接口。 */
@Slf4j
@RestController
@RequestMapping("/api/document")
@RequiredArgsConstructor
public class DocController {

    private final DocumentService documentService;

    /**
     * 上传文档并触发摄入管道。支持 PDF、DOCX、TXT 等常见格式，Tika 自动识别。
     *
     * @param file 上传的文件
     * @return 摄入结果摘要
     */
    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file) throws IOException {
        String fileName =
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown";
        int chunks = documentService.ingest(file.getInputStream(), fileName);
        return "上传完成: " + fileName + ", 切分为 " + chunks + " 个片段";
    }
}
