package eu.anasta.bm.notifier.mail;

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
    private JavaPushMailAccount mail;
    private String host;
    private int port = 993;
    private String name = "WebserviceProber";
    private int pingFailureCount = 0;
    private int sessionFailureCount = 0;
    private long lastBeat = -1;
    private boolean netConnectivity = false;
    private Timer timer;

    public NetworkProber(String host, int port, String accountName) {
        this.host = host;
        this.port = port;
        this.name = "NetworkProper-" + accountName;
    }

    public NetworkProber(JavaPushMailAccount mail) {
        this.mail = mail;
        this.host = mail.getServerAddress();
        this.port = mail.getServerPort();
        this.name = "NetworkProper-" + mail.getAccountName();
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

    private boolean probeWithSessionCheck() {
        boolean status = probe();
        if (status) {
            if (mail.isSessionValid()) {
                sessionFailureCount = 0;
                return true;
            } else {
                sessionFailureCount++;
                return false;
            }
        }
        return false;
    }

    private void periodicProber() {
        TimerTask task = new TimerTask() {

            @Override
            public void run() {
                if (lastBeat != -1) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastBeat > ((SLEEP_TIME*2)-1)) { 
                    	// missed beat, probably because of sleep...
                        LOG.info("missed beat ... ? your sleep ? ");
                        LOG.debug("current time : "+currentTime);
                        LOG.debug("previous beat : "+lastBeat);
                        LOG.debug("difference : "+(currentTime-lastBeat));
                        LOG.debug("marge d'erreur: "+(SLEEP_TIME + 10));
                        LOG.debug("resultat du test "+(currentTime - lastBeat > (SLEEP_TIME + 10)));
                        lastBeat = currentTime;   
                        missedBeat();
                        return;
                    } 
                    lastBeat = currentTime;
                } else {
                    lastBeat = System.currentTimeMillis();
                }
                
                if (mail == null)
                    onNetworkChange(probe());
                else
                    onNetworkChange(probeWithSessionCheck());
            }
        };
        stop();
        timer = new Timer("Network poller for "+name, true);
        timer.scheduleAtFixedRate(task, Calendar.getInstance().getTime(), SLEEP_TIME);
    }

    public void start() {
        Runnable r = new Runnable() {

            public void run() {
                periodicProber();
            }
        };
        sessionFailureCount = 0;
        pingFailureCount = 0;
        Thread t = new Thread(r, "networ poller for "+name);
        t.setDaemon(true);
        t.start();
    }

    public void stop() {
        if (timer == null)
            return;

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
