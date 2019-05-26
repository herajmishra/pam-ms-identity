package com.sunrich.pam.pammsidentity.repository;

import com.sunrich.pam.common.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUserNameAndRecordStatusTrue(String userName);
    Optional<User> findById(Long id);
}
