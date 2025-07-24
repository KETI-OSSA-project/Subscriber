package com.example.messagebroker;


/**
 * Base class for failing states when using the message broker api.
 */
public class MessageBrokerException extends Exception {
    MessageBrokerException(String message) {
        super(message);
    }
}
