package eu.anasta.bm.notifier.ui;

import javax.mail.Message;
import javax.mail.MessagingException;

import net.bluemind.core.api.calendar.Occurrence;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import eu.anasta.bm.notifier.login.ClientFormLogin;
import eu.anasta.bm.notifier.mail.JavaPushMailAccount;
import eu.anasta.bm.notifier.ui.cache.ImageCache;
import eu.anasta.bm.notifier.ui.notifier.NotificationType;
import eu.anasta.bm.notifier.ui.notifier.NotifierDialog;
import eu.anasta.bm.notifier.ui.notifier.SoundPlayer;

public class Notification {

	private static Notification instance;
	public enum TRAY_TYPE{
		NEW_MESSAGE,
		CONNECTED,
		DISCONNECTED,ERROR
	}

	public static Notification getInstance() {
		if (instance == null) {
			instance = new Notification();
		}
		return instance;
	}
	
	private String buildOpenMailUrl(String host, String uid, String folder){
		return " <a href=\"https://"
				+ host
				+ "/webmail/?BMHPS="+ClientFormLogin.getInstance().login()+"&_task=mail&_action=show&_uid="
				+ uid
				+ "&_mbox="+folder+"\">open</a>";
	}
	
	public void eventNotification(){
		
	}

	public void mailNotification(Message message,
			final JavaPushMailAccount mail) {
		if (message == null) {
			return;
		}
		;
		try {
			String tmpfrom = message.getFrom()[0].toString();
			final String from = (tmpfrom.contains("<") && tmpfrom.contains(">"))?tmpfrom.substring(0, tmpfrom.indexOf("<")):tmpfrom;
			final String subject = message.getSubject().trim();
			final long uid = mail.getFolder().getUID(message);
			final String url = buildOpenMailUrl(mail.getServerAddress(), Long.toString(uid), mail.getFolder().getName()); 
			Display.getDefault().syncExec(new Runnable() {

				@Override
				public void run() {
					// TODO externalyse alert
					NotifierDialog
							.notify(from,
									subject,
									NotificationType.MAIL,
									url);

				}
			});
			SoundPlayer.playSound();
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void trayChange(TRAY_TYPE type){
		if (Application.getInstance()==null || Application.getInstance().getTrayicon()==null)
			return;
		switch (type) {
		case CONNECTED:
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					final Image imageMail = ImageCache
							.getImage(ImageCache.LITLLE_BM);
					if (Application.getInstance().getTrayicon().isDisposed()) return;
					Application.getInstance().getTrayicon().setImage(imageMail);
				}
			});

			break;
		case DISCONNECTED:
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					final Image imageMail = ImageCache
							.getImage(ImageCache.LITLLE_WARNING);
					if (Application.getInstance().getTrayicon().isDisposed()) return;
					Application.getInstance().getTrayicon().setImage(imageMail);
				}
			});

			break;
		case NEW_MESSAGE:
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					final Image imageMail = ImageCache
							.getImage(ImageCache.LITLLE_MAIL);
					if (Application.getInstance().getTrayicon().isDisposed()) return;
					Application.getInstance().getTrayicon().setImage(imageMail);
				}
			});
			break;
		case ERROR:
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					final Image imageMail = ImageCache
							.getImage(ImageCache.LITLLE_ERROR);
					if (Application.getInstance().getTrayicon().isDisposed()) return;
					Application.getInstance().getTrayicon().setImage(imageMail);
				}
			});
break;
		default:
			break;
		}
	}

	public void eventNotification(final Occurrence occurence) {
		Display.getDefault().asyncExec(new Runnable() {
			
			@Override
			public void run() {
				Application.getInstance().getWindowEventNotif().open();
				Application.getInstance().getWindowEventNotif().addOccurence(occurence);
				
			}
		});
		System.out.println("Reminder");
		SoundPlayer.playSound();
	}

	public void setTooltip(final String tooltip) {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				if (Application.getInstance().getTrayicon().isDisposed()) return;
				Application.getInstance().getTrayicon().setToolTipText(tooltip);
			}
		});
		
	}
}
