package org.apache.kafka.common.security.dsts;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import java.util.Map;
import org.apache.kafka.common.security.dsts.*;

public class DstsLoginModule implements LoginModule{
    static {
        DstsSaslServerProvider.initialize();
        DstsSaslClientProvider.initialize();
    }

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {

    }

    @Override
    public boolean login() throws LoginException {
        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        return true;
    }

    @Override
    public boolean commit() throws LoginException {
        return true;
    }

    @Override
    public boolean abort() throws LoginException {
        return false;
    }
}
