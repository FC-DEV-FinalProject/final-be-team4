package com.fourformance.tts_vc_web.controller.workspace;

import com.fourformance.tts_vc_web.dto.response.DataResponseDto;
import com.fourformance.tts_vc_web.dto.response.ResponseDto;
import com.fourformance.tts_vc_web.dto.workspace.ExportListDto2;
import com.fourformance.tts_vc_web.dto.workspace.ProjectListDto;
import com.fourformance.tts_vc_web.dto.workspace.RecentExportDto;
import com.fourformance.tts_vc_web.dto.workspace.RecentProjectDto;
import com.fourformance.tts_vc_web.repository.OutputAudioMetaRepository;
import com.fourformance.tts_vc_web.repository.ProjectRepository;
import com.fourformance.tts_vc_web.service.common.ProjectService_team_aws;
import com.fourformance.tts_vc_web.service.workspace.WorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/workspace")
@RequiredArgsConstructor
public class WorkSpaceController {

    private final WorkspaceService workspaceService;
    private final ProjectService_team_aws projectService;
    private final ProjectRepository projectRepository;
    private final OutputAudioMetaRepository outputAudioMetaRepository;

    // 최근 5개의 프로젝트를 조회하는 api
    @Operation(summary = "최근 프로젝트 5개 조회", description = "해당 유저의 최근 프로젝트 5개를 조회합니다. <br>"
            + "유저의 id는 세션에서 가져옵니다. (회원 기능 구현 전 임시 하드코딩으로 멤버 id가 1인 유저의 최근 프로젝트 목록을 가져옵니다.")
    @GetMapping("/project-list")
    public ResponseDto getRecentProjects(HttpSession session) {

//        Long memberId = (Long) session.getAttribute("member_id");
        Long memberId = 1L; // 임시 하드코딩

        // Service에서 처리된 응답 그대로 반환
        List<RecentProjectDto> projects = workspaceService.getRecentProjects(memberId);
        return DataResponseDto.of(projects);
    }

    @GetMapping("/export-list")
    public ResponseDto getRecentExports(HttpSession session) {
        Long memberId = 1L; // 임시 하드코딩 (세션 구현 후 교체)
        List<RecentExportDto> exports = workspaceService.getRecentExports(memberId);
        return DataResponseDto.of(exports);
    }

    @GetMapping("/api/v1/projects")
    public ResponseEntity<List<ProjectListDto>> getProjects(
            @RequestParam(name = "keyword", required = false) String keyword,
            HttpSession session) {
//        Long memberId = (Long) session.getAttribute("memberId");
        Long memberId = 1L; // 개발단계 임시 하드코딩
        List<ProjectListDto> projects = projectRepository.findProjectsBySearchCriteria(memberId, keyword);
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/projects")
    public ResponseDto getProjects(
            @RequestParam(name = "keyword", required = false) String keyword,
            @PageableDefault(size = 10) Pageable pageable,
            HttpSession session
    ) {
        //        Long memberId = (Long) session.getAttribute("memberId");
        Long memberId = 1L; // 개발단계 임시 하드코딩

        Page<ProjectListDto> projects = projectRepository.findProjectsBySearchCriteria(memberId, keyword, pageable);
        return DataResponseDto.of(projects);
    }

    @GetMapping("/api/v1/exports")
    public ResponseDto getExports(
            @RequestParam(name = "keyword", required = false) String keyword,
            HttpSession session
    ) {
        //        Long memberId = (Long) session.getAttribute("memberId");
        Long memberId = 1L; // 개발단계 임시 하드코딩

        List<ExportListDto2> exports = outputAudioMetaRepository.findExportHistoryBySearchCriteria2(memberId, keyword);
        return DataResponseDto.of(exports);
    }
}