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
import java.util.concurrent.LinkedBlockingQueue;
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
import org.eclipse.swt.graphics.Color;
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

import jpl.gds.dictionary.api.ICategorySupport;
import jpl.gds.evr.api.EvrMetadataKeywordEnum;
import jpl.gds.evr.api.config.EvrProperties;
import jpl.gds.evr.api.message.EvrMessageType;
import jpl.gds.evr.api.message.IEvrMessage;
import jpl.gds.evr.api.util.EvrColorUtility;
import jpl.gds.message.api.MessageUtility;
import jpl.gds.monitor.config.MonitorDictionaryUtility;
import jpl.gds.monitor.config.MonitorGuiProperties;
import jpl.gds.monitor.guiapp.MonitorTimers;
import jpl.gds.monitor.guiapp.common.GeneralFlushListener;
import jpl.gds.monitor.guiapp.common.GeneralMessageDistributor;
import jpl.gds.monitor.guiapp.common.GeneralMessageListener;
import jpl.gds.monitor.guiapp.common.gui.DisplayConstants;
import jpl.gds.monitor.guiapp.gui.views.preferences.EvrPreferencesShell;
import jpl.gds.monitor.guiapp.gui.views.support.CountShell;
import jpl.gds.monitor.perspective.view.EvrViewConfiguration;
import jpl.gds.monitor.perspective.view.RealtimeRecordedFilterType;
import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.perspective.view.View;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.swt.ChillColorCreator;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.swt.TextViewShell;
import jpl.gds.shared.swt.types.ChillColor;
import jpl.gds.shared.template.TemplateException;
import jpl.gds.shared.time.TimeUtility;

/**
 * EvrComposite is the GUI widget responsible for displaying a scrolling table
 * of EVR messages. It is considered a monitor view.
 *
 */
public class EvrComposite extends AbstractTableViewComposite implements GeneralMessageListener, GeneralFlushListener, View {

    /**
     * Default tracer
     */
    public static Tracer                                           tracer;

    /**
     * Evr composite title
     */
    public static final String TITLE = "EVR";

    /**
     * Indicates if this composite has paused the telemetry flow
     */
    protected boolean paused;

    /**
     * View shell that displays EVR XML 
     */
    protected TextViewShell textShell;

    /**
     * EVR preferences window
     */
    protected EvrPreferencesShell prefShell;

    /**
     * Right-click menu item for viewing the EVR XML message
     */
    protected MenuItem viewMenuItem;

    /**
     * EVR Level filter
     */
    protected String[] selectedLevels = null;

    /**
     * EVR sources filter
     */
    protected String[] selectedSources = null;

    /**
     * EVR modules filter
     */
    protected String[] selectedModules = null;

    /**
     * Stores EVR messages that need to be processed
     * 
     * Changed from ArrayBlockngQueue for more sensible memory
     * usage.
     */
    protected LinkedBlockingQueue<jpl.gds.shared.message.IMessage> messageQueue;

    /**
     * Lock object for freezing both the
     * messageQueue and the display table, so that operations on them can occur
     * atomically.
     */
    protected Object tableAndMessageLock = new Object();

    /**
     * Maximum number of rows permitted in the EVR composite
     */
    protected int maxRows;

    /**
     * Color used for marking EVRs in the table
     */
    protected Color markColor;

    /**
     * Date formatter
     */
    protected DateFormat dateFormat = TimeUtility.getFormatter();

    /**
     * EVR view configuration
     */
    protected EvrViewConfiguration evrViewConfig;

    private boolean stalled = false;
    private MenuItem pauseMenuItem;
    private MenuItem resumeMenuItem;

	private final ApplicationContext appContext;
	private static EvrColorUtility evrColorUtil;

    /**
     * Creates an instance of EvrComposite.
     * @param appContext the current application context
     * @param config the EvrViewConfiguration object containing display settings
     */
    public EvrComposite(final ApplicationContext appContext, final IViewConfiguration config) {
        super(config);
        this.appContext = appContext;
        if (evrColorUtil == null) {
            evrColorUtil = new EvrColorUtility(appContext.getBean(EvrProperties.class));
        }
        evrViewConfig = (EvrViewConfiguration)config;
        maxRows = evrViewConfig.getMaxRows();
        tracer = TraceManager.getDefaultTracer(appContext);
        /*
         * Change initialization of queue length.
         */
        markColor = ChillColorCreator.getColor(new ChillColor(evrViewConfig.getMarkColor()));
        selectedLevels = evrViewConfig.getLevels();
        selectedSources = evrViewConfig.getSources();
        selectedModules = evrViewConfig.getModules();
        messageQueue = new LinkedBlockingQueue<jpl.gds.shared.message.IMessage>(maxRows * appContext.getBean(MonitorGuiProperties.class).getListQueueScaleFactor());
        setTableDefinition(evrViewConfig.getTable(EvrViewConfiguration.EVR_TABLE_NAME));
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.monitor.guiapp.gui.views.AbstractTableViewComposite#init(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void init(final Composite parent) {
        super.init(parent);
        appContext.getBean(MonitorTimers.class).addGeneralFlushListener(this);
        appContext.getBean(GeneralMessageDistributor.class).addDataListener(this, EvrMessageType.Evr);
        setSortColumn();
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.monitor.guiapp.gui.views.AbstractTableViewComposite#createGui()
     */
    @Override
    protected void createGui() {

        mainComposite = new Composite(parent, SWT.NONE);
        final FormLayout shellLayout = new FormLayout();
        shellLayout.spacing = 5;
        mainComposite.setLayout(shellLayout);

        table = new Table(mainComposite, SWT.MULTI | SWT.FULL_SELECTION);

        table.setHeaderVisible(tableDef.isShowColumnHeader());

        final FormData formData2 = new FormData();
        formData2.left = new FormAttachment(0);
        formData2.top = new FormAttachment(0);
        formData2.right = new FormAttachment(100);
        formData2.bottom = new FormAttachment(100);
        table.setLayoutData(formData2);

        updateTableFontAndColors();

        final Listener sortListener = new SortListener();

        final int numColumns = tableDef.getColumnCount();
        tableColumns = new TableColumn[numColumns];
        for (int i = 0; i < numColumns; i++) {
            if (tableDef.isColumnEnabled(i)) {
                tableColumns[i] = new TableColumn(table, SWT.NONE);
                tableColumns[i].setText(tableDef.getOfficialColumnName(i));
                tableColumns[i].setWidth(tableDef.getColumnWidth(i));
                tableColumns[i].addListener(SWT.Selection, sortListener);
                tableColumns[i].setMoveable(true);
                if (tableDef.isSortColumn(i) && tableDef.isSortAllowed()) {
                    table.setSortColumn(tableColumns[i]);
                    tableColumns[i].setImage(tableDef.isSortAscending() ? upImage : downImage);
                }
            } else {
                tableColumns[i] = null;
            }
        }

        table.setColumnOrder(tableDef.getColumnOrder());

        final Menu viewMenu = new Menu(mainComposite);
        viewMenuItem = new MenuItem(viewMenu, SWT.PUSH);
        viewMenuItem.setText("View Message...");
        table.setMenu(viewMenu);
        viewMenuItem.setEnabled(false);
        new MenuItem(viewMenu, SWT.SEPARATOR);
        final MenuItem prefMenuItem =  new MenuItem(viewMenu, SWT.PUSH);
        prefMenuItem.setText("Preferences...");
        new MenuItem(viewMenu, SWT.SEPARATOR);
        pauseMenuItem =  new MenuItem(viewMenu, SWT.PUSH);
        pauseMenuItem.setText("Pause");
        pauseMenuItem.setEnabled(true);
        resumeMenuItem =  new MenuItem(viewMenu, SWT.PUSH);
        resumeMenuItem.setText("Resume");
        resumeMenuItem.setEnabled(false);
        final MenuItem clearMenuItem =  new MenuItem(viewMenu, SWT.PUSH);
        clearMenuItem.setText("Clear Data");
        new MenuItem(viewMenu, SWT.SEPARATOR);
        final MenuItem copyMenuItem =  new MenuItem(viewMenu, SWT.PUSH);
        copyMenuItem.setText("Copy");
        copyMenuItem.setEnabled(false);
        new MenuItem(viewMenu, SWT.SEPARATOR);
        final MenuItem markMenuItem = new MenuItem(viewMenu, SWT.PUSH);
        markMenuItem.setText("Mark");
        markMenuItem.setEnabled(false);
        final MenuItem unmarkMenuItem = new MenuItem(viewMenu, SWT.PUSH);
        unmarkMenuItem.setText("Unmark");
        unmarkMenuItem.setEnabled(false);
        new MenuItem(viewMenu, SWT.SEPARATOR);
        final MenuItem showDefMenuItem = new MenuItem(viewMenu, SWT.PUSH);
        showDefMenuItem.setText("Show EVR Definition...");
        showDefMenuItem.setEnabled(false);
        new MenuItem(viewMenu, SWT.SEPARATOR);

        final MenuItem viewCountMenuItem = new MenuItem(viewMenu, SWT.PUSH);
        viewCountMenuItem.setText("View Count");
        viewCountMenuItem.setEnabled(true);


        copyMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    synchronized (tableAndMessageLock) {

                        final int[] indices = EvrComposite.this.table
                                .getSelectionIndices();
                        if (indices == null || indices.length == 0) {
                            return;
                        }
                        Arrays.sort(indices);
                        Clipboard clipboard = new Clipboard(
                                EvrComposite.this.mainComposite.getDisplay());
                        final StringBuffer plainText = new StringBuffer();
                        for (int i = 0; i < indices.length; i++) {
                            final TableItem item = EvrComposite.this.table
                                    .getItem(indices[i]);
                            for (int j = 0; j < EvrComposite.this.table
                                    .getColumnCount(); j++) {
                                plainText.append("\"" + item.getText(j) + "\"");
                                if (j < EvrComposite.this.table
                                        .getColumnCount() - 1) {
                                    plainText.append(",");
                                }
                            }
                            plainText.append("\n");
                        }
                        final TextTransfer textTransfer = TextTransfer.getInstance();
                        clipboard.setContents(
                                new String[] { plainText.toString() },
                                new Transfer[] { textTransfer });
                        clipboard.dispose();
                        clipboard = null;

                    }

                } catch (final Exception ex) {
                    ex.printStackTrace();
                    tracer.error("Error handling copy menu item " + ex.toString());
                }
            }
        });

        markMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    synchronized (tableAndMessageLock) {

                        final int[] indices = EvrComposite.this.table
                                .getSelectionIndices();
                        if (indices == null || indices.length == 0) {
                            return;
                        }
                        Arrays.sort(indices);
                        for (int i = 0; i < indices.length; i++) {
                            final TableItem item = EvrComposite.this.table
                                    .getItem(indices[i]);
                            item.setData("isMark", true);
                            item.setBackground(markColor);
                            item.setForeground(null);
                        }

                    }

                } catch (final Exception e1) {
                    e1.printStackTrace();
                    tracer.error("Error handling Mark menu item " + e1.toString());
                }
            }
        });

        unmarkMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    synchronized (tableAndMessageLock) {

                        final int[] indices = EvrComposite.this.table
                                .getSelectionIndices();
                        if (indices == null || indices.length == 0) {
                            return;
                        }
                        Arrays.sort(indices);
                        for (int i = 0; i < indices.length; i++) {
                            final TableItem item = EvrComposite.this.table
                                    .getItem(indices[i]);
                            item.setData("isMark", false);
                            final IEvrMessage evr = (IEvrMessage) item.getData();
                            item.setBackground(evrColorUtil
                                    .getSwtBackgroundColorForLevel(
                                            evr.getEvr().getLevel()));
                            item.setForeground(evrColorUtil
                                    .getSwtForegroundColorForLevel(
                                            evr.getEvr().getLevel()));
                        }

                    }

                } catch (final Exception ex) {
                    ex.printStackTrace();
                    tracer.error("Unable to handle Unmark menu item " + ex.toString());
                }
            }

        });

        clearMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    synchronized (tableAndMessageLock) {
                        clearView();
                    }

                } catch (final Exception e1) {
                    e1.printStackTrace();
                    tracer.error("Unable to handle Clear menu item " + e1.toString());
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
                    tracer.error("Unable to handle Pause menu item " + e1.toString());
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
                    e1.printStackTrace();
                    tracer.error("Unable to handle Resume menu item " + e1.toString());
                }
            }
        });

        prefMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    //add before preferences pane is opened
                    tableDef.setColumnOrder(table.getColumnOrder());

                    if (prefShell == null) {
                        // This kludge works around an SWT bug on Linux
                        // in which column sizes are not remembered
                        final TableColumn[] cols = EvrComposite.this.table.getColumns();
                        for (int i = 0; i < cols.length; i++) {
                            cols[i].setWidth(cols[i].getWidth());
                        }
                        prefShell = new EvrPreferencesShell(appContext, EvrComposite.this.mainComposite.getShell());
                        prefShell.setValuesFromViewConfiguration(evrViewConfig);
                        prefShell.getShell().addDisposeListener(new DisposeListener() {
                            @Override
                            public void widgetDisposed(final DisposeEvent event) {
                                try {
                                    if (!prefShell.wasCanceled()) {

                                        updateViewConfig();

                                        cancelOldSortColumn();

                                        prefShell.getValuesIntoViewConfiguration(evrViewConfig);
                                        selectedLevels = evrViewConfig.getLevels();
                                        selectedSources = evrViewConfig.getSources();
                                        selectedModules = evrViewConfig.getModules();
                                        maxRows = evrViewConfig.getMaxRows();
                                        if (markColor != null && !markColor.isDisposed()) {
                                            markColor.dispose();
                                            markColor = null;
                                        }
                                        markColor = ChillColorCreator.getColor(new ChillColor(evrViewConfig.getMarkColor()));

                                        synchronized (tableAndMessageLock) {

                                            final TableItem[] items = EvrComposite.this.table.getItems();

                                            // recolor items as needed
                                            IEvrMessage def = null;
                                            for (int i = 0; i < items.length; i++) {
                                                final Boolean isMark = (Boolean)items[i].getData("isMark");
                                                if (isMark != null && isMark) {
                                                    items[i].setForeground(null);
                                                    items[i].setBackground(markColor);
                                                } else {
                                                    try {
                                                        def = (IEvrMessage)items[i].getData();
                                                        if (def == null) {
                                                            continue;
                                                        }
                                                        items[i].setForeground(evrColorUtil.getSwtForegroundColorForLevel(def.getEvr().getLevel()));
                                                        items[i].setBackground(evrColorUtil.getSwtBackgroundColorForLevel(def.getEvr().getLevel()));
                                                    } catch (final NullPointerException e) {
                                                        e.printStackTrace();
                                                    }
                                                }	 
                                            }      

                                            updateTableFromConfig(EvrViewConfiguration.EVR_TABLE_NAME, prefShell.needColumnChange());

                                            messageQueue.clear();
                                            clearView();
                                        }

                                    }
                                } catch (final Exception e) {
                                    e.printStackTrace();
                                    tracer.error("Unable to handle exit from Preferences window " + e.toString());
                                } finally {
                                    prefShell = null;
                                    prefMenuItem.setEnabled(true);
                                }
                            }
                        });
                        prefMenuItem.setEnabled(false);
                        prefShell.open();
                    }
                } catch (final Exception e1) {
                    e1.printStackTrace();
                    tracer.error("Unable to handle Preferences menu item " + e1.toString());
                }
            }
        });

        showDefMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    final int[] indices = table.getSelectionIndices();
                    if (indices == null || indices.length != 1) {
                        return;
                    }
                    final IEvrMessage evrMsg = (IEvrMessage)table.getItem(indices[0]).getData();
                    final String text = appContext.getBean(MonitorDictionaryUtility.class).getEvrText(evrMsg.getEvr());
                    final TextViewShell tvs = new TextViewShell(table.getShell(), tracer);
                    tvs.getShell().setSize(500,400);
                    tvs.setText(text);
                    tvs.open();
                } catch (final Exception ex) {
                    ex.printStackTrace();
                    TraceManager.getDefaultTracer().error("Error in show definition menu item handling " + ex.toString());

                }
            }
        });

        viewCountMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    final CountShell tvs = new CountShell(table.getShell(), EvrComposite.this, viewConfig.getViewName());
                    tvs.open();
                } catch (final Exception ex) {
                    ex.printStackTrace();
                    TraceManager.getDefaultTracer().error("Error in showing EVR count" + ex.toString());

                }
            }
        });

        mainComposite.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent event) {
                try {
                    appContext.getBean(GeneralMessageDistributor.class).removeDataListener(EvrComposite.this);
                    appContext.getBean(MonitorTimers.class).removeGeneralFlushListener(EvrComposite.this);
                    if (prefShell != null) {
                        prefShell.getShell().dispose();
                        prefShell = null;
                    }
                } catch (final Exception e) {
                    e.printStackTrace();
                    tracer.error("Unable to handle Evr main shell disposal " + e.toString());
                }
            }
        });

        table.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final org.eclipse.swt.events.SelectionEvent e) {
                try {
                    final int[] i = EvrComposite.this.table.getSelectionIndices();
                    if (i != null && i.length != 0) {
                        copyMenuItem.setEnabled(true);
                        markMenuItem.setEnabled(true);
                        unmarkMenuItem.setEnabled(true);
                    } else {
                        copyMenuItem.setEnabled(false);
                        markMenuItem.setEnabled(false);
                        unmarkMenuItem.setEnabled(false);
                    }
                    if (i != null && i.length == 1) {
                        viewMenuItem.setEnabled(true);
                        showDefMenuItem.setEnabled(true);
                    } else {
                        viewMenuItem.setEnabled(false);
                        showDefMenuItem.setEnabled(false);
                    }
                } catch (final Exception e1) {
                    e1.printStackTrace();
                    tracer.error("Unable to handle Evr table selection event " + e.toString());
                }
            }
        });

        viewMenuItem.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(final SelectionEvent arg0) {
                try {
                    final int i = EvrComposite.this.table.getSelectionIndex();
                    if (i != -1) {
                        if (textShell == null || textShell.getShell().isDisposed()) {
                            textShell = new TextViewShell(EvrComposite.this.mainComposite.getShell(), tracer);
                        }
                        final TableItem it = EvrComposite.this.table.getItem(i);
                        textShell.getShell().setText("EVR" +
                                it.getText(EvrComposite.this.tableDef.getActualIndex(EvrComposite.this.tableDef.getColumnIndex(
                                        EvrViewConfiguration.EVR_ID_COLUMN))));
                        final jpl.gds.shared.message.IMessage m = (jpl.gds.shared.message.IMessage)it.getData();
                        if (m != null) {
                            try {
                                final String msgText = MessageUtility.getMessageText(m, "Xml");
                                textShell.setText(msgText);
                            } catch (final TemplateException e) {
                                textShell.setText("Unable to format EVR");
                            }
                            textShell.open();
                        }
                    }
                } catch (final Exception ex) {
                    ex.printStackTrace();
                    tracer.error("Unable to handle View menu item " + ex.toString());
                }

            }

            @Override
            public void widgetDefaultSelected(final SelectionEvent arg0) {
            }
        });
    }

    private TableItem addNewItem(final IEvrMessage evrMessage) {
        TableItem item = null;
        try {
            synchronized(tableAndMessageLock) {
                tracer.trace("EVR Set data callback listener queue size " + messageQueue.size());

                String source = "FSW";
                if (evrMessage.getEvr().isFromSse()) {
                    source = "SSE";
                }
                final String id =  String.valueOf(evrMessage.getEvr().getEventId());
                String name = "Unknown";
                String message = "No Message";
                String level = "Unknown";
                String module = "Unknown";
                String ert = "Unknown";
                String sclk = "Unknown";
                String scet = "Unknown";
                String sol = "";
                String task = "";
                String seq = "";
                String catSeq = "";
                final String recorded = String.valueOf(!evrMessage.getEvr().isRealtime());

                if(evrMessage.getEvr().getName() != null) {
                    name = evrMessage.getEvr().getName();
                }
                if (evrMessage.getEvr().getMessage() != null) {
                    message = evrMessage.getEvr().getMessage();
                }
                if (evrMessage.getEvr().getLevel() != null && !evrMessage.getEvr().getLevel().equals("")) {
                    level = evrMessage.getEvr().getLevel();
                }
                if (evrMessage.getEvr().getCategory(ICategorySupport.MODULE) != null && !evrMessage.getEvr().getCategory(ICategorySupport.MODULE) .equals("")) {
                    module = evrMessage.getEvr().getCategory(ICategorySupport.MODULE) ;
                }
                if (evrMessage.getEvr().getErt() != null) {
                    ert = evrMessage.getEvr().getErt().getFormattedErt(true);
                }
                if (evrMessage.getEvr().getSclk() != null) {
                    sclk = evrViewConfig.isSclkSubseconds() ? evrMessage.getEvr().getSclk().toDecimalString() : evrMessage.getEvr().getSclk().toTicksString();
                }
                if (evrMessage.getEvr().getSol() != null) {
                    sol = evrMessage.getEvr().getSol().getFormattedSol(true);
                }
                if (evrMessage.getEvr().getScet() != null) {
                    scet = evrMessage.getEvr().getScet().getFormattedScet(true);
                }

                String value = evrMessage.getEvr().getMetadataValue(
                        EvrMetadataKeywordEnum.TASKNAME);

                if (value != null)
                {
                    task = value;
                }

                value = evrMessage.getEvr().getMetadataValue(
                        EvrMetadataKeywordEnum.SEQUENCEID);

                if (value != null)
                {
                    seq = value;
                }

                value = evrMessage.getEvr().getMetadataValue(
                        EvrMetadataKeywordEnum.CATEGORYSEQUENCEID);

                if (value != null)
                {
                    catSeq = value;
                }
                item = createTableItem(new String[] {
                        source, name, id, ert, sclk, scet, module, level, message, task, seq, catSeq, sol, recorded});

                item.setData(evrMessage);

                if (!level.equals("")) {
                    item.setBackground(evrColorUtil.getSwtBackgroundColorForLevel(evrMessage.getEvr().getLevel()));
                    item.setForeground(evrColorUtil.getSwtForegroundColorForLevel(evrMessage.getEvr().getLevel()));
                }  			
            }
        } catch (final Exception ex) {
            ex.printStackTrace();
            tracer.error("Error updating EVR table row " + ex.toString());
        }
        return item;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.monitor.guiapp.common.GeneralMessageListener#messageReceived(jpl.gds.shared.message.IMessage[])
     */
    @Override
    public void messageReceived(final IMessage[] messages) {
        try {

            tracer.trace("upon incoming EVR message queue size is " + messageQueue.size());
            tracer.trace("upon incoming EVR message count is " + messages.length);
            for (int i = 0; i < messages.length; i++) {
                final IEvrMessage evr = (IEvrMessage)messages[i];
                if (evr.getEvr() == null) {
                    continue;
                }
                String source = "FSW";
                if (evr.getEvr().isFromSse()) {
                    source = "SSE";
                }
                if (!isSelectedLevel(evr.getEvr().getLevel())) {
                    continue;
                }
                if (!isSelectedModule(evr.getEvr().getCategory(ICategorySupport.MODULE) )) {
                    continue;
                }
                if (!isSelectedSource(source)) {
                    continue;
                }

                /*
                 *  RT/Recorded flag is now an enum
                 * rather than a boolean. Updated this logic appropriately. If
                 * the data is realtime and we only want recorded, or vice versa,
                 * discard the data.
                 */
                if (evr.getEvr().isRealtime() && 
                        evrViewConfig.getRealtimeRecordedFilterType() == RealtimeRecordedFilterType.RECORDED) {
                    continue;
                } else if (!evr.getEvr().isRealtime() && 
                        evrViewConfig.getRealtimeRecordedFilterType() == RealtimeRecordedFilterType.REALTIME) {
                    continue;
                }

                int repCount = 0;
                boolean offerAccepted = false;
                while (!offerAccepted) {

                    tracer.trace("EVR composite is requesting lock in messageReceived");
                    synchronized (tableAndMessageLock) {
                        tracer.trace("EVR composite got lock in messageReceived");
                        offerAccepted = messageQueue.offer(messages[i], 50, TimeUnit.MILLISECONDS);
                    }    
                    tracer.trace("EVR composite released lock in messageReceived");
                    if (offerAccepted) {
                        break;
                    }

                    // The queue is full. Try to force some of the messages to the display
                    displayMessages(false);

                    // The rest of this is debug code trying to catch the EVR stall.
                    repCount++;
                    if (repCount > 2000) {
                        tracer.error("EVR View appears stalled in offer loop");
                        stalled = true;
                        if (paused) {
                            showPauseError();
                            stalled = false;
                            paused = false;
                            repCount = 0;
                        }
                    }
                    if (repCount > 2100) {
                        showStallError();
                        messageQueue.clear();
                        stalled = false;
                        repCount = 0;
                    }
                }

            }

            displayMessages(false);

        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    private void pause() {
        paused = true;
    }

    private void resume() {
        paused = false;
    }

    /**
     * Determines if an EVR level string is on the list of levels the
     * user has configured to be of interest
     * @param level the EVR level String
     * @return true if the user elected to view evrs of the given level in
     * his configuration
     */
    protected boolean isSelectedLevel(final String level) {
        if (selectedLevels == null) {
            return true;
        }

        String useLevel = level;

        if ((useLevel == null) || useLevel.isEmpty())
        {
            useLevel = DisplayConstants.UNKNOWN_EVR_LEVEL;
        }

        for (int i = 0; i < selectedLevels.length; i++) {
            if (selectedLevels[i].equalsIgnoreCase(useLevel)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Determines if an EVR module string is on the list of modules the
     * user has configured to be of interest
     * @param module the EVR module string
     * @return true if the user elected to view evrs of the given module in
     * his configuration
     */
    protected boolean isSelectedModule(final String module) {
        if (selectedModules == null) {
            return true;
        }
        for (int i = 0; i < selectedModules.length; i++) {
            if (selectedModules[i].equalsIgnoreCase(module)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if an EVR level string is on the list of sources the
     * user has configured to be of interest
     * @param source the EVR source String
     * @return true if the user elected to view evrs from the given source in
     * his configuration
     */
    protected boolean isSelectedSource(final String source) {
        if (selectedSources == null) {
            return true;
        }
        for (int i = 0; i < selectedSources.length; i++) {
            if (selectedSources[i].equalsIgnoreCase(source)) {
                return true;
            }
        }
        return false;
    }

    private void showStallError() {
        parent.getDisplay().asyncExec(new Runnable () {
            @Override
            public void run () {
                try {
                    SWTUtilities.showErrorDialog(EvrComposite.this.mainComposite.getShell(), "Internal Error", "The EVR View has stalled.\n" +
                            "Please contact the MPCS team immediately if you see this error.\n" +
                            String.valueOf(messageQueue.size()) + " EVRs had to be discarded. Operation will continue.");
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void showPauseError() {
        parent.getDisplay().asyncExec(new Runnable () {
            @Override
            public void run () {
                try {
                    SWTUtilities.showErrorDialog(EvrComposite.this.mainComposite.getShell(), "Pause Too Long", "The EVR View has been paused too long.\n" +
                            "The pause will be released."); 
                    resumeMenuItem.setEnabled(false);
                    pauseMenuItem.setEnabled(true);
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }


    /**
     * Display queued messages.
     * @param timedFlush true if this display operation was triggered by the
     * flush timer.
     */
    protected void displayMessages(final boolean timedFlush) {
        if (parent.isDisposed() || parent.getDisplay().isDisposed()) {
            return;
        }
        if (messageQueue.isEmpty()) {
            return;
        }

        tracer.trace("In display messages");

        if (stalled) {
            tracer.error("In display messages " + EvrComposite.this);
        }
        parent.getDisplay().asyncExec(new Runnable () {

            @Override
            public String toString() {
                return "EvrComposite.displayMessages.Runnable";
            }

            @Override
            public void run() {

                tracer.trace("EVR composite is waiting for lock in display messages run(). Queue size is "
                        + messageQueue.size());

                synchronized (tableAndMessageLock) {

                    try {
                        tracer.trace("EVR composite got lock in display messages run() queue size is "
                                + messageQueue.size());

                        if (EvrComposite.this.mainComposite.isDisposed()) {
                            if (stalled) {
                                tracer.error("Composite is disposed in "
                                        + EvrComposite.this);
                            }
                            messageQueue.clear();
                            tracer.trace("EVR composite released lock in display messages run()");
                            return;
                        }
                        if (messageQueue.size() == 0) {
                            tracer.trace("No messages to display in "
                                    + EvrComposite.this);
                            if (stalled) {
                                tracer.error("No messages to display in "
                                        + EvrComposite.this);
                            }
                            tracer.trace("EVR composite released lock in display messages run()");
                            return;
                        }

                        if (paused) {
                            if (stalled) {
                                tracer.error("Display is paused in "
                                        + EvrComposite.this);
                            }
                            tracer.trace("EVR composite released lock in display messages run()");
                            return;
                        }

                        TableItem lastItem = null;

                        if (!EvrComposite.this.mainComposite.isVisible()) {
                            if (messageQueue.size() >= maxRows) {
                                tracer.trace("Throwing out "
                                        + String.valueOf(messageQueue.size()
                                                - maxRows) + " EVR messages");
                                if (stalled) {
                                    tracer.error("Throwing out "
                                            + String.valueOf(messageQueue.size()
                                                    - maxRows) + " EVR messages");
                                }
                                while (messageQueue.size() >= maxRows) {
                                    final IMessage m = messageQueue.poll(10,
                                            TimeUnit.MILLISECONDS);
                                    if (m == null) {
                                        tracer.error("Poll in non-visible logic failed in EVR view");
                                        break;
                                    }
                                }
                                tracer.trace("EVR composite released lock in display messages run()");
                                return;
                            }
                        }

                        if (stalled) {
                            tracer.error("Reached point 1 in "
                                    + EvrComposite.this);
                        }
                        final int messagesInQueue = messageQueue.size();
                        int addNewRows = 0;
                        int removeCount = 0;


                        table.setRedraw(false);

                        // If there are more messages queued than the
                        // maximum number of display
                        // rows, we'll remove everything from the table and
                        // display
                        // max rows new ones, starting with the oldest.
                        //
                        // If there are fewer messages queue than the
                        // maximum number to display,
                        // we'll remove enough rows from the table to make
                        // room for them and
                        // display all new messages, starting with the
                        // oldest
                        if (messagesInQueue >= maxRows) {
                            removeCount = EvrComposite.this.table
                                    .getItemCount();
                            if (stalled) {
                                tracer.error("Throwing out " + removeCount
                                        + " EVR messages from table");
                            }
                            tracer.trace("Throwing out " + removeCount
                                    + " EVR messages");
                            EvrComposite.this.clearView();
                            addNewRows = maxRows;
                        } else {
                            if (EvrComposite.this.table.getItemCount()
                                    + messagesInQueue >= maxRows) {
                                final int availRows = maxRows
                                        - EvrComposite.this.table
                                        .getItemCount();
                                if (availRows < messagesInQueue) {
                                    removeCount = messagesInQueue - availRows;
                                    if (removeCount > EvrComposite.this.table
                                            .getItemCount()) {
                                        removeCount = EvrComposite.this.table
                                                .getItemCount();
                                    }
                                }

                                if (stalled) {
                                    tracer.error("Removing " + removeCount
                                            + " EVR messages from table");
                                }
                                EvrComposite.this
                                .removeOldestEntries(removeCount);
                            }
                            addNewRows = Math.min(messagesInQueue, maxRows
                                    - EvrComposite.this.table.getItemCount());
                        }
                        tracer.trace("Adding " + addNewRows + " EVR messages");
                        if (stalled) {
                            tracer.error("Adding " + addNewRows
                                    + " EVR messages, queue size is "
                                    + messageQueue.size()
                                    + " and rows in table is "
                                    + EvrComposite.this.table.getItemCount());
                        }
                        if (addNewRows < 0) {
                            tracer.error("Number of rows to add in EVR View is less than 0");
                        }
                        for (int i = 0; i < addNewRows
                                && messageQueue.size() > 0; i++) {
                            final IMessage m = messageQueue.poll(10,
                                    TimeUnit.MILLISECONDS);
                            if (m != null) {
                                lastItem = addNewItem((IEvrMessage) m);
                            } else {
                                tracer.error("Poll in visible logic in EVR view failed");
                            }
                        }
                        table.setRedraw(true);

                        if (!paused && lastItem != null) {
                            EvrComposite.this.table.showItem(lastItem);
                        }

                        if (stalled) {
                            tracer.error("Done adding rows in "
                                    + EvrComposite.this + ", queue size is "
                                    + messageQueue.size()
                                    + " and rows in table is "
                                    + EvrComposite.this.table.getItemCount());
                        }

                    } catch (final Exception e) {
                        e.printStackTrace();
                    }
                }
                tracer.trace("EVR composite released lock in display messages run()");
            }
        });
        tracer.trace("Out of display messages");
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.monitor.guiapp.common.GeneralFlushListener#flushTimerFired()
     */
    @Override
    public void flushTimerFired() {
        displayMessages(true);
        tracer.trace(evrViewConfig.getViewName() + " After flush timer fired queue size is " +
                messageQueue.size());
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
     * @see jpl.gds.monitor.guiapp.gui.views.AbstractTableViewComposite#updateViewConfig()
     */
    @Override
    public void updateViewConfig() {
        evrViewConfig.setMaxRows(maxRows);
        evrViewConfig.setMarkColor(markColor.getRed() + "," + markColor.getGreen() + "," + markColor.getBlue());
        evrViewConfig.setLevels(selectedLevels);
        evrViewConfig.setSources(selectedSources);
        evrViewConfig.setModules(selectedModules);
        super.updateViewConfig();
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.monitor.guiapp.gui.views.AbstractTableViewComposite#replaceTableItems(org.eclipse.swt.widgets.TableItem[])
     */
    @Override
    protected void replaceTableItems(final TableItem[] items) {
        final String[] values = new String[tableDef.getActualColumnCount()];
        ArrayList<TableItem> newSelected = null;

        synchronized (tableAndMessageLock) {

            for (int i = 0; i < items.length; i++) {
                for (int valIndex = 0; valIndex < values.length; valIndex++) {
                    values[valIndex] = items[i].getText(valIndex);
                }
                final Object def = items[i].getData();
                final Color bg = items[i].getBackground();
                final Color fg = items[i].getForeground();

                final int oldIndex = table.indexOf(items[i]);
                final boolean selected = table.isSelected(oldIndex);

                final long oldTimestamp = getEntryTimestamp(items[i]);
                removeEntry(items[i]);
                items[i] = null;

                final TableItem item = createTableItem(oldTimestamp);
                item.setData(def);
                item.setText(values);
                item.setBackground(bg);
                item.setForeground(fg);

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
    
    /**
     * {@inheritDoc}
     * @see jpl.gds.monitor.guiapp.gui.views.AbstractTableViewComposite#getMarkedCount()
     */
    @Override
    public long getMarkedCount() {
        final TableItem[] items = EvrComposite.this.table.getItems();
        int markedCount = 0;

        for (int i = 0; i < items.length; i++)
        {
            final Boolean isMark = (Boolean)items[i].getData("isMark");
            if (isMark != null && isMark)
            {
                markedCount++;
            } 
        }
        return markedCount;
    }
}
