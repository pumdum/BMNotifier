package eu.anasta.bm.notifier.network;

import java.net.Socket;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

/**
 * 
 * @author Mo Firouz
 * @since 16/10/11
 */
public abstract class NetworkProber {
	private static final Logger LOG = Logger.getLogger(NetworkProber.class);
	private final static int SLEEP_TIME = 5000; // wait 5secs between each probe
	private String host;
	private int port = 993;
	private String name = "Network Prober";
	private int pingFailureCount = 0;
	private int sessionFailureCount = 0;
	private long lastBeat = -1;
	private boolean netConnectivity = false;
	private Timer timer;

	public NetworkProber() {
		// TODO Auto-generated constructor stub
	}

	private boolean probe() {
		boolean status = true;

		Socket socket = new Socket();
		try {
			socket = new Socket(host, port);
			status = true;
			pingFailureCount = 0;
		} catch (Exception ex) {
			LOG.error(ex);
			status = false;
			pingFailureCount++;
		} finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (Exception e) {
					LOG.error(e);
				}
			}
		}
		netConnectivity = status;
		return status;
	}

	private void periodicProber() {
		TimerTask task = new TimerTask() {

			@Override
			public void run() {
				if (lastBeat != -1) {
					long currentTime = System.currentTimeMillis();
					if (currentTime - lastBeat > ((SLEEP_TIME * 2) - 1)) {
						// missed beat, probably because of sleep...
						lastBeat = currentTime;
						missedBeat();
						return;
					}
					lastBeat = currentTime;
				} else {
					lastBeat = System.currentTimeMillis();
				}

				onNetworkChange(probe());
			}
		};
		stop();
		timer = new Timer("Timer poller for " + name, true);
		timer.scheduleAtFixedRate(task, Calendar.getInstance().getTime(),
				SLEEP_TIME);
		LOG.debug("start timer");
	}

	public void start(String host, int port) {
		this.host = host;
		this.port = port;
		Runnable r = new Runnable() {

			public void run() {
				periodicProber();
			}
		};
		lastBeat=-1 ;
		sessionFailureCount = 0;
		pingFailureCount = 0;
		Thread t = new Thread(r, "main treath of networ poller for " + name);
		t.setDaemon(true);
		t.start();
	}

	public boolean ping(String host, int port) {
		this.host = host;
		this.port = port;
		boolean test = probe();
		return test;
	}

	public void stop() {
		if (timer == null)
			return;
		LOG.debug("stop timer");
		timer.cancel();
		timer.purge();
		timer = null;

	}

	public int getPingFailureCount() {
		return pingFailureCount;
	}

	public int getSessionFailureCount() {
		return sessionFailureCount;
	}

	public boolean getNetConnectivity() {
		return netConnectivity;
	}

	public abstract void onNetworkChange(boolean status);

	public abstract void missedBeat();
}
