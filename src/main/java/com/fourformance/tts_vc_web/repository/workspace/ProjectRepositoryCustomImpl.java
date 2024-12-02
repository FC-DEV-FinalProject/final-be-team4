package com.fourformance.tts_vc_web.repository.workspace;

import static com.fourformance.tts_vc_web.domain.entity.QConcatDetail.concatDetail;
import static com.fourformance.tts_vc_web.domain.entity.QConcatProject.concatProject;
import static com.fourformance.tts_vc_web.domain.entity.QProject.project;
import static com.fourformance.tts_vc_web.domain.entity.QTTSDetail.tTSDetail;
import static com.fourformance.tts_vc_web.domain.entity.QTTSProject.tTSProject;
import static com.fourformance.tts_vc_web.domain.entity.QVCDetail.vCDetail;
import static com.fourformance.tts_vc_web.domain.entity.QVCProject.vCProject;

import com.fourformance.tts_vc_web.domain.entity.ConcatProject;
import com.fourformance.tts_vc_web.domain.entity.Project;
import com.fourformance.tts_vc_web.domain.entity.TTSProject;
import com.fourformance.tts_vc_web.domain.entity.VCProject;
import com.fourformance.tts_vc_web.dto.workspace.ProjectListDto;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

public class ProjectRepositoryCustomImpl implements ProjectRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public ProjectRepositoryCustomImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }


    @Override
    public List<ProjectListDto> findProjectsBySearchCriteria(Long memberId, String keyword) {

        // 공통 필터 조건
        BooleanBuilder whereClause = new BooleanBuilder();
//        commmonFilter(whereClause, project, memberId);

        whereClause.and(project.member.id.eq(memberId)); // 멤버 ID 조건
        whereClause.and(project.isDeleted.isFalse());    // 삭제되지 않은 프로젝트

        // 키워드 검색 조건 (아래 중 하나라도 만족해야 검색 됨)
        if (keyword != null && !keyword.isEmpty()) {
            BooleanBuilder keywordConditions = new BooleanBuilder();
            keywordConditions.or(project.projectName.containsIgnoreCase(keyword)); // 프로젝트 이름 검색

            keywordConditions.or(tTSDetail.unitScript.containsIgnoreCase(keyword)); // TTS 스크립트 검색

            keywordConditions.or(vCDetail.unitScript.containsIgnoreCase(keyword)); // VC 스크립트 검색
            keywordConditions.or(concatDetail.unitScript.containsIgnoreCase(keyword)); // Concat 스크립트 검색

            keywordConditions.or(tTSProject.apiStatus.stringValue().containsIgnoreCase(keyword)); // TTS 상태 검색
            keywordConditions.or(vCProject.apiStatus.stringValue().containsIgnoreCase(keyword)); // VC 상태 검색
//            keywordConditions.or(concatProject..stringValue().containsIgnoreCase(keyword)); // Concat도 그냥 컬럼으로 관리할걸 히스토리 상태에 대한 키가 히스토리 엔티티에 있는데... 일단 보류

            keywordConditions.or(tTSProject.projectType.containsIgnoreCase(keyword)); // TTS 타입
            keywordConditions.or(vCProject.projectType.containsIgnoreCase(keyword)); // VC 타입
            keywordConditions.or(concatProject.projectType.containsIgnoreCase(keyword)); // Concat 타입

            whereClause.and(keywordConditions); // 키워드 조건을 최종적으로 AND 조건에 추가
        }

        // QueryDSL로 데이터 조회
        List<ProjectListDto> result = queryFactory
                .selectFrom(project)
                // TTSDetail과 firstScript 조건으로 디테일 조인
                .leftJoin(tTSProject).on(tTSProject.id.eq(project.id))
                .leftJoin(tTSDetail).on(
                        tTSDetail.ttsProject.id.eq(tTSProject.id)
                                .and(tTSDetail.isDeleted.isFalse())
                                .and(tTSDetail.unitSequence.eq(
                                        JPAExpressions.select(tTSDetail.unitSequence.min())
                                                .from(tTSDetail)
                                                .where(tTSDetail.ttsProject.id.eq(tTSProject.id)
                                                        .and(tTSDetail.isDeleted.isFalse()))
                                ))
                )
                // VCDetail과 firstScript 조건 조인
                .leftJoin(vCProject).on(vCProject.id.eq(project.id))
                .leftJoin(vCDetail).on(
                        vCDetail.vcProject.id.eq(vCProject.id)
                                .and(vCDetail.isDeleted.isFalse())
                                .and(vCDetail.createdAt.eq(
                                        JPAExpressions.select(vCDetail.createdAt.min())
                                                .from(vCDetail)
                                                .where(vCDetail.vcProject.id.eq(vCProject.id)
                                                        .and(vCDetail.isDeleted.isFalse()))
                                ))
                )
                // ConcatDetail과 firstScript 조건 조인
                .leftJoin(concatProject).on(concatProject.id.eq(project.id))
                .leftJoin(concatDetail).on(
                        concatDetail.concatProject.id.eq(concatProject.id)
                                .and(concatDetail.isDeleted.isFalse())
                                .and(concatDetail.audioSeq.eq(
                                        JPAExpressions.select(concatDetail.audioSeq.min())
                                                .from(concatDetail)
                                                .where(concatDetail.concatProject.id.eq(concatProject.id)
                                                        .and(concatDetail.isDeleted.isFalse()))
                                ))
                )
                .where(whereClause) // 검색 필터 조건 적용
                .orderBy(project.updatedAt.desc()) // 프로젝트 업데이트 날짜 기준 내림차순 정렬
                .fetch()
                .stream()
                .map(p -> {
                    ProjectListDto dto = new ProjectListDto();
                    dto.setProjectId(p.getId());
                    dto.setProjectName(p.getProjectName());
                    dto.setUpdatedAt(p.getUpdatedAt());
                    dto.setCreatedAt(p.getCreatedAt());

                    // TTS 프로젝트 처리
                    if (p instanceof TTSProject tts) {
                        String firstScript = queryFactory
                                .select(tTSDetail.unitScript)
                                .from(tTSDetail)
                                .where(tTSDetail.ttsProject.id.eq(tts.getId())
                                        .and(tTSDetail.isDeleted.isFalse()))
                                .orderBy(tTSDetail.unitSequence.asc())
                                .fetchFirst();
                        dto.setScript(firstScript);
                        dto.setProjectStatus(tts.getApiStatus().toString());
                        dto.setProjectType("TTS");

                        // VC 프로젝트 처리
                    } else if (p instanceof VCProject vc) {
                        String firstScript = queryFactory
                                .select(vCDetail.unitScript)
                                .from(vCDetail)
                                .where(vCDetail.vcProject.id.eq(vc.getId())
                                        .and(vCDetail.isDeleted.isFalse()))
                                .orderBy(vCDetail.createdAt.asc())
                                .fetchFirst();
                        dto.setScript(firstScript);
                        dto.setProjectStatus(vc.getApiStatus().toString());
                        dto.setProjectType("VC");

                        // Concat 프로젝트 처리
                    } else if (p instanceof ConcatProject concat) {
                        String firstScript = queryFactory
                                .select(concatDetail.unitScript)
                                .from(concatDetail)
                                .where(concatDetail.concatProject.id.eq(concat.getId())
                                        .and(concatDetail.isDeleted.isFalse()))
                                .orderBy(concatDetail.audioSeq.asc())
                                .fetchFirst();
                        dto.setScript(firstScript);
                        System.out.println("concatScript = " + firstScript);
                        dto.setProjectType("CONCAT");
                    }

                    return dto;
                })
                .toList();

        return result;
    }


    @Override
    public Page<ProjectListDto> findProjectsBySearchCriteria(Long memberId, String keyword, Pageable pageable) {

        // 공통 필터 조건
        BooleanBuilder whereClause = new BooleanBuilder();
        whereClause.and(project.member.id.eq(memberId)); // 멤버 ID 조건
        whereClause.and(project.isDeleted.isFalse());    // 삭제되지 않은 프로젝트

        // 키워드 검색 조건 추가
        if (keyword != null && !keyword.isEmpty()) {
            BooleanBuilder keywordConditions = new BooleanBuilder();
            keywordConditions.or(project.projectName.containsIgnoreCase(keyword)); // 프로젝트 이름 검색
            keywordConditions.or(tTSDetail.unitScript.containsIgnoreCase(keyword)); // TTS 스크립트 검색
            keywordConditions.or(vCDetail.unitScript.containsIgnoreCase(keyword)); // VC 스크립트 검색
            keywordConditions.or(concatDetail.unitScript.containsIgnoreCase(keyword)); // Concat 스크립트 검색
            keywordConditions.or(tTSProject.apiStatus.stringValue().containsIgnoreCase(keyword)); // TTS 상태 검색
            keywordConditions.or(vCProject.apiStatus.stringValue().containsIgnoreCase(keyword)); // VC 상태 검색
            keywordConditions.or(tTSProject.projectType.containsIgnoreCase(keyword)); // TTS 타입
            keywordConditions.or(vCProject.projectType.containsIgnoreCase(keyword)); // VC 타입
            keywordConditions.or(concatProject.projectType.containsIgnoreCase(keyword)); // Concat 타입

            whereClause.and(keywordConditions);
        }

        // QueryDSL fetchResults로 데이터 조회 및 변환
        QueryResults<Project> queryResults = queryFactory
                .selectFrom(project)
                .leftJoin(tTSProject).on(tTSProject.id.eq(project.id))
                .leftJoin(tTSDetail).on(
                        tTSDetail.ttsProject.id.eq(tTSProject.id)
                                .and(tTSDetail.isDeleted.isFalse())
                                .and(tTSDetail.unitSequence.eq(
                                        JPAExpressions.select(tTSDetail.unitSequence.min())
                                                .from(tTSDetail)
                                                .where(tTSDetail.ttsProject.id.eq(tTSProject.id)
                                                        .and(tTSDetail.isDeleted.isFalse()))
                                ))
                )
                .leftJoin(vCProject).on(vCProject.id.eq(project.id))
                .leftJoin(vCDetail).on(
                        vCDetail.vcProject.id.eq(vCProject.id)
                                .and(vCDetail.isDeleted.isFalse())
                                .and(vCDetail.createdAt.eq(
                                        JPAExpressions.select(vCDetail.createdAt.min())
                                                .from(vCDetail)
                                                .where(vCDetail.vcProject.id.eq(vCProject.id)
                                                        .and(vCDetail.isDeleted.isFalse()))
                                ))
                )
                .leftJoin(concatProject).on(concatProject.id.eq(project.id))
                .leftJoin(concatDetail).on(
                        concatDetail.concatProject.id.eq(concatProject.id)
                                .and(concatDetail.isDeleted.isFalse())
                                .and(concatDetail.audioSeq.eq(
                                        JPAExpressions.select(concatDetail.audioSeq.min())
                                                .from(concatDetail)
                                                .where(concatDetail.concatProject.id.eq(concatProject.id)
                                                        .and(concatDetail.isDeleted.isFalse()))
                                ))
                )
                .where(whereClause)
                .orderBy(project.updatedAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetchResults(); // fetchResults 사용

        // 결과 DTO로 변환
        List<ProjectListDto> results = queryResults.getResults().stream()
                .map(p -> {
                    ProjectListDto dto = new ProjectListDto();
                    dto.setProjectId(p.getId());
                    dto.setProjectName(p.getProjectName());
                    dto.setUpdatedAt(p.getUpdatedAt());
                    dto.setCreatedAt(p.getCreatedAt());

                    // TTS 프로젝트 처리
                    if (p instanceof TTSProject tts) {
                        String firstScript = queryFactory
                                .select(tTSDetail.unitScript)
                                .from(tTSDetail)
                                .where(tTSDetail.ttsProject.id.eq(tts.getId())
                                        .and(tTSDetail.isDeleted.isFalse()))
                                .orderBy(tTSDetail.unitSequence.asc())
                                .fetchFirst();
                        dto.setScript(firstScript);
                        dto.setProjectStatus(tts.getApiStatus().toString());
                        dto.setProjectType("TTS");
                    }
                    // VC 프로젝트 처리
                    else if (p instanceof VCProject vc) {
                        String firstScript = queryFactory
                                .select(vCDetail.unitScript)
                                .from(vCDetail)
                                .where(vCDetail.vcProject.id.eq(vc.getId())
                                        .and(vCDetail.isDeleted.isFalse()))
                                .orderBy(vCDetail.createdAt.asc())
                                .fetchFirst();
                        dto.setScript(firstScript);
                        dto.setProjectStatus(vc.getApiStatus().toString());
                        dto.setProjectType("VC");
                    }
                    // Concat 프로젝트 처리
                    else if (p instanceof ConcatProject concat) {
                        String firstScript = queryFactory
                                .select(concatDetail.unitScript)
                                .from(concatDetail)
                                .where(concatDetail.concatProject.id.eq(concat.getId())
                                        .and(concatDetail.isDeleted.isFalse()))
                                .orderBy(concatDetail.audioSeq.asc())
                                .fetchFirst();
                        dto.setScript(firstScript);
                        dto.setProjectType("CONCAT");
                    }

                    return dto;
                })
                .toList();

        // 전체 개수
        long total = queryResults.getTotal();

        // Page 객체로 반환
        return new PageImpl<>(results, pageable, total);
    }
}