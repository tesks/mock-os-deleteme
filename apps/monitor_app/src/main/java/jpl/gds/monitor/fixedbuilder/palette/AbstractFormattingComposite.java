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

import org.eclipse.swt.widgets.Composite;

/**
 * Abstract class that represents a format composite. Two different format 
 * composite classes extend this one:
 * 1. DateFormatComposite
 * 2. SprintfFormatComposite
 */
public abstract class AbstractFormattingComposite extends AbstractComposite {

	/**
	 * Constructor for a new format composite
	 * @param parent Composite in which the format composite will be placed
	 */
	public AbstractFormattingComposite(Composite parent) {
		super(parent);
	}

	/**
	 * Sets the default format in the palette
	 */
	abstract public void setDefaultFormat();
}
