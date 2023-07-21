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

import jpl.gds.monitor.guiapp.gui.views.FrameAccountabilityComposite;
import jpl.gds.monitor.perspective.view.FrameAccountabilityViewConfiguration;
import jpl.gds.perspective.view.IViewConfiguration;

/**
 * 
 * FrameAccountabilityTabItem is a tab container for the FrameAccountabilityComposite
 * used in the non-eclipse RCP version of the monitor. It plays the role of
 * a view that can be manipulated by the monitor's generic view logic.
 *
 */
public class FrameAccountabilityTabItem extends AbstractTabItem {
    
    /**
     * Frame accountability tab item title
     */
    public static final String TITLE = FrameAccountabilityComposite.TITLE;
    
    /**
     * Frame accountability composite within this tab item
     */
    protected FrameAccountabilityComposite frameComp;
    
    /**
     * Creates an instance of FrameAccountabilityTabItem.
     * @param appContext the current application context
     * @param config the FrameAccountabilityViewConfiguration object containing display settings
     */
    public FrameAccountabilityTabItem(final ApplicationContext appContext, final IViewConfiguration config) {
        super(config);
        this.frameComp = new FrameAccountabilityComposite(appContext, this.viewConfig);
        setComposite(this.frameComp);
    }
    
    /**
     * Creates an instance of FrameAccountabilityTabItem with a default view configuration.
     * @param appContext the current application context
     */
    public FrameAccountabilityTabItem(final ApplicationContext appContext) {
        this(appContext, new FrameAccountabilityViewConfiguration(appContext));
    }
     
    /**
     * Creates the controls and composites for this tab display.
     */
    @Override
    public void createControls() {
        this.tab = new CTabItem(this.parent, SWT.NONE);
        this.tab.setText(this.viewConfig.getViewName());

        final Composite comp = new Composite(this.parent, SWT.NONE);
        final FormLayout shellLayout = new FormLayout();
        comp.setLayout(shellLayout);
        
        this.frameComp.init(comp);
        final Control innerComp = this.frameComp.getMainControl();
        final FormData efd = new FormData();
        efd.top = new FormAttachment(0);
        efd.right = new FormAttachment(100);
        efd.bottom = new FormAttachment(100);
        efd.left = new FormAttachment(0);
        innerComp.setLayoutData(efd);
        
        this.tab.setControl(comp);
        this.tab.setToolTipText("Product Status Table");
        
        this.tab.addDisposeListener(new DisposeListener() {
            @Override
			public void widgetDisposed(final DisposeEvent event) {
                try {
                    if (FrameAccountabilityTabItem.this.frameComp != null) {
                    	FrameAccountabilityTabItem.this.frameComp.getMainControl().dispose();
                    }
                } catch (final RuntimeException e) {
                    e.printStackTrace();
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
