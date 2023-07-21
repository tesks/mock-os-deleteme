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

import java.util.List;
import java.util.Map;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.channel.IChannelDefinition;

/**
 * The IDecomMapDefinition interface is to be implemented by decom map
 * definition objects found in an IChannelDecomDictionary.
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 * An IDecomMapDefinition object is the multi-mission representation of a
 * decommutation map, which is used to extract channel samples from packets or
 * other data blocks. IChannelDecomDictionary implementations must parse
 * mission-specific channel decom dictionary files and create
 * IDecomMapDefinitions for the maps found therein. In order to isolate the
 * mission adaptation from changes in the multi-mission core,
 * IChannelDecomDictionary implementations define a mission-specific class that
 * implements this interface. All interaction with these objects in mission
 * adaptations should use the IDecomMapDefinition interface, rather than
 * directly interacting with the objects themselves.
 *
 * 
 *
 */
public interface IDecomMapDefinition extends IStatementContainer {

    /**
     * Undefined APID (Application Process ID).
     */
    public static final int NO_APID = -1;

    /**
     * Indicates whether this is a general (APID-less) map.
     * 
     * @return true if map is a general map, false if not
     */
    public boolean isGeneral();

    /**
     * Sets the flag indicating whether this is a general (APID-less) map.
     * 
     * @param general
     *            true if map is a general map, false if not. Also sets APID to -1.
     */
    public void setGeneral(boolean general);

    /**
     * Gets the APID (Application Process ID) for this map. The APID indicates
     * which packets the map can be applied to.
     * 
     * @return APID, or NO_APID if a general map
     */
    public int getApid();

    /**
     * Sets the APID (Application Process ID) for this map. The APID indicates
     * which packets the map can be applied to.
     * 
     * @param apid
     *            the APID to set
     */
    public void setApid(int apid);

    /**
     * Gets the name of this map.
     * 
     * @return map name
     */
    public abstract String getName();

    /**
     * Sets the name of this map.
     * 
     * @param name
     *            map name to set
     */
    public void setName(String name);
    

    /**
     * Parses the supplied decom map file and populates this object from it. Any
     * previous contents of this object are cleared. The supplied channel map
     * must contain the definitions of any channels referenced in the map file.
     * 
     * @param decomFile
     *            decom map file
     * @param chanMap
     *            map of channel ID to channel definition object
     * 
     * @throws DictionaryException
     *             if any parsing error is encountered
     * 
     *
     */
    public void parseDecomFile(String decomFile, Map<String, IChannelDefinition> chanMap) throws DictionaryException;
    
    /**
     * Adds a statement to a decom map.
     * 
     * @param stmt 
     *             statement to be added.
     */

    @Override
    public void addStatement(IDecomStatement stmt);

    /**
     * Gets the decom statements attached to this map.
     * 
     * @return List of Statements; will be an empty list if there are none
     */
    public List<IDecomStatement> getStatementsToExecute();
    
    /** 
     * Clears this decom map of all statements and APID information.
     * 
     */
    public void clear();
    
    /**
     * Get the map ID for this decom map definition.
     * @return the unique identifier for this decom map
     */
    public IDecomMapId getId();

    /**
     * Set the map ID for this decom map definition.
     * @param id the unique identifier for this decom map
     */
	public void setId(IDecomMapId id);
    
}