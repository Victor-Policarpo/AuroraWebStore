package com.webstore.commerce.exception;

import com.webstore.commerce.exception.errors.BadCredentialsException;
import com.webstore.commerce.exception.errors.DisabledException;
import com.webstore.commerce.exception.errors.ExternalApiAuthenticationException;
import com.webstore.commerce.exception.errors.ExternalApiException;
import com.webstore.commerce.exception.errors.ExternalApiRateLimitException;
import com.webstore.commerce.exception.errors.ResourceAlreadyExistsException;
import com.webstore.commerce.exception.errors.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<StandardError> resourceNotFound(ResourceNotFoundException e, HttpServletRequest http){
        StandardError err = new StandardError();
        err.setTimestamp(Instant.now());
        err.setStatus(HttpStatus.NOT_FOUND.value());
        err.setError("Resource not found");
        err.setMessage(e.getMessage());
        err.setPath(http.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err);
    }

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<StandardError> userAlreadyExistsException(ResourceAlreadyExistsException e, HttpServletRequest http){
        StandardError err = new StandardError();
        err.setTimestamp(Instant.now());
        err.setStatus(HttpStatus.CONFLICT.value());
        err.setMessage(e.getMessage());
        err.setError("Conflict");
        err.setPath(http.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(err);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<StandardError> badCredentialsException(BadCredentialsException e, HttpServletRequest http){
        StandardError err = new StandardError();
        err.setTimestamp(Instant.now());
        err.setStatus(HttpStatus.UNAUTHORIZED.value());
        err.setError("Unauthorized");
        err.setMessage(e.getMessage());
        err.setPath(http.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationError> methodArgumentNotValidException(MethodArgumentNotValidException e, HttpServletRequest http){
        ValidationError err = new ValidationError();
        err.setTimestamp(Instant.now());
        err.setStatus(HttpStatus.BAD_REQUEST.value());
        err.setError("Validation Error");
        err.setMessage("One or more validation erros");
        err.setPath(http.getRequestURI());
        for (FieldError f: e.getBindingResult().getFieldErrors()){
            err.addError(f.getField(), f.getDefaultMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
    }


    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<StandardError> disabledException(DisabledException e, HttpServletRequest http){
        StandardError err = new StandardError();
        err.setTimestamp(Instant.now());
        err.setStatus(HttpStatus.UNAUTHORIZED.value());
        err.setMessage(e.getMessage());
        err.setError("Account Disabled");
        err.setPath(http.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
    }

    @ExceptionHandler(ExternalApiRateLimitException.class)
    public ResponseEntity<StandardError> externalApiRateLimit(ExternalApiRateLimitException e, HttpServletRequest http){
        StandardError err = new StandardError();
        err.setTimestamp(Instant.now());
        err.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        err.setError("External API rate limit exceeded");
        err.setMessage(e.getMessage());
        err.setPath(http.getRequestURI());

        ResponseEntity.BodyBuilder response = ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS);
        Long retryAfterSeconds = e.getRetryAfterSeconds();
        if (retryAfterSeconds != null && retryAfterSeconds > 0) {
            response.header(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds));
        }
        return response.body(err);
    }

    @ExceptionHandler(ExternalApiAuthenticationException.class)
    public ResponseEntity<StandardError> externalApiAuthentication(ExternalApiAuthenticationException e, HttpServletRequest http){
        return externalApiError(HttpStatus.BAD_GATEWAY, "External API authentication failed", e, http);
    }

    @ExceptionHandler(ExternalApiException.class)
    public ResponseEntity<StandardError> externalApiError(ExternalApiException e, HttpServletRequest http){
        return externalApiError(HttpStatus.BAD_GATEWAY, "External API error", e, http);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ValidationError> handlerMethodValidation(HandlerMethodValidationException e, HttpServletRequest http){
        ValidationError err = new ValidationError();
        err.setTimestamp(Instant.now());
        err.setStatus(HttpStatus.BAD_REQUEST.value());
        err.setError("Validation Error");
        err.setMessage("One or more validation erros");
        err.setPath(http.getRequestURI());
        for (ParameterValidationResult result : e.getParameterValidationResults()) {
            String field = result.getMethodParameter().getParameterName();
            for (MessageSourceResolvable resolvable : result.getResolvableErrors()) {
                err.addError(field, resolvable.getDefaultMessage());
            }
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
    }

    private ResponseEntity<StandardError> externalApiError(
            HttpStatus status, String error, ExternalApiException e, HttpServletRequest http){
        StandardError err = new StandardError();
        err.setTimestamp(Instant.now());
        err.setStatus(status.value());
        err.setError(error);
        err.setMessage(e.getMessage());
        err.setPath(http.getRequestURI());
        return ResponseEntity.status(status).body(err);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<StandardError> handleMissingServletRequestParameter(
            MissingServletRequestParameterException e,
            HttpServletRequest http) {

        StandardError err = new StandardError();
        err.setTimestamp(Instant.now());
        err.setStatus(HttpStatus.BAD_REQUEST.value());
        err.setMessage("Required parameter '%s' is missing".formatted(e.getParameterName()));
        err.setError("Bad Request");
        err.setPath(http.getRequestURI());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
    }

}
