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

import jpl.gds.shared.annotation.CustomerAccessible;
import jpl.gds.shared.annotation.Mutator;
/**
 * The IChannelDecomDictionary interface is to be implemented by all channel
 * decom dictionary adaptation classes.
 * <p>
 * <p><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <p>
 * <p>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <p>
 * 
 * The JPL document corresponding to the multimission generic channel decom
 * dictionary schemas is D-001143, the AMPCS Multimission Generic Channel
 * Decommutation Dictionary SIS, in the JPL MGSS Document Management System.
 * <p>
 * The channel decom dictionary is used in downlink processing to extract
 * channelized telemetry samples from packet content, usually based upon packet
 * APID. Every mission may have a different format for representing the channel
 * decom dictionary. An appropriate dictionary parser must be used in order to
 * create the mission-specific IChannelDecomDictionary object.
 * IChannelDecomDictionary objects should only be created via the
 * ChannelDecomDictionaryFactory. Direct creation of an IChannelDecomDictionary
 * object is a violation of multi-mission development standards.
 * <p>
 * This interface extends the IBaseDictionary interface, which requires parse()
 * and clear() methods on all dictionary objects. These methods are used to
 * populate and clear the dictionary contents, respectively.
 * <p>
 * The primary job of the IChannelDecomDictionary object is to produce a set of
 * IDecomMapDefinition objects. Each IDecomMapDefinition is the multi-mission
 * representation of a defined packet decommutation map.
 * <p>
 * 
 *
 *
 * @see IChannelDecomDictionaryFactory
 * @see IDecomMapDefinition
 */
@CustomerAccessible(immutable = false)
public interface IChannelDecomDictionary extends IDecomDictionary, IChannelDecomDefinitionProvider {


	/*
     * Sets the channel map object where all the channel definitions are
     * defined. When parsing a channel decom dictionary, the channels referenced
     * by the decom maps in that dictionary must be referenced in the channel
     * Adds an existing IDecomMapDefinition to the dictionary.
     * object is created by the ChannelDecomDictionaryFactory, since the factory
     * performs the parsing, and the channel map must instead be supplied
     * directly to the factory method.
     * @param apid the APID this map is for
     * @param map the IDecomMapDefinition to add
     */
    @Override
    @Mutator
    public void addMap(final int apid, final IDecomMapDefinition map);

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
