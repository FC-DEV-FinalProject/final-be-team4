package com.fourformance.tts_vc_web.service.common;

import com.fourformance.tts_vc_web.common.exception.common.BusinessException;
import com.fourformance.tts_vc_web.common.exception.common.ErrorCode;
import com.fourformance.tts_vc_web.repository.ProjectRepository;
import com.fourformance.tts_vc_web.repository.common.EmitterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.logging.Logger;

@Service
@RequiredArgsConstructor
public class SseEmitterService {

    private static final Long DEFAULT_TIMEOUT = 30 * 60 * 1000L; // 30분
    private final EmitterRepository emitterRepository; // Emitter 저장소 인터페이스
    private final Logger log = Logger.getLogger(SseEmitterService.class.getName());

    /**
     * 클라이언트의 SSE 구독 요청을 처리
     */
    public SseEmitter subscribe(Long clientId) {
        log.info("Subscribing client with ID: " + clientId);
        SseEmitter emitter = createEmitter(clientId);

        try {
            // 초기 메시지 전송
            emitter.send(SseEmitter.event()
                    .id(String.valueOf(clientId))
                    .name("connection")
                    .data("Connection established for clientId: " + clientId));
            log.info("SSE subscription completed for clientId: " + clientId);
        } catch (IOException exception) {
            log.warning("Failed to send initial message to clientId: " + clientId);
            emitterRepository.deleteById(clientId); // 초기 전송 실패 시 Emitter 삭제
        }

        return emitter;
    }

    /**
     * SSE Emitter 생성 및 저장
     */
    private SseEmitter createEmitter(Long clientId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        emitterRepository.save(clientId, emitter);
        log.info("Created SSE Emitter for clientId: " + clientId);

        // Emitter 완료/타임아웃 처리
        emitter.onCompletion(() -> {
            log.info("SSE Emitter completed for clientId: " + clientId);
            emitterRepository.deleteById(clientId);
        });
        emitter.onTimeout(() -> {
            log.info("SSE Emitter timed out for clientId: " + clientId);
            emitterRepository.deleteById(clientId);
        });
        emitter.onError(e -> {
            log.warning("Error in SSE Emitter for clientId: " + clientId + ", removing emitter.");
            emitterRepository.deleteById(clientId);
        });

        return emitter;
    }

    /**
     * 클라이언트에게 데이터 전송
     */
    public void sendToClient(Long clientId, Object data) {
        if (clientId == null) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }
        if (data == null) {
            data = "No data available";
        }

        SseEmitter emitter = emitterRepository.get(clientId);
        if (emitter != null) {
            try {
                log.info("Sending data to clientId: " + clientId);
                emitter.send(SseEmitter.event()
                        .id(String.valueOf(clientId))
                        .name("taskUpdate")
                        .data(data));
            } catch (IOException exception) {
                log.warning("Failed to send data to clientId: " + clientId + ", removing emitter.");
                emitterRepository.deleteById(clientId);
                emitter.completeWithError(exception);
            }
        } else {
            log.warning("No active SSE connection for clientId: " + clientId);
        }
    }

    /**
     * 특정 클라이언트 연결 강제 종료
     */
    public void disconnect(Long clientId) {
        SseEmitter emitter = emitterRepository.get(clientId);
        if (emitter != null) {
            emitter.complete();
            emitterRepository.deleteById(clientId);
            log.info("Disconnected clientId: " + clientId);
        } else {
            log.warning("No active SSE connection for clientId: " + clientId);
        }
    }

    /**
     * 모든 클라이언트 연결 강제 종료
     */
    public void disconnectAll() {
        emitterRepository.deleteAll();
        log.info("All SSE connections have been disconnected");
    }

    /**
     * 현재 활성화된 연결 수 반환
     */
    public int getActiveConnectionsCount() {
        return emitterRepository.count();
    }
}
