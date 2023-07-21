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

import java.util.Collection;

import jpl.gds.shared.message.IMessageType;

/**
 * A command line option for entering a list of message types.
 * 
 * @since R8
 */
public class MessageTypesOption extends CommandLineOption<Collection<IMessageType>>  {

    private static final long serialVersionUID = -6453749356701095988L;

    /**
     * Short option name.
     */
    public static final String SHORT_OPTION = "t";
    /**
     * Long option name.
     */
    public static final String LONG_OPTION = "types";

    /**
     * Constructor.
     * 
     * @param required true if the option is required
     */
    public MessageTypesOption(final boolean required) {
        super(SHORT_OPTION, LONG_OPTION, true, "type[,type...]", 
              "Specifies message types to subscribe to (required). Message types must "
                        + "be specified as a quoted comma-separated list.", required,
                        new MessageTypesOptionParser());
    }
    
}
