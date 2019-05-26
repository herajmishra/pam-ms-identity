package com.sunrich.pam.pammsidentity.repository;

import com.sunrich.pam.common.domain.UserOrg;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserOrgRepository extends JpaRepository<UserOrg, Long> {
}
