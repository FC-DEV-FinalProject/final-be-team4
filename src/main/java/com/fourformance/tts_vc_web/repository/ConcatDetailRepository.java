package com.fourformance.tts_vc_web.repository;

import com.fourformance.tts_vc_web.domain.entity.ConcatDetail;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ConcatDetailRepository extends JpaRepository<ConcatDetail, Long> {

    // Concat 프로젝트의 id로 Concat 디테일 리스트 조회 - 의준
    List<ConcatDetail> findByConcatProject_Id(Long projectId);

    // Concat Detail Id가 담긴 List로 ConcatDetail 객체 반환 받기 - 의준
//    @Query("SELECT t FROM ConcatDetail c WHERE c.id IN :concatDetailIdList")
    List<ConcatDetail> findByIdIn(List<Long> concatDetailIds);
//    List<ConcatDetail> findByConcatDetailIds(@Param("concatDetailIdList") List<Long> concatDetailIdList);

    // 특정 ConcatProject와 연관된 모든 ConcatDetail 조회 - 의준
    List<ConcatDetail> findByConcatProjectId(Long projectId);

    // 특정 ConcatProject와 연관된 ConcatDetail의 script만 조회
    @Query("SELECT d.unitScript FROM ConcatDetail d WHERE d.concatProject.id = :projectId")
    List<String> findScriptsByConcatProjectId(@Param("projectId") Long projectId);

    // concat 프로젝트 id로 모든 concat 유닛 조회 - 의준
    @Query("SELECT c FROM ConcatDetail c WHERE c.concatProject.id = :projectId")
    List<ConcatDetail> findAllByProjectId(@Param("projectId") Long projectId);

}
