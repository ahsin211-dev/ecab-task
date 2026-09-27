package com.example.ridematching.exception;

public class NoDriverAvailableException extends RideMatchingException {

    public NoDriverAvailableException() {
        super("No driver is available right now");
    }
}
