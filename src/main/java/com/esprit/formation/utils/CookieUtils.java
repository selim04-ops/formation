package com.esprit.formation.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseCookie;
import jakarta.servlet.http.HttpServletResponse;

public class CookieUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(CookieUtils.class);


    public static void addJwtCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from("jwt", token)
                .httpOnly(true) // Prevent JavaScript access
                .secure(false) // Only send over HTTPS
                .path("/") // Make the cookie accessible across the entire domain
                //.maxAge(60 * 60) // 1 hour
                .maxAge(24 * 60 * 60) // 24 hours
                .sameSite("Lax") // Required for cross-origin requests
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
        LOGGER.info("JWT cookie added successfully.");
    }

    public static void clearJwtCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0) // Expire the cookie immediately
                .sameSite("Lax")
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
        LOGGER.info("JWT cookie cleared successfully.");
    }

    public static String extractTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            LOGGER.warn("No cookies found in the request.");
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if ("jwt".equals(cookie.getName())) {
                LOGGER.info("JWT token extracted from cookie: {}", cookie.getValue());
                return cookie.getValue();
            }
        }
        LOGGER.warn("JWT cookie not found in the request.");
        return null;
    }

    public static void clearJsessionIdCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("JSESSIONID", "")
                .httpOnly(true)
                .secure(false) // Set to true in production (HTTPS only)
                .path("/")
                .maxAge(0) // Expire immediately
                .sameSite("Lax")
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
        LOGGER.info("JSESSIONID cookie cleared successfully.");
    }
}
