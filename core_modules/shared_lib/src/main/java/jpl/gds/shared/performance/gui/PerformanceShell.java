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
package jpl.gds.shared.performance.gui;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import jpl.gds.shared.config.PerformanceProperties;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.performance.HealthStatus;
import jpl.gds.shared.performance.HeapPerformanceData;
import jpl.gds.shared.performance.IPerformanceData;
import jpl.gds.shared.performance.ProviderPerformanceSummary;
import jpl.gds.shared.performance.QueuePerformanceData;
import jpl.gds.shared.swt.ChillColorCreator;
import jpl.gds.shared.swt.ChillShell;
import jpl.gds.shared.swt.types.ChillColor;

/**
 * This is a non-modal dialog that displays performance information from
 * multiple performance data providers, as well as heap performance information.
 * It is populated from IPerformanceData objects.
 * 
 *
 */
public class PerformanceShell implements ChillShell {
    private static final String TITLE = "Performance Details";

    private static final Color NONE = ChillColorCreator
            .getColor(new ChillColor(ChillColor.ColorName.BLUE));
    private static final Color GREEN = ChillColorCreator
            .getColor(new ChillColor(ChillColor.ColorName.GREEN));
    private static final Color YELLOW = ChillColorCreator
            .getColor(new ChillColor(ChillColor.ColorName.YELLOW));
    private static final Color RED = ChillColorCreator.getColor(new ChillColor(
            ChillColor.ColorName.RED));
    
    /** The shell widget itself */
    private Shell mainShell = null;
    
    /** Map of health group composites keyed by provider name. */
    private final Map<String, ProviderHealthGroup> healthGroups = new HashMap<String, ProviderHealthGroup>();
    
    /** Containing health group composites. */
    private Composite healthGroupComp;
    
    /** The health group composite for the heap */
    private HeapHealthGroup heapGroup;
    
    /** The last health group added */
    private Group lastHealthGroup;
    
    /**
     * Creates a PerformanceShell with a Display parent.
     * 
     * @param parent
     *            the parent Display
     * @param trace
     *            the log tracer
     */
    public PerformanceShell(final Display parent, Tracer trace) {
        mainShell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE);
        createGui(trace);
    }
    
    /**
     * Creates a PerformanceShell with a Shell parent.
     * 
     * @param parent
     *            the parent Shell widget
     * @param trace
     *            the log tracer
     */
    public PerformanceShell(final Shell parent, Tracer trace) {
        mainShell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE);
        createGui(trace);
    }

    /** Creates the actual Gui components. 
     * @param Trace The application log tracer
     * */
    protected void createGui(Tracer trace) {
        mainShell.setText(TITLE);
        
        /* The main shell contains one composite that fills the whole shell. */ 
        mainShell.setLayout(new FormLayout());

        Composite mainComp = new Composite(mainShell, SWT.NONE);
        FormData fd = new FormData();
        fd.top = new FormAttachment(0);
        fd.right = new FormAttachment(100);
        fd.left = new FormAttachment(0);
        fd.bottom = new FormAttachment(100);
        mainComp.setLayoutData(fd);
        
        /* Main composite has a form layout.*/
        mainComp.setLayout(new FormLayout());
        
        /*
         * The heap group is attached at the top and right. This contains the
         * heap health. This group is populated using a default
         * HeapPerformacneGroup.
         */
        this.heapGroup = new HeapHealthGroup(mainComp);
        FormData hgfd = new FormData();
        hgfd.top = new FormAttachment(0, 3);
        hgfd.left = new FormAttachment(0);
        this.heapGroup.getGroup().setLayoutData(hgfd);
        /* 
         * R8 Refactor - I think it is ok to use a unique properties object here. All
         * this does is populate the GUI until a real heap performance object arrives.
         */
        this.heapGroup.setPerformanceData(new HeapPerformanceData(new PerformanceProperties()));
        
        /*
         * The health group composite is attached to the heap group on the top
         * and to both sides of the main composite, so it will space the whole
         * window. This composite will contain all the other health groups added
         * dynamically to the window.
         */
        healthGroupComp = new Composite(mainComp, SWT.NONE);
        healthGroupComp.setLayout(new FormLayout());    
        FormData ggfd = new FormData();
        ggfd.top = new FormAttachment(this.heapGroup.getGroup(), 3);
        ggfd.right = new FormAttachment(100);
        ggfd.left = new FormAttachment(0);
        healthGroupComp.setLayoutData(ggfd);

        /*
         * The button composite is attached to the health group composite at the
         * top, and to the bottom and right side of the form, right-justifying
         * the buttons.
         */
        Composite buttonComp = new Composite(mainComp, SWT.NONE);
        buttonComp.setLayout(new RowLayout());        
        FormData bcfd = new FormData();
        bcfd.top = new FormAttachment(healthGroupComp, 10);
        bcfd.right = new FormAttachment(100);
        bcfd.bottom = new FormAttachment(100);
        buttonComp.setLayoutData(bcfd);

        /* One button: Close. */
        Button closeButton = new Button(buttonComp, SWT.PUSH);
        closeButton.setText("Close");
        closeButton.addSelectionListener(new SelectionListener() {

            /**
             * {@inheritDoc}
             * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
             */
            @Override
            public void widgetDefaultSelected(SelectionEvent arg0) {
                // do nothing

            }

            /**
             * {@inheritDoc}
             * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                mainShell.close();

            }

        });

        healthGroupComp.pack();
        buttonComp.pack();
        mainComp.pack();
        mainShell.pack();
        mainShell.setSize(mainShell.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.swt.ChillShell#getShell()
     */
    @Override
    public Shell getShell() {
        return mainShell;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.swt.ChillShell#getTitle()
     */
    @Override
    public String getTitle() {
        return TITLE;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.swt.ChillShell#open()
     */
    @Override
    public void open() {
        mainShell.open();

    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.swt.ChillShell#wasCanceled()
     */
    @Override
    public boolean wasCanceled() {
        return false;
    }

    /** 
     * Updates the heap performance display from the supplied performance
     * object.
     * 
     * @param heapData performance object containing updated heap data
     */
    public synchronized void setHeapPerformance(HeapPerformanceData heapData) {
        this.heapGroup.setPerformanceData(heapData);
    }
    
    /**
     * Adds or updates a provider health group from the given provider
     * performance data. 
     *  
     * @param sum ProviderPerformanceSummary containing performance data
     */
    public synchronized void setPerformanceData(ProviderPerformanceSummary sum) {
        
        /* Health groups are kept in a map by provider name. If there isn't
         * one, create it.
         */
        ProviderHealthGroup group = healthGroups.get(sum.getProviderName());
        if (group == null) {
            group = addHealthGroup(sum);
        }
        /* Update the group with the current data. */
        group.setPerformanceData(sum);
        
        mainShell.pack();
        mainShell.setSize(mainShell.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    }

    /**
     * Adds a new provider health group to the window, given a performance summary object
     * for the provider.
     * 
     * @param sum ProviderPerformanceSummary object for the provider
     * @return the new GUI group for the provider
     */
    private ProviderHealthGroup addHealthGroup(ProviderPerformanceSummary sum) {
        ProviderHealthGroup group = new ProviderHealthGroup(healthGroupComp,
                sum.getProviderName());
        
        /* 
         * The group is attached to both sides of the health group form, and to the last 
         * health group in the top.
         */
        FormData fd = new FormData();
        fd.left = new FormAttachment(0);
        fd.right = new FormAttachment(100);
        if (this.lastHealthGroup == null) {
            fd.top = new FormAttachment(0, 3);
        } else {
            fd.top = new FormAttachment(this.lastHealthGroup, 3);
        }
        group.getGroup().setLayoutData(fd);
        this.lastHealthGroup = group.getGroup();
        
        healthGroupComp.pack();
        mainShell.pack();
        mainShell.setSize(mainShell.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        return group;

    }

    /**
     * Gets the background color used for display of a specific health status.
     * 
     * @param status the health status to get color for
     * @return Color
     */
    private Color getBackgroundForStatus(HealthStatus status) {
        switch (status) {
        case GREEN:
            return GREEN;
        case NONE:
            return NONE;
        case RED:
            return RED;
        case YELLOW:
            return YELLOW;
        default:
            break;
            
        }
        return null;
    }

    /**
     * Wraps a group composite that displays the health information for a 
     * performance provider.
     * 
     */
    public class ProviderHealthGroup {
        private final Group group;
        private final HealthBarTableComposite healthTable;
        private final ProviderHealthSummaryComposite summaryComp;
  
        /**
         * Constructor. Adds the new group to the map of groups by provider name.
         * 
         * @param parent the parent composite for the group
         * @param providerName the name of the performance provider this
         *        group is for
         */
        public ProviderHealthGroup(Composite parent, String providerName) {
            group = new Group(parent, SWT.NONE);

            /* The group takes a horizontal layout. */
            group.setText("Health of " + providerName + ":");
            RowLayout rl = new RowLayout();
            rl.wrap = false;
            rl.justify = false;
            rl.pack = true;
            group.setLayout(rl);

            /* First thing on the row is the summary composite. */
            summaryComp = new ProviderHealthSummaryComposite(this.group);

            /* The health bar table occupies the rest of the row, */
            healthTable = new HealthBarTableComposite(this.group);
            healthTable.init();

            group.pack();

            /* Add this group to the map of groups by provider name */
            healthGroups.put(providerName, this);
        }

        /**
         * Updates the group from a current provider performance summary.
         * 
         * @param sum ProviderPerformanceSummary object
         */
        public void setPerformanceData(ProviderPerformanceSummary sum) {
            
            healthTable.clearRowData();
            for (IPerformanceData perf : sum.getPerformanceData()) {
                if (perf instanceof QueuePerformanceData) {
                    QueuePerformanceData qpd = (QueuePerformanceData) perf;
                    healthTable.addOrUpdateRow(qpd);

                }
            }
            summaryComp.setPerformanceData(sum);

        }
        
        /**
         * Gets the group composite this object wraps.
         * 
         * @return GUI Group object
         */
        public Group getGroup() {
           return this.group;
        }
    }
    
    /**
     * Wraps a group composite that displays the heap performance information.
     * 
     */
    public class HeapHealthGroup {
        private final Group group;
        private final HealthBarTableComposite healthTable;
        private final HeapHealthSummaryComposite summaryComp;

        /**
         * Constructor.
         * @param parent the parent Composite
         */
        public HeapHealthGroup(Composite parent) {
            group = new Group(parent, SWT.NONE);

            group.setText("Health of Heap Memory:");
            RowLayout rl = new RowLayout();


            rl.wrap = false;
            rl.justify = true;
            rl.pack = true;
            group.setLayout(rl);

            summaryComp = new HeapHealthSummaryComposite(this.group);

            healthTable = new HealthBarTableComposite(this.group);

            healthTable.init();
            group.pack();
            group.setSize(group.computeSize(SWT.DEFAULT, SWT.DEFAULT));

        }

        /**
         * Sets new heap performance data and updates the GUI elements from it.
         * 
         * @param perf current HeapPeformanceData object
         */
        public void setPerformanceData(HeapPerformanceData perf) {
            healthTable.addOrUpdateRow(perf);
            summaryComp.setPerformanceData(perf);
            group.pack();

        }
        
        /**
         * Gets the GUI group object this class wraps.
         * 
         * @return Group
         */
        public Group getGroup() {
            return this.group;
         }
    }

    /**
     * A composite that summarizes provider performance information.
     * 
     */
    public class ProviderHealthSummaryComposite extends Composite {
         
        private final Label healthLabel;
        private final Label throttleLabel;
        private final Label backlogLabel;
        private final Label backlogTotalLabel;
        
        /**
         * Constructor.
         * 
         * @param parent the parent composite
         */
        public ProviderHealthSummaryComposite(Composite parent) {
            super(parent, SWT.NONE);
            RowLayout rl = new RowLayout(SWT.VERTICAL);
            rl.wrap = false;
            rl.marginRight = 5;

            setLayout(rl);
            
            Composite statusComp = new Composite(this, SWT.NONE);
            rl = new RowLayout();
            statusComp.setLayout(rl);
            rl.marginBottom = 0;
            rl.marginTop = 0;
            rl.marginLeft = 0;
            rl.marginRight = 5;
            rl.spacing = 0;
            Label tempLabel = new Label(statusComp, SWT.LEFT);
            tempLabel.setText("Overall Status: ");
            
            healthLabel = new Label(statusComp, SWT.LEFT);
            healthLabel.setText(HealthStatus.NONE.toString());
            throttleLabel = new Label(this, SWT.LEFT);
            throttleLabel.setText("Throttling: No");
            backlogLabel = new Label(this, SWT.LEFT);
            backlogLabel.setText("Backlogging: No");
            backlogTotalLabel = new Label(this, SWT.LEFT);

            pack();
            setSize(computeSize(SWT.DEFAULT, SWT.DEFAULT));

        }

        /**
         * Updates performance data for the provider this composite belongs to.
         * 
         * @param sum current ProviderPerformanceSummary object for the provider
         */
        public void setPerformanceData(ProviderPerformanceSummary sum) {
            healthLabel.setText(sum.getOverallHealth().toString());
            healthLabel.setBackground(getBackgroundForStatus(sum.getOverallHealth()));
            throttleLabel.setText("Throttling: "
                    + (sum.isThrottling() ? "Yes" : "No"));
            backlogLabel.setText("Backlogging: "
                    + (sum.isBacklogging() ? "Yes" : "No"));
            long totalBacklog = 0;
            
            if (sum.isBacklogging()) {
                for (IPerformanceData perf : sum.getPerformanceData()) {
                    if (perf instanceof QueuePerformanceData) {
                        QueuePerformanceData qpd = (QueuePerformanceData) perf;
                        if (qpd.isBacklog()) {
                            totalBacklog += qpd.getCurrentQueueSize();
                        }

                    }
                }
                backlogTotalLabel.setText("Backlog: " + totalBacklog);

            } else {
                backlogTotalLabel.setText("");
            }

            pack();
            setSize(computeSize(SWT.DEFAULT, SWT.DEFAULT));
            /*
             * Added layout call to redraw gui dimensions
             */
            layout();
        }
      
    }
    
    /**
     * A composite that summarizes heap performance information.
     * 
     */
    public class HeapHealthSummaryComposite extends Composite {

        private final Label healthLabel;
       
        /**
         * Constructor.
         * 
         * @param parent the parent composite
         */
        public HeapHealthSummaryComposite(Composite parent) {
            super(parent, SWT.NONE);
            RowLayout rl = new RowLayout(SWT.VERTICAL);
            rl.wrap = false;

            setLayout(rl);           
            
            Composite statusComp = new Composite(this, SWT.NONE);
            rl = new RowLayout();
            statusComp.setLayout(rl);
            rl.marginBottom = 0;
            rl.marginTop = 0;
            rl.marginLeft = 0;
            rl.marginRight = 0;
            rl.spacing = 0;
            Label tempLabel = new Label(statusComp, SWT.LEFT);
            tempLabel.setText("Overall Status: ");
            
            healthLabel = new Label(statusComp, SWT.LEFT);
            healthLabel.setText(HealthStatus.NONE.toString());
            
            pack();
            setSize(computeSize(SWT.DEFAULT, SWT.DEFAULT));
            
        }


        /**
         * Updates performance data from the given heap performance object.
         * 
         * @param heapData current HeapPerformanceData object
         */
        public void setPerformanceData(HeapPerformanceData heapData) {
            healthLabel.setText(heapData.getHealthStatus().toString());
            healthLabel.setBackground(getBackgroundForStatus(heapData.getHealthStatus()));

            setSize(computeSize(SWT.DEFAULT, SWT.DEFAULT));
            /*
             * Added layout call to redraw gui elements
             */
            layout();
        }

    }

}
