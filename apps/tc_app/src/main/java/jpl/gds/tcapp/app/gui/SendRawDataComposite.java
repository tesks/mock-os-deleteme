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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.springframework.context.ApplicationContext;

import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.tc.api.IScmf;
import jpl.gds.tc.api.ISendCompositeState;
import jpl.gds.tcapp.app.SendRawUplinkDataApp;

public class SendRawDataComposite extends AbstractSendComposite {
	public static final String TITLE = "Send Raw Data File";

	// Do not set this group to a default
	protected Text rawDataFileText;
	protected Button browseButton;
	protected Button isHexButton;

	protected RawDataEventHandler handler = null;
	private final List<RawDataInfo> history;

	public SendRawDataComposite(final ApplicationContext appContext, final Composite parent) {
		super(appContext, parent, false);

		history = new ArrayList<RawDataInfo>();
	}

	@Override
	public Point getSize() {
		return (new Point(625, 250));
	}

	@Override
	protected EventHandler getEventHandler() {
		if (handler == null) {
			handler = new RawDataEventHandler();
		}

		return handler;
	}

	/**
	 * Creates the GUI controls.
	 */
	@Override
	protected Control createBodyControls() {
		rawDataFileText = new Text(this, SWT.SINGLE | SWT.BORDER);
		final FormData fd15 = SWTUtilities.getFormData(rawDataFileText, 1, 15);
		fd15.left = new FormAttachment(25);
		fd15.right = new FormAttachment(80);
		fd15.top = new FormAttachment(0, 5);
		rawDataFileText.setLayoutData(fd15);

		final Label scmfNameLabel = new Label(this, SWT.LEFT);
		scmfNameLabel.setText("Raw Data File:");
		final FormData fd16 = new FormData();
		fd16.left = new FormAttachment(0);
		fd16.right = new FormAttachment(rawDataFileText, 10);
		fd16.top = new FormAttachment(rawDataFileText, 0, SWT.CENTER);
		scmfNameLabel.setLayoutData(fd16);

		browseButton = new Button(this, SWT.PUSH);
		browseButton.setText("Browse...");
		final FormData bfd = new FormData();
		bfd.left = new FormAttachment(rawDataFileText, 10);
		bfd.top = new FormAttachment(rawDataFileText, 0, SWT.CENTER);
		bfd.right = new FormAttachment(100);
		browseButton.setLayoutData(bfd);
		browseButton.addSelectionListener(getEventHandler());

		isHexButton = new Button(this, SWT.CHECK);
		final FormData buttonFormData = new FormData();
		buttonFormData.left = new FormAttachment(25);
		buttonFormData.top = new FormAttachment(rawDataFileText, 0, SWT.LEFT);
		isHexButton.setLayoutData(buttonFormData);
		isHexButton.addSelectionListener(getEventHandler());

		final Label isHexLabel = new Label(this, SWT.LEFT);
		isHexLabel.setText("Hex File:");
		final FormData labelFormData = new FormData();
		labelFormData.top = new FormAttachment(isHexButton, 0, SWT.CENTER);
		labelFormData.right = new FormAttachment(isHexButton);
		labelFormData.left = new FormAttachment(0);
		isHexLabel.setLayoutData(labelFormData);

		return (isHexLabel);
	}

	public void setDefaultState(final String rawDataFile, final boolean isHex) {
		rawDataFileText.setText(rawDataFile);
		isHexButton.setSelection(isHex);
	}

	public String getTitle() {
		return TITLE;
	}

	@Override
	protected void send() {
		final String rawDataFileName = rawDataFileText.getText().trim();
		final boolean isHexFile = isHexButton.getSelection();

		if (rawDataFileName.equals("")) {
			throw new IllegalArgumentException(
					"The raw data file path cannot be empty.");
		}

		final File rawFile = new File(rawDataFileName);
		final long length = rawFile.length();

		final long sizeLimit = IScmf.FILE_SIZE_MAX_VALUE;
		final RawDataInfo rawDataInfo = new RawDataInfo(isHexFile,
				rawDataFileName);

		if (length > sizeLimit) {
			final String errorMessage = "File size exeeds the limit of "
					+ sizeLimit + " bytes.";
			logger.error(errorMessage);

			if (!SendRawDataComposite.this.isDisposed()) {
				SWTUtilities.safeAsyncExec(getDisplay(), "Send Raw Data",
						new Runnable() {
							@Override
							public void run() {
								SWTUtilities.showErrorDialog(getShell(),
										"Send Raw Data Error", errorMessage);
							}

						});
			}

			return;
		}

        bus.publish(messageFactory.createClearUplinkGuiLogMessage());
        bus.publish(messageFactory.createUplinkGuiLogMessage("Executing send raw data file: "
				+ rawDataFileName + "\n\n"));

		this.showProgressIndicatorDialog();

		UplinkExecutors.uplinkExecutor.execute(new Runnable() {

			@Override
			public void run() {
				final TransmitEvent event = new TransmitEvent();
				final int transmitEventId = event.hashCode();

				try {

					SendRawUplinkDataApp.sendRawUplinkData(appContext, rawDataFileName,
							isHexFile, transmitEventId);

					event.setTransmitInfo(SendRawDataComposite.this,
							rawDataInfo, true,
							"Successfully transmitted raw data file.");

					notifyListeners(event);

					if (!SendRawDataComposite.this.isDisposed()) {
						SWTUtilities.safeAsyncExec(getDisplay(),
								"Send Raw Data", new Runnable() {
									@Override
									public void run() {
										rawDataFileText.setText("");
										isHexButton.setSelection(false);
									}
								});
					}

				} catch (final Exception e) {
					final String message = e.getMessage() == null ? e
							.toString() : e.getMessage();
					bus.publish(messageFactory.createUplinkGuiLogMessage("Exception encountered while sending raw data file: "
									+ message + "\n\n"));
					event.setTransmitInfo(SendRawDataComposite.this,
							rawDataInfo, false, e.getMessage());

					notifyListeners(event);

					final String errorMessage = "Send Raw Data Error: " + message;
					logger.error(errorMessage);

					if (!SendRawDataComposite.this.isDisposed()) {
						SWTUtilities.safeAsyncExec(getDisplay(),
								"Send Raw Data", new Runnable() {
									@Override
									public void run() {
										SWTUtilities.showErrorDialog(
												getShell(),
												"Send Raw Data Error", message);
									}

								});
					}

				} finally {
					SendRawDataComposite.this.closeProgressIndicatorDialog();
				}
			}
		});
	}

	protected static class RawDataInfo implements ISendCompositeState {
		private String rawDataFile;
		private boolean isHex;

		public RawDataInfo(final boolean isHex, final String rawDataFile) {
			super();
			this.isHex = isHex;
			this.rawDataFile = rawDataFile;
		}

		public String getRawDataFile() {
			return rawDataFile;
		}

		public void setRawDataFile(final String rawDataFile) {
			this.rawDataFile = rawDataFile;
		}

		public boolean isHex() {
			return isHex;
		}

		public void setHex(final boolean isHex) {
			this.isHex = isHex;
		}

		@Override
		public String getTransmitSummary() {
			return rawDataFile;
		}
	}

	private void setRawDataInfoFromSelection(final int selected) {
		final RawDataInfo info = history.get(selected);
		rawDataFileText.setText(info.getRawDataFile());
		isHexButton.setSelection(info.isHex());
	}

	public class RawDataEventHandler extends EventHandler {
		protected RawDataEventHandler() {
			super();
		}

		@Override
		public void widgetSelected(final SelectionEvent se) {
			super.widgetSelected(se);

			if (se.getSource() == browseButton) {
				final String filename = util.displayStickyFileChooser(false,
						sendShell, "SendShell");
				if (filename != null) {
					rawDataFileText.setText(filename);
				}
			}
			// } else if (se.getSource() == historyList) {
			// int selected = historyList.getSelectionIndex();
			// if (selected >= 0 && selected < historyList.getItemCount()) {
			// setRawDataInfoFromSelection(selected);
			// }
			// }
		}

		@Override
        public void keyPressed(final KeyEvent arg0) {
			// if (arg0.getSource() == rawDataFileText) {
			// if (arg0.keyCode == SWT.ARROW_UP) {
			// int selected = historyList.getSelectionIndex();
			// if (selected == -1) {
			// historyList.setSelection(historyList.getItemCount() - 1);
			// selected = historyList.getItemCount();
			// }
			//
			// int newSelected = selected - 1;
			// if (newSelected >= 0
			// && newSelected < historyList.getItemCount()) {
			// historyList.setSelection(newSelected);
			// setRawDataInfoFromSelection(newSelected);
			// }
			// } else if (arg0.keyCode == SWT.ARROW_DOWN) {
			// int selected = historyList.getSelectionIndex();
			// int newSelected = selected + 1;
			// if (newSelected >= 0
			// && newSelected < historyList.getItemCount()) {
			// historyList.setSelection(newSelected);
			// setRawDataInfoFromSelection(newSelected);
			// }
			// }
			// }
		}

		public void keyTraversed(final TraverseEvent arg0) {
			if (arg0.getSource() == rawDataFileText) {
				if (arg0.detail == SWT.TRAVERSE_RETURN) {
					send();
				}
			}
		}
	}

	@Override
	public String getDisplayName() {
		return "Send Raw Data File";
	}

	@Override
	public void setFieldsFromTransmitHistory(final TransmitEvent historyItem) {
		final ISendCompositeState stateObj = historyItem.getTransmitState();

		if (stateObj instanceof RawDataInfo) {
			final RawDataInfo info = (RawDataInfo) stateObj;
			rawDataFileText.setText(info.getRawDataFile());
			isHexButton.setSelection(info.isHex());
		}
	}

	@Override
	public boolean needSendButton() {
		return true;
	}
}
