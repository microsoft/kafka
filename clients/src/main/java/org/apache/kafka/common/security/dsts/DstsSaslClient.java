package org.apache.kafka.common.security.dsts;

import org.apache.kafka.common.errors.IllegalSaslStateException;
import org.apache.kafka.common.metrics.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslClientFactory;
import javax.security.sasl.SaslException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import net.sf.jni4net.Bridge;
import java.io.*;
import sample.authentication.wcfsoapclient.*;

/**
 * SaslClient implementation for SASL/DSTS.
 *
 */
public class DstsSaslClient implements SaslClient{
    private static final Logger log = LoggerFactory.getLogger(DstsSaslClient.class);

    enum State {
        SEND_CLIENT_FIRST_MESSAGE,
        RECEIVE_SERVER_FINAL_MESSAGE,
        COMPLETE,
        FAILED
    };

    private State state;

    private void setState(State state) {
        log.debug("Setting SASL/DSTS client state to {}", state);
        this.state = state;
    }

    public DstsSaslClient(){
        setState(State.SEND_CLIENT_FIRST_MESSAGE);
    }

    @Override
    public byte[] evaluateChallenge(byte[] challenge) throws SaslException{
        try {
            switch (state) {
                case SEND_CLIENT_FIRST_MESSAGE:
                    if (challenge != null && challenge.length != 0)
                        throw new SaslException("Expected empty challenge");
                    setState(State.RECEIVE_SERVER_FINAL_MESSAGE);

                    // Set Token
                    try {
                        Bridge.setVerbose(true);

                        Bridge.init(new File("D:\\git\\kafka\\core\\build\\distributions\\kafka_2.11-1.1.1-SNAPSHOT\\libs"));
                        Bridge.LoadAndRegisterAssemblyFrom(new java.io.File("D:\\git\\kafka\\core\\build\\distributions\\kafka_2.11-1.1.1-SNAPSHOT\\libs", "WcfSoapClient.j4n.dll"));
                        system.Object token = WcfSoapClient.GetSecurityToken();
                        return token.toString().getBytes();
                    }catch (Exception ex){
                        ex.printStackTrace();
                    }
                    return new byte[0];
                case RECEIVE_SERVER_FINAL_MESSAGE:
                    setState(State.COMPLETE);
                    return null;
                default:
                    throw new IllegalSaslStateException("Unexpected challenge in Sasl client state " + state);
            }
        }catch (SaslException e) {
            setState(State.FAILED);
            throw e;
        }
    }

    @Override
    public boolean isComplete() {
        return state == State.COMPLETE;
    }

    @Override
    public byte[] unwrap(byte[] incoming, int offset, int len) throws SaslException {
        if (!isComplete())
            throw new IllegalStateException("Authentication exchange has not completed");
        return Arrays.copyOfRange(incoming, offset, offset + len);
    }

    @Override
    public byte[] wrap(byte[] outgoing, int offset, int len) throws SaslException {
        if (!isComplete())
            throw new IllegalStateException("Authentication exchange has not completed");
        return Arrays.copyOfRange(outgoing, offset, offset + len);
    }

    @Override
    public Object getNegotiatedProperty(String propName) {
        if (!isComplete())
            throw new IllegalStateException("Authentication exchange has not completed");
        return null;
    }

    @Override
    public void dispose() throws SaslException {
    }

    @Override
    public boolean hasInitialResponse() {
        return true;
    }

    @Override
    public String getMechanismName() {
        return "DSTS";
    }

    public static class DstsSaslClientFactory implements SaslClientFactory {

        @Override
        public SaslClient createSaslClient(String[] mechanisms,
                                           String authorizationId,
                                           String protocol,
                                           String serverName,
                                           Map<String, ?> props,
                                           CallbackHandler cbh) throws SaslException {

            return new DstsSaslClient();
        }

        @Override
        public String[] getMechanismNames(Map<String, ?> props) {
            return new String[]{"DSTS"};
        }
    }
}
