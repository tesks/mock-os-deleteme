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
package jpl.gds.tc.api.config;

/**
 * The order with which to arrange the radiation list items. This should only
 * determine how they should be displayed, not the real radiation order
 * 
 * @since AMPCS R3
 */
public enum RadiationListOrder {
	/** The first item in the list will radiate first */
	TOP_DOWN,

	/**
	 * The last item in the list will radiate first. This effectively reverses
	 * the list received from CPD before displaying it.
	 */
	BOTTOM_UP
}
