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
package jpl.gds.monitor.guiapp.gui.views.tab;


import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.springframework.context.ApplicationContext;

import jpl.gds.monitor.guiapp.gui.views.ChannelChartPageComposite;
import jpl.gds.monitor.perspective.view.ChannelChartViewConfiguration;
import jpl.gds.perspective.view.IViewConfiguration;

/**
 * 
 * ChartTabItem is the tab item container for ChannelChartPageComposite in the non-RCP version
 * of the monitor.  It plays a ViewTab role, so it can be added to or removed
 * from the monitor by generic view logic.
 *
 */
public class ChartTabItem extends AbstractTabItem {
    /**
     * Chart tab item title
     */
    public static final String TITLE = ChannelChartPageComposite.TITLE;

    /**
     * Chart composite within this tab item
     */
    protected ChannelChartPageComposite chartComp;
    
    /**
     * Creates an instance of ChannelTabItem.
     * @param appContext the current application context
     * @param config the ChartViewConfiguration object containing display settings
     */
    public ChartTabItem(final ApplicationContext appContext,final IViewConfiguration config) {
        super(config);
        this.chartComp = new ChannelChartPageComposite(appContext, this.viewConfig);
        setComposite(this.chartComp);
    }
    
    /**
     * Creates an instance of ChartTabItem with a default view configuration.
     * @param appContext the current application context
     */
    public ChartTabItem(final ApplicationContext appContext) {
        this(appContext, new ChannelChartViewConfiguration(appContext));
    }
    
    /**
     * Creates the controls and composites for this tab display.
     */
    @Override
    protected void createControls() {
        this.tab = new CTabItem(this.parent, SWT.NONE);
        this.tab.setText(this.viewConfig.getViewName());
 
        final Composite comp = new Composite(this.parent, SWT.NONE);
        final FormLayout form = new FormLayout();
        comp.setLayout(form);
        
        this.chartComp.init(comp);
        final Control innerComp = this.chartComp.getMainControl();
        final FormData efd = new FormData();
        efd.top = new FormAttachment(0);
        efd.right = new FormAttachment(100);
        efd.bottom = new FormAttachment(100);
        efd.left = new FormAttachment(0);
        innerComp.setLayoutData(efd);
       
        this.tab.setControl(comp);
        this.tab.setToolTipText("Chart");
        
        this.tab.addDisposeListener(new DisposeListener() {
            @Override
			public void widgetDisposed(final DisposeEvent event) {
                if (ChartTabItem.this.chartComp != null) {
                	ChartTabItem.this.chartComp.getMainControl().dispose();
                }
            }
        });
    }      

    /**
     * @{inheritDoc}
     * @see jpl.gds.perspective.view.View#getDefaultName()
     */
    @Override
	public String getDefaultName() {
        return TITLE;
    }
}