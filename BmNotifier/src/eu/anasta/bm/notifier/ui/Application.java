package eu.anasta.bm.notifier.ui;

import java.util.prefs.Preferences;

import javax.mail.AuthenticationFailedException;

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
import eu.anasta.bm.notifier.im.XmppManager;
import eu.anasta.bm.notifier.login.ClientFormLogin;
import eu.anasta.bm.notifier.mail.JavaPushMailAccount;
import eu.anasta.bm.notifier.mail.UnreadMailState;
import eu.anasta.bm.notifier.mail.app.JavaPushMailAccountsManager;
import eu.anasta.bm.notifier.network.NetworkProber;
import eu.anasta.bm.notifier.receiver.HttpServer;
import eu.anasta.bm.notifier.ui.Notification.TRAY_TYPE;
import eu.anasta.bm.notifier.ui.cache.ImageCache;

public class Application {

	private static Application instance;

	private static final String PREF_USER = "user";
	private static final String PREF_PASSWORD = "password";
	private static final String PREF_HOST = "host";
	private static final String PREF_PORT = "port";
	private static final String PREF_VALID = "validParam";

	private static final Logger LOG = Logger.getLogger(Application.class);

	private boolean connected = false;
	private boolean supportXmpp;

	public static Application getInstance() {
		return instance;
	}

	/**
	 * Launch the application.
	 * 
	 * @param args
	 */
	public static void main(String args[]) {
		HttpServer receiver =null;
		try {
			LOG.debug("start application");
			final Application app = new Application();
			app.init();
			app.createTray();
			app.connect(true);
			receiver = new HttpServer();
			receiver.launch();
			app.run();
			app.destroyTray();
			LOG.debug("close application");
		} catch (Exception e) {
			e.printStackTrace();
			LOG.error(e);
		}finally{
			if (receiver!=null){
				receiver.stop();
			}
		}
	}

	private final Preferences prefs = Preferences
			.userNodeForPackage(Application.class);

	private JavaPushMailAccountsManager mailManager;
	private CalendarManager calendar;

	private Shell masterShell;
	private Display display;
	private Login login;
	private TrayItem trayicon;
	private EventNotification windowEventNotif;

	private XmppManager xmppManager;

	private NetworkProber prober;

	public EventNotification getWindowEventNotif() {
		return windowEventNotif;
	}

	private Application() {
		Application.instance = this;
	}

	private void createTray() throws Exception {
		LOG.debug("create tray icon");
		Image imageBM = ImageCache.getImage(ImageCache.LITLLE_ERROR);
		final Tray tray = display.getSystemTray();
		if (tray == null) {
			LOG.error("Tray not supported");
			throw new Exception("System tray not suported");
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
					setParamInvalid();
				}

			});

			trayicon.addListener(SWT.MenuDetect, new Listener() {
				public void handleEvent(Event event) {
					menu.setVisible(true);
				}
			});
			trayicon.addListener(SWT.DefaultSelection, new Listener() {
				public void handleEvent(Event event) {
					if (isParamValid()) {
						org.eclipse.swt.program.Program.launch("http://"
								+ prefs.get(PREF_HOST, "") + "?BMHPS="
								+ ClientFormLogin.getInstance().login());
					}
				}
			});
			trayicon.setImage(imageBM);
		}

	}

	public void connect(boolean autoConnect) {
		LOG.debug("Not allow multiple connection -> try disconnect before;");
		disconnect();
		String user;
		String password;
		String host;
		int port;
		// while not connected show login form unless autoconnect
		while (!connected && param(autoConnect)) {
			user = login.getUser();
			password = login.getPassword();
			host = login.getHost();
			if (!NumberUtils.isNumber(login.getPort())) {
				// not numeric port -> error and continue
				setParamInvalid();
				MessageDialog.openError(masterShell, "Erreur",
						"port imap incorecte ");
				continue;
			} else {
				port = NumberUtils.createInteger(login.getPort());
			}

			// try to ping server
			boolean serverping = startnetworkProber(host, port);
			if (!serverping) {
				setParamInvalid();
				MessageDialog.openError(masterShell, "Erreur",
						"Imposible to connect; host unavailable");
				if (autoConnect)
					break;
				else
					continue;
			}
			// try to start calendar
			ClientFormLogin.getInstance().init(host, user, password);
			boolean calendarstart = startCalendarScan(user, password, host);
			if (!calendarstart) {
				setParamInvalid();
				MessageDialog.openError(masterShell, "Erreur",
						"Imposible to connect; chek user password");
				if (autoConnect)
					break;
				else
					continue;
			}

			startScanMail(user, password, host, port);

			if (supportXmpp) {
				// start IM
				startIm(user, password, host);
			}

			// save preference
			prefs.put(PREF_USER, user);
			prefs.put(PREF_PASSWORD, login.getPassword());
			prefs.put(PREF_HOST, login.getHost());
			prefs.put(PREF_PORT, login.getPort());

			setParamValid();
			connected=true;
			Notification.getInstance().trayChange(TRAY_TYPE.CONNECTED);
		}

	}

	private void destroyTray() {
		// disconnect and dispose()
		LOG.debug("detroy tray");
		disconnect();
		ImageCache.dispose();
		display.dispose();
	}

	private void setParamInvalid() {
		// set flag of session state to invalid
		prefs.putBoolean(PREF_VALID, false);
	}

	private void disconnect() {
		LOG.debug("close connection");
		// disconnect service if possible
		if (mailManager != null && mailManager.getAccount() != null)
			mailManager.disconnectAccounts();
		if (calendar != null){
			calendar.stopPlanner();
			calendar.close();
		}
		if (prober != null){
			prober.stop();
		}
		Notification.getInstance().trayChange(TRAY_TYPE.DISCONNECTED);
		connected =false;
	}

	private void setParamValid() {
		prefs.putBoolean(PREF_VALID, true);
	}

	public JavaPushMailAccountsManager getMailManager() {
		return mailManager;
	}

	public TrayItem getTrayicon() {
		return trayicon;
	}

	public void init() {
		try {
			LOG.debug("init application");
			LOG.debug("new display");
			display = new Display();
			LOG.debug("new shell");
			masterShell = new Shell(display);
			LOG.debug("build login");
			login = new Login(masterShell);
			login.setUser(prefs.get(PREF_USER, ""));
			login.setPassword(prefs.get(PREF_PASSWORD, ""));
			login.setHost(prefs.get(PREF_HOST, ""));
			login.setPort(prefs.get(PREF_PORT, "143"));
			LOG.debug("build notification event");
			windowEventNotif = new EventNotification();
			LOG.debug("end init application");
			buildManager();
		} catch (Exception e) {
			LOG.error(e);
		}

	}

	private boolean isParamValid() {
		return prefs.getBoolean(PREF_VALID, false);
	}

	private boolean param(boolean autoConnect) {
		int reponse = Window.OK;
		if (!isParamValid()) {
			if (autoConnect)
				reponse = Window.CANCEL;
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

	private void buildManager() {
		xmppManager = new XmppManager() {
			@Override
			protected void onDisconnect() {
				// TODO Auto-generated method stub
			}

			@Override
			protected void onAuthFail(Exception e) {
				// TODO Auto-generated method stub
			}
		};
		calendar = new CalendarManager() {
			@Override
			protected void onAuthFail(Exception e) {
				setParamInvalid();
				disconnect();
			}

			@Override
			protected void onDisconnect() {
				if (isParamValid()) {
					Notification.getInstance().trayChange(
							TRAY_TYPE.DISCONNECTED);
				} else {
					Notification.getInstance().trayChange(TRAY_TYPE.ERROR);

				}

			}
		};
		mailManager = new JavaPushMailAccountsManager() {

			@Override
			public void handleError(JavaPushMailAccount acc, Exception ex) {
				LOG.error("[ERROR] on mail manager -> disconnect ", ex);
				if (ex instanceof AuthenticationFailedException) {
					disconnect();
					setParamInvalid();
					MessageDialog.openError(masterShell, "Erreur",
							"Imposible to connect; Check user password ");
				}
			}

			@Override
			public void onModelChange() {
				LOG.debug("mod�le change");
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

		prober = new NetworkProber() {

			@Override
			public void onNetworkChange(boolean status) {
				if (!status && connected) {
					if (getSessionFailureCount() >= 2
							|| getPingFailureCount() >= 2) {
						LOG.info("msut be connction vut seem be off");
						connected = false;
						connect(true);
					}
				}
			}

			@Override
			public void missedBeat() {
				// TODO Auto-generated method stub
				connect(true);
			}
		};
	}

	private void startIm(String user, String password, String host) {
		xmppManager.start(user, password, host);
	}

	private boolean startCalendarScan(String user, String password, String host) {
		boolean started = calendar.startPlanner(user, password, host);
		if (started) {
			supportXmpp = "3".equals(calendar.getVersion());
		}
		return started;
	}

	private void startScanMail(String user, String password, String host,
			int port) {

		mailManager.setAccount("BMNotifierScanMail", host, port, false, user,
				password);

	}

	private boolean startnetworkProber(String host, int port) {

		return prober.start(host, port);

	}

}
