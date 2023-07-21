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
/**
 * 
 */
package jpl.gds.monitor.guiapp.gui.views.tab;

import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import jpl.gds.monitor.guiapp.gui.ViewTab;
import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.perspective.view.View;
import jpl.gds.perspective.view.ViewConfigurationListener;
import jpl.gds.shared.swt.ChillColorCreator;


/**
 * The AbstractTabItem class used as the base class for any class that acts as a tab that contains views in
 * the monitor. A tab item can be used to wrap any view composite class.
 */
public abstract class AbstractTabItem implements ViewTab,
        ViewConfigurationListener {

    /**
     * Tab folder
     */
    protected CTabFolder parent;
    
    /**
     * Individual tab item
     */
    protected CTabItem tab;
    
    /**
     * View configuration in this tab
     */
    protected IViewConfiguration viewConfig;
    
    /**
     * Composite that hold GUI components
     */
    protected View composite;  
    
    /**
     * Creates an instance of AbstractTabItem.
     * @param config the ViewConfiguration for the main composite in this tab
     */
    public AbstractTabItem(final IViewConfiguration config) {
        setViewConfig(config);
        this.viewConfig.addConfigurationListener(this);
    }
    
    /**
     * Creates the controls and composites for this tab display.
     */
    protected abstract void createControls();
    
    /**
     * {@inheritDoc}
     * @see jpl.gds.perspective.view.View#getViewConfig()
     */
    @Override
	public IViewConfiguration getViewConfig() {
        return this.viewConfig;
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.perspective.view.View#init(org.eclipse.swt.widgets.Composite)
     */
    @Override
	public void init(final Composite parent) {
    	if (! (parent instanceof CTabFolder)) {
    		throw new IllegalArgumentException("Parent must be a CTabFolder");
    	}
        this.parent = (CTabFolder)parent;
        createControls();
    }
      
    /**
     * Retrieves the main composite GUI control.
     * @return the composite the main composite instance
     */
    public View getComposite() {
        return this.composite;
    }
   
    /**
     * Sets the main composite GUI control.
     * @param composite the composite to set
     */
    public void setComposite(final View composite) {
        this.composite = composite;
    }
  
    /**
     * Sets the view configuration object used to configure the view displayed in this tab
     * @param viewConfig the ViewConfiguration to set
     */
    public void setViewConfig(final IViewConfiguration viewConfig) {
        this.viewConfig = viewConfig;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.monitor.guiapp.gui.ViewTab#getTabItem()
     */
    @Override
	public CTabItem getTabItem() {
        return this.tab;
    }
    
    /**
     * {@inheritDoc}
     * @see jpl.gds.perspective.view.View#getMainControl()
     */
    @Override
	public Control getMainControl() {
        return this.composite.getMainControl();
    }
    
    /**
     * {@inheritDoc}
     * @see jpl.gds.perspective.view.View#clearView()
     */
    @Override
	public void clearView() {
       this.composite.clearView();
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.perspective.view.View#updateViewConfig()
     */
    @Override
	public void updateViewConfig() {
        this.composite.updateViewConfig();
    }

    /**
     * @{inheritDoc}
     */
    @Override
	public void configurationChanged(final IViewConfiguration config) {
    	if ((this.tab == null) || (this.tab.isDisposed()) || (Display.getDefault().isDisposed())) {
    		return;
    	}
    	Display.getDefault().asyncExec(new Updater(this, config));
    }

    /**
     * Private class to handle deferred updating of GUI elements on the SWT GUI thread.
     */
    private static class Updater implements Runnable {
    	private final AbstractTabItem   tabItem;
    	private final IViewConfiguration config;
    	
    	/**
    	 * @param tabItem The TabItem to be updated.
    	 * @param config The ViewConfiguration from which to update the TabItem
    	 */
    	private Updater(final AbstractTabItem tabItem, final IViewConfiguration config) {
    		this.tabItem = tabItem;
    		this.config = config;
    	}
    	
		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		@Override
        public void run() {
			if (!tabItem.tab.isDisposed()) {
				tabItem.tab.setText(config.getViewName());
				tabItem.parent.setSelectionBackground(ChillColorCreator.getColor(config.getBackgroundColor()));
				tabItem.parent.setSelectionForeground(ChillColorCreator.getColor(config.getForegroundColor()));
			}
		}
    }
}
