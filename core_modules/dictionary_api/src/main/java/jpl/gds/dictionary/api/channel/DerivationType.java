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
 * DerivationType is an enumeration that defines all the valid types of channel derivations.
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 * DerivationType is an enumeration of supported channel derivation types. A derived channel 
 * is a channel created by some means from other channel(s). The DerivationType defines
 * the nature of that derivation. Every ChannelDefinition for a derived channel must have
 * a DerivationType.
 *
 *
 * @see IChannelDefinition
 */
@CustomerAccessible(immutable = true)
public enum DerivationType {
	/**
	 * No derivation defined.
	 */
    NONE,
    /**
     * Derivation is by custom algorithm.
     */
    ALGORITHMIC,
    /**
     * Derivation is by extracting bit fields from a parent channel.
     */
    BIT_UNPACK,
    /**
     * Derivation is directly from a data product object.
     */
    DPO
}
