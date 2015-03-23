package wistron.pushmessage.client;

import java.io.IOException;

import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.sasl.SASLMechanism;

public class XMessengerOAuth2 extends SASLMechanism {

    public XMessengerOAuth2(SASLAuthentication saslAuthentication) {
        super(saslAuthentication);
    }

    @Override
    protected String getName() {
        // TODO Auto-generated method stub
        return "X-MESSENGER-OAUTH2";
    }

    @Override
    protected void authenticate() throws IOException, XMPPException {
        // TODO Auto-generated method stub
        try {
            // Just return the oauth access token
            String authenticationText = this.password;
            getSASLAuthentication().send(
                    new AuthMechanism(getName(), authenticationText));
        } catch (Exception e) {
            throw new XMPPException("SASL authentication failed", e);
        }
    }

}
