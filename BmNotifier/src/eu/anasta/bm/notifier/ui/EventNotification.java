package eu.anasta.bm.notifier.ui;

import java.util.Date;

import net.bluemind.core.api.calendar.Occurrence;

import org.eclipse.jface.action.StatusLineManager;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.nebula.widgets.cdatetime.CDT;
import org.eclipse.nebula.widgets.cdatetime.CDateTime;
import org.eclipse.nebula.widgets.pshelf.PShelf;
import org.eclipse.nebula.widgets.pshelf.PShelfItem;
import org.eclipse.nebula.widgets.pshelf.RedmondShelfRenderer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wb.swt.SWTResourceManager;

public class EventNotification extends ApplicationWindow {

	private static final int TIMEREFRESH = 1000 * 60;

	private PShelf shelf;

	private Text edt_lieu;
	private Text edt_organisateur;
	private Text edt_decription;

	private Composite container;

	private Runnable timer = null;


	public void addOccurence(Occurrence oc) {
		addNotification(oc);
		container.redraw();
		this.getShell().layout(true, true);

	}

	/**
	 * Create the application window.
	 */
	public EventNotification() {
		super(null);
	}

	private void closeAllEvent() {
		for (PShelfItem item : shelf.getItems()) {
			item.dispose();
		}
		this.getShell().layout(true, true);
		close();
	}

	@Override
	public int open() {
		int value = super.open();
		stopTimer();
		timer = new Runnable() {

			public void run() {
				for (PShelfItem item : shelf.getItems()) {
					Occurrence oc = (Occurrence) item.getData();
					item.setText(getLabel(oc));
					getShell().getDisplay().timerExec(TIMEREFRESH, this);
				}
			}
		};
		getShell().getDisplay().timerExec(TIMEREFRESH, timer);
		return value;
	}

	private void stopTimer() {
		if (timer != null)
			getShell().getDisplay().timerExec(-1, timer);
	}

	@Override
	public boolean close() {
		stopTimer();
		// TODO Auto-generated method stub
		return super.close();
	}

	/**
	 * Create contents of the application window.
	 * 
	 * @param parent
	 */
	@Override
	protected Control createContents(Composite parent) {
		container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(1, false));

		shelf = new PShelf(container, SWT.NONE);
		shelf.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 1, 1));
		shelf.setRenderer(new RedmondShelfRenderer());

//		
//		PShelfItem shelfItem = new PShelfItem(shelf, SWT.NONE);
//		shelfItem.setData(null);
//		shelfItem.setText("");
//		shelfItem.getBody().setLayout(new GridLayout(2, false));
//		
//		Label lblDate = new Label(shelfItem.getBody(), SWT.NONE);
//		lblDate.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
//		lblDate.setText("Date");
//		
//		Composite composite = new Composite(shelfItem.getBody(), SWT.NONE);
//		composite.setLayout(new RowLayout(SWT.HORIZONTAL));
//		
//		CDateTime dt_eventBegin = new CDateTime(composite, CDT.DATE_LONG | CDT.TIME_SHORT);
//		dt_eventBegin.setSelection(new Date(1353074345358L));
//		
//		Label lblNewLabel_3 = new Label(composite, SWT.NONE);
//		lblNewLabel_3.setText("Jusqu'au   ");
//		
//		CDateTime dt_eventEnd = new CDateTime(composite, CDT.DATE_LONG | CDT.TIME_SHORT);
//
//		Label lblNewLabel = new Label(shelfItem.getBody(), SWT.NONE);
//		lblNewLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false,
//				false, 1, 1));
//		lblNewLabel.setText("Lieu");
//
//		edt_lieu = new Text(shelfItem.getBody(), SWT.BORDER);
//		edt_lieu.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false,
//				1, 1));
//		edt_lieu.setEditable(false);
//		edt_lieu.setText("");
//
//		Label lblNewLabel_1 = new Label(shelfItem.getBody(), SWT.NONE);
//		lblNewLabel_1.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false,
//				false, 1, 1));
//		lblNewLabel_1.setText("Organisateur");
//
//		edt_organisateur = new Text(shelfItem.getBody(), SWT.BORDER);
//		edt_organisateur.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
//				false, 1, 1));
//		edt_organisateur.setEditable(false);
//		// edt_organisateur.setText(oc.getEvent().getOwnerDisplayName());
//
//		Label lblNewLabel_2 = new Label(shelfItem.getBody(), SWT.NONE);
//		lblNewLabel_2.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false,
//				false, 1, 1));
//		lblNewLabel_2.setText("Description");
//
//		edt_decription = new Text(shelfItem.getBody(), SWT.BORDER | SWT.WRAP);
//		GridData gd_edt_decription = new GridData(SWT.FILL, SWT.CENTER, true,
//				false, 1, 1);
//		gd_edt_decription.heightHint = 70;
//		edt_decription.setLayoutData(gd_edt_decription);
//		edt_decription.setEditable(false);
//		edt_decription.setText("");
//
//		Button btnNewButton = new Button(shelfItem.getBody(), SWT.NONE);
//		btnNewButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false,
//				false, 1, 1));
//		btnNewButton.setText("Ok");
//		new Label(shelfItem.getBody(), SWT.NONE);
//		btnNewButton.addSelectionListener(new closeNotEvent(shelfItem));
//		shelf.setSelection(shelfItem);

		
		
		
		Button btnNewButton_1 = new Button(container, SWT.NONE);
		btnNewButton_1.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true,
				false, 1, 1));
		btnNewButton_1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				closeAllEvent();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		btnNewButton_1.setText("Tout fermer");
		return container;
	}

	private String getLabel(Occurrence oc){
		long now = new Date().getTime() ;
		long remindAt = (oc.getBegin() -now)/1000;
		return oc.getEvent().getTitle()+ " commence dans "+  String.format("%d:%02d", remindAt/3600, (((remindAt%3600)+40)/60));
	}

	private void addNotification(Occurrence oc) {
		PShelfItem shelfItem = new PShelfItem(shelf, SWT.V_SCROLL);
		shelfItem.setData(oc);
		shelfItem.setText(getLabel(oc));
		shelfItem.getBody().setLayout(new GridLayout(2, false));

		Label lblDate = new Label(shelfItem.getBody(), SWT.NONE);
		lblDate.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblDate.setText("Date");
		
		Composite composite = new Composite(shelfItem.getBody(), SWT.NONE);
		composite.setLayout(new RowLayout(SWT.HORIZONTAL));
		
		CDateTime dt_eventBegin = new CDateTime(composite, CDT.DATE_LONG | CDT.TIME_SHORT);
		dt_eventBegin.setSelection(new Date( oc.getBegin()));
		
		Label lblNewLabel_3 = new Label(composite, SWT.NONE);
		lblNewLabel_3.setText("Jusqu'au   ");
		
		CDateTime dt_eventEnd = new CDateTime(composite, CDT.DATE_LONG | CDT.TIME_SHORT);
		dt_eventEnd.setSelection(new Date( oc.getEnd()));
		
		Label lblNewLabel = new Label(shelfItem.getBody(), SWT.NONE);
		lblNewLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false,
				false, 1, 1));
		lblNewLabel.setText("Lieu");

		edt_lieu = new Text(shelfItem.getBody(), SWT.BORDER);
		edt_lieu.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false,
				1, 1));
		edt_lieu.setEditable(false);
		edt_lieu.setText(oc.getEvent().getLocation());

		Label lblNewLabel_1 = new Label(shelfItem.getBody(), SWT.NONE);
		lblNewLabel_1.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false,
				false, 1, 1));
		lblNewLabel_1.setText("Organisateur");

		edt_organisateur = new Text(shelfItem.getBody(), SWT.BORDER);
		edt_organisateur.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
				false, 1, 1));
		edt_organisateur.setEditable(false);
		// edt_organisateur.setText(oc.getEvent().getOwnerDisplayName());

		Label lblNewLabel_2 = new Label(shelfItem.getBody(), SWT.NONE);
		lblNewLabel_2.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false,
				false, 1, 1));
		lblNewLabel_2.setText("Description");

		edt_decription = new Text(shelfItem.getBody(), SWT.BORDER | SWT.WRAP);
		GridData gd_edt_decription = new GridData(SWT.FILL, SWT.CENTER, true,
				false, 1, 1);
		gd_edt_decription.heightHint = 70;
		edt_decription.setLayoutData(gd_edt_decription);
		edt_decription.setEditable(false);
		edt_decription.setText(oc.getEvent().getDescription());

		Button btnNewButton = new Button(shelfItem.getBody(), SWT.NONE);
		btnNewButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false,
				false, 1, 1));
		btnNewButton.setText("Ok");
		btnNewButton.addSelectionListener(new closeNotEvent(shelfItem));
		shelf.setSelection(shelfItem);

	}

	private class closeNotEvent extends SelectionAdapter {
		private PShelfItem shelfItem;

		public closeNotEvent(PShelfItem shelfItem) {
			this.shelfItem = shelfItem;
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			shelfItem.dispose();
			getShell().layout(true, true);
			super.widgetSelected(e);
			// getContents().redraw();
		}
	}

	/**
	 * Create the status line manager.
	 * 
	 * @return the status line manager
	 */
//	@Override
//	protected StatusLineManager createStatusLineManager() {
//		StatusLineManager statusLineManager = new StatusLineManager();
//		return statusLineManager;
//	}

	/**
	 * Launch the application.
	 * 
	 * @param args
	 */
	public static void main(String args[]) {
		try {
			EventNotification window = new EventNotification();
			window.setBlockOnOpen(true);
			window.open();
			Display.getCurrent().dispose();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Configure the shell.
	 * 
	 * @param newShell
	 */
	@Override
	protected void configureShell(Shell newShell) {
		newShell.setImage(SWTResourceManager.getImage(EventNotification.class, "/16/bm.png"));
		super.configureShell(newShell);
		newShell.setText("Reminder");
	}

	/**
	 * Return the initial size of the window.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(453, 334);
	}

}
