package com.fourformance.tts_vc_web.repository.workspace;

import com.fourformance.tts_vc_web.domain.entity.QAPIStatus;
import com.fourformance.tts_vc_web.domain.entity.QConcatProject;
import com.fourformance.tts_vc_web.domain.entity.QOutputAudioMeta;
import com.fourformance.tts_vc_web.domain.entity.QTTSDetail;
import com.fourformance.tts_vc_web.domain.entity.QVCDetail;
import com.fourformance.tts_vc_web.dto.workspace.ExportListDto2;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.util.List;

public class OutputAudioMetaRepositoryCustomImpl implements OutputAudioMetaRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public OutputAudioMetaRepositoryCustomImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public List<ExportListDto2> findExportHistoryBySearchCriteria2(Long memberId, String keyword) {
        QOutputAudioMeta outputAudioMeta = QOutputAudioMeta.outputAudioMeta;
        QTTSDetail ttsDetail = QTTSDetail.tTSDetail;
        QVCDetail vcDetail = QVCDetail.vCDetail;
        QConcatProject concatProject = QConcatProject.concatProject;
        QAPIStatus apiStatus = QAPIStatus.aPIStatus;

        // 필터 조건 생성
        BooleanBuilder whereClause = new BooleanBuilder();
        whereClause.and(outputAudioMeta.isDeleted.isFalse()); // 삭제되지 않은 데이터

        // 멤버 ID 조건 추가 (null 체크 포함)
        BooleanBuilder memberConditions = new BooleanBuilder();
        memberConditions.or(
                outputAudioMeta.ttsDetail.isNotNull()
                        .and(outputAudioMeta.ttsDetail.ttsProject.isNotNull())
                        .and(outputAudioMeta.ttsDetail.ttsProject.member.isNotNull())
                        .and(outputAudioMeta.ttsDetail.ttsProject.member.id.eq(memberId))
        );
        memberConditions.or(
                outputAudioMeta.vcDetail.isNotNull()
                        .and(outputAudioMeta.vcDetail.vcProject.isNotNull())
                        .and(outputAudioMeta.vcDetail.vcProject.member.isNotNull())
                        .and(outputAudioMeta.vcDetail.vcProject.member.id.eq(memberId))
        );
        memberConditions.or(
                outputAudioMeta.concatProject.isNotNull()
                        .and(outputAudioMeta.concatProject.member.isNotNull())
                        .and(outputAudioMeta.concatProject.member.id.eq(memberId))
        );
        whereClause.and(memberConditions);

        // 키워드 검색 조건
        if (keyword != null && !keyword.trim().isEmpty()) {
            BooleanBuilder keywordConditions = new BooleanBuilder();
            keywordConditions.or(outputAudioMeta.projectType.stringValue().containsIgnoreCase(keyword));
            keywordConditions.or(
                    outputAudioMeta.ttsDetail.isNotNull()
                            .and(outputAudioMeta.ttsDetail.ttsProject.isNotNull())
                            .and(outputAudioMeta.ttsDetail.ttsProject.projectName.containsIgnoreCase(keyword))
            );
            keywordConditions.or(
                    outputAudioMeta.vcDetail.isNotNull()
                            .and(outputAudioMeta.vcDetail.vcProject.isNotNull())
                            .and(outputAudioMeta.vcDetail.vcProject.projectName.containsIgnoreCase(keyword))
            );
            keywordConditions.or(
                    outputAudioMeta.concatProject.isNotNull()
                            .and(outputAudioMeta.concatProject.projectName.containsIgnoreCase(keyword))
            );
            keywordConditions.or(
                    Expressions.stringTemplate(
                            "substring({0}, locate('/', reverse({0})) + 1)",
                            outputAudioMeta.bucketRoute
                    ).containsIgnoreCase(keyword)
            );
            keywordConditions.or(
                    outputAudioMeta.ttsDetail.isNotNull()
                            .and(outputAudioMeta.ttsDetail.unitScript.containsIgnoreCase(keyword))
            );
            keywordConditions.or(
                    outputAudioMeta.vcDetail.isNotNull()
                            .and(outputAudioMeta.vcDetail.unitScript.containsIgnoreCase(keyword))
            );
            keywordConditions.or(
                    Expressions.booleanTemplate(
                            "lower({0}) like lower({1})",
                            JPAExpressions.select(apiStatus.apiUnitStatusConst.stringValue())
                                    .from(apiStatus)
                                    .where(apiStatus.ttsDetail.eq(outputAudioMeta.ttsDetail)
                                            .or(apiStatus.vcDetail.eq(outputAudioMeta.vcDetail))),
                            "%" + keyword.toLowerCase() + "%"
                    )
            );
            whereClause.and(keywordConditions);
        }

        // 데이터 조회 및 DTO 매핑
        return queryFactory.select(
                        Projections.constructor(ExportListDto2.class,
                                outputAudioMeta.id,
                                outputAudioMeta.projectType.stringValue(),
                                outputAudioMeta.ttsDetail.ttsProject.projectName
                                        .coalesce(outputAudioMeta.vcDetail.vcProject.projectName,
                                                outputAudioMeta.concatProject.projectName),
                                Expressions.stringTemplate(
                                        "substring({0}, locate('/', reverse({0})) + 1)",
                                        outputAudioMeta.bucketRoute
                                ),
                                outputAudioMeta.ttsDetail.unitScript
                                        .coalesce(outputAudioMeta.vcDetail.unitScript, null),
                                JPAExpressions.select(apiStatus.apiUnitStatusConst.stringValue())
                                        .from(apiStatus)
                                        .where(apiStatus.ttsDetail.eq(outputAudioMeta.ttsDetail)
                                                .or(apiStatus.vcDetail.eq(outputAudioMeta.vcDetail)))
                                        .orderBy(apiStatus.responseAt.desc())
                                        .limit(1),
                                outputAudioMeta.audioUrl,
                                outputAudioMeta.createdAt
                        )
                )
                .from(outputAudioMeta)
                .leftJoin(outputAudioMeta.ttsDetail, ttsDetail)
                .on(ttsDetail.ttsProject.member.id.eq(memberId)) // 안전 조인 조건
                .leftJoin(outputAudioMeta.vcDetail, vcDetail)
                .on(vcDetail.vcProject.member.id.eq(memberId)) // 안전 조인 조건
                .leftJoin(outputAudioMeta.concatProject, concatProject)
                .on(concatProject.member.id.eq(memberId)) // 안전 조인 조건
                .where(whereClause)
                .orderBy(outputAudioMeta.createdAt.desc())
                .fetch();
    }
}