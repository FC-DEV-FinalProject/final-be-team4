package com.fourformance.tts_vc_web.controller.vc;

import com.fourformance.tts_vc_web.dto.response.DataResponseDto;
import com.fourformance.tts_vc_web.dto.response.ResponseDto;
import com.fourformance.tts_vc_web.dto.vc.VCSaveDto;
import com.fourformance.tts_vc_web.dto.vc.VCDetailResDto;
import com.fourformance.tts_vc_web.service.vc.VCService_team_api;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Voice Conversion API 컨트롤러
 * 소스 및 타겟 오디오 파일 처리 및 Voice ID 생성 기능을 제공합니다.
 */
@Tag(name = "Voice Conversion API", description = "Voice Conversion 관련 기능을 제공합니다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/vc")
public class VCController_team_api {

    private static final Logger LOGGER = Logger.getLogger(VCController_team_api.class.getName());
    private final VCService_team_api vcService;

    /**
     * VC 프로젝트 처리 엔드포인트
     *
     * @param vcSaveDto 프로젝트 저장 및 처리 요청 데이터
     * @param files     소스 오디오 파일 리스트
     * @param session   현재 HTTP 세션 (회원 정보 저장)
     * @return VCDetailResDto의 리스트
     */
    @Operation(summary = "VC 프로젝트 처리", description = "소스/타겟 오디오 파일 처리 및 Voice ID 생성")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "VC 프로젝트 처리 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping(value = "/process", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseDto processVCProject(
            @RequestPart("vcSaveDto") VCSaveDto vcSaveDto,
            @RequestPart("files") List<MultipartFile> files,
            HttpSession session) {
        LOGGER.info("VC 프로젝트 처리 요청 시작");

        // 세션에서 memberId 가져오기 (하드코딩된 값 사용)
        Long memberId = getOrSetMemberIdInSession(session);

        try {
            // VC 프로젝트 처리
            List<VCDetailResDto> response = vcService.processVCProject(vcSaveDto, files, memberId);

            LOGGER.info("VC 프로젝트 처리 성공");
            return DataResponseDto.of(response);

        } catch (Exception e) {
            // 예외 처리 및 로그 기록
            LOGGER.log(Level.SEVERE, "VC 프로젝트 처리 중 오류 발생", e);
            throw e; // 예외를 다시 던져 글로벌 예외 처리로 전달
        }
    }

    /**
     * 세션에서 memberId를 가져오거나 하드코딩된 값을 설정합니다.
     *
     * @param session 현재 HTTP 세션
     * @return 회원 ID
     */
    private Long getOrSetMemberIdInSession(HttpSession session) {
        // 세션에 memberId가 없으면 하드코딩된 값을 설정
        if (session.getAttribute("memberId") == null) {
            session.setAttribute("memberId", 1L); // 하드코딩된 회원 ID
            LOGGER.info("세션에 하드코딩된 memberId 설정: 1");
        }

        // 세션에서 memberId 가져오기
        Long memberId = (Long) session.getAttribute("memberId");
        LOGGER.info("현재 세션 memberId: " + memberId);
        return memberId;
    }
}
