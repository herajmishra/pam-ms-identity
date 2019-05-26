package com.sunrich.pam.pammsidentity.service;

import com.sunrich.pam.common.domain.Token;
import com.sunrich.pam.common.domain.User;
import com.sunrich.pam.common.exception.UnauthorizedException;
import com.sunrich.pam.common.util.CommonUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
public class AuthServiceTest {

    @InjectMocks
    private AuthService authService;
    @Mock
    private UserService userService;
    @Mock
    private TokenService tokenService;
    @Mock
    private OrganizationService organizationService;

    @Spy
    private CommonUtil commonUtil = new CommonUtil();

    @Test
    public void validateCredentials_whenValidCredentials_thenReturnsUserObject() throws UnauthorizedException {
        String tokenStr = "11db1d35-bec9-4ac0-bf12-ed4ca212f46d";
        String userName = "jerry@gmail.com", password = "Pass@1234";
        LocalDateTime now = commonUtil.getCurrentDateTime();

        User.UserBuilder userBuilder = User.builder().id(1L).userName(userName).password("Pass@1234").isUsingMfa(true);
        User findByIdResult = userBuilder.build();
        User returnedUser = userBuilder.build();

        Token.TokenBuilder tokenBuilder = Token.builder().userId(findByIdResult.getId()).token(tokenStr).status(false)
                .codeVerified(!findByIdResult.getIsUsingMfa()).lastUpdatedDate(now);
        Token tokenToSave = tokenBuilder.build();
        Token savedToken = tokenBuilder.id(50L).build();

        returnedUser.setToken(savedToken);

        when(userService.findByUserName(userName)).thenReturn(findByIdResult);
        when(tokenService.saveOrUpdate(tokenToSave)).thenReturn(savedToken);
        when(commonUtil.getUUID()).thenReturn(tokenStr);
        when(commonUtil.getCurrentDateTime()).thenReturn(now);

        assertThat(authService.validateCredentials(userName, password)).isEqualTo(returnedUser);

        verify(userService).findByUserName(userName);
        verify(tokenService).saveOrUpdate(tokenToSave);
        verify(commonUtil).getUUID();
        verify(commonUtil, times(2)).getCurrentDateTime();
    }

    @Test(expected = UnauthorizedException.class)
    public void validateCredentials_whenInValidCredentials_shouldThrowException() throws UnauthorizedException {
        String userName = "jerry@gmail.com", password = "WrongPassword";
        User findByIdResult = User.builder().id(1L).userName(userName).password("Pass@1234").isUsingMfa(true).build();

        when(userService.findByUserName(userName)).thenReturn(findByIdResult);
        authService.validateCredentials(userName, password);
        verify(userService).findByUserName(userName);
    }

    @Test
    public void selectOrganization_whenValidInput_shouldReturnTrue() throws UnauthorizedException {
        String tokenStr = "11db1d35-bec9-4ac0-bf12-ed4ca212f46d";
        long orgId = 1L, userId = 101L;

        Token validatedToken = Token.builder().userId(userId).build();

        when(tokenService.getAndValidate(tokenStr)).thenReturn(validatedToken);
        when(tokenService.getByOrgIdAndUserId(orgId, validatedToken.getUserId())).thenReturn(Optional.empty());

        assertThat(authService.selectOrganization(tokenStr, orgId)).isEqualTo(true);

        verify(tokenService).getAndValidate(tokenStr);
        verify(tokenService).getByOrgIdAndUserId(orgId, validatedToken.getUserId());
    }

    @Test
    public void invalidateTokenAndContinue_whenInvalidateRequest_shouldInvalidateAllAndActivateOnlyNewToken() throws UnauthorizedException {
        String tokenStr = "11db1d35-bec9-4ac0-bf12-ed4ca212f46d";
        long userId = 1L, orgId = 1L;

        Token.TokenBuilder tokenBuilder = Token.builder().orgId(orgId).userId(userId);
        Token validatedToken = tokenBuilder.build();
        Token toSave = tokenBuilder.status(true).build();

        when(tokenService.getAndValidate(tokenStr)).thenReturn(validatedToken);
        doNothing().when(tokenService).updateTokenStatusByOrgIdAndUserId(false, validatedToken.getOrgId(), validatedToken.getUserId());
        when(tokenService.saveOrUpdate(toSave)).thenReturn(toSave);

        authService.invalidateTokenAndContinue(tokenStr);

        verify(tokenService).getAndValidate(tokenStr);
        verify(tokenService).updateTokenStatusByOrgIdAndUserId(false, validatedToken.getOrgId(), validatedToken.getUserId());
        verify(tokenService).saveOrUpdate(toSave);
    }

}
