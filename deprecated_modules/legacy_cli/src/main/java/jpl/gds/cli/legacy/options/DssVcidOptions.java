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
package jpl.gds.cli.legacy.options;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.config.mission.StationMapper;
import jpl.gds.shared.holders.StationIdHolder;


/**
 * This class contains various methods to handle VCID and DSS command-line
 * options. All methods are static.
 * 
 */
public class DssVcidOptions extends Object
{
    /** Long option for S/C string ids */
    public static final String STRING_ID_OPTION_LONG = "stringId";

    /** Long option for VCIDs */
    public static final String VCID_OPTION_LONG      = "vcid";

    /** Long option for DSS ids */
    public static final String DSS_ID_OPTION_LONG    = "dssId";

    // MPCS-8979 11/8/17
    /** Long sessionDssId option */
    public static final String SESSION_DSS_ID_LONG   = "sessionDssId";
    /** Long sessionVcid option */
    public static final String SESSION_VCID_LONG     = "sessionVcid";


    /**
     * Private constructor.
     */
    private DssVcidOptions()
    {
        super();
    }


    /**
     * Install string-id and VCID options.
     *
     * @param options Options object to add to
     */
    public static void addVcidOption(final Options options)
    {
        options.addOption(new MpcsOption(null,
                                         STRING_ID_OPTION_LONG,
                                         true,
                                         "string,...",
                                         "S/C string ids"));

        options.addOption(new MpcsOption(null,
                                         VCID_OPTION_LONG,
                                         true,
                                         "integer,...",
                                         "Virtual channel ids"));
    }


    /**
     * Install DSS id options.
     *
     * @param options Options object to add to
     */
    public static void addDssIdOption(final Options options)
    {
        options.addOption(new MpcsOption(null,
                                         DSS_ID_OPTION_LONG,
                                         true,
                                         "integer,...",
                                         "DSS ids"));
    }


    /**
     * Parse string id option and turn into VCIDs.
     *
     * @param cl         Command line
     * @param disallowed Incompatible option or null
     *
     * @return Set of VCIDS or null
     *
     * @throws ParseException           Option error
     * @throws MissingArgumentException Option error
     */
    private static Set<Integer> parseStringId(final MissionProperties mprops,
    		                                  final CommandLine cl,
                                              final String      disallowed)
            throws ParseException
    {
        if (! cl.hasOption(STRING_ID_OPTION_LONG))
        {
            return Collections.emptySet();
        }

        if (cl.hasOption(VCID_OPTION_LONG))
        {
			throw new ParseException("Cannot set both --" +
                                     VCID_OPTION_LONG     +
                                     " and --"            +
                                     STRING_ID_OPTION_LONG);
        }

        if ((disallowed != null) && cl.hasOption(disallowed))
        {
			throw new ParseException("Cannot set both --" +
                                     disallowed           +
                                     " and --"            +
                                     STRING_ID_OPTION_LONG);
        }

        final String value = cl.getOptionValue(STRING_ID_OPTION_LONG);

        if (value == null)
        {
            throw new MissingArgumentException("--"                  +
                                               STRING_ID_OPTION_LONG +
                                               " requires a value");
        }

        return innerParseStringId(mprops, value);
    }


    /**
     * Parse string id option and turn into VCIDs.
     *
     * @param cl          Command line
     * @param disallowed1 Incompatible option letter or null
     * @param disallowed2 Incompatible option letter or null
     * @param from        Option source of option letters
     *
     * @return Set of VCIDS or null
     *
     * @throws ParseException           Option error
     */
    private static Set<Integer> parseStringId(final MissionProperties mprops,
    		                                  final CommandLine cl,
                                              final Character   disallowed1,
                                              final Character   disallowed2,
                                              final String      from)
            throws ParseException
    {
        if (! cl.hasOption(STRING_ID_OPTION_LONG))
        {
            return null;
        }

        if (cl.hasOption(VCID_OPTION_LONG))
        {
			throw new ParseException("Cannot set both --" +
                                     VCID_OPTION_LONG     +
                                     " and --"            +
                                     STRING_ID_OPTION_LONG);
        }

        if (disallowed1 != null)
        {
			throw new ParseException("Cannot set both --" +
                                     from                 +
                                     " "                  +
                                     disallowed1          +
                                     " and --"            +
                                     STRING_ID_OPTION_LONG);
        }

        if (disallowed2 != null)
        {
			throw new ParseException("Cannot set both --" +
                                     from                 +
                                     " "                  +
                                     disallowed2          +
                                     " and --"            +
                                     STRING_ID_OPTION_LONG);
        }

        final String value = cl.getOptionValue(STRING_ID_OPTION_LONG);

        if (value == null)
        {
            throw new MissingArgumentException("--"                  +
                                               STRING_ID_OPTION_LONG +
                                               " requires a value");
        }

        return innerParseStringId(mprops, value);
    }


    /**
     * Parse string id argument and turn into VCIDs.
     *
     * @param value Parameter value
     *
     * @return Set of VCIDS or null
     *
     * @throws ParseException Option error
     */
    private static Set<Integer> innerParseStringId(final MissionProperties mprops, final String value)
        throws ParseException
    {
        final Set<Integer> vcids = new TreeSet<>();

        for (String id : value.trim().toUpperCase().split("[,]"))
        {
            id = id.trim();

            final int vcid = mprops.mapNameToDownlinkVcid(id);

            if (vcid < 0)
            {
                throw new ParseException("String id '"                  +
                                         id                             +
                                         "' has no conversion to VCID " +
                                         "for this mission");
            }

            vcids.add(vcid);
        }

        return vcids;
    }


    /**
     * Parse VCID option and turn into VCIDs.
     * 
     * @param mprops
     *            MissionProperties
     * @param cl
     *            Command line
     * @param disallowed
     *            Incompatible option or null
     *
     * @return Set of VCIDS or null
     *
     * @throws ParseException
     *             Option error
     */
    public static Set<Integer> parseVcid(final MissionProperties mprops,
    		                             final CommandLine cl,
                                         final String      disallowed)
            throws ParseException
    {
        final Set<Integer> vcids = parseStringId(mprops, cl, disallowed);

        if (vcids != null && !vcids.isEmpty())
        {
            return vcids;
        }

        if (! cl.hasOption(VCID_OPTION_LONG))
        {
            return Collections.emptySet();
        }

        if ((disallowed != null) && cl.hasOption(disallowed))
        {
			throw new ParseException("Cannot set both --" +
                                     disallowed           +
                                     " and --"            +
                                     VCID_OPTION_LONG);
        }

        final String value = cl.getOptionValue(VCID_OPTION_LONG);

        if (value == null)
        {
            throw new MissingArgumentException("--"             +
                                               VCID_OPTION_LONG +
                                               " requires a value");
        }

        return innerParseVcid(value);
    }

    // MPCS-8979
    /**
     * Parse SessionVcid option and turn into VCIDs.
     * 
     * @param missionProps
     *            Mission Properties
     *
     * @param cl
     *            Command line
     * @param disallowed
     *            Incompatible option or null
     *
     * @return Set of VCIDS or null
     *
     * @throws ParseException
     *             Option error
     * @throws MissingArgumentException
     *             Option error
     */
    public static Set<Integer> parseSessionVcid(final MissionProperties missionProps, final CommandLine cl,
                                                final String disallowed)
            throws ParseException {
        final Set<Integer> vcids = parseStringId(missionProps, cl, disallowed);

        if (vcids != null && !vcids.isEmpty()) {
            return vcids;
        }

        if (!cl.hasOption(SESSION_VCID_LONG)) {
            return Collections.emptySet();
        }

        if ((disallowed != null) && cl.hasOption(disallowed)) {
            throw new ParseException("Cannot set both --" + disallowed + " and --" + SESSION_VCID_LONG);
        }

        final String value = cl.getOptionValue(SESSION_VCID_LONG);

        if (value == null) {
            throw new MissingArgumentException("--" + SESSION_VCID_LONG + " requires a value");
        }

        return innerParseSessionVcid(value);
    }

    // innerParseSessionVcid
    /**
     * Parse VCID argument and turn into VCIDs.
     *
     * @param value
     *            Option value
     *
     * @return Set of VCIDS or null
     *
     * @throws ParseException
     *             Option error
     */
    private static Set<Integer> innerParseSessionVcid(final String value) throws ParseException {
        final Set<Integer> vcids = new TreeSet<>();

        for (String vcidString : value.trim().split("[,]")) {
            vcidString = vcidString.trim();

            int vcid = 0;

            try {
                vcid = Integer.parseInt(vcidString);
            }
            catch (final NumberFormatException nfe) {
                throw new ParseException("SessionVCID '" + vcidString + "' is not a valid VCID");
            }

            if (vcid < 0) {
                throw new ParseException("SessionVCID " + vcid + " is not a valid VCID");
            }

            vcids.add(vcid);
        }

        return vcids;
    }

    // parseSessionDssId
    /**
     * Parse SessionDSS id option and turn into DSS ids.
     * 
     *
     * @param cl
     *            Command line
     * @param disallowed
     *            Incompatible option or null
     *
     * @return Set of DSS idS or null
     *
     * @throws ParseException
     *             Option error
     */
    public static Set<Integer> parseSessionDssId(final CommandLine cl,
                                                 final String disallowed)
            throws ParseException {
        
        if (!cl.hasOption(SESSION_DSS_ID_LONG)) {
            return Collections.emptySet();
        }

        if ((disallowed != null) && cl.hasOption(disallowed)) {
            throw new ParseException("Cannot set both --" + disallowed + " and --" + SESSION_DSS_ID_LONG);
        }

        final String value = cl.getOptionValue(SESSION_DSS_ID_LONG);

        if (value == null) {
            throw new MissingArgumentException("--" + SESSION_DSS_ID_LONG + " requires a value");
        }

        return innerParseSessionDssId(value);
    }

    // innerParseSessionDssId
    /**
     * Parse Session DSS id argument and turn into DSS ids.
     *
     * MPCS-4839 06/13/13 Use constants for station.
     *
     * @param value
     *            Option value
     *
     * @return Set of DSS idS or null
     *
     * @throws ParseException
     *             Option error
     */
    private static Set<Integer> innerParseSessionDssId(final String value) throws ParseException {
        final Set<Integer> dssIds = new TreeSet<>();

        for (String dssIdString : value.trim().split("[,]")) {
            dssIdString = dssIdString.trim();

            int dssId = 0;

            try {
                dssId = Integer.parseInt(dssIdString);
            }
            catch (final NumberFormatException nfe) {
                throw new ParseException("SessionDSS id '" + dssIdString + "' is not a valid DSS id");
            }

            if ((dssId < 0) || (dssId > 65535)) {
                throw new ParseException("SessionDSS id " + dssId + " is not a valid DSS id");
            }

            dssIds.add(dssId);
        }

        return dssIds;
    }


    /**
     * Parse VCID option and turn into VCIDs.
     * 
     * @param mprops
     *            mission properties
     * @param cl
     *            Command line
     * @param disallowed1
     *            Incompatible option letter or null
     * @param disallowed2
     *            Incompatible option letter or null
     * @param from
     *            Option source of option letters
     *
     * @return Set of VCIDS or null
     *
     * @throws ParseException
     *             Option error
     */
    public static Set<Integer> parseVcid(final MissionProperties mprops,
    		                             final CommandLine cl,
                                         final Character   disallowed1,
                                         final Character   disallowed2,
                                         final String      from)
            throws ParseException
    {
        final Set<Integer> vcids = parseStringId(mprops, cl, disallowed1, disallowed2, from);

        if (vcids != null) {
            return vcids;
        }

        if (!cl.hasOption(VCID_OPTION_LONG)) {
            return null;
        }

        if (disallowed1 != null)
        {
			throw new ParseException("Cannot set both --" +
                                     from                 +
                                     " "                  +
                                     disallowed1          +
                                     " and --"            +
                                     VCID_OPTION_LONG);
        }

        if (disallowed2 != null)
        {
			throw new ParseException("Cannot set both --" +
                                     from                 +
                                     " "                  +
                                      disallowed2         +
                                     " and --"            +
                                     VCID_OPTION_LONG);
        }

        final String value = cl.getOptionValue(VCID_OPTION_LONG);

        if (value == null)
        {
            throw new MissingArgumentException("--"             +
                                               VCID_OPTION_LONG +
                                               " requires a value");
        }

        return innerParseVcid(value);
    }


    /**
     * Parse VCID argument and turn into VCIDs.
     *
     * @param value Option value
     *
     * @return Set of VCIDS or null
     *
     * @throws ParseException Option error
     */
    private static Set<Integer> innerParseVcid(final String value)
        throws ParseException
    {
        final Set<Integer> vcids = new TreeSet<>();

        for (String vcidString : value.trim().split("[,]"))
        {
            vcidString = vcidString.trim();

            int vcid = 0;

            try
            {
                vcid = Integer.parseInt(vcidString);
            }
            catch (final NumberFormatException nfe)
            {
                throw new ParseException("VCID '"   +
                                         vcidString +
                                         "' is not a valid VCID");
            }

            if (vcid < 0)
            {
                throw new ParseException("VCID " +
                                         vcid    +
                                         " is not a valid VCID");
            }

            vcids.add(vcid);
        }

        return vcids;
    }


    /**
     * Parse DSS id option and turn into DSS ids.
     *
     * @param cl         Command line
     * @param disallowed Incompatible option or null
     *
     * @return Set of DSS idS or null
     *
     * @throws ParseException           Option error
     */
    public static Set<Integer> parseDssId(final CommandLine cl,
                                          final String      disallowed)
            throws ParseException
    {
        if (! cl.hasOption(DSS_ID_OPTION_LONG))
        {
            return Collections.emptySet();
        }

        if ((disallowed != null) && cl.hasOption(disallowed))
        {
			throw new ParseException("Cannot set both --" +
                                     disallowed           +
                                     " and --"            +
                                     DSS_ID_OPTION_LONG);
        }

        final String value = cl.getOptionValue(DSS_ID_OPTION_LONG);

        if (value == null)
        {
            throw new MissingArgumentException("--"               +
                                               DSS_ID_OPTION_LONG +
                                               " requires a value");
        }

        return innerParseDssId(value);
    }


    /**
     * Parse DSS id option and turn into DSS ids.
     *
     * @param cl         Command line
     * @param disallowed Incompatible option letter or null
     * @param from       Option source of option letters
     *
     * @return Set of DSS idS or null
     *
     * @throws ParseException           Option error
     */
    public static Set<Integer> parseDssId(final CommandLine cl,
                                          final Character   disallowed,
                                          final String      from)
            throws ParseException
    {
        if (! cl.hasOption(DSS_ID_OPTION_LONG))
        {
            return Collections.emptySet();
        }

        if (disallowed != null)
        {
			throw new ParseException("Cannot set both --" +
                                     from                 +
                                     " "                  +
                                     disallowed           +
                                     " and --"            +
                                     DSS_ID_OPTION_LONG);
        }

        final String value = cl.getOptionValue(DSS_ID_OPTION_LONG);

        if (value == null)
        {
            throw new MissingArgumentException("--"               +
                                               DSS_ID_OPTION_LONG +
                                               " requires a value");
        }

        return innerParseDssId(value);
    }


    /**
     * Parse DSS id argument and turn into DSS ids.
     *
     * MPCS-4839 06/13/13 Use constants for station.
     *
     * @param value Option value
     *
     * @return Set of DSS idS or null
     *
     * @throws ParseException Option error
     */
    private static Set<Integer> innerParseDssId(final String value)
        throws ParseException
    {
        final Set<Integer> dssIds = new TreeSet<>();

        for (String dssIdString : value.trim().split("[,]"))
        {
            dssIdString = dssIdString.trim();

            int dssId = StationMapper.MINIMUM_REAL_DSSID;

            try
            {
                dssId = Integer.parseInt(dssIdString);
            }
            catch (final NumberFormatException nfe)
            {
                throw new ParseException("DSS id '"  +
                                         dssIdString +
                                         "' is not a valid DSS id");
            }

            if ((dssId < StationMapper.MINIMUM_REAL_DSSID) ||
                (dssId > StationIdHolder.MAX_VALUE))
            {
                throw new ParseException("DSS id " +
                                         dssId     +
                                         " is not a valid DSS id");
            }

            dssIds.add(dssId);
        }

        return dssIds;
    }
}
