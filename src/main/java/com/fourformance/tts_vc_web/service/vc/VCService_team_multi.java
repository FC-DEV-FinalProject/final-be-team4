package com.fourformance.tts_vc_web.service.vc;

import com.fourformance.tts_vc_web.common.constant.APIStatusConst;
import com.fourformance.tts_vc_web.common.constant.AudioType;
import com.fourformance.tts_vc_web.common.exception.common.BusinessException;
import com.fourformance.tts_vc_web.common.exception.common.ErrorCode;
import com.fourformance.tts_vc_web.domain.entity.*;
import com.fourformance.tts_vc_web.dto.vc.*;
import com.fourformance.tts_vc_web.domain.entity.MemberAudioMeta;
import com.fourformance.tts_vc_web.domain.entity.OutputAudioMeta;
import com.fourformance.tts_vc_web.domain.entity.VCDetail;
import com.fourformance.tts_vc_web.domain.entity.VCProject;
import com.fourformance.tts_vc_web.dto.vc.*;
import com.fourformance.tts_vc_web.repository.*;
import com.fourformance.tts_vc_web.service.common.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class VCService_team_multi {
    private final MemberRepository memberRepository;
    private final VCProjectRepository vcProjectRepository;
    private final VCDetailRepository vcDetailRepository;
    private final MemberAudioMetaRepository memberAudioMetaRepository;
    private final S3Service s3Service;

    private final OutputAudioMetaRepository outputAudioMetaRepository;
    private final MemberAudioVCRepository memberAudioVCRepository;

    // VC 프로젝트 상태 조회하기
    @Transactional(readOnly = true)
    public VCProjectResDto getVCProjectDto(Long projectId) {
        // 프로젝트 조회
        VCProject vcProject = vcProjectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_EXISTS_PROJECT));

        // VCProjectDto로 변환
        VCProjectDto vcPrjDto = VCProjectDto.createVCProjectDto(vcProject);

        List<TrgAudioDto> trgAudioDtos = null; // 변경된 구조에 맞는 변수 선언
        if (vcPrjDto.getId() != null) {
            // MemberAudioMeta ID 조회
            List<Long> memberAudioIds = memberAudioVCRepository.findMemberAudioMetaByVcProjectId(vcPrjDto.getId());

            // Audio URL과 ID 조회하여 TrgAudioDto 리스트 생성
            List<MemberAudioMeta> memberAudioMetaList = memberAudioMetaRepository.findByMemberAudioIds(
                    memberAudioIds, AudioType.VC_TRG
            );

            // MemberAudioMeta를 TrgAudioDto로 변환
            trgAudioDtos = memberAudioMetaList.stream()
                    .map(meta -> new TrgAudioDto(meta.getId(), meta.getAudioUrl()))
                    .toList();
        }

        // VCProjectResDto 생성 및 반환
        VCProjectResDto resDto = new VCProjectResDto();
                        resDto.setId(vcPrjDto.getId());
                        resDto.setProjectName(vcPrjDto.getProjectName());
                        resDto.setTrgAudios(trgAudioDtos); // 새로운 구조 반영

        return resDto;
    }



    // VC 프로젝트 상세 값 조회하기
    @Transactional(readOnly = true)
    public List<VCDetailResDto> getVCDetailsDto(Long projectId) {
        List<VCDetail> vcDetails = vcDetailRepository.findByVcProject_Id(projectId);

        // isDeleted가 false인 경우에만 VCDetailResDto 목록으로 변환
        return vcDetails.stream()
                .filter(detail -> !detail.getIsDeleted()) // isDeleted가 false인 경우
                .map(this::convertToVCDetailResDto) // VCDetailResDto로 변환
                .collect(Collectors.toList());
    }

    // VCDetail 엔티티를 VCDetailResDto로 변환하는 메서드
    private VCDetailResDto convertToVCDetailResDto(VCDetail vcDetail) {

        // src 오디오 url 추가하기
        List<String> audioUrls = outputAudioMetaRepository.findAudioUrlsByVcDetail(vcDetail.getId())
                .stream() // List<OutputAudioMeta>를 Stream으로 변환
                .filter(meta -> meta.getAudioUrl() != null) // audioUrl이 null이 아닌 경우만 필터링
                .map(OutputAudioMeta::getAudioUrl) // OutputAudioMeta의 audioUrl만 추출
                .collect(Collectors.toList()); // Stream 결과를 List<String>으로 변환

        VCDetailResDto resDto = new VCDetailResDto();
                       resDto.setId(vcDetail.getId());
                       resDto.setProjectId(vcDetail.getVcProject().getId());
                       resDto.setIsChecked(vcDetail.getIsChecked());
                       resDto.setUnitScript(vcDetail.getUnitScript());
                       resDto.setGenAudios(audioUrls);
        return resDto;
    }

    // VC 프로젝트 TRG 오디오 삭제
    public void deleteTRGAudio(Long trgAudioId){

        // trgVoiceId로 MemberAudioMeta를 조회
        MemberAudioMeta audioMeta = memberAudioMetaRepository.findByIdAndAudioType(trgAudioId, AudioType.VC_TRG);

        // isDeleted = true로 변경
        if (audioMeta == null) { throw new BusinessException(ErrorCode.AUDIO_NOT_FOUND_EXCEPTION); }

        try{

            audioMeta.delete();
            memberAudioMetaRepository.save(audioMeta);

        }catch (Exception e) {
            throw new BusinessException(ErrorCode.SERVER_ERROR);
        }

    }

    // VCProject, VCDetail 저장하는 메서드
    public Long saveVCProject(VCSaveDto vcSaveDto, List<MultipartFile> localFiles, Member member) {
        // 1. VCProject 생성/업데이트
        VCProject vcProject = vcSaveDto.getProjectId() == null
                ? createNewVCProject(vcSaveDto, member)
                : updateExistingVCProject(vcSaveDto); // member는 안넘겨도 될 것 같음

        // 2. 타겟 파일 처리
        processFiles(vcSaveDto.getTrgFiles(), localFiles, vcProject, AudioType.VC_TRG);

        // 3. 소스 파일 처리
        processFiles(vcSaveDto.getSrcFiles(), localFiles, vcProject, AudioType.VC_SRC);

        return vcProject.getId();
    }

    // VCProject 생성, 저장
    private VCProject createNewVCProject(VCSaveDto vcSaveDto, Member member) {

        VCProject vcProject = VCProject.createVCProject(member, vcSaveDto.getProjectName());
        vcProjectRepository.save(vcProject);
        return vcProject;
    }

    //VCProject 업데이트
    private VCProject updateExistingVCProject(VCSaveDto vcSaveDto) {
        VCProject vcProject = vcProjectRepository.findById(vcSaveDto.getProjectId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_EXISTS_PROJECT));

        vcProject.updateVCProject(vcSaveDto.getProjectName(), null);

        return vcProject;
    }

    private void processFiles(List<AudioFileDto> fileDtos, List<MultipartFile> files, VCProject vcProject, AudioType audioType) {

        if (fileDtos == null || fileDtos.isEmpty()) { // 업로드 된 파일이 없을 때
            return;
        }

        for (AudioFileDto fileDto : fileDtos) {
            MemberAudioMeta audioMeta = null;

            if (fileDto.getS3MemberAudioMetaId() != null) {
                // S3 파일 처리
                audioMeta = memberAudioMetaRepository.findById(fileDto.getS3MemberAudioMetaId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_EXISTS_AUDIO));
            } else if (fileDto.getLocalFileName() != null) {
                // 로컬 파일 처리
                MultipartFile localFile = findMultipartFileByName(files, fileDto.getLocalFileName());
                List<String> uploadedUrls = s3Service.uploadAndSaveMemberFile(
                        List.of(localFile), vcProject.getMember().getId(), vcProject.getId(), audioType); // voiceId를 받아오는 api 호출해서 null을 반환값으로 채우면 될 듯
                String fileUrl = uploadedUrls.get(0);

                audioMeta = memberAudioMetaRepository.findFirstByAudioUrl(fileUrl)
                        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_EXISTS_AUDIO));
            }

            if (audioMeta == null) {
                throw new BusinessException(ErrorCode.INVALID_PROJECT_DATA);
            }

            if (audioType == AudioType.VC_TRG) {
                // 타겟 파일은 VCProject에 저장
                vcProject.updateVCProject(vcProject.getProjectName(), audioMeta);
            } else {
                // 소스 파일은 VCDetail에 저장
                VCDetail vcDetail = VCDetail.createVCDetail(vcProject, audioMeta);
                vcDetail.updateDetails(fileDto.getIsChecked(), fileDto.getUnitScript());
                memberAudioMetaRepository.selectAudio(audioMeta.getId(), AudioType.VC_TRG); // 방금 저장한 trg select 표시
                vcDetailRepository.save(vcDetail);
            }
        }
    }
    private MultipartFile findMultipartFileByName(List<MultipartFile> files, String localFileName) {
        return files.stream()
                .filter(file -> file.getOriginalFilename().equals(localFileName))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_PROCESSING_ERROR));
    }

}
