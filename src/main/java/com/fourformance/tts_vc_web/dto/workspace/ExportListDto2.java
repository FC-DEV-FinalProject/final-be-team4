package com.fourformance.tts_vc_web.dto.workspace;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ExportListDto2 {

    Long outputAudioMetaId;
    String projectType;
    String projectName;
    String fileName;
    String script;
    String status;
    String downloadLink;
    LocalDateTime createAt;
}