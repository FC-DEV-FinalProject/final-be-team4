package com.fourformance.tts_vc_web.domain.entity;

import jakarta.persistence.*;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Inheritance(strategy = InheritanceType.JOINED)
@Entity
@DiscriminatorColumn // 나중에 다시 보기
@NoArgsConstructor
public class Project {

    @Id @GeneratedValue @Column(name = "project_id")
    private Long id;

    @JoinColumn(name = "member_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;

    private String projectName;
    private Boolean isDeleted = false;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt=null;

    // projectName 업데이트 메서드
    public void updateProjectName(String newProjectName) {
        this.projectName = newProjectName;
    }

    // isDeleted 업데이트 메서드
    public void markAsDeleted() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    public void restore() {
        this.isDeleted = false;
        this.deletedAt = null;
    }

    // createdAt 업데이트 메서드
    public void createCreatedAt() {
        this.createdAt = LocalDateTime.now();
    }

    // updatedAt 업데이트 메서드
    public void updateUpdatedAt() {
        this.updatedAt = LocalDateTime.now();
    }

    public static Project createProject(Member member, String projectName) {
        Project project = new Project();
        project.member = member;
        project.projectName = projectName;
        project.createdAt = LocalDateTime.now(); // 생성 시간 설정
        project.updatedAt = LocalDateTime.now(); // 최초 생성 시 업데이트 시간도 함께 설정
        return project;
    }

}