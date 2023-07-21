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
package jpl.gds.evr.impl.service.extractor;

import java.util.Map;

import jpl.gds.dictionary.api.OpcodeUtil;
import jpl.gds.shared.log.TraceManager;


/**
 * OpcodeReplacement is a type of EVR ParameterReplacement that replaces a numeric
 * opcode with a command stem from the command dictionary.
 * 
 */
public class OpcodeReplacement implements ParameterReplacement {

    private Map<String, String> commands;
    private final OpcodeUtil opcodeUtil;

    /** UNMAPPED OPCODE constant */
    protected static final String HIDDEN_OPCODE = "UNMAPPED_OPCODE";
  
    /**
     * Constructor.
     * 
     * @param opcodeMap map of command opcode to command stem
     * @param opcodeUtility opcode utility object to use for opcode formatting
     */
    public OpcodeReplacement(final Map<String, String> opcodeMap, final OpcodeUtil opcodeUtility) {
        this.commands = opcodeMap;
        this.opcodeUtil = opcodeUtility;
    }
    
    /**
     * Sets the command opcode to stem map to be used for OPCODE replacement.
     * @param dictionaryMap the map to set
     */
    public void setOpcodeMap(final Map<String, String> dictionaryMap) {
        commands = dictionaryMap;
    }

    @Override
    public Object replace(final Object opcodeObj)
    {
        if (opcodeObj == null || commands == null) {
            return opcodeObj;
        }

        String opcode = null;


        if (opcodeObj instanceof Long) {
            opcode = opcodeUtil.formatOpcode(
                         Long.class.cast(opcodeObj).longValue(),
                         true);
        } else if (opcodeObj instanceof Integer) {
            opcode = opcodeUtil.formatOpcode(
                         Integer.class.cast(opcodeObj).intValue(),
                         true);
        } else if (opcodeObj instanceof String) {
            opcode = String.class.cast(opcodeObj);
        } else {
            TraceManager.getDefaultTracer().error("Opcode of unsupported type: " 
                + opcodeObj.getClass().getName());
            throw new IllegalArgumentException("Opcode of unsupported type: " +
                                               opcodeObj.getClass().getName());
        }
        final String stem = commands.get(OpcodeUtil.stripHexPrefix(opcode));

        if (stem != null) {
            return stem;
        }
        else {
            return (opcodeUtil.hideOpCode() ? HIDDEN_OPCODE : opcode);
        }
    }
}
