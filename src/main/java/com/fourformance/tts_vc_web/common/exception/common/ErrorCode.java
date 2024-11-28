package com.fourformance.tts_vc_web.common.exception.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // HttpStatus는 애매하면 일단 BAD_REQUEST로 통일

    // 0번대 코드 : ok
    OK(0, HttpStatus.OK, "Ok"),

    // 1000번대 코드 : 오디오 파일 s3 버킷 업/다운로드 관련
    NOT_EXISTS_AUDIO(1001, HttpStatus.BAD_REQUEST, "존재하지 않는 파일에 대한 접근 시도입니다."),
    EMPTY_FILE(1002, HttpStatus.BAD_REQUEST, "파일이 비어 있습니다."), // *
    PROJECT_NOT_FOUND(1003, HttpStatus.NOT_FOUND, "프로젝트를 찾을 수 없습니다."), // *
    MEMBER_NOT_FOUND(1004, HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다."), // *
    DETAIL_NOT_FOUND(1005, HttpStatus.NOT_FOUND, "유닛 정보를 찾을 수 없습니다."),
    UNSUPPORTED_PROJECT_TYPE(1006, HttpStatus.BAD_REQUEST, "지원되지 않는 프로젝트 유형입니다."), // *
    S3_UPLOAD_FAILED(1007, HttpStatus.INTERNAL_SERVER_ERROR, "S3 업로드에 실패했습니다."), // *
    FILE_PROCESSING_ERROR(1008, HttpStatus.INTERNAL_SERVER_ERROR, "파일 처리 중 오류가 발생했습니다."), // *
    S3_PRESIGNED_URL_FAILED(1009, HttpStatus.INTERNAL_SERVER_ERROR, "S3 presigned URL 생성에 실패했습니다."), // *
    MISSING_REQUIRED_FIELD(1010, HttpStatus.NOT_FOUND, "VC_TRG 파일이 누락되었습니다."),
    INVALID_PROJECT_DATA(1011, HttpStatus.BAD_REQUEST, "AudioFileDto에 MemberAudioMetaId와 LocalAudioFile이 모두 누락되었습니다."),
    INVALID_TRG_FILE_COMBINATION(1012, HttpStatus.BAD_REQUEST,
            "trgVoiceId가 설정된 경우, audioFiles에 VC_TRG 파일이 포함될 수 없습니다."),
    MISSING_TRG_FILE(1013, HttpStatus.BAD_REQUEST, "VC_TRG 파일은 반드시 하나여야 합니다."),
    S3_DOWNLOAD_FAILED(1014, HttpStatus.BAD_REQUEST, "VC_TRG 파일은 반드시 하나여야 합니다."),
    DETAIL_MISMATCH(1015, HttpStatus.BAD_REQUEST, "유닛 수와 파일 수가 일치하지 않습니다."),
    UNSUPPORTED_AUDIO_TYPE(1016, HttpStatus.BAD_REQUEST, "지원하지 않는 형식의 오디오 타입입니다."),
    INVALID_FILE_COUNT(1017, HttpStatus.BAD_REQUEST, "VC_TRG는 단일 파일만 허용합니다."),

    // 3000번대 코드 : DB 관련
    NOT_EXISTS_PROJECT(3000, HttpStatus.BAD_REQUEST, "해당 프로젝트를 찾을 수 없습니다."),
    AUDIO_NOT_FOUND_EXCEPTION(3400, HttpStatus.BAD_REQUEST, "VC Target 오디오를 찾을 수 없습니다."),
    VOICE_STYLE_NOT_FOUND_EXCEPTION(3500, HttpStatus.BAD_REQUEST, "Voice Style 데이터를 찾을 수 없습니다."),

    // 4000번대 코드 : 서버 내부 오류, 코드 오류
    SERVER_ERROR(4000, HttpStatus.BAD_REQUEST, "서버 내부 오류가 발생했습니다."),
    INVALID_REQUEST_DATA(4001, HttpStatus.BAD_REQUEST, "유효하지 않은 요청 데이터입니다."),
    INVALID_PROJECT_ID(40001, HttpStatus.BAD_REQUEST, "유효하지 않은 프로젝트 ID입니다."),
    UNAUTHORIZED(4010, HttpStatus.UNAUTHORIZED, "사용자 인증이 필요합니다."),

    // 5000번대 코드 : 서버 내부 오류 관련
    INTERNAL_SERVER_ERROR(5001, HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    INVALID_VOICE_ID(5002, HttpStatus.BAD_REQUEST, "유효하지 않은 Voice ID입니다."),
    TTS_CREATE_FAILED(5003, HttpStatus.INTERNAL_SERVER_ERROR, "TTS 변환 실패: 세부 데이터가 없습니다."),
    ID_REQUIRED_FOR_EXISTING_DATA(5004, HttpStatus.BAD_REQUEST, "기존 데이터에 대한 ID가 필요합니다."),
    NEW_DATA_PROCESSING_FAILED(5005, HttpStatus.INTERNAL_SERVER_ERROR, "New Data 처리에 실패했습니다."),
    TTS_DETAIL_NOT_FOUND(5006, HttpStatus.NOT_FOUND, "ID에 해당하는 TTSDetail을 찾을 수 없습니다."),
    EXISTING_DATA_PROCESSING_FAILED(5007, HttpStatus.INTERNAL_SERVER_ERROR, "Existing Data 처리에 실패했습니다."),
    TTS_CONVERSION_FAILED_EMPTY_CONTENT(5008, HttpStatus.INTERNAL_SERVER_ERROR, "TTS 변환 실패: 응답의 오디오 콘텐츠가 비어 있습니다."),
    TTS_CONVERSION_FAILED(5009, HttpStatus.INTERNAL_SERVER_ERROR, "TTS 변환 중 오류가 발생했습니다."),
    AUDIO_FILE_SAVE_ERROR(5010, HttpStatus.INTERNAL_SERVER_ERROR, "오디오 파일 저장 중 오류가 발생했습니다."),
    INVALID_TEXT_FOR_KO_KR(5011, HttpStatus.BAD_REQUEST, "언어 코드가 'ko-KR'로 설정되었지만, 텍스트는 한국어가 아닙니다."),
    INVALID_TEXT_FOR_ZH_CN(5012, HttpStatus.BAD_REQUEST, "언어 코드가 'zh-CN'로 설정되었지만, 텍스트는 중국어(간체)가 아닙니다."),
    INVALID_TEXT_FOR_CMN_TW(5013, HttpStatus.BAD_REQUEST, "언어 코드가 'cmn-TW'로 설정되었지만, 텍스트는 중국어(번체)가 아닙니다."),
    INVALID_TEXT_FOR_YUE_HK(5014, HttpStatus.BAD_REQUEST, "언어 코드가 'yue-HK'로 설정되었지만, 텍스트는 광둥어가 아닙니다."),
    INVALID_TEXT_FOR_JA_JP(5015, HttpStatus.BAD_REQUEST, "언어 코드가 'ja-JP'로 설정되었지만, 텍스트는 일본어가 아닙니다."),
    INVALID_TEXT_FOR_EN_US(5016, HttpStatus.BAD_REQUEST, "언어 코드가 'en-US'로 설정되었지만, 텍스트는 영어(미국)가 아닙니다."),
    INVALID_TEXT_FOR_EN_GB(5017, HttpStatus.BAD_REQUEST, "언어 코드가 'en-GB'로 설정되었지만, 텍스트는 영어(영국)가 아닙니다."),
    INVALID_TEXT_FOR_EN_AU(5018, HttpStatus.BAD_REQUEST, "언어 코드가 'en-AU'로 설정되었지만, 텍스트는 영어(호주)가 아닙니다."),
    INVALID_TEXT_FOR_EN_IN(5019, HttpStatus.BAD_REQUEST, "언어 코드가 'en-IN'로 설정되었지만, 텍스트는 영어(인도)가 아닙니다."),
    INVALID_TEXT_FOR_FR_FR(5020, HttpStatus.BAD_REQUEST, "언어 코드가 'fr-FR'로 설정되었지만, 텍스트는 프랑스어가 아닙니다."),
    INVALID_TEXT_FOR_FR_CA(5021, HttpStatus.BAD_REQUEST, "언어 코드가 'fr-CA'로 설정되었지만, 텍스트는 프랑스어(캐나다)가 아닙니다."),
    INVALID_TEXT_FOR_ES_ES(5022, HttpStatus.BAD_REQUEST, "언어 코드가 'es-ES'로 설정되었지만, 텍스트는 스페인어(스페인)가 아닙니다."),
    INVALID_TEXT_FOR_ES_US(5023, HttpStatus.BAD_REQUEST, "언어 코드가 'es-US'로 설정되었지만, 텍스트는 스페인어(미국)가 아닙니다."),
    INVALID_TEXT_FOR_PT_BR(5024, HttpStatus.BAD_REQUEST, "언어 코드가 'pt-BR'로 설정되었지만, 텍스트는 포르투갈어(브라질)가 아닙니다."),
    INVALID_TEXT_FOR_PT_PT(5025, HttpStatus.BAD_REQUEST, "언어 코드가 'pt-PT'로 설정되었지만, 텍스트는 포르투갈어(포르투갈)가 아닙니다."),
    INVALID_TEXT_FOR_DE_DE(5026, HttpStatus.BAD_REQUEST, "언어 코드가 'de-DE'로 설정되었지만, 텍스트는 독일어가 아닙니다."),
    INVALID_TEXT_FOR_IT_IT(5027, HttpStatus.BAD_REQUEST, "언어 코드가 'it-IT'로 설정되었지만, 텍스트는 이탈리아어가 아닙니다."),
    INVALID_TEXT_FOR_NL_BE(5028, HttpStatus.BAD_REQUEST, "언어 코드가 'nl-BE'로 설정되었지만, 텍스트는 네덜란드어(벨기에)가 아닙니다."),
    INVALID_TEXT_FOR_NL_NL(5029, HttpStatus.BAD_REQUEST, "언어 코드가 'nl-NL'로 설정되었지만, 텍스트는 네덜란드어(네덜란드)가 아닙니다."),
    INVALID_TEXT_FOR_RU_RU(5030, HttpStatus.BAD_REQUEST, "언어 코드가 'ru-RU'로 설정되었지만, 텍스트는 러시아어가 아닙니다."),
    INVALID_TEXT_FOR_UK_UA(5031, HttpStatus.BAD_REQUEST, "언어 코드가 'uk-UA'로 설정되었지만, 텍스트는 우크라이나어가 아닙니다."),
    INVALID_TEXT_FOR_BG_BG(5032, HttpStatus.BAD_REQUEST, "언어 코드가 'bg-BG'로 설정되었지만, 텍스트는 불가리아어가 아닙니다."),
    INVALID_TEXT_FOR_SR_RS(5033, HttpStatus.BAD_REQUEST, "언어 코드가 'sr-RS'로 설정되었지만, 텍스트는 세르비아어가 아닙니다."),
    INVALID_TEXT_FOR_TH_TH(5034, HttpStatus.BAD_REQUEST, "언어 코드가 'th-TH'로 설정되었지만, 텍스트는 태국어가 아닙니다."),
    INVALID_TEXT_FOR_HE_IL(5035, HttpStatus.BAD_REQUEST, "언어 코드가 'he-IL'로 설정되었지만, 텍스트는 히브리어가 아닙니다."),
    INVALID_TEXT_FOR_AR_XA(5036, HttpStatus.BAD_REQUEST, "언어 코드가 'ar-XA'로 설정되었지만, 텍스트는 아랍어가 아닙니다."),
    INVALID_TEXT_FOR_FIL_PH(5037, HttpStatus.BAD_REQUEST, "언어 코드가 'fil-PH'로 설정되었지만, 텍스트는 필리핀어가 아닙니다."),
    INVALID_TEXT_FOR_ID_ID(5038, HttpStatus.BAD_REQUEST, "언어 코드가 'id-ID'로 설정되었지만, 텍스트는 인도네시아어가 아닙니다."),
    INVALID_TEXT_FOR_MS_MY(5039, HttpStatus.BAD_REQUEST, "언어 코드가 'ms-MY'로 설정되었지만, 텍스트는 말레이어가 아닙니다."),
    INVALID_TEXT_FOR_AF_ZA(5040, HttpStatus.BAD_REQUEST, "언어 코드가 'af-ZA'로 설정되었지만, 텍스트는 아프리칸스어가 아닙니다."),
    INVALID_TEXT_FOR_IS_IS(5041, HttpStatus.BAD_REQUEST, "언어 코드가 'is-IS'로 설정되었지만, 텍스트는 아이슬란드어가 아닙니다."),
    INVALID_TEXT_FOR_GL_ES(5042, HttpStatus.BAD_REQUEST, "언어 코드가 'gl-ES'로 설정되었지만, 텍스트는 갈리시아어가 아닙니다."),
    INVALID_TEXT_FOR_EU_ES(5043, HttpStatus.BAD_REQUEST, "언어 코드가 'eu-ES'로 설정되었지만, 텍스트는 바스크어가 아닙니다."),

    INVALID_TEXT_FOR_CHINESE(5044, HttpStatus.BAD_REQUEST, "언어 코드가 'cmn-CN' 또는 'cmn-TW'로 설정되었지만, 텍스트는 중국어가 아닙니다."),
    INVALID_TEXT_FOR_LATIN_BASED(5045, HttpStatus.BAD_REQUEST, "라틴 기반 언어 코드로 설정되었지만, 텍스트가 해당 언어에 적합하지 않습니다."),
    INVALID_TEXT_FOR_CYRILLIC(5046, HttpStatus.BAD_REQUEST, "키릴 문자 기반 언어 코드로 설정되었지만, 텍스트가 해당 언어에 적합하지 않습니다."),
    INVALID_TEXT_FOR_THAI(5047, HttpStatus.BAD_REQUEST, "언어 코드가 'th-TH'로 설정되었지만, 텍스트는 태국어가 아닙니다."),
    INVALID_TEXT_FOR_HEBREW(5048, HttpStatus.BAD_REQUEST, "언어 코드가 'he-IL'로 설정되었지만, 텍스트는 히브리어가 아닙니다."),
    INVALID_TEXT_FOR_ARABIC(5049, HttpStatus.BAD_REQUEST, "언어 코드가 'ar-XA'로 설정되었지만, 텍스트는 아랍어가 아닙니다."),

    INVALID_LANGUAGE_CODE_FORMAT(5050, HttpStatus.BAD_REQUEST, "유효하지 않은 언어 코드 형식입니다."),
    TTS_DETAIL_PROCESSING_FAILED(5051, HttpStatus.INTERNAL_SERVER_ERROR, "TTSDetail 처리 중 오류가 발생했습니다."),
    DUPLICATE_TTS_DETAIL(5052, HttpStatus.INTERNAL_SERVER_ERROR, "중복된 TTSDetail이 발견되었습니다."),
    INVALID_UNIT_SCRIPT(5053, HttpStatus.BAD_REQUEST, "유효하지 않은 Unit Script입니다."),
    DIRECTORY_CREATION_FAILED(5054, HttpStatus.INTERNAL_SERVER_ERROR, "디렉토리 생성에 실패했습니다."),
    TTS_PROJECT_NOT_FOUND(5055, HttpStatus.INTERNAL_SERVER_ERROR, "TTS Project를 찾을 수 없습니다."),
    FILE_NOT_FOUND(5056, HttpStatus.INTERNAL_SERVER_ERROR, "FILE를 찾을 수 없습니다."),
    MEMBER_AUDIO_META_NOT_FOUND(5057, HttpStatus.INTERNAL_SERVER_ERROR, "Member audio meta가 존재하지 않습니다."),
    NO_FILES_TO_MERGE(5058, HttpStatus.INTERNAL_SERVER_ERROR, "병합할 파일이 없습니다."),
    INTERNAL_SERVER_TTS_ERROR(5059, HttpStatus.INTERNAL_SERVER_ERROR, "TTS 변환 중 예기치 않은 오류가 발생했습니다."),
    INVALID_REQUEST_TEXT_DETAIL_DATA(5060, HttpStatus.BAD_REQUEST, "요청 데이터에 유효한 텍스트 디테일이 없습니다."),
    UNSUPPORTED_LANGUAGE_CODE(5061, HttpStatus.BAD_REQUEST, "지원되지 않는 언어 코드입니다."),
    INVALID_TEXT_FOR_EN(5062, HttpStatus.BAD_REQUEST, "언어 코드가 'en-US', 'en-GB', 'en-AU', 'en-IN'로 설정되었지만, 텍스트는 영어가 아닙니다."),
    VC_DETAIL_NOT_FOUND(5063, HttpStatus.NOT_FOUND, "VC 디테일을 찾을 수 없습니다."),
    FFMPEG_INITIALIZATION_FAILED(5064, HttpStatus.NOT_FOUND, "FFMPEG_INITIALIZATION_FAILED"),

    // 6000번대 코드 : 회원 서비스 예외
    PASSWORD_MISMATCH(6001, HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."),
    CURRENT_PASSWORD_MISMATCH(6002, HttpStatus.BAD_REQUEST, "현재 비밀번호가 일치하지 않습니다."),
    NEW_PASSWORD_MISMATCH(6003, HttpStatus.BAD_REQUEST, "새 비밀번호가 일치하지 않습니다."),


    // 7000번대 코드 : 작업 큐 관련 예외
    UNKNOWN_TASK_TYPE(6000, HttpStatus.NOT_FOUND, "작업 유형을 알 수 없습니다."),


    // 9999 : 테스트용 커스텀 예외
    TEST_ERROR(9999, HttpStatus.BAD_REQUEST, "테스트용 커스텀 예외입니다."),

    // 10000 : 알 수 없는 예외
    UNKNOWN_ERROR(10000, HttpStatus.BAD_REQUEST, "알 수 없는 예외입니다,"),
    METADATA_FORM_FAULT(1234, HttpStatus.BAD_REQUEST, "메타데이터 형식이 잘못되었습니다."),

    NOT_EXISTS_VOICESTYLE(3001, HttpStatus.BAD_REQUEST, "해당 voice style의 id를 찾을 수 없습니다."),

    NOT_EXISTS_PROJECT_DETAIL(3100, HttpStatus.BAD_REQUEST, "해당 프로젝트 디테일을 찾을 수 없습니다."),

    DUPLICATE_UNIT_SEQUENCE(3200, HttpStatus.BAD_REQUEST, "중복된 unitSequence를 가집니다."),

    INVALID_UNIT_SEQUENCE_ORDER(3300, HttpStatus.BAD_REQUEST, "unitSequenc의 순서가 잘못됐습니다."),

    MEMBER_PROJECT_NOT_MATCH(9001, HttpStatus.BAD_REQUEST, "해당 유저의 프로젝트가 아닙니다!"),
    PROJECT_DETAIL_NOT_MATCH(9002, HttpStatus.BAD_REQUEST, "해당 프로젝트의 유닛이 아닙니다."),
    DTO_NOT_LOGICAL(9003, HttpStatus.BAD_REQUEST, "DTO가 논리적으로 어색합니다.(자세한건 다음에 쓸게요)");

    private final Integer code;
    private final HttpStatus httpStatus;
    private final String message;
}