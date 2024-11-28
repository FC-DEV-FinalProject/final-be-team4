package com.fourformance.tts_vc_web.service.concat;

import com.fourformance.tts_vc_web.common.constant.AudioType;
import com.fourformance.tts_vc_web.common.constant.ConcatStatusConst;
import com.fourformance.tts_vc_web.common.exception.common.BusinessException;
import com.fourformance.tts_vc_web.common.exception.common.ErrorCode;
import com.fourformance.tts_vc_web.domain.entity.*;
import com.fourformance.tts_vc_web.dto.concat.*;
import com.fourformance.tts_vc_web.repository.ConcatDetailRepository;
import com.fourformance.tts_vc_web.repository.ConcatProjectRepository;
import com.fourformance.tts_vc_web.repository.ConcatStatusHistoryRepository;
import com.fourformance.tts_vc_web.repository.MemberRepository;
import com.fourformance.tts_vc_web.service.common.S3Service;
import lombok.RequiredArgsConstructor;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFprobe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ConcatService_team_api {

    // 서비스 의존성 주입
    private final S3Service s3Service; // S3 연동 서비스
    private final AudioProcessingService audioProcessingService; // 오디오 처리 서비스
    private final ConcatProjectRepository concatProjectRepository; // 프로젝트 관련 저장소
    private final ConcatStatusHistoryRepository concatStatusHistoryRepository; // 프로젝트 이력 관련 저장소
    private final ConcatDetailRepository concatDetailRepository; // 디테일 관련 저장소
    private final MemberRepository memberRepository; // 멤버 관련 저장소

    private static final Logger LOGGER = Logger.getLogger(ConcatService_team_api.class.getName());

    @Value("${upload.dir}")
    private String uploadDir;

    @Value("${ffmpeg.path}")
    private String ffmpegPath;

    /**
     * 서비스 초기화 메서드: 업로드 디렉토리를 생성하고 FFmpeg 경로를 검증합니다.
     */
    @PostConstruct
    public void initialize() {
        // 업로드 디렉토리 생성
        File uploadFolder = new File(uploadDir);
        if (!uploadFolder.exists()) {
            if (!uploadFolder.mkdirs()) {
                throw new RuntimeException("업로드 디렉토리를 생성할 수 없습니다: " + uploadDir);
            }
        }
        LOGGER.info("업로드 디렉토리가 설정되었습니다: " + uploadDir);

        // FFmpeg 경로 검증
        File ffmpegFile = new File(ffmpegPath);
        if (!ffmpegFile.exists() || !ffmpegFile.canExecute()) {
            throw new RuntimeException("FFmpeg 실행 파일을 찾을 수 없거나 실행 권한이 없습니다: " + ffmpegPath);
        }
        LOGGER.info("FFmpeg 경로가 설정되었습니다: " + ffmpegPath);

        // FFmpeg 인스턴스 초기화 (예시)
        setupFFmpeg();
    }

    /**
     * FFmpeg 인스턴스 생성 (예시)
     */
    private void setupFFmpeg() {
        try {
            FFmpeg ffmpeg = new FFmpeg(ffmpegPath);
            FFprobe ffprobe = new FFprobe(ffmpegPath.replace("ffmpeg", "ffprobe"));
            // FFmpeg 및 FFprobe를 사용하는 로직 추가
        } catch (IOException e) {
            LOGGER.severe("FFmpeg 초기화 오류: " + e.getMessage());
            throw new BusinessException(ErrorCode.FFMPEG_INITIALIZATION_FAILED);
        }
    }

    /**
     * 오디오 파일 병합 프로세스 수행
     *
     * @param concatRequestDto 요청 데이터 DTO
     * @return 병합 결과 DTO
     */
    public ConcatResponseDto convertAllConcatDetails(ConcatRequestDto concatRequestDto) {
        LOGGER.info("convertAllConcatDetails 호출: " + concatRequestDto);

        // 1. 프로젝트 생성 또는 업데이트
        ConcatProject concatProject = saveOrUpdateProject(concatRequestDto);

        // 2. 응답 DTO 생성 및 초기화
        ConcatResponseDto concatResponseDto = initializeResponseDto(concatProject);

        try {
            // 3. 각 요청 디테일 처리
            List<ConcatResponseDetailDto> responseDetails = processRequestDetails(concatRequestDto, concatProject);

            // 4. 병합된 오디오 생성 및 S3 업로드
            String mergedFileUrl = mergeAudioFilesAndUploadToS3(responseDetails, uploadDir, concatRequestDto.getMemberId(), concatProject.getId());

            // 응답 DTO에 데이터 설정
            concatResponseDto.setOutputConcatAudios(Collections.singletonList(mergedFileUrl));
            concatResponseDto.setConcatResponseDetails(responseDetails);

            // 성공 시 ConcatStatusHistory 저장
            saveConcatStatusHistory(concatProject, ConcatStatusConst.SUCCESS);

            return concatResponseDto;

        } catch (Exception e) {
            LOGGER.severe("오류 발생: " + e.getMessage());

            // 실패 시 ConcatStatusHistory 저장
            saveConcatStatusHistory(concatProject, ConcatStatusConst.FAILURE);

            throw e;
        }
    }

    /**
     * 요청 디테일을 처리하여 응답 디테일 리스트를 반환
     */
    private List<ConcatResponseDetailDto> processRequestDetails(ConcatRequestDto concatRequestDto, ConcatProject concatProject) {
        List<ConcatResponseDetailDto> responseDetails = new ArrayList<>();

        for (ConcatRequestDetailDto detailDto : concatRequestDto.getConcatRequestDetails()) {
            try {
                LOGGER.info("ConcatDetail 처리 시작: " + detailDto);
                ConcatDetail concatDetail;
                MemberAudioMeta memberAudioMeta = null;

                if (detailDto.getId() == null) {
                    LOGGER.info("새로운 ConcatDetail 생성 - AudioSeq: " + detailDto.getAudioSeq());
                    // 새로운 디테일인 경우
                    memberAudioMeta = uploadConcatDetailSourceAudio(detailDto, concatProject);
                }

                // 디테일 저장 또는 업데이트
                concatDetail = saveOrUpdateDetail(detailDto, concatProject, memberAudioMeta);

                // 응답 디테일 생성 및 추가
                responseDetails.add(createResponseDetailDto(concatDetail));

                LOGGER.info("ConcatDetail 처리 완료 - ID: " + concatDetail.getId());

            } catch (Exception e) {
                LOGGER.severe("ConcatDetail 처리 중 오류 발생: " + detailDto + ", 메시지: " + e.getMessage());
                throw new BusinessException(ErrorCode.TTS_DETAIL_PROCESSING_FAILED);
            }
        }

        return responseDetails;
    }

    /**
     * 오디오 파일 병합 및 S3 업로드
     */
    public String mergeAudioFilesAndUploadToS3(List<ConcatResponseDetailDto> audioDetails, String uploadDir, Long userId, Long projectId) {
        List<String> savedFilePaths = new ArrayList<>();
        List<String> silenceFilePaths = new ArrayList<>();
        String mergedFilePath = null;

        try {
            // 1. 체크된 파일 필터링
            List<ConcatResponseDetailDto> filteredDetails = audioDetails.stream()
                    .filter(ConcatResponseDetailDto::isChecked)
                    .collect(Collectors.toList());

            if (filteredDetails.isEmpty()) {
                LOGGER.severe("병합할 파일이 없습니다.");

                // 실패 시 ConcatStatusHistory 저장
                saveConcatStatusHistory(concatProjectRepository.findById(projectId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_EXISTS_PROJECT)), ConcatStatusConst.FAILURE);

                throw new BusinessException(ErrorCode.NO_FILES_TO_MERGE);
            }

            // 2. S3에서 파일 다운로드 및 침묵 파일 생성
            for (ConcatResponseDetailDto detail : filteredDetails) {
                if (detail.getAudioUrl() != null) {
                    String savedFilePath = s3Service.downloadFileFromS3(detail.getAudioUrl(), uploadDir);
                    savedFilePaths.add(savedFilePath);

                    String silenceFilePath = audioProcessingService.createSilenceFile(detail.getEndSilence().longValue(), uploadDir);
                    if (silenceFilePath != null) silenceFilePaths.add(silenceFilePath);
                } else {
                    LOGGER.warning("Audio URL이 없습니다. Detail ID: " + detail.getId());
                }
            }

            // 3. 병합된 파일 생성
            mergedFilePath = audioProcessingService.mergeAudioFilesWithSilence(savedFilePaths, silenceFilePaths, uploadDir);

            // 4. 병합된 파일을 S3에 업로드 후 URL 반환
            return s3Service.uploadConcatSaveFile(audioProcessingService.convertToMultipartFile(mergedFilePath), userId, projectId);

        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_PROCESSING_ERROR);
        } finally {
            // 5. 임시 파일 삭제
            cleanupTemporaryFiles(savedFilePaths, silenceFilePaths, mergedFilePath);
        }
    }

    /**
     * 프로젝트 생성 또는 업데이트
     */
    private ConcatProject saveOrUpdateProject(ConcatRequestDto concatRequestDto) {
        return Optional.ofNullable(concatRequestDto.getProjectId())
                .map(projectId -> {
                    updateProject(concatRequestDto);
                    return concatProjectRepository.findById(projectId)
                            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_EXISTS_PROJECT));
                })
                .orElseGet(() -> createNewProject(concatRequestDto));
    }

    /**
     * 새로운 프로젝트 생성
     */
    private ConcatProject createNewProject(ConcatRequestDto dto) {
        Member member = memberRepository.findById(dto.getMemberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        ConcatProject concatProject = ConcatProject.createConcatProject(member, dto.getProjectName());

        // 프로젝트 전역 값 업데이트
        if (dto.getGlobalFrontSilenceLength() != 0.0F || dto.getGlobalTotalSilenceLength() != 0.0F || dto.getProjectName() != null) {
            concatProject.updateConcatProject(dto.getProjectName(), dto.getGlobalFrontSilenceLength(), dto.getGlobalTotalSilenceLength());
        }

        return concatProjectRepository.save(concatProject);
    }

    /**
     * 기존 프로젝트 업데이트
     */
    private void updateProject(ConcatRequestDto dto) {
        ConcatProject project = concatProjectRepository.findById(dto.getProjectId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_EXISTS_PROJECT));

        if (!project.getMember().getId().equals(dto.getMemberId())) {
            throw new BusinessException(ErrorCode.MEMBER_PROJECT_NOT_MATCH);
        }

        project.updateConcatProject(dto.getProjectName(), dto.getGlobalFrontSilenceLength(), dto.getGlobalTotalSilenceLength());
    }

    /**
     * 디테일 저장 또는 업데이트
     */
    private ConcatDetail saveOrUpdateDetail(ConcatRequestDetailDto detailDto, ConcatProject concatProject, MemberAudioMeta memberAudioMeta) {
        return Optional.ofNullable(detailDto.getId())
                .map(id -> {
                    updateConcatDetail(detailDto, concatProject);
                    return concatDetailRepository.findById(id)
                            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_EXISTS_PROJECT_DETAIL));
                })
                .orElseGet(() -> {
                    // 새로운 ConcatDetail 생성
                    return concatDetailRepository.save(
                            ConcatDetail.createConcatDetail(
                                    concatProject,
                                    detailDto.getAudioSeq(),
                                    detailDto.isChecked(),
                                    detailDto.getUnitScript(),
                                    detailDto.getEndSilence(),
                                    memberAudioMeta
                            )
                    );
                });
    }

    /**
     * 기존 디테일 업데이트
     */
    private void updateConcatDetail(ConcatRequestDetailDto detailDto, ConcatProject concatProject) {
        ConcatDetail concatDetail = concatDetailRepository.findById(detailDto.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_EXISTS_PROJECT_DETAIL));

        if (!concatDetail.getConcatProject().getId().equals(concatProject.getId())) {
            throw new BusinessException(ErrorCode.NOT_EXISTS_PROJECT_DETAIL);
        }

        concatDetail.updateDetails(
                detailDto.getAudioSeq(),
                detailDto.isChecked(),
                detailDto.getUnitScript(),
                detailDto.getEndSilence()
        );
    }

    /**
     * 요청된 디테일의 소스 오디오를 S3에 업로드 후 MemberAudioMeta 반환
     */
    private MemberAudioMeta uploadConcatDetailSourceAudio(ConcatRequestDetailDto detailDto, ConcatProject concatProject) {
        // 파일 업로드 및 MemberAudioMeta 생성
        List<MemberAudioMeta> memberAudioMetas = s3Service.uploadAndSaveMemberFile2(
                Collections.singletonList(detailDto.getSourceAudio()),
                concatProject.getMember().getId(),
                concatProject.getId(),
                AudioType.CONCAT
        );

        // 업로드된 첫 번째 파일의 MemberAudioMeta를 반환
        return memberAudioMetas.get(0);
    }

    /**
     * 응답 DTO 초기화
     */
    private ConcatResponseDto initializeResponseDto(ConcatProject concatProject) {
        return ConcatResponseDto.builder()
                .projectId(concatProject.getId())
                .projectName(concatProject.getProjectName())
                .globalFrontSilenceLength(concatProject.getGlobalFrontSilenceLength())
                .globalTotalSilenceLength(concatProject.getGlobalTotalSilenceLength())
                .build();
    }

    /**
     * 응답 디테일 DTO 생성
     */
    private ConcatResponseDetailDto createResponseDetailDto(ConcatDetail concatDetail) {
        return ConcatResponseDetailDto.builder()
                .id(concatDetail.getId())
                .audioSeq(concatDetail.getAudioSeq())
                .isChecked(concatDetail.isChecked())
                .unitScript(concatDetail.getUnitScript())
                .endSilence(concatDetail.getEndSilence())
                .audioUrl(concatDetail.getMemberAudioMeta() != null ? concatDetail.getMemberAudioMeta().getAudioUrl() : null)
                .build();
    }

    /**
     * 임시 파일 정리
     */
    private void cleanupTemporaryFiles(List<String> savedFilePaths, List<String> silenceFilePaths, String mergedFilePath) {
        audioProcessingService.deleteFiles(savedFilePaths);
        audioProcessingService.deleteFiles(silenceFilePaths);
        if (mergedFilePath != null) {
            audioProcessingService.deleteFiles(Collections.singletonList(mergedFilePath));
        }
    }

    /**
     * ConcatStatusHistory 저장
     */
    private void saveConcatStatusHistory(ConcatProject concatProject, ConcatStatusConst status) {
        ConcatStatusHistory concatStatusHistory = ConcatStatusHistory.createConcatStatusHistory(concatProject, status);
        concatStatusHistoryRepository.save(concatStatusHistory);
    }
}
