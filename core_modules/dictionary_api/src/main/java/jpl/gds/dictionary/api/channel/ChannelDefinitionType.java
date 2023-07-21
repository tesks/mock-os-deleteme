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
package jpl.gds.dictionary.api.channel;

import jpl.gds.shared.annotation.CustomerAccessible;

/**
 * The ChannelDefinitionType enumeration defines the basic channel processing
 * types: flight (channels come from flight telemetry), sse (channel come from
 * simulation software or ground support equipment), monitor (channels come form
 * DSN monitor data), and header (channels come from telemetry headers). <p>
 * <p>
 * <b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <p>
 * <p>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <p>
 * 
 *
 */
@CustomerAccessible(immutable = true)
public enum ChannelDefinitionType {
    /**
     * Channel comes from flight telemetry, or is derived
     * from other channels from that source.
     */
	FSW,
	/**
	 * Channel comes from SSE/GSE telemetry, or is derived from other
	 * channels from that source.
	 * 
	 */
	SSE,
	/**
	 * Channel is a ground channel that comes from station monitor data, or
	 * is derived from channels from that source.
	 */
	M,
	/**
	 * Channel is a ground channel that is extracted from telemetry headers.
	 */
	H
}