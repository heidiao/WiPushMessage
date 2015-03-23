package wistron.pushmessage.client;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

public class MsnXMPPClient {

    public static final int miPort = 5222;
    // public static final String Host = "xmpp.messenger.live.com";
    public static final String mstrHost = "BY2MSG3020306.gateway.edge.messenger.live.com";
    public static final String mstrService = "messenger.live.com";

    /**
     * This block initializes asmack with SASL mechanism used by Windows Live.
     */
    static {
        SASLAuthentication.registerSASLMechanism("X-MESSENGER-OAUTH2",
                XMessengerOAuth2.class);
        SASLAuthentication.supportSASLMechanism("X-MESSENGER-OAUTH2");
    }
    private XMPPConnection mConnection;
    private Handler mHandler;

    private String mstrAccessToken;

    public MsnXMPPClient(String strAccessToken) {
        this.mstrAccessToken = strAccessToken;
    }

    /**
     * Get the Jid for this client instance.
     */
    public String getLocalJid() {
        return StringUtils.parseBareAddress(this.mConnection.getUser());
    }

    /**
     * Get the Roster for this client instance.
     * 
     * @return The full Roster for the client.
     */
    public Roster getRoster() {
        return this.mConnection.getRoster();
    }

    String mUser = new String("");
    public String getUser() {
//        if (null == mConnection)
//            return null;
//        RosterEntry entry = this.getRoster().getEntry(this.getLocalJid());
//        return StringUtils.parseBareAddress(mConnection.getUser());
        return mUser;
    }
    
    public void setUser(String User) {
        this.mUser = User;
    }

    public boolean isAuthenticated() {
        if (null == mConnection)
            return false;
        return mConnection.isAuthenticated();
    }

    public boolean isConnected() {
        if (null == mConnection)
            return false;
        return mConnection.isConnected();
    }

    /**
     * Login to Server
     */
    public void Login() {

        XMPPConnection.DEBUG_ENABLED = true;
        ConnectionConfiguration connConfig = new ConnectionConfiguration(
                MsnXMPPClient.mstrHost, MsnXMPPClient.miPort,
                MsnXMPPClient.mstrService);

        connConfig.setRosterLoadedAtLogin(true);

        this.mConnection = new XMPPConnection(connConfig);

        Log.i("MsnXMPPClient", "newXMPPConnection");
        try {
            Log.i("MsnXMPPClient", "connect before");
            this.mConnection.connect();
            Log.i("MsnXMPPClient", "connect after");
            Log.i("MsnXMPPClient", "Login Before");

            if (this.mConnection.isConnected()) {

                this.mConnection
                        .addConnectionListener(new ConnectionListener() {

                            @Override
                            public void connectionClosed() {
                                Log.i("ConnectionListener", "connectionClosed");
                                android.os.Message msg = mHandler
                                        .obtainMessage(
                                                IMCoreService.MSG_XMPP_DISCONNECTED,
                                                new String("connectionClosed"));
                                Bundle bundle = new Bundle();
                                bundle.putString("user", mUser);
                                msg.setData(bundle );
                                mHandler.sendMessage(msg);
                            }

                            @Override
                            public void connectionClosedOnError(Exception arg0) {
                                Log.i("ConnectionListener",
                                        "connectionClosedOnError");
                                android.os.Message msg = mHandler
                                        .obtainMessage(
                                                IMCoreService.MSG_XMPP_DISCONNECTED,
                                                new String(
                                                        "connectionClosedOnError"));
                                Bundle bundle = new Bundle();
                                bundle.putString("user", mUser);
                                msg.setData(bundle );
                                mHandler.sendMessage(msg);
                            }

                            @Override
                            public void reconnectingIn(int arg0) {
                                Log.i("ConnectionListener", "reconnectingIn:"
                                        + arg0);
                                android.os.Message msg = mHandler
                                        .obtainMessage(
                                                IMCoreService.MSG_XMPP_DISCONNECTED,
                                                new String("reconnectingIn"
                                                        + arg0));
                                Bundle bundle = new Bundle();
                                bundle.putString("user", mUser);
                                msg.setData(bundle );
                                mHandler.sendMessage(msg);
                            }

                            @Override
                            public void reconnectionFailed(Exception arg0) {
                                Log.i("ConnectionListener",
                                        "reconnectionFailed:" + arg0);

                                android.os.Message msg = mHandler
                                        .obtainMessage(
                                                IMCoreService.MSG_XMPP_DISCONNECTED,
                                                new String("reconnectionFailed"
                                                        + arg0));
                                Bundle bundle = new Bundle();
                                bundle.putString("user", mUser);
                                msg.setData(bundle );
                                mHandler.sendMessage(msg);
                            }

                            @Override
                            public void reconnectionSuccessful() {
                                Log.i("ConnectionListener",
                                        "reconnectionSuccessful:");

                                android.os.Message msg = mHandler
                                        .obtainMessage(
                                                IMCoreService.MSG_XMPP_DISCONNECTED,
                                                new String(
                                                        "reconnectionSuccessful"));
                                Bundle bundle = new Bundle();
                                bundle.putString("user", mUser);
                                msg.setData(bundle );
                                mHandler.sendMessage(msg);
                            }

                        });
            }
            // We do not need user name in this case.
            this.mConnection.login("", this.mstrAccessToken);
            // this.connection.login("peterlu1204", "google27639871");
            Log.i("MsnXMPPClient", "Login After");
        } catch (XMPPException ex) {
            Log.e("MsnXMPPClient", ex.toString());
            this.mConnection = null;
            return;
        }

        // set the message and presence handlers
        this.setPacketFilters();

        // This will set up the roster detail
        // this.activity.logInComplete();

        // Set the status to available
        Presence presence = new Presence(Presence.Type.available);
        presence.setMode(Presence.Mode.available);
        this.mConnection.sendPacket(presence);

    }

    public void Logout() {
        this.mConnection.disconnect();
    }

    /**
     * Send a text message to the buddy
     * 
     * @param to
     *            The Buddy Jid
     * @param text
     *            The text message to be sent
     */
    public void sendMessage(String to, String text) {
        Message msg = new Message(to, Message.Type.chat);
        msg.setBody(text);
        this.mConnection.sendPacket(msg);
    }

    public void setHandler(Handler handler) {
        mHandler = handler;
    }

    /**
     * Set the packet filters for handling incoming stanzas.
     */
    private void setPacketFilters() {
        if (this.mConnection != null) {
            PacketFilter presenceFilter = new PacketTypeFilter(Presence.class);
            this.mConnection.addPacketListener(new PacketListener() {
                public void processPacket(Packet packet) {
                    // Presence presence = (Presence) packet;
                    // activity.handlePresenceReceived(presence);
                }
            }, presenceFilter);

            PacketFilter messageFilter = new MessageTypeFilter(
                    Message.Type.chat);
            this.mConnection.addPacketListener(new PacketListener() {
                public void processPacket(Packet packet) {
                    Message message = (Message) packet;
                    if (message.getBody() != null) {
                        // activity.handleMessageReceived(message);
                        String fromJId = StringUtils.parseBareAddress(message
                                .getFrom());
                        Roster roster = MsnXMPPClient.this.getRoster();
                        RosterEntry entry = roster.getEntry(fromJId);
                        String Name = new String(entry.getName());
                        android.os.Message msg = mHandler.obtainMessage(
                                IMCoreService.MSG_XMPP_GOT_MESSAGE, Name);
                        mHandler.sendMessage(msg);
                    }
                }
            }, messageFilter);
        }
    }

}
