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
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import jpl.gds.shared.performance.HealthStatus;
import jpl.gds.shared.performance.HeapPerformanceData;
import jpl.gds.shared.performance.IPerformanceData;
import jpl.gds.shared.performance.QueuePerformanceData;
import jpl.gds.shared.swt.ChillFontCreator;
import jpl.gds.shared.swt.types.ChillFont;

/**
 * A GUI widget that wraps a HealthStatusBarComposites, each representing the
 * current status and health of a single component, in a scrolling composite,
 * using a three column format. The columns in the table are the component name,
 * an embedded health status bar widget, and performance notes. It resembles a
 * table widget in appearance, but is not a table widget, for reasons that make
 * me curse profusely and colorfully about SWT in general.
 * 
 */
public class HealthBarTableComposite extends ScrolledComposite {

    /** Width of health bars in the second column. */
    private static final int DEFAULT_BAR_WIDTH = 250;

    /** Default queue maximum for unbounded queues with no RED bound. */
    private static final int DEFAULT_QUEUE_MAX = 100;

    /** The table of health bars */
    private Composite barTable;

    /** A map of the table items already in the table, keyed by component name. */
    private final Map<String, HealthStatusBarComposite> rowBarMap = new HashMap<String, HealthStatusBarComposite>();

    /** A map of the table items already in the table, keyed by component name. */
    private final Map<String, Label> rowLabelMap = new HashMap<String, Label>();

    /**
     * The font used for label text in the table. The default font is too large.
     */
    private static final Font tableFont = ChillFontCreator
            .getFont(new ChillFont(ChillFont.DEFAULT_FACE,
                    ChillFont.FontSize.SMALL, SWT.NONE));

    /** Flag indicating if the object has been fully initialized */
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    /**
     * Constructor. Must call init() to complete initialization of the object.
     * 
     * @param parent
     *            the parent composite
     */
    public HealthBarTableComposite(Composite parent) {
        super(parent, SWT.V_SCROLL | SWT.H_SCROLL);
    }

    /**
     * Initializes the object.
     */
    public void init() {
        if (!initialized.getAndSet(true)) {
            createGui();
        }
    }

    /**
     * Creates the actual GUI components.
     */
    protected void createGui() {

        setLayout(new FillLayout());

        /*
         * barTable is the top level composite. Its components are layed out in
         * a 3 column grid.
         */
        barTable = new Composite(this, SWT.NONE | SWT.BORDER);
        GridLayout gd = new GridLayout();
        gd.numColumns = 3;
        gd.verticalSpacing = 0;
        gd.marginBottom = 0;
        barTable.setLayout(gd);

        /*
         * There is nothing in the grid to start with. The top composite gets
         * put into the scrolled composite (this).
         */

        setContent(barTable);
        setExpandHorizontal(true);
        setExpandVertical(true);
        setMinSize(barTable.computeSize(SWT.DEFAULT, SWT.DEFAULT));

        pack();

    }

    /**
     * Adds or updates a row in the component table (grid) for the supplied
     * performance data, depending on whether a row for the component name
     * already exists. Currently, a row can be added for HeapPerformanceData and
     * QueuePerformanceData objects. Any other type of IPerformanceData object
     * will be ignored and this method will do nothing.
     * 
     * @param rowData
     *            an IPerformanceData object for a component
     */
    public synchronized void addOrUpdateRow(IPerformanceData rowData) {

        if (!initialized.get()) {
            throw new IllegalStateException(
                    "Object instance has not been initialized");
        }

        /* If the component already has an associated row, we want to update it. */
        if (this.rowBarMap.get(rowData.getComponentName()) != null) {
            updateRow(rowData);

        } else {
            addRow(rowData);
        }
    }

    /**
     * Adds a row from an IPerformanceData object.
     * 
     * @param rowData
     *            IPerformanceData to create the row from
     */
    private void addRow(IPerformanceData rowData) {

        /*
         * Add the component name as a label. This will go into the first column
         * of the three-column grid.
         */
        Label nameLabel = new Label(barTable, SWT.RIGHT);
        nameLabel.setText(rowData.getComponentName() + ":");
        nameLabel.setFont(tableFont);
        GridData gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalAlignment = SWT.RIGHT;
        nameLabel.setLayoutData(gd);

        /*
         * Create a HealthBarStatus composite, which becomes the second column
         * of the grid row.
         * 
         * For bounded queues and heap, create the default
         * HealthStatusBarComposite. This expresses state as a percentage of max
         * volume.
         * 
         * Unbounded queues are trickier. They have no max length. Use an
         * UnboundedHealthStatusBarComposite. However, this still needs a
         * maximum in order to draw the status bar. If there is a red bound on
         * the queue, we use the red bound. If there is not (red checking
         * disabled, there is little we can do. In this case, I just default the
         * maximum queue length to a constant.
         */
        HealthStatusBarComposite bar = null;
        if (rowData instanceof QueuePerformanceData) {
            QueuePerformanceData queueData = (QueuePerformanceData) rowData;
            if (queueData.isBounded()) {
                bar = new HealthStatusBarComposite(barTable,
                        (int) queueData.getMaxQueueSize(), DEFAULT_BAR_WIDTH,
                        queueData.getUnits());
            } else {
                bar = new UnboundedHealthStatusBarComposite(barTable,
                        queueData.getRedBound() == 0 ? DEFAULT_QUEUE_MAX
                                : (int) queueData.getRedBound(),
                        DEFAULT_BAR_WIDTH, queueData.getUnits(), false);
            }
        } else if (rowData instanceof HeapPerformanceData) {
            HeapPerformanceData heapData = (HeapPerformanceData) rowData;
            bar = new HealthStatusBarComposite(barTable,
                    (int) heapData.getMaxHeap(), DEFAULT_BAR_WIDTH, heapData.getUnits());
        }

        /* Health bars are kept in a map keyed by component name */
        this.rowBarMap.put(rowData.getComponentName(), bar);

        /* Initialize the health status bar and set its grid information. */
        bar.init();
        bar.setCurrentState(0, 0, HealthStatus.NONE);
        gd = new GridData();
        gd.horizontalAlignment = SWT.LEFT;
        gd.grabExcessHorizontalSpace = true;
        bar.setLayoutData(gd);

        /*
         * The third column in the grid row is a note label. This is kept in a
         * map keyed by component name as well.
         */
        Label noteLabel = new Label(barTable, SWT.LEFT);
        noteLabel.setText("Note");
        noteLabel.setFont(tableFont);
        gd = new GridData();
        gd.horizontalAlignment = SWT.LEFT;
        gd.grabExcessHorizontalSpace = true;
        noteLabel.setLayoutData(gd);
        this.rowLabelMap.put(rowData.getComponentName(), noteLabel);

        barTable.pack();

        /*
         * Update the new row with the actual performance data.
         */
        updateRow(rowData);
    }

    /**
     * Updates the contents of a existing grid row from a performance object.
     * 
     * @param rowData
     *            IPerformanceData containing current performance data
     */
    private void updateRow(IPerformanceData rowData) {

        /*
         * Find the note and health bar for this component from the maps.
         */
        Label note = this.rowLabelMap.get(rowData.getComponentName());
        HealthStatusBarComposite bar = this.rowBarMap.get(rowData
                .getComponentName());

        /* Update the state of the health status bar */
        if (rowData instanceof QueuePerformanceData) {
			bar.setCurrentState((int) ((QueuePerformanceData) rowData)
					.getCurrentQueueSize(),
					(int) ((QueuePerformanceData) rowData).getHighWaterMark(),
					rowData.getHealthStatus());
			note.setText("(High="
             + ((QueuePerformanceData) rowData).getHighWaterMark() + ")");
        } else {
            bar.setCurrentState(
                    (int) ((HeapPerformanceData) rowData).getCurrentHeap(),
                    (int) ((HeapPerformanceData) rowData).getHighWaterMark(),
                    rowData.getHealthStatus());
            note.setText("(High="
                    + ((HeapPerformanceData) rowData).getHighWaterMark() + ")");
        }
        
        barTable.pack();

    }
    
    /**
     * Clears the state of all rows, leaving an empty bar in each,
     * but leaving high water marking. 
     */
    public void clearRowData() {
    	for (HealthStatusBarComposite bar: this.rowBarMap.values()) {
    		bar.clearCurrentState();
    	}
    }

}
