package com.fourformance.tts_vc_web.dto.workspace;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fourformance.tts_vc_web.common.constant.APIStatusConst;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// 프로젝트 id, 프로젝트 타입, 프로젝트 이름, API 상태, 생성 날짜, 업데이트 날짜

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class RecentProjectDto {
    private Long id;
    private String type;
    private String name;
    private APIStatusConst status;
    private String script; // 스크립트 추가

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}