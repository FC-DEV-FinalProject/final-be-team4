package com.fourformance.tts_vc_web.controller.tts;

import com.fourformance.tts_vc_web.common.exception.common.BusinessException;
import com.fourformance.tts_vc_web.common.exception.common.ErrorCode;
import com.fourformance.tts_vc_web.dto.response.DataResponseDto;
import com.fourformance.tts_vc_web.dto.response.ResponseDto;
import com.fourformance.tts_vc_web.dto.tts.TTSRequestDto;
import com.fourformance.tts_vc_web.service.tts.TTSService_TaskJob;
import com.fourformance.tts_vc_web.service.tts.TTSService_team_api;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.logging.Logger;

/**
 * TTS API 컨트롤러
 * 텍스트 데이터를 음성 파일로 변환하는 API를 제공합니다.
 */
@Tag(name = "tts-controller-_team-_api", description = "텍스트를 음성 파일로 변환하는 API")
@RestController
@RequestMapping("/tts")
@RequiredArgsConstructor
public class TTSController_team_api {

    private final TTSService_TaskJob ttsServiceTaskJob;


    /**
     * 텍스트 목록을 음성 파일로 변환하는 API 엔드포인트
     *
     * @param ttsRequestDto 변환 요청 데이터 (프로젝트 정보 및 텍스트 디테일 포함)
     * @return 변환 결과 데이터 (음성 파일 URL 및 상태 정보 포함)
     */
    @Operation(summary = "TTS 배치 변환", description = "주어진 텍스트 목록을 Google TTS API를 사용하여 음성 파일로 변환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "TTS 변환 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping("/convert/batch")
    public ResponseDto convertBatchTexts(
            @RequestBody TTSRequestDto ttsRequestDto,
            HttpSession session) {

        Long memberId = (Long) session.getAttribute("memberId");

        // 세션에 memberId 값이 설정되지 않았다면 예외 처리
        if (memberId == null) {
            throw new BusinessException(ErrorCode.SESSION_MEMBER_ID_NOT_SET);
        }

        ttsServiceTaskJob.enqueueTTSBatchTasks(ttsRequestDto, memberId);

        return DataResponseDto.of("TTS 작업이 큐에 추가되었습니다.");
//        return DataResponseDto.of(Map.of(
//                "message", "TTS 작업이 큐에 추가되었습니다.",
//                "projectId", ttsRequestDto.getProjectId(),
//                "memberId", memberId
//        ));

    }

}
