package azpubsub.kafka.security.auth;

public class NoClaimInTokenException extends Exception {
    public NoClaimInTokenException(String error) {
        super(error);
    }

    public NoClaimInTokenException(String error, Throwable c) {
        super(error, c);
    }
}
