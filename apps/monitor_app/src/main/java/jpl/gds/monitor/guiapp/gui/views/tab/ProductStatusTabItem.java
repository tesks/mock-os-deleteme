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

import jpl.gds.monitor.guiapp.gui.views.ProductStatusComposite;
import jpl.gds.monitor.perspective.view.ProductStatusViewConfiguration;
import jpl.gds.perspective.view.IViewConfiguration;

/**
 * 
 * ProductStatusTabItem is a tab container for the ProductStatusComposite
 * used in the non-eclipse RCP version of the monitor. It plays the role of
 * a view that can be manipulated by the monitor's generic view logic.
 *
 */
public class ProductStatusTabItem extends AbstractTabItem {

    /**
     * Product status tab item title
     */
    public static final String TITLE = ProductStatusComposite.TITLE;

    /**
     * Product status composite within this tab item
     */
    protected ProductStatusComposite productComp;
    
    /**
     * Creates an instance of ProductStatusTabItem.
     * @param appContext the current application context
     * @param config the ProductStatusViewConfiguration object containing display settings
     */
    public ProductStatusTabItem(final ApplicationContext appContext, final IViewConfiguration config) {
        super(config);
        this.productComp = new ProductStatusComposite(appContext, this.viewConfig);
        setComposite(this.productComp);
    }
    
    /**
     * Creates an instance of ProductStatusTabItem with a default view configuration.
     * @param appContext the current application context
     */
    public ProductStatusTabItem(final ApplicationContext appContext) {
        this(appContext, new ProductStatusViewConfiguration(appContext));
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
        
        this.productComp.init(comp);
        final Control innerComp = this.productComp.getMainControl();
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
                    if (ProductStatusTabItem.this.productComp != null) {
                    	ProductStatusTabItem.this.productComp.getMainControl().dispose();
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
