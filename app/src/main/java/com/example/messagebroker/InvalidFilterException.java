package com.example.messagebroker;

/**
 * Exception thrown when an invalid {@link Filter} object is being constructed by the user.
 *
 * @deprecated Instead an {@link IllegalArgumentException} is thrown.
 * See {@link Filter.Builder#build()}
 */
@Deprecated
public class InvalidFilterException extends MessageBrokerException {
    InvalidFilterException() {
        super("Not all filter arguments can be null");
    }
}