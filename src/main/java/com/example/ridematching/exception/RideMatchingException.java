package com.example.ridematching.exception;

/**
 * Base type for every business-rule violation, so the API layer can tell
 * expected failures apart from genuine bugs.
 */
public abstract class RideMatchingException extends RuntimeException {

    protected RideMatchingException(String message) {
        super(message);
    }
}
