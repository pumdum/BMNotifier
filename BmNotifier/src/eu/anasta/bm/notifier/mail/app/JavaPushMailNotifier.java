package eu.anasta.bm.notifier.mail.app;

import javax.mail.Flags.Flag;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.event.MessageChangedEvent;
import javax.mail.event.MessageChangedListener;
import javax.mail.event.MessageCountEvent;
import javax.mail.event.MessageCountListener;

import eu.anasta.bm.notifier.mail.JavaPushMailAccount;
import eu.anasta.bm.notifier.ui.Notification;

/**
 * 
 * @author Mo Firouz
 * @since 2/10/11
 */
public class JavaPushMailNotifier {

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
					if (!isReminderMail(e.getMessages()[0])){
						showNotification(e.getMessages()[0]);
					}else{
						e.getMessages()[0].setFlag(Flag.SEEN, true);
					}
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
					if (e.getMessageChangeType()==MessageChangedEvent.FLAGS_CHANGED) {
//						printHeader(e.getMessage());
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		};
	}
	private boolean isReminderMail(Message m){
		try {
			String[] returnpath = m.getHeader("Return-Path");
			String[] subject = m.getHeader("Subject");
			if (returnpath!=null && subject!=null && returnpath.length==1 && subject.length==1 && returnpath[0].startsWith("<null@") && subject[0].startsWith("Rappel:")){
				return true;
			}
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
//	private void printHeader(Message e){
//		Enumeration<Header> headers;
//		try {
//			headers = e.getAllHeaders();
//			while (headers.hasMoreElements()) {
//				Header h = (Header) headers.nextElement();
//				System.out.println(h.getName() + ": " + h.getValue());
//			}
//		} catch (MessagingException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//			
//	}

	private void showNotification(Message message) throws MessagingException {
		Notification.getInstance().mailNotification(message, mail);
	}


}
