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
package jpl.gds.telem.down.gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import jpl.gds.shared.swt.ChillShell;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.telem.down.DownConfiguration;

/**
 * DownConfigShell is a GUI window for displaying and modifying configuration
 * for the downlink test controller.
 *
 *
 * @see jpl.gds.telem.down.DownConfiguration
 */
public class DownConfigShell implements ChillShell {
	private static final String TITLE = "Downlink Control Configuration";
	private Shell configShell;
	private final Shell parent;
	private Text meterText;
	private boolean canceled;
	private final DownConfiguration config;
	private Button useJMS;
	private Button useDB;
	private Button showFrameSync;
	private Button showPacketExtract;
	private Button showRawData;
	private Button showControl;
	private Button showLog;
	private Button showWarning;
	private Button showError;
	private Button showInfo;
	private Group logFilterGroup;

	/**
	 * Creates an instance of DownConfigShell with a Shell as parent
	 * @param parent the parent Shell
	 * @param config the current Downlink Configuration object
	 */
	public DownConfigShell(final Shell parent, final DownConfiguration config) {
		this.parent = parent;
		this.config = config;
		createControls();
	}

	/**
	 * Creates an instance of DownConfigShell with a Display as parent
	 * @param display the parent Display
	 * @param config the current Downlink Configuration object
	 */
	public DownConfigShell(final Display display, final DownConfiguration config) {
		this(new Shell(display, SWT.NONE), config);
	}

	/**
	 * Creates and populates the GUI controls.
	 */
	private void createControls() {
		configShell = new Shell(parent, SWT.PRIMARY_MODAL
				| SWT.DIALOG_TRIM);
		configShell.setText(TITLE);
		configShell.setSize(450, 440);
		final FormLayout shellLayout = new FormLayout();
		shellLayout.spacing = 10;
		shellLayout.marginHeight = 5;
		shellLayout.marginWidth = 5;
		configShell.setLayout(shellLayout);

		Group configGroup = null;

		final FontData groupFontData = new FontData("Helvetica", 14, SWT.BOLD);
		final Font groupFont = new Font(configShell.getDisplay(), groupFontData);

		configGroup = new Group(configShell, SWT.SHADOW_ETCHED_IN);            
		configGroup.setText(TITLE);
		configGroup.setFont(groupFont);

		final FormData fd2 = new FormData();
		fd2.left = new FormAttachment(0);
		fd2.top = new FormAttachment(0, 10);      
		fd2.right = new FormAttachment(100);
		configGroup.setLayoutData(fd2);
		final FormLayout fl = new FormLayout();
		fl.spacing = 5;
		fl.marginHeight = 5;
		fl.marginWidth = 10;
		configGroup.setLayout(fl);

		final Label warningLabel = new Label(configGroup, SWT.NONE);
		//  Wrap text differently so it does not get cut off.
		warningLabel.setText("(Note: These settings are not effective after the Start \nbutton has been pressed.)");
		final FontData warnFontData = new FontData("Helvetica", 12, SWT.ITALIC);
		final Font warnFont = new Font(configShell.getDisplay(), warnFontData);
		warningLabel.setFont(warnFont);
		final FormData wlfd = new FormData();
		wlfd.top = new FormAttachment(3);
		wlfd.left = new FormAttachment(0);
		warningLabel.setLayoutData(wlfd);

		final Label meterLabel = new Label(configGroup, SWT.NONE);
		meterLabel.setText("Input Meter Interval (ms):");
		final FormData fd3 = new FormData();
		fd3.left = new FormAttachment(0);
		fd3.top = new FormAttachment(warningLabel, 7);            
		meterLabel.setLayoutData(fd3);
		meterText = new Text(configGroup, SWT.SINGLE | SWT.BORDER);
		meterText.setText("");
		final FormData fd5 = SWTUtilities.getFormData(meterText, 1, 10);
		fd5.left = new FormAttachment(meterLabel);
		fd5.top = new FormAttachment(meterLabel, 0, SWT.CENTER);        
		meterText.setLayoutData(fd5);
		meterText.setText(String.valueOf(config.getMeterInterval()));

		useJMS = new Button(configGroup, SWT.CHECK);
		useJMS.setText("Publish Messages");
		final FormData fduj = new FormData();
		fduj.top = new FormAttachment(meterText);
		fduj.left = new FormAttachment(0);
		useJMS.setLayoutData(fduj);
		useJMS.setSelection(config.isUseMessageService());
		useDB = new Button(configGroup, SWT.CHECK);
		useDB.setText("Write to Database");
		final FormData fdud = new FormData();
		fdud.top = new FormAttachment(useJMS);
		fdud.left = new FormAttachment(0);
		useDB.setLayoutData(fdud);
		useDB.setSelection(config.isUseDb());

		final Group filterGroup = new Group(configShell, SWT.BORDER | SWT.SHADOW_ETCHED_IN);
		filterGroup.setText("Message Filtering");
		filterGroup.setFont(groupFont);
		final FormData fgfd = new FormData();
		fgfd.top = new FormAttachment(configGroup, 5);
		fgfd.left = new FormAttachment(0);
		filterGroup.setLayoutData(fgfd);
		GridLayout gl = new GridLayout(1, false);
		filterGroup.setLayout(gl);
		showControl = new Button(filterGroup, SWT.CHECK);
		showControl.setText("Show Session Control Messages");
		showControl.setSelection(config.getMessageViewConfig().isShowTestControl());
		showFrameSync = new Button(filterGroup, SWT.CHECK);
		showFrameSync.setText("Show Frame Sync Messages");
		showFrameSync.setSelection(config.getMessageViewConfig().isShowFrameSync());
		showPacketExtract = new Button(filterGroup, SWT.CHECK);
		showPacketExtract.setText("Show Packet Extract Messages");
		showPacketExtract.setSelection(config.getMessageViewConfig().isShowPacketExtract());
		showRawData = new Button(filterGroup, SWT.CHECK);
		showRawData.setText("Show Raw Input Messages");
		showRawData.setSelection(config.getMessageViewConfig().isShowRawData());
		showLog = new Button(filterGroup, SWT.CHECK);
		showLog.setText("Show Log Messages");

		showLog.setSelection(config.getMessageViewConfig().isShowLog());
		showLog.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				enableLogGroup(showLog.getSelection());
			}
		});

		logFilterGroup = new Group(configShell, SWT.BORDER | SWT.SHADOW_ETCHED_IN);
		logFilterGroup.setText("Log Filtering");
		logFilterGroup.setFont(groupFont);
		final FormData lfgfd = new FormData();
		lfgfd.top = new FormAttachment(configGroup, 5);
		lfgfd.left = new FormAttachment(filterGroup);
		lfgfd.right = new FormAttachment(100);
		logFilterGroup.setLayoutData(lfgfd);
		gl = new GridLayout(1, false);
		logFilterGroup.setLayout(gl);
		showInfo = new Button(logFilterGroup, SWT.CHECK);
		showInfo.setText("Info");
		showInfo.setSelection(config.getMessageViewConfig().isShowInfoLog());
		showWarning = new Button(logFilterGroup, SWT.CHECK);
		showWarning.setText("Warning");
		showWarning.setSelection(config.getMessageViewConfig().isShowWarningLog());
		showError = new Button(logFilterGroup, SWT.CHECK);
		showError.setText("Error");
		showError.setSelection(config.getMessageViewConfig().isShowErrorLog());

		enableLogGroup(config.getMessageViewConfig().isShowLog());

		final Composite composite = new Composite(configShell, SWT.NONE);
		final GridLayout rl = new GridLayout(2, true);
		composite.setLayout(rl);
		final FormData formData8 = new FormData();
		formData8.right = new FormAttachment(100);
		formData8.bottom = new FormAttachment(100);
		composite.setLayoutData(formData8);

		final Button applyButton = new Button(composite, SWT.PUSH);
		applyButton.setText("Ok");
		configShell.setDefaultButton(applyButton);
		final GridData gd = new GridData();
		gd.horizontalAlignment = SWT.FILL;
		applyButton.setLayoutData(gd);
		Button cancelButton = null;

		cancelButton = new Button(composite, SWT.PUSH);
		cancelButton.setText("Cancel");

		final Label line = new Label(configShell, SWT.SEPARATOR | SWT.HORIZONTAL
				| SWT.SHADOW_ETCHED_IN);
		final FormData formData6 = new FormData();
		formData6.left = new FormAttachment(0, 3);
		formData6.right = new FormAttachment(100, 3);
		formData6.bottom = new FormAttachment(composite, 5);
		line.setLayoutData(formData6);

		applyButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				if (setConfigDataFromFields()) {
					configShell.close();
				}
			}
		});
		cancelButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				canceled = true;
				configShell.close();
			}
		});
	}

	private void enableLogGroup(final boolean enable) {
		showError.setEnabled(enable);
		showInfo.setEnabled(enable);
		showWarning.setEnabled(enable);
		logFilterGroup.setEnabled(enable);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.swt.ChillShell#open()
	 */
	@Override
	public void open() {
		configShell.open();
		meterText.setText(String.valueOf(config.getMeterInterval()));
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.swt.ChillShell#getShell()
	 */
	@Override
	public Shell getShell() {
		return configShell;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.swt.ChillShell#getTitle()
	 */
	@Override
	public String getTitle() {
		return TITLE;
	}

	/**
	 * Validates and sets configuration data from GUI fields.
	 * @return true if field entries are valid; false otherwise, and no configuration
	 * will be set
	 */
	protected boolean setConfigDataFromFields() {
		String meterStr = meterText.getText().trim();
		int meter = 0;
		if (meterStr.equals("")) {
			meterStr = "0";
			meterText.setText("0");
		} 
		try {
			meter = Integer.parseInt(meterStr);
		} catch (final NumberFormatException e) {
			SWTUtilities.showErrorDialog(configShell, "Invalid Meter Interval", 
			"Meter Interval must be an integer.");
			return false;
		}

		config.setMeterInterval(meter);
		config.setUseDb(useDB.getSelection());
		config.setUseMessageService(useJMS.getSelection());
		config.getMessageViewConfig().setShowFrameSync(showFrameSync.getSelection());
		config.getMessageViewConfig().setShowLog(showLog.getSelection());
		config.getMessageViewConfig().setShowPacketExtract(showPacketExtract.getSelection());            
		config.getMessageViewConfig().setShowRawData(showRawData.getSelection());
		config.getMessageViewConfig().setShowTestControl(showControl.getSelection());
        config.getMessageViewConfig().setLogFilters(showInfo.getSelection(), showWarning.getSelection(),
                                                    showError.getSelection());
		return true;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.swt.ChillShell#wasCanceled()
	 */
	@Override
	public boolean wasCanceled() {
		return canceled;
	}
}
