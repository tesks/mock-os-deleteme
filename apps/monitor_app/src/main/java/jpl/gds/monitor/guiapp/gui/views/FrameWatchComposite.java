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
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.springframework.context.ApplicationContext;

import jpl.gds.context.api.IContextIdentification;
import jpl.gds.db.api.sql.IDbTableNames;
import jpl.gds.monitor.guiapp.common.GeneralMessageDistributor;
import jpl.gds.monitor.guiapp.common.GeneralMessageListener;
import jpl.gds.monitor.guiapp.gui.views.preferences.FrameWatchPreferencesShell;
import jpl.gds.monitor.perspective.view.FrameWatchViewConfiguration;
import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.perspective.view.View;
import jpl.gds.shared.formatting.SprintfFormat;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.swt.TextViewShell;
import jpl.gds.shared.time.TimeUtility;
import jpl.gds.tm.service.api.TmServiceMessageType;
import jpl.gds.tm.service.api.frame.FrameSummaryRecord;
import jpl.gds.tm.service.api.frame.IFrameSummaryMessage;

/**
 * FrameWatchComposite is the GUI widget responsible for displaying a scrolling table
 * of Frame Summary messages. It is considered a monitor view.
 *
 */
public class FrameWatchComposite extends AbstractTableViewComposite implements GeneralMessageListener, View {
    /**
     * Default tracer
     */
    public final Tracer                      tracer;
    
    /**
     * Frame watch composite title
     */
    public static final String TITLE = "Frame Watch";

    /**
     * Indicates if view has been paused
     */
    protected boolean paused;
    
    /**
     * Window for displaying frame watch information
     */
    protected TextViewShell textShell;
    
    /**
     * Window that contains frame watch preferences
     */
    protected FrameWatchPreferencesShell prefShell;
    
    /**
     * Frame watch date formatter
     */
    protected DateFormat dateFormat = TimeUtility.getFormatter();
    
    /**
     * Frame watch view configuration
     */
    protected FrameWatchViewConfiguration frameViewConfig;
    private final HashMap<String,TableItem> frameMap = new HashMap<String,TableItem>();
    private FrameStatisticsComposite statsComp;

	private final ApplicationContext appContext;

	private final SprintfFormat format;
    
    /**
     * Creates an instance of FrameWatchComposite.
     * @param appContext the current application context
     * @param config the FrameWatchViewConfiguration object containing display settings
     */
    public FrameWatchComposite(final ApplicationContext appContext, final IViewConfiguration config) {
    	super(config);
    	this.appContext = appContext;
        tracer = TraceManager.getDefaultTracer(appContext);
        format = new SprintfFormat(appContext.getBean(IContextIdentification.class).getSpacecraftId());
        this.frameViewConfig = (FrameWatchViewConfiguration)config;
        setTableDefinition(this.frameViewConfig.getTable(IDbTableNames.DB_FRAME_DATA_TABLE_NAME));
     }

    /**
     * @{inheritDoc}
     * @see jpl.gds.monitor.guiapp.gui.views.AbstractTableViewComposite#init(org.eclipse.swt.widgets.Composite)
     */
    @Override
	public void init(final Composite parent) {
        super.init(parent);
        appContext.getBean(GeneralMessageDistributor.class).addDataListener(this, TmServiceMessageType.TelemetryFrameSummary);
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

        this.statsComp = new FrameStatisticsComposite(this.mainComposite);
        final FormData formData1 = new FormData();
        formData1.top = new FormAttachment(0);
        formData1.left = new FormAttachment(0,5);
        formData1.right = new FormAttachment(100);
        this.statsComp.setLayoutData(formData1);
        
        this.table = new Table(this.mainComposite, SWT.MULTI | SWT.FULL_SELECTION);
        this.table.setHeaderVisible(this.tableDef.isShowColumnHeader());

        final FormData formData2 = new FormData();
        formData2.left = new FormAttachment(0);
        formData2.top = new FormAttachment(this.statsComp);
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
                int align = SWT.NONE;
                if (this.tableDef.getColumnIndex(FrameWatchViewConfiguration.FRAME_COUNT_COLUMN) == i ||
                    this.tableDef.getColumnIndex(FrameWatchViewConfiguration.FRAME_SEQ_COLUMN) == i) {
                    align = SWT.RIGHT;
                }
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
                    final int[] indices = FrameWatchComposite.this.table.getSelectionIndices();
                    if (indices == null || indices.length == 0) {
                        return;
                    }
                    Arrays.sort(indices);
                    Clipboard clipboard = new Clipboard(FrameWatchComposite.this.mainComposite.getDisplay());
                    final StringBuffer plainText = new StringBuffer();
                    for (int i = 0; i < indices.length; i++) {
                        final TableItem item = FrameWatchComposite.this.table.getItem(indices[i]);
                        for (int j = 0; j < FrameWatchComposite.this.table.getColumnCount(); j++) {
                            plainText.append("\"" + item.getText(j) + "\"");
                            if (j < FrameWatchComposite.this.table.getColumnCount() - 1) {
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
                    e1.printStackTrace();
                    tracer.error("Unable to handle Copy menu item " + e1.toString());
                }
            }
        });

        clearMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    clearView();
                } catch (final Exception e1) {
                    e1.printStackTrace();
                    tracer.error("Unable to handle Clear menu item " + e1.toString());
                }
            }
        });

        prefMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    if (FrameWatchComposite.this.prefShell == null) {
                    	// This kludge works around an SWT bug on Linux
                    	// in which column sizes are not remembered
                    	final TableColumn[] cols = FrameWatchComposite.this.table.getColumns();
                    	for (int i = 0; i < cols.length; i++) {
                    		cols[i].setWidth(cols[i].getWidth());
                    	}
                        FrameWatchComposite.this.prefShell = new FrameWatchPreferencesShell(appContext, FrameWatchComposite.this.mainComposite.getShell());
                        FrameWatchComposite.this.prefShell.setValuesFromViewConfiguration(FrameWatchComposite.this.frameViewConfig);
                        
                        FrameWatchComposite.this.prefShell.getShell().addDisposeListener(new DisposeListener() {
                            @Override
							public void widgetDisposed(final DisposeEvent event) {
                                try {
                                    if (!FrameWatchComposite.this.prefShell.wasCanceled()) {
                                    	
                                    	cancelOldSortColumn();
                                    	
                                        FrameWatchComposite.this.prefShell.getValuesIntoViewConfiguration(FrameWatchComposite.this.frameViewConfig);
                                        
                                        updateTableFromConfig(IDbTableNames.DB_FRAME_DATA_TABLE_NAME, FrameWatchComposite.this.prefShell.needColumnChange());

                                        FrameWatchComposite.this.statsComp.setDisplayCharacteristics();
                                        FrameWatchComposite.this.mainComposite.setBackground(background);
                                        FrameWatchComposite.this.mainComposite.setForeground(foreground);
                                    }
                                } catch (final RuntimeException e) {
                                    e.printStackTrace();
                                    tracer.error("Unable to handle exit from preferences window " + e.toString());
                                } finally {
                                    FrameWatchComposite.this.prefShell = null;
                                    prefMenuItem.setEnabled(true);
                                }
                            }
                        });
                        prefMenuItem.setEnabled(false);
                        FrameWatchComposite.this.prefShell.open();
                    }
                } catch (final Exception e1) {
                    e1.printStackTrace();
                    tracer.error("Unable to handle Preferences menu item" + e1.toString());
                }
            }
        });

        this.mainComposite.addDisposeListener(new DisposeListener() {
            @Override
			public void widgetDisposed(final DisposeEvent event) {
                try {
                    appContext.getBean(GeneralMessageDistributor.class).removeDataListener(FrameWatchComposite.this);
                     if (FrameWatchComposite.this.prefShell != null) {
                        FrameWatchComposite.this.prefShell.getShell().dispose();
                        prefShell = null;
                    }
                } catch (final Exception e) {
                    e.printStackTrace();
                    tracer.error("Unable to handle Frame watch main shell disposal " + e.toString());
                }
            }
        });

        this.table.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    final int[] i = FrameWatchComposite.this.table.getSelectionIndices();
                    if (i != null && i.length != 0) {
                        copyMenuItem.setEnabled(true);
                    } else {
                        copyMenuItem.setEnabled(false);
                    }
                } catch (final RuntimeException e1) {
                    e1.printStackTrace();
                    tracer.error("Unable to handle Frame table selection event " + e1.toString());
                }
            }
        });
    }

    /**
     * Processes a frame watch message and creates a table row for it if the 
     * composite is not disposed and
     * 
     * @param msg frame watch message
     */
    public void displayMessage(final IFrameSummaryMessage msg) {
        if (this.parent.isDisposed()) {
            return;
        }
        this.parent.getDisplay().asyncExec(new Runnable () {
        	
    		@Override
			public String toString() {
				return "FrameWatchComposite.displayMessage.Runnable";
			}
    		
            @Override
			public void run () {
                try {
                    if (FrameWatchComposite.this.mainComposite.isDisposed()) {
                        return;
                    }
                    tracer.trace("Frame watch is processing message"); 
                    
                    FrameWatchComposite.this.statsComp.setDataFromMessage(msg);
                    boolean needSort = false;
                    synchronized(FrameWatchComposite.this.table) {
                    	final Map<String, FrameSummaryRecord> sumMap = msg.getFrameSummaryMap();
                       	if (sumMap == null || sumMap.size() == 0) {
                    		return;
                    	}
                    	final Collection<FrameSummaryRecord> sums = sumMap.values();
                    	
                    	for (final FrameSummaryRecord sum: sums) {

                            final String count = String.valueOf(sum.getCount());
                            final String ert = sum.getLastErtStr();
                            final String frameType = sum.getFrameType();
                            final long vcid = sum.getVcid();
                            final String seq = String.valueOf(sum.getSequenceCount());

                            final String key = format.anCsprintf("%03d", vcid) + "/" + format.anCsprintf("%s", frameType);
                            
                            TableItem item = frameMap.get(key);
                            
                            if (item == null) {
                                item = createTableItem(new String[] {key, count, seq, ert});
                            	item.setData(key);
                            	frameMap.put(key, item);
                            } else {
                                setColumn(item, tableDef.getColumnIndex(FrameWatchViewConfiguration.FRAME_COUNT_COLUMN), count);
                                setColumn(item, tableDef.getColumnIndex(FrameWatchViewConfiguration.FRAME_ERT_COLUMN), ert);
                                setColumn(item, tableDef.getColumnIndex(FrameWatchViewConfiguration.FRAME_SEQ_COLUMN), seq);
                            	needSort = true;
                            }
                
                        }
                        
                        if (needSort && FrameWatchComposite.this.table.getSortColumn() != null) {
                        	if (!tableDef.getSortColumn().equals(FrameWatchViewConfiguration.FRAME_VCID_TYPE_COLUMN)) {
                        		final int index = FrameWatchComposite.this.tableDef.getColumnIndex(FrameWatchComposite.this.tableDef.getSortColumn());
                        		sortTableItems(index);
                        	}
                        }
                    }
                } catch (final Exception e) {
                    e.printStackTrace();
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
            // We only need the last message, since each message contains the total frame state
            final IFrameSummaryMessage msg = (IFrameSummaryMessage)messages[messages.length - 1];
            displayMessage(msg);

        } catch (final Exception e) {
            e.printStackTrace();
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
            this.frameMap.clear();
            this.statsComp.clearData();
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

    @Override
	protected void replaceTableItems(final TableItem[] items) {
    	final String[] values = new String[this.tableDef.getActualColumnCount()];
    	
    	ArrayList<TableItem> newSelected = null;
    	   
    	for (int i = 0; i < items.length; i++) {            

    		for (int valIndex = 0; valIndex < values.length; valIndex++) {
    			values[valIndex] = items[i].getText(valIndex);
    		}

    		final String key = (String)items [ i ].getData();
    		this.frameMap.remove ( key );
    		
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
    				newSelected = new ArrayList<TableItem>(1);
    			}
    			newSelected.add(item);
    		}

    		this.frameMap.put ( key, item );
    	} // end for
    	
    	if (newSelected != null) {
    		final TableItem[] selectedItems = new TableItem[newSelected.size()];	
    		newSelected.toArray(selectedItems);
    		table.setSelection(selectedItems);
    	}
    }
    
    /**
     * Small composite that displays frame statistics such as the total number 
     * of frames, number of invalid frames and number of idle frames
     *
     */
    private class FrameStatisticsComposite extends Composite {
        private static final String VALID_LABEL   = "FSW Total Frames: ";
        private static final String INVALID_LABEL = "FSW Invalid Frames: ";
        private static final String FILL_LABEL    = "FSW Idle Frames: ";

        private final Label validFrames;
        private final Label invalidFrames;
        private final Label idleFrames;
        
        public FrameStatisticsComposite(final Composite parent) {
            super(parent, SWT.NONE);
            setLayout(new FillLayout());
            this.validFrames = new Label(this, SWT.NONE);
            this.validFrames.setText(VALID_LABEL);
            this.invalidFrames = new Label(this, SWT.NONE);
            this.invalidFrames.setText(INVALID_LABEL);
            this.idleFrames = new Label(this, SWT.BOLD);
            this.idleFrames.setText(FILL_LABEL);
            setDisplayCharacteristics();
        }
        
        public void setDisplayCharacteristics() {
            setBackground(background);
            setForeground(foreground);
            setFont(dataFont);
            this.validFrames.setBackground(background);
            this.invalidFrames.setBackground(background);
            this.idleFrames.setBackground(background);
            this.validFrames.setForeground(foreground);
            this.invalidFrames.setForeground(foreground);
            this.idleFrames.setForeground(foreground);
            this.validFrames.setFont(dataFont);
            this.invalidFrames.setFont(dataFont);
            this.idleFrames.setFont(dataFont);
        }
        
        public void setDataFromMessage(final IFrameSummaryMessage msg) {
            this.validFrames.setText ( VALID_LABEL + msg.getNumFrames () );
            this.invalidFrames.setText ( INVALID_LABEL + msg.getBadFrames() );
            this.idleFrames.setText(FILL_LABEL + msg.getIdleFrames() );
        }
        
        public void clearData() {
        	 this.validFrames.setText ( VALID_LABEL + "0" );
        	 this.invalidFrames.setText ( INVALID_LABEL + "0" );
             this.idleFrames.setText(FILL_LABEL + "0" );  
        }
    }
}
