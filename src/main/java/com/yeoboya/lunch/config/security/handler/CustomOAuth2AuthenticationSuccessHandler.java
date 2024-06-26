package com.yeoboya.lunch.config.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeoboya.lunch.api.v1.member.domain.Member;
import com.yeoboya.lunch.api.v1.member.domain.MemberInfo;
import com.yeoboya.lunch.api.v1.member.repository.MemberRepository;
import com.yeoboya.lunch.config.security.JwtTokenProvider;
import com.yeoboya.lunch.config.security.constants.Authority;
import com.yeoboya.lunch.config.security.domain.Role;
import com.yeoboya.lunch.config.security.domain.UserSecurityStatus;
import com.yeoboya.lunch.config.security.dto.Token;
import com.yeoboya.lunch.config.security.repository.RoleRepository;
import com.yeoboya.lunch.config.security.service.OAuth2UserImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomOAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final RoleRepository roleRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;
    private final MemberRepository memberRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        Member member = ((OAuth2UserImpl) authentication.getPrincipal()).getMember();

        String redirectURL;
        if (member.getRole().getRole().equals(roleRepository.findByRole(Authority.ROLE_GUEST).getRole())) {
            redirectURL = UriComponentsBuilder.fromUriString("http://localhost:8080/oauth2/signUp")
                    .queryParam("email", member.getEmail())
                    .queryParam("provider", member.getProvider())
                    .queryParam("providerId", member.getProviderId())
                    .build()
                    .encode(StandardCharsets.UTF_8)
                    .toUriString();

            MemberInfo memberInfo = MemberInfo.createMemberInfo(member);
            UserSecurityStatus userSecurityStatus = UserSecurityStatus.createUserSecurityStatus(member);
            Member saveMember = Member.createMember(member, memberInfo, roleRepository.findByRole(Authority.ROLE_USER), userSecurityStatus);
            memberRepository.save(saveMember);

        } else {
            Token token = jwtTokenProvider.generateToken(authentication, member.getProvider(), member.getLoginId());
            response.addHeader("AccessToken", token.getAccessToken());
            response.addHeader("RefreshToken", token.getRefreshToken());

            redisTemplate.opsForValue().set("RT:" + member.getLoginId(),
                    token.getRefreshToken(),
                    token.getRefreshTokenExpirationTime() - new Date().getTime(),
                    TimeUnit.MILLISECONDS);

            redirectURL = UriComponentsBuilder.fromUriString("http://localhost:8080/")
                    .build()
                    .encode(StandardCharsets.UTF_8)
                    .toUriString();
        }

        getRedirectStrategy().sendRedirect(request, response, redirectURL);

    }
}
