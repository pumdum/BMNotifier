package eu.anasta.bm.notifier.ui;

import java.util.prefs.Preferences;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;

import eu.anasta.bm.notifier.calendar.CalendarManager;
import eu.anasta.bm.notifier.mail.JavaPushMailAccount;
import eu.anasta.bm.notifier.mail.UnreadMailState;
import eu.anasta.bm.notifier.mail.app.JavaPushMailAccountsManager;
import eu.anasta.bm.notifier.ui.Notification.TRAY_TYPE;
import eu.anasta.bm.notifier.ui.cache.ImageCache;

public class Application {

	private static final String HOST = "host";
	private static Application instance;
	private static final Logger LOG = Logger.getLogger(Application.class);
	private static final String PASSWORD = "password";
	private static final String PORT = "port";

	private static final String USER = "user";
	private static final String VALID = "validParam";

	public static Application getInstance() {
		return instance;
	}

	/**
	 * Launch the application.
	 * 
	 * @param args
	 */
	public static void main(String args[]) {
		try {
			LOG.debug("start application");
			final Application app = new Application();
			app.init();
			app.createTray();
			if (app.trayicon==null){
				return;
			}
			app.connect(true);
			app.run();
			app.destroyTray();
			LOG.debug("close application");
		} catch (Exception e) {
			LOG.error(e);
		}
	}

	private CalendarManager calendar;
	private boolean connect;
	private Display display;

	private String host;

	private Image imageBM;
	private Login login;
	private JavaPushMailAccountsManager mailManager;

	public Shell masterShell;

	private String password;
	private int port;
	private final Preferences prefs = Preferences
			.userNodeForPackage(Application.class);

	private TrayItem trayicon;

	private String user;

	private EventNotification windowEventNotif;


	public EventNotification getWindowEventNotif() {
		return windowEventNotif;
	}

	private Application() {
		Application.instance = this;
	}

	public void connect(boolean autoConnect) {
		disconnect();
		connect = false;
		while (!connect && param(autoConnect)) {
			user = login.getUser();
			password = login.getPassword();
			host = login.getHost();
			if (!NumberUtils.isNumber(login.getPort())) {
				disableSession();
				MessageDialog.openError(masterShell, "Erreur",
						"port imap incorecte ");
				continue;
			} else {
				port = NumberUtils.createInteger(login.getPort());
			}
			// open calendar connect = openWebservice(user, password, host);
			calendar = new CalendarManager(user, password, host) {

				@Override
				protected void onAuthFail() {
					disableSession();
					disconnect();
					

				}

				@Override
				protected void onDisconnect() {
					if (isSessionEnnable()){
					Notification.getInstance().trayChange(
							TRAY_TYPE.DISCONNECTED);
					}else{
						Notification.getInstance().trayChange(
								TRAY_TYPE.ERROR);
						
					}

				}
			};
			connect = calendar.startPlanner();
			// connect =true;
			Notification.getInstance().trayChange(TRAY_TYPE.CONNECTED);
			scanMail(user, password, host, port);
			// start calendar planNextreminder();
			prefs.put(USER, user);
			prefs.put(PASSWORD, login.getPassword());
			prefs.put(HOST, login.getHost());
			prefs.put(PORT, login.getPort());
			ennableSession();
		}

	}

	private void createTray() {
		LOG.debug("create tray icon");
		imageBM = ImageCache.getImage(ImageCache.LITLLE_ERROR);
		final Tray tray = display.getSystemTray();
		if (tray == null) {
			LOG.error("Tray not supported");
		} else {
			trayicon = new TrayItem(tray, SWT.NONE);
			trayicon.setToolTipText("BM Notifier");
			final Menu menu = new Menu(masterShell, SWT.POP_UP);
			MenuItem mi = new MenuItem(menu, SWT.PUSH);
			mi.setText("close");
			mi.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					disconnect();
					masterShell.close();
				}
			});
			menu.setDefaultItem(mi);
			mi = new MenuItem(menu, SWT.PUSH);
			mi.setText("connect");
			mi.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					connect(false);
				}

			});
			mi = new MenuItem(menu, SWT.PUSH);
			mi.setText("disconect");
			mi.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					disconnect();
					disableSession();
				}

			});


			trayicon.addListener(SWT.MenuDetect, new Listener() {
				public void handleEvent(Event event) {
					menu.setVisible(true);
				}
			});
			trayicon.setImage(imageBM);
//			trayicon.add
		}

	}

	private void destroyTray() {
		// stop calendar
		LOG.debug("detroy tray");
		disconnect();
		imageBM.dispose();
		display.dispose();
	}
	private void disableSession(){
		prefs.putBoolean(VALID, false);
	}

	private void disconnect() {
		LOG.debug("close connection");
		if (mailManager != null)
			mailManager.disconnectAccounts();
		if (calendar != null)
			calendar.stopPlanner();
		Notification.getInstance().trayChange(
				TRAY_TYPE.DISCONNECTED);
		System.out.println("disconnected");
	}
	private void ennableSession(){
		prefs.putBoolean(VALID, true);
	}
	
	public JavaPushMailAccountsManager getMailManager() {
		return mailManager;
	}

	public TrayItem getTrayicon() {
		return trayicon;
	}

	public void init() {
		try{
		LOG.debug("init application");
		LOG.debug("new display");
		display = new Display();
		LOG.debug("new shell");
		masterShell = new Shell(display);
		LOG.debug("build login");
		login = new Login(masterShell);
		login.setUser(prefs.get(USER, ""));
		login.setPassword(prefs.get(PASSWORD, ""));
		login.setHost(prefs.get(HOST, ""));
		login.setPort(prefs.get(PORT, "143"));
		LOG.debug("build notification event");
		windowEventNotif = new EventNotification();
		LOG.debug("end init application");
		}catch (Exception e ){
			LOG.error(e);
		}

	}

	private boolean isSessionEnnable(){
		return prefs.getBoolean(VALID, false);
	}

	private boolean param(boolean autoConnect) {
		int reponse = Window.OK;
		if (!prefs.getBoolean(VALID, false) ){
			if (autoConnect)
				reponse =Window.CANCEL;
			else
				reponse = login.open();
		}
		return reponse == Window.OK;
	}

	private void run() {

		while (!masterShell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
	}

	private void scanMail(String user, String password, String host, int port) {

		mailManager = new JavaPushMailAccountsManager() {

			@Override
			public void handleError(JavaPushMailAccount acc, Exception ex) {
				ex.printStackTrace();
				disconnect();

			}

			@Override
			public void onModelChange() {
				LOG.debug("modéle change");
			}

			@Override
			public void onStateChange() {
				if (this.isConnected()) {
					UnreadMailState.check();
				} else {
					Notification.getInstance().trayChange(
							TRAY_TYPE.DISCONNECTED);

				}
			}
		};
		mailManager.setAccount("BM", host, port, false, user, password);

	}

}
