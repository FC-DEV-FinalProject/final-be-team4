package com.fourformance.tts_vc_web.controller.tts;

import com.fourformance.tts_vc_web.common.exception.common.BusinessException;
import com.fourformance.tts_vc_web.common.exception.common.ErrorCode;
import com.fourformance.tts_vc_web.domain.entity.Member;
import com.fourformance.tts_vc_web.dto.response.DataResponseDto;
import com.fourformance.tts_vc_web.dto.response.ResponseDto;
import com.fourformance.tts_vc_web.dto.tts.*;
import com.fourformance.tts_vc_web.repository.MemberRepository;
import com.fourformance.tts_vc_web.service.common.ProjectService_team_multi;
import com.fourformance.tts_vc_web.service.tts.TTSService_team_multi;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequestMapping("/tts")
@RequiredArgsConstructor
public class TTSViewController_team_multi {

    private final TTSService_team_multi ttsService;
    private final ProjectService_team_multi projectService;
    private final MemberRepository memberRepository;


    // TTS 상태 로드 메서드
    @Operation(
            summary = "TTS 상태 로드",
            description = "TTS 프로젝트 상태를 가져옵니다.")
    @GetMapping("/{projectId}")
    public ResponseDto ttsLoad(@PathVariable("projectId") Long projectId) {

        // TTSProjectDTO와 TTSDetailDTO 리스트 가져오기
        TTSProjectDto ttsProjectDTO = ttsService.getTTSProjectDto(projectId);
        List<TTSDetailDto> ttsDetailsDTO = ttsService.getTTSDetailsDto(projectId);

        if (ttsProjectDTO == null) {
            throw new BusinessException(ErrorCode.NOT_EXISTS_PROJECT);
        }

        try {
            // DTO를 포함한 응답 객체 생성
            TTSProjectWithDetailsDto response = new TTSProjectWithDetailsDto(ttsProjectDTO, ttsDetailsDTO);
            return DataResponseDto.of(response);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SERVER_ERROR);
        }
    }

    // TTS 상태 저장 메서드
    @Operation(
            summary = "TTS 상태 저장",
            description = "TTS 프로젝트 상태를 저장합니다." )
    @PostMapping("/save")
    public RedirectView ttsSave(@RequestBody TTSSaveDto ttsSaveDto, HttpSession session) {
        try {
            // 세션에 임의의 memberId 설정
            if (session.getAttribute("memberId") == null) {
                session.setAttribute("memberId", 1L);
            }

            Long memberId = (Long) session.getAttribute("memberId");


            Long projectId;
            if (ttsSaveDto.getProjectId() == null) {
                // projectId가 null인 경우, 새 프로젝트 생성
                projectId = ttsService.createNewProject(ttsSaveDto, memberId);
            } else {
                // projectId가 존재하면, 기존 프로젝트 업데이트
                projectId = ttsService.updateProject(ttsSaveDto, memberId);
            }
            // 상태 저장 후 리다이렉트
            return new RedirectView("/tts/" + projectId); // TTS 상태 로드 URL로 리다이렉트
        } catch (BusinessException e) {
            throw e;  // 기존의 BusinessException 그대로 던짐
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SERVER_ERROR);  // 일반 예외를 서버 에러로 처리
        }
    }

    // TTS 프로젝트 삭제
    @Operation(
            summary = "TTS 프로젝트 삭제",
            description = "TTS 프로젝트와 생성된 오디오를 전부 삭제합니다." )
    @DeleteMapping("/delete/{projectId}")
    public ResponseDto deleteTTSProject(@PathVariable("projectId") Long projectId) {

        // 타입 검증
        if(projectId == null) { throw new BusinessException(ErrorCode.INVALID_PROJECT_ID); }

        // 프로젝트 삭제
        projectService.deleteTTSProject(projectId);

        // 작업 상태 : Terminated(종료)
        return DataResponseDto.of("","TTS 프로젝트가 정상적으로 삭제되었습니다.");
    }

    // TTS 선택된 모든 항목 삭제
    @Operation(
            summary = "TTS 선택된 항목 삭제",
            description = "TTS 프로젝트에서 선택된 모든 항목을 삭제합니다." )
    @DeleteMapping("/delete/details")
    public ResponseDto deleteTTSDetails(@RequestBody TTSDeleteReqDto ttsDeleteDto) {

        // TTS 선택된 항목 삭제
        if(ttsDeleteDto.getDetailIds() != null) {  projectService.deleteTTSDetail(ttsDeleteDto.getDetailIds()); }

        // 선택된 오디오 삭제
        if(ttsDeleteDto.getAudioIds() != null ) {  projectService.deleteTTSAudios(ttsDeleteDto.getAudioIds());}


        return DataResponseDto.of("","선택된 모든 항목이 정상적으로 삭제되었습니다.");
    }

}
