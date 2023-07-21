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
 * Abstract class that represents a coordinate class
 *
 */
public abstract class CoordinateComposite extends AbstractComposite {
	
	/**
	 * Constructor for a new coordinate composite
	 * @param parent will contain the new coordination composite
	 */
    public CoordinateComposite(Composite parent) {
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
