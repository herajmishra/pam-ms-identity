package com.sunrich.pam.pammsidentity.service;

import com.sunrich.pam.common.domain.Organization;
import com.sunrich.pam.common.domain.Token;
import com.sunrich.pam.common.domain.User;
import com.sunrich.pam.common.dto.OrganizationDTO;
import com.sunrich.pam.common.exception.ErrorCodes;
import com.sunrich.pam.common.exception.UnauthorizedException;
import com.sunrich.pam.common.util.CommonUtil;
import com.sunrich.pam.common.util.TOTPUtils;
import com.sunrich.pam.pammsidentity.util.CodeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AuthService {

    private UserService userService;
    private TokenService tokenService;
    private OrganizationService organizationService;
    private CommonUtil commonUtil;

    public AuthService(UserService userService, TokenService tokenService, OrganizationService organizationService, CommonUtil commonUtil) {
        this.userService = userService;
        this.tokenService = tokenService;
        this.organizationService = organizationService;
        this.commonUtil = commonUtil;
    }

    @Transactional
    public User validateCredentials(String userName, String password) throws UnauthorizedException {
        User user = userService.findByUserName(userName);
        if (!user.getPassword().equals(password)) {
            throw new UnauthorizedException(ErrorCodes.INVALID_CREDENTIALS, "Invalid credentials!");
        }
        Token token = createNewToken(user);
        user.setToken(token);
        return user;
    }

    private Token createNewToken(User user) {
        Token token = Token.builder().userId(user.getId()).token(commonUtil.getUUID()).status(false)
                .codeVerified(!user.getIsUsingMfa()).lastUpdatedDate(commonUtil.getCurrentDateTime()).build();
        return tokenService.saveOrUpdate(token);
    }

    public List<OrganizationDTO> getOrganizationsByUserId(Long userId) {
        List<Organization> orgList = organizationService.getOrgsByUserId(userId);
        return orgList.stream()
                .map(org -> OrganizationDTO.builder().id(org.getId()).name(org.getName()).build())
                .collect(Collectors.toList());
    }

    /**
     * @param tokenStr - token string
     * @param orgId    - selected organization's id
     * @return - boolean whether user can continue with selected org or not ?
     * @throws UnauthorizedException - if the given token is invalid/expired/TOTP verification is incomplete
     */
    public synchronized boolean selectOrganization(String tokenStr, Long orgId) throws UnauthorizedException {
        Token token = tokenService.getAndValidate(tokenStr);     // get the tokenTemp from db
        // Check if any session is active with the same userId and orgId
        Optional<Token> optToken = tokenService.getByOrgIdAndUserId(orgId, token.getUserId());
        token.setStatus(!optToken.isPresent());
        token.setOrgId(orgId);
        tokenService.saveOrUpdate(token);
        return token.getStatus();
    }

    @Transactional
    public void invalidateTokenAndContinue(String newTokenStr) throws UnauthorizedException {
        Token token = invalidateSession(newTokenStr);
        token.setStatus(true);
        tokenService.saveOrUpdate(token);
    }

    /**
     * Invalidates all tokens with same orgId and userId as of given token
     *
     * @param tokenStr - token string to get orgId and UserId
     * @return returns the token object
     * @throws UnauthorizedException - if the given token is invalid/expired/TOTP verification is incomplete
     */
    private Token invalidateSession(String tokenStr) throws UnauthorizedException {
        Token token = tokenService.getAndValidate(tokenStr);
        tokenService.updateTokenStatusByOrgIdAndUserId(false, token.getOrgId(), token.getUserId());
        return token;
    }

    /**
     * Verifies the TOTP code
     * @param tokenStr - token string
     * @param code - TOTP code to be verified
     * @return returns user object if verification succeeds
     * @throws UnauthorizedException - if token is invalid or expired OR TOTP code is not valid
     */
    @Transactional
    public User verifyCode(String tokenStr, Long code) throws UnauthorizedException {

        Token token = tokenService.getAndValidate(tokenStr, false);
        User user = userService.findById(token.getUserId());

        boolean isValidCode = TOTPUtils.checkCode(user.getSecret(), code);
        if (!isValidCode) throw new UnauthorizedException(ErrorCodes.INVALID_CODE, "Invalid Code!");

        if (!user.getIsUsingMfa()) {
            user.setIsUsingMfa(true);
            userService.saveOrUpdate(user);
        }

        if (!token.getCodeVerified()) {
            token.setCodeVerified(true);
            tokenService.saveOrUpdate(token);
        }

        user.setToken(token);
        return user;
    }

    /**
     * Generates a QR code image and writes it to the HttpServletResponse
     * @param tokenStr - to validate token and get user details
     * @param response - to write the QR code image
     * @throws UnauthorizedException - if the given token is invalid/expired/TOTP verification is incomplete
     */
    public void generate2FACode(String tokenStr, HttpServletResponse response) throws UnauthorizedException {
        Token token = tokenService.getAndValidate(tokenStr);
        User user = userService.findById(token.getUserId());
        CodeUtil.generateCode(user, response);
    }

}
