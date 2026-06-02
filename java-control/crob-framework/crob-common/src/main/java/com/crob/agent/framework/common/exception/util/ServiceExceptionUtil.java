package com.crob.agent.framework.common.exception.util;

public class ServiceExceptionUtil {

    public static ServiceException exception(int code) {
        return new ServiceException(code);
    }

    public static ServiceException exception(int code, String message) {
        return new ServiceException(code, message);
    }

    public static class ServiceException extends RuntimeException {

        private final int code;

        public ServiceException(int code) {
            super("Service error: " + code);
            this.code = code;
        }

        public ServiceException(int code, String message) {
            super(message);
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }
}
