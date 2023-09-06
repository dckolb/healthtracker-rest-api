package com.navigatingcancer.healthtracker.api.data.auth;

import org.springframework.stereotype.Component;

@Component
public class IdentityContextHolder {
    private static ThreadLocal<IdentityContext> userContext = new ThreadLocal<>();

    public void set(IdentityContext context){
        userContext.set(context);
    }

    public IdentityContext get(){
        return userContext.get();
    }
}
