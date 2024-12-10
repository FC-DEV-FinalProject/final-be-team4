package com.fourformance.tts_vc_web.service.vc;


import com.fourformance.tts_vc_web.common.constant.APIStatusConst;
import com.fourformance.tts_vc_web.common.constant.APIUnitStatusConst;
import com.fourformance.tts_vc_web.common.constant.AudioType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fourformance.tts_vc_web.common.constant.ProjectType;
import com.fourformance.tts_vc_web.common.exception.common.BusinessException;
import com.fourformance.tts_vc_web.common.exception.common.ErrorCode;
import com.fourformance.tts_vc_web.common.util.CommonFileUtils;
import com.fourformance.tts_vc_web.common.util.ElevenLabsClient_team_api;
import com.fourformance.tts_vc_web.domain.entity.*;
import com.fourformance.tts_vc_web.dto.common.VCMsgDto;
import com.fourformance.tts_vc_web.dto.vc.TrgAudioFileRequestDto;
import com.fourformance.tts_vc_web.dto.vc.VCDetailDto;
import com.fourformance.tts_vc_web.dto.vc.VCDetailResDto;
import com.fourformance.tts_vc_web.dto.vc.VCSaveRequestDto;
import com.fourformance.tts_vc_web.repository.*;
import com.fourformance.tts_vc_web.service.common.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.fourformance.tts_vc_web.service.common.TaskProducer;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VCService_TaskJob {

    private static final Logger LOGGER = Logger.getLogger(VCService_TaskJob.class.getName());

    // 의존성 주입
    private final ElevenLabsClient_team_api elevenLabsClient; // 외부 TTS 클라이언트
    private final S3Service s3Service; // S3 업로드 서비스
    private final MemberRepository memberRepository; // 멤버 리포지토리
    private final VCProjectRepository vcProjectRepository; // VC 프로젝트 리포지토리
    private final VCDetailRepository vcDetailRepository; // VC 디테일 리포지토리
    private final MemberAudioMetaRepository memberAudioMetaRepository; // 멤버 오디오 메타 리포지토리
    private final VCService_team_multi vcService; // VC 프로젝트 저장 및 처리 서비스
    private final APIStatusRepository apiStatusRepository; // API 상태 리포지토리

    private final ObjectMapper objectMapper;
    private final TaskRepository taskRepository;
    private final TaskProducer taskProducer;

    /**
     * VC 프로젝트 처리 메서드
     * 1. 멤버 검증
     * 2. VC 프로젝트 저장 및 ID 반환
     * 3. VC 디테일 정보 조회 및 처리
     * 4. 프로젝트 상태 업데이트
     */



    /**
     * 소스 파일 처리 및 변환 - 승민 변경
     */
    public VCDetailResDto processSourceFile( VCMsgDto vcMsgDto) {

        // 매칭된 파일 변환
        if (vcMsgDto.getMemberAudioId() != null) {
            // API 상태 기록 생성
            String requestPayload = String.format("Voice ID: %s, Source File: %s", vcMsgDto.getTrgVoiceId(), vcMsgDto.getLocalFileName());
            VCDetail vcDetail = vcDetailRepository.findById(vcMsgDto.getDetailId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.VC_DETAIL_NOT_FOUND));
            APIStatus apiStatus = APIStatus.createAPIStatus(vcDetail, null, requestPayload);
            apiStatusRepository.save(apiStatus);

            try {
                // 변환 작업 수행
                VCDetailResDto result = processSingleSourceFile(vcMsgDto);

                // 성공 상태 업데이트
                String responsePayload = String.format("변환 성공. 출력 URL: %s", result.getGenAudios());
                apiStatus.updateResponseInfo(responsePayload, 200, APIUnitStatusConst.SUCCESS);
                apiStatusRepository.save(apiStatus);

                return result;
            } catch (Exception e) {
                // 실패 상태 업데이트
                String responsePayload = String.format("변환 실패: %s", e.getMessage());
                apiStatus.updateResponseInfo(responsePayload, 500, APIUnitStatusConst.FAILURE);
                apiStatusRepository.save(apiStatus);
                return null;
            }
        }

        return null;
    }

    private VCDetailResDto processSingleSourceFile(VCMsgDto vcMsgDto) {

        String convertedFilePath = null;
        File convertedFile = null;
        try {
            // Step 1: 소스 파일 URL 가져오기
            String sourceFileUrl = memberAudioMetaRepository.findAudioUrlsByAudioMetaIds(
                    vcMsgDto.getMemberAudioId(),
                    AudioType.VC_SRC
            );
            LOGGER.info("[소스 파일 URL 조회] URL: " + sourceFileUrl);

            // Step 2: 변환 작업 수행
            convertedFilePath = elevenLabsClient.convertSpeechToSpeech(vcMsgDto.getTrgVoiceId(), sourceFileUrl);
            LOGGER.info("[파일 변환 완료] 파일 경로: " + convertedFilePath);

            // Step 3: 변환된 파일 읽기 및 S3 저장
            convertedFile = new File(convertedFilePath);
            byte[] convertedFileBytes = Files.readAllBytes(convertedFile.toPath());

            MultipartFile convertedMultipartFile = CommonFileUtils.convertFileToMultipartFile(convertedFile, convertedFile.getName());
            String vcOutputUrl = s3Service.uploadUnitSaveFile(convertedMultipartFile, vcMsgDto.getMemberId(), vcMsgDto.getProjectId(), vcMsgDto.getDetailId());
            LOGGER.info("[S3 업로드 완료] URL: " + vcOutputUrl);

            // Step 4: 결과 DTO 생성 및 반환
            return new VCDetailResDto(
                    vcMsgDto.getDetailId(),
                    vcMsgDto.getProjectId(),
                    true,
                    vcMsgDto.getUnitScript(),
                    sourceFileUrl,
                    List.of(vcOutputUrl)
            );
        } catch (Exception e) {
            LOGGER.severe("[소스 파일 변환 실패] " + e.getMessage());
            e.printStackTrace();
            throw new BusinessException(ErrorCode.SERVER_ERROR);
        } finally {
            // 변환 파일 삭제 로직을 finally 블록에 추가하여 항상 실행되도록 함
            if (convertedFile != null && convertedFile.exists()) {
                if (!convertedFile.delete()) {
                    LOGGER.warning("변환 파일 삭제 실패: " + convertedFile.getAbsolutePath());
                } else {
                    LOGGER.info("변환 파일 삭제 성공: " + convertedFilePath);
                }
            }
        }
    }



    /**
     * dto를 문자열 json으로 변환
     *
     * @param detailDto
     * @return
     */
    private String convertDetailToJson(VCDetailDto detailDto) {
        try {
            return objectMapper.writeValueAsString(detailDto);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.JSON_PROCESSING_ERROR);
        }
    }

    /**
     * VC 메시지 DTO 생성
     * @param detail
     * @param taskId
     * @return
     */
    private VCMsgDto createVCMsgDto(VCDetailDto detail, Long taskId, Long memberId, String trgId) {
        return VCMsgDto.builder()
                .memberId(memberId)
                .taskId(taskId)          // 생성된 Task ID
                .projectId(detail.getProjectId())
                .detailId(detail.getId()) // detailId
                .memberAudioId(detail.getMemberAudioMetaId())
                .trgVoiceId(trgId)
                .unitScript(detail.getUnitScript())
                .localFileName(detail.getLocalFileName())
                .build();
    }

    /**
     * 타겟 오디오 파일 처리 및 Voice ID 생성 ->  월 한도 제한으로 하드코딩 함
     */
    private String processTargetFiles(List<TrgAudioFileRequestDto> trgFiles, MemberAudioMeta memberAudio) {
        if (trgFiles == null || trgFiles.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_PROCESSING_ERROR);
        }
        try {
            // 하드코딩된 Voice ID 사용
            String voiceId = "DNSy71aycodz7FWtd91e"; // 테스트용 하드코딩

            // Voice ID를 MemberAudioMeta에 업데이트
            memberAudio.update(voiceId);
            memberAudioMetaRepository.save(memberAudio);

            return voiceId;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.FILE_PROCESSING_ERROR);
        }
    }
    /**
     * VC 프로젝트에 trg_voice_id 업데이트
     */
    private void updateProjectTargetVoiceId(Long projectId, String trgVoiceId) {
        VCProject vcProject = vcProjectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
        vcProject.updateTrgVoiceId(trgVoiceId);
        vcProjectRepository.save(vcProject);
    }

    //-----------------------------------------------------------------------------


    @Transactional(isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRED)
    public void enqueueVCTasks(VCSaveRequestDto vcReqDto, List<MultipartFile> files, Long memberId) {
        // Step 1: 멤버 확인
        Member member = validateMember(memberId);

        // Step 2: VC 프로젝트 저장 및 디테일 정보 조회
        VCProject vcProject = saveVCProjectAndGetDetails(vcReqDto, files, member);

        // Step 3: 타겟 Voice ID 생성 또는 조회
        String voiceId = getOrCreateVoiceId(vcReqDto, memberId);

        // Step 4: VC 디테일로 Task 생성 및 큐에 추가
        processDetailsForQueue(vcReqDto, vcProject, voiceId, memberId);
    }

    private Member validateMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private VCProject saveVCProjectAndGetDetails(VCSaveRequestDto vcReqDto, List<MultipartFile> files, Member member) {
        Long projectId = vcService.saveVCProject(vcReqDto, files, member);

        if (projectId == null) {
            throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND);
        }

        return vcProjectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VC_PROJECT_NOT_FOUND));
    }

    private String getOrCreateVoiceId(VCSaveRequestDto vcReqDto, Long memberId) {
        MemberAudioMeta memberAudio = memberAudioMetaRepository.findSelectedAudioByTypeAndMember(AudioType.VC_TRG, memberId);

        if (memberAudio != null) {
            return processTargetFiles(vcReqDto.getTrgFiles(), memberAudio);
        } else {
            return memberAudioMetaRepository.findtrgVoiceIdById(vcReqDto.getTrgFiles().get(0).getS3MemberAudioMetaId());
        }
    }

    private void processDetailsForQueue(VCSaveRequestDto vcReqDto, VCProject vcProject, String voiceId, Long memberId) {
        List<VCDetail> vcDetails = vcDetailRepository.findByVcProject_Id(vcProject.getId());

        List<VCDetailDto> vcDetailDtos = vcDetails.stream()
                .filter(vcDetail -> vcDetail.getIsChecked() && !vcDetail.getIsDeleted())
                .map(VCDetailDto::createVCDetailDtoWithLocalFileName)
                .collect(Collectors.toList());

        for (VCDetailDto detail : vcDetailDtos) {
            Task task = createAndSaveTask(vcProject, detail);
            sendTaskToQueue(detail, task, memberId, voiceId);
        }
    }

    private Task createAndSaveTask(VCProject vcProject, VCDetailDto detail) {
        String detailJson = convertDetailToJson(detail);
        Task task = Task.createTask(vcProject, ProjectType.VC, detailJson);
        taskRepository.save(task);
        return task;
    }

    private void sendTaskToQueue(VCDetailDto detail, Task task, Long memberId, String voiceId) {
        VCMsgDto message = createVCMsgDto(detail, task.getId(), memberId, voiceId);
        taskProducer.sendTask("AUDIO_VC", message);
    }

    //    @Transactional
//    public void enqueueVCTasks(VCSaveRequestDto vcReqDto, List<MultipartFile> files, Long memberId) {
//        // 프로젝트 저장, 디테일 저장
//        Member member = memberRepository.findById(memberId)
//                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
//
//        // Step 2: VC 프로젝트 저장 및 ID 반환, VcDetail 저장
//        Long projectId = vcService.saveVCProject(vcReqDto, files, member);
//        if (projectId == null) {
//            throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND);
//        }
//        // vcProject 객체 반환
//        VCProject vcProject = vcProjectRepository.findById(projectId)
//                .orElseThrow(() -> new BusinessException(ErrorCode.VC_PROJECT_NOT_FOUND));
//
//        // Step 3: 프로젝트 ID로 연관된 VC 디테일(src) 조회
//        List<VCDetail> vcDetails = vcDetailRepository.findByVcProject_Id(projectId);
//
//        // Step 4: VC 디테일 DTO 변환 및 필터링 (체크된 항목만)
//        List<VCDetailDto> vcDetailDtos = vcDetails.stream()
//                .filter(vcDetail -> vcDetail.getIsChecked() && !vcDetail.getIsDeleted())
//                .map(VCDetailDto::createVCDetailDtoWithLocalFileName)
//                .collect(Collectors.toList());
//
//        // Step 5: 저장된 타겟(TRG) 오디오 정보 가져오기
//        MemberAudioMeta memberAudio = memberAudioMetaRepository.findSelectedAudioByTypeAndMember(AudioType.VC_TRG, memberId);
//
//        String voiceId;
//        if (memberAudio != null) {
//            // Step 6: 타겟 오디오로 Voice ID 생성
//            voiceId = processTargetFiles(vcReqDto.getTrgFiles(), memberAudio);
//
//        }else{
//            voiceId = memberAudioMetaRepository.findtrgVoiceIdById(vcReqDto.getTrgFiles().get(0).getS3MemberAudioMetaId());
//        }
//
//        // Step 7: VC 프로젝트에 trg_voice_id 업데이트
//        updateProjectTargetVoiceId(projectId, voiceId);
//
//        // vcDetailDtos에서 하나씩 꺼내서 Task 생성하고 큐에 넣기
//        for (VCDetailDto detail : vcDetailDtos) {
//            // dto를 문자열 json으로 변환
//            String detailJson = convertDetailToJson(detail);
//
//            // Task 생성 및 저장
//            Task task = Task.createTask(vcProject, ProjectType.VC, detailJson);
//            taskRepository.save(task);
//
//            // 메시지 생성 및 RabbitMQ에 전송
//            VCMsgDto message = createVCMsgDto(detail, task.getId(), memberId, voiceId);
//            taskProducer.sendTask("AUDIO_VC", message);
//        }
//    }

}
