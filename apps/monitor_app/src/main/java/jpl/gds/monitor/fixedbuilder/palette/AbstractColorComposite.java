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

import java.util.List;

import org.eclipse.swt.widgets.Composite;

import jpl.gds.monitor.canvas.ElementConfigurationChangeListener;

/**
 * Abstract class that represents a color composite.
 * Two different color composite classes extend this one:
 * 1. OneColorComposite: Foreground only
 * 2. TwoColorComposite: Foreground & Background
 */
public abstract class AbstractColorComposite extends AbstractComposite{
	
    /** Display constant for default color selection */
	protected static final String DEFAULT_COLOR = "[DEFAULT]";
	
	/**
	 * Constructor for a new color composite
	 * @param parent the parent Composite
	 */
	public AbstractColorComposite(Composite parent) {
		super(parent);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.monitor.fixedbuilder.palette.AbstractComposite#getListeners()
	 */
	@Override
    public List<ElementConfigurationChangeListener> getListeners() {
		return listeners;
	}
}
