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
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.perspective.view.View;
import jpl.gds.perspective.view.ViewConfiguration;
import jpl.gds.session.message.SessionMessageType;
import jpl.gds.shared.exceptions.ExcessiveInterruptException;
import jpl.gds.shared.log.AmpcsLog4jMessage;
import jpl.gds.shared.log.GuiNotifier;
import jpl.gds.shared.log.GuiTraceListener;
import jpl.gds.shared.log.ILogMessage;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.message.CommonMessageType;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.message.IMessageType;
import jpl.gds.shared.message.MessageSubscriber;
import jpl.gds.shared.metadata.context.IContextKey;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.swt.ChillColorCreator;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.swt.types.ChillColor;
import jpl.gds.shared.thread.SleepUtilities;
import jpl.gds.telem.down.perspective.view.DownMessageViewConfiguration;
import jpl.gds.telem.input.api.TmInputMessageType;
import jpl.gds.tm.service.api.TmServiceMessageType;
/**
 * MessageTable displays the list of internal messages received by the
 * downlink control GUI.
 *
 *
 */
public class MessageTable implements MessageSubscriber, View, GuiTraceListener {
	private static final String TITLE = "Downlink Messages";

	private static final String[] columnNames = new String[] {
		"", 
		"Message Type",
		"Receive Time",
		"Message"
	};

	private static final int ICON_COLUMN = 0;
	private static final int TYPE_COLUMN = 1;
	private static final int TIME_COLUMN = 2;    
	private static final int MESSAGE_COLUMN = 3;
	private static final int BATCH_SIZE = 15;
	private static final long FLUSH_INTERVAL = 3000;

	private static Image redMessage;
	private static Image yellowMessage;
	private static Image greenMessage;
	private static Image purpleMessage;

    private Table                                                     theMessageTable;
	private Shell parent;
	private final int maxRows;
	private final ArrayBlockingQueue<jpl.gds.shared.message.IMessage> messageQueue;
	private DownMessageViewConfiguration config;
	private final List<jpl.gds.shared.message.IMessage> displayList;

    private final GuiNotifier                                         notifier;
    private final IContextKey                                         key;
    private final IMessagePublicationBus                              bus;
    private final SseContextFlag                                      sseFlag;

	/**
     * Creates an instance of MessageTable.
     * 
     * @param config
     *            View configuration
     * @param key
     *            current IContextKey
     * @param bus
     *            internal message bus
     * @param sseFlag
     *            SSE context flag
     * @param notifier
     *            <GuiNotifier>s
     */
    public MessageTable(final ViewConfiguration config, final IContextKey key, final IMessagePublicationBus bus,
            final SseContextFlag sseFlag, final GuiNotifier notifier) {
		this.config = (DownMessageViewConfiguration)config;
        this.key = key;
        this.bus = bus;
        this.sseFlag = sseFlag;
		maxRows = this.config.getMaxRows();
        messageQueue = new ArrayBlockingQueue<>(maxRows * 2);
        displayList = new ArrayList<>(maxRows);

        this.notifier = notifier;
        notifier.addContextListener(this);
	}

    /**
     * Subscribe to internal bus messages
     */
    public void subscribe() {
        final IMessagePublicationBus context = bus;
        context.unsubscribeAll(this);

        // subscribe for SESSION message types
        // SESSION messages do not extend IPublishableLogMessage, so they cannot be
        // routed to the GUI like everything else. AbstractSessionMessage needs to extend
        // IPublishableLogMessage but this requires schema changes. Deferred for now

        if (config.isShowTestControl()) {
            context.subscribe(SessionMessageType.StartOfSession, this);
            context.subscribe(SessionMessageType.EndOfSession, this);
        }
    }

	private void startDisplayTimer() {
		/* Name the timer thread. */
		final Timer displayTimer = new Timer("Downlink GUI Update Timer");
		displayTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				displayMessages(true);
			}
		}, FLUSH_INTERVAL, FLUSH_INTERVAL);
	}

	/**
	 * Clears all rows in the table.
	 *
	 */
	@Override
	public void clearView() {
		synchronized(theMessageTable) {
			theMessageTable.removeAll();
		}
		synchronized (displayList) {
			displayList.clear();
		}
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.perspective.view.View#getMainControl()
	 */
	@Override
	public Control getMainControl() {
		return theMessageTable;
	}

	/**
	 * Creates the controls and composites for this table.
	 */
	private void createControls() {

		if (yellowMessage == null) {
			yellowMessage = SWTUtilities.createImage(parent.getDisplay(), "jpl/gds/down/gui/MessageYellow.gif");
			redMessage = SWTUtilities.createImage(parent.getDisplay(), "jpl/gds/down/gui/MessageRed.gif");
			greenMessage = SWTUtilities.createImage(parent.getDisplay(), "jpl/gds/down/gui/MessageGreen.gif");
			purpleMessage = SWTUtilities.createImage(parent.getDisplay(), "jpl/gds/down/gui/MessagePurple.gif");
		}

		theMessageTable = new Table(parent, SWT.FULL_SELECTION | SWT.MULTI | SWT.VIRTUAL);
		theMessageTable.setHeaderVisible(true);
        if (sseFlag.isApplicationSse()) {
			theMessageTable.setBackground(ChillColorCreator.getColor(new ChillColor( 
					ChillColor.ColorName.LIGHT_AQUA_BLUE)));
		}
		theMessageTable.setLinesVisible(true);
		theMessageTable.addListener(SWT.SetData, new SetDataCallbackListener());

		final TableColumn[] tableCols = new TableColumn[columnNames.length];
		for (int i = 0; i < columnNames.length; i++) {
			tableCols[i] = new TableColumn(theMessageTable, SWT.NONE);
			tableCols[i].setText(columnNames[i]);
		}  
		tableCols[ICON_COLUMN].setWidth(20);
		tableCols[TYPE_COLUMN].setWidth(165);
		tableCols[TIME_COLUMN].setWidth(185);
		tableCols[MESSAGE_COLUMN].setWidth(500);

		final Menu viewMenu = new Menu(theMessageTable);
		final MenuItem copyMenuItem =  new MenuItem(viewMenu, SWT.PUSH);
		copyMenuItem.setText("Copy");
		copyMenuItem.setEnabled(false);
		theMessageTable.setMenu(viewMenu);

		copyMenuItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				try {
					final int[] indices = theMessageTable.getSelectionIndices();
					if (indices == null || indices.length == 0) {
						return;
					}   
					Arrays.sort(indices);
					Clipboard clipboard = new Clipboard(theMessageTable.getDisplay());
					final StringBuffer plainText = new StringBuffer();
					for (int i = 0; i < indices.length; i++) {
						final TableItem item = theMessageTable.getItem(indices[i]);
						for (int j = 0; j < theMessageTable.getColumnCount(); j++) {
							plainText.append("\"" + item.getText(j) + "\"");
							if (j < theMessageTable.getColumnCount() - 1) {
								plainText.append(',');
							}
						}
						plainText.append('\n');
					}
					final TextTransfer textTransfer = TextTransfer.getInstance();
					clipboard.setContents(new String[]{plainText.toString()}, new Transfer[]{textTransfer});
					clipboard.dispose();
					clipboard = null;
				} catch (final Exception ex) {
					ex.printStackTrace();
				}
			}
		});

		theMessageTable.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
				try {
					final int[] i = theMessageTable.getSelectionIndices();
					if (i != null && i.length != 0) {
						copyMenuItem.setEnabled(true);
					} else {
						copyMenuItem.setEnabled(false);
					}
				} catch (final Exception ex) {
					ex.printStackTrace();
				}
			}
		});

	}  

	/**
	 * This class defines the listener that actually performs table updates.
	 * 
	 *
	 */
	private class SetDataCallbackListener implements Listener {
		@Override
		public void handleEvent(final Event event) {
			try {
				final TableItem item = (TableItem)event.item;

				IMessage msg = null;
				final int tableIndex = theMessageTable.indexOf(item);
				synchronized (displayList) {
					if (tableIndex >= displayList.size()) {
						return;
					}
					msg = displayList.get(tableIndex);
				}
				item.setText(TYPE_COLUMN, formatType(msg.getType().getSubscriptionTag()));
				item.setText(MESSAGE_COLUMN, msg.getOneLineSummary());
				item.setText(TIME_COLUMN, msg.getEventTimeString());
				final Image image = getImageForType(msg);
				if (image != null) {
					item.setImage(ICON_COLUMN, image);
				}
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
	}

    /**
     * Removes the GUI Listener from the log message table
     */
    public void stopGuiListener() {
        synchronized (notifier) {
            notifier.removeContextListener(this);
        }
    }

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void handleMessage(final IMessage message) {
		// Filter out any log messages the user elected not to see
        final IMessageType msgType = message.getType();

		if (message instanceof ILogMessage) {
			final TraceSeverity severity = ((ILogMessage)message).getSeverity();
			final String msgString =  ((ILogMessage)message).getMessage();
            
			switch (severity) {
			case INFO: 
				if (!config.isShowInfoLog()) {
					return;
				}
				break;
            case WARN:
            case WARNING:
				if (!config.isShowWarningLog()) {
					return;
				}
				break;
				
			case ERROR:
			    /* ERROR case not the same as the FATAL case.
			     * We do not want a pop-up dialog for errors. 
			     */
			    if (!config.isShowErrorLog()) {
                    return;
                }  
			    break;
			case FATAL:
                if (!config.isShowErrorLog()) {
					return;
				}                
                if (parent.isDisposed()) {
                    return;
                }
                    parent.getDisplay().asyncExec(new Runnable() {
                        @Override
                        @SuppressWarnings("REC_CATCH_EXCEPTION")
                        public void run() {
                            try {
                                SWTUtilities.showErrorDialog(parent, TraceSeverity.ERROR.getValueAsString(), msgString);
                            }
                            catch (final Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
				break;
				
			default:
			   // do nothing - we'll try to display the message		
			}
		}

		if (parent.isDisposed()) {
			return;
		}

        if (!config.isShowLog() && msgType.equals(CommonMessageType.Log)) {
            return;
        }
        if (!config.isShowTestControl() && (msgType.equals(SessionMessageType.StartOfSession)
                || msgType.equals(SessionMessageType.EndOfSession) || msgType.equals(CommonMessageType.EndOfData)
                || msgType.equals(CommonMessageType.StartOfData))) {
            return;
        }
        /*  add out of sync messages to this set */
        if (!config.isShowFrameSync()
                && (msgType.equals(TmServiceMessageType.InSync) || msgType.equals(TmServiceMessageType.LossOfSync)
                        || msgType.equals(TmServiceMessageType.OutOfSyncData)
                        || msgType.equals(TmServiceMessageType.TelemetryFrameSummary))) {
            return;
        }
        if (!config.isShowPacketExtract() && msgType.equals(TmServiceMessageType.TelemetryPacketSummary)) {
            return;
        }
        if (!config.isShowRawData() && msgType.equals(TmInputMessageType.TelemetryInputSummary)) {
            return;
        }

		try
		{
			while (! SleepUtilities.fullOffer(messageQueue,
					message,
					100L,
					TimeUnit.MILLISECONDS).getTaken())
			{
				displayMessages(false);
			}
		}
		catch (final ExcessiveInterruptException eie)
		{
			eie.printStackTrace();
		}

		displayMessages(false);
	} 


	/**
	 * Formats a message type for display.
	 * @param type the internal message type
	 * @return the display String
	 */
	private String formatType(final String type) {
		final StringBuffer buf = new StringBuffer();
		buf.append(type.charAt(0));
		for (int i =1; i < type.length(); i++) {
			if (Character.isUpperCase(type.charAt(i))) {
				buf.append(' ');
				buf.append(type.charAt(i));
			} else {
				buf.append(type.charAt(i));
			}
		}
		return buf.toString();
	}

	private void displayMessages(final boolean flush) {
		if (messageQueue.size() > BATCH_SIZE 
                || flush && !messageQueue.isEmpty()) {
			if (parent.isDisposed()) {
				return;
			}
			parent.getDisplay().asyncExec(new Runnable () {
				@Override
				@SuppressWarnings("REC_CATCH_EXCEPTION")
				public void run () {
					try {
						if (theMessageTable.isDisposed()) {
							return;
						}

                        if (messageQueue.isEmpty()) {
							return;
						}
						final int messagesInQueue = messageQueue.size();
						int addNewRows = 0;
						int removeCount = 0;
						synchronized(displayList) {
							// If there are more messages queued than the maximum number of display
							// rows, we'll remove everything from the table and display
							// max rows new ones, starting with the oldest.
							//
							// If there are fewer messages queue than the maximum number to display,
							// we'll remove enough rows from the table to make room for them and
							// display all new messages, starting with the oldest
							if (messagesInQueue > maxRows) {
								removeCount = displayList.size();
								displayList.clear();
								addNewRows = maxRows;
							} else {
								if (displayList.size() + messagesInQueue > maxRows) {
									final int availRows = maxRows - displayList.size();
									if (availRows < messagesInQueue) {
										removeCount = messagesInQueue - availRows;
										if (removeCount > displayList.size()) {
											removeCount = displayList.size();
										}
									}
									for (int i = 0; i < removeCount; i++) {
										displayList.remove(0);
									}

								}
								addNewRows = Math.min(messagesInQueue, maxRows - displayList.size());
							}
                            for (int i = 0; i < addNewRows && !messageQueue.isEmpty(); i++) {

								displayList.add(messageQueue.poll(10, TimeUnit.MILLISECONDS));
							}
							theMessageTable.remove(0, removeCount - 1);
						}
						theMessageTable.setItemCount(displayList.size());    
						theMessageTable.showItem(theMessageTable.getItem(displayList.size() - 1));


					} catch (final Exception e) {
						e.printStackTrace();
					}
				}
			});      
		}
	}

	/**
	 * Gets the icon image for a particular message.
	 * @param msg the Message
	 * @return the Image to display for the message
	 */
	private Image getImageForType(final IMessage msg) {
	    
        /* Rework this.  Only start/end of
         * session messages need special handling. Everything else is a log
         * message.
         */
        if (msg.isType(SessionMessageType.StartOfSession)) {
			return greenMessage;
        } else if (msg.isType(SessionMessageType.EndOfSession)) {
			return greenMessage;
        } else if (msg instanceof ILogMessage) {
			final ILogMessage log = (ILogMessage)msg;
			final TraceSeverity level = log.getSeverity();
			switch (level) {
                case INFO:    
                    return greenMessage;
                case ERROR:
                    return redMessage;
                case WARN:
                case WARNING:
                    return yellowMessage;
                default:
                    return purpleMessage;
			}
		}
        
        /*
         * Return purple icon rather than none if the message is not recognized.
         */
		return purpleMessage;
	}


	/**
	 * {@inheritDoc}
	 * @see jpl.gds.perspective.view.View#getDefaultName()
	 */
	@Override
	public String getDefaultName() {
		return TITLE;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.perspective.view.View#updateViewConfig()
	 */
	@Override
	public void updateViewConfig() {
		// nothing to do - cannot change the config from this class
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.perspective.view.View#init(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void init(final Composite parent) {
		if (!(parent instanceof Shell)) {
			throw new IllegalArgumentException("Input object must be an instance of Shell");
		}
		this.parent = (Shell)parent;
		createControls();
		startDisplayTimer();
        subscribe();
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.perspective.view.View#getViewConfig()
	 */
	@Override
	public IViewConfiguration getViewConfig() {
		return config;
	}   

	/**
	 * Sets a new view configuration
	 * @param config the DownMessageViewConfiguration to set
	 */
	public void setViewConfig(final DownMessageViewConfiguration config) {
		this.config = config;
        subscribe();
	}


    @Override
    public void notifyTraceListeners(final AmpcsLog4jMessage message) {
        handleMessage(message);
    }


}
