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
package jpl.gds.dictionary.api.decom;

/**
 * The IWidthStatementDefinition interface is to be implemented by all width
 * statement definition objects found in IDecomMapDefinitions.
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 * An IWidthStatementDefinition object is the multi-mission representation of a
 * width statement specification in a decommutation map. A width statement
 * specifically directs the decom processor to establish a default channel field
 * width, which remains in effect unless overridden by another width statement,
 * or by a specific width defined in an IChannelStatementDefinition.
 * IChannelDecomDictionary implementations must parse mission-specific channel
 * decom dictionary files and create IDecomMapDefinitions with attached
 * IWidthStatementsDefinition objects for the width statements found therein.
 * In order to isolate the mission adaptation from changes in the multi-mission
 * core, IChannelDecomDictionary implementations define a mission-specific class
 * that implements this interface. All interaction with these objects in mission
 * adaptations should use the IWidthStatementDefinition interface, rather than
 * directly interacting with the objects themselves.
 * 
 *
 *
 */
public interface IWidthStatementDefinition extends IDecomStatement {

    /**
     * Gets the width in bits, to be used for all upcoming channel
     * sample extractions until overridden.
     * 
     * @return width in bits
     */
    public int getWidth();

    /**
     * Sets the width in bits, to be used for all upcoming channel
     * sample extractions until overridden.
     * 
     * @param w width in bits
     * 
     *
     */
    public void setWidth(int w);

}