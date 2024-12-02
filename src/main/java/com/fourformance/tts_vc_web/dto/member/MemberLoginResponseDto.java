package com.fourformance.tts_vc_web.dto.member;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberLoginResponseDto {
    /**
     * 로그인 응답 DTO
     * 서버가 로그인 성공 후 회원 정보를 클라이언트로 전달합니다.
     */

    private Long id; // 로그인한 회원의 고유 ID
    private String email; // 로그인한 회원의 이메일
    private String name; // 로그인한 회원의 이름
    private String phoneNumber; // 로그인한 회원의 전화번호

}
