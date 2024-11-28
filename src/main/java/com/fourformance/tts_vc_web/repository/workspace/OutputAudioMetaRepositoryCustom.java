package com.fourformance.tts_vc_web.repository.workspace;

import com.fourformance.tts_vc_web.dto.workspace.ExportListDto2;
import java.util.List;

public interface OutputAudioMetaRepositoryCustom {

    //    List<ExportListDto> findExportHistoryBySearchCriteria(Long memberId, String keyword);
    List<ExportListDto2> findExportHistoryBySearchCriteria2(Long memberId, String keyword);
}
