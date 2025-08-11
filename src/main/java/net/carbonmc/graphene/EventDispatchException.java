package net.carbonmc.graphene;

public class EventDispatchException extends RuntimeException {
    public EventDispatchException(String message, Throwable cause) {
        super(message, cause);
    }
}