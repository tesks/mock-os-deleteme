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
package jpl.gds.product.api.builder;

/**
 * AssemblyTrigger enumerates the reasons for triggering data product assembly. 
 * How a given product builder adaptation responds to these triggers is 
 * mission-specific.
 * 
 */
public enum AssemblyTrigger {
	/**
	 * Do not trigger product assembly.
	 */
	NO_TRIGGER,
	/**
	 * Assembly is triggered because an EPDU has been received.
	 */
	END_PART,
	/**
	 * Assembly is triggered because no new packets have been received during
	 * the project-configured aging timeout interval.
	 */
	AGING_TIMER,
	/**
	 * Assembly is triggered because the incoming data stream has switched from
	 * packets for one product to packets for another.
	 */
	PROD_CHANGE,
	/**
	 * Assembly is triggered because the end of the input telemetry has been
	 * reached.
	 */
	END_DATA,
	/**
	 * Assembly is triggered because the end of the processing session has
	 * been reached.
	 */
	END_TEST,
	// Added dictionary correction, used by PostDownlinkProductProcessor to
	// indicate that a product needs its dictionary info fixed
	/**
	 * Assembly is triggered because the dictionary indicated in data is incorrect.
	 */
	DICTIONARY_CORRECTION,
	/**
	 * The AMPCS Product Plug-In in CFDP Processor triggered this generation.
	 */
	CFDP_AMPCS_PRODUCT_PLUGIN
}
