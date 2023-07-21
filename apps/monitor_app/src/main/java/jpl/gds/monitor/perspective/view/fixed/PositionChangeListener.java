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
package jpl.gds.monitor.perspective.view.fixed;

import jpl.gds.shared.swt.types.ChillPoint;


/**
 * An interface to be implemented by classes that want to be notified of position
 * changes in a fixed field configuration.
 *
 */
public interface PositionChangeListener {
	/**
	 * Notifies the listener that the position of a single-coordinate fixed field 
	 * configuration has changed.
	 * 
	 * @param onConfig the field configuration that has changed
	 * @param startPoint the new start location
	 */
	public void positionChanged(IFixedFieldConfiguration onConfig, ChillPoint startPoint);

	/**
	 * Notifies the listener that the position of a dual-coordinate fixed field 
	 * configuration has changed.
	 * 
	 * @param onConfig the field configuration that has changed
	 * @param startPoint the new starting position
	 * @param endPoint the new ending position
	 */
	public void positionChanged(IFixedFieldConfiguration onConfig, ChillPoint startPoint, ChillPoint endPoint);
}
