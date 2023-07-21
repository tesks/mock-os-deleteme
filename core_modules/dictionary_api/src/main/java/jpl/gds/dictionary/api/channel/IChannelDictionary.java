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
package jpl.gds.dictionary.api.channel;

import java.util.List;
import java.util.Map;

import jpl.gds.dictionary.api.EnumerationDefinition;
import jpl.gds.dictionary.api.IBaseDictionary;
import jpl.gds.shared.annotation.CustomerAccessible;
import jpl.gds.shared.annotation.Jira;

/**
 * The IChannelDictionary interface is to be implemented by all telemetry
 * channel dictionary adaptation classes. 
 * <p>
 * <p><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <p>
 * <p>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <p>
 * 
 * The JPL document corresponding to the multimission channel dictionary schema
 * is D-001138, the AMPCS Multimission Channel Dictionary SIS, in the JPL MGSS
 * Document Management System (DMS).
 * <p>
 * The Channel dictionary is used by the telemetry processing system for the
 * extraction and creation of channelized telemetry values. It identifies the
 * type and characteristics of each supported telemetry channel for a given
 * project. An appropriate dictionary parser must be used in order to create the
 * mission-specific IChannelDictionary object, which must implement this
 * interface. IChannelDictionary objects should only be created via the
 * ChannelDictionaryFactory. Direct creation of an IChannelDictionary object is
 * a violation of multi-mission development practice.
 * <p>
 * This interface extends the IBaseDictionary interface, which requires parse()
 * and clear() methods on all dictionary objects. These methods are used to
 * populate and clear the object contents, respectively.
 * <p>
 * The primary job of the IChannelDictionary object is to produce a set of
 * IChannelDefinition objects by parsing a channel dictionary file. Each
 * IChannelDefinition is the multi-mission representation of a defined telemetry
 * channel in the dictionary.
 * <p>
 * The IChannelDictionary parsing process may also produce a set of
 * IChannelDerivation objects. Each IChannelDerivation is the multi-mission
 * representation of the method for producing a ground-derived channel in the
 * dictionary. Any time a derived IChannelDefinition is created, a matching
 * IChannelDerivation must also be produced by the dictionary parser.
 * <p>
 * Finally, the parsing process may produce Enumeration Definition objects.
 * These represent named lookup tables that define the allowed values for
 * enumerated/status channels.
 * 
 *
 * @see IChannelDictionaryFactory
 * @see jpl.gds.dictionary.api.IBaseDictionary
 */
@CustomerAccessible(immutable = true)
public interface IChannelDictionary extends IBaseDictionary, IChannelDefinitionProvider {

	/**
	 * Retrieves a list of parsed Channel Definitions from the
	 * IChannelDictionary, in the order the definitions were encountered in the
	 * dictionary file.
	 * 
	 * @return list of IChannelDefinition objects; list will be empty (not null)
	 *         if no channel definitions exist
	 */
	public List<IChannelDefinition> getChannelDefinitions();

	/**
	 * Retrieves a map of IChannelDefinition objects, keyed by channel ID, from
	 * the IChannelDictionary.
	 * 
	 * @return Map of IChannelDefinitions objects, keyed by the channel ID; map
	 *         will be empty (not null) if none exist
	 */
	@Override
	public Map<String, IChannelDefinition> getChannelDefinitionMap();
	
	@Override
    public IChannelDefinition getDefinitionFromChannelId(String channelId);

//	/**
//	 * Retrieves a list of parsed Channel Derivations from the
//	 * IChannelDictionary, in the order they were encountered in the dictionary
//	 * file.
//	 * 
//	 * @return list of IChannelDerivation objects; list will be empty (not null)
//	 *         if no derivations exist
//	 */
//	public List<IChannelDerivation> getChannelDerivations();

	/**
	 * Retrieves a list of parsed Enumeration Definitions from
	 * the IChannelDictionary.
	 * 
	 * @return Map of EnumerationDefinition objects, keyed by the enumeration
	 *         (typedef) name; map will be empty (not null) if no enumeration
	 *         definitions exist
	 */
	public Map<String, EnumerationDefinition> getEnumDefinitions();

}
