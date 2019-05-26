package com.sunrich.pam.pammsidentity.service;

import com.sunrich.pam.common.constants.Constants;
import com.sunrich.pam.common.domain.Token;
import com.sunrich.pam.common.exception.ErrorCodes;
import com.sunrich.pam.common.exception.UnauthorizedException;
import com.sunrich.pam.pammsidentity.repository.TokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
public class TokenService {

    private TokenRepository tokenRepository;

    public TokenService(TokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    Token saveOrUpdate(Token token) {
        return tokenRepository.save(token);
    }

    public Token getAndValidate(String tokenStr) throws UnauthorizedException {
        return getAndValidate(tokenStr, true);
    }

    /**
     * Used to find token by token string and validate it
     *
     * @param tokenStr   - token string
     * @param verifyCode - whether to validate the TOTP code
     * @return - the token object
     * @throws UnauthorizedException - if the token is not found / expired / TOTP code is not verified
     */
    Token getAndValidate(String tokenStr, boolean verifyCode) throws UnauthorizedException {
        Token token = findByToken(tokenStr);
        return validateToken(token, verifyCode);
    }

    private Token findByToken(String tokenStr) throws UnauthorizedException {
        Optional<Token> optToken = tokenRepository.findByToken(tokenStr);
        if (!optToken.isPresent()) {
            throw new UnauthorizedException(ErrorCodes.INVALID_TOKEN, Constants.INVALID_TOKEN);
        }
        return optToken.get();
    }

    /**
     * Used to validate token by checking if the token has expired or the TOTP code is verified or not
     *
     * @param token      - token object
     * @param verifyCode - whether to validate the TOTP code is verified or not
     * @return - token object
     * @throws UnauthorizedException - if the token has expired or TOTP code is not verified
     */
    private Token validateToken(Token token, boolean verifyCode) throws UnauthorizedException {
        LocalDateTime expiresIn = token.getLastUpdatedDate().plusMinutes(Constants.TOKEN_VALIDITY_DURATION); // TODO-AD: Right now keeping static duration for token expiry (60 minutes), Needs to be decided and updated
        log.info("LocalDateTime.now() : {}, expiresIn : {}", LocalDateTime.now(), expiresIn);
        if (LocalDateTime.now().isAfter(expiresIn) || (verifyCode && !token.getCodeVerified())) {
            // TODO-AD: Do we need different exception ? & Different exception message to specifically convey that token has been expired or qrcode verification is incomplete
            throw new UnauthorizedException(ErrorCodes.INVALID_TOKEN, Constants.INVALID_TOKEN);
        }
        return token;
    }

    Optional<Token> getByOrgIdAndUserId(Long orgId, Long userId) {
        return tokenRepository.findByOrgIdAndUserIdAndStatus(orgId, userId, true);
    }

    @Transactional
    public void updateTokenStatusByOrgIdAndUserId(boolean status, Long orgId, Long userId) {
        tokenRepository.updateStatusByOrgIdAndUserId(status, orgId, userId);
    }

    @Transactional
    public void delete(String tokenStr) {
        tokenRepository.deleteByToken(tokenStr);
    }

}
