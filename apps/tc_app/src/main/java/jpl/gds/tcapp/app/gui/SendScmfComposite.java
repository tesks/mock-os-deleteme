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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;

import jpl.gds.tc.api.*;
import jpl.gds.tc.api.message.IScmfCommandMessage;
import jpl.gds.tc.api.scmf.IScmfSerializer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.springframework.context.ApplicationContext;

import jpl.gds.shared.checksum.RotatedXorAlgorithm;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.time.TimeUtility;
import jpl.gds.tc.api.config.ScmfProperties;
import jpl.gds.tc.api.exception.ScmfVersionMismatchException;
import jpl.gds.tcapp.app.SendScmfApp;

/**
 * SendScmfComposite is a composite that allows the user to select an SCMF for 8
 * transmission.
 * 
 *
 */
public class SendScmfComposite extends AbstractSendComposite {
	private final Tracer logger;

	public static final String DISABLE_CHECKS_OPTION_NAME = "Disable Validity Checks";
	public static final String TITLE = "Send SCMF";

	// Do not default this group to null
	protected Text scmfFileText;
	protected Button browseButton;
	protected Button disableChecksButton;
	protected Group validationInfoGroup;
	protected Label scmfHeaderLabel;
	protected Label fileMetadataLabel;
	protected Label fileChecksumField;
	protected Label calculatedChecksumField;
	protected Label calculatedChecksumLabel;
	protected Label fileChecksumLabel;

	protected ScmfEventHandler handler = null;
	private final IScmfSerializer serializer;

	public SendScmfComposite(final ApplicationContext appContext, final Composite parent) {
		super(appContext, parent, false);
        logger = TraceManager.getDefaultTracer(appContext);
        this.serializer = appContext.getBean(IScmfSerializer.class);
	}

	@Override
	public Point getSize() {
		return (new Point(UplinkShell.DEFAULT_WIDTH - 50, 250));
	}

	@Override
	protected EventHandler getEventHandler() {
		if (handler == null) {
			handler = new ScmfEventHandler();
		}

		return handler;
	}

	/**
	 * Creates the GUI controls.
	 */
	@Override
	protected Control createBodyControls() {
		scmfFileText = new Text(this, SWT.SINGLE | SWT.BORDER);
		final FormData fd15 = SWTUtilities.getFormData(scmfFileText, 1, 15);
		fd15.left = new FormAttachment(25);
		fd15.right = new FormAttachment(80);
		fd15.top = new FormAttachment(0, 5);
		scmfFileText.setLayoutData(fd15);
		scmfFileText.addKeyListener(getEventHandler());

		final Label scmfNameLabel = new Label(this, SWT.LEFT);
		scmfNameLabel.setText("SCMF Output File:");
		final FormData fd16 = new FormData();
		fd16.left = new FormAttachment(0);
		fd16.right = new FormAttachment(scmfFileText, 10);
		fd16.top = new FormAttachment(scmfFileText, 0, SWT.CENTER);
		scmfNameLabel.setLayoutData(fd16);

		browseButton = new Button(this, SWT.PUSH);
		browseButton.setText("Browse...");
		final FormData bfd = new FormData();
		bfd.left = new FormAttachment(scmfFileText, 10);
		bfd.top = new FormAttachment(scmfFileText, 0, SWT.CENTER);
		bfd.right = new FormAttachment(100);
		browseButton.setLayoutData(bfd);
		browseButton.addSelectionListener(getEventHandler());

		disableChecksButton = new Button(this, SWT.CHECK);
		final FormData buttonFormData = new FormData();
		buttonFormData.left = new FormAttachment(25);
		buttonFormData.top = new FormAttachment(scmfFileText, 0, SWT.LEFT);
		disableChecksButton.setLayoutData(buttonFormData);
		disableChecksButton.addSelectionListener(getEventHandler());

		final Label checksumLabel = new Label(this, SWT.LEFT);
		checksumLabel.setText(DISABLE_CHECKS_OPTION_NAME + ": ");
		final FormData labelFormData = new FormData();
		labelFormData.top = new FormAttachment(disableChecksButton, 0,
				SWT.CENTER);
		labelFormData.right = new FormAttachment(disableChecksButton);
		labelFormData.left = new FormAttachment(0);
		checksumLabel.setLayoutData(labelFormData);

		final FormLayout validationLayout = new FormLayout();
		validationLayout.marginWidth = 10;
		validationLayout.marginHeight = 10;
		validationLayout.spacing = 10;

		validationInfoGroup = new Group(this, SWT.SHADOW_NONE);
		validationInfoGroup.setText("SCMF Metadata");
		validationInfoGroup.setLayout(validationLayout);

		final FormData valFormData = new FormData();
		valFormData.left = new FormAttachment(0);
		valFormData.right = new FormAttachment(100);
		valFormData.top = new FormAttachment(disableChecksButton, 0);
		validationInfoGroup.setLayoutData(valFormData);

		scmfHeaderLabel = new Label(validationInfoGroup, SWT.LEFT);
		FormData fd = new FormData();
		scmfHeaderLabel.setLayoutData(fd);

		fileMetadataLabel = new Label(validationInfoGroup, SWT.LEFT);
		fd = new FormData();
		fd.left = new FormAttachment(scmfHeaderLabel, 0, SWT.RIGHT);
		fileMetadataLabel.setLayoutData(fd);

		fileChecksumLabel = new Label(validationInfoGroup, SWT.LEFT);
		fileChecksumLabel.setText("File Checksum:");
		fd = new FormData();
		fd.top = new FormAttachment(scmfHeaderLabel, 0);
		fileChecksumLabel.setLayoutData(fd);
		fileChecksumLabel.setVisible(false);

		fileChecksumField = new Label(validationInfoGroup, SWT.SINGLE);
		fd = new FormData();
		fd.left = new FormAttachment(fileChecksumLabel, 0);
		fd.top = new FormAttachment(fileChecksumLabel, 0, SWT.CENTER);
		fileChecksumField.setLayoutData(fd);

		calculatedChecksumLabel = new Label(validationInfoGroup, SWT.LEFT);
		calculatedChecksumLabel.setText("Calculated Checksum:");
		fd = new FormData();
		fd.top = new FormAttachment(fileChecksumLabel, 0);
		calculatedChecksumLabel.setLayoutData(fd);
		calculatedChecksumLabel.setVisible(false);

		calculatedChecksumField = new Label(validationInfoGroup, SWT.SINGLE);
		fd = new FormData();
		fd.left = new FormAttachment(calculatedChecksumLabel, 0);
		fd.top = new FormAttachment(calculatedChecksumLabel, 0, SWT.CENTER);
		calculatedChecksumField.setLayoutData(fd);

		validationInfoGroup.pack(true);

		return (validationInfoGroup);
	}

	public void setDefaultState(final String scmfFile,
			final boolean disableChecks) {
		scmfFileText.setText(scmfFile);
		disableChecksButton.setSelection(disableChecks);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jpl.gds.swt.common.ChillShell#getTitle()
	 */
	public String getTitle() {
		return TITLE;
	}

	@Override
	protected void send() {
		final boolean disableChecks = disableChecksButton.getSelection();
		final String scmfName = scmfFileText.getText().trim();

		if (scmfName.equals("")) {
			throw new IllegalArgumentException(
					"SCMF File field cannot be empty");
		}

		final ScmfInfo scmfInfo = new ScmfInfo(disableChecks, scmfName);

		bus.publish(messageFactory.createClearUplinkGuiLogMessage());
		bus.publish(messageFactory.createUplinkGuiLogMessage("Executing send SCMF file: " + scmfName
				+ "\n\n"));

		final File scmfFile = new File(scmfName);
		final long length = scmfFile.length();

		if (length > IScmf.FILE_SIZE_MAX_VALUE) {
			final String errorMessage = "File size exeeds the SCMF size limit of "
					+ IScmf.FILE_SIZE_MAX_VALUE + " bytes.";

			logger.error(errorMessage);

			if (!SendScmfComposite.this.isDisposed()) {
				SWTUtilities.safeAsyncExec(getDisplay(), "Send SCMF",
						new Runnable() {
							@Override
							public void run() {
								SWTUtilities.showErrorDialog(getShell(),
										"Send SCMF Error", errorMessage);
							}

						});
			}

			return;
		}

		this.showProgressIndicatorDialog();

		UplinkExecutors.uplinkExecutor.execute(new Runnable() {
			@Override
			public void run() {
				final TransmitEvent event = new TransmitEvent();
				final int transmitEventId = event.hashCode();
				String message = "";
				boolean success = true;
				try {
					SendScmfApp.sendScmf(appContext, scmfName, disableChecks,
							transmitEventId);

					event.setTransmitInfo(SendScmfComposite.this, scmfInfo,
							success, "Successfully transmitted SCMF.");
					notifyListeners(event);
				} catch (final ScmfVersionMismatchException svme) {
					message = svme.getMessage();
					success = false;
					bus.publish(messageFactory.createUplinkGuiLogMessage("Exception encountered while sending SCMF: "
									+ message + "\n\n"));
					event.setTransmitInfo(SendScmfComposite.this, scmfInfo,
							success, "SCMF Version Mismatch: " + message);
					notifyListeners(event);

				} catch (final Exception e) {
					message = e.getMessage() == null ? e.toString() : e
							.getMessage();
					success = false;
					bus.publish(messageFactory.createUplinkGuiLogMessage("Exception encountered while sending SCMF: "
									+ message + "\n\n"));
					event.setTransmitInfo(SendScmfComposite.this, scmfInfo,
							success, e.getMessage());
					notifyListeners(event);
				} finally {
					SendScmfComposite.this.closeProgressIndicatorDialog();

					if (!success) {
						final String errorMessage = "Send SCMF error: "
								+ message;
						logger.error(errorMessage);

						final String finalMessage = message;

						if (!SendScmfComposite.this.isDisposed()) {
							SWTUtilities.safeAsyncExec(getDisplay(),
									"Send SCMF", new Runnable() {
										@Override
										public void run() {
											SWTUtilities.showErrorDialog(
													getShell(),
													"Send SCMF Error",
													finalMessage);
										}

									});
						}
					} else {
						if (!SendScmfComposite.this.isDisposed()) {
							SWTUtilities.safeAsyncExec(getDisplay(),
									"Send SCMF", new Runnable() {
										@Override
										public void run() {
											scmfFileText.setText("");
											disableChecksButton
													.setSelection(false);
											resetScmfMetadataFields();
										}

									});
						}
					}
				}
			}
		});
	}

	protected static class ScmfInfo implements ISendCompositeState {
		private String scmfFile;
		private boolean disableChecks;

		public ScmfInfo(final boolean disableChecks, final String scmfFile) {
			super();
			this.disableChecks = disableChecks;
			this.scmfFile = scmfFile;
		}

		public String getScmfFile() {
			return scmfFile;
		}

		public void setScmfFile(final String scmfFile) {
			this.scmfFile = scmfFile;
		}

		public boolean isDisableChecks() {
			return disableChecks;
		}

		public void setDisableChecks(final boolean disableChecks) {
			this.disableChecks = disableChecks;
		}

		@Override
		public String getTransmitSummary() {
			return scmfFile;
		}
	}

	public class ScmfEventHandler extends EventHandler {
		protected ScmfEventHandler() {
			super();
		}

		@Override
		public void widgetSelected(final SelectionEvent se) {
			super.widgetSelected(se);

			if (se.getSource() == browseButton) {
				final String filename = util.displayStickyFileChooser(false,
						sendShell, "SendShell", new String[] { "*.scmf", "*" });
				setSelectedScmf(filename);
			} else if (se.getSource() == disableChecksButton) {
				disableChecksButton.getSelection();
			}
		}

		public void keyTraversed(final TraverseEvent arg0) {
			if (arg0.getSource() == scmfFileText) {
				if (arg0.detail == SWT.TRAVERSE_RETURN) {
					send();
				}
			}
		}

		@Override
		public void keyPressed(final KeyEvent arg0) {

		}
	}

	private void setSelectedScmf(final String filename) {
		resetScmfMetadataFields();

		// Parse SCMF metadata and create SCMF Validation
		// Information
		if (filename != null) {
			scmfFileText.setText(filename);

			IScmf scmf;

			try {
				scmf = appContext.getBean(IScmfFactory.class).parse(filename);
			} catch (final Exception e) {
				// probably could not parse because of a validation issue
				// disable checks in Scmf class so we can parse and get whatever
				// information we can to display as metadata
				// we reset it after we are done
				appContext.getBean(ScmfProperties.class).setDisableChecksums(true);
				try {
					scmf = appContext.getBean(IScmfFactory.class).parse(filename);
				} catch (final Exception e1) {
					// at this point, it is probably not a validity error so
					// display error message and abort parse
					final String errorMsg = "Error parsing SCMF: " + e.getMessage();
					logger.error(errorMsg);
					final Display display = Display.getCurrent();

					this.scmfHeaderLabel.setText(errorMsg);
					this.scmfHeaderLabel.setForeground(display
							.getSystemColor(SWT.COLOR_RED));
					this.layout(true);
					return;
				}
			}

			final IScmfSfduHeader scmfHeader = scmf.getSfduHeader();

			final StringBuffer buffer = new StringBuffer(1024);

			buffer.append(scmfHeader.getFileName().trim());
			buffer.append(scmfHeader.getMissionName().toUpperCase().trim());
			buffer.append(scmfHeader.getSpacecraftId());
			buffer.append(scmfHeader.getProductCreationTime().trim());
			buffer.append(scmfHeader.getProductVersion().trim());

			final String scmfHeaderString = buffer.toString();

			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			try {

				baos.write(serializer.getHeaderBytes(scmf));

				for (IScmfCommandMessage msg : scmf.getCommandMessages()) {
					baos.write(serializer.getCommandMessageBytes(msg));
				}
			} catch (IOException e) {
				logger.warn("IOException encountered while trying to read SCMF. Calculated checksum may not be correct");
			}

			final byte[] spacecraftMessageFileBytes = baos.toByteArray();
			//TODO  - Replace with calculating a new one with MPS?
			final int ckSum = RotatedXorAlgorithm
					.calculate16BitChecksum(spacecraftMessageFileBytes);
			final int fileCkSum = scmf.getFileChecksum();

			this.scmfHeaderLabel.setText(scmfHeaderString.trim());
			this.scmfHeaderLabel.setForeground(null);

			// file metadata
			final File scmfFile = new File(filename);
			final StringBuilder sb = new StringBuilder();
			sb.append("PATH = ");
			sb.append(scmfFile.getAbsolutePath());
			sb.append("\n");
			sb.append("FILE = ");
			sb.append(scmfFile.getName());
			sb.append("\n");
			sb.append("FILE_MOD_TIME = ");

			final DateFormat df = TimeUtility.getFormatterFromPool();
			final String timestamp = df.format(new Date(scmfFile.lastModified()));
			TimeUtility.releaseFormatterToPool(df);
			sb.append(timestamp);

			this.fileMetadataLabel.setText(sb.toString());

			String checksum = Integer.toHexString(fileCkSum);
			while (checksum.length() < 4) {
				checksum = "0" + checksum;
			}
			checksum = "0x" + checksum;
			this.fileChecksumField.setText(checksum);

			String calcChecksum = Integer.toHexString(ckSum);
			while (calcChecksum.length() < 4) {
				calcChecksum = "0" + calcChecksum;
			}
			calcChecksum = "0x" + calcChecksum;
			this.calculatedChecksumField.setText(calcChecksum);

			final Display display = Display.getCurrent();

			if (fileCkSum == ckSum) {
				fileChecksumField.setBackground(display
						.getSystemColor(SWT.COLOR_GREEN));
				calculatedChecksumField.setBackground(display
						.getSystemColor(SWT.COLOR_GREEN));
			} else {
				fileChecksumField.setBackground(display
						.getSystemColor(SWT.COLOR_RED));
				calculatedChecksumField.setBackground(display
						.getSystemColor(SWT.COLOR_RED));
			}

			this.fileChecksumLabel.setVisible(true);
			this.calculatedChecksumLabel.setVisible(true);

			this.fileChecksumField.pack();
			this.calculatedChecksumField.pack();
			this.validationInfoGroup.pack();
			this.layout(true);

			// reset disable checks status
			appContext.getBean(ScmfProperties.class).setDisableChecksums(
					this.disableChecksButton.getSelection());
		}
	}

	private void resetScmfMetadataFields() {
		this.fileMetadataLabel.setText("");

		this.scmfHeaderLabel.setText("");
		this.scmfHeaderLabel.setForeground(null);

		this.fileChecksumField.setText("");
		this.fileChecksumField.setBackground(null);

		this.calculatedChecksumField.setText("");
		this.calculatedChecksumField.setBackground(null);

		this.fileChecksumLabel.setVisible(false);
		this.calculatedChecksumLabel.setVisible(false);
	}

	@Override
	public String getDisplayName() {
		return "Send SCMF";
	}

	@Override
	public void setFieldsFromTransmitHistory(final TransmitEvent historyItem) {
		final ISendCompositeState stateObj = historyItem.getTransmitState();

		if (stateObj instanceof ScmfInfo) {
			final ScmfInfo info = (ScmfInfo) stateObj;
			scmfFileText.setText(info.getTransmitSummary());
			disableChecksButton.setSelection(info.isDisableChecks());
			appContext.getBean(ScmfProperties.class).setDisableChecksums(
					disableChecksButton.getSelection());
		}
	}

	@Override
	public boolean needSendButton() {
		return true;
	}
}
