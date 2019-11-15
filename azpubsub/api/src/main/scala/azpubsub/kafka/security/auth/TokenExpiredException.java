package azpubsub.kafka.security.auth;

/**
 * Exception class, thrown when token is expired.
 */
public class TokenExpiredException extends  Exception {
    private String errorMessage;

    /**
     * Constructor
     * @param error
     */
    public TokenExpiredException(String error) {
        errorMessage = error;
    }

    /**
     * get error message
     * @return error message
     */
    public String getMessage() {
        return errorMessage;
    }
}
