package eu.anasta.bm.notifier.calendar;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;

import net.bluemind.core.api.AccessToken;
import net.bluemind.core.api.calendar.CalendarQuery;
import net.bluemind.core.api.calendar.Occurrence;
import net.bluemind.core.api.calendar.OccurrenceFactory;
import net.bluemind.core.api.fault.AuthFault;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.client.calendar.CalendarClient;
import net.bluemind.core.client.locators.CalendarLocator;

public abstract class CalendarManager {
	private CalendarClient cal;
	CalendarAnalyser account = null;
	private AccessToken token;

	private int failSearch;

	private Timer plannerNextReminder;
	private static final int MAXDAYRECALL = 7;
	private static final long ELAPSETIMEBETEEWNSEARCH = 1000 * 60 * 10;
	private static final long MARGETIMESYNCRO = 1000 * 60 * 60 * 3;
	private CalendarAnalyser task;
	private String user;
	private String password;
	private String host;
	private String version="0";

	public CalendarManager() {
	}

	public void close() {
		if (cal != null)
			cal.logout(token);
	}

	private boolean openWebservice() {
		CalendarLocator cl = new CalendarLocator();
		// via l'url du serveur Blue Mind
		String url = "https://" + host + "/services";
		this.cal = cl.locate(url);
		try {
			token = cal.login(user, password, "BM Notifier");
			System.out.println(token.getSessionId());
			System.out.println(token.getAuthService());
			System.out.println(token.getUserId());
			if (token!=null)version =token.getVersion().getMajor();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return (token != null && token.getSessionId() != null);
	}

	public List<Occurrence> findNextRecallEvent() throws AuthFault, ServerFault {
		List<Occurrence> listRecall = new LinkedList<Occurrence>();
		Calendar now = Calendar.getInstance();
		Calendar future = Calendar.getInstance();
		future.add(Calendar.DAY_OF_YEAR, MAXDAYRECALL);
		// determine que la prochaine recherche aura a l'instant T + temps entre
		// chaque recherche (10min) + une marge d'ecart en cas de synchro entre
		// serveur et client (ici 3 minute)
		// TODO prevoir un fichier de param�tre
		Long nextSearch = now.getTimeInMillis() + ELAPSETIMEBETEEWNSEARCH
				+ MARGETIMESYNCRO;
		CalendarQuery cq = new CalendarQuery();
		cq.setFrom(now.getTime());
		cq.setTo(future.getTime());
		OccurrenceFactory of = cal.find(token, cq);
		for (Occurrence oc : of.getOccurrences()) {
			net.bluemind.core.api.calendar.Event e = oc.getEvent();
			if (e.getAlert() == null)
				continue;
			// heure a la quelle � la quelle l'alert doit �tre donn�e
			Long alertAt = oc.getBegin() - 1000 * e.getAlert();
			// si l'alert a lieux entre mtn et la prochaine recherche on
			// l'ajoute a la liste des rapelle de notre recherche
			if (alertAt <= nextSearch && alertAt > now.getTimeInMillis()) {
				listRecall.add(oc);
			}
		}
		failSearch = 0;
		return listRecall;

	}

	public boolean startPlanner(String user, String password, String host) {
		failSearch = 0;
		this.user = user;
		this.password = password;
		this.host = host;

		if (!openWebservice()) {
			return false;
		}
		task = new CalendarAnalyser(this) {

			@Override
			protected void onException(Exception e) {
				if (e instanceof AuthFault) {
					onAuthFail(e);
					return;
				} else if (e instanceof ServerFault) {
					if (failSearch > 3) {
						onDisconnect();
						return;
					}
					failSearch++;
					openWebservice();
				} else {

				}

			}
		};
		stopPlanner();
		plannerNextReminder = new Timer("planneNextReminder", true);
		plannerNextReminder.scheduleAtFixedRate(task, 0,
				ELAPSETIMEBETEEWNSEARCH);
		return true;
	}

	protected abstract void onDisconnect();

	protected abstract void onAuthFail(Exception e);

	public void stopPlanner() {
		if (plannerNextReminder != null) {
			task.stopNotifyTimer();
			plannerNextReminder.cancel();
			plannerNextReminder.purge();
			plannerNextReminder = null;
		}
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

}
