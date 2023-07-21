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
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.springframework.context.ApplicationContext;

import jpl.gds.message.api.status.IStatusMessageFactory;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.ILogMessage;
import jpl.gds.shared.log.IPublishableLogMessage;
import jpl.gds.shared.log.LogMessageType;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.CommonMessageType;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.swt.ChillShell;
import jpl.gds.shared.swt.SWTUtilities;

/**
 * LogEntryShell is a GUI window that allows the user to make a log entry.
 * 
 *
 */
public class LogEntryShell implements ChillShell {
    private static final String TITLE = "Log Entry";
    private Shell logShell;
    private final Shell parent;
    private Text logText;
    private IPublishableLogMessage message;
    private Combo classification;
    private boolean canceled;

    private Table messageTable;

    private static final String[] columnNames = new String[] { "",
            "Receive Time", "Message" };

    private static final int ICON_COLUMN = 0;
    private static final int TIME_COLUMN = 1;
    private static final int MESSAGE_COLUMN = 2;

    private static Image redMessage;
    private static Image yellowMessage;
    private static Image purpleMessage;
    
    private final IMessagePublicationBus publishContext;
    private final IStatusMessageFactory statusMessageFactory;
    private final Tracer log;

    /**
     * Creates an instance of LogEntryShell.
     * 
     * @param appContext the current application context
     * @param display
     *            the parent Display
     */
    public LogEntryShell(final Display display, final ApplicationContext appContext) {
        parent = new Shell(display, SWT.NONE);
        publishContext = appContext.getBean(IMessagePublicationBus.class);
        statusMessageFactory = appContext.getBean(IStatusMessageFactory.class);
        log = TraceManager.getDefaultTracer(appContext);
        createControls();
    }

    /**
     * Creates an instance of LogEntryShell.
     * 
     * @param parent
     *            the parent Shell
     * @param pubContext the internal message bus to publish to
     */
    public LogEntryShell(final Shell parent, final ApplicationContext appContext) {
        this.parent = parent;
        publishContext = appContext.getBean(IMessagePublicationBus.class);
        statusMessageFactory = appContext.getBean(IStatusMessageFactory.class);
        log = TraceManager.getDefaultTracer(appContext);
        createControls();
    }

    /**
     * Creates the GUI controls.
     */
    protected void createControls() {
        logShell = new Shell(parent, SWT.SHELL_TRIM | SWT.DIALOG_TRIM);
        logShell.setText(TITLE);
        logShell.setSize(625, 320);
        final FormLayout shellLayout = new FormLayout();
        shellLayout.spacing = 5;
        shellLayout.marginHeight = 5;
        shellLayout.marginWidth = 5;
        logShell.setLayout(shellLayout);

        final Label classLabel = new Label(logShell, SWT.RIGHT);
        classLabel.setText("Classification:");
        classification = new Combo(logShell, SWT.DROP_DOWN | SWT.READ_ONLY);
        final FormData fd3 = new FormData();
        fd3.top = new FormAttachment(0);
        fd3.left = new FormAttachment(20);
        classification.setLayoutData(fd3);
        classification.add(TraceSeverity.INFO.getValueAsString());
        classification.add(TraceSeverity.WARN.getValueAsString());
        classification.add(TraceSeverity.ERROR.getValueAsString());
        classification.setText(TraceSeverity.INFO.getValueAsString());

        final FormData fd4 = new FormData();
        fd4.right = new FormAttachment(classification);
        fd4.top = new FormAttachment(classification, 0, SWT.CENTER);
        classLabel.setLayoutData(fd4);

        final Label logLabel = new Label(logShell, SWT.NONE);
        logLabel.setText("Log Text:");
        logText = new Text(logShell, SWT.SINGLE | SWT.BORDER);
        logText.setText("");
        final FormData fd2 = SWTUtilities.getFormData(logText, 1, 60);
        fd2.left = new FormAttachment(15);
        fd2.top = new FormAttachment(classification);
        fd2.right = new FormAttachment(100);
        logText.setLayoutData(fd2);
        final FormData fd1 = new FormData();
        fd1.right = new FormAttachment(logText);
        fd1.top = new FormAttachment(logText, 0, SWT.CENTER);
        logLabel.setLayoutData(fd1);

        final Composite composite = new Composite(logShell, SWT.NONE);
        final GridLayout rl = new GridLayout(3, true);
        rl.makeColumnsEqualWidth = false;
        composite.setLayout(rl);
        final FormData formData8 = new FormData();
        formData8.right = new FormAttachment(100);
        formData8.bottom = new FormAttachment(100);
        composite.setLayoutData(formData8);

        final Button applyButton = new Button(composite, SWT.PUSH);
        applyButton.setText("Send Log");
        logShell.setDefaultButton(applyButton);
        final GridData gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        applyButton.setLayoutData(gd);
        final Button cancelButton = new Button(composite, SWT.PUSH);
        cancelButton.setText("Cancel");
        cancelButton.setLayoutData(gd);
        final Button clearButton = new Button(composite, SWT.PUSH);
        clearButton.setText("Clear");
        clearButton.setLayoutData(gd);
        final Label line = new Label(logShell, SWT.SEPARATOR | SWT.HORIZONTAL
                | SWT.SHADOW_ETCHED_IN);
        final FormData formData6 = new FormData();
        formData6.left = new FormAttachment(0, 3);
        formData6.right = new FormAttachment(100, 3);
        formData6.bottom = new FormAttachment(composite, 5);
        line.setLayoutData(formData6);

        // Log table

        final Table logTable = getLogTable();
        final FormData formDataLog = new FormData();
        formDataLog.top = new FormAttachment(logText);
        formDataLog.bottom = new FormAttachment(composite);
        formDataLog.left = new FormAttachment(0);
        formDataLog.right = new FormAttachment(100);
        logTable.setLayoutData(formDataLog);

        // end

        applyButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    final String logStr = logText.getText().trim();
                    if (logStr.equals("")) {
                        SWTUtilities
                                .showMessageDialog(logShell, "Invalid Entry",
                                        "You must enter the log text.");
                        return;
                    }
                    message = statusMessageFactory.createPublishableLogMessage(
                              TraceSeverity.fromStringValue(
                                    classification.getText()), logStr, LogMessageType.USER);
                    canceled = false;

                    logText.setText("");

                    log.log(message);
                    
                    //publishContext.publish(message);

                    final Table logTable = getLogTable();

                    final TableItem item = new TableItem(logTable, SWT.NONE);
                    item.setText(1, message.getEventTimeString());
                    item.setText(2, message.getOneLineSummary());
                    final Image image = getImageForType(message);
                    if (image != null) {
                        item.setImage(ICON_COLUMN, image);
                    }

                    logTable.setTopIndex(logTable.getItemCount() - 1);
                } catch (final Exception eE) {
                    TraceManager
                            .getDefaultTracer()
                            .error(
                                    "APPLY button caught unhandled and " +
                                    "unexpected exception in " +
                                    "LogEntryShell.java: ", ExceptionTools.getMessage(eE), eE);
                }
            }
        });
        cancelButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(
                    final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    message = null;
                    canceled = true;
                    logShell.close();
                } catch (final Exception eE) {
                    TraceManager

                            .getDefaultTracer()
                            .error(
                                    "CANCEL button caught unhandled and " +
                                    "unexpected exception in " +
                                    "LogEntryShell.java");
                }
            }
        });
        clearButton.addSelectionListener(new SelectionAdapter() {

            /*
             * (non-Javadoc)
             * 
             * @see
             * org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse
             * .swt.events.SelectionEvent)
             */
            @Override
            public void widgetSelected(final SelectionEvent event) {
                getLogTable().removeAll();
            }
        });
    }

    /**
     * Returns the associate image for the message severity level.
     * 
     * @param m
     *            Message to grab the severity from.
     * @return Specific image based on severity.
     */
    private Image getImageForType(final IMessage m) {
        if (m.isType(CommonMessageType.Log)) {
            final ILogMessage log = (ILogMessage) m;
            final TraceSeverity level = log.getSeverity();
            if (level.equals(TraceSeverity.ERROR)) {
                return redMessage;
            } else if (level.equals(TraceSeverity.WARN)) {
                return yellowMessage;
            } else {
                return purpleMessage;
            }
        }
        return null;
    }

    /**
     * Retrieves a LogMessage object corresponding to the user's entries.
     * 
     * @return the LogMessage
     */
    public IPublishableLogMessage getLogMessage() {
        return message;
    }

    /**
     * Lazy initialization of the log message table.
     * 
     * @return The single reference to the Table.
     */
    protected synchronized Table getLogTable() {
        if (messageTable == null) {
            messageTable = new Table(logShell, SWT.FULL_SELECTION | SWT.MULTI
                    | SWT.VIRTUAL);
            messageTable.setHeaderVisible(true);
            messageTable.setLinesVisible(true);

            final TableColumn[] tableCols = 
                new TableColumn[columnNames.length];
            for (int i = 0; i < columnNames.length; i++) {
                tableCols[i] = new TableColumn(messageTable, SWT.LEFT);
                tableCols[i].setText(columnNames[i]);
            }
            tableCols[ICON_COLUMN].setWidth(20);
            tableCols[TIME_COLUMN].setWidth(185);
            tableCols[MESSAGE_COLUMN].setWidth(395);

            if (yellowMessage == null) {
                yellowMessage = SWTUtilities.createImage(parent.getDisplay(), "jpl/gds/down/gui/MessageYellow.gif");
                redMessage = SWTUtilities.createImage(parent.getDisplay(), "jpl/gds/down/gui/MessageRed.gif");
                purpleMessage = SWTUtilities.createImage(parent.getDisplay(), "jpl/gds/down/gui/MessagePurple.gif");
            }
        }
        return messageTable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public Shell getShell() {
        return logShell;
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public String getTitle() {
        return TITLE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public void open() {
        message = null;
        canceled = true;
        logShell.open();
    }

    /**
     * {@inheritDoc}
     */
    @Override
	public boolean wasCanceled() {
        return canceled;
    }
}
