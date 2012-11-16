package eu.anasta.bm.notifier.calendar;

import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import net.bluemind.core.api.calendar.Occurrence;
import eu.anasta.bm.notifier.ui.Notification;

public abstract class CalendarAnalyser extends TimerTask {

	CalendarManager cal;
	private Timer notifyReminder;

	public CalendarAnalyser(CalendarManager cal) {
		super();
		this.cal = cal;
	}

	@Override
	public void run() {
		stopNotifyTimer();
		notifyReminder = new Timer("Timer for Notify event");
		List<Occurrence> ocs;
		try {
			ocs = cal.findNextRecallEvent();
			System.out.println("find and schedul next reminder");
			for (Occurrence oc : ocs) {
				NotifyTask task = new NotifyTask(oc);
				Date schedul = new Date(oc.getBegin()
						- (oc.getEvent().getAlert() * 1000));
				System.out.println("scheldult event at " + schedul);
				notifyReminder.schedule(task, schedul);
			}
		} catch (Exception e) {
			onException(e);
			e.printStackTrace();
		}

	}

	public void stopNotifyTimer() {
		if (notifyReminder == null) {
			return;
		}
		notifyReminder.cancel();
		notifyReminder.purge();
		notifyReminder = null;

	}

	private class NotifyTask extends TimerTask {
		private Occurrence occurence;

		public NotifyTask(Occurrence oc) {
			this.occurence = oc;
		}

		@Override
		public void run() {
			Notification.getInstance().eventNotification(occurence);
//			Display.getDefault().asyncExec(new Runnable() {
//
//				@Override
//				public void run() {
//					// windowEventNotif.open();
//					// windowEventNotif.addOccurence(occurence);
//					// windowEventNotif.refresh();
//				}
//			});
		}

	}

	protected abstract void onException(Exception e);

}
