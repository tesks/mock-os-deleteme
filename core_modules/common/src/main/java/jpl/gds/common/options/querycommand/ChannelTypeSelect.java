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
 * A simple class to store the result of parsing an Channel types option from the
 * command line.
 * 
 *
 * @since R8
 */
public class ChannelTypeSelect {
	
	 /** Set true for FSW realtime channels */
    public boolean fswRealtime = false;

    /** Set true for FSW recorded channels */
    public boolean fswRecorded = false;
    
    /** Set true for FSW headers */
    public boolean fswHeader = false;
    
    /** Set true for monitor */
    public boolean monitor = false;

    /** Set true for SSE channels */
    public boolean sse = false;
    
    /** Set true for SSE headers */
    public boolean sseHeader = false;

    /**
     * Constructor.
     */
    public ChannelTypeSelect()
    {
        // do nothing
    }
    
    /**
     * Constructor.
     * @param val string value containing Channel type flags
     */
    public ChannelTypeSelect(final String val)
    {
        if (val == null) {
            throw new IllegalArgumentException("Channel types input value may not be null");
        }
        fswRealtime = val.contains("f");
        fswRecorded = val.contains("r");
        fswHeader = val.contains("h");
        monitor = val.contains("m");
        sse = val.contains("s");
        sseHeader = val.contains("g");

        
    }


}
