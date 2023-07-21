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
package jpl.gds.tcapp.app.gui.fault;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.springframework.context.ApplicationContext;

import jpl.gds.context.api.IGeneralContextInformation;
import jpl.gds.shared.file.FileUtility;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.util.BinOctHexUtility;
import jpl.gds.tc.api.cltu.ICltu;
import jpl.gds.tc.api.IScmfFactory;
import jpl.gds.tc.api.config.ScmfProperties;
import jpl.gds.tc.api.config.UplinkParseException;
import jpl.gds.tc.api.exception.RawOutputException;
import jpl.gds.tc.api.exception.UplinkException;
import jpl.gds.tc.api.message.ICommandMessageFactory;
import jpl.gds.tc.api.output.OutputFileNameFactory;
import jpl.gds.tcapp.app.SendRawUplinkDataApp;
import jpl.gds.tcapp.app.gui.AbstractUplinkComposite;
import jpl.gds.tcapp.app.gui.TransmitEvent;
import jpl.gds.tcapp.app.gui.UplinkExecutors;

/**
 * The last page in the fault injection flow. This gives the user the option of
 * saving their corrupted uplink as a raw data file, saving it as an scf, or
 * sending it to the flight system.
 * 
 *
 */
public final class UplinkOutputComposite extends AbstractUplinkComposite
		implements FaultInjectorGuiComponent {
	/** The font this composite will use */
	private static final String FONT_NAME = "Helvetica";

	/** The title of this page */
	private static final String FAULT_PAGE_TITLE = "Uplink Output";
	/** The description of what this page does */
	private static final String FAULT_PAGE_DESC = "Choose what to do with your corrupted uplink data.";

	/** The current fault injection state */
	private FaultInjectionState dataState = null;

	/** The label text for the save scmf button */
	private Label saveAsScmfLabel = null;
	/** The button to allow the user to save the output as an scmf */
	private Button saveAsScmfButton = null;
	/** The label text for the save raw data button */
	private Label saveAsRawFileLabel = null;
	/** The button to allow the user to save the output as a raw data file */
	private Button saveAsRawFileButton = null;
	/** The label text for the send button */
	private Label sendLabel = null;
	/** The button to allow the user to send the output to the flight system */
	private Button sendButton = null;

	/** A handler instance for all user input */
	private EventHandler handler = null;

	/**
	 * Create an instance of the uplink output composite
	 * 
	 * @param appContext
	 *            The ApplicationContext in which this object is being used
	 * @param parent
	 *            The parent widget of this composite
	 */
	public UplinkOutputComposite(final ApplicationContext appContext,
			final Composite parent) {
		super(appContext, parent, SWT.NONE);

		handler = new EventHandler();

		createControls();

		setTabList(new Control[] {});
		layout(true);
	}

	/**
	 * Create the various widgets in the body of the GUI
	 */
	private void createControls() {
		setLayout(new FormLayout());

		final int buttonLeft = 10;
		final int buttonRight = 30;
		final int labelLeft = 35;
		final int labelRight = 100;

		saveAsRawFileButton = new Button(this, SWT.PUSH);
		saveAsRawFileButton.setText("Save Raw File...");
		final FormData sahfbFormData = new FormData();
		sahfbFormData.top = new FormAttachment(20);
		// sahfbFormData.right = new FormAttachment(buttonRight);
		sahfbFormData.left = new FormAttachment(buttonLeft);
		saveAsRawFileButton.setLayoutData(sahfbFormData);
		saveAsRawFileButton.addSelectionListener(handler);

		saveAsRawFileLabel = new Label(this, SWT.LEFT);
		saveAsRawFileLabel
				.setText("Save corrupted data in a raw uplink data file");
		final FormData sahflFormData = new FormData();
		sahflFormData.top = new FormAttachment(saveAsRawFileButton, 0,
				SWT.CENTER);
		sahflFormData.left = new FormAttachment(labelLeft);
		sahflFormData.right = new FormAttachment(labelRight);
		saveAsRawFileLabel.setLayoutData(sahflFormData);

		saveAsScmfButton = new Button(this, SWT.PUSH);
		saveAsScmfButton.setText("Save SCMF...");
		final FormData sasbFormData = new FormData();
		sasbFormData.top = new FormAttachment(45);
		// sasbFormData.right = new FormAttachment(buttonRight);
		sasbFormData.left = new FormAttachment(buttonLeft);
		saveAsScmfButton.setLayoutData(sasbFormData);
		saveAsScmfButton.addSelectionListener(handler);

		saveAsScmfLabel = new Label(this, SWT.LEFT);
		saveAsScmfLabel.setText("Save corrupted data in an SCMF file");
		final FormData saslFormData = new FormData();
		saslFormData.top = new FormAttachment(saveAsScmfButton, 0, SWT.CENTER);
		saslFormData.left = new FormAttachment(labelLeft);
		saslFormData.right = new FormAttachment(labelRight);
		saveAsScmfLabel.setLayoutData(saslFormData);

		sendButton = new Button(this, SWT.PUSH);
		sendButton.setText("Send Now...");
		final FormData sbFormData = new FormData();
		sbFormData.top = new FormAttachment(70);
		// sbFormData.right = new FormAttachment(buttonRight);
		sbFormData.left = new FormAttachment(buttonLeft);
		sendButton.setLayoutData(sbFormData);
		sendButton.addSelectionListener(handler);

		sendLabel = new Label(this, SWT.LEFT);
        sendLabel.setText("Send the corrupted data to the Flight Software");
		final FormData slFormData = new FormData();
		slFormData.top = new FormAttachment(sendButton, 0, SWT.CENTER);
		slFormData.left = new FormAttachment(labelLeft);
		slFormData.right = new FormAttachment(labelRight);
		sendLabel.setLayoutData(slFormData);

		setDefaultFieldValues();
	}

	/**
	 * Set the default values of all the fields
	 */
	private void setDefaultFieldValues() {

	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.tcapp.app.gui.fault.FaultInjectorGuiComponent#destroy()
	 */
	@Override
	public void destroy() {
		dispose();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.tcapp.app.gui.fault.FaultInjectorGuiComponent#getDescription()
	 */
	@Override
	public String getDescription() {
		return (FAULT_PAGE_DESC);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.tcapp.app.gui.fault.FaultInjectorGuiComponent#getTitle()
	 */
	@Override
	public String getTitle() {
		return (FAULT_PAGE_TITLE);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.tcapp.app.gui.fault.FaultInjectorGuiComponent#getCurrentState()
	 */
	@Override
	public FaultInjectionState getCurrentState() {
		return (dataState);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.tcapp.app.gui.fault.FaultInjectorGuiComponent#setFromState(jpl.gds.tcapp.app.gui.fault.FaultInjectionState)
	 */
	@Override
	public void setFromState(final FaultInjectionState state) {
		dataState = state;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.tcapp.app.gui.fault.FaultInjectorGuiComponent#updateDisplay()
	 */
	@Override
	public void updateDisplay() throws FaultInjectorException {
		if (dataState.rawOutputHex == null) {
			throw new IllegalStateException(
					getTitle()
							+ " display does not have enough information to construct the editor GUI.");
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.tcapp.app.gui.fault.FaultInjectorGuiComponent#updateState()
	 */
	@Override
	public void updateState() throws FaultInjectorException {
		// no state to save
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.tcapp.app.gui.fault.FaultInjectorGuiComponent#getTextFieldFont()
	 */
	@Override
	public Font getTextFieldFont() {
		return (new Font(getDisplay(), new FontData(FONT_NAME, 14, SWT.NONE)));
	}

	/**
	 * 
	 * A handler to deal with all the user mouse clicks in the GUI. There are
	 * only 3 buttons to worry about in this case.
	 * 
	 *
	 */
	public class EventHandler extends SelectionAdapter {
		/**
		 * Create an event handler
		 */
		protected EventHandler() {}

		/**
		 * {@inheritDoc}
		 * 
		 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
		 */
		@Override
		public void widgetSelected(final SelectionEvent se) {
		    appContext.getBean(IMessagePublicationBus.class).publish(
		            appContext.getBean(ICommandMessageFactory.class).createClearUplinkGuiLogMessage());

			if (se.getSource() == saveAsRawFileButton) {
				saveRawDataFile();
			} else if (se.getSource() == saveAsScmfButton) {
				saveScmf();
			} else if (se.getSource() == sendButton) {
				UplinkExecutors.uplinkExecutor.execute(new Runnable() {
					@Override
					public void run() {
						try {
							sendRawData();
						} catch (final FaultInjectorException e) {
						}
					}
				});
			}
		}

		/**
		 * Save the current generated user information into a raw data file.
		 */
		private void saveRawDataFile() {
			final boolean saveHex = false;

			String filename = null;

			// let the user choose a filename and where to save the file
			final SWTUtilities util = new SWTUtilities();
			try {
				filename = util.displayStickyFileSaver(getShell(),
						appContext.getBean(IGeneralContextInformation.class).getOutputDir(), "UplinkOutputComposite",
								OutputFileNameFactory.createName(appContext, "FaultInjectionData",
										saveHex ? OutputFileNameFactory.HEX_EXTENSION
												: OutputFileNameFactory.RAW_EXTENSION,
												false));

				if (filename == null) {
					return;
				}

				// write the file to disk
				FileUtility.writeRawDataFileFromHex(filename,
						dataState.rawOutputHex, saveHex);
			} catch (final Exception e) {
				SWTUtilities.showErrorDialog(getShell(),
						"Data File Write Error",
						"Could not write raw data file to location " + filename
						+ " due to an error: " + e.getMessage());
				return;
			}

			// confirm to the user
			SWTUtilities.showMessageDialog(getShell(), "Raw Data File Saved",
					"Raw data file successfully saved to " + filename);
		}

		/**
		 * Save the current generated user information into an SCMF.
		 */
		private void saveScmf() {
			// let the user choose a filename and where to save the file
			final SWTUtilities util = new SWTUtilities();
			String filename = null;
			boolean oldValue = false;
			
			try {
				filename = util.displayStickyFileSaver(getShell(),
						appContext.getBean(IGeneralContextInformation.class).getOutputDir(), "UplinkOutputComposite",
								OutputFileNameFactory.createName(appContext, "FaultInjectionData",
										OutputFileNameFactory.SCMF_EXTENSION, false));
				if (filename == null) {
					return;
				}

				final ScmfProperties props = appContext.getBean(ScmfProperties.class);
				props.setScmfName(filename);
				oldValue = props.getWriteScmf();
				props.setWriteScmf(true);

                /**
                 * If the data wasn't modified in the raw output editor, then
                 * let's write the cltus. This allows the saved SCMF to have the appropriate number of
                 * command messages.
                 * If it was modified in the raw output editor, however, we can't be
                 * sure that the supplied data can be parsed back into valid CLTUs. In this case, let's
                 * write the raw data to the SCMF as one command message. (We did only this back in R7)
                 */
                if (dataState.rawOutputHex.equalsIgnoreCase(cltusToHex())) {
                    appContext.getBean(IScmfFactory.class).toScmf(dataState.cltus, true);
                }
                else {
                    final byte[] totalRawBytes = BinOctHexUtility.toBytesFromHex(dataState.rawOutputHex);
                    appContext.getBean(IScmfFactory.class).toScmf(totalRawBytes, true);
                }

			} catch (final Exception e) {
				SWTUtilities.showErrorDialog(getShell(), "SCMF Write Error",
						"Could not write SCMF file to location " + filename
						+ " due to an error: " + e.getMessage());
				appContext.getBean(ScmfProperties.class).setWriteScmf(oldValue);
				return;
			}

			// display a confirmation to the user
			SWTUtilities.showMessageDialog(getShell(), "SCMF Saved",
					"SCMF file successfully saved to " + filename);
		}

        private String cltusToHex() throws FaultInjectorException {
            final StringBuilder hex = new StringBuilder(1024);
            for (final ICltu cltu : dataState.cltus) {
                try {
                	// changed to getPlopBytes - getBytes doesn't include plop acquisition and idle bytes
                    hex.append(BinOctHexUtility.toHexFromBytes(cltu.getPlopBytes()));
                }
                catch (final Exception e) {
                    throw new FaultInjectorException("Error transforming CLTUs to bytes: " + e.getMessage(), e);
                }
            }
            return hex.toString();
        }

		/**
		 * Send the current generated user information to the flight system.
		 * 
		 * @throws FaultInjectorException
		 */
		private boolean sendRawData() throws FaultInjectorException {
			// We can't know whether or not the user has left us with valid
			// CLTUs to transmit, so
			// we have to transmit the data as a raw data file

			// write a temporary raw data file to disk
			File dataFile = null;
			try {
				dataFile = File.createTempFile("chill_up", ".hex");
				final FileWriter fw = new FileWriter(dataFile);
				fw.write(dataState.rawOutputHex);
				fw.flush();
				fw.close();
			} catch (final IOException e) {
				final String errorMessage = "Could not write temporary raw data file to location "
						+ dataFile.getAbsolutePath()
						+ " due to an error: "
						+ e.getMessage();
				SWTUtilities.showErrorDialog(getShell(), "Filesystem Error",
						errorMessage);

				throw new FaultInjectorException(errorMessage);
			}

			final TransmitEvent event = new TransmitEvent();
			final int transmitEventId = event.hashCode();

			boolean success = true;
			String eventMessage = "Command Successfuly Transmitted";

			// send the raw data file we generated
			try {
				SendRawUplinkDataApp.sendRawUplinkData(appContext, dataFile, true, true,
						transmitEventId);
			} catch (final UplinkException e) {
				eventMessage = e.getUplinkResponse().getDiagnosticMessage();

				final String finalMessage = eventMessage;
				if (!UplinkOutputComposite.this.isDisposed()) {
					SWTUtilities.safeAsyncExec(getDisplay(), "Fault Injector",
							new Runnable() {
								@Override
								public void run() {
									SWTUtilities.showErrorDialog(getShell(),
											"Radiation Error", finalMessage);
								}
							});
				}

				success = false;

				throw new FaultInjectorException(eventMessage);
			} catch (final RawOutputException e) {
				eventMessage = "Missing output adapter for venue: "
						+ e.getMessage();
				SWTUtilities.showErrorDialog(getShell(), "Raw Output Error",
						eventMessage);
				success = false;

				throw new FaultInjectorException(eventMessage);
			} finally {
				event.setTransmitInfo(UplinkOutputComposite.this, null,
						success, eventMessage);
				notifyListeners(event);
			}

			if (!UplinkOutputComposite.this.isDisposed()) {
				SWTUtilities.safeAsyncExec(UplinkOutputComposite.this.getDisplay(), "Fault Injector", new Runnable() {

					@Override
					public void run() {
						SWTUtilities
								.showMessageDialog(getShell(),
										"Corrupted Data Sent",
										"The corrupted uplink data was successfully transmitted.");
					}

				});
			}

			return true;
		}
	}

//	/**
//	 * A command line interface for testing.
//	 * 
//	 * @param args
//	 *            The command line arguments
//	 * 
//	 * @throws Exception
//	 *             If anything at all goes wrong.
//	 */
//	public static void main(final String[] args) throws Exception {
//		Display display = null;
//		try {
//			display = Display.getDefault();
//		} catch (SWTError e) {
//			if (e.getMessage().indexOf("No more handles") != -1) {
//				throw new RuntimeException(
//						"Unable to initialize user interface.  If you are using X-Windows, make sure your DISPLAY variable is set.");
//			} else {
//				throw (e);
//			}
//		}
//
//		SessionConfiguration tc = new SessionConfiguration(true);
//		SessionConfiguration.setGlobalInstance(tc);
//
//		Shell uos = new Shell(display, SWT.SHELL_TRIM);
//		uos.setSize(700, 525);
//		uos.setLayout(new FillLayout());
//
//		UplinkOutputComposite uoc = new UplinkOutputComposite(uos);
//
//		FaultInjectionState fis = new FaultInjectionState();
//		fis.rawOutputHex = "ABCD";
//		uoc.setFromState(fis);
//		uoc.updateDisplay();
//		uoc.layout(true);
//
//		uos.open();
//		while (!uos.isDisposed()) {
//			if (!display.readAndDispatch()) {
//				display.sleep();
//			}
//		}
//		display.dispose();
//	}

	@Override
	public void initiateSend() throws UplinkParseException {
		// no-op, because we are not using the common send button

	}

	@Override
	public boolean needSendButton() {
		return false;
	}

	@Override
	public String getDisplayName() {
		return "Fault Injector";
	}

	@Override
	public void setFieldsFromTransmitHistory(final TransmitEvent historyItem) {
		// TODO figure out how to repoulate this wizard
	}
}
