/*
 * Copyright 2006-2019. California Institute of Technology.
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
package jpl.gds.tcapp.app.gui;

import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.tc.api.ICommandFileLoad;
import jpl.gds.tc.api.ICommandObjectFactory;
import jpl.gds.tc.api.IFileLoadInfo;
import jpl.gds.tc.api.IScmf;
import jpl.gds.tc.api.ISendCompositeState;
import jpl.gds.tc.api.command.parser.UplinkInputParser;
import jpl.gds.tc.api.config.CommandProperties;
import jpl.gds.tc.api.config.FileLoadParseException;
import jpl.gds.tcapp.app.SendFileApp;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This class configures and displays the send file load GUI tab in the uplink
 * application
 * 
 */
public class SendFileLoadComposite extends AbstractSendComposite {
	/** title of the tab */
	public static final String TITLE = "Send File Load";

	// Do NOT initialize this group!
	/** Name of the file on the local system */
	protected Text localFileText;
	/**
	 * button for displaying the system browser used to find the file to be sent
	 */
	protected Button browseButton;
	/** Name to be given to the file on the spacecraft */
	protected Text targetFileText;
	/** drop-down combo box for the type of file being sent */
	protected Combo fileTypeCombo;
	/**
	 * button for designating if the file can forcibly overwrite a file with the
	 * same name on the spacecraft
	 */
	protected Button overwriteButton;

	private FileLoadEventHandler handler = null;
	private final List<IFileLoadInfo> history = new ArrayList<IFileLoadInfo>();

	private final CommandProperties commandProperties;

	private final ICommandObjectFactory commandObjectFactory;

	/**
	 * Constructor
	 * 
	 * @param appContext
	 *            the ApplicationContext used by this uplink application
	 * @param parent
	 *            the parent composite window
	 */
	public SendFileLoadComposite(final ApplicationContext appContext, final Composite parent,
								 final CommandProperties commandProperties, final ICommandObjectFactory commandObjectFactory) {
		super(appContext, parent, false);
		this.commandProperties = commandProperties;
		this.commandObjectFactory = commandObjectFactory;
	}

	@Override
	public Point getSize() {
		return (new Point(625, 320));
	}

	@Override
	protected EventHandler getEventHandler() {
		if (handler == null) {
			handler = new FileLoadEventHandler();
		}

		return (handler);
	}

	/**
	 * Creates the GUI controls.
	 */
	@Override
	protected Control createBodyControls() {
		localFileText = new Text(this, SWT.SINGLE | SWT.BORDER);
		final FormData fd15 = SWTUtilities.getFormData(localFileText, 1, 15);
		fd15.left = new FormAttachment(25);
		fd15.right = new FormAttachment(80);
		fd15.top = new FormAttachment(0, 5);
		localFileText.setLayoutData(fd15);
		localFileText.addKeyListener(getEventHandler());

		final Label localFileLabel = new Label(this, SWT.LEFT);
		localFileLabel.setText("Local File:");
		final FormData fd16 = new FormData();
		fd16.left = new FormAttachment(0);
		fd16.right = new FormAttachment(localFileText, 10);
		fd16.top = new FormAttachment(localFileText, 0, SWT.CENTER);
		localFileLabel.setLayoutData(fd16);

		browseButton = new Button(this, SWT.PUSH);
		browseButton.setText("Browse...");
		final FormData bfd = new FormData();
		bfd.left = new FormAttachment(localFileText, 10);
		bfd.top = new FormAttachment(localFileText, 0, SWT.CENTER);
		bfd.right = new FormAttachment(100);
		browseButton.setLayoutData(bfd);
		browseButton.addSelectionListener(getEventHandler());

		targetFileText = new Text(this, SWT.SINGLE | SWT.BORDER);
		final FormData tftFormData = SWTUtilities.getFormData(targetFileText, 1, 15);
		tftFormData.left = new FormAttachment(25);
		tftFormData.right = new FormAttachment(80);
		tftFormData.top = new FormAttachment(localFileText, 0, SWT.LEFT);
		targetFileText.setLayoutData(tftFormData);
		targetFileText.addKeyListener(getEventHandler());

		final Label targetFileLabel = new Label(this, SWT.LEFT);
		targetFileLabel.setText("Target File (on flight system):");
		final FormData tflFormData = new FormData();
		tflFormData.left = new FormAttachment(0);
		tflFormData.right = new FormAttachment(localFileText, 10);
		tflFormData.top = new FormAttachment(targetFileText, 0, SWT.CENTER);
		targetFileLabel.setLayoutData(tflFormData);

		overwriteButton = new Button(this, SWT.CHECK);
		final FormData buttonFormData = new FormData();
		buttonFormData.left = new FormAttachment(25);
		buttonFormData.top = new FormAttachment(targetFileText, 0, SWT.LEFT);
		overwriteButton.setLayoutData(buttonFormData);
		overwriteButton.addSelectionListener(getEventHandler());

		final Label overwriteLabel = new Label(this, SWT.LEFT);
		overwriteLabel.setText("Overwrite target file (if exists)?:");
		final FormData labelFormData = new FormData();
		labelFormData.top = new FormAttachment(overwriteButton, 0, SWT.CENTER);
		labelFormData.right = new FormAttachment(overwriteButton);
		labelFormData.left = new FormAttachment(0);
		overwriteLabel.setLayoutData(labelFormData);

		fileTypeCombo = new Combo(this, SWT.DROP_DOWN);
		/*
		 * This bean call is necessary - this function is called by the super class (AbstractSendComposite) call in the constructor,
		 * which is BEFORE commandProperties is initialized.
		 */
		final String[] types = appContext.getBean(CommandProperties.class).getFileLoadTypes();
		if (types != null) {
			for (final String type : types) {
				fileTypeCombo.add(type);
			}
		} else {
			for (int i = 0; i < (int) Math.pow(2, ICommandFileLoad.FILE_TYPE_BIT_LENGTH); i++) {
				fileTypeCombo.add(Integer.toString(i));
			}
		}
		fileTypeCombo.select(0);
		final FormData ftcFormData = new FormData();
		ftcFormData.left = new FormAttachment(25);
		ftcFormData.right = new FormAttachment(65);
		ftcFormData.top = new FormAttachment(overwriteButton, 0, SWT.LEFT);
		fileTypeCombo.setLayoutData(ftcFormData);

		final Label fileTypeLabel = new Label(this, SWT.LEFT);
		fileTypeLabel.setText("File Type:");
		final FormData ftlFormData = new FormData();
		ftlFormData.top = new FormAttachment(fileTypeCombo, 0, SWT.CENTER);
		ftlFormData.left = new FormAttachment(0);
		ftlFormData.right = new FormAttachment(fileTypeCombo);
		fileTypeLabel.setLayoutData(ftlFormData);

		return (overwriteLabel);
	}

	/**
	 * Utility function used to clear all file properties
	 * 
	 * @param localFile
	 *            String name of the local file
	 * @param targetFile
	 *            String name for the file when it is received by the spacecraft
	 * @param fileType
	 *            The type of file being sent
	 * @param overwrite
	 *            If the file being sent is allowed to overwrite a file with the
	 *            same name on the spacecraft
	 */
	public void setDefaultState(final String localFile, final String targetFile, final String fileType,
			final boolean overwrite) {
		localFileText.setText(localFile);
		targetFileText.setText(targetFile);

		if (fileType == null) {
			fileTypeCombo.select(0);
		} else {
			fileTypeCombo.setText(fileType);
		}
		overwriteButton.setSelection(overwrite);
	}

	/**
	 * Get the title of this composite frame
	 *
	 * @return the String name of this composite
	 *
	 * @see jpl.gds.shared.swt.ChillShell#getTitle()
	 */
	public String getTitle() {
		return TITLE;
	}

	@Override
	protected void send() {
	    
		final String localFile = localFileText.getText().trim();
		if (localFile.length() == 0) {
			throw new IllegalArgumentException("Local File field cannot be empty.");
		}

		final String targetFile = targetFileText.getText().trim();
		if (targetFile.length() == 0) {
			throw new IllegalArgumentException("Target File field cannot be empty.");
		}

		final String fileType = fileTypeCombo.getText().trim().toUpperCase();
		final boolean overwrite = overwriteButton.getSelection();

		final File fileLoad = new File(localFile);

		final long sizeLimit = IScmf.FILE_SIZE_MAX_VALUE;

		if (fileLoad.length() > sizeLimit) {
			final String errorMessage = "File size exeeds the limit of " + sizeLimit + " bytes.";
			logger.error(errorMessage);

			SWTUtilities.safeAsyncExec(getDisplay(), TITLE,
					() -> SWTUtilities.showErrorDialog(getShell(), "Send File Load Error", errorMessage));
			return;
		}

        final IMessage msg = this.messageFactory.createClearUplinkGuiLogMessage();
        this.bus.publish(msg);
        logger.debug(msg);

		try {
			final IFileLoadInfo fileLoadInfo = this.commandObjectFactory.createFileLoadInfo(fileType,
					localFile, targetFile, overwrite);
			sendCommandFile(fileLoadInfo, msg);
		} catch (final FileLoadParseException e) {
			final String message = e.getMessage() == null ? e.toString() : e.getMessage();

			final String errorMessage = "Send file load error: " + message;
			logger.error(errorMessage);

			SWTUtilities.safeAsyncExec(getDisplay(), TITLE,
					() -> SWTUtilities.showErrorDialog(getShell(), "Send File Load Error", errorMessage));
		}
	}

	private void sendCommandFile(final IFileLoadInfo fileLoadInfo, IMessage msg) {

		msg = this.messageFactory.createUplinkGuiLogMessage("Executing send file load: " + fileLoadInfo.getInputFilePath()
				+ "\n\n");
		this.bus.publish(msg);

		this.showProgressIndicatorDialog();

		UplinkExecutors.uplinkExecutor.execute(() -> {
			final TransmitEvent event = new TransmitEvent();
			final int transmitEventId = event.hashCode();

			List<ICommandFileLoad> fileLoads = new ArrayList<ICommandFileLoad>();
			try {
				fileLoads = UplinkInputParser.createFileLoadsFromInfo(appContext, fileLoadInfo);
				SendFileApp.sendFileLoads(appContext, fileLoads, transmitEventId);

				event.setTransmitInfo(SendFileLoadComposite.this, fileLoadInfo, true,
						"Successfully transmitted file load");
				notifyListeners(event);

				if (!SendFileLoadComposite.this.isDisposed()) {
					SWTUtilities.safeAsyncExec(getDisplay(), TITLE,
							() -> setDefaultState("", "", null, false)
					);
				}
			} catch (final Exception e) {
				final String message = e.getMessage() == null ? e.toString() : e.getMessage();
				this.bus.publish(this.messageFactory.createUplinkGuiLogMessage(
						"Exception encountered while sending file load: " + message + "\n\n"));
				event.setTransmitInfo(SendFileLoadComposite.this, fileLoadInfo, false,
						e.getMessage());
				notifyListeners(event);

				final String errorMessage = "Send file load error: " + message;
				logger.error(errorMessage);

				if (!SendFileLoadComposite.this.isDisposed()) {
					SWTUtilities.safeAsyncExec(getDisplay(), TITLE,
							() -> SWTUtilities.showErrorDialog(getShell(), "Send File Load Error", message));
				}
			} finally {
				SendFileLoadComposite.this.closeProgressIndicatorDialog();
			}
		});

	}

	/**
	 * Reload a previously used FileLoad info from the history via index
	 * 
	 * @param selected
	 *            the integer index corresponding to the desired FileLoadInfo
	 */
	private void setFileLoadInfoFromSelection(final int selected) {
		final IFileLoadInfo info = history.get(selected);
		localFileText.setText(info.getInputFilePath());
		targetFileText.setText(info.getTargetFilePath());
		fileTypeCombo.setText(info.getFileTypeString());
		overwriteButton.setSelection(info.isOverwrite());
	}

	public class FileLoadEventHandler extends EventHandler {
		protected FileLoadEventHandler() {
			super();
		}

		@Override
		public void widgetSelected(final SelectionEvent se) {
			super.widgetSelected(se);

			if (se.getSource() == browseButton) {
				final String filename = util.displayStickyFileChooser(false, sendShell, "SendShell");

				if (filename != null) {
					localFileText.setText(filename);
				}
			}
			// else if(se.getSource() == historyList)
			// {
			// int selected = historyList.getSelectionIndex();
			// if(selected >= 0 && selected < historyList.getItemCount())
			// {
			// setFileLoadInfoFromSelection(selected);
			// }
			// }
		}

		@Override
		public void keyPressed(final KeyEvent arg0) {
			// if(arg0.getSource() == localFileText ||
			// arg0.getSource() == targetFileText)
			// {
			// if(arg0.keyCode == SWT.ARROW_UP)
			// {
			// int selected = historyList.getSelectionIndex();
			// if (selected == -1)
			// {
			// historyList.setSelection(historyList.getItemCount() - 1);
			// selected = historyList.getItemCount();
			// }
			//
			// int newSelected = selected - 1;
			// if(newSelected >= 0 && newSelected < historyList.getItemCount())
			// {
			// historyList.setSelection(newSelected);
			// setFileLoadInfoFromSelection(newSelected);
			// }
			// }
			// else if(arg0.keyCode == SWT.ARROW_DOWN)
			// {
			// int selected = historyList.getSelectionIndex();
			// int newSelected = selected + 1;
			// if(newSelected >= 0 && newSelected < historyList.getItemCount())
			// {
			// historyList.setSelection(newSelected);
			// setFileLoadInfoFromSelection(newSelected);
			// }
			// }
			// }
		}

		public void keyTraversed(final TraverseEvent arg0) {
			if (arg0.getSource() == localFileText || arg0.getSource() == targetFileText) {
				if (arg0.detail == SWT.TRAVERSE_RETURN) {
					send();
				}
			}
		}
	}

	@Override
	public String getDisplayName() {
		return TITLE;
	}

	@Override
	public void setFieldsFromTransmitHistory(final TransmitEvent historyItem) {
		final ISendCompositeState stateObj = historyItem.getTransmitState();

		if (stateObj instanceof IFileLoadInfo) {
			final IFileLoadInfo info = (IFileLoadInfo) stateObj;
			localFileText.setText(info.getInputFilePath());
			targetFileText.setText(info.getTargetFilePath());
			fileTypeCombo.setText(info.getFileTypeString());
			overwriteButton.setSelection(info.isOverwrite());
		}
	}

	@Override
	public boolean needSendButton() {
		return true;
	}
}
