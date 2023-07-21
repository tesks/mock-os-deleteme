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
package jpl.gds.shared.junit;

import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Class used to mark execution as under control of a JUnit test. The idea here
 * is that the state could be set without triggering an instantiation of any
 * class that needs the state.
 *
 */
public final class JunitMarker extends Object
{
    private static final AtomicBoolean JUNIT = new AtomicBoolean(false);


    /**
     * Private constructor, never called.
     */
    private JunitMarker()
    {
        super();
    }


    /**
     * Set JUnit state.
     *
     * @param junit True if under JUnit control
     */
    public static void setJunitState(final boolean junit)
    {
        JUNIT.set(junit);
    }


    /**
     * Get JUnit state.
     *
     * @return True if under JUnit control
     */
    public static boolean getJunitState()
    {
        return JUNIT.get();
    }
}
