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
package jpl.gds.tc.impl;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.client.command.ICommandUtilityDictionaryManager;
import jpl.gds.dictionary.api.command.ICommandDefinition;
import jpl.gds.tc.api.*;
import jpl.gds.tc.api.cltu.ICltu;
import jpl.gds.tc.api.cltu.ICltuBuilder;
import jpl.gds.tc.api.cltu.ICltuFactory;
import jpl.gds.tc.api.cltu.ICltuParser;
import jpl.gds.tc.api.cltu.ITcCltuBuilder;
import jpl.gds.tc.api.command.IFlightCommand;
import jpl.gds.tc.api.command.IFlightCommandTranslator;
import jpl.gds.tc.api.command.ISseCommand;
import jpl.gds.tc.api.config.CltuProperties;
import jpl.gds.tc.api.config.CommandProperties;
import jpl.gds.tc.api.config.FileLoadParseException;
import jpl.gds.tc.api.config.PlopProperties;
import jpl.gds.tc.api.exception.CltuEndecException;
import jpl.gds.tc.api.exception.UnblockException;
import jpl.gds.tc.impl.cltu.Cltu;
import jpl.gds.tc.impl.cltu.BchCodeblock;
import jpl.gds.tc.impl.cltu.parsers.BchCodeBlockBuilder;
import jpl.gds.tc.impl.cltu.parsers.CltuBuilder;
import jpl.gds.tc.impl.fileload.CommandFileLoad;
import jpl.gds.tc.impl.fileload.FileLoadInfo;
import org.springframework.context.ApplicationContext;

import java.util.Collections;

/**
 * This is a static factory class for creating command objects from
 * ICommandDefinition objects.
 * 
 */
public final class CommandObjectFactory implements ICommandObjectFactory {

    private final ApplicationContext appContext;


    /**
     * Constructor
     */
    public CommandObjectFactory(final ApplicationContext appContext) {

        this.appContext = appContext;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final IFlightCommand createFlightCommand(final ICommandDefinition definition) {

        if (definition == null) {
            throw new IllegalArgumentException("Command definition cannot be null");
        }
        switch (definition.getType()) {
        case FLIGHT:
        case HARDWARE:
        case SEQUENCE_DIRECTIVE:
            return new FlightCommand(appContext, definition);
        case SSE:
        case UNDEFINED:
        default:
            throw new RuntimeException("Unrecognized command definition type "
                    + definition.getType()
                    + " in CommandObjectFactory.createFlightCommand()");

        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public final ISseCommand createSseCommand() {
        return new SseCommand(appContext);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ISseCommand createSseCommand(final String cmd)
    {
        return new SseCommand(appContext, cmd);
    }
    
    /**
	 * {@inheritDoc}
	 */
	@Override
	public IFlightCommand getCommandObjectFromOpcode(final String opcodeHexString)  throws DictionaryException {

	    if (opcodeHexString == null) {
	        throw new IllegalArgumentException("Null input opcode hex string");
        }

	    // get the command object (without arg values) from the dictionary
	
	    // MPCS-7163 04/22/15 Previously tried to cast to FlightCommand.
	    // Cast is not possible, we build the FlightCommand from the definition.
	
	    return createFlightCommand(appContext.getBean(ICommandUtilityDictionaryManager.class).getCommandDefinitionForOpcode(opcodeHexString));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IFlightCommand getCommandObjectFromBits(final String bitString, final int offset) throws UnblockException {
	    return appContext.getBean(IFlightCommandTranslator.class).parseFromBitString(bitString, offset);
	}

    @Override
    public ICommandFileLoad createCommandFileLoad() {
        return new CommandFileLoad(appContext);
    }

    @Override
    public IFileLoadInfo createFileLoadInfo(final byte fileType, final String inFile, final String outFile, final boolean overwriteFlag) throws FileLoadParseException {
        return new FileLoadInfo(appContext.getBean(CommandProperties.class), fileType, inFile, outFile, overwriteFlag);
    }

    @Override
    public IFileLoadInfo createFileLoadInfo() {
        return new FileLoadInfo(appContext.getBean(CommandProperties.class));
    }

    @Override
    public IFileLoadInfo createFileLoadInfo(final String fileType, final String inFile, final String outFile, final boolean overwriteFlag)
            throws FileLoadParseException {
        return new FileLoadInfo(appContext.getBean(CommandProperties.class), fileType, inFile, outFile, overwriteFlag);
    }

    @Override
    public IBchCodeblock createBchCodeblock() {
        BchCodeBlockBuilder builder = new BchCodeBlockBuilder();
        builder.setData(new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
        builder.setEdac(new byte[]{0x00});
        return builder.build();
    }

    @Override
    public IBchCodeblock createEmptyBchCodeblock() {
        return new BchCodeblock();
    }

    @Override
    public ICltu createCltu() {
        ICltuBuilder cltuBuilder = appContext.getBean(ICltuBuilder.class);
        CltuBuilder.setSequences(appContext.getBean(CltuProperties.class), appContext.getBean(PlopProperties.class));
        cltuBuilder.setData(new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
        cltuBuilder.setCodeblocks(Collections.singletonList(createBchCodeblock()));
        return cltuBuilder.build();
    }

    @Override
    public ICltu createCltu(final byte[] cltu) throws CltuEndecException {
        return appContext.getBean(ICltuParser.class).parse(cltu);
    }
}
