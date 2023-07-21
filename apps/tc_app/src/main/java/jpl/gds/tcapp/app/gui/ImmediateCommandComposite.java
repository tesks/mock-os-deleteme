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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.springframework.context.ApplicationContext;

import static jpl.gds.shared.exceptions.ExceptionTools.rollUpMessages;

import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.tc.api.ISendCompositeState;
import jpl.gds.tc.api.command.ICommand;
import jpl.gds.tc.api.command.parser.UplinkInputParser;
import jpl.gds.tc.api.config.UplinkParseException;
import jpl.gds.tc.api.exception.UplinkException;
import jpl.gds.tc.api.message.ICommandMessageFactory;
import jpl.gds.tcapp.app.SendCommandApp;

/**
 * The ImmediateCommandComposite is the GUI class for the Immediate Command
 * tab of chill_up. It facilitates the transmission:
 *   * individual commands, including SSE, represented by a command stem and any arguments
 *   * command list files
 * The display includes a preview window to show the contents of a command list file 
 *
 * 04/05/19 - Updated due to FlightCommand refactor. Command arguments
 *         must be utilized by referencing it through the command and not the individual argument.
 */
public class ImmediateCommandComposite extends AbstractUplinkComposite {

    private final Tracer          log;   
	private final SWTUtilities swtUtil = new SWTUtilities();
	private Text commandInputText;
	private Text commandFileInputText;
	private Button singleCommandRadio;
	private Button commandListRadio;
	private Button placeHolderButton;
	private Button loadCommandFileButton;
	private final EventHandler handler;
	private final Text cmdListField;
	private final IMessagePublicationBus bus;

	public ImmediateCommandComposite(final ApplicationContext appContext, final Composite parent) {
		super(appContext, parent, SWT.NONE);
        log = TraceManager.getDefaultTracer(appContext);
		bus = appContext.getBean(IMessagePublicationBus.class);
		
		handler = new EventHandler();

		final CommandEntryComposite cec = new CommandEntryComposite(this);

		FormData data = new FormData();
		data.top = new FormAttachment(0);
		data.left = new FormAttachment(0);
		data.right = new FormAttachment(100);

		cec.setLayoutData(data);

		final Label cmdListPreviewLabel = new Label(this, SWT.NONE);
		cmdListPreviewLabel.setText("Command List Preview:");

		data = new FormData();
		data.top = new FormAttachment(cec, 0);
		data.left = new FormAttachment(0);

		cmdListPreviewLabel.setLayoutData(data);

		cmdListField = new Text(this, SWT.MULTI);

		data = new FormData();
		data.top = new FormAttachment(cmdListPreviewLabel, 0);
		data.left = new FormAttachment(0);
		data.right = new FormAttachment(100);
		data.bottom = new FormAttachment(100);

		cmdListField.setLayoutData(data);
		cmdListField.setEditable(false);
	}

	private void triggerCommandSend() {
	    
		/* Do nothing if the command field is empty */
		final String command = commandInputText.getText().trim();
		if (command.isEmpty()) {
			return;
		}
		bus.publish(appContext.getBean(ICommandMessageFactory.class).createUplinkGuiLogMessage("Sending command..."));
		sendText(command, false);
		commandInputText.setText("");
	}

	private void triggerFileSend() {
		/* Do nothing if the command file field is empty */
		final String commandFile = commandFileInputText.getText().trim();
		if (commandFile.isEmpty()) {
			return;
		}
		bus.publish(appContext.getBean(ICommandMessageFactory.class).createUplinkGuiLogMessage("Sending command file..."));
		sendText(commandFile, true);
		commandFileInputText.setText("");

	}

	private java.util.List<String> parseCommandListFromFile(final File cmdFile,
			final boolean stripComments) throws IOException {
		if (cmdFile.exists() == false) {
			throw new FileNotFoundException("The command file "
					+ cmdFile.getName() + " does not exist.");
		}

		final java.util.List<String> fileCommandList = new ArrayList<String>();
		final BufferedReader reader = new BufferedReader(new FileReader(cmdFile));

		String readLine = reader.readLine();
		while (readLine != null) {
			String command = readLine.trim();
			if (stripComments) {
				command = UplinkInputParser.removeComments(command);
			}

			// skip lines that are commented
			if (!command.isEmpty()) {
				fileCommandList.add(command);
			}
			readLine = reader.readLine();
		}
		reader.close();

		return (fileCommandList);
	}

	private void sendText(final String inputText, final boolean isFile) {
		this.showProgressIndicatorDialog();

		UplinkExecutors.uplinkExecutor.execute(new Runnable() {
			@Override
			public void run() {
				final TransmitEvent event = new TransmitEvent();
				final int transmitEventId = event.hashCode();

				final ImmediateCommandState state = new ImmediateCommandState();
				boolean success = true;
				String eventMessage = "Command Successfuly Transmitted";

				final ICommandMessageFactory msgFact = appContext.getBean(ICommandMessageFactory.class);
				
				java.util.List<ICommand> commands = new ArrayList<ICommand>();
				try {
					if (isFile) {
					    bus.publish(msgFact.createUplinkGuiLogMessage("Executing command list file: "
								+ inputText + "\n\n"));
						final File inputFile = new File(inputText);
						state.setCommandFilename(inputText);

						commands = UplinkInputParser
								.parseCommandListFromFile(appContext, inputFile);

						parseCommandListFromFile(inputFile, true);

					} else {
					    bus.publish(msgFact.createUplinkGuiLogMessage("Executing Command: "
								+ inputText + "\n\n"));
						state.setCommand(inputText);
						commands.add(UplinkInputParser
								.parseCommandString(appContext, inputText));
					}

					SendCommandApp.sendCommands(appContext, commands, transmitEventId);
				} catch (UplinkException | UplinkParseException e) {
                    // filter out stack dump generated from authorization error
                    // stack dump for other exceptions as a result of SendCommandsApp remain
				    /*
                     * These exceptions should not print a stack trace.
                     * Refactored to use multicatch here, and moved common error message handling to a
                     * private method to minimize repetition.
                     */
				    success = false;
				    eventMessage = handleErrorMessage(e);
				} catch (final Exception e) {
				    e.printStackTrace();
					success = false;
					eventMessage = handleErrorMessage(e);
				} finally {
					ImmediateCommandComposite.this
					.closeProgressIndicatorDialog();

					if (!ImmediateCommandComposite.this.isDisposed()) {
						SWTUtilities.safeAsyncExec(getDisplay(),
								"Immediate Command", new Runnable() {

							@Override
							public void run() {
								cmdListField.setText("");
							}

						});
					}
				}

				event.setTransmitInfo(ImmediateCommandComposite.this, state,
						success, eventMessage);
				notifyListeners(event);
			}
		});
	}
	
	/*
	 * Moved common error message handling from sendText() here.
	 */
	private String handleErrorMessage(final Exception e) {
	    final String message = e.getMessage() == null ? e.toString() : e
                .getMessage();
        final String errorMessage = "Exception encountered while sending uplink: "
                + message;
        bus.publish(appContext.getBean(ICommandMessageFactory.class).createUplinkGuiLogMessage(errorMessage + "\n\n"));
        log.error(errorMessage);

        final String finalEventMessage = message;

        if (!ImmediateCommandComposite.this.isDisposed()) {
            SWTUtilities.safeAsyncExec(getDisplay(),
                    "Immediate Command", new Runnable() {

                @Override
                public void run() {
                    SWTUtilities.showErrorDialog(
                            ImmediateCommandComposite.this
                            .getShell(),
                            "Immediate Command Error",
                            finalEventMessage);
                }

            });
        }
        
        return "Command transmission failed: "
                + errorMessage;
	}

	public class EventHandler extends SelectionAdapter implements
	TraverseListener, VerifyListener, ModifyListener {
		
		protected EventHandler() {}

		/**
		 * {@inheritDoc}
		 * 
		 * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
		 */
		@Override
		public void widgetDefaultSelected(final SelectionEvent arg0) {

		}

		@Override
		// Added to clean up user text when it's pasted into a field
		public void verifyText(final VerifyEvent ve) {
			if(ve.getSource() == ImmediateCommandComposite.this.commandInputText
					|| ve.getSource() == ImmediateCommandComposite.this.commandFileInputText) {
				//want to still allow users to enter single characters that would be trimmed, like spaces and such
				if(!ve.text.isEmpty() && ve.text.length() > 1) {
					ve.text = ve.text.trim();
					ve.doit = !ve.text.isEmpty();
				}
			}
		}

		@Override
		public void keyTraversed(final TraverseEvent arg0) {
			try {
				safeKeyTraversed(arg0);
			} catch (final Exception e) {
				log.error("keyTraversed error: " + rollUpMessages(e));

				e.printStackTrace();
			}
		}

		private void safeKeyTraversed(final TraverseEvent arg0) {
			if (arg0.getSource() == commandInputText) {
				if (arg0.detail == SWT.TRAVERSE_RETURN) {
					triggerCommandSend();
				}
			} else if (arg0.getSource() == commandFileInputText) {
				if (arg0.detail == SWT.TRAVERSE_RETURN) {
					triggerFileSend();
				}
			}
		}

		@Override
		public void widgetSelected(final SelectionEvent arg0) {
			try {
				safeWidgetSelected(arg0);
			} catch (final Exception e) {
				log.error("widgetSelected error: " + rollUpMessages(e));

				e.printStackTrace();
			}
		}

		private void safeWidgetSelected(final SelectionEvent arg0) {
			if (arg0.getSource() == loadCommandFileButton) {
				final String filename = swtUtil.displayStickyFileChooser(false,
						ImmediateCommandComposite.this.getShell(),
						"loadCommandFileButton");

				if (filename == null) {
					return;
				}

				SWTUtilities.safeAsyncExec(ImmediateCommandComposite.this.getDisplay(), log,
						"load command file", () -> {
								commandFileInputText.setText(filename);
								commandFileInputText.forceFocus();
								commandFileInputText.setSelection(filename.length());
								//cmdListField now updated automatically now, except if the file doesn't exist.
								if(!(new File(filename).exists())) {
									cmdListField.append("Command list file " + filename + " does not exist.\n");
								} else {
									updateCommandListPreviewPane(filename.trim());
								}
							}
						);
			}
		}

		@Override
		public void modifyText(final ModifyEvent me) {
			try {
				if(me.getSource() == commandFileInputText) {

					String filePathStr = commandFileInputText.getText();

					if(filePathStr == null) {
						filePathStr = "";
					} else {
						filePathStr = filePathStr.trim();
					}

					updateCommandListPreviewPane(filePathStr);

				}
			} catch (Exception e) {
				log.error("Encountered unexpected exception: "
						          + (e.getCause() != null ? e.getCause() : e.getMessage()), e);
				return;
			}
		}

		private void updateCommandListPreviewPane(String filename) {
			cmdListField.setText("");
			File filePath = new File(filename);

			if (!(filePath.exists() && filePath.isFile())) {
				//return, leave it empty.
				return;
			} else if (!filePath.canRead()) {
				cmdListField.append("command list file "
						                    + filename + " cannot be read\n");
				return;
			}
			cmdListField.append("Contents of list file "
					                    + filename + ":\n\n");
			try {
				File cmdFile = new File(filename);
				java.util.List<String> list = parseCommandListFromFile(
						cmdFile, false);
				for (int index = 0; index < list.size(); index++) {
					String cmd = list.get(index);
					cmdListField.append(cmd + "\n");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	private class CommandEntryComposite extends Composite {
		public CommandEntryComposite(final Composite parent) {
			super(parent, SWT.NONE);

			final GridLayout layout = new GridLayout();
			layout.numColumns = 3;
			this.setLayout(layout);

			singleCommandRadio = new Button(this, SWT.RADIO);
			singleCommandRadio.setText("Command:");
			singleCommandRadio.setSelection(true);

			commandInputText = new Text(this, SWT.NONE);

			commandInputText.addTraverseListener(handler);
			GridData gridData = new GridData();
			gridData.horizontalAlignment = GridData.FILL;
			gridData.grabExcessHorizontalSpace = true;
			commandInputText.setLayoutData(gridData);

			// place holder, does not really do anything
			placeHolderButton = new Button(this, SWT.NONE);
			placeHolderButton.setText("Place");
			placeHolderButton.setVisible(false);

			commandListRadio = new Button(this, SWT.RADIO);
			commandListRadio.setText("Command List File:");

			commandFileInputText = new Text(this, SWT.NONE);
			gridData = new GridData();
			gridData.horizontalAlignment = GridData.FILL;
			gridData.grabExcessHorizontalSpace = true;
			commandFileInputText.setLayoutData(gridData);

			loadCommandFileButton = new Button(this, SWT.NONE);
			loadCommandFileButton.setText("Load...");

			commandFileInputText.addTraverseListener(handler);
			loadCommandFileButton.addSelectionListener(handler);

			// modify handlers to handle radio select when focus is in text
			// field
			commandInputText.addModifyListener(new ModifyListener() {

				@Override
				public void modifyText(final ModifyEvent arg0) {
					selectRadioButton(singleCommandRadio);
				}

			});

			commandFileInputText.addModifyListener(new ModifyListener() {

				@Override
				public void modifyText(final ModifyEvent arg0) {
					selectRadioButton(commandListRadio);
				}

			});
		}

		private void selectRadioButton(final Button buttonToSelect) {
			final Control[] children = this.getChildren();
			for (final Control child : children) {
				if (buttonToSelect != child && child instanceof Button
						&& (child.getStyle() & SWT.RADIO) != 0) {
					((Button) child).setSelection(false);
				}
			}

			buttonToSelect.setSelection(true);
		}
	}

	// changed to public, like the other SendCompositeState classes
	public class ImmediateCommandState implements ISendCompositeState {
		private String command;
		private String filename;

		public void setCommand(final String command) {
			this.command = command;
			this.filename = null;
		}

		public void setCommandFilename(final String filename) {
			this.filename = filename;
			this.command = null;
		}

		public String getCommand() {
			return command;
		}

		public String getCommandFilename() {
			return filename;
		}

		@Override
		public String getTransmitSummary() {
			return command == null ? filename + " (Command List)" : command;
		}
	}

	@Override
	public String getDisplayName() {
		return "Immediate Command";
	}

	@Override
	public void setFieldsFromTransmitHistory(final TransmitEvent historyItem) {
		final ISendCompositeState state = historyItem.getTransmitState();
		if (state instanceof ImmediateCommandState) {

			// updated. Always use the command
			final ImmediateCommandState myState = (ImmediateCommandState) state;
			this.commandInputText.setText(myState.getCommand());
		}
	}
	
	/**
	 * Set immediate command in text field
	 * @param commandText immediate command with arguments or empty string
	 */
	public void setCommandText(final String commandText) {
		if(commandText != null) {
			this.commandInputText.setText(commandText);
		}
	}

	@Override
	public void initiateSend() throws UplinkParseException {
		if (this.singleCommandRadio.getSelection()) {
			this.triggerCommandSend();
		} else if (this.commandListRadio.getSelection()) {
			this.triggerFileSend();
		}
	}

	@Override
	public boolean needSendButton() {
		return true;
	}
}
