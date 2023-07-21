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
package jpl.gds.cfdp.common.action;

import jpl.gds.cfdp.common.ICltCommandType;

/**
 * The relative URI to be used to perform the operation
 *
 * @since 8.0.1
 */
public enum EActionCommandType implements ICltCommandType {

    /**
     * CFDP PUT subcommand type
     */
	PUT("put", "action/put"),

    /**
     * CFDP CANCEL subcommand type
     */
	CANCEL("cancel", "action/cancel"),

    /**
     * CFDP ABANDON subcommand type
     */
	ABANDON("abandon", "action/abandon"),

    /**
     * CFDP SUSPEND subcommand type
     */
	SUSPEND("suspend", "action/suspend"),

    /**
     * CFDP RESUME subcommand type
     */
	RESUME("resume", "action/resume"),

    /**
     * CFDP REPORT subcommand type
     */
	REPORT("report", "action/report"),

    /**
     * Reset statistics subcommand type
     */
	RESET_STAT("resetstat", "action/resetstat"),

    /**
     * Save CFDP Processor state subcommand type
     */
	SAVE_STATE("savestate", "action/savestate"),

    /**
     * Force generate downlink transaction file subcommand type
     */
	FORCE_GEN("forcegen", "action/forcegen"),

    /**
     * Pause timer subcommand type
     */
	PAUSE_TIMER("pausetimer", "action/pausetimer"),

    /**
     * Resume timer subcommand type
     */
	RESUME_TIMER("resumetimer", "action/resumetimer"),

    /**
     * Ingest CFDP PDUs file subcommand type
     */
	INGEST("ingest", "action/ingest"),

    /**
     * Query Message to User mappings
     */
    MTU_MAP("mtumap", "mtumap"),

    /**
     * Clear subcommand type
     */
    CLEAR("clear", "action/clear");

    private String cltCommandStr;
	private String relativeUri;

    /**
     * Constructor that sets the actual CLT subcommand string to the defined type.
     *
     * @param cltCommandStr subcommand string
     */
	EActionCommandType(final String cltCommandStr, final String relativeUri) {
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