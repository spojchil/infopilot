package io.github.spojchil.infopilot.server.controller;

import io.github.spojchil.infopilot.server.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@Tag(name = "对话", description = "LLM 对话接口，支持多轮上下文管理")
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @Operation(summary = "同步对话", description = "阻塞等待完整响应，自动维护会话历史")
    @GetMapping
    public String chat(
            @Parameter(description = "会话 ID，不传则每次都是新会话") @RequestParam(required = false) String sessionId,
            @Parameter(description = "用户消息") @RequestParam(defaultValue = "你好，请用一句话介绍你自己") String message) {
        return chatService.chat(sessionId, message);
    }

    @Operation(summary = "流式对话", description = "SSE 逐 token 推送，自动维护会话历史")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(
            @Parameter(description = "会话 ID，不传则每次都是新会话") @RequestParam(required = false) String sessionId,
            @Parameter(description = "用户消息") @RequestParam(defaultValue = "你好，请用一句话介绍你自己") String message) {
        return chatService.chatStream(sessionId, message);
    }
}
