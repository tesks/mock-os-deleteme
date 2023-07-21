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
package jpl.gds.context.api.options;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.cli.ParseException;

import jpl.gds.common.options.SseOption;
import jpl.gds.context.api.ContextTopicNameFactory;
import jpl.gds.context.api.ISimpleContextConfiguration;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.ExpandTopicsOption;
import jpl.gds.shared.cli.options.FlagOptionParser;
import jpl.gds.shared.cli.options.ICommandLineOption;
import jpl.gds.shared.cli.options.ICommandLineOptionsGroup;
import jpl.gds.shared.cli.options.StringOption;
import jpl.gds.shared.cli.options.StringOptionParser;

/**
 * The ContextCommandOptions are used for parsing context sensitive command line
 * options (e.g. --publishTopic) and storing them in the current
 * IContextConfiguration.
 *
 *
 */
public class ContextCommandOptions implements ICommandLineOptionsGroup {
    /** Constants used by the command options */
    public static final String          PUBLISH_TOPIC_PARAM_LONG = "publishTopic";
    public static final String PUBLISH_TOPIC_DESC = "the root messaging topic for publication";

    /** PUBLISH TOPIC Option object */
    public final StringOption PUBLISH_TOPIC;

    /** SUBSCRIBER TOPICS Option object */
    public final SubscriberTopicsOption SUBSCRIBER_TOPICS;

    /** EXPAND TOPICS Option object */
    public final ExpandTopicsOption EXPAND_TOPICS;

    /** SSE option */
    public final SseOption SSE_OPTION;

    private final ISimpleContextConfiguration contextConfig;

    /**
     * ContextCommandOptions constructor
     *
     * @param ctx
     *            unique instance of IContextConfiguration
     */
    public ContextCommandOptions(final ISimpleContextConfiguration ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("Invalid null context configuration argument");
        }
        contextConfig = ctx;

        PUBLISH_TOPIC = new StringOption(null, PUBLISH_TOPIC_PARAM_LONG, "topic",
                                         PUBLISH_TOPIC_DESC, false);
        PUBLISH_TOPIC.setParser(new PublishTopicOptionParser());

        SUBSCRIBER_TOPICS = new SubscriberTopicsOption(false);
        SUBSCRIBER_TOPICS.setParser(new ContextSubscriberTopicsOptionParser());

        EXPAND_TOPICS = new ExpandTopicsOption();

        SSE_OPTION = new SseOption();
        SSE_OPTION.setParser(new SSeOptionParser());
    }

    /**
     * Gets all the context topic configuration command line options
     *
     * @return Collection of ICommandLineOption; never empty or null
     */
    public Collection<ICommandLineOption<?>> getContextTopicCommandOptions() {
        final List<ICommandLineOption<?>> result = new LinkedList<>();

        result.add(PUBLISH_TOPIC);
        result.add(SUBSCRIBER_TOPICS);
        result.add(EXPAND_TOPICS);

        return result;
    }



    /**
     * Gets all the context topic configuration command line options
     *
     * @return Collection of ICommandLineOption; never empty or null
     */
    public Collection<ICommandLineOption<?>> getAllContextCommandOptions() {
        final Collection<ICommandLineOption<?>> result = getContextTopicCommandOptions();
        result.add(SSE_OPTION);

        return result;
    }

    /**
     * Gets the IContextConfiguration member instance.
     *
     * @return IContextConfiguration; never null
     */
    public ISimpleContextConfiguration getContextConfiguration() {
        return contextConfig;
    }


    /**
     * Option parser for the PUBLISH_TOPIC option. The parsed value will be
     * scanned for invalid characters. The parsed value will be set into the
     * IContextConfiguration member instance.
     *
     *
     */
    protected class PublishTopicOptionParser extends StringOptionParser {

        /**
         * Constructor.
         */
        public PublishTopicOptionParser() {
            super(true);
        }

        /**
         * @{inheritDoc
         * @see jpl.gds.shared.cli.options.StringOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption)
         */
        @Override
        public String parse(final ICommandLine commandLine, final ICommandLineOption<String> opt)
                throws ParseException {

            final String desc = super.parse(commandLine, opt);

            if (desc != null) {
                ContextTopicNameFactory.checkTopicCommandOption(desc, PUBLISH_TOPIC.getLongOpt());
                contextConfig.getGeneralInfo().setRootPublicationTopic(desc);
                contextConfig.getGeneralInfo().setTopicIsOverridden(true);
            }
            return desc;
        }
    }

    /**
     * Option parser for the SUBSCRIBER_TOPICS option. The parsed value will be
     * scanned for invalid characters. If the EXPAND_TOPICS option is present,
     * the topic list is also expanded to include data subtopics. 
     * The parsed value will be set into the IContextConfiguration member instance.
     *
     *
     */
    protected class ContextSubscriberTopicsOptionParser extends SubscriberTopicsOptionParser {

        @Override
        public Collection<String> parse(final ICommandLine commandLine, final ICommandLineOption<Collection<String>> opt)
                throws ParseException {

            Collection<String> topics = super.parse(commandLine, opt);

            if (topics == null || topics.isEmpty()) {
                return topics;
            }

            if (commandLine.hasOption(EXPAND_TOPICS.getLongOpt())) {
                topics = ContextTopicNameFactory.expandTopics(topics);
            }
            contextConfig.getGeneralInfo().setSubscriptionTopics(topics);

            return topics;
        }
    }

    /**
     * Option parser for the SSE option.
     * The parsed value will be set into the IContextConfiguration member instance.
     *
     */
    private class SSeOptionParser extends FlagOptionParser{
        @Override
        public Boolean parse(final ICommandLine commandLine, final ICommandLineOption<Boolean> opt,
                             final boolean required) throws ParseException {
            final boolean isSse = super.parse(commandLine, opt, required);
            contextConfig.getGeneralInfo().getSseContextFlag().setApplicationIsSse(isSse);
            return isSse;
        }
    }

    /**
     * Parses the all the options defined by this class from the supplied
     * command line object. Requires none of the options, and sets no defaults.
     * Result values are set into the ISimpleContextConfiguration member.
     *
     * @param commandLine
     *            the parsed command line options
     * @throws ParseException
     *             if there is a parse error
     */
    public void parseAllOptionsAsOptional(final ICommandLine commandLine) throws ParseException {
        PUBLISH_TOPIC.parse(commandLine);
        SUBSCRIBER_TOPICS.parse(commandLine);
        EXPAND_TOPICS.parse(commandLine);
        SSE_OPTION.parse(commandLine);
    }
}
