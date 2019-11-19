package azpubsub.kafka.security.auth;

/**
 * Exception class, thrown when token is expired.
 */
public class TokenExpiredException extends  Exception {

    public TokenExpiredException(String error) {
        super(error);
    }

    public TokenExpiredException(String error, Throwable c) {
        super(error, c);
    }
}
