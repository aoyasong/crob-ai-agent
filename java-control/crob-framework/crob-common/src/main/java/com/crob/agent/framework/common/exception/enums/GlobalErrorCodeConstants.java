package com.crob.agent.framework.common.exception.enums;

public interface GlobalErrorCodeConstants {

    int SUCCESS = 0;
    int BAD_REQUEST = 400;
    int UNAUTHORIZED = 401;
    int FORBIDDEN = 403;
    int NOT_FOUND = 404;
    int INTERNAL_SERVER_ERROR = 500;

    int NOT_FOUND_VALUE = 1002007000;

    static void checkNotNull(Object obj, String message, Object... args) {
        if (obj == null) {
            throw new IllegalArgumentException(format(message, args));
        }
    }

    static String format(String template, Object... args) {
        if (args == null || args.length == 0) {
            return template;
        }
        String result = template;
        for (int i = 0; i < args.length; i++) {
            result = result.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return result;
    }
}
