package jpl.gds.context.api.options;
/*
 * Copyright 2006-2022. California Institute of Technology.
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
import jpl.gds.shared.cli.options.ICommandLineOptionsGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Restful helper for parsing API parameters into Context command-line arguments
 */
public class ContextCommandOptionsCliHelper implements ICommandLineOptionsGroup {
    private ContextCommandOptionsCliHelper() {}

    /**
     * Build context related command-line arguments from parameters
     * @param topic the context publishing topic
     * @return List of context related command-line arguments
     */
    public static List<String> buildMiscOptionsFromCli(final String topic) {
        final List<String> argList = new ArrayList<>();

        if (topic != null) {
            argList.add("--" + ContextCommandOptions.PUBLISH_TOPIC_PARAM_LONG);
            argList.add(topic);
        }

        return argList;
    }
}