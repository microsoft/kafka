package azpubsub.kafka.security.auth;

public class InvalidTokenException extends Exception {

    public InvalidTokenException(String error) {
        super(error);
    }

    public InvalidTokenException(String error, Throwable c) {
        super(error, c);
    }
}
