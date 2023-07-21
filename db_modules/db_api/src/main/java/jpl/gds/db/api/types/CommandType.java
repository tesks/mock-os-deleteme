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
package jpl.gds.db.api.types;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import jpl.gds.shared.types.EnumeratedType;
import jpl.gds.tc.api.message.CommandMessageType;


/**
 * An enumeration of the different types of command-related messages
 * that will be stored in the CommandMessageLDIStore.
 *
 * @version MPCS-10869 - Added support for FileCfdp and CltuF CommandMessageTypes
 */
public class CommandType extends EnumeratedType
{
    // Static integer values

    /** UNKNOWN_COMMAND_TYPE */
	public static final int UNKNOWN_COMMAND_TYPE         = 0;

    /** HARDWARE_COMMAND_TYPE */
	public static final int HARDWARE_COMMAND_TYPE        = 1;

    /** FLIGHT_SOFTWARE_COMMAND_TYPE */
	public static final int FLIGHT_SOFTWARE_COMMAND_TYPE = 2;

    /** SSE_COMMAND_TYPE */
	public static final int SSE_COMMAND_TYPE             = 3;

    /** FILE_LOAD_TYPE */
	public static final int FILE_LOAD_TYPE               = 4;

    /** SCMF_TYPE */
	public static final int SCMF_TYPE                    = 5;

    /** RAW_UPLINK_DATA_TYPE */
	public static final int RAW_UPLINK_DATA_TYPE         = 6;

    /**SEQUENCE_DIRECTIVE _TYPE */
	public static final int SEQUENCE_DIRECTIVE_TYPE      = 7;
	
	/** FILE_CFDP_TYPE */
	public static final int FILE_CFDP_TYPE				 = 8;
	
	/** CLTU_F_TYPE */
	public static final int CLTU_F_TYPE					 = 9;

	/** Static string values */
	@SuppressWarnings({"MS_MUTABLE_ARRAY","MS_PKGPROTECT"})
	public static final String messageTypes[] =
    {
        "UNKNOWN",
        CommandMessageType.HardwareCommand.getSubscriptionTag(),
        CommandMessageType.FlightSoftwareCommand.getSubscriptionTag(),
        CommandMessageType.SseCommand.getSubscriptionTag(),
        CommandMessageType.FileLoad.getSubscriptionTag(),
        CommandMessageType.Scmf.getSubscriptionTag(),
        CommandMessageType.RawUplinkData.getSubscriptionTag(),
        CommandMessageType.SequenceDirective.getSubscriptionTag(),
        CommandMessageType.FileCfdp.getSubscriptionTag(),
        CommandMessageType.CltuF.getSubscriptionTag()
    };

	// Static instances for each enumerated value

    /** UNKNOWN_COMMAND */
	public static final CommandType UNKNOWN_COMMAND         =
        new CommandType(UNKNOWN_COMMAND_TYPE);

    /** HARDWARE_COMMAND */
	public static final CommandType HARDWARE_COMMAND        =
        new CommandType(HARDWARE_COMMAND_TYPE);

    /** FLIGHT_SOFTWARE_COMMAND */
    public static final CommandType FLIGHT_SOFTWARE_COMMAND =
        new CommandType(FLIGHT_SOFTWARE_COMMAND_TYPE);

    /** SSE_COMMAND */
    public static final CommandType SSE_COMMAND             =
        new CommandType(SSE_COMMAND_TYPE);

    /** FILE_LOAD */
    public static final CommandType FILE_LOAD               =
        new CommandType(FILE_LOAD_TYPE);

    /** SCMF */
    public static final CommandType SCMF                    =
        new CommandType(SCMF_TYPE);

    /** RAW_UPLINK_DATA */
    public static final CommandType RAW_UPLINK_DATA         =
        new CommandType(RAW_UPLINK_DATA_TYPE);

    /** SEQUENCE_DIRECTIVE */
    public static final CommandType SEQUENCE_DIRECTIVE      =
        new CommandType(SEQUENCE_DIRECTIVE_TYPE);
    
    public static final CommandType FILE_CFDP      =
            new CommandType(FILE_CFDP_TYPE);
    
    public static final CommandType CLTU_F      =
            new CommandType(CLTU_F_TYPE);


    /**
     * Creates an instance of CommandMessageType.
     *
     * @param strVal The initial value
     */
    public CommandType(final String strVal)
	{
		super(strVal);
	}


    /**
     * Creates an instance of CommandMessageType.
     *
     * @param intVal The initial value
     */
    public CommandType(final int intVal)
	{
		super(intVal);
	}


    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.types.EnumeratedType#getStringValue(int)
     */
	@Override
	protected String getStringValue(final int index)
	{
		if(index < 0 || index > getMaxIndex())
		{
			throw new ArrayIndexOutOfBoundsException();
		}

		return(messageTypes[index]);
	}


	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.types.EnumeratedType#getMaxIndex()
	 */
	@Override
	protected int getMaxIndex()
	{
		return(messageTypes.length-1);
	}
}
