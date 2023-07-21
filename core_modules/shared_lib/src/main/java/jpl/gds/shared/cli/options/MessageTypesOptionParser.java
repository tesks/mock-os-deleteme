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
package jpl.gds.shared.cli.options;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.cli.ParseException;

import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.message.IMessageConfiguration;
import jpl.gds.shared.message.IMessageType;
import jpl.gds.shared.message.MessageRegistry;

/**
 * Parses a message type command line option.
 * 
 * @since R8
 */
public class MessageTypesOptionParser extends AbstractOptionParser<Collection<IMessageType>> {
	

	@Override
	public Collection<IMessageType> parse(final ICommandLine commandLine, final ICommandLineOption<Collection<IMessageType>> opt)
			throws ParseException {
        final String str = getValue(commandLine,opt);

        if (str == null) {
            return Collections.<IMessageType>emptyList();
        } else {
            return verifyMessageTypes(str);
        }
	}
	
    private List<IMessageType> verifyMessageTypes(final String typeList)
            throws ParseException {
        final String[] types = typeList.split(",");
        final List<IMessageConfiguration> availTypes = MessageRegistry.getAllMessageConfigs(true);
        final List<IMessageType> result = new ArrayList<>(types.length);
        boolean found = false;
        for (int i = 0; i < types.length; i++) {
            for (final IMessageConfiguration mc: availTypes) {
                if (mc.isTagAliasFor(types[i].trim())) {
                    result.add(mc.getMessageType());
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new ParseException("Unrecognized message type: "
                        + types[i]);
            }
        }
        return result;
    }
}
