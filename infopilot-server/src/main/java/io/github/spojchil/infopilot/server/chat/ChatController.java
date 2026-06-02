package io.github.spojchil.infopilot.server.chat;

import io.github.spojchil.infopilot.server.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** 对话接口。 {@code POST /api/chat} 返回完整回复， {@code POST /api/chat/stream} 通过 SSE 逐字推送。 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /** 普通对话，等待模型完整回复后一次性返回。 */
    @PostMapping
    public ApiResponse<String> chat(@RequestBody ChatRequest request) {
        return ApiResponse.success(chatService.chat(request.sessionId(), request.message()));
    }

    /** SSE 流式对话，每个 token 作为一个 {@code data:} 事件推送。 */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody ChatRequest request) {
        return chatService.chatStream(request.sessionId(), request.message());
    }

    /** 请求体：{@code message} 必填，{@code sessionId} 可选（不传则每次独立对话）。 */
    record ChatRequest(String message, String sessionId) {}
}
