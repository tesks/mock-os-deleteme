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
 * A simple class to store the result of parsing an EVR types option from the
 * command line.
 * 
 *
 * @since R8
 */
public class EvrTypeSelect {

    /** Set true for FSW realtime evrs */
    public boolean fswRealtime = false;

    /** Set true for FSW recorded evrs */
    public boolean fswRecorded = false;

    /** Set true for SSE evrs */
    public boolean sse         = false;

    /**
     * Constructor.
     */
    public EvrTypeSelect()
    {
        // do nothing
    }
    
    /**
     * Constructor.
     * @param val string value containing EVR type flags
     */
    public EvrTypeSelect(final String val)
    {
        if (val == null) {
            throw new IllegalArgumentException("EVR types input value may not be null");
        }
        fswRealtime = val.contains("f");
        fswRecorded = val.contains("r");
        sse = val.contains("s");
        
    }
}
