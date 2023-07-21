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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
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

import jpl.gds.context.api.IContextIdentification;
import jpl.gds.monitor.guiapp.common.GeneralMessageDistributor;
import jpl.gds.monitor.guiapp.common.GeneralMessageListener;
import jpl.gds.monitor.guiapp.gui.views.preferences.EncodingWatchPreferencesShell;
import jpl.gds.monitor.guiapp.gui.views.support.BadFrameShell;
import jpl.gds.monitor.perspective.view.EncodingViewConfiguration;
import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.perspective.view.View;
import jpl.gds.shared.formatting.SprintfFormat;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.swt.TextViewShell;
import jpl.gds.shared.time.TimeUtility;
import jpl.gds.tm.service.api.TmServiceMessageType;
import jpl.gds.tm.service.api.frame.EncodingSummaryRecord;
import jpl.gds.tm.service.api.frame.IFrameEventMessage;
import jpl.gds.tm.service.api.frame.IFrameSummaryMessage;

/**
 * EncodingWatchComposite is the GUI widget responsible for displaying a scrolling table
 * of Frame Summary encoding messages. It is considered a monitor view.
 *
 */
public class EncodingWatchComposite extends AbstractTableViewComposite implements GeneralMessageListener, View {
    /**
     * Default tracer
     */
    public final Tracer                                 tracer;
    
    /**
     * Encoding watch composite title
     */
    public static final String TITLE = "Encoding Watch";

    /**
     * Indicates if the data flow in this composite has been paused
     */
    protected boolean paused;
    
    /**
     * Text window that displays encoding watch XML message
     */
    protected TextViewShell textShell;
    
    /**
     * Encoding watch preferences window
     */
    protected EncodingWatchPreferencesShell prefShell;
    
    /**
     * Date formatter
     */
    protected DateFormat dateFormat = TimeUtility.getFormatter();
    
    /**
     * Encoding view configuration object
     */
    protected EncodingViewConfiguration encodingViewConfig;
    private final Map<String,TableItem> encodingMap = new HashMap<>();
    private final Map<String, List<IFrameEventMessage>> messages = new HashMap<>();

	private final ApplicationContext appContext;

	private final SprintfFormat format;
    
    /**
     * Creates an instance of EncodingWatchComposite.
     * @param appContext the current application context
     * @param config the EncodingViewConfiguration object containing display settings
     */
    public EncodingWatchComposite(final ApplicationContext appContext, final IViewConfiguration config) {
    	super(config);
    	this.appContext = appContext;
        tracer = TraceManager.getDefaultTracer(appContext);
        format = new SprintfFormat(appContext.getBean(IContextIdentification.class).getSpacecraftId());
        this.encodingViewConfig = (EncodingViewConfiguration)config;
        setTableDefinition(this.encodingViewConfig.getTable(EncodingViewConfiguration.ENCODING_TABLE_NAME));
     }

    /**
     * @{inheritDoc}
     * @see jpl.gds.monitor.guiapp.gui.views.AbstractTableViewComposite#init(org.eclipse.swt.widgets.Composite)
     */
    @Override
	public void init(final Composite parent) {
        super.init(parent);
        final GeneralMessageDistributor gmd = appContext.getBean(GeneralMessageDistributor.class);
        gmd.addDataListener(this, TmServiceMessageType.TelemetryFrameSummary);
        gmd.addDataListener(this, TmServiceMessageType.BadTelemetryFrame);
        setSortColumn();
    }

    /**
     * Creates the controls and composites for this display.
     */
    @Override
	protected void createGui() {

        this.mainComposite = new Composite(this.parent, SWT.NONE);
        final FormLayout shellLayout = new FormLayout();
        shellLayout.spacing = 5;
        this.mainComposite.setLayout(shellLayout);
       
        this.table = new Table(this.mainComposite, SWT.MULTI | SWT.FULL_SELECTION);
        this.table.setHeaderVisible(this.tableDef.isShowColumnHeader());

        final FormData formData2 = new FormData();
        formData2.left = new FormAttachment(0);
        formData2.top = new FormAttachment(0);
        formData2.right = new FormAttachment(100);
        formData2.bottom = new FormAttachment(100);
        this.table.setLayoutData(formData2);
        updateTableFontAndColors();
        this.mainComposite.setBackground(this.background);
        this.mainComposite.setForeground(this.foreground);
        final Listener sortListener = new SortListener();

        final int numColumns = this.tableDef.getColumnCount();
        this.tableColumns = new TableColumn[numColumns];
        for (int i = 0; i < numColumns; i++) {
            if (this.tableDef.isColumnEnabled(i)) {
                final int align = SWT.LEFT;
                this.tableColumns[i] = new TableColumn(this.table, align);
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
        this.table.setMenu(viewMenu);
        final MenuItem prefMenuItem =  new MenuItem(viewMenu, SWT.PUSH);
        prefMenuItem.setText("Preferences...");
        new MenuItem(viewMenu, SWT.SEPARATOR);
        final MenuItem badMenuItem =  new MenuItem(viewMenu, SWT.PUSH);
        badMenuItem.setText("Show Bad Frames...");
        new MenuItem(viewMenu, SWT.SEPARATOR);
        final MenuItem clearMenuItem =  new MenuItem(viewMenu, SWT.PUSH);
        clearMenuItem.setText("Clear Data");
        new MenuItem(viewMenu, SWT.SEPARATOR);
        final MenuItem copyMenuItem =  new MenuItem(viewMenu, SWT.PUSH);
        copyMenuItem.setText("Copy");
        copyMenuItem.setEnabled(false);

        copyMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    final int[] indices = EncodingWatchComposite.this.table.getSelectionIndices();
                    if (indices == null || indices.length == 0) {
                        return;
                    }
                    Arrays.sort(indices);
                    final Clipboard clipboard = new Clipboard(EncodingWatchComposite.this.mainComposite.getDisplay());
                    final StringBuilder plainText = new StringBuilder();
                    for (int i = 0; i < indices.length; i++) {
                        final TableItem item = EncodingWatchComposite.this.table.getItem(indices[i]);
                        for (int j = 0; j < EncodingWatchComposite.this.table.getColumnCount(); j++) {
                            plainText.append("\"" + item.getText(j) + "\"");
                            if (j < EncodingWatchComposite.this.table.getColumnCount() - 1) {
                                plainText.append(",");
                            }
                        }
                        plainText.append("\n");
                    }
                    final TextTransfer textTransfer = TextTransfer.getInstance();
                    clipboard.setContents(new String[]{plainText.toString()}, new Transfer[]{textTransfer});
                    clipboard.dispose();
                } catch (final Exception e1) {
                    tracer.error("Unable to handle Copy menu item " + e1.toString(), e1);
                }
            }
        });

        clearMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    clearView();
                } catch (final Exception e1) {
                    tracer.error("Unable to handle Clear menu item " + e1.toString(), e1);
                }
            }
        });
        
        badMenuItem.addSelectionListener(new SelectionAdapter() {
	    	 @Override
	    	 public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
	    		 try {

	    			 final TableItem[] selection = table.getSelection();
						if (selection.length != 1) {
							return;
				     }
					 final TableItem item = selection[0];
                     final String key = (String)item.getData("key");
					 final List<IFrameEventMessage> badFrames = messages.get(key);
					 if  (badFrames == null || badFrames.isEmpty()) {
	    				 SWTUtilities.showMessageDialog(mainComposite.getShell(), "No Bad Frames", "There are no bad frames in the current history.");
	    				 return;
	    			 }
                    final BadFrameShell shell = new BadFrameShell(mainComposite.getShell(),
                                                                  appContext.getBean(SseContextFlag.class));
	    			 shell.setBadFrameList(badFrames);
	    			 badMenuItem.setEnabled(false);
	    			 shell.open();
	    			 shell.getShell().addDisposeListener(new DisposeListener() {

	    				 @Override
						public void widgetDisposed(
	    						 final DisposeEvent event) {
	    					 badMenuItem.setEnabled(true);
	    				 }
	    			 });
	    		 } catch (final Exception ex) {
	    			 tracer.error("Error in view bad frames menu item handling " + ex.toString(), ex);
	    		 }
	    	 }
	     });

        prefMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    if (EncodingWatchComposite.this.prefShell == null) {
                    	// This kludge works around an SWT bug on Linux
                    	// in which column sizes are not remembered
                    	final TableColumn[] cols = EncodingWatchComposite.this.table.getColumns();
                    	for (int i = 0; i < cols.length; i++) {
                    		cols[i].setWidth(cols[i].getWidth());
                    	}
                        EncodingWatchComposite.this.prefShell = new EncodingWatchPreferencesShell(appContext, EncodingWatchComposite.this.mainComposite.getShell());
                        EncodingWatchComposite.this.prefShell.setValuesFromViewConfiguration(EncodingWatchComposite.this.encodingViewConfig);
                        
                        EncodingWatchComposite.this.prefShell.getShell().addDisposeListener(new DisposeListener() {
                            @Override
							public void widgetDisposed(final DisposeEvent event) {
                                try {
                                    if (!EncodingWatchComposite.this.prefShell.wasCanceled()) {
                                    	
                                    	cancelOldSortColumn();

                                        EncodingWatchComposite.this.prefShell.getValuesIntoViewConfiguration(EncodingWatchComposite.this.encodingViewConfig);
                                        
										updateTableFromConfig(EncodingViewConfiguration.ENCODING_TABLE_NAME, EncodingWatchComposite.this.prefShell.needColumnChange());

										EncodingWatchComposite.this.mainComposite.setBackground(background);
                                        EncodingWatchComposite.this.mainComposite.setForeground(foreground);
                                        
                                    }
                                } catch (final RuntimeException e) {
                                    tracer.error("Unable to handle exit from preferences window " + e.toString(), e);
                                } finally {
                                    EncodingWatchComposite.this.prefShell = null;
                                    prefMenuItem.setEnabled(true);
                                }
                            }
                        });
                        prefMenuItem.setEnabled(false);
                        EncodingWatchComposite.this.prefShell.open();
                    }
                } catch (final Exception e1) {
                    tracer.error("Unable to handle Preferences menu item" + e1.toString(), e1);
                }
            }
        });

        this.mainComposite.addDisposeListener(new DisposeListener() {
            @Override
			public void widgetDisposed(final DisposeEvent event) {
                try {
                	appContext.getBean(GeneralMessageDistributor.class).removeDataListener(EncodingWatchComposite.this);
                	if (EncodingWatchComposite.this.prefShell != null) {
                		EncodingWatchComposite.this.prefShell.getShell().dispose();
                		prefShell = null;
                	}
                } catch (final Exception e) {
                    tracer.error("Unable to handle Frame watch main shell disposal " + e.toString(), e);
                }
            }
        });

        this.table.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    final int[] i = EncodingWatchComposite.this.table.getSelectionIndices();
                    if (i != null && i.length != 0) {
                        copyMenuItem.setEnabled(true);
                        if (i.length == 1) {
                        	badMenuItem.setEnabled(true);
                        } else {
                        	badMenuItem.setEnabled(false);
                        }
                    } else {
                        copyMenuItem.setEnabled(false);
                        badMenuItem.setEnabled(false);
                    }
                } catch (final RuntimeException e1) {
                    tracer.error("Unable to handle Frame table selection event " + e1.toString(), e1);
                }
            }
        });
    }

    
    /**
     * Creates an Encoding Summary table row upon receipt of a frame sync 
     * summary message, as long as the composite is not disposed
     * 
     * @param msg Frame Sync Summary message
     */
    public void displayMessage(final IFrameSummaryMessage msg) {
        if (this.parent.isDisposed()) {
            return;
        }
        this.parent.getDisplay().asyncExec(new Runnable () {
        	
    		@Override
			public String toString() {
				return "EncodingWatchComposite.displayMessage.Runnable";
			}
            @Override
			public void run () {
                try {
                    if (EncodingWatchComposite.this.mainComposite.isDisposed()) {
                        return;
                    }
                    tracer.trace("Encoding watch is processing message"); 
                    
                    boolean needSort = false;
                    synchronized(EncodingWatchComposite.this.table) {
                    	final Map<String, EncodingSummaryRecord> sumMap = msg.getEncodingSummaryMap();
                       	if (sumMap == null || sumMap.size() == 0) {
                    		return;
                    	}
                    	final Collection<EncodingSummaryRecord> sums = sumMap.values();
                    	
                    	for (final EncodingSummaryRecord sum: sums) {

                            final String count = String.valueOf(sum.getInstanceCount());
                            final String ert = sum.getLastErtStr();
                            final String encodingType = sum.getType().toString();
                            final long vcid = sum.getVcid();
                            final String seq = String.valueOf(sum.getLastSequence());
                            final String badCount = String.valueOf(sum.getBadFrameCount());
                            final String errorCount = String.valueOf(sum.getErrorCount());
                            
                            final String key = format.anCsprintf("%03d", vcid) + "/" + format.anCsprintf("%s", encodingType);
                            
                            TableItem item = encodingMap.get(key);
                            
                            if (item == null) {
                                item = createTableItem(new String[] {key, count, badCount, errorCount, seq, ert});
                            	item.setData(key);
                            	encodingMap.put(key, item);
                                item.setData("key", key);
                            } else {
                                setColumn(item, tableDef.getColumnIndex(EncodingViewConfiguration.ENCODING_COUNT_COLUMN), count);
                                setColumn(item, tableDef.getColumnIndex(EncodingViewConfiguration.ENCODING_ERT_COLUMN), ert);
                                setColumn(item, tableDef.getColumnIndex(EncodingViewConfiguration.ENCODING_SEQ_COLUMN), seq);
                                setColumn(item, tableDef.getColumnIndex(EncodingViewConfiguration.ENCODING_BAD_COUNT_COLUMN), badCount);
                                setColumn(item, tableDef.getColumnIndex(EncodingViewConfiguration.ENCODING_ERROR_COUNT_COLUMN), errorCount);
                            	needSort = true;
                            }
                
                        }
                        
                        if (needSort && EncodingWatchComposite.this.table.getSortColumn() != null) {
                        	if (!tableDef.getSortColumn().equals(EncodingViewConfiguration.ENCODING_VCID_TYPE_COLUMN)) {
                        		final int index = EncodingWatchComposite.this.tableDef.getColumnIndex(EncodingWatchComposite.this.tableDef.getSortColumn());
                        		sortTableItems(index);
                        	}
                        }
                    }
                } catch (final Exception e) {
                    tracer.error("Unexpected error displaying message: " + e.toString(), e);
                }
            }
        });
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.monitor.guiapp.common.GeneralMessageListener#messageReceived(jpl.gds.shared.message.IMessage[])
     */
    @Override
	public void messageReceived(final IMessage[] messages) {
        try {
            if (messages.length == 0) {
                return;
            }
            IFrameSummaryMessage last = null;
            for (int i = 0; i < messages.length; i++) {
            	if (messages[i].isType(TmServiceMessageType.BadTelemetryFrame)) {
            		final IFrameEventMessage bad = (IFrameEventMessage)messages[i];
            		final int vcid = bad.getFrameInfo().getVcid();
            		final String encoding = bad.getFrameInfo().getFrameFormat().getEncoding().toString();
                    final String key = format.anCsprintf("%03d", vcid) + "/" + format.anCsprintf("%s", encoding);
                    List<IFrameEventMessage> list = this.messages.get(key);
                    if (list == null) {
                    	list = new ArrayList<>(1);
                        this.messages.put(key, list);
                    }
                    list.add(bad);

            	} else {
            		last = (IFrameSummaryMessage)messages[i];
            	}
            }
            /* We only need the last message, since each message contains the total frame state. */
            if (last != null) {
            	displayMessage(last);
            }

        } catch (final Exception e) {
            tracer.error("Unexpected error processing message: " + e.toString(), e);
        }
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.perspective.view.View#clearView()
     */
    @Override
	public void clearView() {
    	super.clearView();
        synchronized(this.table) {
            this.encodingMap.clear();
            this.messages.clear();
        }
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
     * {@inheritDoc}
     * @see jpl.gds.monitor.guiapp.gui.views.AbstractTableViewComposite#replaceTableItems(org.eclipse.swt.widgets.TableItem[])
     */
	@Override
	protected void replaceTableItems(final TableItem[] items) {
    	final String[] values = new String[this.tableDef.getActualColumnCount()];
    	ArrayList<TableItem> newSelected = null;
    	
    	for (int i = 0; i < items.length; i++) {            

    		for (int valIndex = 0; valIndex < values.length; valIndex++) {
    			values[valIndex] = items[i].getText(valIndex);
    		}

    		final String key = (String)items [ i ].getData();
    		this.encodingMap.remove ( key );

    		final int oldIndex = this.table.indexOf(items[i]);
    		final boolean selected = table.isSelected(oldIndex);
   		
    		final long oldTimestamp = getEntryTimestamp(items[i]);
			removeEntry(items[i]);
			items[i] = null;

    		final TableItem item = createTableItem(oldTimestamp);
    		item.setData ( key );
    		item.setText ( values );

    		if (selected) {
    			if (newSelected == null) {
    				newSelected = new ArrayList<>(1);
    			}
    			newSelected.add(item);
    		}

    		this.encodingMap.put ( key, item );
    	} // end for

    	if (newSelected != null) {
    		final TableItem[] selectedItems = new TableItem[newSelected.size()];	
    		newSelected.toArray(selectedItems);
    		table.setSelection(selectedItems);
    	}
    }
}
