package com.yeoboya.lunch.config.security.service;

import com.yeoboya.lunch.api.v1.common.exception.EntityNotFoundException;
import com.yeoboya.lunch.api.v1.common.response.Code;
import com.yeoboya.lunch.api.v1.common.response.Pagination;
import com.yeoboya.lunch.api.v1.common.response.Response;
import com.yeoboya.lunch.api.v1.member.domain.Member;
import com.yeoboya.lunch.api.v1.member.repository.MemberRepository;
import com.yeoboya.lunch.config.security.domain.MemberRole;
import com.yeoboya.lunch.config.security.domain.Role;
import com.yeoboya.lunch.config.security.domain.UserSecurityStatus;
import com.yeoboya.lunch.config.security.repository.MemberRolesRepository;
import com.yeoboya.lunch.config.security.repository.RoleRepository;
import com.yeoboya.lunch.config.security.repository.UserSecurityStatusRepository;
import com.yeoboya.lunch.config.security.reqeust.RoleRequest;
import com.yeoboya.lunch.api.v1.member.response.MemberRoleResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class RoleService {

    private final MemberRepository memberRepository;
    private final MemberRolesRepository memberRolesRepository;
    private final UserSecurityStatusRepository userSecurityStatusRepository;
    private final RoleRepository roleRepository;
    private final Response response;

    @Transactional
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Response.Body> updateAuthority(RoleRequest roleRequest) {

        Member targetMember = memberRepository.findByEmail(roleRequest.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("Member not found - " + roleRequest.getEmail()));

        Role role = roleRepository.findByRole(roleRequest.getRole());

        Optional<MemberRole> memberRoles = memberRolesRepository.findByMemberEmail(targetMember.getEmail());

        MemberRole memberRole;
        if(memberRoles.isPresent()) {
            memberRole = memberRoles.get();
            memberRole.setRole(role);
        } else {
            memberRole = MemberRole.createMemberRoles(targetMember, role);
        }

        memberRolesRepository.save(memberRole);

//        String token = jwtTokenProvider.resolveToken(request);
//        Authentication authentication = jwtTokenProvider.getAuthentication(token);
//        String redisRT = redisTemplate.opsForValue().get("RT:" + authentication.getName());

        return null;
    }

    public ResponseEntity<Response.Body> updateSecurityStatus(RoleRequest roleRequest) {

        log.info("roleRequest->{}", roleRequest);

        Member member = memberRepository.findByEmail(roleRequest.getEmail())
                .orElseThrow(() -> new EntityNotFoundException("Member with email " + roleRequest.getEmail() + " is not found"));

        UserSecurityStatus userSecuritystatus = member.getUserSecurityStatus();
        userSecuritystatus.setEnabled(roleRequest.isEnabled());
        userSecuritystatus.setAccountNonLocked(roleRequest.isAccountNonLocked());
        userSecurityStatusRepository.save(userSecuritystatus);


        return null;
    }


    public ResponseEntity<Response.Body> getAuthorityList(Pageable pageable) {
        Page<MemberRoleResponse> withRolesInPages = memberRepository.findWithRolesInPages(pageable);

        Pagination pagination = new Pagination(
                withRolesInPages.getNumber() + 1,
                withRolesInPages.isFirst(),
                withRolesInPages.isLast(),
                withRolesInPages.isEmpty(),
                withRolesInPages.getTotalPages(),
                withRolesInPages.getTotalElements());

        Map<String, Object> data = Map.of(
                "list", withRolesInPages.getContent(),
                "pagination", pagination);

        return response.success(Code.SEARCH_SUCCESS, data);
    }

    @Transactional
    public List<Role> getRoles() {
        return roleRepository.findAll();
    }

    @Transactional
    public void createRole(Role role){
        roleRepository.save(role);
    }


}
