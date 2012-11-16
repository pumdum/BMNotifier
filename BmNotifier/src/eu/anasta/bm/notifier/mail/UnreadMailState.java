package eu.anasta.bm.notifier.mail;

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
			int nbUnreadmail = mail.getUnreadMessages().length;
			if (nbUnreadmail > 0) {
			Notification.getInstance().trayChange(TRAY_TYPE.NEW_MESSAGE);
				
			} else {
				Notification.getInstance().trayChange(TRAY_TYPE.CONNECTED);
			}
		} catch (MessagingException e) {
			e.printStackTrace();
		}
	}

}
