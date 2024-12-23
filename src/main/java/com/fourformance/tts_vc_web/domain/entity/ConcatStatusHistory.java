package com.fourformance.tts_vc_web.domain.entity;

import com.fourformance.tts_vc_web.common.constant.ConcatStatusConst;
import com.fourformance.tts_vc_web.domain.baseEntity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@ToString
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConcatStatusHistory extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private ConcatProject concatProject;

    @Enumerated(EnumType.STRING)
    private ConcatStatusConst concatStatusConst;
    private LocalDateTime createdAt;

    @ElementCollection
    @CollectionTable(name = "concat_user_audio", joinColumns = @JoinColumn(name = "history_id"))
    private List<String> userAudioList = new ArrayList<>();

    // 생성 메서드
    public static ConcatStatusHistory createConcatStatusHistory(ConcatProject concatProject, ConcatStatusConst concatStatusConst) {
        ConcatStatusHistory history = new ConcatStatusHistory();
        history.concatProject = concatProject;
        history.concatStatusConst = concatStatusConst;
        history.createdAt = LocalDateTime.now();
        return history;
    }
}