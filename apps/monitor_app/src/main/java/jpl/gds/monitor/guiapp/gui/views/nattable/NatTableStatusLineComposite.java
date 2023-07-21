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
package jpl.gds.monitor.guiapp.gui.views.nattable;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import jpl.gds.monitor.perspective.view.RealtimeRecordedFilterType;
import jpl.gds.shared.swt.ChillColorCreator;
import jpl.gds.shared.swt.ChillFontCreator;
import jpl.gds.shared.swt.types.ChillColor;
import jpl.gds.shared.swt.types.ChillColor.ColorName;
import jpl.gds.shared.swt.types.ChillFont;

/**
 * A composite class that is used by NAT table composite classes to display a status
 * line that advertises pause, filter, realtime-recorded status, etc. Only the
 * pause status is always included. The others are added dynamically if required.
 *
 */
public class NatTableStatusLineComposite extends Composite {
    
    private static Color RED = ChillColorCreator.getColor(new ChillColor(ColorName.RED));
    private static Color GREEN = ChillColorCreator.getColor(new ChillColor(ColorName.GREEN));
    private static Color WHITE = ChillColorCreator.getColor(new ChillColor(ColorName.WHITE));
    private static Color BLACK = ChillColorCreator.getColor(new ChillColor(ColorName.BLACK));
    
    private Label pauseLabel;
    private Label rtRecLabel;
    private Label filterLabel;
    private Label sourceLabel;
    private Font standardFont;
    private Font boldFont;
    
    /**
     * Constructor
     * 
     * @param parent parent Composite
     */
    public NatTableStatusLineComposite(Composite parent) {
        super(parent, SWT.NONE);
        createGui();
    }
    
    /**
     * Creates initial GUI components. Only the pause status is included by default.
     */
    private void createGui() {
        RowLayout rl = new RowLayout(SWT.HORIZONTAL);
        setLayout(rl);
        rl.spacing = 10;
        pauseLabel = new Label(this, SWT.NONE);
        setPaused(false);
        standardFont = pauseLabel.getFont();
        FontData[] fontData = standardFont.getFontData();
        boldFont = ChillFontCreator.getFont(new ChillFont(fontData[0].getName(), fontData[0].getHeight(), SWT.BOLD));
        addDisposeListener(new DisposeListener() {

            /**
             * {@inheritDoc}
             * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
             */
            @Override
            public void widgetDisposed(DisposeEvent arg0) {
                boldFont.dispose();
                
            }         
        });
        
    }
    
    /**
     * Sets the pause status.
     * 
     * @param paused true if the view is paused, false it not.
     */
    public void setPaused(boolean paused) {
        if (paused) {
            pauseLabel.setText("Paused");
            pauseLabel.setBackground(RED);
            pauseLabel.setForeground(WHITE);
        } else {
            pauseLabel.setText("Not Paused");
            pauseLabel.setBackground(GREEN);
            pauseLabel.setForeground(BLACK);
        }
        layout();
        pack();
    }
    
    /**
     * Sets the realtime/recorded filter status. If this is the first call,
     * the GUI components for this status element are created.
     * 
     * @param filter the current realtime/recorded filter type for the view
     */
    public void setRealtimeRecorded(RealtimeRecordedFilterType filter) {
        if (this.rtRecLabel == null) {
            rtRecLabel = new Label(this, SWT.NONE);
        }
        switch (filter) {
        case BOTH:
            rtRecLabel.setText("Realtime/Recorded");
            break;
        case REALTIME:
            rtRecLabel.setText("Realtime");
            break;
        case RECORDED:
            rtRecLabel.setText("Recorded");
            break;
        default:
            break;
        }
        layout();
        pack();
    }
    
    /**
     * Sets the overall filter status. If this is the first call,
     * the GUI components for this status element are created.
     * 
     * @param filter true if the view is filtered, false if not
     */
    public void setFiltered(boolean filtered) {
        if (this.filterLabel == null) {
            this.filterLabel = new Label(this, SWT.NONE);
        }
        if (filtered) {
            this.filterLabel.setText("Filtered*");
            this.filterLabel.setFont(boldFont);
        } else {
            this.filterLabel.setText("Not filtered");
            this.filterLabel.setFont(standardFont);
        }
        layout();
        pack();
    }
    
    /**
     * Sets the sources label. If this is the first call, the GUI components for
     * this status element are created.
     * 
     * @param sources
     *            list of data sources for the view; if empty, FSW and SSE
     *            sources are assumed.
     */
    public void setSources(List<String> sources) {
        if (this.sourceLabel == null) {
            this.sourceLabel = new Label(this, SWT.NONE);
        }
        if (sources == null || sources.isEmpty()) {
            sourceLabel.setText("FSW,SSE");
        } else {
            StringBuilder sourceList = new StringBuilder("");
            for (int i = 0; i < sources.size(); i++) {
                sourceList.append(sources.get(i));
                if (i != sources.size() - 1) {
                    sourceList.append(',');
                }
            }
            this.sourceLabel.setText(sourceList.toString());
        }
        layout();
        pack();
    }


}
