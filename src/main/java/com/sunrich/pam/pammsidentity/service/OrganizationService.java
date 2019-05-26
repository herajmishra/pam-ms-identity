package com.sunrich.pam.pammsidentity.service;

import com.sunrich.pam.common.domain.Organization;
import com.sunrich.pam.pammsidentity.repository.OrganizationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrganizationService {

    private OrganizationRepository organizationRepository;

    public OrganizationService(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    List<Organization> getOrgsByUserId(Long userId) {
        return organizationRepository.getOrgsByUserId(userId);
    }
}
