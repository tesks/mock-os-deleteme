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
package jpl.gds.dictionary.impl.channel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.xml.sax.SAXParseException;

import jpl.gds.dictionary.api.EnumerationDefinition;
import jpl.gds.dictionary.api.channel.ChannelDefinitionFactory;
import jpl.gds.dictionary.api.channel.ChannelDerivationFactory;
import jpl.gds.dictionary.api.channel.ChannelType;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.channel.IChannelDerivation;
import jpl.gds.dictionary.api.channel.IChannelDictionary;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.dictionary.api.eu.EUDefinitionFactory;
import jpl.gds.dictionary.impl.AbstractBaseDictionary;
import jpl.gds.shared.annotation.Jira;

/**
 * AbstractChannelDictionary is the base class for channel dictionary parsing
 * adaptations. It must be extended to develop a complete SAX parser. It provides
 * methods for generating channel definitions, enumeration definition, and channel 
 * derivations, and lists for storing these objects once they are created by the
 * parser.
 *
 * Initialize version to "unknown" rather
 * that null so that the absence of an XML attribute with the expected
 * name does not cause a Null Pointer Exception.
 *
 */
public abstract class AbstractChannelDictionary extends AbstractBaseDictionary implements IChannelDictionary
{
	/** True if we are processing a FSW duplicate channel */
	private boolean duplicate = false;

	/**
	 * The definition of the channel currently being parsed.
	 */
	protected IChannelDefinition currentChannel;

	/**
	 * The definition of the enumeration currently being parsed.
	 */
	protected EnumerationDefinition currentEnumDef;

	private final List<IChannelDefinition> channelDefs = new LinkedList<IChannelDefinition>();
	private Map<String, IChannelDefinition> channelDefMap;
	private final List<IChannelDerivation> channelDerivations = new LinkedList<IChannelDerivation>();
	private final List<Integer> bitStarts = new LinkedList<Integer>(); 
	private final List<Integer> bitLengths = new LinkedList<Integer>(); 
	private final List<String> derivationParents = new LinkedList<String>();
	private final List<String> derivationChildren = new LinkedList<String>();
	private final Map<String,String> parameterMap = new HashMap<String,String>();
	private final List<String> parameterList = new LinkedList<String>();
	private String derivationId;
	private boolean isInStates; 
	private boolean isInDnToEu;
	private boolean inChannel;
	private boolean inEnumDef;
	private boolean inBitUnpackDerivation;
	private boolean isInAlgorithmDerivation;
	private int stateId = 0;
	private final List<Double> dnTable = new ArrayList<Double>();
	private final List<Double> euTable = new ArrayList<Double>();
	private int polyOff = 0;
	private final List<Double> coeffTable = new ArrayList<Double>();
	private String algorithmName;
	private String triggerId;

    /*  Make this map sorted for predictable results with Java 8. */
	private final Map<String, EnumerationDefinition> definedEnums = 
			new TreeMap<String, EnumerationDefinition>();

	/**
	 * Constructor.
	 * 
	 * @param maxSchemaVersion the currently implemented max schema version
	 * 
	 */
	AbstractChannelDictionary(String maxSchemaVersion) {
	    super(DictionaryType.CHANNEL, maxSchemaVersion);
	}
	
	/**
     * Constructor.
     * 
     * @param dictType the dictionary type
     * @param maxSchemaVersion the currently implemented max schema gdsDictVersion
     * 
     */
    AbstractChannelDictionary(DictionaryType dictType, String maxSchemaVersion) {
        super(dictType, maxSchemaVersion);
    }

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.IBaseDictionary#clear()
	 */
	@Override
	public void clear() {
		channelDefs.clear();
		channelDefMap = null;
		channelDerivations.clear();
		currentChannel = null;
		currentEnumDef = null;
		bitStarts.clear();
		bitLengths.clear();
		derivationParents.clear();
		derivationChildren.clear();
		derivationId = null;
		isInStates = false;
		isInDnToEu = false;
		inChannel = false;
		inEnumDef = false;
		inBitUnpackDerivation = false;
		isInAlgorithmDerivation = false;
		stateId = 0;
		polyOff = 0;
		coeffTable.clear();
		dnTable.clear();
		euTable.clear();
		algorithmName = null;
		parameterMap.clear();
		parameterList.clear();
		super.clear();	
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.channel.IChannelDictionary#getChannelDefinitions()
	 */
	@Override
	public List<IChannelDefinition> getChannelDefinitions() {
		return new LinkedList<IChannelDefinition>(channelDefs);
	}
	
	@Override
    public IChannelDefinition getDefinitionFromChannelId(String id) {
	   return getChannelDefinitionMap().get(id);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.channel.IChannelDictionary#getChannelDerivations()
	 */
	@Override
	public List<IChannelDerivation> getChannelDerivations() {
		return new LinkedList<IChannelDerivation>(channelDerivations);
	}
	
	@Override
    public SortedSet<String> getChanIds() {
	    return new TreeSet<String>(channelDefMap.keySet());
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.channel.IChannelDictionary#getEnumDefinitions()
	 */
	@Override
	public Map<String, EnumerationDefinition> getEnumDefinitions() {
	    /* Make this a sorted map */
		return new TreeMap<String, EnumerationDefinition>(definedEnums);
	}

	
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.channel.IChannelDictionary#getChannelDefinitionMap()
	 */
	@Override
	public synchronized Map<String, IChannelDefinition> getChannelDefinitionMap() {
	    if (channelDefMap == null) { 
	        channelDefMap = new TreeMap<String, IChannelDefinition>();
	        for (final IChannelDefinition channel : this.getChannelDefinitions()) {
	            channelDefMap.put(channel.getId(), channel);
	        }
	    }
		return channelDefMap;
	}
	
	/**
	 * Set channel size base on channel type for monitor and header channels.
	 * @param type the channel data type
	 */
	public void setChannelSize(final ChannelType type) {
        /* 
         * Set monitor channel size based upon type. This is
         * here because the old format monitor/header dictionary had no channel lengths. Channel
         * lengths are needed for multimission conversion.  Thus I am defaulting them 
         * here.
         */
        switch(type) {
        case UNSIGNED_INT:
        case SIGNED_INT:
        case FLOAT:
        case DIGITAL:
            currentChannel.setSize(64);
            break;
        case STATUS:
            currentChannel.setSize(8);
            break;
        default:
            break;
        }
	    
	}

	/**
	 * Starts building or updating of a monitor channel definition by the parser. If a definition
	 * already exists in the channel definition table with the given ID, then that definition
	 * will become the current definition and the parser will update it. If not, a new
	 * channel definition will be created.
	 *
	 * @param cid the ChannelId of the new channel
	 * @param type the channel data type
	 */
	public void startMonitorChannelDefinition(final String cid, final ChannelType type) {
		currentChannel = ChannelDefinitionFactory.createMonitorChannel(cid, type);
		setChannelSize(type);
		inChannel = true;
	}

	/**
	 * Starts building or updating of a header channel definition by the parser. If a definition
	 * already exists in the channel definition table with the given ID, then that definition
	 * will become the current definition and the parser will update it. If not, a new
	 * channel definition will be created.
	 *
	 * @param cid the ChannelId of the new channel
	 * @param type the channel data type
	 */
	public void startHeaderChannelDefinition(final String cid, final ChannelType type) {
		currentChannel = ChannelDefinitionFactory.createHeaderChannel(cid, type);
        setChannelSize(type);
		inChannel = true;
	}

	/**
	 * Starts building or updating of a flight channel definition by the parser. If a definition
	 * already exists in the channel definition table with the given ID, then that definition
	 * will become the current definition and the parser will update it. If not, a new
	 * channel definition will be created.
	 *
	 * @param cid the ChannelId of the new channel
	 * @param type the channel data type
	 * 
	 */
	public void startFlightChannelDefinition(final String cid, final ChannelType type)
	{
		/*
		 * Removing some commented out
		 * stuff for handling duplicate channels I forgot to remove before. 
		 * This class can no longer access the Channel Definition Table.
		 */

		if (type == null)
		{
			currentChannel = ChannelDefinitionFactory.createFlightChannel(cid);
		}


		else
		{
			currentChannel = ChannelDefinitionFactory.createFlightChannel(cid, type);
		}

		inChannel = true;
	}

	/**
	 * Starts building or updating of an SSE channel definition by the parser. If a definition
	 * already exists in the channel definition table with the given ID, then that definition
	 * will become the current definition and the parser will update it. If not, a new
	 * channel definition will be created.
	 *
	 * @param cid the ChannelId of the new channel
	 * @param type the channel data type
	 * 
	 */
	public void startSseChannelDefinition(final String cid, final ChannelType type)
	{
		/*
		 * Removing some commented out
		 * stuff for handling duplicate channels I forgot to remove before. 
		 * This class can no longer access the Channel Definition Table.
		 */

		if (type == null)
		{
			currentChannel = ChannelDefinitionFactory.createSseChannel(cid);
		}
		else
		{
			currentChannel = ChannelDefinitionFactory.createSseChannel(cid, type);
		}

		inChannel = true;
	}


	/**
	 * Starts building or updating of a header channel definition by the parser. If a definition
	 * already exists in the channel definition table with the given ID, then that definition
	 * will become the current definition and the parser will update it. If not, a new
	 * channel definition will be created.
	 *
	 * @param cid the ChannelId of the new channel
	 */
	public void startHeaderChannelDefinition(final String cid) {
		currentChannel = ChannelDefinitionFactory.createHeaderChannel(cid);
		inChannel = true;
	}

	/**
	 * Starts building or updating of a flight channel definition by the parser.
	 * If a definition already exists in the channel definition table with the
	 * given ID, then that definition will become the current definition and the
	 * parser will update it. If not, a new channel definition will be created.
	 * 
	 * @param cid
	 *            the ChannelId of the new channel
	 * 
	 */
	public void startFlightChannelDefinition(final String cid)
	{
		startFlightChannelDefinition(cid, null);
	}

	/**
	 * Starts building or updating of an SSE channel definition by the parser.
	 * If a definition already exists in the channel definition table with the
	 * given ID, then that definition will become the current definition and the
	 * parser will update it. If not, a new channel definition will be created.
	 * 
	 * @param cid
	 *            the ChannelId of the new channel
	 * 
	 */
	public void startSseChannelDefinition(final String cid)
	{
		startSseChannelDefinition(cid, null);
	}


	/**
	 * Indicates whether we are currently parsing a channel definition.
	 * @return true if parsing a channel
	 */
	public boolean inChannelDefinition() {
		return inChannel;
	}


	/**
	 * Completes the building or updating of the current channel definition.
	 */
	public void endChannelDefinition()
	{
		if (! duplicate)
		{
			channelDefs.add(currentChannel);
		}

		duplicate      = false;
		currentChannel = null;
		inChannel      = false;
	}


	/**
	 * Cancels the definition of the channel in progress.
	 */
	protected void cancelChannelDefinition() {
		currentChannel = null;
		inChannel = false;
	}

	/**
	 * Starts parsing of a bit unpack channel derivation.
	 * @param parent the parent channel ID for the derivation
	 */
	protected void startBitUnpackDerivation(final String parent)
	{
		inBitUnpackDerivation = true;
		derivationParents.add(parent);
	}

	/**
	 * Indicate if we are currently parsing a bit unpack channel derivation.
	 * 
	 * @return true if in a bit unpack derivation, false if not
	 */
	protected boolean isInBitUnpackDerivation() {
		return inBitUnpackDerivation;
	}

	/**
	 * Ends parsing of a bit unpack channel derivation.
	 */
	protected void endBitUnpackDerivation() 
	{
		final IChannelDerivation derivation = ChannelDerivationFactory.createBitUnpackDerivation(derivationParents.get(0), derivationChildren.get(0), 
				bitStarts, bitLengths);

		channelDerivations.add(derivation);
		this.currentChannel.setSourceDerivationId(derivation.getId());
		inBitUnpackDerivation = false;
		derivationParents.clear();
		derivationChildren.clear();
		bitStarts.clear();
		bitLengths.clear();
	}

	/**
	 * Adds a bit range to the current bit unpack derivation.
	 * 
	 * @param start start bit of the bit range
	 * @param len length of the bit range
	 */
	protected void addBitUnpackRange(final int start, final int len) {
		bitStarts.add(start);
		bitLengths.add(len);
	}

	/**
	 * Starts parsing of an algorithm channel derivation.
	 *
	 * @param id the id or trigger channel for the derivation
	 *
	 */
	protected void startAlgorithmicDerivation(final String id)
	{
		derivationId = id;
		isInAlgorithmDerivation      = true;
	}

	/**
	 * Adds a parent to the derivation currently being parsed.
	 * @param parentId parent channel ID to add
	 */
	protected void addDerivationParent(final String parentId) {
		derivationParents.add(parentId);
	}

	/**
	 * Adds a child to the derivation currently being parsed.
	 * @param childId child channel ID to add
	 */
	protected void addDerivationChild(final String childId) {
		derivationChildren.add(childId);
	}

	private IChannelDerivation lookupDerivationByName(final String derivationId) throws SAXParseException {
		for (final IChannelDerivation cd : channelDerivations) {
			if (cd.getId().equals(derivationId)) {
				return cd;
			}
		}
		error("Referencing undefined derivation id: "+derivationId);
		return null; // NOTREACHED
	}
	/**
	 * Alternate form of this method for use when the association is declared in the
	 * channel definition that references the derivation by name. So we have to lookup
	 * the derivation in order to add the association to it.
	 * @param derivationId ID of derivation to add to
	 * @param childId child channel ID to add
	 * 
	 * @throws SAXParseException of the derivation is not defined
	 */
	protected void addDerivationChild(final String derivationId, final String childId) throws SAXParseException {
		final IChannelDerivation algorithm = lookupDerivationByName(derivationId);
		algorithm.addChild(childId);
	}

	/**
	 * Indicates whether we are currently parsing an algorithmic channel derivation.
	 * @return true if parsing an algorithmic derivation
	 */
	protected boolean inAlgorithmDerivation() {
		return isInAlgorithmDerivation;
	}

	/**
	 * This logic really should be in endAlgorithmicDerivation but I put it here in order to
	 * preserve the signature of that method for backward compatibility. The minimum requirement
	 * of a derivation is that it defines at least one parent channel id and an algorithm.
	 * (I don't know why it makes sense to trap exceptions in endAlgorithmicDerivation and then
	 * continually generate runtime exceptions when trying to execute the algorithms, but I don't
	 * want to break any other mission parsers that might expect this behavior.  It also seems a
	 * bit redundant that we have to specify parent/child channel IDs both here and in the algorithm
	 * itself. This makes it possible to specify the parents and children differently in the algorithm
	 * and the dictionary, which would be hard to diagnose. -- DAW).
	 * 
	 * @throws SAXParseException if there is an error in the algorith definition
	 */
	protected void checkAlgorithmRequirements() throws SAXParseException {
		/*
		 * Removed check for whether we are doing
		 * derivations.  The derivation definitions are now loaded from the 
		 * dictionary regardless, since the dictonary classes no longer
		 * have access to the GDS configuration. 
		 */
		if (derivationParents.isEmpty()) {
			error("Derivation with no parent channels defined: "+derivationId);
		}

		if (algorithmName == null || algorithmName.isEmpty()) {
			error("Derivation with no algorithm defined: "+derivationId);
		}
	}

	/**
	 * Ends parsing of an algorithmic channel derivation.
	 */
	protected void endAlgorithmicDerivation() 
	{
		/*
		 * Removed check for whether we are doing
		 * derivations.  The derivation definitions are now loaded from the 
		 * dictionary regardless, since the dictionary classes no longer
		 * have access to the GDS configuration. In addition, algorithms classes 
		 * are not loaded here any more, so handling for bad algorithm reporting
		 * has been removed from the code below.
		 */
		try {
			IChannelDerivation derivation = null;


			/* 
			 * Add trigger ID to calls to create derivations below.
			 */
			if (! parameterMap.isEmpty())
			{
				derivation =
						ChannelDerivationFactory.createAlgorithmicDerivation(
								derivationId,
								algorithmName, derivationParents,
								derivationChildren, parameterMap,
								triggerId);
			}
			else
			{
				derivation =
						ChannelDerivationFactory.createAlgorithmicDerivation(
								derivationId,
								algorithmName, derivationParents,
								derivationChildren, parameterList, 
								triggerId);
			}

			channelDerivations.add(derivation); 
		} catch (final IllegalArgumentException e) {
			tracer.error(e.getMessage());
		}

		isInAlgorithmDerivation = false;
		derivationParents.clear();
		derivationChildren.clear();
		parameterMap.clear();
		parameterList.clear();
		derivationId = null;
		algorithmName = null;
	}

	/**
	 * Adds a parameter to the current derivation or algorithmic EU using a key name.
	 * 
	 * @param key name of the parameter; must be unique within derivation
	 * @param value value of the parameter
	 * 
	 */
	protected void addNamedParameter(String key, final String value) {
		if (key == null) {
			key = String.valueOf(parameterMap.size() + 1);
		}
		parameterMap.put(key, value);
		parameterList.add(value);
	}

	/**
	 * Adds a parameter to the current derivation or algorithmic EU; 
	 * assumes positional rather than keyed parameters.
	 * 
	 * @param value value of the parameter
	 * 
	 */
	protected void addPositionalParameter(final String value) {
		parameterList.add(value);
	}

	/**
	 * Starts parsing of an enumerated type definition.
	 * @param name the name of the enumeration
	 */
	protected void startEnumDefinition(final String name) {
		inEnumDef = true;
		currentEnumDef = new EnumerationDefinition(name);
	}

	/**
	 * Indicates whether we are in the process of parsing an enumerated type definition.
	 * @return rue if parsing an enumeration
	 */
	protected boolean inEnumDefinition() {
		return inEnumDef;
	}

	/**
	 * Ends parsing of an enumerated type definition.
	 */
	protected void endEnumDefinition() {
		definedEnums.put(currentEnumDef.getName(), currentEnumDef);
		currentEnumDef = null;
		inEnumDef = false;
	}

	/**
	 * Gets a previously parsed enumerated type definition.
	 * 
	 * @param name the name of the enumeration to get
	 * @return the EnumerationDefinition, or null if the name is not found
	 */
	protected EnumerationDefinition getEnumDefinition(final String name) {
		return definedEnums.get(name);
	}

	/**
	 * Starts building of a polynomial DN to EU conversion.
	 */
	protected void startDnToEu() {
		isInDnToEu = true;
	}

	/**
	 * Indicates we are currently parsing a DN to EU conversion.
	 * 
	 * @return true if parsing DN to EU
	 */
	protected boolean inDnToEu() {
		return isInDnToEu;
	}

	/**
	 * Sets the current coefficient index in the current polynomial DN to EU conversion.
	 *
	 * @param index the 0-based index
	 */
	protected void setDnToEuPolyIndex(final int index) {
		polyOff = index;
	}

	/**
	 * Sets the coefficient corresponding to the current coefficient index in the
	 * current polynomial DN to EU conversion.
	 *
	 * @param coef the coefficient value
	 */
	protected void setDnToEuPolyCoefficient(final double coef) {
		while (coeffTable.size() - 1 < polyOff) {
			coeffTable.add(null);
		}
		coeffTable.set(polyOff, coef);
	}

	/** 
	 * Add a polynomial coefficient at next available index (starting at zero).
	 * This method is used when factors are expressed as an ordered list rather than as
	 * individually indexed coefficients. Values are added assuming the initial index is
	 * zero.
	 * 
	 * @param coef the coefficient value to add
	 */
	protected void addDnToEuPolyCoefficient(final double coef) {
		coeffTable.add(coef);
	}

	/**
	 * Ends building of a polynomial DN to EU conversion.
	 */
	protected void endDnToEuPoly() {
		/* 
		 * Used to catch NullPointerException below. I changed
		 * what the factory throws to IllegalArgumentException.
		 */
		try {
			currentChannel.setDnToEu(EUDefinitionFactory.createPolynomialEU(coeffTable));
			/* Make sure hasEu() is set on the definition. */
			currentChannel.setHasEu(true);
		} catch(final IllegalArgumentException e) {
			tracer.error("A polynomial coefficient is missing from the channel dictionary. DN to EU conversions will not be performed for channel " + currentChannel.getId());
		}
		isInDnToEu = false;
		coeffTable.clear();
	}

	/**
	 * Sets the current DN for the current table interpolation DN to EU conversion.
	 * @param dn the data number to set
	 */
	protected void setTableDn(final double dn) {
		dnTable.add(dn);
	}

	/**
	 * Sets the current EU for the current table interpolation DN to EU conversion.
	 * @param eu the engineering units to set
	 */
	protected void setTableEu(final double eu) {
		euTable.add(eu);
	}

	/**
	 * Ends building of a table interpolation DN to EU conversion.
	 * @throws SAXParseException if the table does not contain at least two entries
	 */
	protected void endDnToEuTable() throws SAXParseException {
		if (dnTable.size() < 2) {
			throw new SAXParseException("DN-to-EU lookup table must contain at least two points", locator);
		}
		currentChannel.setDnToEu(EUDefinitionFactory.createTableEU(dnTable, euTable));
		/*  Make sure hasEu() is set on the definition. */
		currentChannel.setHasEu(true);
		isInDnToEu = false;
		dnTable.clear();
		euTable.clear();
	}

	/**
	 * Sets the algorithm name for the current algorithmic derivation or DN to EU conversion.
	 * @param name the algorithm name (Java class name)
	 */
	public void setAlgorithmName(final String name) {
		algorithmName = name;
	}

	/**
	 * Ends building of a simple algorithmic DN to EU conversion.
	 */
	protected void endDnToEuAlgorithmic() {
		currentChannel.setDnToEu(EUDefinitionFactory.createAlgorithmicEU(algorithmName));
		/* Make sure hasEu() is set on the definition. */
		currentChannel.setHasEu(true);
		algorithmName = null;
		isInDnToEu = false;
	}

	/**
	 * Ends building of an algorithmic DN to EU conversion.
	 */
	protected void endDnToEuParameterizedAlgorithmic() {

		currentChannel.setDnToEu(EUDefinitionFactory.createAlgorithmicEU(currentChannel.getId(), algorithmName,
				new HashMap<String,String>(parameterMap)));

		currentChannel.setHasEu(true);
		algorithmName = null;
		isInDnToEu = false;
		parameterMap.clear();
		parameterList.clear();
	}

	/**
	 * Starts building a state table mapping (unnamed enumeration).
	 */
	protected void startStates() {
		currentEnumDef = new EnumerationDefinition("no-name");
		isInStates = true;
	}

	/**
	 * Indicates if we are currently building a state table mapping.
	 *
	 * @return true if building a state table
	 */
	protected boolean inStates() {
		return isInStates;
	}

	/**
	 * Sets the current index (key) for the current state table mapping
	 * or enumeration definition.
	 * @param id the id/key to set
	 */
	protected void setCurrentEnumIndex(final int id) {
		stateId = id;
	}

	/**
	 * Sets the value to be mapped to the current index (key) for the current
	 * state table mapping or enumeration definition.
	 * @param value the value to set
	 */
	protected void setCurrentEnumValue(final String value) {
		currentEnumDef.addValue(stateId, value);
	}

	/**
	 * Ends building of a state table mapping (unnamed enumeration).
	 */
	protected void endStates() {
		isInStates = false;
		definedEnums.put(currentEnumDef.getName(), currentEnumDef);
		currentChannel.setLookupTable(currentEnumDef);
		currentEnumDef = null;
	}

	/**
	 * Sets the trigger channel identifier for the current algorithmic
	 * derivation. If set, and the mission is configured to use trigger
	 * channels, the derivation will be triggered when this channel is seen
	 * incoming. If null, the derivation will trigger when any incoming parent
	 * channel is seen.
	 * 
	 * @param triggerChannel
	 *            the ID of the trigger channel, which must be a parent 
	 *            of the derivation if non-null, and may be null
	 */
	protected void setAlgorithmTriggerId(final String triggerChannel) {
		this.triggerId = triggerChannel;
	}
}
