package com.fourformance.tts_vc_web.dto.workspace;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ExportListDto {

    Long outputAudioMetaId; // 추가된 필드
    String projectType;
    String projectName;
    String fileName;
    String script;
    String unitStatus;
    String downloadLink;
    LocalDateTime updateAt;

    // 생성자 수정
    public ExportListDto(Long outputAudioMetaId, String projectType, String projectName, String filename, String script,
                         String unitStatus, LocalDateTime updateAt, String downloadLink) {
        this.outputAudioMetaId = outputAudioMetaId; // 추가된 필드 초기화
        this.projectType = projectType;
        this.projectName = projectName;
        this.fileName = filename;
        this.script = script;
        this.unitStatus = unitStatus;
        this.updateAt = updateAt;
        this.downloadLink = downloadLink;
    }
}