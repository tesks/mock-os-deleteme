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
package jpl.gds.message.api.options;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.cli.ParseException;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.config.types.DownlinkStreamType;
import jpl.gds.common.config.types.VenueType;
import jpl.gds.common.options.DownlinkStreamTypeOption;
import jpl.gds.common.options.DssIdOption;
import jpl.gds.common.options.SpacecraftIdOption;
import jpl.gds.common.options.SubtopicOption;
import jpl.gds.common.options.TestbedNameOption;
import jpl.gds.common.options.VcidOption;
import jpl.gds.common.options.VenueTypeOption;
import jpl.gds.context.api.ContextTopicNameFactory;
import jpl.gds.context.api.TopicNameToken;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.ICommandLineOption;
import jpl.gds.shared.cli.options.ICommandLineOptionsGroup;
import jpl.gds.shared.cli.options.StringOption;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.util.HostPortUtility;

/**
 * A command line options manager for a set of command line options that mock
 * the session options needed to construct a messaging topic. To be used by
 * messaging client applications that really do not need a session configuration
 * but still need to have backward-compatible support for session topics.
 */
public class MockSessionTopicCommandOptions implements ICommandLineOptionsGroup {

	/**
	 * The messaging subtopic option.
	 */
	public final SubtopicOption SUBTOPIC = new SubtopicOption(false);

	/**
	 * The venue type option.
	 */
	public final VenueTypeOption VENUE_TYPE = new VenueTypeOption(false);

	/**
	 * The testbed name option.
	 */
	public final TestbedNameOption TESTBED_NAME = new TestbedNameOption(false);

	/**
	 * The downlink stream type option.
	 */
	public final DownlinkStreamTypeOption DOWNLINK_STREAM_TYPE = new DownlinkStreamTypeOption(
			false);

	/**
	 * The DSS ID option. Used for subscription filtering, not topic
	 * construction.
	 */
	public final DssIdOption FILTER_DSSID;

	/**
	 * The VCID option. Used for subscription filtering, not topic construction.
	 */
	public final VcidOption FILTER_VCID;

	/**
	 * The spacecraft ID option. Used for subscription filtering, not topic
	 * construction.
	 */
	public final SpacecraftIdOption FILTER_SCID;
	
	public final StringOption USER;
	
	public final StringOption HOST;

	/**
	 * Constructor.
	 * @param missionProps the current MissionProperties object to get default and valid values from
	 */
	public MockSessionTopicCommandOptions(final MissionProperties missionProps) {
		FILTER_DSSID = new DssIdOption(missionProps, false);
		FILTER_DSSID.setLongOpt("sessionDssId");
		FILTER_VCID = new VcidOption(missionProps, false);
		FILTER_VCID.setLongOpt("sessionVcid");      
		FILTER_SCID =  new SpacecraftIdOption(missionProps, false);

        HOST = new StringOption("O", "sessionHost", "hostname",
                "the name of the host computing system executing the session",
                false);
        HOST.addAlias("testHost");
        HOST.setDefaultValue(HostPortUtility.getLocalHostName());

        USER = new StringOption("P", "sessionUser", "username",
                "the name of the user/entity executing the session", false);
        USER.addAlias("testUser");
        USER.setDefaultValue(GdsSystemProperties.getSystemUserName());
        
        VENUE_TYPE.setDefaultValue(missionProps.getDefaultVenueType());
	}

	/**
	 * Gets the collection of all the defined option objects.
	 * @return Collection of ICommandLineOption
	 */
	public Collection<ICommandLineOption<?>> getAllOptions() {
		final List<ICommandLineOption<?>> result = new LinkedList<ICommandLineOption<?>>();
		result.add(VENUE_TYPE);
		result.add(DOWNLINK_STREAM_TYPE);
		result.add(TESTBED_NAME);
		result.add(SUBTOPIC);
		result.add(HOST);
		result.add(USER);
		result.add(FILTER_DSSID);
		result.add(FILTER_VCID);
		result.add(FILTER_SCID);
		return result;
	}
	
	/**
     * Gets the collection of all the defined option objects, less the FILTER_SCID option.
     * @return Collection of ICommandLineOption
     */
    public Collection<ICommandLineOption<?>> getAllOptionsWithoutScid() {
        final List<ICommandLineOption<?>> result = new LinkedList<ICommandLineOption<?>>();
        result.add(VENUE_TYPE);
        result.add(DOWNLINK_STREAM_TYPE);
        result.add(TESTBED_NAME);
        result.add(SUBTOPIC);
        result.add(HOST);
        result.add(USER);
        result.add(FILTER_DSSID);
        result.add(FILTER_VCID);
        return result;
    }


	/**
	 * Parse the options needed to formulate a flight data/session topic name.
	 * @param commandLine parsed user command line
	 * @return topic name
	 * @throws ParseException if there is an issue with the command line
	 */
	public String parseForFlightTopic(final ICommandLine commandLine) throws ParseException {
		final VenueType vt = VENUE_TYPE.parseWithDefault(commandLine, false, true);
		final String tbName = TESTBED_NAME.parse(commandLine, false);
		final DownlinkStreamType dst = DOWNLINK_STREAM_TYPE.parse(commandLine, false);
		final String subtopic = SUBTOPIC.parse(commandLine, false);
		final String host = HOST.parseWithDefault(commandLine, false, true);
		final String user = USER.parseWithDefault(commandLine, false, true);
		final String venueName = ContextTopicNameFactory.getVenueName(vt, tbName, 
		        host + TopicNameToken.DELIMITER + user);
        return ContextTopicNameFactory.getVenueTopic(vt, venueName, dst, subtopic, new SseContextFlag());

	}
	
	/**
	 * Parse the options needed to formulate an SSE data/session topic name.
	 * @param commandLine parsed user command line
	 * @return topic name
	 * @throws ParseException if there is an issue with the command line
	 */
	public String parseForSseTopic(final ICommandLine commandLine) throws ParseException {
		final VenueType vt = VENUE_TYPE.parseWithDefault(commandLine, false, true);
		final String tbName = TESTBED_NAME.parse(commandLine, false);
		final String subtopic = SUBTOPIC.parse(commandLine, false);
		final String host = HOST.parseWithDefault(commandLine, false, true);
		final String user = USER.parseWithDefault(commandLine, false, true);
		final String venueName = ContextTopicNameFactory.getVenueName(vt, tbName, 
		        host + TopicNameToken.DELIMITER + user);
		return ContextTopicNameFactory.getSseVenueTopic(vt, venueName, subtopic);

	}

	/**
	 * Parses the options needed to formulate both the flight and SSE data/session topic names.
	 * 
	 * @param commandLine  parsed user command line
	 * @return a two element list; the first value is the flight topic, the second is the SSE topic
	 * @throws ParseException if there is an issue with the command line
	 */
	public List<String> parseForTopics(final ICommandLine commandLine) throws ParseException {
		final List<String> result = new LinkedList<>();
		result.add(parseForFlightTopic(commandLine));
		result.add(parseForSseTopic(commandLine));
		return result;
	}

}
