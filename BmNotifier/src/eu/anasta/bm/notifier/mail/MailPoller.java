package eu.anasta.bm.notifier.mail;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import com.sun.mail.imap.IMAPFolder;

/**
 *
 * @author Mo Firouz
 * @since 20/10/11
 */
public abstract class MailPoller {

    private final static int SLEEP_TIME = 300000; // check mail every 5 min
    private IMAPFolder folder;
    private String name = "";
    private int previousCount = -1;
    private int diffCount = -1;
    protected Timer timer;

    public MailPoller(IMAPFolder folder) {
        this.folder = folder;
    }

    private boolean poll() {
        try {
            int newCount = folder.getMessageCount();
            if (previousCount == -1) {
            	diffCount = 0;
                previousCount = newCount;
                return false;
            } else {
                if (previousCount < newCount) {
                	diffCount = newCount - previousCount;
                    previousCount = newCount;
                    return true;
                }
                return false;
            }
        } catch (Exception ex) {
//            SimpleLogger.debug("Poller Error: ", ex);
            return false;
        }
    }

    private void periodicPoller() {
        TimerTask task = new TimerTask() {

            @Override
            public void run() {
                if (poll())
                    onNewMessage();
            }
        };
        stop();
        timer = new Timer("periodique mail poller for "+name, true);
        timer.scheduleAtFixedRate(task, Calendar.getInstance().getTime(), SLEEP_TIME);
    }

    public void start(String name) {
        this.name = "MailPoller-" + name;
        Runnable r = new Runnable() {

            public void run() {
                periodicPoller();
            }
        };
        stop();
        Thread t = new Thread(r, "mail poller for "+this.name);
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
    
    public int getDiffCount() {
    	return diffCount;
    }
    
    public void setFolder(IMAPFolder folder) {
        this.folder = folder;
    }

    public abstract void onNewMessage();
}
