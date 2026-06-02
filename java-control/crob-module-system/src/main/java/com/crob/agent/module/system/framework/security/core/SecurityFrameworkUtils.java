package com.crob.agent.module.system.framework.security.core;

public class SecurityFrameworkUtils {

    private static final ThreadLocal<LoginUser> LOGIN_USER = new ThreadLocal<>();

    public static void setLoginUser(LoginUser user) {
        LOGIN_USER.set(user);
    }

    public static LoginUser getLoginUser() {
        return LOGIN_USER.get();
    }

    public static Long getLoginUserId() {
        LoginUser user = getLoginUser();
        return user != null ? user.getId() : null;
    }

    public static String getLoginUsername() {
        LoginUser user = getLoginUser();
        return user != null ? user.getUsername() : null;
    }

    public static void clear() {
        LOGIN_USER.remove();
    }
}
