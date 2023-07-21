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
package jpl.gds.monitor.guiapp.gui.views;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.springframework.context.ApplicationContext;

import jpl.gds.message.api.MessageUtility;
import jpl.gds.monitor.config.MonitorGuiProperties;
import jpl.gds.monitor.guiapp.MonitorTimers;
import jpl.gds.monitor.guiapp.common.GeneralFlushListener;
import jpl.gds.monitor.guiapp.common.GeneralMessageDistributor;
import jpl.gds.monitor.guiapp.common.GeneralMessageListener;
import jpl.gds.monitor.guiapp.gui.views.preferences.MessageListPreferencesShell;
import jpl.gds.monitor.guiapp.gui.views.support.CountShell;
import jpl.gds.monitor.perspective.view.MessageListViewConfiguration;
import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.perspective.view.View;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessageConfiguration;
import jpl.gds.shared.message.IMessageType;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.swt.TextViewShell;
import jpl.gds.shared.template.TemplateException;

/**
 * 
 * MessageListComposite is a general purpose widget that displays a table of 
 * one line summaries of selected message types. It is considered a monitor view.
 *
 */
public class MessageListComposite extends AbstractTableViewComposite implements GeneralMessageListener, GeneralFlushListener, View {
    private final Tracer                              trace;
    
    /**
     * Message list composite title
     */
    public static final String TITLE = "Message List";
    
    /**
     * Total number of received messages
     */
    protected long totalReceived;
    
    /**
     * Window for displaying XML message information
     */
    protected TextViewShell textShell;
    
    /**
     * Indicates if scrolling of display is paused
     */
    protected boolean paused;
    
    /**
     * Message list preferences window
     */
    protected MessageListPreferencesShell prefShell;
    
    /**
     * Right click menu option for viewing the message XML
     */
    protected MenuItem viewMenuItem;
    
    /**
     * Queue that contains messages waiting to be processed
     */
    protected final ArrayBlockingQueue<QueuedMessage> messageQueue;
    
    /**
     * Maximum number of rows allowed in tabled
     */
    protected int maxRows = 500;
    
    /**
     * Message list view configuration
     */
    protected MessageListViewConfiguration msgViewConfig;

	private final ApplicationContext appContext;
	
    /**
     * Creates an instance of MessageListComposite.
     * @param appContext the current application context
     * @param config the MessageListViewConfiguration object containing display settings
     */
    public MessageListComposite(final ApplicationContext appContext, final IViewConfiguration config) {
    	super(config);
    	this.appContext = appContext;
        trace = TraceManager.getDefaultTracer(appContext);
        this.msgViewConfig = (MessageListViewConfiguration)config;
        this.maxRows = this.msgViewConfig.getMaxRows();
        this.messageQueue = new ArrayBlockingQueue<QueuedMessage>(this.maxRows * appContext.getBean(MonitorGuiProperties.class).getListQueueScaleFactor());
        setTableDefinition(this.msgViewConfig.getTable(MessageListViewConfiguration.MESSAGE_TABLE_NAME));
        appContext.getBean(MonitorTimers.class).addGeneralFlushListener(this);
        resubscribe();
     }
    
    /**
     * Creates the controls and composites for this tab display.
     */
    @Override
	protected void createGui() {

        this.mainComposite = new Composite(this.parent, SWT.NONE);
        final FormLayout shellLayout = new FormLayout();
        shellLayout.spacing = 5;
        this.mainComposite.setLayout(shellLayout);

        this.table = new Table(this.mainComposite, SWT.FULL_SELECTION | SWT.MULTI);
        final FormData formData2 = new FormData();
        formData2.left = new FormAttachment(0);
        formData2.top = new FormAttachment(0);
        formData2.right = new FormAttachment(100);
        formData2.bottom = new FormAttachment(100);
        this.table.setLayoutData(formData2);
        this.table.setHeaderVisible(this.tableDef.isShowColumnHeader());
        
        updateTableFontAndColors();
        
        final Listener sortListener = new SortListener();
        
        final int numColumns = this.tableDef.getColumnCount();
        this.tableColumns = new TableColumn[numColumns];
        for (int i = 0; i < numColumns; i++) {
            if (this.tableDef.isColumnEnabled(i)) {
                this.tableColumns[i] = new TableColumn(this.table, SWT.NONE);
                this.tableColumns[i].setText(this.tableDef.getOfficialColumnName(i));
                this.tableColumns[i].setWidth(this.tableDef.getColumnWidth(i));
                this.tableColumns[i].addListener(SWT.Selection, sortListener);
                this.tableColumns[i].setMoveable(true);
                if (tableDef.isSortColumn(i) && tableDef.isSortAllowed()) {
					table.setSortColumn(tableColumns[i]);
					tableColumns[i].setImage(tableDef.isSortAscending() ? upImage : downImage);
				}
            } else {
                this.tableColumns[i] = null;
            }
        }
                
        this.table.setColumnOrder(this.tableDef.getColumnOrder());
        final Menu viewMenu = new Menu(this.mainComposite);
        this.viewMenuItem = new MenuItem(viewMenu, SWT.PUSH);
        this.viewMenuItem.setText("View Message...");
        this.table.setMenu(viewMenu);
        this.viewMenuItem.setEnabled(false);
        new MenuItem(viewMenu, SWT.SEPARATOR);
        final MenuItem prefMenuItem =  new MenuItem(viewMenu, SWT.PUSH);
        prefMenuItem.setText("Preferences...");
        new MenuItem(viewMenu, SWT.SEPARATOR);
        final MenuItem pauseMenuItem =  new MenuItem(viewMenu, SWT.PUSH);
        pauseMenuItem.setText("Pause");
        final MenuItem resumeMenuItem =  new MenuItem(viewMenu, SWT.PUSH);
        resumeMenuItem.setText("Resume");
        resumeMenuItem.setEnabled(false);
        final MenuItem clearMenuItem =  new MenuItem(viewMenu, SWT.PUSH);
        clearMenuItem.setText("Clear Data");
        new MenuItem(viewMenu, SWT.SEPARATOR);
        final MenuItem copyMenuItem =  new MenuItem(viewMenu, SWT.PUSH);
        copyMenuItem.setText("Copy");
        copyMenuItem.setEnabled(false);
        new MenuItem(viewMenu, SWT.SEPARATOR);
        final MenuItem viewCountMenuItem = new MenuItem(viewMenu, SWT.PUSH);
        viewCountMenuItem.setText("View Count");
        
        copyMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    final int[] indices = MessageListComposite.this.table.getSelectionIndices();
                    if (indices == null || indices.length == 0) {
                        return;
                    }   
                    Arrays.sort(indices);
                    Clipboard clipboard = new Clipboard(MessageListComposite.this.mainComposite.getDisplay());
                    final StringBuffer plainText = new StringBuffer();
                    for (int i = 0; i < indices.length; i++) {
                        final TableItem item = MessageListComposite.this.table.getItem(indices[i]);
                        for (int j = 0; j < MessageListComposite.this.table.getColumnCount(); j++) {
                            plainText.append("\"" + item.getText(j) + "\"");
                            if (j < MessageListComposite.this.table.getColumnCount() - 1) {
                                plainText.append(",");
                            }
                        }
                        plainText.append("\n");
                    }
                    final TextTransfer textTransfer = TextTransfer.getInstance();
                    clipboard.setContents(new String[]{plainText.toString()}, new Transfer[]{textTransfer});
                    clipboard.dispose();
                    clipboard = null;
                } catch (final Exception e1) {
                    trace.error("Unable to handle copy menu item " + e1.toString());
                    e1.printStackTrace();
                }
            }
        });
        clearMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    clearView();
                } catch (final RuntimeException e1) {
                    trace.error("Unable to handle clear menu item " + e1.toString());
                    e1.printStackTrace();
                }
            }
        });
        
        pauseMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    pause();
                    resumeMenuItem.setEnabled(true);
                    pauseMenuItem.setEnabled(false);
                } catch (final Exception e1) {
                    e1.printStackTrace();
                    trace.error("Unable to handle pause menu item " + e1.toString());
                }
            }
        });
        
        resumeMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    resume();
                    resumeMenuItem.setEnabled(false);
                    pauseMenuItem.setEnabled(true);
                } catch (final Exception e1) {
                    trace.error("Unable to handle resume menu item " + e1.toString());
                    e1.printStackTrace();
                }
            }
        });
        
        prefMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    if (MessageListComposite.this.prefShell == null) { 
                    	// This kludge works around an SWT bug on Linux
                    	// in which column sizes are not remembered
                    	final TableColumn[] cols = MessageListComposite.this.table.getColumns();
                    	for (int i = 0; i < cols.length; i++) {
                    		cols[i].setWidth(cols[i].getWidth());
                    	}
                        MessageListComposite.this.prefShell = new MessageListPreferencesShell(appContext, MessageListComposite.this.mainComposite.getShell());
                        MessageListComposite.this.prefShell.setValuesFromViewConfiguration(MessageListComposite.this.msgViewConfig);
                        MessageListComposite.this.prefShell.getShell().addDisposeListener(new DisposeListener() {
                            @Override
                            public void widgetDisposed(final DisposeEvent event) {
                                try {
                                    if (!MessageListComposite.this.prefShell.wasCanceled()) {

                                    	cancelOldSortColumn();

                                    	MessageListComposite.this.prefShell.getValuesIntoViewConfiguration(MessageListComposite.this.msgViewConfig);
                                    	MessageListComposite.this.maxRows = MessageListComposite.this.msgViewConfig.getMaxRows();
                                    	
                                    	updateTableFromConfig(MessageListViewConfiguration.MESSAGE_TABLE_NAME, MessageListComposite.this.prefShell.needColumnChange());

                                    	resubscribe();
                                    }
                                } catch (final Exception e) {
                                    trace.error("Unable to handle preferences window exit " + e.toString());
                                    e.printStackTrace();
                                } finally {
                                    MessageListComposite.this.prefShell = null;
                                    prefMenuItem.setEnabled(true);
                                }
                            }
                        });
                        prefMenuItem.setEnabled(false);
                        MessageListComposite.this.prefShell.open();
                    }
                } catch (final Exception e1) {
                    trace.error("Unable to handle preferences window exit " + e1.toString());
                    e1.printStackTrace();
                }
            }
        });
        
        viewCountMenuItem.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
        	    try {
        	        final CountShell tvs = new CountShell(table.getShell(), MessageListComposite.this, viewConfig.getViewName());
        	        tvs.open();
        	    } catch (final Exception ex) {
        	        ex.printStackTrace();
                    trace.error("Error in showing Message List count" + ex.toString());
        	    }
        	}
        });
        
        this.table.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    final int[] i = MessageListComposite.this.table.getSelectionIndices();
                    if (i != null && i.length != 0) {
                        copyMenuItem.setEnabled(true);
                    } else {
                        copyMenuItem.setEnabled(false);
                    }
                    if (i != null && i.length == 1) {
                        MessageListComposite.this.viewMenuItem.setEnabled(true);
                    } else {
                        MessageListComposite.this.viewMenuItem.setEnabled(false);
                    }
                } catch (final RuntimeException e1) {
                    e1.printStackTrace();
                    trace.error("Unable to handle message table selection event " + e1.toString());
                }
            }
        });
        
        this.viewMenuItem.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(final SelectionEvent arg0) {
                try {
                    final int i = MessageListComposite.this.table.getSelectionIndex();
                    if (i != -1) {
                        if (MessageListComposite.this.textShell == null || MessageListComposite.this.textShell.getShell().isDisposed()) {
                            MessageListComposite.this.textShell = new TextViewShell(MessageListComposite.this.mainComposite.getShell(), trace);
                        }
                        final TableItem it = MessageListComposite.this.table.getItem(i);
                        MessageListComposite.this.textShell.getShell().setText(it.getText(MessageListComposite.this.tableDef.getActualIndex(MessageListComposite.this.tableDef.getColumnIndex(
                                MessageListViewConfiguration.TYPE_COLUMN))) + " Message");
                        final jpl.gds.shared.message.IMessage m = (jpl.gds.shared.message.IMessage)it.getData();
                        if (m != null) {
                            try {
                                final String msgText = MessageUtility.getMessageText(m, "Xml");
                                MessageListComposite.this.textShell.setText(msgText);
                            } catch (final TemplateException e) {
                                MessageListComposite.this.textShell.setText("Unable to format message.");   
                            }
                            MessageListComposite.this.textShell.open();
                        }
                    }
                } catch (final Exception e) {
                    trace.error("Unable to handle message table selection event " + e.toString());
                    e.printStackTrace();
                }
            }

            @Override
            public void widgetDefaultSelected(final SelectionEvent arg0) {
            }

        });
        
        this.mainComposite.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent event) {
                try {
                	appContext.getBean(MonitorTimers.class).removeGeneralFlushListener(MessageListComposite.this);
                   appContext.getBean(GeneralMessageDistributor.class).removeDataListener(MessageListComposite.this);
                   if (MessageListComposite.this.prefShell != null) {
                        MessageListComposite.this.prefShell.getShell().dispose();
                        prefShell = null;
                    }
                 } catch (final Exception e) {
                    trace.error("Unable to handle message main shell dispose event " + e.toString());
                    e.printStackTrace();
                }
            }
        });
        
        setSortColumn();

     }  
    


    private TableItem addNewItem(final QueuedMessage qm) {
    	TableItem item = null;
    	try {
    		synchronized(this.table) {

    			final String type = qm.type;
    			final String time = qm.receiveTime;
    			String message = null;

    			try {
    				message = qm.message.getOneLineSummary();
    			} catch (final Exception e) {
    				message = qm.message.toString();
    			}

    			item = createTableItem(new String[] {
    					type, time, message});

    			item.setData(qm.message);
    		}
    	} catch (final Exception e) {
    		trace.error("Unable to handle message display event " + e.toString());
    		e.printStackTrace();
    	}
        return item;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.monitor.guiapp.common.GeneralMessageListener#messageReceived(jpl.gds.shared.message.IMessage[])
     */
    @Override
    public void messageReceived(final IMessage[] msgs) {    
        try {
            trace.debug("Message List queue length in messageReceived is " + 
                    this.messageQueue.size());

            for (int i = 0; i < msgs.length; i++) {
            	
            	/*
        		 * This check was previously not needed
        		 * because this GUI composite subscribes only to messages it wants
        		 * from the general distributor. But now the "Status" GUI view shows 
        		 * internal log messages as well, which do not come from the 
        		 * general distributor, but rather from the message context. So this
        		 * check filters them out if the user has set the preferences to do
        		 * so.
        		 */
            	if (!this.isSelectedType(msgs[i].getType())) {
            		continue;
            	}
            	
                ++this.totalReceived;
                
                final QueuedMessage qm = new QueuedMessage(msgs[i], 
                        msgs[i].getEventTimeString(), msgs[i].getType().getSubscriptionTag());
                while (!this.messageQueue.offer(qm, 100, TimeUnit.MILLISECONDS)) {
                    displayMessages(false);
                    trace.debug("Message List is waiting for offer");
                }
                trace.debug("Message List is done with offer");
            }

            displayMessages(false);

        } catch (final Exception e) {
            e.printStackTrace();
        }
    } 
    
    /**
     * Displays queued messages.
     * @param timedFlush true if this display operation was invoked by the
     * flush timer.
     */
    protected synchronized void displayMessages(final boolean timedFlush) {

        if (this.parent == null) {
            return;
        }
        if (this.parent.isDisposed() || this.parent.getDisplay().isDisposed()) {
            return;
        }
        this.parent.getDisplay().asyncExec(new Runnable () {
        	
    		@Override
			public String toString() {
				return "MessageListComposite.displayMessages.Runnable";
			}
    		
            @Override
            public void run () {
                try {
                    if (MessageListComposite.this.mainComposite.isDisposed()) {
                    	messageQueue.clear();
                        return;
                    }

                    if (MessageListComposite.this.messageQueue.size() == 0) {
                        return;
                    }

                    if (MessageListComposite.this.paused) {
                        return;
                    }

                    TableItem lastItem = null;
                    
                    // If we are not visible, we do not update the table, and there is
                    // no point in keeping more than max rows messages to display when
                    // we become visible. Throw away any extra.
                    if (!MessageListComposite.this.mainComposite.isVisible()) {
                        trace.debug("Message List is throwing away messages " + 
                                String.valueOf(MessageListComposite.this.messageQueue.size() - MessageListComposite.this.maxRows));
                        
                        if (MessageListComposite.this.messageQueue.size() > MessageListComposite.this.maxRows) {

                        	while (MessageListComposite.this.messageQueue.size() > MessageListComposite.this.maxRows) {
                        		MessageListComposite.this.messageQueue.poll(10, TimeUnit.MILLISECONDS);
                        	}
                        	return;
                        }
                    }

                    final int messagesInQueue = MessageListComposite.this.messageQueue.size();
                    int addNewRows = 0;
                    int removeCount = 0;
                    trace.debug("Message list is waiting on table lock");

                    synchronized(MessageListComposite.this.table) {
                    	trace.debug("Message list is done waiting on table lock");
                    	// If there are more messages queued than the maximum number of display
                    	// rows, we'll remove everything from the table and display
                    	// max rows new ones, starting with the oldest.
                    	//
                    	// If there are fewer messages queue than the maximum number to display,
                    	// we'll remove enough rows from the table to make room for them and
                    	// display all new messages, starting with the oldest
                    	if (messagesInQueue > MessageListComposite.this.maxRows) {
                    		removeCount = MessageListComposite.this.table.getItemCount();
                    		trace.debug("Message List is clearing messages from displayList " + removeCount);
                    		MessageListComposite.this.clearView();
                    		addNewRows = MessageListComposite.this.maxRows;
                    	} else {
                    		if (MessageListComposite.this.table.getItemCount() + messagesInQueue > MessageListComposite.this.maxRows) {
                    			final int availRows = MessageListComposite.this.maxRows - MessageListComposite.this.table.getItemCount();
                    			if (availRows < messagesInQueue) {
                    				removeCount = messagesInQueue - availRows;
                    				if (removeCount > MessageListComposite.this.table.getItemCount()) {
                    					removeCount = MessageListComposite.this.table.getItemCount();
                    				}
                    			}
                    			MessageListComposite.this.removeOldestEntries(removeCount);
                     		}
                    		addNewRows = Math.min(messagesInQueue, MessageListComposite.this.maxRows - MessageListComposite.this.table.getItemCount());
                    	}
                    	trace.debug("Message list is adding up to " + addNewRows + " messages");
                    	for (int i = 0; i < addNewRows && MessageListComposite.this.messageQueue.size() > 0; i++) {
                    		final QueuedMessage m = MessageListComposite.this.messageQueue.poll(10, TimeUnit.MILLISECONDS);
                    		if (m != null) {
                    			lastItem = addNewItem(m);
                    		}

                    	}
                    	if (lastItem != null) {
                    		MessageListComposite.this.table.showItem(lastItem);
                    	}
                    }
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

          
    /**
     * Pauses the scrolling of the message display.
     *
     */
    protected void pause() {
        this.paused = true;
    }
    
    /**
     * Resumes the scrolling of the message display.
     */
    protected void resume() {
        this.paused = false;
    }
    
    /**
     * 
     * QueuedMessage represents an internal message that has been
     * queued for display.
     */
    public class QueuedMessage {
        IMessage message;
        String receiveTime;
        String type;
        
        /**
         * Queue message constructor
         * 
         * @param msgs Message that will be queued
         * @param time Time message was received
         * @param typeStr message type string
         */
        public QueuedMessage(final IMessage msgs, final String time, 
                final String typeStr) {
            this.message=msgs;
            this.receiveTime=time;
            this.type=typeStr;
         }
    }
    
    /**
     * Determines if a message type is on the list of message types the
     * user has configured to be of interest.
     * @param type the message type 
     * @return true if the user elected to view messages of this type
     */
    protected boolean isSelectedType(final IMessageType type) {
        final String[] selectedTypes = this.msgViewConfig.getMessageTypes();
        if (selectedTypes == null) {
            return true;
        }
        final IMessageConfiguration mc = MessageRegistry.getMessageConfig(type);
        for (int i = 0; i < selectedTypes.length; i++) {
            if (mc.isTagAliasFor(selectedTypes[i])) {
                return true;
            }
        }
        return false;
    }
    
    private void resubscribe() {
        String[] types = this.msgViewConfig.getMessageTypes();
        if (types == null) {
            types = MessageRegistry.getAllSubscriptionTags(true).toArray(new String[] {});
        }
        final List<IMessageType> configs = new ArrayList<>(types.length);
        for (int i = 0; i < types.length; i++) {
            final IMessageConfiguration mc = MessageRegistry.getMessageConfig(types[i]);
            if (mc == null) {
                trace.warn("Unrecognized message type found in perspective for message view: " 
                    + types[i] + ". Ignored.");
            } else {
                configs.add(mc.getMessageType());
            }
        }
        appContext.getBean(GeneralMessageDistributor.class).
            replaceDataListeners(this, configs.toArray(new IMessageType[configs.size()]));
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.monitor.guiapp.common.GeneralFlushListener#flushTimerFired()
     */
    @Override
    public void flushTimerFired() {
        displayMessages(true);
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.perspective.view.View#getDefaultName()
     */
    @Override
    public String getDefaultName() {
        return TITLE;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.monitor.guiapp.gui.views.AbstractTableViewComposite#updateViewConfig()
     */
    @Override
	public void updateViewConfig() {
    	super.updateViewConfig();
        this.msgViewConfig.setMaxRows(this.maxRows);
    }

    @Override
	protected void replaceTableItems(final TableItem[] items) {
    	final String[] values = new String[this.tableDef.getActualColumnCount()];
    	
      	ArrayList<TableItem> newSelected = null;
      	
    	for (int i = 0; i < items.length; i++) {
    		for (int valIndex = 0; valIndex < values.length; valIndex++) {
    			values[valIndex] = items[i].getText(valIndex);
    		}
    		final Object def = items[i].getData();
    		
     		final int oldIndex = this.table.indexOf(items[i]);
    		final boolean selected = table.isSelected(oldIndex);
    		
    		final long oldTimestamp = getEntryTimestamp(items[i]);
			removeEntry(items[i]);
			items[i] = null;

			final TableItem item = createTableItem(oldTimestamp);

    		item.setData(def);
    		item.setText(values);
    		
    		if (selected) {
    			if (newSelected == null) {
    				newSelected = new ArrayList<TableItem>(1);
    			}
    			newSelected.add(item);
    		}
    	}
    	
    	if (newSelected != null) {
    		final TableItem[] selectedItems = new TableItem[newSelected.size()];	
    		newSelected.toArray(selectedItems);
    		table.setSelection(selectedItems);
    	}
    }
}


