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

/**
 * The IChannelDerivation interface is to be implemented by all channel
 * derivation definition classes. 
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 * IChannelDerivation defines methods needed to interact with Channel Derivation
 * objects as required by the IChannelDictionary interface. It is primarily used
 * by channel file parser implementations in conjunction with the
 * ChannelDerivationFactory, which is used to create actual Channel Derivation
 * objects in the parsers. IChannelDictionary objects should interact with
 * Channel Derivation objects only through the Factory and the
 * IChannelDerivation interfaces. Interaction with the actual Channel Derivation
 * implementation classes in an IChannelDictionary implementation is contrary to
 * multi-mission development standards.
 * 
 *
 * @see IChannelDictionary
 * @see ChannelDerivationFactory
 */
public interface IChannelDerivation {
	/**
	 * Gets the unique identifier for this derivation definition.
	 * 
	 * @return unique id (name) of the derivation
	 */
	public String getId();
	
	/**
	* Gets the Derivation Type of the class that implements this interface.
	* 
	* @return ALGORITHMIC, BIT_UNPACK or DPO depending on class that
	*         implements this interface 
	*/
	public DerivationType getDerivationType();

	/**
	 * Add a channel ID of a child channel that will be output from invocation 
	 * of derivation. 
	 * 
	 * @param childId child channel ID to add
	 */
	public void addChild(String childId);
	
	/**
     * Add a parent ID of a parent channel that will is input to invocation 
     * of derivation. 
     * 
     * @param parentId parent channel ID to add
     */
    public void addParent(String parentId);
	
	/**
	 * Gets the list of parent channels for this derivation definition.
	 * @return list of channel IDs
	 */
	public List<String> getParents();
	
	/**
     * Gets the list of children channels for this derivation definition.
     * @return list of channel IDs
     */
	public List<String> getChildren();
	
}
