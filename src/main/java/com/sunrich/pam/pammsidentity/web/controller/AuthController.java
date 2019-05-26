package com.sunrich.pam.pammsidentity.web.controller;

import com.sunrich.pam.common.constants.Constants;
import com.sunrich.pam.common.domain.User;
import com.sunrich.pam.common.dto.OrganizationDTO;
import com.sunrich.pam.common.dto.identity.AuthPayloadDTO;
import com.sunrich.pam.common.dto.identity.TokenDTO;
import com.sunrich.pam.common.exception.BadRequestException;
import com.sunrich.pam.common.exception.ErrorCodes;
import com.sunrich.pam.common.exception.UnauthorizedException;
import com.sunrich.pam.pammsidentity.service.AuthService;
import com.sunrich.pam.pammsidentity.service.TokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
public class AuthController {

    private AuthService authService;
    private TokenService tokenService;

    public AuthController(AuthService authService, TokenService tokenService) {
        this.authService = authService;
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody @Valid AuthPayloadDTO authPayload) throws UnauthorizedException {
        User user = authService.validateCredentials(authPayload.getUsername(), authPayload.getPassword());
        String token = user.getToken().getToken();
        if (user.getIsUsingMfa()) { // If TwoFactorAuth is enabled for this user ask for the code
            Map<String, Object> result = new HashMap<>();
            result.put(Constants.TOKEN, token);
            result.put(Constants.STATUS, 2);
            return result;
        } else {
            return getOrgListOrSelectOrg(token, user.getId());
        }
    }

    @PostMapping("/select-org")
    public Map<String, Object> selectOrganization(@RequestBody TokenDTO tokenDTO) throws UnauthorizedException {
        boolean status = authService.selectOrganization(tokenDTO.getToken(), tokenDTO.getOrgId());
        Map<String, Object> result = new HashMap<>();
        result.put(Constants.TOKEN, tokenDTO.getToken());
        result.put(Constants.STATUS, status ? 1 : 0);
        return result;
    }

    @PostMapping("/continue")
    public Map<String, Object> removeSession(@RequestBody Map<String, Object> reqBody) throws UnauthorizedException {
        String token = (String) reqBody.get(Constants.TOKEN);
        authService.invalidateTokenAndContinue(token);
        Map<String, Object> result = new HashMap<>();
        result.put(Constants.TOKEN, token);
        result.put(Constants.STATUS, 1);
        return result;
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(@RequestBody Map<String, Object> reqBody) {
        String token = (String) reqBody.get(Constants.TOKEN);
        tokenService.delete(token);
        Map<String, Object> result = new HashMap<>();
        result.put(Constants.STATUS, 1);
        return result;
    }

    @PostMapping("/get-code")
    public void enable2FA(@RequestBody Map<String, Object> reqBody, HttpServletResponse response) throws UnauthorizedException {
        String tokenStr = (String) reqBody.get(Constants.TOKEN);

        authService.generate2FACode(tokenStr, response);

        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        response.setContentType("image/png");
    }

    @PostMapping("/verify-code")
    public Map<String, Object> validateCode(@RequestBody Map<String, Object> reqBody) throws UnauthorizedException, BadRequestException {
        String tokenStr = (String) reqBody.get(Constants.TOKEN);
        String codeStr = (String) reqBody.get("code");

        long code;
        try {
            code = Long.parseLong(codeStr);
        } catch (NumberFormatException e) {
            throw new BadRequestException(ErrorCodes.BAD_CREDENTIALS, "Bad Request!");
        }

        User user = authService.verifyCode(tokenStr, code);
        return getOrgListOrSelectOrg(user.getToken().getToken(), user.getId());
    }

    @GetMapping("/health")
    public Map<String, Object> health(@RequestBody Map<String, Object> reqBody) throws UnauthorizedException {
        String token = (String) reqBody.get(Constants.TOKEN);
        tokenService.getAndValidate(token);
        Map<String, Object> result = new HashMap<>();
        result.put("status", 1);
        return result;
    }

    private Map<String, Object> getOrgListOrSelectOrg(String tokenStr, Long userId) throws UnauthorizedException {
        Map<String, Object> result = new HashMap<>();
        List<OrganizationDTO> userOrgList = authService.getOrganizationsByUserId(userId);
        if (userOrgList.isEmpty()) {
            throw new UnauthorizedException(ErrorCodes.INVALID_CREDENTIALS, "Invalid Credentials!");
        } else if (userOrgList.size() == 1) { // If user has only single org then skip Select Organization page and send him to dashboard
            TokenDTO token = TokenDTO.builder().token(tokenStr).orgId(userOrgList.get(0).getId()).build();
            return selectOrganization(token);
        } else {
            // Return list of organizations
            result.put("organizations", userOrgList);
            result.put(Constants.TOKEN, tokenStr);
            result.put(Constants.STATUS, 1);
        }
        return result;
    }
}
