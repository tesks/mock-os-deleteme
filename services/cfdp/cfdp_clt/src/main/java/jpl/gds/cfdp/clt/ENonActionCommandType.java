/*
 * Copyright 2006-2019. California Institute of Technology.
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
package jpl.gds.cfdp.clt;

import jpl.gds.cfdp.common.ICltCommandType;

/**
 * {@code ENonActionCommandType} is an enumeration for CFDP CLT subcommands that do not call the 'action' endpoints
 * at CFDP Processor.
 *
 * @since 8.0.1
 */
public enum ENonActionCommandType implements ICltCommandType {

    /**
     * Management Information Base subcommand type
     */
    MIB("mib", "mib"),

    /**
     * Configuration subcommand type
     */
    CONFIG("config", "config"),

    /**
     * Statistics subcommand type
     */
    STAT("stat", "stat"),

    /**
     * Root subcommand type
     */
    ROOT("root", "root"),

    /**
     * Status subcommand type
     */
    STATUS("status", "status"),

    /**
     * Entity map subcommand type
     */
    ENTITY_MAP("entitymap", "no-uri"),

    /**
     * Shutdown subcommand type
     */
    SHUTDOWN("shutdown", "shutdown");

    private String cltCommandStr;
    private String relativeUri;

    /**
     * Constructor that sets the actual CLT subcommand string to the defined type.
     *
     * @param cltCommandStr subcommand string
     * @param relativeUri the subcommand's relative URI
     */
    ENonActionCommandType(final String cltCommandStr, final String relativeUri) {
        this.cltCommandStr = cltCommandStr;
        this.relativeUri = relativeUri;
    }

    @Override
    public String getCltCommandStr() {
        return cltCommandStr;
    }
    
    @Override
    public String getRelativeUri() {
    	return relativeUri;
    }

}