package com.pokemon.battle.exception;

/** Expected failures that can be explained directly to the user. */
public class AppException extends RuntimeException {
    public AppException(String message) { super(message); }
    public AppException(String message, Throwable cause) { super(message, cause); }
}
