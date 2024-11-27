package com.fourformance.tts_vc_web.repository.workspace;

import com.fourformance.tts_vc_web.domain.entity.OutputAudioMeta;
import com.fourformance.tts_vc_web.dto.workspace.ExportListDto;

import java.util.List;

public interface OutputAudioMetaRepositoryCustom {

    List<ExportListDto> findExportHistoryBySearchCriteria(Long memberId, String keyword);
}
