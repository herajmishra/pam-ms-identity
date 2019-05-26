package com.sunrich.pam.pammsidentity.service;

import com.sunrich.pam.common.domain.UserOrg;
import com.sunrich.pam.pammsidentity.repository.UserOrgRepository;
import org.springframework.stereotype.Service;

@Service
public class UserOrgService {

    private UserOrgRepository userOrgRepository;

    public UserOrgService(UserOrgRepository userOrgRepository) {
        this.userOrgRepository = userOrgRepository;
    }

    void saveOrUpdate(UserOrg userOrg) {
        userOrgRepository.save(userOrg);
    }
}
