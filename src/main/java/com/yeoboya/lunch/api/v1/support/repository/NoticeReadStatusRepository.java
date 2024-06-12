package com.yeoboya.lunch.api.v1.support.repository;

import com.yeoboya.lunch.api.v1.member.domain.Member;
import com.yeoboya.lunch.api.v1.support.domain.Notice;
import com.yeoboya.lunch.api.v1.support.domain.NoticeReadStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NoticeReadStatusRepository extends JpaRepository<NoticeReadStatus, Long> {
    List<NoticeReadStatus> findByMember(Member member);
    Optional<NoticeReadStatus> findByMemberAndNotice(Member member, Notice notice);
}
