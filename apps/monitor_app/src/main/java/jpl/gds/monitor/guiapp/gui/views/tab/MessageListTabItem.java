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

import jpl.gds.monitor.guiapp.gui.views.MessageListComposite;
import jpl.gds.monitor.perspective.view.MessageListViewConfiguration;
import jpl.gds.perspective.view.IViewConfiguration;

/**
 * 
 * MessageListTabItem is a tab container for display of the MessageListComposite
 * in the non-eclipse RCP GUI. It plays the role of a ViewTab in the monitor so
 * it can be manipulated by general view logic.
 *
 */
public class MessageListTabItem extends AbstractTabItem {
    /**
     * Message list tab item title
     */
    public static final String TITLE = MessageListComposite.TITLE;

    /**
     * Message list composite within this tab item
     */
    protected MessageListComposite messageComp;
    
    /**
     * Creates an instance of MessageListTabItem with a MessageListComposite
     * as GUI content.
     * @param appContext the current application context
     * @param config the MessageListViewConfiguration object containing display settings
     */
    public MessageListTabItem(final ApplicationContext appContext, final IViewConfiguration config) {
        super(config);
        this.messageComp = new MessageListComposite(appContext, this.viewConfig);
        setComposite(this.messageComp);
     }
    
    /**
     * Creates an instance of MessageListTabItem, but allows the caller to 
     * specify the contained MessageListComposite. This allows us to use
     * subclasses of MessageListComposite.
     * 
     * @param config the MessageListViewConfiguration object containing display settings
     * @param useComposite the MessageListComposite that will contain the GUI content
     *                     of this tab item.
     */
    public MessageListTabItem(final IViewConfiguration config, final MessageListComposite useComposite) {
        super(config);
        this.messageComp = useComposite;
        setComposite(this.messageComp);
    }
    
    /**
     * Creates an instance of MessageListTabItem with a default view configuration.
     * @param appContext the current application context
     */
    public MessageListTabItem(final ApplicationContext appContext) {
        this(appContext, new MessageListViewConfiguration(appContext));
    }
    
    /**
     * Creates the controls and composites for this tab display.
     */
    @Override
    protected void createControls() {
        this.tab = new CTabItem(this.parent, SWT.NONE);
        this.tab.setText(this.viewConfig.getViewName());

        final Composite comp = new Composite(this.parent, SWT.NONE);
        final FormLayout shellLayout = new FormLayout();
        comp.setLayout(shellLayout);
        
        this.messageComp.init(comp);
        final Control innerComp = this.messageComp.getMainControl();
        final FormData efd = new FormData();
        efd.top = new FormAttachment(0);
        efd.right = new FormAttachment(100);
        efd.bottom = new FormAttachment(100);
        efd.left = new FormAttachment(0);
        innerComp.setLayoutData(efd);
        
        this.tab.setControl(comp);
        this.tab.setToolTipText("Message Summary List");
        
        this.tab.addDisposeListener(new DisposeListener() {
            @Override
			public void widgetDisposed(final DisposeEvent event) {
                try {
                    if (MessageListTabItem.this.messageComp != null) {
                    	MessageListTabItem.this.messageComp.getMainControl().dispose();
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
