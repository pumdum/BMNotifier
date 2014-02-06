package eu.anasta.bm.notifier.im;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.XMPPException.ErrorCondition;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.observer.Listener;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule.MessageEvent;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule.PresenceEvent;
import tigase.jaxmpp.core.client.xmpp.stanzas.Presence.Show;
import tigase.jaxmpp.core.client.xmpp.stanzas.StanzaType;
import tigase.jaxmpp.j2se.Jaxmpp;
import eu.anasta.bm.notifier.ui.Notification;

public abstract class XmppManager {
	public enum Presence {
		away, online, dnd
	}

	boolean chatOpen = false;

	boolean connected = false;

	public XmppManager() {
		// TODO Auto-generated constructor stub
	}

	private Jaxmpp jaxmpp;

	public void start(final String user, String password, String host) {
		try {
			jaxmpp = new Jaxmpp();
			jaxmpp.getModulesManager()
					.getModule(PresenceModule.class)
					.addListener(PresenceModule.ContactChangedPresence,
							new Listener<PresenceModule.PresenceEvent>() {

								@Override
								public void handleEvent(PresenceEvent be)
										throws JaxmppException {
									boolean im = (be.getJid()!=null && be.getJid().getResource()!=null && be.getJid().getResource()
											.startsWith("Blue-Mind-IM_"));
									boolean isMe = user.equals(be.getJid()
											.getBareJid().toString());
									boolean offline = StanzaType.unavailable
											.equals(be.getPresence().getType());
									if (im && isMe) {
										System.out.println("chat open : "+ !offline
												);
										chatOpen = !offline;
									}
								}
							});
			jaxmpp.addListener(MessageModule.MessageReceived,
					new Listener<MessageModule.MessageEvent>() {

						@Override
						public void handleEvent(MessageEvent be)
								throws JaxmppException {
							if (!chatOpen)
								if (be.getChat() != null) {
									ErrorCondition err = be.getMessage()
											.getErrorCondition();
									if (err == null) {
										Notification.getInstance()
												.xmppNotification(
														be.getMessage());
									}
								}
						}
					});
//			jaxmpp.getConnectionConfiguration().setServer(host);
//			jaxmpp.getConnectionConfiguration().setUserJID(
//					BareJID.bareJIDInstance(user));
//			jaxmpp.getConnectionConfiguration().setUserPassword(password);
            jaxmpp.getProperties().setUserProperty( SessionObject.SERVER_NAME, host);
            jaxmpp.getProperties().setUserProperty( SessionObject.USER_BARE_JID, BareJID.bareJIDInstance( user ) );
            jaxmpp.getProperties().setUserProperty( SessionObject.PASSWORD, password );

			jaxmpp.login();
			connected = true;
		} catch (Exception e) {
			onAuthFail(e);
			e.printStackTrace();
		}
	}

	private void close() {
		try {
			if (connected) {
				jaxmpp.disconnect();
				onDisconnect();
			}
		} catch (JaxmppException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected abstract void onDisconnect();

	protected abstract void onPresenceChange(Presence p);

	protected abstract void onAuthFail(Exception e);

	public void stopPlanner() {
		close();
	}

	public void changePresence(Presence p) {
		try {
			switch (p) {
			case away:
				jaxmpp.getPresence().setPresence(Show.away, null, 0);
				break;
			case dnd:
				jaxmpp.getPresence().setPresence(Show.dnd, null, 0);
				break;
			case online:
				jaxmpp.getPresence().setPresence(Show.online, null, 0);
				break;
			default:
				break;
			}
			onPresenceChange(p);
		} catch (XMLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JaxmppException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
