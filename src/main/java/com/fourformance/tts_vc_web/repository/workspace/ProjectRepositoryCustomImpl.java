package com.fourformance.tts_vc_web.repository.workspace;

import com.fourformance.tts_vc_web.domain.entity.*;
import com.fourformance.tts_vc_web.dto.workspace.ProjectListDto;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

public class ProjectRepositoryCustomImpl implements ProjectRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public ProjectRepositoryCustomImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public List<ProjectListDto> findProjectsBySearchCriteria(Long memberId, String keyword) {
        QProject project = QProject.project;
        QTTSProject ttsProject = QTTSProject.tTSProject;
        QVCProject vcProject = QVCProject.vCProject;
        QConcatProject concatProject = QConcatProject.concatProject;

        // 공통 필터 조건
        BooleanBuilder whereClause = new BooleanBuilder();
        whereClause.and(project.member.id.eq(memberId)); // 멤버 ID 조건
        whereClause.and(project.isDeleted.isFalse());    // 삭제되지 않은 프로젝트

        // 키워드 검색
        if (keyword != null && !keyword.isEmpty()) {
            whereClause.and(project.projectName.containsIgnoreCase(keyword));
        }

        // QueryDSL로 데이터 조회
        List<ProjectListDto> result = queryFactory
                .selectFrom(project)
                .where(whereClause)
                .orderBy(project.updatedAt.desc())
                .fetch()
                .stream()
                .map(p -> {
                    ProjectListDto dto = new ProjectListDto();
                    dto.setProjectId(p.getId());
                    dto.setProjectName(p.getProjectName());
                    dto.setUpdatedAt(p.getUpdatedAt());
                    dto.setCreatedAt(p.getCreatedAt());

                    // 구체적인 타입별 분기 처리
                    if (p instanceof TTSProject) {
                        TTSProject tts = (TTSProject) p;

                        // TTS의 첫 번째 디테일 가져오기
                        String firstScript = queryFactory
                                .select(QTTSDetail.tTSDetail.unitScript)
                                .from(QTTSDetail.tTSDetail)
                                .where(QTTSDetail.tTSDetail.ttsProject.id.eq(tts.getId())
                                        .and(QTTSDetail.tTSDetail.isDeleted.isFalse()))
                                .orderBy(QTTSDetail.tTSDetail.unitSequence.asc())
                                .fetchFirst();

                        dto.setScript(firstScript);
                        dto.setProjectStatus(tts.getApiStatus().toString());
                        dto.setProjectType("TTS");
                    } else if (p instanceof VCProject) {
                        VCProject vc = (VCProject) p;

                        // VC의 첫 번째 디테일 가져오기
                        String firstScript = queryFactory
                                .select(QVCDetail.vCDetail.unitScript)
                                .from(QVCDetail.vCDetail)
                                .where(QVCDetail.vCDetail.vcProject.id.eq(vc.getId())
                                        .and(QVCDetail.vCDetail.isDeleted.isFalse()))
//                                .orderBy(QVCDetail.vCDetail.unitSequence.asc())
                                .fetchFirst();

                        dto.setScript(firstScript);
                        dto.setProjectStatus(vc.getApiStatus().toString());
                        dto.setProjectType("VC");
                    } else if (p instanceof ConcatProject) {
                        ConcatProject concat = (ConcatProject) p;

                        // CONCAT의 첫 번째 디테일 가져오기
                        String firstScript = queryFactory
                                .select(QConcatDetail.concatDetail.unitScript)
                                .from(QConcatDetail.concatDetail)
                                .where(QConcatDetail.concatDetail.concatProject.id.eq(concat.getId())
                                        .and(QConcatDetail.concatDetail.isDeleted.isFalse()))
                                .orderBy(QConcatDetail.concatDetail.audioSeq.asc())
                                .fetchFirst();

                        dto.setScript(firstScript);
                        dto.setProjectStatus(null); // CONCAT 프로젝트의 상태는 null
                        dto.setProjectType("CONCAT");
                    }
                    return dto;
                })
                .toList();

        return result;
    }
    @Override
    public Page<ProjectListDto> findProjectsBySearchCriteria(Long memberId, String keyword, Pageable pageable) {
        QProject project = QProject.project;
        QTTSProject ttsProject = QTTSProject.tTSProject;
        QVCProject vcProject = QVCProject.vCProject;
        QConcatProject concatProject = QConcatProject.concatProject;
        QTTSDetail ttsDetail = QTTSDetail.tTSDetail;
        QVCDetail vcDetail = QVCDetail.vCDetail;
        QConcatDetail concatDetail = QConcatDetail.concatDetail;

        // 공통 필터 조건
        BooleanBuilder whereClause = new BooleanBuilder();
        whereClause.and(project.member.id.eq(memberId)); // 멤버 ID 조건
        whereClause.and(project.isDeleted.isFalse());    // 삭제되지 않은 프로젝트

        if (keyword != null && !keyword.isEmpty()) {
            BooleanBuilder keywordConditions = new BooleanBuilder();
            keywordConditions.or(project.projectName.containsIgnoreCase(keyword));
            whereClause.and(keywordConditions);
        }

        // 전체 개수 조회
        long totalCount = queryFactory
                .select(project.count())
                .from(project)
                .where(whereClause)
                .fetchOne();

        // 페이징 처리된 데이터 조회
        List<Project> projects = queryFactory
                .selectFrom(project)
                .leftJoin(ttsProject).on(ttsProject.id.eq(project.id))
                .leftJoin(ttsDetail).on(
                        ttsDetail.ttsProject.id.eq(ttsProject.id)
                                .and(ttsDetail.isDeleted.isFalse())
                                .and(ttsDetail.unitSequence.eq(
                                        JPAExpressions.select(ttsDetail.unitSequence.min())
                                                .from(ttsDetail)
                                                .where(ttsDetail.ttsProject.id.eq(ttsProject.id)
                                                        .and(ttsDetail.isDeleted.isFalse()))
                                ))
                )
                .leftJoin(vcProject).on(vcProject.id.eq(project.id))
                .leftJoin(vcDetail).on(
                        vcDetail.vcProject.id.eq(vcProject.id)
                                .and(vcDetail.isDeleted.isFalse())
                                .and(vcDetail.createdAt.eq(
                                        JPAExpressions.select(vcDetail.createdAt.min())
                                                .from(vcDetail)
                                                .where(vcDetail.vcProject.id.eq(vcProject.id)
                                                        .and(vcDetail.isDeleted.isFalse()))
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
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(project.updatedAt.desc())
                .fetch();

        // DTO 변환
        List<ProjectListDto> dtos = projects.stream().map(p -> {
            ProjectListDto dto = new ProjectListDto();
            dto.setProjectId(p.getId());
            dto.setProjectName(p.getProjectName());
            dto.setUpdatedAt(p.getUpdatedAt());
            dto.setCreatedAt(p.getCreatedAt());

            if (p instanceof TTSProject) {
                TTSProject tts = (TTSProject) p;
                String firstScript = queryFactory
                        .select(ttsDetail.unitScript)
                        .from(ttsDetail)
                        .where(ttsDetail.ttsProject.id.eq(tts.getId())
                                .and(ttsDetail.isDeleted.isFalse()))
                        .orderBy(ttsDetail.unitSequence.asc())
                        .fetchFirst();
                dto.setScript(firstScript);
                dto.setProjectStatus(tts.getApiStatus().toString());
                dto.setProjectType("TTS");
            } else if (p instanceof VCProject) {
                VCProject vc = (VCProject) p;
                String firstScript = queryFactory
                        .select(vcDetail.unitScript)
                        .from(vcDetail)
                        .where(vcDetail.vcProject.id.eq(vc.getId())
                                .and(vcDetail.isDeleted.isFalse()))
                        .orderBy(vcDetail.createdAt.asc())
                        .fetchFirst();
                dto.setScript(firstScript);
                dto.setProjectStatus(vc.getApiStatus().toString());
                dto.setProjectType("VC");
            } else if (p instanceof ConcatProject) {
                ConcatProject concat = (ConcatProject) p;
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
        }).toList();

        return new PageImpl<>(dtos, pageable, totalCount);
    }


}