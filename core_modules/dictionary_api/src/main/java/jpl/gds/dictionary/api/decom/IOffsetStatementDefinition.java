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
 * The IOffsetStatementDefinition interface is to be implemented by all offset
 * statement definition objects found in IDecomMapDefinitions.
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 * An IOffsetStatementDefinition object is the multi-mission representation of
 * an offset statement specification in a decommutation map. An offset statement
 * specifically directs the decom processor to move either to a specific offset,
 * or by a relative offset, in the data block being decommutated. This offset is
 * applied automatically for each channel sample extraction, until overridden by
 * another Offset statement, or by a specific offset in an
 * IChannelStatementDefinition. IChannelDecomDictionary implementations must
 * parse mission-specific channel decom dictionary files and create
 * IDecomMapDefinitions with attached IOffsetStatementsDefinition objects for
 * the offset statements found therein. In order to isolate the mission
 * adaptation from changes in the multi-mission core, IChannelDecomDictionary
 * implementations define a mission-specific class that implements this
 * interface. All interaction with these objects in mission adaptations should
 * use the IOffsetStatementDefinition interface, rather than directly
 * interacting with the objects themselves.
 * 
 *
 *
 */
public interface IOffsetStatementDefinition extends IDecomStatement {

    /**
     * An enumeration of offset types.
     * 
     *
     */
    public enum OffsetType {
        
        /** Specified offset is absolute within the data block */
        ABSOLUTE,
        
        /**
         * Specified offset is relative to the current offset, in the reverse
         * (backward) direction.
         */
        
        MINUS,
        
        /**
         * Specified offset is relative to the current offset, in the forward
         * direction.
         */
        PLUS
    };

    /**
     * Indicates whether the desired offset is the start of the DATA area.
     * In this case, no offset value is returned by this object. The caller
     * must determine where the start of the DATA area is.
     * 
     * @return true if this offset is the start of the DATA area; false if
     *         it is not
     */
    public boolean isDataOffset();

    /**
     * Gets the offset value for this offset statement, in bits.
     * Offsets are always non-negative.
     * 
     * @return the number of offset bits
     */
    public int getOffset();

    /**
     * Sets the offset value in bits for this offset statement.
     * Offsets are always non-negative.
     * 
     * @param offValue the number of offset bits
     * 
     */
    public void setOffset(int offValue);

    /**
     * Gets the offset type.
     * 
     * @return OffsetType enum values: ABSOLUTE, PLUS, MINUS
     */
    public OffsetType getOffsetType();

    /**
     * Sets the offset type to one of these constants: ABSOLUTE, PLUS (move
     * forward), or MINUS (move backward) in the data stream.
     * 
     * @param offType
     *            the offsetType to set
     * 
     */
    public void setOffsetType(OffsetType offType);

}