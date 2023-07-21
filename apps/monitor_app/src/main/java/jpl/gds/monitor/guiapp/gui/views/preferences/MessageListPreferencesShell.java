/*
 * Copyright 2006-2018. California Institute of Technology.
 * ALL RIGHTS RESERVED.
 * U.S. Government sponsorship acknowledged.
 *
 * This software is subject to U. S. export control laws and
 * regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 * extent that the software is subject to U.S. export control laws
 * and regulations, the recipient has the responsibility to obtain
 * export licenses or other export authority as may be required
 * before exporting such information to foreign countries or
 * providing access to foreign nationals.
 */
package jpl.gds.monitor.guiapp.gui.views.preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.springframework.context.ApplicationContext;

import jpl.gds.monitor.guiapp.gui.MessageFilterComposite;
import jpl.gds.monitor.perspective.view.MessageListViewConfiguration;
import jpl.gds.perspective.ChillTable;
import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.swt.ColorComposite;
import jpl.gds.shared.swt.FontComposite;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.swt.TitleComposite;

/**
 * MessageListPreferencesShell is the SWT GUI preferences window class for the 
 * message monitor's message list view.
 *
 */
public class MessageListPreferencesShell extends AbstractViewPreferences {
	/**
	 * Message list preferences window title
	 */
	public static final String TITLE = "Message List Preferences";
	
	/**
	 * Parent shell
	 */
	protected Shell parent;
	
	/**
	 * Textfield widget for entering maximum number of rows in table
	 */
	protected Text linesText;
	
	/**
	 * Array of message types that are selected
	 */
	protected String[] selectedTypes;
	
	/**
	 * Composite for filtering messages in the table
	 */
	protected MessageFilterComposite filterComp;
	
	/**
	 * Maximum number of rows permitted in the message list table
	 */
	protected int maxRows;
	
	private TableComposite tableConfigurer;
	private ChillTable messageTable;
	private boolean dirty;
	private boolean changeColumns;

    private final Tracer             trace;

	/**
	 * Constructor: Creates an instance of MessageListPreferencesShell.
	 * @param parent the parent display of this widget
	 */
	public MessageListPreferencesShell(final ApplicationContext appContext, final Shell parent) {
		super(appContext, TITLE);
		this.parent = parent;
        this.trace = TraceManager.getTracer(appContext, Loggers.DEFAULT);
		createControls();
		// this.prefShell.setSize(780, 765);
		prefShell.setLocation(parent.getLocation().x + 100, parent.getLocation().y + 100);
	}

	/**
	 * Creates all the preferences controls and composites.
	 */
	protected void createControls() {
		prefShell = new Shell(parent, SWT.SHELL_TRIM | 
				SWT.APPLICATION_MODAL);
		prefShell.setText(TITLE);
		final FormLayout fl = new FormLayout();
		fl.marginHeight = 5;
		fl.marginWidth = 5;
		fl.spacing = 5;
		prefShell.setLayout(fl);

		titleText = new TitleComposite(prefShell);
		final Composite titleComp = titleText.getComposite();
		final FormData fdLabel2 = new FormData();
		fdLabel2.top = new FormAttachment(0, 10);
		fdLabel2.left = new FormAttachment(0, 3);
		titleComp.setLayoutData(fdLabel2);       

		final Label linesLabel = new Label(prefShell, SWT.NONE);
		linesLabel.setText("Maximum # of List Entries:");
		final FormData fd1 = new FormData();
		fd1.top = new FormAttachment(titleComp, 15);
		fd1.left = new FormAttachment(0,4);
		linesLabel.setLayoutData(fd1);
		linesText = new Text(prefShell, SWT.SINGLE | SWT.BORDER);
		linesText.setText(String.valueOf(0));
		final FormData fd2 = SWTUtilities.getFormData(linesText, 1, 10);
		fd2.left = new FormAttachment(linesLabel);
		fd2.top = new FormAttachment(linesLabel, 0, SWT.CENTER);
		linesText.setLayoutData(fd2);

        fontGetter = new FontComposite(prefShell, "Data Font", trace);
		final Composite fontComp = fontGetter.getComposite();
		final FormData fontFd = new FormData();
		fontFd.top = new FormAttachment(linesText);
		fontFd.left = new FormAttachment(0);
		fontComp.setLayoutData(fontFd);

		foreColorGetter = new ColorComposite(prefShell,
                "Foreground Color", trace);
		final Composite foreColorComp = foreColorGetter.getComposite();
		final FormData foreColorFd = new FormData();
		foreColorFd.top = new FormAttachment(fontComp, 0, 7);
		foreColorFd.left = new FormAttachment(0, 2);
		foreColorFd.right = new FormAttachment(100);
		foreColorComp.setLayoutData(foreColorFd);

        backColorGetter = new ColorComposite(prefShell, "Background Color", trace);
		final Composite colorComp = backColorGetter.getComposite();
		final FormData colorFd = new FormData();
		colorFd.top = new FormAttachment(foreColorComp);
		colorFd.left = new FormAttachment(0);
		colorComp.setLayoutData(colorFd);

		filterComp = new MessageFilterComposite(prefShell);
		final Composite filterComposite = filterComp.getContainer();
		final FormData fdFilter = new FormData();
		fdFilter.left = new FormAttachment(0);
		fdFilter.right = new FormAttachment(100);
		fdFilter.top = new FormAttachment(colorComp, 5);
		filterComposite.setLayoutData(fdFilter);

		final Composite m = new Composite(prefShell, SWT.NONE);
		final FormData fd = new FormData();
		fd.top = new FormAttachment(filterComposite);
		fd.right = new FormAttachment(100);
		fd.left = new FormAttachment(0);
		m.setLayoutData(fd);
		final FillLayout layout = new FillLayout();
		layout.type = SWT.VERTICAL;
		m.setLayout(layout);

		tableConfigurer = new TableComposite(m);

		final Composite composite = new Composite(prefShell, SWT.NONE);
		final GridLayout rl = new GridLayout(2, true);
		composite.setLayout(rl);
		final FormData formData8 = new FormData();
		formData8.top = new FormAttachment(m);
		formData8.right = new FormAttachment(100);
		formData8.bottom = new FormAttachment(100);
		composite.setLayoutData(formData8);

		final Button applyButton = new Button(composite, SWT.PUSH);
		applyButton.setText("Ok");
		prefShell.setDefaultButton(applyButton);
		final GridData gd = new GridData();
		gd.horizontalAlignment = SWT.FILL;
		applyButton.setLayoutData(gd);
		final Button cancelButton = new Button(composite, SWT.PUSH);
		cancelButton.setText("Cancel");

		final Label line = new Label(prefShell, SWT.SEPARATOR | SWT.HORIZONTAL
				| SWT.SHADOW_ETCHED_IN);
		final FormData formData6 = new FormData();
		formData6.left = new FormAttachment(0, 3);
		formData6.right = new FormAttachment(100, 3);
		formData6.bottom = new FormAttachment(composite, 5);
		line.setLayoutData(formData6);

		applyButton.addSelectionListener(new SelectionAdapter() {
			/**
		     * {@inheritDoc}
			 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
			 */
			@Override
            public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				try {
					try {
						final int lines = Integer.parseInt(linesText.getText().trim());
						if (lines < 1) {
							SWTUtilities.showMessageDialog(MessageListPreferencesShell.this.prefShell, "Invalid Number",
									"The maximum number of list entries must be greater than 0.");
							return;                        
						}
						maxRows = lines;
					} catch (final NumberFormatException ex) {
						SWTUtilities.showMessageDialog(MessageListPreferencesShell.this.prefShell, "Invalid Number",
						"You must enter the maximum number of list entries as an integer.");
						return;
					}
					if (tableConfigurer.getTable().getActualColumnCount() == 0) {
					    SWTUtilities.showMessageDialog(MessageListPreferencesShell.this.prefShell, "No Columns",
					            "You cannot disable all the table columns, or the table will be blank.");
					    return;
					}
					applyChanges();
					dirty = tableConfigurer.getDirty();
					if (dirty) {
						changeColumns = true;
					}
					filterComp.applyChanges();
					selectedTypes = filterComp.getSelectedTypes();
					MessageListPreferencesShell.this.prefShell.close();
				} catch (final Exception e1) {
					e1.printStackTrace();
					MessageListPreferencesShell.this.prefShell.close();
				} 
			}
		});
		cancelButton.addSelectionListener(new SelectionAdapter() {
			/**
		     * {@inheritDoc}
			 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
			 */
			@Override
            public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				try {
					MessageListPreferencesShell.this.canceled = true;
					MessageListPreferencesShell.this.prefShell.close();
				} catch (final Exception e1) {
					e1.printStackTrace();
				}
			}
		});
		prefShell.pack();
	}

	/**
	 * Retrieves the list of selected message types. Note that a null
	 * return value means all types are selected. An empty array means
	 * that no message types were selected.
	 * @return the array of message types
	 */
	private String[] getSelectedMessageTypes() {
		return selectedTypes;
	}

	/**
	 * Returns the maximum list size entered by the user.
	 * @return the maximum number of rows in the table/list
	 */
	private int getMaxTableRows() {
		return maxRows;
	}

	/**
	 * Sets the maximum list size.
	 * @param size the new size to set
	 */
	private void setMaxTableRows(final int size) {
		maxRows = size;
		if (linesText != null && !linesText.isDisposed()) {
			linesText.setText(String.valueOf(size));
		}
	}

	/**
	 * Sets the selected message types.
	 * @param types the selected message types as an array of Strings
	 */
	private void setSelectedMessageTypes(final String[] types) {
		selectedTypes = types;
		if (filterComp != null && !filterComp.getContainer().isDisposed()) {
			filterComp.setSelectedTypes(types);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.monitor.guiapp.gui.views.preferences.AbstractViewPreferences#setValuesFromViewConfiguration(jpl.gds.perspective.view.ViewConfiguration)
	 */
	@Override
    public void setValuesFromViewConfiguration(final IViewConfiguration config) {
		super.setValuesFromViewConfiguration(config);
		final MessageListViewConfiguration msgConfig = (MessageListViewConfiguration)config;
		setMaxTableRows(msgConfig.getMaxRows());
		filterComp.setAllowedMessageTypes(msgConfig.getAllowedMessageTypes());
		setSelectedMessageTypes(msgConfig.getMessageTypes());
		dirty = false;
		changeColumns = false;
		messageTable = msgConfig.getTable(MessageListViewConfiguration.MESSAGE_TABLE_NAME);
		if (tableConfigurer != null && !tableConfigurer.getComposite().isDisposed()) {
			tableConfigurer.init(messageTable);
			prefShell.layout();
			prefShell.pack();
		}
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.monitor.guiapp.gui.views.preferences.AbstractViewPreferences#getValuesIntoViewConfiguration(jpl.gds.perspective.view.ViewConfiguration)
	 */
	@Override
    public void getValuesIntoViewConfiguration(final IViewConfiguration config) {
		super.getValuesIntoViewConfiguration(config);
		final MessageListViewConfiguration msgConfig = (MessageListViewConfiguration)config;
		msgConfig.setMaxRows(getMaxTableRows());
		msgConfig.setMessageTypes(getSelectedMessageTypes());
		if (tableConfigurer != null) {
			msgConfig.setTable(tableConfigurer.getTable());  
		}
	}

	/**
	 * Indicates whether the user has elected to change the columns in the message table.
	 * @return true if column change required
	 */
	public boolean needColumnChange() {
		return changeColumns;
	}
}
