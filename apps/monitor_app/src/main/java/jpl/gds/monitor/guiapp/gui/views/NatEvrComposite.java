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
import java.util.List;
import java.util.Map;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.config.AbstractRegistryConfiguration;
import org.eclipse.nebula.widgets.nattable.config.CellConfigAttributes;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.layer.ILayer;
import org.eclipse.nebula.widgets.nattable.layer.LabelStack;
import org.eclipse.nebula.widgets.nattable.layer.cell.IConfigLabelAccumulator;
import org.eclipse.nebula.widgets.nattable.style.CellStyleAttributes;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.nebula.widgets.nattable.style.Style;
import org.eclipse.nebula.widgets.nattable.ui.NatEventData;
import org.eclipse.nebula.widgets.nattable.ui.menu.IMenuItemProvider;
import org.eclipse.nebula.widgets.nattable.ui.menu.IMenuItemState;
import org.eclipse.nebula.widgets.nattable.ui.menu.PopupMenuBuilder;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.springframework.context.ApplicationContext;

import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.matchers.Matcher;
import jpl.gds.evr.api.config.EvrProperties;
import jpl.gds.evr.api.util.EvrColorUtility;
import jpl.gds.monitor.config.MonitorDictionaryUtility;
import jpl.gds.monitor.guiapp.common.gui.IGlazedListHistorySubscriber;
import jpl.gds.monitor.guiapp.gui.ViewPreferencesShell;
import jpl.gds.monitor.guiapp.gui.views.nattable.BaseCellLabelAccumulator;
import jpl.gds.monitor.guiapp.gui.views.nattable.EvrEventSubscriber;
import jpl.gds.monitor.guiapp.gui.views.nattable.EvrNatListItem;
import jpl.gds.monitor.guiapp.gui.views.preferences.NatEvrPreferencesShell;
import jpl.gds.monitor.perspective.view.NatEvrViewConfiguration;
import jpl.gds.monitor.perspective.view.RealtimeRecordedFilterType;
import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.swt.ChillColorCreator;
import jpl.gds.shared.swt.TextViewShell;
import jpl.gds.shared.swt.types.ChillColor;

/**
 * NatEvrComposite is the GUI widget responsible for displaying a scrolling
 * table of EVR messages, implemented using a NAT Table widget with a Glazed
 * list data feed. It is considered a monitor view. The data items it displays
 * are EvrNatListItem objects. Its IGlazedListHistorySubscriber object is
 * EvrEventSubscriber.
 * 
 * <p><br>
 * All instances of this class shared a single data feed in the form of a
 * glazed event list that has a length limit. The first instantiation of this
 * class creates the BoundedGlazedListEvent object, which subscribes to the
 * GeneralMessageDistributor for EVR messages. Thereafter, new instances just
 * use the same data provider. It is therefore VERY IMPORTANT that nothing be
 * done in this class to modify the underlying event list.
 *
 */
public class NatEvrComposite extends AbstractNatTableViewComposite<EvrNatListItem> {
    
    private static final String DEFINITION_MENU_KEY = "ShowEvrDefinition";

    /**
     * Evr composite title.
     */
    public static final String TITLE = "EVR";

    /**
     * This is the subscriber for incoming EVR messages. It is shared among all
     * NatEvrComposite instances. The first instantiation call creates a single
     * subscription to the message distributor.
     */
    private final IGlazedListHistorySubscriber<EvrNatListItem> subscriber;

    /**
     * EVR Level filter list.
     */
    protected List<String> selectedLevels = new ArrayList<>();

    /**
     * EVR sources filter list.
     */
    private final List<String> selectedSources = new ArrayList<>();

    /**
     * EVR modules filter list.
     */
    private final List<String> selectedModules = new ArrayList<>();

    /**
     * EVR view configuration.
     */
    private final NatEvrViewConfiguration evrViewConfig;

    /**
     * Glazed filter list matcher that matches records using the current filters
     * set in the view configuration.
     */
    private Matcher<EvrNatListItem> viewFilterMatcher;

	private static EvrColorUtility evrColorUtil;

    private final Tracer                       trace;

    /**
     * Creates an instance of NatEvrComposite.
     * @param appContext the current application context
     * 
     * @param config
     *            the EvrViewConfiguration object containing display settings
     */
    public NatEvrComposite(final ApplicationContext appContext, final IViewConfiguration config) {
        super(appContext, config);

        if (evrColorUtil == null) {
            evrColorUtil = new EvrColorUtility(appContext.getBean(EvrProperties.class));
        }
        subscriber = appContext.getBean(EvrEventSubscriber.class);
        
        evrViewConfig = (NatEvrViewConfiguration) config;
        trace = TraceManager.getTracer(appContext, Loggers.DEFAULT);
        /* Do this now so it will be correct when the static filters are added 
         * for the first time.
         */
        updateFilterLists();
        setTableDefinition(evrViewConfig
                .getTable(NatEvrViewConfiguration.EVR_TABLE_NAME));
        setMarkAttributes(new ChillColor(evrViewConfig.getMarkColor()));
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.monitor.guiapp.gui.views.AbstractNatTableViewComposite#init(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void init(final Composite parent) {
        super.init(parent);
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
     * 
     * @see jpl.gds.monitor.guiapp.gui.views.AbstractTableViewComposite#updateViewConfig()
     */
    @Override
    public void updateViewConfig() {
        super.updateViewConfig();
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.monitor.guiapp.gui.views.AbstractNatTableViewComposite#createTable()
     */
    @Override
    protected void createTable() {
        super.createTable();

        eventList.setMaxSize(evrViewConfig.getMaxRows());

        /* Add configuration needed for EVR coloration. */
        table.addConfiguration(new AbstractRegistryConfiguration() {
            @Override
            public void configureRegistry(final IConfigRegistry configRegistry) {

                /* Get all the configured EVR levels */
                final Map<String, ChillColor> bgColors = evrColorUtil.getBackgroundColorsForAllLevels();
                final Map<String, ChillColor> fgColors = evrColorUtil.getForegroundColorsForAllLevels();

                /*
                 * This bit registers the the cell styles that are used to color
                 * rows based upon their level label. The last argument to the
                 * register call is the label name corresponding to the cell
                 * style being registered.
                 */
                for (final String level : bgColors.keySet()) {

                    final Style cellStyle = new Style();
                    final Color bgCol = ChillColorCreator.getColor(bgColors.get(level));
                    final Color fgCol = ChillColorCreator.getColor(fgColors.get(level));
                    cellStyle.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, bgCol);
                    cellStyle.setAttributeValue(CellStyleAttributes.FOREGROUND_COLOR, fgCol);
                    configRegistry.registerConfigAttribute(
                            CellConfigAttributes.CELL_STYLE, cellStyle,
                            DisplayMode.NORMAL, level);
                }

            }
        });

        this.table.configure();

    }

    
    /**
     * {@inheritDoc}
     * @see jpl.gds.monitor.guiapp.gui.views.AbstractNatTableViewComposite#getEventSubscriber()
     */
    @Override
    protected IGlazedListHistorySubscriber getEventSubscriber() {
        /* All NAT EVR composites share the same underlying subscriber. */
        return subscriber;
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.monitor.guiapp.gui.views.AbstractNatTableViewComposite#createCellLabelAccumulator(org.eclipse.nebula.widgets.nattable.layer.ILayer,
     *      ca.odell.glazedlists.SortedList)
     */
    @Override
    protected IConfigLabelAccumulator createCellLabelAccumulator(
            final ILayer bodyLayer,
            final SortedList<EvrNatListItem> sortedEventList) {
        
        /* Extend the BaseCellLabelAccumulator to add the EVR level config label
         * acccumulator. This will cause the EVRs to be colorized by level.
         */
        return new BaseCellLabelAccumulator<EvrNatListItem>(bodyLayer, sortedEventList) {
            /**
             * {@inheritDoc}
             * 
             * @see jpl.gds.monitor.guiapp.gui.views.nattable.BaseCellLabelAccumulator#accumulateConfigLabels(org.eclipse.nebula.widgets.nattable.layer.LabelStack,
             *      int, int)
             */
            @Override
            public void accumulateConfigLabels(final LabelStack configLabels,
                    final int columnPosition, final int rowPosition) {

                /* The super class applies color coding for EVR marking */
                super.accumulateConfigLabels(configLabels, columnPosition,
                        rowPosition);

                final int rowIndex = bodyLayer.getRowIndexByPosition(rowPosition);
                final EvrNatListItem evr = sortedEventList.get(rowIndex);

                /*
                 * Apply EVR color coding to this row, but only if the EVR is
                 * not marked. The mark color takes precedence. The color coding
                 * happens by applying a label to the record, which matches the
                 * EVR level. This label in turn matches a registered
                 * configuration for cell style.
                 */
                if (!evr.isMarked()) {
                    final String level = evr.getLEVEL();
                    if (!level.isEmpty()) {
                        if (evrViewConfig.isUseColorCoding()) {
                            configLabels.addLabel(evr.getLEVEL());
                        } else {
                            configLabels.removeLabel(level);
                        }
                    }
                }

            }
        };
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.monitor.guiapp.gui.views.AbstractNatTableViewComposite#addStaticFilters()
     */
    @Override
    protected void addStaticFilters() {
        super.addStaticFilters();

        /*
         * Create the view filter matcher, which will match rows in the sorted
         * glazed list based upon all the filter settings in the current view
         * configuration.
         */
        viewFilterMatcher = new Matcher<EvrNatListItem>() {
            @Override
            public boolean matches(final EvrNatListItem item) {

                /* Filter for level, module, and source. */
                boolean matches = (selectedLevels.isEmpty() || selectedLevels
                        .contains(item.getLEVEL()));
                matches = matches
                        && (selectedSources.isEmpty() || selectedSources
                                .contains(item.getSOURCE()));
                matches = matches
                        && (selectedModules.isEmpty() || selectedModules
                                .contains(item.getMODULE()));

                /* Filter for RT/REC status. */
                final RealtimeRecordedFilterType rtFilter = evrViewConfig
                        .getRealtimeRecordedFilterType();
                matches = matches
                        && (rtFilter == RealtimeRecordedFilterType.BOTH
                        || (rtFilter == RealtimeRecordedFilterType.RECORDED && item
                        .getRecordedAsFlag()) || (rtFilter == RealtimeRecordedFilterType.REALTIME && !item
                        .getRecordedAsFlag()));

                return matches;
            }
        };

        /*
         * Add a matcher editor for the view filter to the overall list for
         * matcher editors.
         */
        addMatcherEditor(viewFilterMatcher);

    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.monitor.guiapp.gui.views.AbstractTableViewComposite#createGui()
     */
    @Override
    protected void createGui() {

        mainComposite = new Composite(parent, SWT.NONE);
        final GridLayout gridLayout = new GridLayout(1, false);
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        gridLayout.horizontalSpacing = 0;
        gridLayout.verticalSpacing = 0;
        mainComposite.setLayout(gridLayout);

        /* Creates the Nattable */
        createTable();

        /*
         * The table is supposed to fill the whole composite except for the
         * status line. Unfortunately, this does not mean that the column widths
         * expand to fill the available space.
         */
        GridDataFactory.fillDefaults().grab(true, true).applyTo(table);
        
        /*
         * Add the view status line composite below the table.
         */
        addStatusLine();
        
        /*
         * Set view-specific filters. This was done in the constructor so the
         * status filters would be correct during table configuration, but needs
         * to be done again to update the status line.
         */
        updateFilterLists();
        
        /*
         * This view is filtered for FSW/SSE sources. Set the status line from current sources.
         */
        this.statusLine.setSources(selectedSources);
        
        /*
         * This view filters for RT/Recorded. Set the status line indicating from current
         * rt/rec filter type.
         */
        this.statusLine.setRealtimeRecorded(evrViewConfig.getRealtimeRecordedFilterType());
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.monitor.guiapp.gui.views.AbstractNatTableViewComposite#addCustomBodyMenuItems(org.eclipse.nebula.widgets.nattable.ui.menu.PopupMenuBuilder)
     */
    @Override
    protected PopupMenuBuilder addCustomBodyMenuItems(
            final PopupMenuBuilder menuBuilder) {
        /*
         * The only additional menu item on the nattable menu beyond that added
         * by the superclass is an item to show the dictionary definition of the
         * selected EVR.
         */
        return menuBuilder.withMenuItemProvider(DEFINITION_MENU_KEY,
                getShowDefinitionMenuItemProvider()).withEnabledState(
                        DEFINITION_MENU_KEY, new IMenuItemState() {

                            /**
                             * {@inheritDoc}
                             * 
                             * @see org.eclipse.nebula.widgets.nattable.ui.menu.IMenuItemState#isActive(org.eclipse.nebula.widgets.nattable.ui.NatEventData)
                             */
                            @Override
                            public boolean isActive(final NatEventData natEventData) {
                                /* This menu item active if 1 row is selected. */
                                return selectedItems.size() == 1;
                            }
                        });

    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.monitor.guiapp.gui.views.AbstractNatTableViewComposite#createPreferencesShell()
     */
    @Override
    protected ViewPreferencesShell createPreferencesShell() {
        return new NatEvrPreferencesShell(appContext, mainComposite.getShell());
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.monitor.guiapp.gui.views.AbstractNatTableViewComposite#updateFromConfigChange()
     */
    @Override
    protected void updateFromConfigChange() {
        /*
         * For now, mark color is set here. It cannot be set by the superclass
         * because mark color is not a generally-available view configuration
         * property.
         */
        setMarkAttributes(new ChillColor(evrViewConfig.getMarkColor()));

        /*
         * Update other filter lists from the view config and reset the glazed
         * list filters.
         */
        updateFilterLists();
        resetStaticFilters();
        
        /*
         * Update the status line with the current rt/rec filter type and current sources.
         */
        this.statusLine.setSources(selectedSources);
        this.statusLine.setRealtimeRecorded(evrViewConfig.getRealtimeRecordedFilterType());
        eventList.setMaxSize(evrViewConfig.getMaxRows());

        super.updateFromConfigChange();

    }

    /**
     * Creates the menu item provider for the NAT table menu for showing EVR
     * dictionary definitions.
     * 
     * @return IMenuItemProvider
     */
    private IMenuItemProvider getShowDefinitionMenuItemProvider() {
        return new IMenuItemProvider() {

            @Override
            public void addMenuItem(final NatTable natTable,
                    final Menu popupMenu) {
                final MenuItem showDefMenuItem = new MenuItem(popupMenu, SWT.PUSH);
                showDefMenuItem.setText("Show Dictionary Definition");

                showDefMenuItem.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(final SelectionEvent e) {
                        try {
                            /* Only works if one item selected */
                            if (selectedItems.size() != 1) {
                                return;
                            }

                            /*
                             * Get the formatted EVR definition text for the
                             * selected record
                             */
                            final EvrNatListItem evrItem = selectedItems.get(0);
                            final String text = appContext.getBean(MonitorDictionaryUtility.class)
                                    .getEvrText(evrItem.getEvr());
                            /* Display it in a text shell */
                            final TextViewShell tvs = new TextViewShell(table
                                    .getShell(), evrItem.getRecordIdString()
                                    + " Definition", SWT.BORDER | SWT.V_SCROLL
                                            | SWT.WRAP,
                                    trace);
                            tvs.getShell().setSize(500, 400);
                            tvs.setText(text);
                            tvs.open();
                        } catch (final Exception ex) {
                            ex.printStackTrace();
                            trace.error(
                                    "Error in show definition menu item handling "
                                            + ex.toString());
                        }

                    }
                });
            }
        };
    }

    /**
     * Updates local filter lists based upon current view configuration.
     */
    private void updateFilterLists() {
        selectedLevels.clear();
        if (evrViewConfig.getLevels() != null) {
            selectedLevels.addAll(evrViewConfig.getLevels());
        }
        selectedSources.clear();
        if (evrViewConfig.getSources() != null) {
            selectedSources.addAll(evrViewConfig.getSources());
        }
        selectedModules.clear();
        if (evrViewConfig.getModules() != null) {
            selectedModules.addAll(evrViewConfig.getModules());
        }
       
        /*
         * Update the status line to indicate filter status. Note that sources
         * and rt/recorder filter are not included in this assessment, since
         * they are generally set on every view configuration and are mostly
         * static.
         */
        setFiltered(!selectedModules.isEmpty() || !selectedLevels.isEmpty());

    }

    /* Leave for now. A method to create test rows if needed. 
    private void createTestData() {
        List<IMessage> evrList = new LinkedList<IMessage>();
        
        SessionIdentification id = new SessionIdentification();

        for (int i = 0; i < 10; i++) {
            EvrMessage msg = new EvrMessage();
            msg.setSessionId(id);

            IEvr e = new Evr();
            msg.setEvr(e);
            try {
                e.setEvrDefinition(EvrDefinitionFactory.createEvrDefinition());
            } catch (DictionaryException x) {
                // TODO Auto-generated catch block
                x.printStackTrace();
            }

            e.setMessage("Howdy There! We are trying out the NatTable for making EVR views.");
            e.setSclk(new Sclk(i));
            e.setScet(new AccurateDateTime(i));
            e.setErt(new AccurateDateTime(i));
            e.setSol(LocalSolarTimeFactory.getNewLst());
            e.getEvrDefinition().setId(123456 + i);
            e.getEvrDefinition().setLevel("DIAGNOSTIC");
            e.getEvrDefinition().setModule("My Module " + i);
            e.getEvrDefinition().setOpsCategory("My Cat " + i);
            e.getEvrDefinition().setName(
                    "LONG_LONG_EVR_NAME_GENERIC " + i);

            e.getMetadata()
            .addKeyValue(EvrMetadataKeywordEnum.TASKNAME, "TASK");
            e.getMetadata().addKeyValue(
                    EvrMetadataKeywordEnum.CATEGORYSEQUENCEID,
                    String.valueOf(5 + i));
            e.getMetadata().addKeyValue(EvrMetadataKeywordEnum.SEQUENCEID,
                    String.valueOf(1 + i));
            e.setRealtime(true);

            evrList.add(msg);
        }
        IMessage[] msgArray = new IMessage[evrList.size()];
        this.subscriber.messageReceived(evrList.toArray(msgArray));

    };
    */

}
