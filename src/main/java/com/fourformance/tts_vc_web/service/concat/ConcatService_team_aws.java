package com.fourformance.tts_vc_web.service.concat;

import com.fourformance.tts_vc_web.common.constant.AudioType;
import com.fourformance.tts_vc_web.common.exception.common.BusinessException;
import com.fourformance.tts_vc_web.common.exception.common.ErrorCode;
import com.fourformance.tts_vc_web.domain.entity.ConcatDetail;
import com.fourformance.tts_vc_web.domain.entity.ConcatProject;
import com.fourformance.tts_vc_web.domain.entity.Member;
import com.fourformance.tts_vc_web.dto.concat.ConcatDetailDto;
import com.fourformance.tts_vc_web.dto.concat.ConcatSaveDto;
import com.fourformance.tts_vc_web.repository.ConcatDetailRepository;
import com.fourformance.tts_vc_web.repository.ConcatProjectRepository;
import com.fourformance.tts_vc_web.repository.MemberRepository;
import com.fourformance.tts_vc_web.service.common.S3Service;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
@RequiredArgsConstructor
public class ConcatService_team_aws {

    private final ConcatProjectRepository concatProjectRepository;
    private final ConcatDetailRepository concatDetailRepository;
    private final MemberRepository memberRepository;
    private final S3Service s3Service;


    // Concat 프로젝트 생성
    @Transactional
    public Long createNewProject(ConcatSaveDto dto, Long memberId, List<MultipartFile> files) {

        validateSaveDto(dto);

        // 멤버 id로 멤버 객체 찾기
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // 프로젝트 생성
        ConcatProject concatProject = ConcatProject.createConcatProject(
                member, // 멤버 ID를 주입
                dto.getProjectName()
        );
        concatProject = concatProjectRepository.save(concatProject);

        // 디테일 생성
        if (dto.getConcatDetails() != null) {
            // 파일 인덱스를 관리
            int fileIndex = 0;

            for (ConcatDetailDto detailDto : dto.getConcatDetails()) {
                // 파일 꺼내오기
                MultipartFile file = (files != null && fileIndex < files.size()) ? files.get(fileIndex) : null;

                // 디테일 생성 로직 호출
                createConcatDetail(detailDto, concatProject, memberId, file);

                // 파일 인덱스 증가
                fileIndex++;
            }
        }

        return concatProject.getId();
    }

    // Concat 프로젝트 업데이트
    @Transactional
    public Long updateProject(ConcatSaveDto dto, Long memberId, List<MultipartFile> files) {

        validateSaveDto(dto);

        ConcatProject concatProject = concatProjectRepository.findById(dto.getProjectId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_EXISTS_PROJECT));

        // 소유권 확인
        if (!concatProject.getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.MEMBER_PROJECT_NOT_MATCH);
        }

        // 프로젝트 업데이트
        concatProject.updateConcatProject(
                dto.getProjectName(),
                dto.getGlobalFrontSilenceLength(),
                dto.getGlobalTotalSilenceLength()
        );

//        // 디테일 업데이트
//        if (dto.getConcatDetails() != null) {
//            for (ConcatDetailDto detailDto : dto.getConcatDetails()) {
//                processConcatDetail(detailDto, concatProject, memberId, files);
//            }
//        }

        return concatProject.getId();
    }

    // 디테일 생성
    private void createConcatDetail(ConcatDetailDto detailDto, ConcatProject concatProject, Long memberId,
                                    MultipartFile file) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (file == null || file.isEmpty()) {
            return;
        }

        // 버킷에 오디오 파일 저장하고 db 업데이트
        s3Service.uploadAndSaveSingleMemberFile(file, memberId, concatProject.getId(), AudioType.CONCAT, null);

        ConcatDetail concatDetail = ConcatDetail.createConcatDetail(
                concatProject,
                detailDto.getAudioSeq(),
                detailDto.isChecked(),
                detailDto.getUnitScript(),
                detailDto.getEndSilence(),
                null // 이거 나중에 멤버 오디오 메타 추가해야함
        );
        concatDetailRepository.save(concatDetail);
    }

    // 디테일 업데이트
//    private void processConcatDetail(ConcatDetailDto detailDto, ConcatProject concatProject, Long memberId,
//                                     List<MultipartFile> files) {
//        if (detailDto.getId() != null) {
//            ConcatDetail concatDetail = concatDetailRepository.findById(detailDto.getId())
//                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_EXISTS_PROJECT_DETAIL));
//
//            // 디테일이 해당 프로젝트의 디테일인지 검증
//            if (!concatDetail.getConcatProject().getId().equals(concatProject.getId())) {
//                throw new BusinessException(ErrorCode.NOT_EXISTS_PROJECT_DETAIL);
//            }
//
//            concatDetail.updateDetails(
//                    detailDto.getAudioSeq(),
//                    detailDto.isChecked(),
//                    detailDto.getUnitScript(),
//                    detailDto.getEndSilence(),
//                    false
//            );
//            concatDetailRepository.save(concatDetail);
//        } else {
//            createConcatDetail(detailDto, concatProject, memberId, files);
//        }
//    }

    // DTO 유효성 검증
    private void validateSaveDto(ConcatSaveDto dto) {
        if (dto.getProjectId() == null) {
            if (dto.getConcatDetails() != null) {
                for (ConcatDetailDto detail : dto.getConcatDetails()) {
                    if (detail.getId() != null) {
                        throw new BusinessException(ErrorCode.PROJECT_DETAIL_NOT_MATCH);
                    }
                }
            }
        }

        // 시퀀스 검증
        validateAudioSequence(dto.getConcatDetails());
    }

    // 오디오 시퀀스 유효성 검증
    private void validateAudioSequence(List<ConcatDetailDto> detailDtos) {
        Set<Integer> audioSeqSet = new HashSet<>();

        for (ConcatDetailDto detailDto : detailDtos) {
            if (!audioSeqSet.add(detailDto.getAudioSeq())) {
                throw new BusinessException(ErrorCode.DUPLICATE_UNIT_SEQUENCE);
            }
        }

        List<Integer> sequences = detailDtos.stream()
                .map(ConcatDetailDto::getAudioSeq)
                .sorted()
                .collect(Collectors.toList());

        for (int i = 0; i < sequences.size(); i++) {
            if (sequences.get(i) != i + 1) {
                throw new BusinessException(ErrorCode.INVALID_UNIT_SEQUENCE_ORDER);
            }
        }
    }

    public void fileProcess(ConcatSaveDto concatSaveDto, List<MultipartFile> files, Long memberId) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (files == null || files.isEmpty()) {
            return;
        }

        // 버킷에 오디오 파일 저장하고 db 업데이트
        s3Service.uploadAndSaveMemberFile(files, memberId, concatSaveDto.getProjectId(), AudioType.CONCAT, null);

    }
}
