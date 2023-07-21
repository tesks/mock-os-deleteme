package jpl.gds.dictionary.api.decom;

import java.util.Map;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.IDefinitionProviderLoadStatus;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.shared.annotation.CustomerAccessible;
import jpl.gds.shared.annotation.Mutator;

@CustomerAccessible(immutable = false)
public interface IChannelDecomDefinitionProvider extends IDefinitionProviderLoadStatus {

	/**
	 * Add a decom map corresponding to a given apid.  The mapping between
	 * the APID and the underlying decom map to load is defined by the 
	 * implementation.  
	 * @param apid the application process identifier the returned IDecomMapDefinition
	 * 		  should correspond to
	 * @return the IDecomMapDefinition resulting from loading the decom map corresponding
	 * 		   to the APID argument, or null if no map is found.
	 * @throws DictionaryException
	 */
	@Mutator
	public IDecomMapDefinition addDecomPacketMap(int apid) throws DictionaryException;


	/**
	 * Retrieves a decom map definition by APID. If no map can be found for the
	 * specified APID, returns the general map, if found.
	 * 
	 * @param apid
	 *            packet APID for the desired map
	 * @return IDecomMapDefinition for the APID's decom map, if any, or the
	 *         general map, if defined, or null if no general map is set and
	 *         there is no map for the APID
	 */
	IDecomMapDefinition getDecomMapByApid(int apid);

	/**
	 * Retrieves the general decom map definition. This is the map used if a
	 * packet APID definition states the packet is a generic decom APID, but no
	 * APID-specific map is defined.
	 * 
	 * @return the general IDecomMapDefinition object, or null if none is set
	 */
	IDecomMapDefinition getGeneralDecomMap();

    /**
     * Sets the "general" (non-APID) IDecomMapDefinition, which defines the map
     * applied when no map specific to a packet's APID can be found.
     * 
     * @param map
     *            the IDecomMapDefinition object for the general map
     */
	@Mutator
    void setGeneralMap(IDecomMapDefinition map);

	/**
	 * Retrieves a map of all the DecomMapDefinitions, keyed by APID. The
	 * returned map will NOT include the general map.
	 * 
	 * @return map of IDecomMapDefinition objects by APID
	 */
	Map<Integer, IDecomMapDefinition> getAllDecomMaps();

    /**
     * Parses a single decom map file into an IDecomMapDefinition and adds it to
     * this instance of IChannelDecomDictionary 
     * 
     * @param filename
     *            path to the map file containing the decom map to add
     * @throws DictionaryException if there is a problem locating or parsing the file
     * @return IDecomMapDefinition object resulting from the parse
     */
	@Mutator
    IDecomMapDefinition addDecomMapFromFile(String filename) throws DictionaryException;
    
    
    /**
     * Exposes the clear method.
     */
    void clear();

    /**
     * Sets the channel map object where all the channel definitions are
     * defined. When parsing a channel decom dictionary, the channels referenced
     * by the decom maps in that dictionary must be referenced in the channel
     * map. Note that this method is useless when the IChannelDecomDictionary
     * object is created by the ChannelDecomDictionaryFactory, since the factory
     * performs the parsing, and the channel map must instead be supplied
     * directly to the factory method.
     * 
     * @param chanMap
     *            channel definition map to set
     */
	@Mutator
    public void setChannelMap(Map<String, IChannelDefinition> chanMap);
    
    /**
     * Adds an existing IDecomMapDefinition to the dictionary.
     * 
     * @param apid the APID this map is for
     * @param map the IDecomMapDefinition to add
     */
	@Mutator
    public void addMap(final int apid, final IDecomMapDefinition map);

	/**
	 * Fetch a decom map by the given ID.
	 * @param id the decom map ID uniquely identifying the desired map to return
	 * @return the IDecomMapDefinition identified by the ID, or null if none is found.
	 */
	public IDecomMapDefinition getDecomMapById(IDecomMapId id);

}