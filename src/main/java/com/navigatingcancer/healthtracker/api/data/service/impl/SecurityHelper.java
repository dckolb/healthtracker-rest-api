package com.navigatingcancer.healthtracker.api.data.service.impl;

import com.auth0.spring.security.api.authentication.JwtAuthentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityHelper {
    private static final Logger log = LoggerFactory.getLogger(SecurityHelper.class);

    public static  String getToken(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        JwtAuthentication jwtAuth = null;
        String token = "";
        if (JwtAuthentication.class.isInstance(auth)){
            jwtAuth  = (JwtAuthentication)auth;
            token = jwtAuth.getToken();
            log.debug("token {}", token);
        }
        return token;
    }
}
