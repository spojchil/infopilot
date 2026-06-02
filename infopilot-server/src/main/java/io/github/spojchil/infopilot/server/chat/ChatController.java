package io.github.spojchil.infopilot.server.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

  private final ChatService chatService;

  @PostMapping(produces = MediaType.TEXT_PLAIN_VALUE)
  public String chat(@RequestBody ChatRequest request) {
    return chatService.chat(request.sessionId(), request.message());
  }

  @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter chatStream(@RequestBody ChatRequest request) {
    return chatService.chatStream(request.sessionId(), request.message());
  }

  record ChatRequest(String message, String sessionId) {}
}
