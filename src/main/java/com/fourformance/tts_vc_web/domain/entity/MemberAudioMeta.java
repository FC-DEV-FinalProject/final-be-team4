package com.fourformance.tts_vc_web.domain.entity;

import com.fourformance.tts_vc_web.common.constant.AudioFormat;
import com.fourformance.tts_vc_web.common.constant.AudioType;
import com.fourformance.tts_vc_web.domain.baseEntity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
public class MemberAudioMeta extends BaseEntity {
    @Id
    @GeneratedValue
    @Column(name = "member_audio_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    private String audioUrl;
    private String script;
    private AudioType audioType;
    private Boolean isDeleted=false;
    private AudioFormat audioFormat;
    private LocalDateTime createdAt;

    // MemberAudioMeta 생성 메서드
    public static MemberAudioMeta createMemberAudioMeta(Member member, String audioUrl, String script, AudioType audioType, AudioFormat audioFormat) {
        MemberAudioMeta memberAudioMeta = new MemberAudioMeta();
        memberAudioMeta.member = member;
        memberAudioMeta.audioUrl = audioUrl;
        memberAudioMeta.script = script;
        memberAudioMeta.audioType = audioType;
        memberAudioMeta.audioFormat = audioFormat;
        memberAudioMeta.createdAt = LocalDateTime.now();
        return memberAudioMeta;
    }

    public void delete() {
        this.isDeleted = true;
    }
}