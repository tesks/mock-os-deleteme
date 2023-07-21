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
package jpl.gds.monitor.fixedbuilder.palette;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;

import jpl.gds.monitor.canvas.ElementConfigurationChangeListener;
import jpl.gds.monitor.canvas.ElementConfigurationComposite;
import jpl.gds.monitor.perspective.view.fixed.IFixedFieldConfiguration;
import jpl.gds.monitor.perspective.view.fixed.IFixedLayoutViewConfiguration;
import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.perspective.view.ViewConfigurationListener;

/**
 * The parent class for all of the palette composites
 *
 */
public abstract class AbstractComposite implements 
ElementConfigurationComposite, ViewConfigurationListener {

    /**
     * Light gray fill color for expand bar items.
     * Design rationale: linux shows no colors on the expand bar items
     * this color is used for the backgrounds of all composites in the 
     * composite to differentiate the expand item header from the contents
     */
	protected final Color backgroundColor = new Color(null, 222,222,222);

	/** The parent composite */
	protected Composite parent;
	/** Listeners for palette changes */
	protected List<ElementConfigurationChangeListener> listeners = 
	    new ArrayList<ElementConfigurationChangeListener>();
	/** List of configuration for all fixed page fields */
	protected List<IFixedFieldConfiguration> fieldConfigs = 
	    new ArrayList<IFixedFieldConfiguration>();
	/** Current fixed field configuration */
	protected IFixedFieldConfiguration fieldConfig;
	/** The view configuration for the whole fixed view */
	protected IFixedLayoutViewConfiguration viewConfig;

	/**
	 * Constructor for a new palette composite
	 * @param parent the parent composite
	 */
	public AbstractComposite(final Composite parent) {
		this.parent = parent;

		parent.addDisposeListener(new DisposeListener() {

			@Override
            public void widgetDisposed(final DisposeEvent arg0) {
				if (viewConfig != null) {
					viewConfig.removeConfigurationListener(
					        AbstractComposite.this);
				}
			}
		});
	}

	/**
	 * Sets the fixed layout view configuration for the fixed view being 
	 * configured.
	 * 
	 * @param config the FixedLayoutViewConfiguration to set
	 */
	public void setFixedLayoutViewConfiguration(
	        final IFixedLayoutViewConfiguration config) {
		if (viewConfig != null) {
			viewConfig.removeConfigurationListener(this);
		}
		viewConfig = config;
		if (viewConfig != null) {
			viewConfig.addConfigurationListener(this);
			configurationChanged(viewConfig);
		}
	}

	/**
	 * Adds a listener to composite
	 * @param l listens for changes in the composite
	 */
	@Override
    public synchronized void addChangeListener(
	        final ElementConfigurationChangeListener l) {
		if (!listeners.contains(l)) {
			listeners.add(l);
		}
	}
	
	/**
	 * Removes listeners from composite
	 * @param l listened for changes in the composite
	 */
	@Override
    public synchronized void removeChangeListener(
	        final ElementConfigurationChangeListener l) {
		listeners.remove(l);
	}

	/**
	 * Notifies listeners of changes in the composite
	 */
	public synchronized void notifyChangeListeners() {
		for (final ElementConfigurationChangeListener l : listeners) {
			if(fieldConfigs != null) {
			    final List<IFixedFieldConfiguration> fcs = 
			        new ArrayList<IFixedFieldConfiguration>();
			    for(final IFixedFieldConfiguration fc : fieldConfigs)
			    { 
			        fieldConfig = fc;
			        fcs.add(this.getConfiguration());
			    }
			    
			    l.elementsChanged(fcs);
			    
			    //reset fieldConfig
                fieldConfig = null;
			}
		}
	}

	/**
	 * Get all the listeners that will be notified if user changes composite 
	 * fields
	 * @return listeners
	 */
	public List<ElementConfigurationChangeListener> getListeners()
	{
		return listeners;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.monitor.canvas.ElementConfigurationComposite#getComposite()
	 */
	@Override
    abstract public Composite getComposite();


	/**
	 * {@inheritDoc}
	 * @see jpl.gds.perspective.view.ViewConfigurationListener#configurationChanged(jpl.gds.perspective.view.ViewConfiguration)
	 */
	@Override
    public void configurationChanged(final IViewConfiguration config) {
		// default implementation - do nothing
	}
}
