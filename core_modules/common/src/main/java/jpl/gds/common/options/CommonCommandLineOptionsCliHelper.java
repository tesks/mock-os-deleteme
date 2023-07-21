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
package jpl.gds.common.options;

import jpl.gds.common.config.types.DownlinkStreamType;
import jpl.gds.common.config.types.VenueType;
import jpl.gds.shared.cli.options.ICommandLineOptionsGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for building command-line args[] for some 'common' command-line options
 * 
 *
 */
public class CommonCommandLineOptionsCliHelper implements ICommandLineOptionsGroup {
    private CommonCommandLineOptionsCliHelper() { }

    /**
     * Used to help building a command-line args[] for common command-line options
     * 
     * @param venueType
     *            <VenueTypeOption>
     * @param streamId
     *            <DownlinkStreamIdOption>
     * @param scid
     *            <SpacecraftIdOption>
     * @param subtopic
     *            <SubtopicOption>
     * @param testbedName
     *             Testbed name
     * @return an array list of command-line arguments and their arguments - if present
     */
    public static List<String> buildMiscOptionsFromCli(final VenueType venueType, final DownlinkStreamType streamId,
                                                       final Integer scid, final String subtopic, String testbedName) {
        final List<String> argList = new ArrayList<>();

        if (venueType != null) {
            argList.add("--" + VenueTypeOption.LONG_OPTION);
            argList.add(venueType.toString());
        }
        if (streamId != null) {
            argList.add("--" + DownlinkStreamTypeOption.LONG_OPTION);
            argList.add(streamId.toString());
        }
        if (scid != null) {
            argList.add("--" + SpacecraftIdOption.LONG_OPTION);
            argList.add(scid.toString());
        }
        if (subtopic != null) {
            argList.add("--" + SubtopicOption.LONG_OPTION);
            argList.add(subtopic);
        }
        if (testbedName != null) {
            argList.add("--" + TestbedNameOption.LONG_OPTION);
            argList.add(testbedName);
        }

        return argList;
    }
}
