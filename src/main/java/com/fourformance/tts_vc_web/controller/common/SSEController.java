package com.fourformance.tts_vc_web.controller.common;

import com.fourformance.tts_vc_web.service.common.SseEmitterService;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sse")
public class SSEController {

    private final SseEmitterService sseEmitterService;

    /**
     * 클라이언트의 SSE 구독 요청을 처리 (새 연결 생성)
     */
    @GetMapping(value = "/{clientId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable Long clientId) {
        return sseEmitterService.subscribe(clientId);
    }

    /**
     * 서버에서 특정 클라이언트로 데이터 전송 (디버그/테스트용)
     */
    @PostMapping("/{clientId}/messages")
    public ResponseEntity<String> sendMessageToClient(@PathVariable Long clientId, @RequestBody String message) {
        sseEmitterService.sendToClient(clientId, message);
        return ResponseEntity.ok("Message sent to clientId: " + clientId);
    }

    /**
     * 특정 클라이언트 연결 종료
     */
    @DeleteMapping("/{clientId}")
    public ResponseEntity<String> disconnect(@PathVariable Long clientId) {
        sseEmitterService.disconnect(clientId);
        return ResponseEntity.ok("Disconnected clientId: " + clientId);
    }

    /**
     * 모든 연결 종료
     */
    @DeleteMapping
    public ResponseEntity<String> disconnectAll() {
        sseEmitterService.disconnectAll();
        return ResponseEntity.ok("All clients disconnected");
    }

    /**
     * 활성화된 연결 수 조회
     */
    @GetMapping("/count")
    public ResponseEntity<Integer> activeConnections() {
        return ResponseEntity.ok(sseEmitterService.getActiveConnectionsCount());
    }

}