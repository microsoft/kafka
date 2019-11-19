package azpubsub.kafka.security.auth;

/**
 * Exception class, the token is not authenticated:
 *          Not a DSTS issued token
 */
public class AuthenticationFailedException extends TokenValidationException{

    public AuthenticationFailedException(String error)  {
        super(error);
    }

    public AuthenticationFailedException(String error, Throwable c) {
        super(error, c);
    }
}
