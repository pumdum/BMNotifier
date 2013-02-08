package eu.anasta.bm.notifier.mail;

import javax.mail.Message;
import javax.mail.MessagingException;

import eu.anasta.bm.notifier.ui.Application;
import eu.anasta.bm.notifier.ui.Notification;
import eu.anasta.bm.notifier.ui.Notification.TRAY_TYPE;

/**
 * 
 * @author Mo Firouz
 * @since 16/10/11
 */
public class UnreadMailState {




	public static void check() {
		try {
			if (Application.getInstance()==null || Application.getInstance().getMailManager()==null)
				return;
			JavaPushMailAccount mail = Application.getInstance().getMailManager().getAccount();
			Message[] msgs = mail.getUnreadMessages();
			int nbUnreadmail = msgs.length;
			if (nbUnreadmail > 0) {
			Notification.getInstance().trayChange(TRAY_TYPE.NEW_MESSAGE);
//			String tooltip="";
//			for (Message msg: msgs){
//				String from =(msg.getFrom().length>0)?msg.getFrom()[0].toString():""; 
//				tooltip+= "  "+msg.getSubject().substring(0, 20)+"...\n";
//			}
			Notification.getInstance().setTooltip(nbUnreadmail + " unread mail");
				
			} else {
				Notification.getInstance().trayChange(TRAY_TYPE.CONNECTED);
				Notification.getInstance().setTooltip("BM Notifier");
			}
		} catch (MessagingException e) {
			e.printStackTrace();
		}
	}

}
