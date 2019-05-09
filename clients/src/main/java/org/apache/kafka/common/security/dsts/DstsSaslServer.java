package org.apache.kafka.common.security.dsts;

import org.apache.kafka.common.errors.IllegalSaslStateException;
import org.apache.kafka.common.errors.SaslAuthenticationException;
import org.apache.kafka.common.security.scram.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import javax.security.sasl.SaslServerFactory;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 * Dsts SaslServer implementation for SASL/DSTS.
 * <p>
 * DSTS is a token based authentication method supported in Microsoft internally
 *
 */
public class DstsSaslServer implements SaslServer {
    private static final Logger log = LoggerFactory.getLogger(DstsSaslServer.class);

    enum State {
        RECEIVE_CLIENT_FIRST_MESSAGE,
        COMPLETE,
        FAILED
    };

    private State state;

    public DstsSaslServer() {
        setState(DstsSaslServer.State.RECEIVE_CLIENT_FIRST_MESSAGE);
    }

    /**
     * @throws SaslAuthenticationException if the requested authorization id is not the same as username.
     * <p>
     * <b>Note:</b> This method may throw {@link SaslAuthenticationException} to provide custom error messages
     * to clients. But care should be taken to avoid including any information in the exception message that
     * should not be leaked to unauthenticated clients. It may be safer to throw {@link SaslException} in
     * most cases so that a standard error message is returned to clients.
     * </p>
     */
    @Override
    public byte[] evaluateResponse(byte[] response) throws SaslException, SaslAuthenticationException {
            switch (state) {
                case RECEIVE_CLIENT_FIRST_MESSAGE:
                    setState(State.COMPLETE);
                    return new byte[0];
                default:
                    throw new IllegalSaslStateException("Unexpected challenge in Sasl server state " + state);
            }
    }

    private void setState(State state) {
        log.debug("Setting SASL/DSTS server state to {}", state);
        this.state = state;
    }

    @Override
    public String getMechanismName(){
        return "DSTS";
    }

    @Override
    public boolean isComplete(){
        return state==State.COMPLETE;
    }

    @Override
    public String getAuthorizationID(){
        return "testID";
    }

    @Override
    public byte[] unwrap(byte[] incoming, int offset, int len){
        return Arrays.copyOfRange(incoming, offset, offset + len);
    }

    @Override
    public byte[] wrap(byte[] outgoing, int offset, int len){
        return Arrays.copyOfRange(outgoing, offset, offset + len);
    }

    @Override
    public Object getNegotiatedProperty(String propName){
        return null;
    }

    @Override
    public void dispose(){
    }

    public static class DstsSaslServerFactory implements SaslServerFactory {
        @Override
        public SaslServer createSaslServer(String mechanism, String protocol, String serverName, Map<String, ?> props, CallbackHandler cbh)
                throws SaslException {
            return new DstsSaslServer();
        }

        @Override
        public String[] getMechanismNames(Map<String, ?> props) {
            return new String[]{"DSTS"};
        }
    }
}
