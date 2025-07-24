package com.example.messagebroker;

/**
 * Gives information about errors.
 * Possible exception cases can be found in {@link Type}.
 */
public class MessageListenerException extends MessageBrokerException {
  private final Type type;

  MessageListenerException(Type type) {
    super(type.message);
    this.type = type;
  }

  /**
   * Get the error type.
   *
   * @return indicating the type of failure
   */
  public Type getType() {
    return type;
  }

  /**
   * Error type of an {@link MessageListenerException}.
   */
  public enum Type {
    LISTENER_NOT_SUBSCRIBED("Listener not found in the subscription list");

    private final String message;

    Type(String message) {
      this.message = message;
    }
  }
}

