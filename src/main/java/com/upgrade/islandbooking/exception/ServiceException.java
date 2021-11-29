package com.upgrade.islandbooking.exception;

public class ServiceException extends RuntimeException {

    private static final String LABEL_CODE = ".code";
    private static final String LABEL_MESSAGE = ".message";
    private static final String LABEL_HTTP_CODE = ".httpCode";

    private final Error error;
    private final Object[] parameters;

    public ServiceException(Error error) {
        super();
        this.error = error;
        this.parameters = null;
    }

    public ServiceException(Error error, Object[] parameters) {
        super();
        this.error = error;
        this.parameters = parameters;
    }

    public String getCodeLabel() {
        return error.getKey() + LABEL_CODE;
    }

    public String getMessageLabel() {
        return error.getKey() + LABEL_MESSAGE;
    }

    public String getHttpCodeLabel() {
        return error.getKey() + LABEL_HTTP_CODE;
    }

    public Object[] getParameters() {
        return parameters;
    }
}
