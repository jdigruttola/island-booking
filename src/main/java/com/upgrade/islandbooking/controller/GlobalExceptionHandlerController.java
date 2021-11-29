package com.upgrade.islandbooking.controller;

import com.upgrade.islandbooking.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
@PropertySource("classpath:error.properties")
public class GlobalExceptionHandlerController {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandlerController.class);

    private final Environment environment;

    public GlobalExceptionHandlerController(Environment environment) {
        this.environment = environment;
    }

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<Object> businessExceptionHandler(ServiceException e) {
        LOGGER.debug("Handling Business Exception with code label " + e.getCodeLabel() );

        // Since HTTP code is optional, checks if it was specified
        HttpStatus httpStatus;
        String httpCode = environment.getProperty(e.getHttpCodeLabel());
        if(httpCode != null) {
            httpStatus = HttpStatus.resolve(Integer.parseInt(httpCode));
            LOGGER.debug("The exception contains a custom HTTP status: " + httpStatus.toString());
        } else {
            httpStatus = HttpStatus.BAD_REQUEST;
            LOGGER.debug("The exception does not contain a specific HTTP status. Using the default: " + httpStatus.toString());
        }

        // Gets mandatory fields
        String code = environment.getProperty(e.getCodeLabel());
        String message = environment.getProperty(e.getMessageLabel());

        //Checks if the message has dynamic parameters
        if(e.getParameters() != null  && e.getParameters().length > 0) {
            LOGGER.debug("The exception has parameters to add in the error message. Length: " + e.getParameters().length);
            MessageFormat messageFormat = new MessageFormat(message);
            message = messageFormat.format(e.getParameters());
        }

        ErrorInfo errorInfo = new ErrorInfo(code, message, httpStatus);
        LOGGER.error(errorInfo.toString());


        return new ResponseEntity<>(errorInfo, httpStatus);
    }

    @ExceptionHandler(value = {BindException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<Object> handleBindException(BindException ex) {
        Map<String, List<String>> body = new HashMap<>();

        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.toList());

        body.put("errors", errors);

        return new ResponseEntity(body, HttpStatus.BAD_REQUEST);
    }

    public final class ErrorInfo {
        private final LocalDateTime localDateTime;
        private final String internalErrorCode;
        private final String message;
        private final String httpStatus;

        public ErrorInfo(String internalErrorCode, String message, HttpStatus httpStatus) {
            this.localDateTime = LocalDateTime.now();
            this.internalErrorCode = internalErrorCode;
            this.message = message;
            this.httpStatus = httpStatus.toString();
        }

        public String getInternalErrorCode() {
            return internalErrorCode;
        }

        public String getMessage() {
            return message;
        }

        public String getHttpStatus() {
            return httpStatus;
        }

        public LocalDateTime getLocalDateTime() {
            return localDateTime;
        }

        @Override
        public String toString() {
            return "ErrorInfo{" +
                    "localDateTime=" + localDateTime +
                    ", internalErrorCode='" + internalErrorCode + '\'' +
                    ", message='" + message + '\'' +
                    ", httpStatus='" + httpStatus + '\'' +
                    '}';
        }
    }
}
