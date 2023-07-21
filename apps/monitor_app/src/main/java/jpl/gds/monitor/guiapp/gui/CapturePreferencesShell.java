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
package jpl.gds.monitor.guiapp.gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import jpl.gds.message.api.MessageUtility;
import jpl.gds.message.api.external.MessageHeaderMode;
import jpl.gds.message.api.util.MessageCaptureHandler;
import jpl.gds.message.api.util.MessageCaptureHandler.CaptureType;
import jpl.gds.shared.message.CommonMessageType;
import jpl.gds.shared.swt.ChillShell;
import jpl.gds.shared.swt.SWTUtilities;

/**
 * CapturePreferencesShell is the SWT GUI preferences window class for the 
 * message monitor file capture function.
 *
 */
public class CapturePreferencesShell implements ChillShell {
	/**
	 * Capture preferences shell title
	 */
	public static final String TITLE = "Capture Preferences";
	
	/**
	 * The shell object itself
	 */
	protected Shell prefShell;
	
	/**
	 * Button for confirming changes
	 */
	protected Button applyButton;
	
	/**
	 * Button for exiting out of shell without making changes
	 */
	protected Button cancelButton;
	
	/**
	 * Composite that contains preferences related to message filtering for 
	 * capture files
	 */
	protected CaptureFilterComposite filterPanel;
	
	/**
	 * Composite that has options for where to capture file to
	 */
	protected CaptureDestinationPanel destPanel;
	
	/**
	 * Enable headers check box
	 */
	protected Button showHeaders;
	
	/**
	 * Parent shell
	 */
	protected Shell parent;
	
	/**
	 * Shell cancelled flag 
	 */
	protected boolean canceled;
	
	/**
	 * File capture format drop down menu
	 */
	protected Combo formatSelect;

	private final MessageCaptureHandler captureHandler;

	/**
	 * Creates an instance of CapturePreferencesShell.
	 * @param parent the parent Shell of this widget
	 */
	public CapturePreferencesShell(final Shell parent, final MessageCaptureHandler capture) {
		this.parent = parent;
		this.captureHandler = capture;
		createControls();
		prefShell.pack();
	}

	/**
     * {@inheritDoc}
	 * @see jpl.gds.shared.swt.ChillShell#open()
	 */
	@Override
    public void open() {
		prefShell.open();
		filterPanel.refreshFromData();
	}

	/**
     * {@inheritDoc}
	 * @see jpl.gds.shared.swt.ChillShell#getShell()
	 */
	@Override
    public Shell getShell() {
		return prefShell;
	}

	/**
     * {@inheritDoc}
	 * @see jpl.gds.shared.swt.ChillShell#wasCanceled()
	 */
	@Override
    public boolean wasCanceled() {
		return canceled;
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
	 * Creates all the preferences controls and composites.
	 */
	protected void createControls() {
		prefShell = new Shell(parent, SWT.DIALOG_TRIM | 
				SWT.APPLICATION_MODAL);
		prefShell.setText("Preferences");
		final FormLayout fl = new FormLayout();
		fl.marginHeight = 5;
		fl.marginWidth = 5;
		fl.spacing = 5;
		prefShell.setLayout(fl);

		filterPanel = new CaptureFilterComposite(prefShell, captureHandler);
		final Composite filterGroup = filterPanel.getContainer();
		final FormData formData2 = new FormData();
		formData2.left = new FormAttachment(0);
		formData2.right = new FormAttachment(100);
		filterGroup.setLayoutData(formData2);

		final Label formatLabel = new Label(prefShell, SWT.RIGHT);
		formatLabel.setText("Preferred Message Save Format:");
		final FormData formData3 = new FormData();
		formData3.left = new FormAttachment(0);
		formatLabel.setLayoutData(formData3);

		formatSelect = new Combo(prefShell, SWT.DROP_DOWN | SWT.READ_ONLY);
		final FormData formData44 = new FormData();
		formData44.left = new FormAttachment(formatLabel);
		formData44.top = new FormAttachment(filterPanel.getContainer());
		formatSelect.setLayoutData(formData44);
		
		final String[] formats = MessageUtility.getMessageStyles(CommonMessageType.Log).toArray(new String[] {});
		for (int i = 0; i < formats.length; i++) {
			formatSelect.add(formats[i]);
		}

		//Set the default format to xml
		formatSelect.setText(captureHandler.getCaptureMessageStyle().toLowerCase());

		formData3.top = new FormAttachment(formatSelect, 0 , SWT.CENTER);

		showHeaders = new Button(prefShell, SWT.CHECK);
		showHeaders.setSelection(captureHandler.getHeaderMode() == MessageHeaderMode.HEADERS_ON);
		showHeaders.setText("Include Message Header Info");
		final FormData formData5 = new FormData();
		formData5.left = new FormAttachment(formatSelect, 30);
		formData5.top = new FormAttachment(formatSelect, 0, SWT.CENTER);
		showHeaders.setLayoutData(formData5);

		final Composite buttonComp = new Composite(prefShell, SWT.NONE);
		final GridLayout rl = new GridLayout(2, true);
		buttonComp.setLayout(rl);             
		final FormData formData4 = new FormData();
		formData4.right = new FormAttachment(100);
		//formData4.bottom = new FormAttachment(100);
		formData4.top = new FormAttachment(formatSelect, 0, SWT.CENTER);
		buttonComp.setLayoutData(formData4);

		applyButton = new Button(buttonComp, SWT.PUSH);
		applyButton.setText("Start Capture");
		GridData gd1 = new GridData();
		gd1.horizontalAlignment = SWT.FILL;
		applyButton.setLayoutData(gd1);

		cancelButton = new Button(buttonComp, SWT.PUSH);
		cancelButton.setText("Cancel");
		gd1 = new GridData();
		gd1.horizontalAlignment = SWT.FILL;
		cancelButton.setLayoutData(gd1);

		prefShell.setDefaultButton(applyButton);

		destPanel = new CaptureDestinationPanel();
		final FormData formData55 = new FormData();
		formData55.left = new FormAttachment(0);
		formData55.top = new FormAttachment(0);        
		formData55.right = new FormAttachment(100);
		destPanel.getContainer().setLayoutData(formData55);

		formData2.top = new FormAttachment(destPanel.getContainer());

		final Label line = new Label(prefShell, SWT.SEPARATOR | 
				SWT.HORIZONTAL | SWT.SHADOW_ETCHED_IN);
		final FormData formData6 = new FormData();
		formData6.left = new FormAttachment(0,3);
		formData6.right = new FormAttachment(100,3);
		formData6.bottom = new FormAttachment(buttonComp, 5);
		line.setLayoutData(formData6);

		cancelButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				canceled = true;
				prefShell.dispose();
				prefShell = null;
			}
		});

		applyButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				if (showHeaders.getSelection()) {
					captureHandler.setHeaderMode(MessageHeaderMode.HEADERS_ON);
				} else {
					captureHandler.setHeaderMode(MessageHeaderMode.HEADERS_OFF);
				}
				if (!filterPanel.applyChanges()) {
					return;
				}
				if (!destPanel.applyChanges()) {
					return;
				}
				final String format = formatSelect.getText();
				captureHandler.setCaptureMessageStyle(format);
				prefShell.dispose();
				prefShell = null;
			}
		});
	}

	/**
	 * Inner panel in the main file capture window for choosing the 
	 * destination where the file should be saved
	 *
	 */
	public class CaptureDestinationPanel {

	    /**
	     * Composite that contains GUI components
	     */
		protected Composite comp;
		
		/**
		 * Capture to file option button
		 */
		protected Button fileCaptureButton;
		
		/**
		 * Capture to directory option button
		 */
		protected Button dirCaptureButton;
		
		/**
		 * Browse file system button
		 */
		protected Button browseButton;
		
		/**
		 * Label that precedes textfield that will contain location file will 
		 * be written to
		 */
		protected Label outputLabel;
		
		/**
		 * Text that indicates where file will be written to
		 */
		protected Text outputText;
		
		/**
		 * Custom SWT utilities object
		 */
		protected SWTUtilities swtUtil;

		/**
		 * Constructor: creates the GUI components
		 */
		public CaptureDestinationPanel() {
			createControls();
		}

		/**
		 * Enables/disables fields on the capture window based upon current settings.
		 */
		protected void enableCaptureControls() {
			final boolean enable = dirCaptureButton.getSelection() || fileCaptureButton.getSelection();
			outputText.setEditable(enable);
			outputLabel.setEnabled(enable);
			browseButton.setEnabled(enable);
		}

		/**
		 * Gets the file capture destination composite
		 * @return capture destination composite
		 */
		protected Composite getContainer() {
			return comp;
		}

		/**
		 * Creates all the GUI components and adds them to the composite
		 */
		public void createControls() {
			final CaptureType mode = captureHandler.getWriteMode();      
			comp = new Composite(prefShell, SWT.NONE);
			final FormLayout shellLayout = new FormLayout();
			shellLayout.spacing = 5;
			shellLayout.marginHeight = 5;
			shellLayout.marginWidth = 5;
			comp.setLayout(shellLayout);

			final Group wholeGroup = new Group(comp, SWT.SHADOW_ETCHED_IN | SWT.BORDER);
			wholeGroup.setText("Capture Destination");
			final FormData formData1 = new FormData();
			formData1.left = new FormAttachment(0);
			formData1.top = new FormAttachment(5);
			wholeGroup.setLayoutData(formData1);
			final FormLayout groupLayout = new FormLayout();
			groupLayout.spacing = 5;
			groupLayout.marginHeight = 5;
			groupLayout.marginWidth = 5;
			wholeGroup.setLayout(groupLayout);

			final Composite capTypeGroup = new Composite(wholeGroup, SWT.NONE);
			capTypeGroup.setLayout(new RowLayout());

			fileCaptureButton = new Button(capTypeGroup, SWT.RADIO);
			fileCaptureButton.setText("Capture to File");

			dirCaptureButton = new Button(capTypeGroup, SWT.RADIO);
			dirCaptureButton.setText("Capture to Directory");

			outputLabel = new Label(wholeGroup, SWT.LEFT);
			outputLabel.setText("Destination: ");
			final FormData formData3 = new FormData();
			formData3.left = new FormAttachment(0);
			formData3.top = new FormAttachment(capTypeGroup, 15);

			outputText = new Text(wholeGroup, SWT.SINGLE | SWT.BORDER);

			browseButton = new Button(wholeGroup, SWT.PUSH);
			browseButton.setText("Browse...");

			final FormData formData4 = SWTUtilities.getFormData(outputText, 1, 50);
			formData4.left = new FormAttachment(outputLabel);
			formData4.top = new FormAttachment(capTypeGroup);
			formData4.right = new FormAttachment(browseButton);
			outputText.setLayoutData(formData4);

			formData3.top = new FormAttachment(outputText, 0, SWT.CENTER);
			outputLabel.setLayoutData(formData3);

			final FormData formData5 = new FormData();
			formData5.right = new FormAttachment(100);
			formData5.top = new FormAttachment(outputText, 0, SWT.CENTER);
			browseButton.setLayoutData(formData5);

			dirCaptureButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
					enableCaptureControls();
				}
			});

			fileCaptureButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
					enableCaptureControls();
				}
			});

			browseButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
					if (swtUtil == null) {
						swtUtil = new SWTUtilities();
					}
					String filename = null;
					if (dirCaptureButton.getSelection()) {
						filename = swtUtil.displayStickyDirSaver(prefShell, "CapturePreferencesShell", null);
					} else {
						filename = swtUtil.displayStickyFileSaver(prefShell, "CapturePreferencesShell", null, null);
					}
					if (filename != null) {
						outputText.setText(filename);
					}
				}
			});

			if (mode.equals(MessageCaptureHandler.CaptureType.WRITE_NONE)) {
				outputText.setEditable(false);
				dirCaptureButton.setSelection(false);
				fileCaptureButton.setSelection(false);
				outputLabel.setEnabled(false);
				browseButton.setEnabled(false);
			} else {
				outputText.setText(captureHandler.getMessageOutput().getAbsolutePath());
				dirCaptureButton.setSelection(mode.equals(CaptureType.WRITE_DIR));
				fileCaptureButton.setSelection(mode.equals(CaptureType.WRITE_FILE));
				outputLabel.setEnabled(true);
				browseButton.setEnabled(true);
			}
		}  

		/**
		 * Applies chosen settings from the file capture destination window 
		 * and shows a message indicating that file capture has started
		 * @return true if file capture will start successfully, false otherwise
		 */
		public boolean applyChanges() {
			if (!dirCaptureButton.getSelection() && !fileCaptureButton.getSelection()) {
				SWTUtilities.showErrorDialog(prefShell, "Error", "You must select a capture destination type.");
				return false;
			}
			final String file = outputText.getText().trim();
			if (file.equals("")) {
				SWTUtilities.showErrorDialog(prefShell, "Error", "You must enter a file or directory name.");
				return false;
			}
			if (dirCaptureButton.getSelection()) {
				if (!captureHandler.setOutputDir(file)) {
					SWTUtilities.showErrorDialog(prefShell, "Directory Error", 
					"The capture directory does not exist or is not writeable.");
					return false;
				}
			} else if (fileCaptureButton.getSelection()) {
				if (!captureHandler.setOutputFile(file)) {
					SWTUtilities.showErrorDialog(prefShell, "File Error", "The capture file is not writeable.");
					return false;
				}
			}
			SWTUtilities.showMessageDialog(prefShell, "Capture Started", "Capture Started to " + file); 
			return true;
		}
	}
}