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

package jpl.gds.shared.metadata.context;

/**
 * Enumeration for context configuration types.
 * 
 *
 * @since R8
 */
public enum ContextConfigurationType {
	/** simple */
	SIMPLE,
	/** generic */
	GENERIC_FULL,
	/** session */
	SESSION,
	/** Telemetry Ingestor */
	TELEM_INGEST_SERVER,
	/** Telemetry processor */
	TELEM_PROCESS_SERVER,
	/** CFDP context */
	CFDP;
}
