package eu.anasta.bm.notifier.ui;

import javax.mail.Message;
import javax.mail.MessagingException;

import net.bluemind.core.api.calendar.Occurrence;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import tigase.jaxmpp.core.client.xml.XMLException;
import eu.anasta.bm.notifier.login.ClientFormLogin;
import eu.anasta.bm.notifier.mail.JavaPushMailAccount;
import eu.anasta.bm.notifier.ui.cache.ImageCache;
import eu.anasta.bm.notifier.ui.notifier.NotifierDialog;
import eu.anasta.bm.notifier.ui.notifier.SoundPlayer;

public class Notification {

	public enum TRAY_TYPE {
		CONNECTED, DISCONNECTED, ERROR, NEW_MESSAGE
	}

	private static Notification instance;

	public static Notification getInstance() {
		if (instance == null) {
			instance = new Notification();
		}
		return instance;
	}

	private String buildOpenIMUrl() {
		return " <a href=\"" + getBaseUrl() + "im/?BMHPS="
				+ ClientFormLogin.getInstance().login() + "\">open</a>";
	}

	private String buildOpenMailUrl(String uid, String folder) {
		return " <a href=\"" + getBaseUrl() + "webmail/?BMHPS="
				+ ClientFormLogin.getInstance().login()
				+ "&_task=mail&_action=show&_uid=" + uid + "&_mbox=" + folder
				+ "\">open</a>";
	}

	public void eventNotification() {
	}

	public void eventNotification(final Occurrence occurence) {
		Display.getDefault().asyncExec(new Runnable() {

			@Override
			public void run() {
				Application.getInstance().getWindowEventNotif().open();
				Application.getInstance().getWindowEventNotif()
						.addOccurence(occurence);

			}
		});
		System.out.println("Reminder");
		SoundPlayer.playSound();
	}

	private String getBaseUrl() {
		String host;
		try {
			host = Application.getInstance().getActiveHost();
			return "https://" + host + "/";
		} catch (Exception e) {
			// TODO LOG
			e.printStackTrace();
			return "";
		}
	}

	public void mailNotification(Message message, final JavaPushMailAccount mail) {
		if (message == null) {
			return;
		}
		try {
			String tmpfrom = message.getFrom()[0].toString();
			final String from = (tmpfrom.contains("<") && tmpfrom.contains(">")) ? tmpfrom
					.substring(0, tmpfrom.indexOf("<")) : tmpfrom;
			final String subject = message.getSubject().trim();
			final long uid = mail.getFolder().getUID(message);
			final String url = buildOpenMailUrl(Long.toString(uid), mail
					.getFolder().getName());
			Display.getDefault().syncExec(new Runnable() {

				@Override
				public void run() {
					// TODO externalyse alert
					NotifierDialog.notify(from, subject, ImageCache.getImage(ImageCache.MAIL),
							url);

				}
			});
			SoundPlayer.playSound();
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void sendMailTo(String host, String dest) {
		String url = getBaseUrl() + "webmail/?BMHPS="
				+ ClientFormLogin.getInstance().login()
				+ "&_task=mail&_action=compose&_to=" + dest;
		org.eclipse.swt.program.Program.launch(url);
	}

	public void setTooltip(final String tooltip) {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				if (Application.getInstance().getTrayicon().isDisposed())
					return;
				Application.getInstance().getTrayicon().setToolTipText(tooltip);
			}
		});

	}

	public void trayChange(TRAY_TYPE type) {
		if (Application.getInstance() == null
				|| Application.getInstance().getTrayicon() == null)
			return;
		switch (type) {
		case CONNECTED:
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					Image imageMail = ImageCache
							.getImage(ImageCache.LITLLE_BM);
					if (Application.getInstance().getTrayicon().isDisposed())
						return;
					Application.getInstance().getTrayicon().setImage(imageMail);
				}
			});

			break;
		case DISCONNECTED:
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					Image imageMail = ImageCache
							.getImage(ImageCache.BM_STATUS_DISCONECT);
					if (Application.getInstance().getTrayicon().isDisposed())
						return;
					Application.getInstance().getTrayicon().setImage(imageMail);
				}
			});

			break;
		case NEW_MESSAGE:
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					if (Application.getInstance().getTrayicon().isDisposed())
						return;
					Image imageBm = ImageCache
							.getImage(ImageCache.LITLLE_MAIL);
					
					Application.getInstance().getTrayicon().setImage(imageBm);
				}
			});
			break;
		case ERROR:
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					Image imageMail = ImageCache
							.getImage(ImageCache.BM_STATUS_WARN);
					if (Application.getInstance().getTrayicon().isDisposed())
						return;
					Application.getInstance().getTrayicon().setImage(imageMail);
				}
			});
			break;
		default:
			break;
		}
	}

	public void xmppNotification(
			tigase.jaxmpp.core.client.xmpp.stanzas.Message message) {
		if (message == null) {
			return;
		}
		;
		String tmpfrom;
		try {
			tmpfrom = message.getFrom().getBareJid().toString();
			final String from = tmpfrom;
			final String subject = message.getBody();
			final String url = buildOpenIMUrl();
			Display.getDefault().syncExec(new Runnable() {

				@Override
				public void run() {
					NotifierDialog.notify(from, subject, ImageCache.getImage(ImageCache.MAIL),
							url);

				}
			});
			SoundPlayer.playSound();
		} catch (XMLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
