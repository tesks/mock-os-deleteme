package jpl.gds.dictionary.api.apid;

import java.util.SortedSet;

import jpl.gds.dictionary.api.IDefinitionProviderLoadStatus;

/**
 * Interface for ApidDefinitionProvider to implement
 * 
 *
 */
public interface IApidDefinitionProvider extends IDefinitionProviderLoadStatus {


	/**
	 * Retrieves the IApidDefinition for a specific APID number.
	 * 
	 * @param apid the APID number
	 * 
	 * @return IApidDefinition, or null if no such APID defined
	 */
	IApidDefinition getApidDefinition(int apid);

	/**
	 * Retrieves the IApidDefinition for a specific APID name.
	 * 
	 * @param apid the APID name
	 * 
	 * @return IApidDefinition, or null if no such APID defined
	 */
	IApidDefinition getApidDefinition(String apid);

    /**
     * Gets the list of APID numbers for packets that contain pre-channelized
     * information, sorted in ascending order. This list should include both
     * recorded and realtime pre-channelized APID numbers.
     * 
     * @return a non-modifiable sorted integer set of APID numbers; if there are
     *         no pre-channelized APIDs, this method may return either null or
     *         an empty set
     */
    public SortedSet<Integer> getChannelApids();

    /**
     * Gets the list of APID numbers for packets that contain channelized
     * information that must be extracted using a decommutation map, sorted in
     * ascending order. This list should include both recorded and realtime
     * decom APID numbers.
     * 
     * @return a non-modifiable sorted integer set of APID numbers; if there are
     *         no decom APIDs, this method may return either null or an empty
     *         set
     */
    public SortedSet<Integer> getDecomApids();

    /**
     * Gets the list of APID numbers for packets that contain EVR information,
     * sorted in ascending order. This list should include both recorded and
     * realtime EVR APID numbers.
     * 
     * @return a non-modifiable sorted integer set of APID numbers; if there are
     *         no EVR APIDs, this method may return either null or an empty set
     */
    public SortedSet<Integer> getEvrApids();

    /**
     * Gets the list of APID numbers for packets that contain product
     * information, sorted in ascending order. This list should include both
     * recorded and realtime product APID numbers.
     * 
     * @return a non-modifiable sorted integer set of APID numbers; if there are
     *         no product APIDs, this method may return either null or an empty
     *         set
     */
    public SortedSet<Integer> getProductApids();

    /**
     * Gets the list of APID numbers for packets that contain CFDP PDU
     * information, sorted in ascending order. This list should include both
     * recorded and realtime product APID numbers.
     * 
     * @return a non-modifiable sorted integer set of APID numbers; if there are
     *         no CFDP APIDs, this method may return either null or an empty
     *         set
     */
    public SortedSet<Integer> getCfdpApids();

    /**
     * Indicates whether the given APID number is defined in the APID
     * dictionary.
     * 
     * @param apid the APID number to look for
     * 
     * @return true if the APID is defined, false if not.
     */
    public boolean isDefinedApid(int apid);

    /**
     * Retrieves the APID name for a specific APID number
     * 
     * @param apid the APID number
     * 
     * @return name, or null if no such APID
     */
    public String getApidName(int apid);
}