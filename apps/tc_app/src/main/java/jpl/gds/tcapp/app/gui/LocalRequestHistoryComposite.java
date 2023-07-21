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
package jpl.gds.tcapp.app.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.connection.IConnectionMap;
import jpl.gds.common.config.types.UplinkConnectionType;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.message.MessageSubscriber;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.swt.TableItemQuickSort;
import jpl.gds.tc.api.CommandStatusType;
import jpl.gds.tc.api.ICpdUplinkStatus;
import jpl.gds.tc.api.message.CommandMessageType;
import jpl.gds.tc.api.message.ICpdUplinkMessage;
import jpl.gds.tc.api.message.IFileLoadMessage;
import jpl.gds.tc.api.message.IFlightCommandMessage;
import jpl.gds.tc.api.message.IInternalCpdUplinkStatusMessage;
import jpl.gds.tc.api.message.IRawUplinkDataMessage;
import jpl.gds.tc.api.message.ITransmittableCommandMessage;
import jpl.gds.tcapp.app.gui.AbstractSendComposite.TransmitListener;

/**
 * Message based composite that adds command history entries to a table when a
 * message is received.
 * 
 *
 */
public class LocalRequestHistoryComposite extends Composite implements
TransmitListener {

	private static final String NO_ID = "No Id";

	public static final int MAX_HISTORY_BUFFER_SIZE = 1000;

	public static final String FILE_LOAD_TRANSMITTER_NAME = "Send File Load";
	public static final String IMMEDIATE_TRANSMITTER_NAME = "Immediate Command";
	public static final String SCMF_TRANSMITTER_NAME = "Send SCMF";
	public static final String CFDP_TRANSMITTER_NAME = "Send CFDP";
	public static final String RAW_FILE_TRANSMITTER_NAME = "Send Raw Data File";
	public static final String FAULT_INJECTOR_TRANSMITTER_NAME = "Fault Injector";

	/* Use images in the cmd package, not the monitor package. */
	private static final String UP_IMAGE = "jpl/gds/tcapp/gui/up.gif";
	private static final String DOWN_IMAGE = "jpl/gds/tcapp/gui/down.gif";
//	private static final int COLOR_COLUMN_INDEX;
//	private static int itemCount = 0;

	private final Table historyTable;
	private final HashMap<Integer, TransmitEvent> eventHistory;
	private final TableColumn[] columns;
	private int sortColumnIndex = -1;
	private final CommandStatusColorMapper colorMapper = new CommandStatusColorMapper();
	private final Map<String, ReceivedMessage> receivedMessages = new HashMap<>();

	private final Queue<TableEntry> tableItemsQueue = new LinkedBlockingQueue<>(
			MAX_HISTORY_BUFFER_SIZE);

	// TODO filtering
	// private TransmitHistoryFilterSelector filterSelectorShell;
	// private TransmitHistoryFilter historyFilter;

	/**
	 * Up arrow for ascending sorting symbol
	 */
	protected Image upImage;

	/**
	 * Down arrow for descending sorting symbol
	 */
	protected Image downImage;

	private Object app;

	private CommandMessageSubscriber subscriber;

	private final ApplicationContext appContext;

	/**
	 * Constructor: Creates a small composite in the uplink window with a table
	 * that subscribes to session based command messages
	 * 
	 * @param parent
	 *            Uplink shell composite
	 */
	public LocalRequestHistoryComposite(final ApplicationContext appContext, final Composite parent) {
		super(parent, SWT.NONE);
		
		this.appContext = appContext;

		final Composite filterComposite = new Composite(this, SWT.None);
		filterComposite.setLayout(new RowLayout());

		// Label filterLabel = new Label(filterComposite, SWT.NONE);
		// filterLabel.setText("Filtering is OFF");
		//
		// Button filterButton = new Button(filterComposite, SWT.PUSH);
		// filterButton.setText("Change Filter");

		final FormData filterFd = new FormData();
		filterFd.top = new FormAttachment(0);

		this.eventHistory = new HashMap<Integer, TransmitEvent>();

		historyTable = new Table(this, SWT.NONE);
		historyTable.setHeaderVisible(true);
		historyTable.setLinesVisible(true);

		columns = new TableColumn[4];

		int i = 0;

		/*
		 * Daniel Hurley asked to remove the ID column from local request
		 * history because it serves no real purpose
		 *
		 * if (SessionConfiguration.getGlobalInstance()
		 * .getUplinkConnectionType()
		 * .equals(UplinkConnectionType.COMMAND_SERVICE)) { columns = new
		 * TableColumn[5]; } else { columns = new TableColumn[4]; }
		 *
		 * if (SessionConfiguration.getGlobalInstance()
		 * .getUplinkConnectionType()
		 * .equals(UplinkConnectionType.COMMAND_SERVICE)) { columns[i] = new
		 * TableColumn(historyTable, SWT.NONE); columns[i].setText("ID");
		 * columns[i].setWidth(300); // columns[0].setData("sortType",
		 * "CHARACTER"); i++; }
		 */

		columns[i] = new TableColumn(historyTable, SWT.NONE);
		columns[i].setText("Source");
		columns[i].setWidth(150);
		i++;

		columns[i] = new TableColumn(historyTable, SWT.NONE);
		columns[i].setText("Status");
		columns[i].setWidth(120);
		columns[i].setData("needsColor");
		i++;

		columns[i] = new TableColumn(historyTable, SWT.NONE);
		columns[i].setText("Data");
		columns[i].setWidth(385);
		i++;

		columns[i] = new TableColumn(historyTable, SWT.NONE);
		columns[i].setText("Timestamp");
		columns[i].setWidth(180);

		final FormData historyTableFd = new FormData();
		historyTableFd.top = new FormAttachment(0);
		historyTableFd.left = new FormAttachment(0);
		historyTableFd.right = new FormAttachment(100);
		historyTableFd.bottom = new FormAttachment(100);
		historyTable.setLayoutData(historyTableFd);

		upImage = SWTUtilities.createImage(Display.getCurrent(), UP_IMAGE);
		downImage = SWTUtilities.createImage(Display.getCurrent(), DOWN_IMAGE);

		final Listener sortListener = new SortListener();
		for (i = 0; i < columns.length; i++) {
			columns[i].addListener(SWT.Selection, sortListener);
		}

		subscriber = new CommandMessageSubscriber();
		
		// Keydown listener to disable text field popup
		historyTable.addListener(SWT.KeyDown, new Listener() {
			@Override
            public void handleEvent(final Event e) {
				if (e.character > 0) {
					e.doit = false;
				}
			}
		});

		// Menu viewMenu = new Menu(this);
		// this.historyTable.setMenu(viewMenu);
		// final MenuItem prefMenuItem = new MenuItem(viewMenu, SWT.PUSH);
		// prefMenuItem.setText("Preferences...");
		//
		// prefMenuItem.addSelectionListener(new SelectionAdapter() {
		// @Override
		// public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
		// try {
		// filterSelectorShell = new
		// TransmitHistoryFilterSelector(parent.getShell());
		// filterSelectorShell.createGUI();
		//
		// if(historyFilter != null) {
		// filterSelectorShell.updateGUI(historyFilter);
		// }
		//
		// Shell filterShell = filterSelectorShell.getShell();
		//
		// filterShell.addDisposeListener(new DisposeListener() {
		// public void widgetDisposed(final DisposeEvent e) {
		//
		// historyFilter = filterSelectorShell.getFilter();
		// updateFilter();
		// }
		// });
		// } catch (RuntimeException e1) {
		// e1.printStackTrace();
		// }
		// }
		// });
	}

	protected class SortListener implements Listener {
		/**
		 * {@inheritDoc}
		 *
		 * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
		 */
		@Override
		public void handleEvent(final Event e) {
			try {
				// get column that was clicked on
				final TableColumn column = (TableColumn) e.widget;
				int index = 0;
				// are we sorting on the same column?
				boolean changed = false;
				// get column index for column we want to sort on
				for (; index < columns.length; index++) {
					if (column == columns[index]) {
						break;
					}
				}
				// if index is out of bounds, return
				if (index == columns.length) {
					return;
				}
				// if sorting hasn't been done, set the sort column and set
				// initial sort direction to ascending
				if (sortColumnIndex == -1) {
					sortColumnIndex = index;
					historyTable.setSortDirection(SWT.UP);
				}
				// find out if a new column was clicked
				else if (sortColumnIndex != index) {
					columns[sortColumnIndex].setImage(null);
					changed = true;
					sortColumnIndex = index;
				}
				if (changed) {
					historyTable.setSortDirection(SWT.UP);
				} else {
					historyTable.setSortDirection(historyTable
							.getSortDirection() == SWT.UP ? SWT.DOWN : SWT.UP);
				}

				// set the sort column and set the sorting indicator image
				historyTable.setSortColumn(column);
				columns[index]
						.setImage(historyTable.getSortDirection() == SWT.UP ? upImage
								: downImage);

				// sort table items
				sortTableItems(index);
			} catch (final Exception e1) {
				TraceManager.getDefaultTracer(appContext).error(

						"Error handling table sort " + e1.toString());
				e1.printStackTrace();
			}
		}
	};

	protected void sortTableItems(final int index) {
		synchronized (historyTable) {
			// get items
			final TableItem[] items = historyTable.getItems();
			if (items.length <= 1) {
				return; // Only one item so don't sort
			}

			boolean changed = false;
			final TableItemQuickSort aQuickSort = new TableItemQuickSort();
			final int lo0 = 0;

			final boolean ascending = historyTable.getSortDirection() != SWT.DOWN
					&& historyTable.getSortDirection() != SWT.NONE;

			final TableItemQuickSort.CollatorType collatorType = TableItemQuickSort.CollatorType.CHARACTER;

			try {
				final int hi0 = items.length - 1;

				aQuickSort.quickSort(items, lo0, hi0, ascending, index,
						collatorType);
				changed = aQuickSort.wasSwapped();

			} catch (final Exception e) {
				TraceManager.getDefaultTracer(appContext).error(

						"index = " + index + " items.length = " + items.length);
				e.printStackTrace();
			}

			if (changed) {
				replaceTableItems(items);
			}
		}
	}

	private void replaceTableItems(final TableItem[] items) {
		final String[] values = new String[historyTable.getColumnCount()];
		final Color[] colors = new Color[historyTable.getColumnCount()];

		ArrayList<TableItem> newSelected = null;

		for (int i = 0; i < items.length; i++) {
			for (int valIndex = 0; valIndex < values.length; valIndex++) {
				values[valIndex] = items[i].getText(valIndex);
				colors[valIndex] = items[i].getBackground(valIndex);
			}
			final Object def = items[i].getData();
			final Object id = items[i].getData("transmitId");

			final int oldIndex = this.historyTable.indexOf(items[i]);
			final boolean selected = historyTable.isSelected(oldIndex);
			items[i].dispose();
			items[i] = null;

			final TableItem item = new TableItem(historyTable, SWT.NONE);

			item.setData("transmitId", id);
			item.setData(def);
			item.setText(values);

			for (int valIndex = 0; valIndex < values.length; valIndex++) {
				item.setBackground(valIndex, colors[valIndex]);
			}

			if (selected) {
				if (newSelected == null) {
					newSelected = new ArrayList<>(1);
				}
				newSelected.add(item);
			}
		}

		if (newSelected != null) {
			final TableItem[] selectedItems = new TableItem[newSelected.size()];
			newSelected.toArray(selectedItems);
			historyTable.setSelection(selectedItems);
		}
	}

	// public void updateFilter() {
	// //TODO make sure it not only removes, but ADDS items back in that should
	// be added!
	// //need to store a list of all local request history items, even those
	// that aren't currently being displayed
	// //bad, removing while iterating!
	//
	// ArrayList<TableItem> items = new
	// ArrayList<TableItem>(Arrays.asList(historyTable.getItems()));
	//
	// Iterator<TableItem> it = items.iterator();
	//
	//
	// //TODO need to get i
	// // while(it.hasNext()) {
	// // TableItem item = it.next();
	// // if(!historyFilter.accept(item.getText(1))) {
	// // historyTable.remove(i);
	// // }
	// // }
	//
	// for(int i=0; i< historyTable.getItemCount(); i++) {
	// System.out.println("TableItem: " + historyTable.getItem(i).getText(1));
	// if(!historyFilter.accept(historyTable.getItem(i).getText(1))) {
	// historyTable.remove(i);
	// }
	// }
	// }

	private void packColumns() {
		for (final TableColumn tc : columns) {
			tc.pack();
		}
	}

	public void addHistoryMouseListener(final MouseListener listener) {
		this.historyTable.addMouseListener(listener);
	}
	
	/**
	 * Add history table selection listener
	 * @param listener selection listener
	 */
	public void addHistorySelectionListener(final SelectionListener listener) {
		this.historyTable.addSelectionListener(listener);
	}

	@Override
	public void onTransmit(final TransmitEvent event) {
		eventHistory.put(event.hashCode(), event);
	}

	@Override
	public void onTransmit(ITransmittableCommandMessage msg) {
		subscriber.handleMessage(msg);
	}

	private void addRow(final String requestId, final String transmitterName,
			final CommandStatusType status, final String transmitSummary,
			final String timestamp, final int id) {

		// need asyncExec to avoid Invalid Thread Access SWT Exception
		if (!this.isDisposed() && !this.getDisplay().isDisposed()) {
			this.getDisplay().asyncExec(new Runnable() {

				@Override
                public void run() {
					if (!historyTable.isDisposed()) {
						if (tableItemsQueue.size() == MAX_HISTORY_BUFFER_SIZE) {
							final TableEntry entry = tableItemsQueue.remove();

							// search through table for correct row
							final TableItem[] items = historyTable.getItems();
							int rowToBeRemoved = -1;
							// System.out.println("Table length: " +
							// items.length);
							for (int i = 0; i < items.length; i++) {

								int colIndex = 0;
								boolean remove = true;

								if (appContext.getBean(IConnectionMap.class).getFswUplinkConnection()
										.getUplinkConnectionType()
										.equals(UplinkConnectionType.COMMAND_SERVICE)) {
									if (!items[i].getText(colIndex).equals(
											entry.getRequestId())) {
										remove = false;
									}
									colIndex++;
								}

								if (remove
										&& items[i].getText(colIndex).equals(
												entry.getTransmitter())
												&& items[i].getText(++colIndex).equals(
														entry.getStatus().toString())
														&& items[i].getText(++colIndex).equals(
																entry.getCommandString())
																&& items[i].getText(++colIndex).equals(
																		entry.getTimestamp())) {

									rowToBeRemoved = i;

									final int id = Integer.valueOf(historyTable
											.getItem(i).getData("transmitId")
											.toString());
									eventHistory.remove(id);

									break;
								}
							}
							if (rowToBeRemoved != -1) {
								historyTable.remove(rowToBeRemoved);
							}
						}

						tableItemsQueue.add(new TableEntry(requestId,
								transmitterName, status, transmitSummary,
								timestamp));

						final TableItem historyItem = new TableItem(historyTable,
								SWT.NONE);

						historyItem.setForeground(getDisplay().getSystemColor(
								SWT.COLOR_BLACK));

						int colIndex = 0;

						// Removed request ID column per Daniel Hurley's request
						// if (SessionConfiguration.getGlobalInstance()
						// .getUplinkConnectionType()
						// .equals(UplinkConnectionType.COMMAND_SERVICE)) {
						// historyItem
						// .setText(colIndex, checkRequestId(requestId));
						// colIndex++;
						// }
						historyItem.setText(colIndex, transmitterName);
						historyItem.setText(++colIndex, status.name());
						historyItem.setBackground(colIndex,
								colorMapper.getColorForStatus(status));
						historyItem.setText(++colIndex, transmitSummary);
						historyItem.setText(++colIndex, timestamp);

						historyItem.setData("transmitId", id);

						if (sortColumnIndex != -1) {
							sortTableItems(sortColumnIndex);
						}

						// Yes, this looks wrong. It's not. The only way to
						// scroll
						// an
						// SWT window
						// is either to set the top index, which is nearly
						// impossible to
						// calculate,
						// or to set the selection. But we want no selection,
						// because
						// then the
						// UP
						// arrow cannot recall the last command. so we set the
						// selection
						// and
						// then remove
						// it.
						historyTable.setSelection(historyTable.getItemCount() - 1);
						historyTable.deselectAll();
					}
				}

			});
		}
	}

	/**
	 * Gets the selected index in the history table.
	 * @return selected index, or -1 if nothing selected
	 */
	public int getSelectionIndex() {
		return historyTable.getSelectionIndex();
	}

	/**
	 * Returns the number of items in history
	 * 
	 * @return the number of items in history
	 */
	public int getHistoryCount() {
		return this.historyTable.getItemCount();
	}

	/**
     * Gets the selected table item in the history table.
     * @return selected table item
     */
	public TableItem getSelectedHistory() {
		return historyTable.getSelection()[0];
	}

	/**
	 * Get the associated Transmit Event object with its unique ID
	 * 
	 * @param id
	 *            hashcode associated with the Transmit Event object
	 * @return Transmit Event
	 */
	public TransmitEvent getTransmitEvent(final int id) {
		return eventHistory.get(id);
	}

	public void removeAll() {
		eventHistory.clear();
	}

	public void refreshTable() {
		// historyTable.removeAll();
		//
		// for (TransmitEvent e : eventHistory) {
		// AbstractUplinkComposite transmitter = e.getTransmitter();
		// ISendCompositeState state = e.getTransmitState();
		//
		// String transmiterName = transmitter != null &&
		// transmitter.getDisplayName() != null ? transmitter.getDisplayName() :
		// "No Name";
		// String transmitSummary = state != null && state.getTransmitSummary()
		// != null ? state.getTransmitSummary() : "";
		//
		// //addRow(transmiterName, String.valueOf(e.isSuccessful()),
		// transmitSummary, String.valueOf(e.getTimestamp()));
		// }
	}

	/**
	 * CommandMessageSubscriber is the listener for internal messages that are
	 * used to populate this composite.
	 * 
	 * 10/02/17 - handleMessage has been updated to allow sequence directives to be sent via CPD.
	 * 
	 */
	private class CommandMessageSubscriber implements MessageSubscriber {

		public CommandMessageSubscriber() {
			
			final IMessagePublicationBus bus = appContext.getBean(IMessagePublicationBus.class);
			
			bus.subscribe(
					CommandMessageType.FlightSoftwareCommand, this);
			bus.subscribe(
					CommandMessageType.HardwareCommand, this);
			bus.subscribe(
					CommandMessageType.Scmf, this);
			bus.subscribe(
					CommandMessageType.FileLoad, this);
			bus.subscribe(
					CommandMessageType.RawUplinkData, this);
			bus.subscribe(
					CommandMessageType.UplinkStatus, this);
			bus.subscribe(
					CommandMessageType.SseCommand, this);
			// Fix internal CPD uplink status
			bus.subscribe(CommandMessageType.InternalCpdUplinkStatus, this);
			/*  Restore support for sequence directives. */
			bus.subscribe(
					CommandMessageType.SequenceDirective, this);
			bus.subscribe(
					CommandMessageType.FileCfdp, this);
			layout(true);
		}

		@Override
		public void handleMessage(final IMessage message) {

			try {
				if (message instanceof ICpdUplinkMessage) {
					final ICpdUplinkMessage msg = (ICpdUplinkMessage) message;
					if (message.isType(CommandMessageType.FileLoad)) {
						processMessage(msg.getICmdRequestId(),
								FILE_LOAD_TRANSMITTER_NAME,
								msg.getICmdRequestStatus(),
								msg.getOriginalFilename(),
								message.getEventTimeString(),
								((IFileLoadMessage) message)
								.getTransmitEventId());
					} else if (message.isType(CommandMessageType.FlightSoftwareCommand)) {
						processMessage(msg.getICmdRequestId(),
								IMMEDIATE_TRANSMITTER_NAME,
								msg.getICmdRequestStatus(),
								((IFlightCommandMessage) message)
								.getCommandString(),
								message.getEventTimeString(),
								((IFlightCommandMessage) message)
								.getTransmitEventId());
					} else if (message.isType(CommandMessageType.HardwareCommand)) {
						processMessage(msg.getICmdRequestId(),
								IMMEDIATE_TRANSMITTER_NAME,
								msg.getICmdRequestStatus(),
								((IFlightCommandMessage) message)
								.getCommandString(),
								message.getEventTimeString(),
								((IFlightCommandMessage) message)
								.getTransmitEventId());
					} else if (message.isType(CommandMessageType.SequenceDirective)) {
                        processMessage(msg.getICmdRequestId(),
                                IMMEDIATE_TRANSMITTER_NAME,
                                msg.getICmdRequestStatus(),
                                ((IFlightCommandMessage) message)
                                .getCommandString(),
                                message.getEventTimeString(),
                                ((IFlightCommandMessage) message)
                                .getTransmitEventId());
                    } else if (message.isType(CommandMessageType.Scmf)) {
						processMessage(msg.getICmdRequestId(),
								SCMF_TRANSMITTER_NAME,
								msg.getICmdRequestStatus(),
								msg.getScmfFilename(),
								message.getEventTimeString(),
								((ICpdUplinkMessage) message).getTransmitEventId());
					} else if (message.isType(CommandMessageType.RawUplinkData)) {
						final IRawUplinkDataMessage rudm = (IRawUplinkDataMessage) message;
						String transmitterName = "";
						String transmitSummary = rudm.getOriginalFilename();

						if (rudm.isFaultInjected()) {
							transmitterName = FAULT_INJECTOR_TRANSMITTER_NAME;
							transmitSummary = "Raw data from Fault Injector";
						} else {
							transmitterName = RAW_FILE_TRANSMITTER_NAME;
						}

						processMessage(msg.getICmdRequestId(), transmitterName,
								msg.getICmdRequestStatus(), transmitSummary,
								message.getEventTimeString(),
								rudm.getTransmitEventId());
					}
				} else if (message.isType(CommandMessageType.InternalCpdUplinkStatus)) {
					final IInternalCpdUplinkStatusMessage msg = (IInternalCpdUplinkStatusMessage) message;

					final List<ICpdUplinkStatus> statuses = msg.getDeltas();

					for (final ICpdUplinkStatus status : statuses) {

						final ReceivedMessage previouslyReceivedMsg = receivedMessages
								.get(status.getId());
						// If previouslyReceivedMsg is null then this command
						// came from an external
						// client - in this case it should not be displayed in
						// this local chill_up
						// history window
						if (previouslyReceivedMsg == null) {
							continue;
						} else {
							if (!previouslyReceivedMsg.getStatus().equals(
									status.getStatus())) {
								addRow(status.getId(),
										previouslyReceivedMsg.getTransmitter(),
										status.getStatus(),
										previouslyReceivedMsg
										.getCommandString(),
										status.getTimestampString(),
										previouslyReceivedMsg.getTransmitId());
							}
						}
					}
				} else if (message.isType(CommandMessageType.SseCommand) ) {
					final ITransmittableCommandMessage msg = (ITransmittableCommandMessage) message;
					final CommandStatusType status = msg.isSuccessful() ? CommandStatusType.Radiated
							: CommandStatusType.Send_Failure;

					processMessage(null, IMMEDIATE_TRANSMITTER_NAME, status,
							"sse:" + msg.getCommandString(),
							msg.getEventTimeString(), msg.getTransmitEventId());

                  // added case for FileCfdp command messages
				} else if (message.isType(CommandMessageType.FileCfdp)) {
					final ITransmittableCommandMessage msg = (ITransmittableCommandMessage) message;
					final CommandStatusType status = msg.isSuccessful() ? CommandStatusType.Submitted
							: CommandStatusType.Failed;

					processMessage(null, CFDP_TRANSMITTER_NAME, status,
							msg.getCommandString(),
							msg.getEventTimeString(), msg.getTransmitEventId());
				}
				/*
				 * Removed final else if. In R7.7 this referred only to
				 * SequenceDirectiveMessage, but in R8 uses ITransmittableCommandMessage. As of this
				 * JIRA, only SseCommand implements this interface.
				 */

			} catch (final Exception e) {
				e.printStackTrace();
			}
		}

		/**
		 * Sets values for the parameters if they are null, adds a new entry to
		 * the local request history composite and adds the requestId/message
		 * data mapping to a hashmap (this info will be needed for command
		 * status updates)
		 * 
		 * @param requestId
		 *            iCmd key for this command request
		 * @param transmitterName
		 *            command origin (SCMF, Raw Data, Immediate, File Load)
		 * @param status
		 *            iCmd radiation status
		 * @param transmitSummary
		 *            will be the command itself for immediate commands and the
		 *            scmf file for all others
		 * @param timestamp
		 *            time the message was processed, it has already been
		 *            formatted as a string
		 */
		private void processMessage(String requestId, String transmitterName,
				CommandStatusType status, String transmitSummary,
				final String timestamp, final int id) {

			if (requestId != null && !requestId.equals("")
					&& receivedMessages.containsKey(requestId)) {
				return;
			}

			if (requestId == null /* || requestId.equals("") */) {
				requestId = NO_ID;
			}
			if (transmitterName == null) {
				transmitterName = "No transmitter";
			}
			if (status == null) {
				status = CommandStatusType.UNKNOWN;
			}
			if (transmitSummary == null) {
				transmitSummary = "No summary";
			}

			// System.out.println("ID: " + requestId + ",
			// Transmitter: " + transmitterName + ",
			// Status: " + status.toString() + ",
			// Summary: " + transmitSummary + ",
			// Timestamp: " + timestamp);

			addRow(requestId, transmitterName, status, transmitSummary,
					timestamp, id);

			receivedMessages.put(requestId, new ReceivedMessage(status,
					transmitterName, transmitSummary, id));
		}

	}

	/**
	 * Inner class that stores data for previously received ICpdUplinkMessages.
	 * This data is needed when a CpdUplinkStatusesMessage is received so it can
	 * retrieve the transmitter and command with the request ID. //TODO remove
	 * this and use TableEntry?
	 * 
	 *
	 */
	private class ReceivedMessage {
		private final String transmitterName;
		private final String commandString;
		private final CommandStatusType status;
		private final int transmitId;

		/**
		 * Constructor: set the transmitter name and the command string
		 * @param status 
		 * @param transmitterName
		 * @param commandString
		 * @param transmitId 
		 */
		public ReceivedMessage(final CommandStatusType status,
				final String transmitterName, final String commandString, final int transmitId) {
			this.status = status;
			this.transmitterName = transmitterName;
			this.commandString = commandString;
			this.transmitId = transmitId;
		}

		/**
		 * Gets the status
		 * 
		 * @return status
		 */
		public CommandStatusType getStatus() {
			return status;
		}

		/**
		 * Gets the transmitter name
		 * 
		 * @return transmitter name
		 */
		public String getTransmitter() {
			return transmitterName;
		}

		/**
		 * Gets the command string
		 * 
		 * @return command string
		 */
		public String getCommandString() {
			return commandString;
		}

		/**
		 * Gets the transmit ID
		 * 
		 * @return transmit ID
		 */
		public int getTransmitId() {
			return transmitId;
		}
	}

	/**
	 * Inner class that stores data for previously received ICpdUplinkMessages.
	 * This data is needed to keep track of all the table rows in a queue. When
	 * the max-buffer is reached, we want to remove the first item that was
	 * added and always add new entries to the end of the queue
	 * 
	 * Note: I couldn't figure out if there was a guaranteed "key" I could use
	 * to identify a table row, so I store everything.
	 * 
	 *
	 */
	private class TableEntry {
		private final String requestId;
		private final String transmitterName;
		private final CommandStatusType status;
		private final String commandString;
		private final String timestamp;

		/**
		 * Constructor: set the transmitter name and the command string
		 * 
		 * @param transmitterName
		 * @param commandString
		 */
		public TableEntry(final String requestId, final String transmitterName,
				final CommandStatusType status, final String commandString, final String timestamp) {
			this.requestId = requestId;
			this.transmitterName = transmitterName;
			this.status = status;
			this.commandString = commandString;
			this.timestamp = timestamp;
		}

		/**
		 * Gets the request ID
		 * 
		 * @return request ID
		 */
		public String getRequestId() {
			return requestId;
		}

		/**
		 * Gets the transmitter name
		 * 
		 * @return transmitter name
		 */
		public String getTransmitter() {
			return transmitterName;
		}

		/**
		 * Gets the command status
		 * 
		 * @return status type
		 */
		public CommandStatusType getStatus() {
			return status;
		}

		/**
		 * Gets the command string
		 * 
		 * @return command string
		 */
		public String getCommandString() {
			return commandString;
		}

		/**
		 * Gets the time stamp
		 * 
		 * @return time stamp
		 */
		public String getTimestamp() {
			return timestamp;
		}
	}

	/**
	 * Make sure we have something for request id.
	 *
	 * @param requestId
	 *            Initial value
	 *
	 * @return Final value
	 */
	private static String checkRequestId(final String requestId) {
		final String rid = StringUtil.safeTrim(requestId);

		return (!rid.isEmpty() ? rid : NO_ID);
	}
}
