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

import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;

import jpl.gds.dictionary.api.channel.IChannelDefinitionProvider;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.swt.ChillColorCreator;
import jpl.gds.shared.swt.ChillShell;
import jpl.gds.shared.swt.FileEntryComposite;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.swt.StringSelectorComposite;
import jpl.gds.shared.swt.types.ChillColor;

/**
 * MarkSuspectChannelShell is a GUI window that allows the user to mark a channel as questionable.
 *
 *
 */
public class MarkSuspectChannelShell implements ChillShell {
	private static final String TITLE = "Suspect Channel Selection";
	private Shell mainShell;
	private final Shell parent;
	private Button dnCheckbox;
	private Button euCheckbox;
	private Button alarmCheckbox;
	private StringSelectorComposite channelSelector;
	private FileEntryComposite fileComposite;
	private List suspectChannelList;
	private boolean canceled;
	private boolean isDnSuspicious;
	private boolean isEuSuspicious;
	private boolean isAlarmSuspicious;
	private String[] selectedChannels;
	private String filePath;
	/**
	 * The actual suspect channel list.
	 */
	protected String[] suspectChans;
	
	private final IChannelDefinitionProvider channelTable;

	/**
	 * Creates an instance of MarkSuspectChannelShell.
	 * @param parent the parent Shell
	 * @param filePath the path to the current suspect channel file
	 * @param startChans the list of initial suspect channels
	 * @param chanTable the table of channel definitions
	 */
	public MarkSuspectChannelShell(final Shell parent, final String filePath, final java.util.List<String> startChans,
	        final IChannelDefinitionProvider chanTable) {
		this.parent = parent;
		this.filePath = filePath;
		suspectChans = new String[startChans.size()];
		startChans.toArray(suspectChans);
		Arrays.sort(suspectChans);
		channelTable = chanTable;
		createControls();
	}

	/**
	 * Gets the flag indicating if channel DN should be considered suspicious.
	 * 
	 * @return true if DN is suspect, false if not
	 */
	public boolean isDn() {
		return isDnSuspicious;
	}

	/**
	 * Gets the flag indicating if channel EU should be considered suspicious.
	 * 
	 *  @return true if EU is suspect, false if not
	 */
	public boolean isEu() {
		return isEuSuspicious;
	}

	/**
	 * Gets the flag indicating if channel alarms should be considered suspicious.
	 * 
	 *  @return true if alarm is suspect, false if not
	 */
	public boolean isAlarm() {
		return isAlarmSuspicious;
	}

	/**
	 * Retrieves the channel IDs the user selected to be marked as suspicious.
	 * @return channel ID
	 */
	public String[] getSelectedChannels() {
		return selectedChannels;
	}

	/**
	 * Gets the path to the suspect channel file.
	 * @return the current file path
	 */
	public String getFilePath() {
		return filePath;
	}

	/**
	 * Creates the GUI controls.
	 */
	private void createControls() {

		mainShell = new Shell(parent, SWT.DIALOG_TRIM);
		mainShell.setText(TITLE);
		mainShell.setSize(625, 160);
		final FormLayout shellLayout = new FormLayout();
		shellLayout.spacing = 5;
		shellLayout.marginHeight = 5;
		shellLayout.marginWidth = 5;
		mainShell.setLayout(shellLayout);

		final Label suspectLabel = new Label(mainShell, SWT.NONE);
		suspectLabel.setText("Current Suspect Channels:");
		FormData fds = new FormData();
		fds.top = new FormAttachment(0, 5);
		fds.left = new FormAttachment(0, 5);
		suspectLabel.setLayoutData(fds);

		suspectChannelList = new List(mainShell, SWT.READ_ONLY | SWT.V_SCROLL);
		fds = SWTUtilities.getFormData(suspectChannelList, 15, 20);
		fds.top = new FormAttachment(suspectLabel);
		fds.left = new FormAttachment(0, 10);
		suspectChannelList.setLayoutData(fds);
		suspectChannelList.setItems(suspectChans);
		suspectChannelList.setBackground(ChillColorCreator.getColor(new ChillColor(ChillColor.ColorName.LIGHT_GREY)));

		final Label sep = new Label(mainShell, SWT.SEPARATOR | SWT.VERTICAL);
		final FormData sepFd = new FormData();
		sepFd.left =  new FormAttachment(suspectLabel);
		sepFd.top = new FormAttachment(0);
		sep.setLayoutData(sepFd);

		channelSelector = new StringSelectorComposite(mainShell, channelTable.getChanIds());
		final FormData fd = new FormData();
		fd.top = new FormAttachment(0);
		fd.left = new FormAttachment(sep, 0 , 10);
		channelSelector.getComposite().setLayoutData(fd);

		final Composite checkComposite = new Composite(mainShell, SWT.BORDER);
		final RowLayout gl = new RowLayout(SWT.VERTICAL);
		checkComposite.setLayout(gl);
		final FormData cfd = new FormData();
		cfd.left = new FormAttachment(channelSelector.getComposite());
		cfd.top = new FormAttachment(30);
		checkComposite.setLayoutData(cfd);

		dnCheckbox = new Button(checkComposite, SWT.CHECK);
		dnCheckbox.setText("DN is Suspect");
		dnCheckbox.setSelection(true);
		dnCheckbox.setEnabled(false);
		euCheckbox = new Button(checkComposite, SWT.CHECK);
		euCheckbox.setText("EU is Suspect");
		euCheckbox.setSelection(true);
		euCheckbox.setEnabled(false);
		alarmCheckbox = new Button(checkComposite, SWT.CHECK);
		alarmCheckbox.setText("Alarm is Suspect");
		alarmCheckbox.setSelection(true);
		alarmCheckbox.setEnabled(false);

        fileComposite = new FileEntryComposite(mainShell, "Current Suspect Channel File:", "Save As...",
                TraceManager.getDefaultTracer());
		final FormData fdc = new FormData();
		fdc.top = new FormAttachment(channelSelector.getComposite());
		fdc.left = new FormAttachment(0);
		fdc.right = new FormAttachment(100);
		fileComposite.getComposite().setLayoutData(fdc);
		fileComposite.setCurrentFile(filePath);

		sepFd.bottom = new FormAttachment(fileComposite.getComposite());

		final Composite composite = new Composite(mainShell, SWT.NONE);
		final GridLayout rl = new GridLayout(3, true);
		composite.setLayout(rl);
		final FormData formData8 = new FormData();
		formData8.right = new FormAttachment(100);
		formData8.bottom = new FormAttachment(100);
		formData8.top = new FormAttachment(fileComposite.getComposite());
		composite.setLayoutData(formData8);

		final Button applyButton = new Button(composite, SWT.PUSH);
		applyButton.setText("Mark");
		mainShell.setDefaultButton(applyButton);
		GridData gd = new GridData();
		gd.horizontalAlignment = SWT.FILL;
		applyButton.setLayoutData(gd);

		final Button closeButton = new Button(composite, SWT.PUSH);
		closeButton.setText("Mark and Close");
		gd = new GridData();
		gd.horizontalAlignment = SWT.FILL;
		closeButton.setLayoutData(gd);

		final Button cancelButton = new Button(composite, SWT.PUSH);
		cancelButton.setText("Cancel");
		final Label line = new Label(mainShell, SWT.SEPARATOR | SWT.HORIZONTAL
				| SWT.SHADOW_ETCHED_IN);
		final FormData formData6 = new FormData();
		formData6.left = new FormAttachment(0, 3);
		formData6.right = new FormAttachment(100, 3);
		formData6.bottom = new FormAttachment(composite, 5);
		line.setLayoutData(formData6);

		applyButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				try {
					if (channelSelector.getSelectedStrings() == null) {
						SWTUtilities.showErrorDialog(mainShell, "No Channel Selected", "You must select at least one channel ID");
						return;
					}
					if (fileComposite.getCurrentFile() == null) {
						SWTUtilities.showErrorDialog(mainShell, "No File Path", "You must specify a file path to the suspect channel file");
						return;
					}
					filePath = fileComposite.getCurrentFile().trim();                    
					selectedChannels = appendArrays(selectedChannels, channelSelector.getSelectedStrings());
					isDnSuspicious = dnCheckbox.getSelection();
					isEuSuspicious = euCheckbox.getSelection();
					isAlarmSuspicious = alarmCheckbox.getSelection();
					suspectChans = appendArrays(suspectChans, channelSelector.getSelectedStrings());
					Arrays.sort(suspectChans);
					suspectChannelList.setItems(suspectChans);
					channelSelector.clearSelection();

				} catch(final Exception eE) {
					TraceManager.getDefaultTracer().error("APPLY button caught unhandled and unexpected exception in SuspectChannelShell.java" );

					eE.printStackTrace();
				}
			}
		});

		cancelButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				try {
					canceled = true;
					mainShell.close();
				} catch(final Exception eE){
					TraceManager.getDefaultTracer ().error ( "CANCEL button caught unhandled and unexpected exception in SuspectChannelShell.java" );

				}
			}
		});

		closeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				try {              
					filePath = fileComposite.getCurrentFile().trim();
					selectedChannels = appendArrays(selectedChannels, channelSelector.getSelectedStrings());
					isDnSuspicious = dnCheckbox.getSelection();
					isEuSuspicious = euCheckbox.getSelection();
					isAlarmSuspicious = alarmCheckbox.getSelection();

					canceled = false;
					mainShell.close();
				} catch(final Exception eE){
					TraceManager.getDefaultTracer ().error ( "CLOSE button caught unhandled and unexpected exception in SuspectChannelShell.java" );

				}
			}
		});


		mainShell.pack();
	}

	private String[] appendArrays(final String[] arr1, final String[] arr2) {
		if (arr1 == null) {
			return arr2;
		} else if (arr2 == null) {
			return arr1;
		} else {
			final String[] temp = new String[arr1.length + arr2.length];
			System.arraycopy(arr1, 0, temp, 0, arr1.length);
			System.arraycopy(arr2, 0, temp, arr1.length, arr2.length);
			return temp;
		}
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.swt.ChillShell#open()
	 */
	@Override
	public void open() {
		canceled = true;
		mainShell.open();
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.swt.ChillShell#getShell()
	 */
	@Override
	public Shell getShell() {
		return mainShell;
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
	 * {@inheritDoc}
	 * @see jpl.gds.shared.swt.ChillShell#wasCanceled()
	 */
	@Override
	public boolean wasCanceled() {
		return canceled;
	}
}
