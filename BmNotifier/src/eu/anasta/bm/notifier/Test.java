package eu.anasta.bm.notifier;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.proxy.ProxyInfo;

public class Test {

	public static void main(String[] args) throws XMPPException {
		ConnectionConfiguration config = new ConnectionConfiguration("debian.anasta.eu", 5222,"anasta.eu",ProxyInfo.forDefaultProxy());
		config.setSecurityMode(ConnectionConfiguration.SecurityMode.enabled);
//		config.setSocketFactory(new DummySSLSocketFactory());
		Connection connection = new XMPPConnection(config);
		connection.connect();

		connection.login("n.dumont@anasta.eu", "test");
		Chat chat = connection.getChatManager().createChat("test@anasta.eu", new MessageListener() {

		    public void processMessage(Chat chat, Message message) {
		        System.out.println("Received message: " + message);
		    }

	
		});
		chat.sendMessage("Howdy!");
	}
	
	

}
