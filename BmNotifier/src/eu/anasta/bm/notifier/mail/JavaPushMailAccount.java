package eu.anasta.bm.notifier.mail;

import java.util.EventListener;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import javax.mail.AuthenticationFailedException;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.FolderClosedException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.event.MessageChangedEvent;
import javax.mail.event.MessageChangedListener;
import javax.mail.event.MessageCountEvent;
import javax.mail.event.MessageCountListener;
import javax.mail.search.FlagTerm;

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;

/**
 * 
 * @author Mo Firouz
 * @since 2/10/11
 */
public abstract class JavaPushMailAccount implements Runnable {

	public final static int READ_ONLY_FOLDER = Folder.READ_ONLY;
	public final static int READ_WRITE_FOLDER = Folder.READ_WRITE;
	private static final long STOPPUSHTIMER = 29 * 60 * 1000;
	private boolean connected = false;
	private boolean usePush = true;
	private String accountName;
	private String serverAddress;
	private String username;
	private String password;
	private int serverPort;
	private boolean useSSL;
	private IMAPStore server;
	private Session session;
	private IMAPFolder folder;
	private Timer timer;

	public IMAPFolder getFolder() {
		return folder;
	}

	public void setFolder(IMAPFolder folder) {
		this.folder = folder;
	}

	private MessageCountListener messageCountListener, externalCountListener;
	private MessageChangedListener messageChangedListener,
			externalChangedListener;
	private NetworkProber prober;
	private MailPoller poller;
	private Thread pushThread;

	public JavaPushMailAccount(String accountName, String serverAddress,
			int serverPort, boolean useSSL) {
		this.accountName = accountName;
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;
		this.useSSL = useSSL;
	}

	public void setCredentials(String username, String password) {
		this.username = username;
		this.password = password;
	}

	public void run() {
		this.initConnection();
	}

	public void connect() {
		try {
			server.connect(serverAddress, serverPort, username, password);
			selectFolder("");
			prober.start();
			connected = true;
			onConnect();
		} catch (AuthenticationFailedException ex) {
			ex.printStackTrace();
			connected = false;
			// TODO log 
			onError(ex);
		} catch (MessagingException ex) {
			ex.printStackTrace();
			connected = false;
			folder = null;
			messageChangedListener = null;
			messageCountListener = null;
			// TODO log 
			onError(ex);
		} catch (IllegalStateException ex) {
			ex.printStackTrace();
			// TODO log 
			connected = true;
			onConnect();
		}
	}

	public void disconnect() {
		if (!connected && server == null && !server.isConnected())
			return;

		Thread t = new Thread(new Runnable() {

			public void run() {
				try {
					closeFolder();
					server.close();
					prober.stop();
					poller.stop();
					connected = false;
					onDisconnect();
				} catch (Exception e) {
					onError(e);
				}
			}
		});
		t.setName("close conection mail");
		t.start();
	}

	public void setMessageChangedListerer(MessageChangedListener listener) {
		removeListener(externalChangedListener);
		externalChangedListener = listener;
		addListener(externalChangedListener);
	}

	public void setMessageCounterListerer(MessageCountListener listener) {
		removeListener(externalCountListener);
		externalCountListener = listener;
		addListener(externalCountListener);
	}

	private void initConnection() {
		prober = new NetworkProber(this) {

			@Override
			public void onNetworkChange(boolean status) {
				if (status != connected) { // if two states do not match,
											// something has truly changed!
					if (status && !connected) { // if connection up, but not
												// connected...
						connect();
					} else if (!status && connected) { // if previously
														// connected, but link
														// down... then just
														// disconnect...
						if (getSessionFailureCount() >= 2
								|| getPingFailureCount() >= 2) {
							connected = false;
							if (!usePush)
								poller.stop();

							onDisconnect();
							connect();
						}
					}
				} else { // if link (either session or net connection) and
							// connection down, something gone wrong...
					if (!isSessionValid() && getNetConnectivity()) // need to
																	// make sure
																	// that
																	// session
																	// is down,
																	// but link
																	// is up...
						connect();
				}
			}

			@Override
			public void missedBeat() { // missed beat, because of going to
										// sleep, probably?!
				connected = false;
			}
		};

		poller = new MailPoller(folder) {

			@Override
			public void onNewMessage() {
				try {
					if (externalCountListener != null) {
						externalCountListener
								.messagesAdded(new MessageCountEvent(folder,
										MessageCountEvent.ADDED, false,
										getUnreadMessages()));
						messageCountListener
								.messagesAdded(new MessageCountEvent(folder,
										MessageCountEvent.ADDED, false,
										getUnreadMessages()));
					}
				} catch (Exception e) {
					onError(e);
				}

			}
		};

		Properties props = System.getProperties();

		// enable to throw out everything...
		// props.put("mail.debug", "true");

		String imapProtocol = "imap";
		if (useSSL) {
			imapProtocol = "imaps";
			props.setProperty("mail.imap.socketFactory.class",
					"javax.net.ssl.SSLSocketFactory");
			props.setProperty("mail.imap.socketFactory.fallback", "false");
		}
		props.setProperty("mail.store.protocol", imapProtocol);
		session = Session.getDefaultInstance(props, null);
		try {
			server = (IMAPStore) session.getStore(imapProtocol);
			connect();
		} catch (MessagingException ex) {
			ex.printStackTrace();
			// TODO log SimpleLogger.debug(ex);
			onError(ex);
		}
	}

	private void selectFolder(String folderName) {
		try {
			closeFolder();
			if (folderName.equalsIgnoreCase("")) {
				folder = (IMAPFolder) server.getFolder("INBOX");
			} else {
				folder = (IMAPFolder) server.getFolder(folderName);
			}
			openFolder();
		} catch (MessagingException ex) {
			ex.printStackTrace();
			//TODO  log
			onError(ex);
		} catch (IllegalStateException ex) {
			ex.printStackTrace();
			//TODO  log 
		}
	}

	private void openFolder() throws MessagingException {
		if (folder == null)
			return;

		folder.open(Folder.READ_WRITE);
		folder.setSubscribed(true);
		removeAllListenersFromFolder();
		addAllListenersFromFolder();
		poller.setFolder(folder);
		if (usePush){
			usePush();
			TimerTask task = new TimerTask() {

				@Override
				public void run() {
					usePush();
				}
			};
			stopPeriodiquePush();
			timer = new Timer("Pushperiodique-" + accountName, true);
			timer.scheduleAtFixedRate(task, STOPPUSHTIMER,STOPPUSHTIMER);
		}
		else
			poller.start(accountName);
	}

	private void closeFolder() throws MessagingException {
		if (folder == null || !folder.isOpen())
			return;

		stopPeriodiquePush();
		removeAllListenersFromFolder();
		folder.setSubscribed(false);
		folder.close(false);
		folder = null;
		messageChangedListener=null;
		messageCountListener=null;
	}

	private void usePush() {
		if (folder == null || !usePush) {
			return;

		}

		Runnable r = new Runnable() {

			public void run() {
				try {
					UnreadMailState.check();
					folder.idle(false);
					//TODO log ex
				} catch (FolderClosedException e) {
					e.printStackTrace();
					messageChangedListener = null;
					messageCountListener = null;
					usePush = true;
					selectFolder("");
				} catch (javax.mail.StoreClosedException e) {
					e.printStackTrace();
					usePush = true;
				} catch (MessagingException e) {
					e.printStackTrace();
					usePush = false;
					selectFolder("");
				} catch (Exception e) {
					e.printStackTrace();
					usePush = false;
					selectFolder("");
				}
			}
		};
		pushThread = new Thread(r, "Push-" + accountName);
		pushThread.setDaemon(true);
		pushThread.start();

	}

	public void stopPeriodiquePush() {
		if (timer == null)
			return;

		timer.cancel();
		timer.purge();
		timer = null;
	}

	private void removeAllListenersFromFolder() {
		removeListener(externalChangedListener);
		removeListener(externalCountListener);
	}

	private void removeListener(EventListener listener) {
		if (listener == null || folder == null) {
			return;
		}

		if (listener instanceof MessageChangedListener) {
			folder.removeMessageChangedListener((MessageChangedListener) listener);
		} else {
			if (listener instanceof MessageCountListener) {
				folder.removeMessageCountListener((MessageCountListener) listener);
			}
		}
	}

	private void addAllListenersFromFolder() {
		addListener(externalCountListener);
		addListener(externalChangedListener);
	}

	private void addListener(EventListener listener) {
		if (listener == null || folder == null) {
			return;
		}

		if (listener instanceof MessageChangedListener) {
			folder.addMessageChangedListener((MessageChangedListener) listener);
		} else {
			if (listener instanceof MessageCountListener) {
				folder.addMessageCountListener((MessageCountListener) listener);
			}
		}

		addInternalListeners(listener);

	}

	private void addInternalListeners(EventListener listener) {
		if (listener == null || folder == null) {
			return;
		}
		if (listener instanceof MessageChangedListener
				&& messageChangedListener == null) {
			messageChangedListener = new MessageChangedListener() {

				public void messageChanged(MessageChangedEvent mce) {
					usePush();
				}
			};
			folder.addMessageChangedListener(messageChangedListener);
		} else {
			if (listener instanceof MessageCountListener
					&& messageCountListener == null) {
				messageCountListener = new MessageCountListener() {

					public void messagesAdded(MessageCountEvent mce) {
						usePush();
					}

					public void messagesRemoved(MessageCountEvent mce) {
						usePush();
					}
				};
				folder.addMessageCountListener(messageCountListener);
			}
		}
	}

	public Message[] getUnreadMessages() throws MessagingException {
		if (this.getFolder() == null || !this.getFolder().isOpen()
				|| !this.isSessionValid()) {
			return new Message[0];
		}
		Message[] unreadMessages = this.getFolder().search(
				new FlagTerm(new Flags(Flags.Flag.SEEN), false));
		return unreadMessages;
	}

	public Message[] getMessages() throws MessagingException {
		return folder.getMessages();
	}

	public String getAccountName() {
		return accountName;
	}

	public String getPassword() {
		return password;
	}

	public String getServerAddress() {
		return serverAddress;
	}

	public int getServerPort() {
		return serverPort;
	}

	public boolean isSSL() {
		return useSSL;
	}

	public String getUsername() {
		return username;
	}

	public boolean isConnected() {
		return connected;
	}

	public boolean isSessionValid() {
		return server.isConnected();
	}

	@Override
	public String toString() {
		return accountName;
	}

	public void setAccountName(String accountName) {
		this.accountName = accountName;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setServerAddress(String serverAddress) {
		this.serverAddress = serverAddress;
	}

	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}

	public void setUseSSL(boolean useSSL) {
		this.useSSL = useSSL;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public abstract void onError(Exception e);

	public abstract void onDisconnect();

	public abstract void onConnect();
}
