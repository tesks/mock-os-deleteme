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
package jpl.gds.common.options.querycommand;

/**
 * A simple class to store the result of parsing an Packet types option from the
 * command line.
 * 
 *
 * @since R8
 */
public class PacketTypeSelect {
	
	/** Set true for FSW packets */
    public boolean fsw = false;

    /** Set true for SSE packets */
    public boolean sse = false;

    /**
     * Constructor.
     */
    public PacketTypeSelect()
    {
        // do nothing
    }
    
    /**
     * Constructor.
     * @param val string value containing Packet type flags
     */
    public PacketTypeSelect(final String val)
    {
        if (val == null) {
            throw new IllegalArgumentException("Packet types input value may not be null");
        }
        fsw = val.contains("f");
        sse = val.contains("s");
        
    }
}
