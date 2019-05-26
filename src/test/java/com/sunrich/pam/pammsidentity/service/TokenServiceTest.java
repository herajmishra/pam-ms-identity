package com.sunrich.pam.pammsidentity.service;

import com.sunrich.pam.common.domain.Token;
import com.sunrich.pam.common.exception.UnauthorizedException;
import com.sunrich.pam.common.util.CommonUtil;
import com.sunrich.pam.pammsidentity.repository.TokenRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
public class TokenServiceTest {

    @InjectMocks
    TokenService tokenService;

    @Mock
    TokenRepository tokenRepository;

    @Spy
    CommonUtil commonUtil = new CommonUtil();

    @Test
    public void getAndValidate_whenValidToken_shouldReturnCorrespondingToken() throws UnauthorizedException {
        String tokenString = commonUtil.getUUID();
        Token token = Token.builder().id(1L).token(tokenString).userId(101L).orgId(1L).status(true).codeVerified(true).lastUpdatedDate(commonUtil.getCurrentDateTime()).build();

        when(tokenRepository.findByToken(tokenString)).thenReturn(Optional.of(token));
        assertThat(tokenService.getAndValidate(tokenString)).isEqualTo(token);
        verify(tokenRepository).findByToken(tokenString);
    }

    @Test(expected = UnauthorizedException.class)
    public void getAndValidate_whenTokenNotFound_shouldThrowException() throws UnauthorizedException {
        String nonExistingToken = commonUtil.getUUID();
        try {
            when(tokenRepository.findByToken(nonExistingToken)).thenReturn(Optional.empty());
            tokenService.getAndValidate(nonExistingToken);
        } finally {
            verify(tokenRepository).findByToken(nonExistingToken);
        }
    }

    @Test(expected = UnauthorizedException.class)
    public void getAndValidate_whenExpiredToken_shouldThrowException() throws UnauthorizedException {
        String tokenString = commonUtil.getUUID();
        Token token = Token.builder().id(1L).token(tokenString).userId(101L).orgId(1L).status(true).codeVerified(true).lastUpdatedDate(commonUtil.getCurrentDateTime().minusMinutes(61)).build();

        when(tokenRepository.findByToken(tokenString)).thenReturn(Optional.of(token));
        assertThat(tokenService.getAndValidate(tokenString)).isEqualTo(token);
        verify(tokenRepository).findByToken(tokenString);
    }

    @Test(expected = UnauthorizedException.class)
    public void getAndValidate_whenTotpCodeNotVerified_shouldThrowException() throws UnauthorizedException {
        String tokenString = commonUtil.getUUID();
        Token token = Token.builder().id(1L).token(tokenString).userId(101L).orgId(1L).status(true).codeVerified(false).lastUpdatedDate(commonUtil.getCurrentDateTime()).build();

        when(tokenRepository.findByToken(tokenString)).thenReturn(Optional.of(token));
        assertThat(tokenService.getAndValidate(tokenString)).isEqualTo(token);
        verify(tokenRepository).findByToken(tokenString);
    }

    @Test
    public void getAndValidate_whenTotpVerificationNotRequiredAndCodeNotVerified_shouldNotThrowException() throws UnauthorizedException {
        String tokenString = commonUtil.getUUID();
        Token token = Token.builder().id(1L).token(tokenString).userId(101L).orgId(1L).status(true).codeVerified(false).lastUpdatedDate(commonUtil.getCurrentDateTime()).build();

        when(tokenRepository.findByToken(tokenString)).thenReturn(Optional.of(token));
        assertThat(tokenService.getAndValidate(tokenString, false)).isEqualTo(token);
        verify(tokenRepository).findByToken(tokenString);
    }

    @Test
    public void getByOrgIdAndUserId_shouldReturnOptional() {
        Token token = Token.builder().id(1L).token(commonUtil.getUUID()).userId(101L).orgId(1L).status(true).codeVerified(false).lastUpdatedDate(commonUtil.getCurrentDateTime()).build();
        when(tokenRepository.findByOrgIdAndUserIdAndStatus(1L, 101L, true)).thenReturn(Optional.of(token));
        assertThat(tokenService.getByOrgIdAndUserId(token.getOrgId(), token.getUserId())).isEqualTo(Optional.of(token));
        verify(tokenRepository).findByOrgIdAndUserIdAndStatus(1L, 101L, true);
    }

    @Test
    public void updateSessionByOrgIdAndUserId_whenUpdate_thenSucceeds() {
        long userId = 1L, orgId = 101L;
        boolean status = true;
        doNothing().when(tokenRepository).updateStatusByOrgIdAndUserId(status, orgId, userId);
        tokenService.updateTokenStatusByOrgIdAndUserId(status, orgId, userId);
        verify(tokenRepository).updateStatusByOrgIdAndUserId(status, orgId, userId);
    }

    @Test
    public void delete_whenDelete_thenSucceeds() {
        String tokenStr = commonUtil.getUUID();
        doNothing().when(tokenRepository).deleteByToken(tokenStr);
        tokenService.delete(tokenStr);
        verify(tokenRepository).deleteByToken(tokenStr);
    }

}