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
import eu.anasta.bm.notifier.im.XmppManager.Presence;
import eu.anasta.bm.notifier.login.ClientFormLogin;
import eu.anasta.bm.notifier.mail.JavaPushMailAccount;
import eu.anasta.bm.notifier.mail.UnreadMailState;
import eu.anasta.bm.notifier.mail.app.JavaPushMailAccountsManager;
import eu.anasta.bm.notifier.network.NetworkProber;
import eu.anasta.bm.notifier.receiver.HttpServer;
import eu.anasta.bm.notifier.ui.Notification.TRAY_TYPE;
import eu.anasta.bm.notifier.ui.cache.ImageCache;

// TODO: Auto-generated Javadoc
/**
 * The Class Application.
 * 
 * Fonction de démarage
 * 
 * @author n.dumont
 */
public class Application {

	/** The instance. */
	private static Application instance;

	/** The Constant LOG. */
	private static final Logger LOG = Logger.getLogger(Application.class);

	/** The Constant PREF_HOST. */
	private static final String PREF_HOST = "host";

	/** The Constant PREF_PASSWORD. */
	private static final String PREF_PASSWORD = "password";

	/** The Constant PREF_PORT. */
	private static final String PREF_PORT = "port";

	/** The Constant PREF_USER. */
	private static final String PREF_USER = "user";

	/** The Constant PREF_VALID. */
	private static final String PREF_VALID = "validParam";

	/** The calendar. */
	private CalendarManager calendar;

	/** The connected. */
	private boolean connected = false;

	/** The display. */
	private Display display;

	/** The login. */
	private Login login;

	/** The mail manager. */
	private JavaPushMailAccountsManager mailManager;

	/** The master shell. */
	private Shell masterShell;

	/** The prefs. */
	private final Preferences prefs = Preferences
			.userNodeForPackage(Application.class);

	/** The prober. */
	private NetworkProber prober;

	/** The support xmpp. */
	private boolean supportXmpp;

	/** The trayicon. */
	private TrayItem trayBMicon;

	private TrayItem trayIMicon;

	/** The window event notif. */
	private EventNotification windowEventNotif;

	/** The xmpp manager. */
	private XmppManager xmppManager;

	/**
	 * Launch the application.
	 * 
	 * @param args
	 *            the arguments
	 */
	public static void main(String args[]) {
		HttpServer receiver = null;
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
		} finally {
			if (receiver != null) {
				receiver.stop();
			}
		}
	}

	/**
	 * Gets the single instance of Application.
	 * 
	 * @return single instance of Application
	 */
	public static Application getInstance() {
		return instance;
	}

	/**
	 * Instantiates a new application.
	 */
	private Application() {
		Application.instance = this;
	}

	/**
	 * Builds the manager.
	 */
	private void buildManager() {
		xmppManager = new XmppManager() {
			@Override
			protected void onAuthFail(Exception e) {
				// TODO Auto-generated method stub
			}

			@Override
			protected void onDisconnect() {
				// TODO Auto-generated method stub
			}

			@Override
			protected void onPresenceChange(Presence p) {
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
				} else {
					if (connected) {
						LOG.debug("[CONNECT] [OFF]");
						connected = false;
						stopPlanner();
					}
				}
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
			public void missedBeat() {
				// TODO Auto-generated method stub
				if (getNetConnectivity() && connected)
					connect(true);
			}

			@Override
			public void onNetworkChange(boolean status) {
				if (!status && connected) {
					if (getSessionFailureCount() >= 2
							|| getPingFailureCount() >= 2) {
						LOG.debug("[CONNECT] [OFF]");
						connected = false;
						LOG.info("msut be connction vut seem be off");
						Notification.getInstance().trayChange(
								TRAY_TYPE.DISCONNECTED);
						stopPlanner();
					}
				}
				if (status && !connected) {
					LOG.debug("[CONNECT] [ON]");
					connected = true;
					startPlaner(getPrefUser(), getPrefHost(),
							getPrefPassword(), getPrefPort(), true);
					// Notification.getInstance().trayChange(
					// TRAY_TYPE.CONNECTED);
				}
			}
		};

	}

	private void stopPlanner() {
		LOG.debug("stop all planner");
		if (mailManager != null && mailManager.isReady())
			mailManager.disconnectAccounts();
		if (calendar != null && calendar.isRunning()) {
			calendar.stopPlanner();
			calendar.close();
		}
		if (xmppManager != null) {
			// TODO check running
			xmppManager.stopPlanner();
		}
		Notification.getInstance().trayChange(TRAY_TYPE.DISCONNECTED);

	}

	/**
	 * Connect.
	 * 
	 * @param autoConnect
	 *            the auto connect
	 */
	private void connect(boolean autoConnect) {
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
				if (!autoConnect) {
					MessageDialog.openError(masterShell, "Erreur",
							"port imap incorecte ");
				}
				continue;
			} else {
				port = NumberUtils.createInteger(login.getPort());
			}

			// try to ping server
			boolean serverping = ping(host, port);
			if (!serverping) {
				setParamInvalid();
				if (autoConnect)
					break;
				else
					MessageDialog.openError(masterShell, "Erreur",
							"Imposible to connect; host unavailable");
				continue;
			}

			// save preference
			setPrefUser(user);
			setPrefPassword(password);
			setPrefHost(host);
			setPrefPort(port);
			ClientFormLogin.getInstance().init(host, user, password);
			if (!startPlaner(user, host, password, port, autoConnect)) {

				if (autoConnect) {
					break;
				} else {
					continue;
				}
			}
			setParamValid();
			LOG.debug("[CONNECT] [ON]");
			connected = true;
			startnetworkProber(host, port);
			Notification.getInstance().trayChange(TRAY_TYPE.CONNECTED);
		}

	}

	private boolean startPlaner(String user, String host, String password,
			int port, boolean autoConnect) {
		// try to start calendar

		boolean calendarstart = startCalendarScan(user, password, host);
		if (!calendarstart) {
			setParamInvalid();
			if (autoConnect)
				return false;
			else
				MessageDialog.openError(masterShell, "Erreur",
						"Imposible to connect; chek user password");
			return false;
		}

		startScanMail(user, password, host, port);

		if (supportXmpp) {
			// start IM
			startIm(user, password, host);
		}
		return true;

	}

	/**
	 * Creates the tray.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	private void createTray() throws Exception {
		LOG.debug("create BM tray icon");
		Image imageBM = ImageCache.getImage(ImageCache.BM_STATUS_DISCONECT);
		final Tray tray = display.getSystemTray();
		if (tray == null) {
			LOG.error("Tray not supported");
			throw new Exception("System tray not suported");
		} else {
			trayBMicon = new TrayItem(tray, SWT.NONE);
			trayBMicon.setToolTipText("BM Notifier");
			final Menu menuBM = new Menu(masterShell, SWT.POP_UP);
			MenuItem mi = new MenuItem(menuBM, SWT.PUSH);
			mi.setText("close");
			mi.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					disconnect();
					masterShell.close();
				}
			});
			menuBM.setDefaultItem(mi);
			mi = new MenuItem(menuBM, SWT.PUSH);
			mi.setText("connect");
			mi.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					connect(false);
				}

			});
			mi = new MenuItem(menuBM, SWT.PUSH);
			mi.setText("disconect");
			mi.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					disconnect();
					setParamInvalid();
				}

			});

			trayBMicon.addListener(SWT.MenuDetect, new Listener() {
				public void handleEvent(Event event) {
					menuBM.setVisible(true);
				}
			});
			trayBMicon.addListener(SWT.DefaultSelection, new Listener() {
				public void handleEvent(Event event) {
					if (isParamValid()) {
						org.eclipse.swt.program.Program.launch("http://"
								+ getPrefHost() + "?BMHPS="
								+ ClientFormLogin.getInstance().login());
					}
				}
			});
			trayBMicon.setImage(imageBM);
			trayIMicon = new TrayItem(tray, SWT.NONE);// lITLLE_USER_ONLINE
			trayIMicon.setImage(ImageCache
					.getImage(ImageCache.LITLLE_USER_ONLINE));
			final Menu menuIM = new Menu(masterShell, SWT.POP_UP);
			mi = new MenuItem(menuIM, SWT.PUSH);
			mi.setText("En ligne");
			mi.setImage(ImageCache.getImage(ImageCache.LITLLE_TEMOIN_ONLINE));
			mi.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
				}
			});
			mi = new MenuItem(menuIM, SWT.PUSH);
			mi.setText("Absent");
			mi.setImage(ImageCache.getImage(ImageCache.lITLLE_AWAY));
			mi.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
				}
			});
			mi = new MenuItem(menuIM, SWT.PUSH);
			mi.setText("Hors ligne");
			mi.setImage(ImageCache.getImage(ImageCache.lITLLE_OFFLINE));
			mi.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					xmppManager.changePresence(Presence.dnd);
				}

			});
			trayIMicon.addListener(SWT.MenuDetect, new Listener() {
				public void handleEvent(Event event) {
					menuIM.setVisible(true);
				}
			});

		}

	}

	/**
	 * Destroy tray.
	 */
	private void destroyTray() {
		// disconnect and dispose()
		LOG.debug("detroy tray");
		disconnect();
		ImageCache.dispose();
		display.dispose();
	}

	/**
	 * Disconnect.
	 */
	private void disconnect() {
		LOG.debug("close connection");
		// disconnect service if possible
		if (prober != null) {
			prober.stop();
		}
		LOG.debug("[CONNECT] [OFF]");
		connected = false;
		ClientFormLogin.getInstance().closeFormLogin();
		stopPlanner();
		Notification.getInstance().trayChange(TRAY_TYPE.DISCONNECTED);
	}

	/**
	 * Gets the active host.
	 * 
	 * @return the active host
	 * @throws Exception
	 *             the exception
	 */
	public String getActiveHost() throws Exception {
		if (connected) {
			return getPrefHost();
		}
		throw new Exception("Not connected");
	}

	/**
	 * Gets the mail manager.
	 * 
	 * @return the mail manager
	 */
	public JavaPushMailAccountsManager getMailManager() {
		return mailManager;
	}

	/**
	 * Gets the pref host.
	 * 
	 * @return the pref host
	 */
	private String getPrefHost() {
		return prefs.get(PREF_HOST, "");
	}

	/**
	 * Gets the pref passwor.
	 * 
	 * @return the pref passwor
	 */
	private String getPrefPassword() {
		return prefs.get(PREF_PASSWORD, "");
	}

	/**
	 * Gets the pref port.
	 * 
	 * @return the pref port
	 */
	private int getPrefPort() {
		return prefs.getInt(PREF_PORT, 143);
	}

	/**
	 * Gets the pref user.
	 * 
	 * @return the pref user
	 */
	private String getPrefUser() {
		return prefs.get(PREF_USER, "");
	}

	/**
	 * Gets the trayicon.
	 * 
	 * @return the trayicon
	 */
	public TrayItem getTrayicon() {
		return trayBMicon;
	}

	/**
	 * Gets the window event notif.
	 * 
	 * @return the window event notif
	 */
	public EventNotification getWindowEventNotif() {
		return windowEventNotif;
	}

	/**
	 * Inits the.
	 */
	public void init() {
		try {
			LOG.debug("init application");
			LOG.debug("new display");
			display = new Display();
			LOG.debug("new shell");
			masterShell = new Shell(display);
			LOG.debug("build login");
			login = new Login(masterShell);
			login.setUser(getPrefUser());
			login.setPassword(getPrefPassword());
			login.setHost(getPrefHost());
			login.setPort(getPrefPort() + "");
			LOG.debug("build notification event");
			windowEventNotif = new EventNotification();
			LOG.debug("end init application");
			buildManager();
		} catch (Exception e) {
			LOG.error(e);
		}

	}

	/**
	 * Checks if is param valid.
	 * 
	 * @return true, if is param valid
	 */
	private boolean isParamValid() {
		return prefs.getBoolean(PREF_VALID, false);
	}

	/**
	 * Param.
	 * 
	 * @param autoConnect
	 *            the auto connect
	 * @return true, if successful
	 */
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

	/**
	 * Run.
	 */
	private void run() {

		while (!masterShell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
	}

	/**
	 * Sets the param invalid.
	 */
	private void setParamInvalid() {
		// set flag of session state to invalid
		prefs.putBoolean(PREF_VALID, false);
	}

	/**
	 * Sets the param valid.
	 */
	private void setParamValid() {
		prefs.putBoolean(PREF_VALID, true);
	}

	/**
	 * Sets the pref host.
	 * 
	 * @param host
	 *            the new pref host
	 */
	private void setPrefHost(String host) {
		prefs.put(PREF_HOST, host);
	}

	/**
	 * Sets the pref password.
	 * 
	 * @param password
	 *            the new pref password
	 */
	private void setPrefPassword(String password) {
		prefs.put(PREF_PASSWORD, password);
	}

	/**
	 * Sets the pref port.
	 * 
	 * @param port
	 *            the new pref port
	 */
	private void setPrefPort(int port) {
		prefs.putInt(PREF_PORT, port);
	}

	/**
	 * Sets the pref user.
	 * 
	 * @param user
	 *            the new pref user
	 */
	private void setPrefUser(String user) {
		prefs.put(PREF_USER, user);
	}

	/**
	 * Start calendar scan.
	 * 
	 * @param user
	 *            the user
	 * @param password
	 *            the password
	 * @param host
	 *            the host
	 * @return true, if successful
	 */
	private boolean startCalendarScan(String user, String password, String host) {
		boolean started = calendar.startPlanner(user, password, host);
		if (started) {
			supportXmpp = "3".equals(calendar.getVersion());
			LOG.debug("version BM: " + calendar.getVersion());
			LOG.debug("support xmpp : " + supportXmpp);
			// DEBUG ETCH
			supportXmpp = false;
		}
		return started;
	}

	/**
	 * Start im.
	 * 
	 * @param user
	 *            the user
	 * @param password
	 *            the password
	 * @param host
	 *            the host
	 */
	private void startIm(String user, String password, String host) {
		xmppManager.start(user, password, host);
	}

	/**
	 * Startnetwork prober.
	 * 
	 * @param host
	 *            the host
	 * @param port
	 *            the port
	 * @return true, if successful
	 */
	private void startnetworkProber(String host, int port) {

		prober.start(host, port);

	}

	private boolean ping(String host, int port) {

		return prober.ping(host, port);

	}

	/**
	 * Start scan mail.
	 * 
	 * @param user
	 *            the user
	 * @param password
	 *            the password
	 * @param host
	 *            the host
	 * @param port
	 *            the port
	 */
	private void startScanMail(String user, String password, String host,
			int port) {

		mailManager.setAccount("BMNotifierScanMail", host, port, false, user,
				password);

	}

}
