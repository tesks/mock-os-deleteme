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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.swt.ChillShell;
import jpl.gds.shared.swt.FileEntryComposite;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.swt.StringSelectorComposite;

/**
 * UnmarkSuspectChannelShell is a GUI window that allows the user to mark a channel as questionable.
 *
 *
 */
public class UnmarkSuspectChannelShell implements ChillShell {
	private static final String TITLE = "Suspect Channel Removal";
	private Shell mainShell;
	private final Shell parent;
	private StringSelectorComposite channelSelector;
	private FileEntryComposite fileComposite;
	private boolean canceled;
	private String[] selectedChannels;
	private String filePath;
	
	/**
	 * The actual list of suspect channels.
	 */
	protected String[] suspectChans;

	/**
	 * Creates an instance of UnmarkSuspectChannelShell.
	 * @param parent the parent Shell
	 * @param filePath the path to the current suspect channel file
	 * @param startChans the list of channels on the initial suspect channel list
	 */
	public UnmarkSuspectChannelShell(Shell parent, String filePath, java.util.List<String> startChans) {
		this.parent = parent;
		this.filePath = filePath;
		suspectChans = new String[startChans.size()];
		startChans.toArray(suspectChans);
		Arrays.sort(suspectChans);
		createControls();  
	}

	/**
	 * Retrieves the channel IDs the user selected to be marked as no longer suspicious.
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
		FormLayout shellLayout = new FormLayout();
		shellLayout.spacing = 5;
		shellLayout.marginHeight = 5;
		shellLayout.marginWidth = 5;
		mainShell.setLayout(shellLayout);

		SortedSet<String> chans = new TreeSet<String>(Arrays.asList(suspectChans));
		
		channelSelector = new StringSelectorComposite(mainShell, chans);
		FormData fd = new FormData();
		fd.top = new FormAttachment(0);
		fd.left = new FormAttachment(0);
		channelSelector.getComposite().setLayoutData(fd);

        fileComposite = new FileEntryComposite(mainShell, "Current Suspect Channel File:", "Save As...",
                TraceManager.getDefaultTracer());
		FormData fdc = new FormData();
		fdc.top = new FormAttachment(channelSelector.getComposite());
		fdc.left = new FormAttachment(0);
		fdc.right = new FormAttachment(100);
		fileComposite.getComposite().setLayoutData(fdc);
		fileComposite.setCurrentFile(filePath);

		Composite composite = new Composite(mainShell, SWT.NONE);
		GridLayout rl = new GridLayout(3, true);
		composite.setLayout(rl);
		FormData formData8 = new FormData();
		formData8.right = new FormAttachment(100);
		formData8.bottom = new FormAttachment(100);
		formData8.top = new FormAttachment(fileComposite.getComposite());
		composite.setLayoutData(formData8);

		Button applyButton = new Button(composite, SWT.PUSH);
		applyButton.setText("Unmark");
		mainShell.setDefaultButton(applyButton);
		GridData gd = new GridData();
		gd.horizontalAlignment = SWT.FILL;
		applyButton.setLayoutData(gd);

		Button closeButton = new Button(composite, SWT.PUSH);
		closeButton.setText("Unmark and Close");
		gd = new GridData();
		gd.horizontalAlignment = SWT.FILL;
		closeButton.setLayoutData(gd);

		Button cancelButton = new Button(composite, SWT.PUSH);
		cancelButton.setText("Cancel");
		Label line = new Label(mainShell, SWT.SEPARATOR | SWT.HORIZONTAL
				| SWT.SHADOW_ETCHED_IN);
		FormData formData6 = new FormData();
		formData6.left = new FormAttachment(0, 3);
		formData6.right = new FormAttachment(100, 3);
		formData6.bottom = new FormAttachment(composite, 5);
		line.setLayoutData(formData6);

		applyButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				try {
					if (channelSelector.getSelectedStrings() == null) {
						SWTUtilities.showErrorDialog(mainShell, "No Channel Selected", "You must select at least one channel ID");
						return;
					}
					filePath = fileComposite.getCurrentFile().trim();
					selectedChannels = appendArrays(selectedChannels, channelSelector.getSelectedStrings());
					suspectChans = removeArrayFromArray(suspectChans, channelSelector.getSelectedStrings());
					Arrays.sort(suspectChans);
					channelSelector.setStrings(suspectChans);
					channelSelector.clearSelection();
				} catch(Exception eE) {
					TraceManager.getDefaultTracer().error("APPLY button caught unhandled and unexpected exception in UnmarkSuspectChannelShell.java" );

					eE.printStackTrace();
				}
			}
		});
		cancelButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				try {
					canceled = true;
					mainShell.close();
				} catch(Exception eE){
					TraceManager.getDefaultTracer ().error ( "CANCEL button caught unhandled and unexpected exception in SuspectChannelShell.java" );

				}
			}
		});

		closeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				try {

					filePath = fileComposite.getCurrentFile().trim();
					selectedChannels = appendArrays(selectedChannels, channelSelector.getSelectedStrings());

					canceled = false;
					mainShell.close();
				} catch(Exception eE) {
					TraceManager.getDefaultTracer().error("APPLY button caught unhandled and unexpected exception in UnmarkSuspectChannelShell.java" );

					eE.printStackTrace();
				}
			}
		});


		mainShell.pack();
	}

	private String[] appendArrays(String[] arr1, String[] arr2) {
		if (arr1 == null) {
			return arr2;
		} else if (arr2 == null) {
			return arr1;
		} else {
			String[] temp = new String[arr1.length + arr2.length];
			System.arraycopy(arr1, 0, temp, 0, arr1.length);
			System.arraycopy(arr2, 0, temp, arr1.length, arr2.length);
			return temp;
		}
	}

	private String[] removeArrayFromArray(String[] arr1, String[] arr2) {
		if (arr1 == null) {
			return null;
		} else if (arr2 == null) {
			return arr1;
		} else {
			String[] temp = new String[arr1.length - arr2.length];
			ArrayList<String> tempList = new ArrayList<String>(arr1.length - arr2.length);
			for (String val : arr1) {
				tempList.add(val);
			}
			for (String val: arr2) {
				tempList.remove(val);
			}
			tempList.toArray(temp);
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
