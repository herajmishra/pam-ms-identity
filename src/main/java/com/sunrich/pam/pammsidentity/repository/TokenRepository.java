package com.sunrich.pam.pammsidentity.repository;

import com.sunrich.pam.common.domain.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {

    Optional<Token> findByOrgIdAndUserIdAndStatus(Long orgId, Long userId, Boolean status);

    Optional<Token> findByToken(String token);

    @Modifying
    @Query(value = "UPDATE token SET status =:status WHERE org_id =:orgId AND user_id =:userId", nativeQuery = true)
    void updateStatusByOrgIdAndUserId(@Param("status") boolean status, @Param("orgId") Long orgId, @Param("userId") Long userId);

    void deleteByToken(String token);
}
