package com.talkeasy.server.controller.member;

import com.talkeasy.server.common.CommonResponse;
import com.talkeasy.server.dto.user.MemberDetailRequest;
import com.talkeasy.server.service.member.OAuthService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Api(tags = {"Oauth 로그인 관련 API"})
@Controller
@RequiredArgsConstructor
@RequestMapping("/api/oauth")
@Slf4j
public class OAuthController {
    private final OAuthService oAuthService;

    @ApiOperation(value = "로그인 하기", notes = "access token 을 보내면 유저정보 반환")
    @PostMapping("/login")
    public ResponseEntity<CommonResponse> login(@ApiParam("") @RequestParam(value = "accessToken") String accessToken) { // 인가 코드
        log.info("========== /login/oauth accessToken : {}", accessToken);

        String token = null;
        token = oAuthService.getToken(accessToken);

        if (token == null) {
            return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.of("정보가 없습니다.", ""));
        }

        return ResponseEntity.status(HttpStatus.OK).body(CommonResponse.of("로그인 성공", token));
    }

    @ApiOperation(value = "회원정보 등록하기", notes = "jwt 토큰 리턴")
    @PostMapping("/register")
    ResponseEntity<CommonResponse> registerUser(@RequestBody MemberDetailRequest member) {
        log.info("========== /register registerUser name : {}, email : {}, imageUrl : {}", member.getName(), member.getEmail(), member.getImageUrl());

        String token = oAuthService.registerUser(member);

        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.of("회원가입 성공", token));
    }

}