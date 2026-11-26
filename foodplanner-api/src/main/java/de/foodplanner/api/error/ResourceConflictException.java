package de.foodplanner.api.error;

public class ResourceConflictException extends RuntimeException {

    private final String reason;

    public ResourceConflictException(String message) {
        this(message, (String) null);
    }

    public ResourceConflictException(String message, String reason) {
        super(message);
        this.reason = reason;
    }

    public ResourceConflictException(String message, Throwable cause) {
        super(message, cause);
        this.reason = null;
    }

    /** Maschinenlesbarer Grund, z. B. REFUSED_NO_PACKAGE_SIZE; kann fehlen. */
    public String getReason() {
        return reason;
    }
}