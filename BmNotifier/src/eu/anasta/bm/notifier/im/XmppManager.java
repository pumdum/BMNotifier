package eu.anasta.bm.notifier.im;

import tigase.jaxmpp.core.client.BareJID;
import tigase.jaxmpp.core.client.XMPPException.ErrorCondition;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.observer.Listener;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule;
import tigase.jaxmpp.core.client.xmpp.modules.chat.MessageModule.MessageEvent;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule;
import tigase.jaxmpp.core.client.xmpp.modules.presence.PresenceModule.PresenceEvent;
import tigase.jaxmpp.core.client.xmpp.stanzas.StanzaType;
import tigase.jaxmpp.j2se.Jaxmpp;
import eu.anasta.bm.notifier.ui.Notification;


public abstract class XmppManager {
	boolean chatOpen=false;
	
	public XmppManager() {
		// TODO Auto-generated constructor stub
	}

	private Jaxmpp jaxmpp;
	public void start(final String user, String password, String host) {
	       try{
	    	   jaxmpp = new Jaxmpp();
	               jaxmpp.getModulesManager().getModule( PresenceModule.class ).addListener( PresenceModule.ContactChangedPresence, new Listener<PresenceModule.PresenceEvent>() {
	       
	                   @Override
	                   public void handleEvent( PresenceEvent be ) throws JaxmppException {
	                	   boolean im = be.getJid().getResource().startsWith("Blue-Mind-IM_");
	                	   boolean isMe = user.equals( be.getJid().getBareJid().toString());
	                	   boolean offline =StanzaType.unavailable.equals(be.getPresence().getType() );
	                	   if (im && isMe){
	                		   chatOpen = !offline;
	                	   }
	                   }
	               } );
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
										.xmppNotification(be.getMessage());
	    							}
	    						}
	    					}
	    				});
	               jaxmpp.getConnectionConfiguration().setServer(host);
	               jaxmpp.getConnectionConfiguration().setUserJID(BareJID.bareJIDInstance( user ));
	               jaxmpp.getConnectionConfiguration().setUserPassword(password);
	               jaxmpp.login();
		}catch (Exception e){
			e.printStackTrace();
		}
	}

	private void close() {
	}

	protected abstract void onDisconnect();

	protected abstract void onAuthFail(Exception e);

	public void stopPlanner() {
		close();
	}
}
