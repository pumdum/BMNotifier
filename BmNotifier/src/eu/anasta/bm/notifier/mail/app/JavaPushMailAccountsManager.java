package eu.anasta.bm.notifier.mail.app;

import eu.anasta.bm.notifier.mail.JavaPushMailAccount;

/**
 * 
 * @author Mo Firouz
 * @since 16/10/11
 */
public abstract class JavaPushMailAccountsManager {

	private JavaPushMailAccount account = null;
	private JavaPushMailNotifier notifiers = null;
	private static boolean connected = false;

	public JavaPushMailAccountsManager() {
	}

	public synchronized JavaPushMailAccount setAccount(final String name,
			final String server, final int port, final boolean useSSL,
			final String username, final String password) {
		account = new JavaPushMailAccount(name, server, port, useSSL) {

			@Override
			public void onError(Exception e) {
				connected = false;
				e.printStackTrace();
				handleError(this, e);
			}

			@Override
			public void onConnect() {
				connected = true;
				onStateChange();
			}

			@Override
			public void onDisconnect() {
				connected = false;
				onStateChange();
			}
		};
		account.setCredentials(username, password);
		notifiers = new JavaPushMailNotifier(account);
		startMailDaemon(account);
		return account;
	}

	public synchronized void reconnectDisconnected() {
		startMailDaemon(account);
	}

	public synchronized void disconnectAccounts() {
		account.disconnect();
	}

	public JavaPushMailAccount getAccount() {
		return account;
	}

	public JavaPushMailNotifier getNotifier() {
		return notifiers;
	}

	public boolean isConnected() {
		return connected;
	}
	
	public boolean isReady(){
		return notifiers!=null && account!=null;
	}

	private void startMailDaemon(JavaPushMailAccount mail) {
		Thread t = new Thread(mail);
		t.setName("JPM-" + mail.getAccountName());
		t.start();
	}

	public abstract void handleError(JavaPushMailAccount acc, Exception ex);


	public abstract void onStateChange();
}