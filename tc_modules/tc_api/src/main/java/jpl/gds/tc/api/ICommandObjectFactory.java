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
package jpl.gds.tc.api;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.command.ICommandDefinition;
import jpl.gds.tc.api.cltu.ICltu;
import jpl.gds.tc.api.command.IFlightCommand;
import jpl.gds.tc.api.command.ISseCommand;
import jpl.gds.tc.api.config.FileLoadParseException;
import jpl.gds.tc.api.exception.CltuEndecException;
import jpl.gds.tc.api.exception.UnblockException;

public interface ICommandObjectFactory {

    /**
     * Creates a flight command object from the given command definition object.
     * 
     * @param definition
     *            the command's dictionary definition object
     * 
     * @return new instance of FlightCommand, if the appropriate type to match
     *         the definition
     */
    IFlightCommand createFlightCommand(ICommandDefinition definition);

    /**
     * Creates an SSE command object. SSE command objects have no dictionary
     * definition.
     * 
     * @return new instance of SseCommand
     */
    ISseCommand createSseCommand();

    /**
     * Creates an SSE command object. SSE command objects have no dictionary
     * definition.
     *
     * @param cmd Command string
     *
     * @return new instance of SseCommand
     *
     */
    ISseCommand createSseCommand(String cmd);
    
    /**
     * Given an opcode bit string, return the associated command definition from
     * the command dictionary. If you plan on modifying the returned object, you
     * should make a copy of it first so you don't corrupt the command
     * dictionary.
     * 
     * @param opcodeHexString
     *            The opcode of the command as a String containing only valid hex characters (0-9 and A-F)
     * 
     * @return The command associated with the input opcode or null if there
     *         isn't one
     *
     * @throws DictionaryException
     *             If there's an error parsing the command dictionary
     *             
     * MPCS-10473 - 04/05/19 - Moved to here from FlightCommand
     */
    IFlightCommand getCommandObjectFromOpcode(String opcodeHexString) throws DictionaryException;

    /**
     * Given the binary representation of a command as a bit string, de-serialize
     * the bit string back into a command object. This function will only parse
     * the opcode portion of the command, it will not parse any associated
     * argument values.
     * 
     * @param bitString
     *            The bit string to parse the command out of
     * @param offset
     *            The offset into the bit string where parsing should begin
     * @return The FlightCommand object corresponding to the input bit string
     *
     * @throws UnblockException
     *             If there is an error reversing the bit string back into a
     *             command object
     *
     * MPCS-7725  01/20/16 Use OpcodeUtil
     * 
     * MPCS-10473 - 04/05/19 - Moved to here from FlightCommand
     */
	IFlightCommand getCommandObjectFromBits(String bitString, int offset) throws UnblockException;
    
    ICommandFileLoad createCommandFileLoad();
      
    IFileLoadInfo createFileLoadInfo();
    
    IFileLoadInfo createFileLoadInfo(byte fileType, String inFile, String outFile, boolean overwriteFlag) throws FileLoadParseException;
    
    IFileLoadInfo createFileLoadInfo(String fileType, String inFile, String outFile, boolean overwriteFlag) throws FileLoadParseException;
    
    IBchCodeblock createBchCodeblock();
    
    IBchCodeblock createEmptyBchCodeblock();
    
    ICltu createCltu();
    
    ICltu createCltu(final byte[] cltu) throws CltuEndecException;

}