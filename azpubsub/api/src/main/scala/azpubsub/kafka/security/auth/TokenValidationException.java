package azpubsub.kafka.security.auth;


/**
 * Base class for token validation.
 */
public class TokenValidationException  extends Exception{
    private String errorMessage;
    private Throwable cause;

    public TokenValidationException(String error) {
        errorMessage = error;
    }

    public TokenValidationException(String error, Throwable c) {
        errorMessage = error;
        cause = c;
    }

    public TokenValidationException() {

    }
}
