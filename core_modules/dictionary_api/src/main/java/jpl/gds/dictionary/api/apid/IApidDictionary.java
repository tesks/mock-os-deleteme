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
package jpl.gds.dictionary.api.apid;

import java.util.List;

import jpl.gds.dictionary.api.IBaseDictionary;
import jpl.gds.shared.annotation.CustomerAccessible;


/**
 * The IApidDictionary interface is to be implemented by all APID (Application
 * Process Identifier) Dictionary adaptation classes.
 * <p>
 * <p><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <p>
 * <p>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <p>
 * <p>
 * The JPL document corresponding to the multimission APID dictionary schema
 * is D-001137, the AMPCS Multimission APID Dictionary SIS, in the JPL MGSS
 * Document Management System.
 * <p>
 * The APID (Application Process IDentifier) dictionary is used by the telemetry
 * processing system in order to identify the types and format of data in
 * telemetry packets. When a packet is received, its APID number is extracted
 * from the CCSDS packet header. The IApidDictionary maps that APID number to an
 * APID Definition, which can then be used to identify the format of the packet
 * and allow for its proper routing within the ground system. Every mission may
 * have a different format for representing the APID dictionary. An appropriate
 * dictionary parser must be used in order to create the mission-specific
 * IApidDictionary object, which MUST implement this interface. IApidDictionary
 * objects should only be created via the ApidDictionaryFactory. Direct creation
 * of an IApidDictionary object is a violation of multi-mission development
 * standards.
 * <p>
 * This interface extends the IBaseDictionary interface, which requires parse()
 * and clear() methods on all dictionary objects. These methods are used to
 * populate and clear the dictionary contents, respectively.
 * <p>
 * The key aspects of an APID definition are that it has a name and a number.
 * The APID number is found in the actual telemetry. The name is used in the
 * database and on displays and can be fetched from the APID definition. There
 * is also a method to determine whether a specific APID is defined in the
 * dictionary at all.
 * <p>
 * In addition, APIDs can be divided into processing categories: those requiring
 * EVR processing, those requiring product processing, those that contain
 * pre-channelized information, and those that must be decommutated into
 * channels using a decom map. This interface therefore provides methods for the
 * GDS to determine which APIDs fall into which categories.
 * <p>
 * It is also critical for the GDS to know whether packets with a given APID
 * contains recorded or realtime telemetry. A method for that purpose is
 * therefore also included in this interface.
 * <p>
 *
 * @see IApidDictionaryFactory
 * @see jpl.gds.dictionary.api.IBaseDictionary
 */
@CustomerAccessible(immutable = true)
public interface IApidDictionary extends IBaseDictionary, IApidDefinitionProvider {

    /**
     * Retrieves all IApidDefinitions in the dictionary.
     * 
     * @return List of IApidDefinition, or the empty list if no APIDs defined
     * 
     */
    public List<IApidDefinition> getApidDefinitions();



    /**
     * Returns the highest APID number in the APID dictionary.
     * 
     * @return the maximum APID; if no APIDs are defined, returns 0
     */
    public int getMaxApid();


    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.dictionary.api.IDefinitionProviderLoadStatus#isLoaded()
     */
    @Override
    default public boolean isLoaded() {
        return true;
    }

}
