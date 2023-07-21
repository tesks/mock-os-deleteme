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

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import jpl.gds.shared.performance.HealthStatus;
import jpl.gds.shared.swt.ChillColorCreator;
import jpl.gds.shared.swt.ChillFontCreator;
import jpl.gds.shared.swt.types.ChillColor;
import jpl.gds.shared.swt.types.ChillFont;

/**
 * A GUI composite that displays the health status for some entity that has a
 * current volume, a max volume, units, and a HealthStatus. Consists of an
 * optional title, a colored bar representing current percentage of max volume
 * and current health. There is also a tooltip attached to the colored bar that
 * reflects more detailed information. This class sets the max volume label to
 * "NNN%", and the tooltip lists current volume, max volume, percentage of
 * total, and units. Subclasses may override these behaviors.
 * 
 *
 */
public class HealthStatusBarComposite extends Composite {

    private static final Color NONE = ChillColorCreator
            .getColor(new ChillColor(ChillColor.ColorName.BLUE));
    private static final Color GREEN = ChillColorCreator
            .getColor(new ChillColor(ChillColor.ColorName.GREEN));
    private static final Color YELLOW = ChillColorCreator
            .getColor(new ChillColor(ChillColor.ColorName.YELLOW));
    private static final Color RED = ChillColorCreator.getColor(new ChillColor(
            ChillColor.ColorName.RED));
    private static final Color WHITE = ChillColorCreator
            .getColor(new ChillColor(ChillColor.ColorName.WHITE));
    private static final Color AQUA = ChillColorCreator
            .getColor(new ChillColor(ChillColor.ColorName.LIGHT_AQUA_BLUE));

    /** This font is used if the monospace flag is passed to the constructor. */
    private static final Font fixedFont = ChillFontCreator
            .getFont(new ChillFont(ChillFont.MONOSPACE_FACE,
                    ChillFont.FontSize.SMALL, SWT.BOLD));

    /**
     * The default font used for label text in the table. The default font is
     * too large.
     */
    private static final Font variableFont = ChillFontCreator
            .getFont(new ChillFont(ChillFont.DEFAULT_FACE,
                    ChillFont.FontSize.SMALL, SWT.NONE));

    /** The default value for unit display string */
    private static final String DEFAULT_UNIT = "";

    /** The actual "bar" that displays volume and color */
    protected ColorBarComposite colorBar;

    /** Title displayed before the color bar */
    private String title;
    /**
     * The maximum volume of the status bar. This volume essentially represents
     * a 100% full level.
     */
    private final int maxVolume;
    /**
     * The unit string, describing what kind of things the volume level
     * represents (mb, seconds, entries).
     */
    protected String unitString = DEFAULT_UNIT;
    /**
     * The GUI Label widget for the current volume label after the color bar.
     */
    protected Label currentVolumeLabel;
    /**
     * Indicates whether to use monospace font. If false, the default SWT label
     * font is used for text. If true, a 12 point Courier font is used. This
     * allows for easier alignment of multiple instances of this composite.
     */
    private boolean useMonospace = false;
    /**
     * The desired width (pixels across) for the color bar.
     */
    private final int barWidth;

    /** Flag indicating if the object has been initialized */
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    /**
     * Constructs a HealthBarStatusComposite with a title. Must invoke init() to
     * complete the initialization of the object.
     * 
     * @param parent
     *            the parent Composite
     * @param title
     *            the title string to label the queue bar with
     * @param maxVol
     *            the maximum number volume of the status bar (represents 100%
     *            full)
     * @param barWidth
     *            the pixel width of the actual color bar
     * @param unit
     *            the units in which volume is expressed; may be null
     * @param enableMonospace
     *            use fixed font rather than variable font for label text
     */
    public HealthStatusBarComposite(Composite parent, String title, int maxVol,
            int barWidth, String unit, boolean enableMonospace) {
        super(parent, SWT.NULL);

        this.title = title;
        if (unit != null) {
            unitString = unit;
        }
        this.barWidth = barWidth;
        this.useMonospace = enableMonospace;
        this.maxVolume = maxVol;
    }

    /**
     * Constructs a variable font HealthBarStatusComposite without a title. Must
     * invoke init() to complete the initialization of the object.
     * 
     * @param parent
     *            the parent Composite
     * @param maxVol
     *            the maximum number volume of the status bar (represents 100%
     *            full)
     * @param barWidth
     *            the pixel width of the actual color bar
     * @param unit
     *            the units in which volume is expressed; may be null
     */
    public HealthStatusBarComposite(Composite parent, int maxVol, int barWidth,
            String unit) {
        super(parent, SWT.NULL);

        if (unit != null) {
            unitString = unit;
        }
        this.barWidth = barWidth;
        this.maxVolume = maxVol;

    }

    /**
     * Initializes the object.
     */
    public void init() {
        if (!this.initialized.getAndSet(true)) {
            createGui();
        }
    }

    /**
     * Actually creates the GUI components.
     */
    protected void createGui() {

        /* Default is variable font. Switch to fixed font if so configured. */
        Font font = variableFont;
        if (this.useMonospace) {
            font = fixedFont;
        }

        /*
         * This composite is layed out in a horizontal row, with layout settings
         * that allow each component to take its natural size.
         */
        RowLayout layout = new RowLayout(SWT.HORIZONTAL);
        layout.marginHeight = 0;
        layout.marginTop = 1;
        layout.marginBottom = 1;
        layout.spacing = 2;
        layout.pack = true;
        layout.justify = true;
        layout.wrap = false;
        setLayout(layout);

        /* Title label is optional. If present, it is first in the row. */
        if (title != null) {
            Label titleLabel = new Label(this, SWT.LEFT);
            titleLabel.setText(this.title + ":");
            titleLabel.setFont(font);
        }

        /* Create the color bar. It is second in the row. */
        this.colorBar = new ColorBarComposite(this, barWidth);

        /*
         * The current volume label is dynamic, and is the third thing in the
         * row.
         */
        this.currentVolumeLabel = new Label(this, SWT.NONE);
        this.currentVolumeLabel.setFont(font);

        /* The bar starts out empty, with no health status. */
        setCurrentState(0, 0, HealthStatus.NONE);

        pack();
        setSize(computeSize(SWT.DEFAULT, SWT.DEFAULT));
    }

    /**
     * Updates the status of this widget based upon current volume. Sets the
     * color of the status bar based upon health state, and fills percentage of
     * the color bar based upon current volume. Sets the current volume label
     * and color bar tooltip text using the overrideable methods that do so.
     * 
     * @param currentVolume
     *            the current volume of the object being tracked
     * @param highVolume
     *            the highest volume of the object being tracked
     * @param status
     *            current health of the object being tracked
     */
    public void setCurrentState(int currentVolume, int highVolume,
    		HealthStatus status) {

        if (this.isDisposed()) {
            return;
        }

        if (!this.initialized.get()) {
            throw new IllegalStateException(
                    "The object has not been initialized");
        }

        /*
         * Compute the new current percentage of the max volume. Note there is no issue
         * with this being over 100%.
         */
        int percentage = (int)Math
                .round(((double) currentVolume / (double) maxVolume) * 100.0);
        
        /*
         * Compute the new high percentage of the max volume. Note there is no issue
         * with this being over 100%.
         */
        int highPercentage = (int)Math
                .round(((double) highVolume / (double) maxVolume) * 100.0);

        setBarColorFromHealth(status);
        
        /*
         * The queue bar doesn't understand percentages over 100, so set its
         * percentage to the lesser of 100 and the current percentage.
         */
        colorBar.setPercentages(Math.min(percentage, 100), Math.min(highPercentage, 100));

        setCurrentVolumeLabel(status, currentVolume, percentage);
        setBarTooltipText(status, currentVolume, percentage);
        
        pack();
        setSize(computeSize(SWT.DEFAULT, SWT.DEFAULT));

    }
    
    /**
     * Clears the portion of the color bar representing current percentage
     * by setting it to 0, and sets the health status to NONE.
     * 
     */
    public void clearCurrentState() {

        if (this.isDisposed()) {
            return;
        }

        if (!this.initialized.get()) {
            throw new IllegalStateException(
                    "The object has not been initialized");
        }

        setBarColorFromHealth(HealthStatus.NONE);
        
        colorBar.clearCurrentPercentage();

        setCurrentVolumeLabel(HealthStatus.NONE, 0, 0);
        setBarTooltipText(HealthStatus.NONE, 0, 0);
        
        pack();
        setSize(computeSize(SWT.DEFAULT, SWT.DEFAULT));

    }

    /**
     * Overrideable method to set the current volume label. Sets the label to
     * " NNN%", where NNN is the input percentage.
     * 
     * @param status
     *            the current health status
     * @param currentVolume
     *            the current volume
     * @param percentage
     *            the current percentage of max volume
     */
    protected void setCurrentVolumeLabel(HealthStatus status,
            int currentVolume, int percentage) {
        if (this.isDisposed()) {
            return;
        }

        if (!this.initialized.get()) {
            throw new IllegalStateException(
                    "The object has not been initialized");
        }

        String maxDisplayStr = String.format("%-4s", String.valueOf(percentage)
                + "%");
        this.currentVolumeLabel.setText(maxDisplayStr);
    }

    /**
     * Overrideable method to set color bar tooltip text. Includes current
     * volume with units and percentage, max volume with units, and health
     * status.
     * 
     * @param status
     *            current health status
     * @param currentVolume
     *            the current volume
     * @param percentage
     *            the current percentage of max volume
     */
    protected void setBarTooltipText(HealthStatus status, int currentVolume,
            int percentage) {
        if (this.isDisposed()) {
            return;
        }

        if (!this.initialized.get()) {
            throw new IllegalStateException(
                    "The object has not been initialized");
        }

        StringBuilder b = new StringBuilder("Health: " + status);
        b.append("\nCurrent Size: " + currentVolume + " " + unitString + " ("
                + percentage + "% of max)");
        b.append("\nMax Size: " + this.maxVolume + " " + unitString);
        colorBar.setToolTipText(b.toString());
    }

    /**
     * Overrideable method to set the color of the filled portion of the color
     * bar from health status.
     * 
     * @param status
     *            the current health status
     */
    protected void setBarColorFromHealth(HealthStatus status) {
        if (this.isDisposed()) {
            return;
        }

        if (!this.initialized.get()) {
            throw new IllegalStateException(
                    "The object has not been initialized");
        }

        switch (status) {
        case GREEN:
            colorBar.setFillColor(HealthStatusBarComposite.GREEN);
            break;
        case NONE:
            colorBar.setFillColor(HealthStatusBarComposite.NONE);
            break;
        case RED:
            colorBar.setFillColor(HealthStatusBarComposite.RED);
            break;
        case YELLOW:
            colorBar.setFillColor(HealthStatusBarComposite.YELLOW);
            break;
        default:
            break;
        }
    }

    /**
     * Class that renders the color bar. Can fill the bar to a stated percentage
     * with a specified color.
     * 
     */
    public class ColorBarComposite extends Composite {

        private static final int HEIGHT = 20;

        private Color fillColor = GREEN;
        private Color backgroundColor = WHITE;
        private int currentPercentage = 0;
        private int highPercentage = 0;
        private Color highColor = AQUA;

        /**
         * Constructor.
         * 
         * @param parent
         *            the parent composite
         * @param width
         *            the desired width of the bar in pixels
         */
        public ColorBarComposite(Composite parent, int width) {
            super(parent, SWT.NONE);

            /* Fix the height, set width based upon input. */
            setSize(width, HEIGHT);

            RowData rd = new RowData(width, HEIGHT);
            setLayoutData(rd);

            /* This paint listener is the important part. */
            addPaintListener(new PaintListener() {
                /**
                 * {@inheritDoc}
                 *       e.gc.setBackground(backgroundColor);
                    e.gc.fillRectangle(2, 2, clientArea.width - 3,
                            clientArea.height - 3);

                 * @see org.eclipse.swt.events.PaintListener#paintControl(org.eclipse.swt.events.PaintEvent)
                 */
                @Override
                public void paintControl(PaintEvent e) {
                    Rectangle clientArea = getClientArea();

                    /* First fill the entire rectangle with the background color */
                    e.gc.setBackground(backgroundColor);
                    e.gc.fillRectangle(2, 2, clientArea.width - 3,
                            clientArea.height - 3);
                    

                    /*
                     * Now fill a percentage of the total length with the high
                     * color (high volume)
                     */
                    e.gc.setBackground(highColor);
                    e.gc.fillRectangle(
                            2,
                            2,
                            (int) ((clientArea.width) * (highPercentage / 100.0)) - 3,
                            clientArea.height - 3);
                    /*
                     * Now fill a percentage of the total length with the fill
                     * color (current volume)
                     */
                    e.gc.setBackground(fillColor);
                    e.gc.fillRectangle(
                            2,
                            2,
                            (int) ((clientArea.width) * (currentPercentage / 100.0)) - 3,
                            clientArea.height - 3);
                    
                    /* Outline the rectangle. */
                    e.gc.setLineWidth(2);
                    e.gc.drawRectangle(1, 1, clientArea.width - 2,
                            clientArea.height - 2);
                }
            });

        }

        /**
         * Sets the fill color, used to fill the used percentage of the bar.
         * 
         * @param toSet
         *            the fill color
         */
        public void setFillColor(Color toSet) {
            fillColor = toSet;
        }
        

        /**
         * Sets the high color, used to fill the high percentage of the bar.
         * 
         * @param toSet
         *            the high color
         */
        public void setHighColor(Color toSet) {
            highColor = toSet;
        }

        /**
         * Sets the background color, used to fill the non-used percentage of
         * the bar.
         * 
         * @param toSet
         *            the background color
         */
        public void setBackgroundColor(Color toSet) {
            backgroundColor = toSet;
        }

        /**
         * Fills the bar to the specified percentages with the current fill
         * colors.
         * 
         * @param percent
         *            current percentage of the bar to fill, 0-100
         * @param highPercent
         *            high percentage of the bar to fill, 0-100
         */
        public void setPercentages(int percent, int highPercent) {
            this.currentPercentage = percent;
            this.highPercentage = highPercent;
            this.redraw();
        }
        
        /**
         * Clears only the current percentage, not the high percentage.
         */
        public void clearCurrentPercentage() {
        	 this.currentPercentage = 0;
        	 this.redraw();
        }
    }

    /**
     * Test harness.
     * 
     * @param args
     *            command line arguments (none used)
     */
    public static void main(String[] args) {
        Display display = Display.getDefault();
        Shell shell = new Shell(display);
        shell.setText("QueueBar Test");
        shell.setLayout(new FormLayout());
        Composite m = new Composite(shell, SWT.NONE);
        FormData fd = new FormData();
        fd.top = new FormAttachment(0);
        fd.right = new FormAttachment(100);
        fd.left = new FormAttachment(0);
        fd.bottom = new FormAttachment(100);
        m.setLayoutData(fd);
        FormLayout layout = new FormLayout();
        layout.marginHeight = 0;
        layout.marginTop = 0;
        layout.marginBottom = 0;
        m.setLayout(layout);
        HealthStatusBarComposite qb = new HealthStatusBarComposite(m,
                "Queue 1", 100, 250, "messages", true);
        qb.init();
        FormData fd1 = new FormData();
        fd1.left = new FormAttachment(0);
        fd1.top = new FormAttachment(0);
        qb.setLayoutData(fd1);
        qb.setCurrentState(50, 75, HealthStatus.GREEN);
        HealthStatusBarComposite qb2 = new UnboundedHealthStatusBarComposite(m,
                "Test Queue 2", 120, 250, "files", true);
        qb2.init();
        qb2.setCurrentState(80, 85, HealthStatus.RED);
        FormData fd2 = new FormData();
        fd2.left = new FormAttachment(0);
        fd2.top = new FormAttachment(qb);
        qb2.setLayoutData(fd2);
        HealthStatusBarComposite qb3 = new HealthStatusBarComposite(m, 100,
                250, "fortnights");
        qb3.init();
        FormData fd3 = new FormData();
        fd3.top = new FormAttachment(qb2);
        fd3.left = new FormAttachment(0);
        qb3.setLayoutData(fd3);
        qb3.setCurrentState(45, 55, HealthStatus.YELLOW);
        shell.pack();
        shell.setSize(600, 600);
        shell.open();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        display.dispose();
    }

}
