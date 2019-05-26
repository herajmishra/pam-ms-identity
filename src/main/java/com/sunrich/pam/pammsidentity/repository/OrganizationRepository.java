package com.sunrich.pam.pammsidentity.repository;

import com.sunrich.pam.common.domain.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    @Query(value = "SELECT o.* FROM organization o INNER JOIN user_org uc ON o.id = uc.org_id WHERE uc.user_id = ?1 AND o.record_status = 1", nativeQuery = true)
    List<Organization> getOrgsByUserId(Long userId);
}
