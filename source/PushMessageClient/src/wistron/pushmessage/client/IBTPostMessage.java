package wistron.pushmessage.client;

public interface IBTPostMessage extends IPostMessage {
    void sendMessage(String to, String from);
    void sendLogin(String user);
    void sendLogout(String user);
}
