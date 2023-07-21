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
package jpl.gds.dictionary.api.command;

/**
 * The CommandDefiinitionType enumeration defines the type of an
 * ICommandDefinition, indicating whether it is a flight software command,
 * hardware command, SSE command, or sequence directive. <br>
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 

 * 
 * @see ICommandDefinition
 */
public enum CommandDefinitionType {
    /**
     * ICommandDefinition type is undefined. Should be used only in
     * uninitialized ICommandDefinition objects.
     */
    UNDEFINED,
    /**
     * ICommandDefinition is for a flight software command.
     */
    FLIGHT,
    /**
     * ICommandDefinition is for an SSE/Simulation command.
     */
    SSE,
    /**
     * ICommandDefinition is for a flight hardware command.
     */
    HARDWARE,
    /**
     * ICommandDefinition is a sequence directive.
     */
    SEQUENCE_DIRECTIVE
}
