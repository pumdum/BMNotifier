package eu.anasta.bm.notifier.mail.app;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.event.MessageChangedEvent;
import javax.mail.event.MessageChangedListener;
import javax.mail.event.MessageCountEvent;
import javax.mail.event.MessageCountListener;

import org.apache.log4j.Logger;

import eu.anasta.bm.notifier.mail.JavaPushMailAccount;
import eu.anasta.bm.notifier.mail.UnreadMailState;
import eu.anasta.bm.notifier.ui.Notification;

/**
 * 
 * @author Mo Firouz
 * @revision Nico.Dumont
 * @since 2/10/11
 */
public class JavaPushMailNotifier {

	private static Logger LOG = Logger.getLogger(JavaPushMailNotifier.class); 
	private MessageCountListener messageCountListener;
	private MessageChangedListener messageChangedListener;
	private JavaPushMailAccount mail;

	public JavaPushMailNotifier(JavaPushMailAccount mail) {
		this.mail = mail;
		initialiseListeners();
		addListeners();
	}

	private void addListeners() {
		mail.setMessageCounterListerer(messageCountListener);
		mail.setMessageChangedListerer(messageChangedListener);
	}

	private void initialiseListeners() {
		
		messageCountListener = new MessageCountListener() {

			public void messagesAdded(final MessageCountEvent e) {
				try {
						showNotification(e.getMessages()[0]);
				} catch (MessagingException ex) {
					ex.printStackTrace();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}

			public void messagesRemoved(MessageCountEvent e) {
				try {
					//TODO make something ? 
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		};
		messageChangedListener = new MessageChangedListener() {

			public void messageChanged(MessageChangedEvent e) {
				try {
					LOG.debug("one message change check unread mail");
					UnreadMailState.check();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		};
	
	}

	private void showNotification(Message message) throws MessagingException {
		Notification.getInstance().mailNotification(message, mail);
	}


}
