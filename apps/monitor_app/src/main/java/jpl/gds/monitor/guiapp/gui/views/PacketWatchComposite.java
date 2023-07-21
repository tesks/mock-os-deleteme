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
import jpl.gds.monitor.config.GlobalPerspectiveParameter;
import jpl.gds.monitor.config.MonitorConfigValues;
import jpl.gds.monitor.config.MonitorConfigValues.SclkFormat;
import jpl.gds.monitor.guiapp.common.GeneralMessageDistributor;
import jpl.gds.monitor.guiapp.common.GeneralMessageListener;
import jpl.gds.monitor.guiapp.gui.views.preferences.PacketWatchPreferencesShell;
import jpl.gds.monitor.perspective.view.PacketWatchViewConfiguration;
import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.perspective.view.View;
import jpl.gds.shared.formatting.SprintfFormat;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.swt.TextViewShell;
import jpl.gds.shared.time.TimeUtility;
import jpl.gds.tm.service.api.TmServiceMessageType;
import jpl.gds.tm.service.api.packet.IPacketSummaryMessage;
import jpl.gds.tm.service.api.packet.PacketSummaryRecord;

/**
 * PacketWatchComposite is the GUI widget responsible for displaying a scrolling table
 * of Packet Summary messages. It is considered a monitor view.
 *
 */
public class PacketWatchComposite extends AbstractTableViewComposite implements GeneralMessageListener, View {
    /**
     * Default packet watch tracer
     */
    public final Tracer                      tracer;
    
    /**
     * Packet watch composite title
     */
    public static final String TITLE = "Packet Watch";

    /**
     * Indicates if scrolling for this display is paused
     */
    protected boolean paused;
    
    /**
     * Window for displaying packet information
     */
    protected TextViewShell textShell;
    
    /**
     * Packet watch preferences shell
     */
    protected PacketWatchPreferencesShell prefShell;
    
    /**
     * Packet watch date formatter
     */
    protected DateFormat dateFormat = TimeUtility.getFormatter();
    
    /**
     * Packet watch view configuration
     */
    protected PacketWatchViewConfiguration packetViewConfig;
    
    private final HashMap<String,TableItem> apidMap = new HashMap<String,TableItem>();
    private PacketStatisticsComposite statsComp;

    private final MonitorConfigValues configValues;

	private final ApplicationContext appContext;

	private final SprintfFormat format;

    /**
     * Creates an instance of PacketWatchComposite.
     * @param appContext the current application context
     * @param config the PacketWatchViewConfiguration object containing display settings
     */
    public PacketWatchComposite(final ApplicationContext appContext, final IViewConfiguration config) {
    	super(config);
    	this.appContext = appContext;
        tracer = TraceManager.getDefaultTracer(appContext);
    	this.configValues = appContext.getBean(MonitorConfigValues.class);
        format = new SprintfFormat(appContext.getBean(IContextIdentification.class).getSpacecraftId());
        this.packetViewConfig = (PacketWatchViewConfiguration)config;
        setTableDefinition(this.packetViewConfig.getTable(PacketWatchViewConfiguration.PACKET_TABLE_NAME));
     }

    /**
     * @{inheritDoc}
     * @see jpl.gds.monitor.guiapp.gui.views.AbstractTableViewComposite#init(org.eclipse.swt.widgets.Composite)
     */
    @Override
	public void init(final Composite parent) {
        super.init(parent);
        appContext.getBean(GeneralMessageDistributor.class).addDataListener(this, TmServiceMessageType.TelemetryPacketSummary);
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

        this.statsComp = new PacketStatisticsComposite(this.mainComposite);
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
        this.mainComposite.setBackground(background);
        this.mainComposite.setForeground(foreground);
        
        final Listener sortListener = new SortListener();

        final int numColumns = this.tableDef.getColumnCount();
        this.tableColumns = new TableColumn[numColumns];
        for (int i = 0; i < numColumns; i++) {
            if (this.tableDef.isColumnEnabled(i)) {
                int align = SWT.NONE;
                if (this.tableDef.getColumnIndex(PacketWatchViewConfiguration.PACKET_COUNT_COLUMN) == i ||
                    this.tableDef.getColumnIndex(PacketWatchViewConfiguration.PACKET_SEQ_COLUMN) == i) {
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
                    final int[] indices = PacketWatchComposite.this.table.getSelectionIndices();
                    if (indices == null || indices.length == 0) {
                        return;
                    }
                    Arrays.sort(indices);
                    Clipboard clipboard = new Clipboard(PacketWatchComposite.this.mainComposite.getDisplay());
                    final StringBuffer plainText = new StringBuffer();
                    for (int i = 0; i < indices.length; i++) {
                        final TableItem item = PacketWatchComposite.this.table.getItem(indices[i]);
                        for (int j = 0; j < PacketWatchComposite.this.table.getColumnCount(); j++) {
                            plainText.append("\"" + item.getText(j) + "\"");
                            if (j < PacketWatchComposite.this.table.getColumnCount() - 1) {
                                plainText.append(",");
                            }
                        }
                        plainText.append("\n");
                    }
                    final TextTransfer textTransfer = TextTransfer.getInstance();
                    clipboard.setContents(new String[]{plainText.toString()}, new Transfer[]{textTransfer});
                    clipboard.dispose();
                    clipboard = null;
                } catch (final Exception ex) {
                    ex.printStackTrace();
                    tracer.error("Error handling copy menu item " + ex.toString());
                }
            }
        });

        clearMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    clearView();
                } catch (final Exception ex) {
                    ex.printStackTrace();
                    tracer.error("Unable to handle clear menu item " + ex.toString());
                }
            }
        });

        prefMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    if (PacketWatchComposite.this.prefShell == null) {
                    	// This kludge works around an SWT bug on Linux
                    	// in which column sizes are not remembered
                    	final TableColumn[] cols = PacketWatchComposite.this.table.getColumns();
                    	for (int i = 0; i < cols.length; i++) {
                    		cols[i].setWidth(cols[i].getWidth());
                    	}
                        PacketWatchComposite.this.prefShell = new PacketWatchPreferencesShell(appContext, PacketWatchComposite.this.mainComposite.getShell());
                        PacketWatchComposite.this.prefShell.setValuesFromViewConfiguration(PacketWatchComposite.this.packetViewConfig);
                        
                        PacketWatchComposite.this.prefShell.getShell().addDisposeListener(new DisposeListener() {
                            @Override
                            public void widgetDisposed(final DisposeEvent event) {
                                try {
                                    if (!PacketWatchComposite.this.prefShell.wasCanceled()) {

                                    	cancelOldSortColumn();
                                        
                                    	PacketWatchComposite.this.prefShell.getValuesIntoViewConfiguration(PacketWatchComposite.this.packetViewConfig);
                                        
                                    	updateTableFromConfig(PacketWatchViewConfiguration.PACKET_TABLE_NAME, PacketWatchComposite.this.prefShell.needColumnChange());
                                        
                                        PacketWatchComposite.this.mainComposite.setBackground(background);
                                        PacketWatchComposite.this.mainComposite.setForeground(foreground);
                                    }
                                } catch (final Exception ex) {
                                    ex.printStackTrace();
                                    tracer.error("Unable to handle exit from preferences window " + ex.toString());
                                } finally {
                                    PacketWatchComposite.this.prefShell = null;
                                    prefMenuItem.setEnabled(true);
                                }
                            }
                        });
                        prefMenuItem.setEnabled(false);
                        PacketWatchComposite.this.prefShell.open();
                    }
                } catch (final Exception ex) {
                    ex.printStackTrace();
                    tracer.error("Unable to handle preferences menu item " + ex.toString());
                }
            }
        });

        this.mainComposite.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent event) {
                try {
                    appContext.getBean(GeneralMessageDistributor.class).removeDataListener(PacketWatchComposite.this);
                    if (PacketWatchComposite.this.prefShell != null) {
                        PacketWatchComposite.this.prefShell.getShell().dispose();
                        prefShell = null;
                    }
                } catch (final Exception ex) {
                    ex.printStackTrace();
                    tracer.error("Unable to packet watch shell dispose event " + ex.toString());
                }
            }
        });

        this.table.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    final int[] i = PacketWatchComposite.this.table.getSelectionIndices();
                    if (i != null && i.length != 0) {
                        copyMenuItem.setEnabled(true);
                    } else {
                        copyMenuItem.setEnabled(false);
                    }
                } catch (final Exception ex) {
                    ex.printStackTrace();
                    tracer.error("Unable to handle packet table selection event " + ex.toString());
                }
            }
        });
    }

    /**
     * Processes packet extract message and creates table row if composite is 
     * not disposed
     * 
     * @param msg Packet extract summary message
     */
    public void displayMessage(final IPacketSummaryMessage msg) {
        if (this.parent.isDisposed()) {
            return;
        }
        this.parent.getDisplay().asyncExec(new Runnable () {
        	
    		@Override
			public String toString() {
				return "PacketWatchComposite.displayMessage.Runnable";
			}
    		
            @Override
            public void run () {
                try {
                    if (PacketWatchComposite.this.mainComposite.isDisposed()) {
                        return;
                    }
                    tracer.trace("Packet watch is processing message"); 
                    
                    PacketWatchComposite.this.statsComp.setDataFromMessage(msg);
                    boolean needSort = false;
                    synchronized(PacketWatchComposite.this.table) {
                    	final Map<String, PacketSummaryRecord> sumMap = msg.getPacketSummaryMap();
                    	if (sumMap == null || sumMap.size() == 0) {
                    		return;
                    	}
                    	final Collection<PacketSummaryRecord> sums = sumMap.values();
                    	for (final PacketSummaryRecord sum: sums) {
                            final long apid = sum.getApid();
                            final String name = sum.getApidName();
                            final String count = String.valueOf(sum.getInstanceCount());
                            final String ert = sum.getLastErtStr();
                            final String scet = sum.getLastScetStr();
                            final String sol = sum.getLastLstStr();
                            final String sclk = ((SclkFormat)configValues.getValue(GlobalPerspectiveParameter.SCLK_FORMAT)) == SclkFormat.DECIMAL ? 
                                    sum.getLastSclk().toDecimalString() : sum.getLastSclk().toTicksString();
                                    final long vcid = sum.getVcid();
                            final String seq = String.valueOf(sum.getSeqCount());
                            final String key = format.anCsprintf("%03d", vcid) + "/" + format.anCsprintf("%04d", apid);

                            TableItem item = apidMap.get(key);
                            
                            if (item == null) {
                                item = createTableItem(new String[] {key, name, count, seq, ert, sclk, scet, sol});
                                item.setData(key);
                                apidMap.put(key, item);
                            } else {
                                setColumn(item, tableDef.getColumnIndex(PacketWatchViewConfiguration.PACKET_COUNT_COLUMN), count);
                                setColumn(item, tableDef.getColumnIndex(PacketWatchViewConfiguration.PACKET_ERT_COLUMN), ert);
                                setColumn(item, tableDef.getColumnIndex(PacketWatchViewConfiguration.PACKET_SCET_COLUMN), scet);
                                setColumn(item, tableDef.getColumnIndex(PacketWatchViewConfiguration.PACKET_SCLK_COLUMN), sclk);
                                setColumn(item, tableDef.getColumnIndex(PacketWatchViewConfiguration.PACKET_LST_COLUMN), sol);
                                setColumn(item, tableDef.getColumnIndex(PacketWatchViewConfiguration.PACKET_SEQ_COLUMN), seq);
                            	needSort = true;
                            }
                        }
                        if (needSort && PacketWatchComposite.this.table.getSortColumn() != null) {
                        	if (!tableDef.getSortColumn().equals(PacketWatchViewConfiguration.PACKET_NAME_COLUMN) &&
                        	!tableDef.getSortColumn().equals(PacketWatchViewConfiguration.PACKET_VCID_APID_COLUMN)) {

                        		final int index = PacketWatchComposite.this.tableDef.getColumnIndex(PacketWatchComposite.this.tableDef.getSortColumn());
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
            // We only need the last message, since each message contains the total packet state
            final IPacketSummaryMessage msg = (IPacketSummaryMessage)messages[messages.length - 1];
            displayMessage(msg);

        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.monitor.guiapp.gui.views.AbstractTableViewComposite#clearView()
     */
	@Override
    public void clearView() {
    	super.clearView();
        synchronized(this.table) {
         	this.apidMap.clear();
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
              final String key = (String)items[i].getData();
              this.apidMap.remove ( key );
              
              final int oldIndex = this.table.indexOf(items[i]);
              final boolean selected = table.isSelected(oldIndex);

              final long oldTimestamp = getEntryTimestamp(items[i]);
              removeEntry(items[i]);
              items[i] = null;

              final TableItem item = createTableItem(oldTimestamp);

              item.setData(key);
              item.setText(values);

              if (selected) {
            	  if (newSelected == null) {
            		  newSelected = new ArrayList<TableItem>(1);
            	  }
            	  newSelected.add(item);
              }
         		
              this.apidMap.put(key, item);
          }

          if (newSelected != null) {
        	  final TableItem[] selectedItems = new TableItem[newSelected.size()];	
        	  newSelected.toArray(selectedItems);
        	  table.setSelection(selectedItems);
          }
    }
    
    /**
     * Composite that displays packet statistics such as number of valid 
     * packets, number of invalid packets, and number of fill packets
     *
     */
    private class PacketStatisticsComposite extends Composite {
        private static final String VALID_LABEL = "FSW Valid Packets: ";
        private static final String INVALID_LABEL = "FSW Invalid Packets: ";
        private static final String FILL_LABEL = "FSW Fill Packets: ";

        private final Label validPackets;
        private final Label invalidPackets;
        private final Label idlePackets;
        
        public PacketStatisticsComposite(final Composite parent) {
            super(parent, SWT.NONE);
            setLayout(new FillLayout());
            this.validPackets = new Label(this, SWT.NONE);
            this.validPackets.setText(VALID_LABEL);
            this.invalidPackets = new Label(this, SWT.NONE);
            this.invalidPackets.setText(INVALID_LABEL);
            this.idlePackets = new Label(this, SWT.BOLD);
            this.idlePackets.setText(FILL_LABEL);
            setDisplayCharacteristics();
        }
        
        public void setDisplayCharacteristics() {
            setBackground(background);
            setForeground(foreground);
            setFont(dataFont);
            this.validPackets.setBackground(background);
            this.invalidPackets.setBackground(background);
            this.idlePackets.setBackground(background);
            this.validPackets.setForeground(foreground);
            this.invalidPackets.setForeground(foreground);
            this.idlePackets.setForeground(foreground);
            this.validPackets.setFont(dataFont);
            this.invalidPackets.setFont(dataFont);
            this.idlePackets.setFont(dataFont);
        }
        
        public void setDataFromMessage(final IPacketSummaryMessage msg) {
            // We display only FSW data here, not SSE
            if (msg.isFromSse()) {
                return;
            }
            this.validPackets.setText(VALID_LABEL + msg.getNumValidPackets());
            this.invalidPackets.setText(INVALID_LABEL + msg.getNumInvalidPackets());
            this.idlePackets.setText(FILL_LABEL + msg.getNumFillPackets());
        }
        
        public void clearData() {
        	  this.validPackets.setText(VALID_LABEL + "0");
              this.invalidPackets.setText(INVALID_LABEL + "0");
              this.idlePackets.setText(FILL_LABEL + "0");
        }
    }
}
