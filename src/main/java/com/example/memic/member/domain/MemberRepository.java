package com.example.memic.member.domain;

import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface MemberRepository extends Repository<Member, Long> {
    Optional<Member> findById(Long id);

    Member save(Member member);

    Optional<Member> findByEmail(String email);
}
