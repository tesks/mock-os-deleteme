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
package jpl.gds.eha.impl.service.channel.derivation;

import java.util.HashSet;
import java.util.Set;

import jpl.gds.shared.log.TraceManager;


/**
 * MissingChannels keeps track of missing channel definitions, reporting them
 * once. This is used by a variety of processing to prevent reporting the same
 * errors over and over during channel processing.
 * 
 */
abstract public class MissingChannels extends Object
{
    private static final Set<String> _missingChildSet  =
        new HashSet<String>(64);
    private static final Set<String> _missingParentSet =
        new HashSet<String>(64);

    /**
     * Indicates whether a child channel definition has already been reported
     * as missing.
     *
     * @param child the child ChannelId
     *
     * @return true if the child has already been reported
     */
    public static boolean isChildAlreadyMissing(final String child)
    {
        return _missingChildSet.contains(child);
    }


    /**
     * Reports a child channel as missing in a log message. If the child has
     * been reported as missing before, this method does nothing.
     *
     * @param child the child channelId
     */
    public static void reportMissingChild(final String child)
    {
        if (! isChildAlreadyMissing(child))
        {
            _missingChildSet.add(child);
            
            TraceManager.getDefaultTracer().warn("Found derivation for undefined child channel " +

                          child);
        }
    }


    /**
     * Indicates whether a parent channel definition has already been reported
     * as missing.
     *
     * @param parent the parent ChannelId
     *
     * @return true if the parent has already been reported
     */
    public static boolean isParentAlreadyMissing(final String parent)
    {
        return _missingParentSet.contains(parent);
    }


    /**
     * Reports a parent channel as missing in a log message. If the parent has
     * been reported as missing before, this method does nothing.
     *
     * @param parent the parent channelId
     */
    public static void reportMissingParent(final String parent)
    {
        if (! isParentAlreadyMissing(parent))
        {
            _missingParentSet.add(parent);

            TraceManager.getDefaultTracer().warn(

                    "Found derivation for undefined parent channel " +
                        parent);
        }
    }
}
