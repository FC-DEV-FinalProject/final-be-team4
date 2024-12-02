package com.fourformance.tts_vc_web.controller.member;

import com.fourformance.tts_vc_web.common.exception.common.BusinessException;
import com.fourformance.tts_vc_web.common.exception.common.ErrorCode;
import com.fourformance.tts_vc_web.dto.member.*;
import com.fourformance.tts_vc_web.dto.response.DataResponseDto;
import com.fourformance.tts_vc_web.dto.response.ResponseDto;
import com.fourformance.tts_vc_web.service.member.MemberService_wonwoo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/member")
@RequiredArgsConstructor
@Tag(name = "Member Controller", description = "회원 관리와 관련된 작업")
public class MemberController_wonwoo {

    private final MemberService_wonwoo memberService;

    /**
     * 회원 로그인
     * 세션을 생성하여 로그인 상태를 유지합니다.
     */
    @Operation(summary = "회원 로그인", description = "회원 인증을 수행하고 세션을 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패 - 잘못된 자격 증명")
    })
    @PostMapping("/login")
    public ResponseDto login(
            @RequestBody MemberLoginRequestDto requestDto,
            HttpServletRequest request) {

        // 로그인 처리
        MemberLoginResponseDto memberLoginResponseDto = memberService.login(requestDto);

        // 기존 세션이 있으면 무효화
        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }

        // 새로운 세션 생성
        HttpSession session = request.getSession(true);
        session.setAttribute("memberId", memberLoginResponseDto.getId());
        session.setAttribute("email", memberLoginResponseDto.getEmail());
        session.setAttribute("phoneNumber", memberLoginResponseDto.getPhoneNumber());

        return DataResponseDto.of(memberLoginResponseDto);
    }

    /**
     * 회원 로그아웃
     * 세션을 무효화하여 로그아웃 처리합니다.
     */
    @Operation(summary = "회원 로그아웃", description = "세션을 무효화하여 회원을 로그아웃 처리합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그아웃 성공")
    })
    @PostMapping("/logout")
    public ResponseDto logout(HttpSession session) {
        // 세션 무효화
        session.invalidate();
        return DataResponseDto.of("로그아웃 성공");
    }


    /**
     * 회원 정보 조회
     * 세션에서 이메일을 얻고, 비밀번호를 확인하여 회원 정보를 반환합니다.
     */
    @Operation(summary = "회원 정보 조회", description = "인증된 회원의 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원 정보 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패 - 세션 없음")
    })
    @PostMapping("/info")
    public ResponseDto getMemberInfo(
            @RequestBody MemberInfoRequestDto requestDto,
            HttpSession session) {

        // 세션에서 이메일 가져오기
        String email = (String) session.getAttribute("email");

        if (email == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED); // 세션 없음
        }

        // 비밀번호 확인 후 회원 정보 조회
        MemberUpdateResponseDto memberUpdateResponseDto = memberService.getMemberInfo(email, requestDto.getPwd());
        return DataResponseDto.of(memberUpdateResponseDto);
    }

    /**
     * 회원 정보 수정
     * 이름과 전화번호를 수정할 수 있습니다.
     */
    @Operation(summary = "회원 정보 수정", description = "인증된 회원의 개인 정보를 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원 정보 수정 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패 - 세션 없음")
    })
    @PutMapping("/info/update")
    public ResponseDto updateMemberInfo(
            @RequestBody MemberUpdateRequestDto requestDto, HttpSession session) {
        String email = (String) session.getAttribute("email");

        if (email == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED); // 세션 없음
        }

        // 회원 정보 수정
        MemberUpdateResponseDto memberUpdateResponseDto = memberService.updateMemberInfo(email, requestDto);
        return DataResponseDto.of(memberUpdateResponseDto);
    }


    /**
     * 비밀번호 수정
     * 현재 비밀번호와 새 비밀번호, 새 비밀번호 확인을 처리합니다.
     */
    @Operation(summary = "비밀번호 수정", description = "인증된 회원의 비밀번호를 변경합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "비밀번호 변경 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패 - 세션 없음")
    })
    @PutMapping("/password/update")
    public ResponseDto updatePassword(
            @RequestBody PasswordUpdateRequestDto requestDto, HttpSession session) {
        String email = (String) session.getAttribute("email");

        if (email == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED); // 세션 없음
        }

        // 비밀번호 수정
        memberService.updatePassword(email, requestDto);
        return DataResponseDto.of("비밀번호가 성공적으로 변경되었습니다.");
    }
}

