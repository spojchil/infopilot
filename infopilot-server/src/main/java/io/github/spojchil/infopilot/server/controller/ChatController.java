package io.github.spojchil.infopilot.server.controller;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.List;

@Tag(name = "对话", description = "LLM 对话接口，支持同步和流式")
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatModel chatModel;
    private final StreamingChatModel streamingChatModel;

    public ChatController(ChatModel chatModel, StreamingChatModel streamingChatModel) {
        this.chatModel = chatModel;
        this.streamingChatModel = streamingChatModel;
    }

    @Operation(summary = "同步对话", description = "阻塞等待完整响应后返回，适合测试和简单调用")
    @GetMapping
    public String chat(
            @Parameter(description = "用户消息") @RequestParam(defaultValue = "你好，请用一句话介绍你自己") String message) {
        ChatResponse response = chatModel.chat(UserMessage.from(message));
        return response.aiMessage().text();
    }

    @Operation(summary = "流式对话", description = "SSE 逐 token 推送，首 Token 延迟 200ms 以内")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(
            @Parameter(description = "用户消息") @RequestParam(defaultValue = "你好，请用一句话介绍你自己") String message) {
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();

        streamingChatModel.chat(List.of(UserMessage.from(message)), new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                sink.tryEmitNext(partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                sink.tryEmitComplete();
            }

            @Override
            public void onError(Throwable error) {
                sink.tryEmitError(error);
            }
        });

        return sink.asFlux();
    }
}
