package com.pigs.voxly.infrastructure.identity.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.pigs.voxly.api.identity.cookie.AccessTokenCookieHelper;
import com.pigs.voxly.application.identity.AuthService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;
    private final AccessTokenCookieHelper cookieHelper;
    private final String successRedirectUrl;

    public OAuth2LoginSuccessHandler(
            AuthService authService,
            AccessTokenCookieHelper cookieHelper,
            @Value("${app.auth.oauth-success-redirect-url:http://localhost:5173/dashboard}") String successRedirectUrl) {
        this.authService = authService;
        this.cookieHelper = cookieHelper;
        this.successRedirectUrl = successRedirectUrl;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        if (!(authentication.getPrincipal() instanceof OAuth2User oauthUser)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "OAuth2 principal is missing");
            return;
        }

        String email = firstNonBlank(
                oauthUser.getAttribute("email"),
                oauthUser.getAttribute("preferred_username"),
                oauthUser.getAttribute("upn"));
        if (email == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "OAuth2 provider did not return an email");
            return;
        }

        String displayName = firstNonBlank(
                oauthUser.getAttribute("name"),
                oauthUser.getAttribute("given_name"),
                email);

        var loginResult = authService.loginWithExternalProvider(email, displayName);
        if (loginResult.isFailure()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "External login failed");
            return;
        }

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookieHelper.createCookie(loginResult.getValue().tokens().refreshToken()).toString());
        response.sendRedirect(successRedirectUrl);
    }

    @SafeVarargs
    private static <T> String firstNonBlank(T... values) {
        for (T value : values) {
            if (value == null) {
                continue;
            }
            String text = value.toString();
            if (!text.isBlank()) {
                return text;
            }
        }
        return null;
    }
}
