package azpubsub.kafka.security.auth;

/**
 * Exception class, thrown when token is invalid:
 *          signature validation failed, etc.
 */
public class InvalidTokenException extends Exception {
    private String errorMessage;

    /**
     * Constructor
     * @param error
     */
    public InvalidTokenException(String error) {
        errorMessage = error;
    }

    /**
     * getting error message
     * @return error message
     */
    public String getMessage() {
        return errorMessage;
    }
}
