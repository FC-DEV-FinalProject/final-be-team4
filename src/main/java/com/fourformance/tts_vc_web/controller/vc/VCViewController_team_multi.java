package com.fourformance.tts_vc_web.controller.vc;

import com.fourformance.tts_vc_web.dto.response.DataResponseDto;
import com.fourformance.tts_vc_web.dto.response.ResponseDto;
import com.fourformance.tts_vc_web.dto.vc.VCSaveDto;
import com.fourformance.tts_vc_web.service.vc.VCService_team_multi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/vc")
@RequiredArgsConstructor
public class VCViewController_team_multi {

    @Autowired
    VCService_team_multi vcService;

    // VC 상태 로드 메서드
    @Operation(
            summary = "VC 상태 로드",
            description = "VC 프로젝트 상태를 가져옵니다." )
    @GetMapping("/{projectId}")
    public ResponseDto ttsLoad(@PathVariable Long projectId) {

        return DataResponseDto.of("");
    }

    // VC 상태 저장 메서드
    @Operation(
            summary = "VC 상태 로드",
            description = "VC 프로젝트 상태를 가져옵니다." )
    @PostMapping("/{projectId}/save")
    public ResponseDto vcSave(@PathVariable Long projectId, @RequestBody VCSaveDto vcSaveDto){
        return DataResponseDto.of("");
    }

}
