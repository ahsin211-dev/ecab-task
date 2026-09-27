package com.example.ridematching.api;

import com.example.ridematching.api.dto.ErrorResponse;
import com.example.ridematching.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Clock;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String VALIDATION_ERROR = "VALIDATION_ERROR";

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    // ---- 400: the request itself is wrong ----

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleInvalidBody(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, VALIDATION_ERROR, message);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleInvalidParameters(HandlerMethodValidationException ex) {
        String message = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> result.getMethodParameter().getParameterName() + ": " + error.getDefaultMessage()))
                .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, VALIDATION_ERROR, message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex) {
        return error(HttpStatus.BAD_REQUEST, VALIDATION_ERROR, "Missing required parameter '" + ex.getParameterName() + "'");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return error(HttpStatus.BAD_REQUEST, VALIDATION_ERROR, "Parameter '" + ex.getName() + "' has an invalid value");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return error(HttpStatus.BAD_REQUEST, VALIDATION_ERROR, "Request body is missing or is not valid JSON");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return error(HttpStatus.BAD_REQUEST, VALIDATION_ERROR, ex.getMessage());
    }

    // ---- 403 / 404 / 409: business rules ----

    @ExceptionHandler(RideAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(RideAccessDeniedException ex) {
        return businessError(HttpStatus.FORBIDDEN, "RIDE_ACCESS_DENIED", ex);
    }

    @ExceptionHandler({RideNotFoundException.class, DriverNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(RideMatchingException ex) {
        return businessError(HttpStatus.NOT_FOUND, "NOT_FOUND", ex);
    }

    @ExceptionHandler(NoDriverAvailableException.class)
    public ResponseEntity<ErrorResponse> handleNoDriver(NoDriverAvailableException ex) {
        return businessError(HttpStatus.CONFLICT, "NO_DRIVER_AVAILABLE", ex);
    }

    @ExceptionHandler(RiderHasActiveRideException.class)
    public ResponseEntity<ErrorResponse> handleActiveRide(RiderHasActiveRideException ex) {
        return businessError(HttpStatus.CONFLICT, "RIDER_HAS_ACTIVE_RIDE", ex);
    }

    @ExceptionHandler(RideAlreadyCompletedException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyCompleted(RideAlreadyCompletedException ex) {
        return businessError(HttpStatus.CONFLICT, "RIDE_ALREADY_COMPLETED", ex);
    }

    @ExceptionHandler(DriverOnRideException.class)
    public ResponseEntity<ErrorResponse> handleDriverOnRide(DriverOnRideException ex) {
        return businessError(HttpStatus.CONFLICT, "DRIVER_ON_RIDE", ex);
    }

    // ---- everything else ----

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        // Spring's own MVC exceptions (unknown URL, wrong HTTP method, bad media type) already know
        // their status; without this they would all be reported as 500.
        if (ex instanceof org.springframework.web.ErrorResponse springError) {
            HttpStatusCode status = springError.getStatusCode();
            HttpStatus known = HttpStatus.resolve(status.value());
            String code = known != null ? known.name() : "HTTP_" + status.value();
            return error(status, code, springError.getBody().getDetail());
        }

        log.error("Unhandled exception", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Something went wrong on our side");
    }

    private ResponseEntity<ErrorResponse> businessError(HttpStatus status, String code, RideMatchingException ex) {
        log.debug("Rejected request with {}: {}", code, ex.getMessage());
        return error(status, code, ex.getMessage());
    }

    private ResponseEntity<ErrorResponse> error(HttpStatusCode status, String code, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(code, message, clock.instant()));
    }
}
