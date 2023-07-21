package jpl.gds.dictionary.api.command;

import java.util.List;
import java.util.Map;

import jpl.gds.dictionary.api.IDefinitionProviderLoadStatus;
import jpl.gds.dictionary.api.OpcodeUtil;

public interface ICommandDefinitionProvider extends IDefinitionProviderLoadStatus {

    /** Constant indicating OPCODE is undefined. */
	public static final int NO_OPCODE = OpcodeUtil.NO_OPCODE;

    /**
     * Retrieves the opcode for stem from the
     * ICommandDefinition.
     * @param stem the command stem to get opcode for
     * 
     * @return opcode opcode may be
     * null or empty if no command definition exist
     */
    public int getOpcodeForStem(String stem);

    /**
     * Retrieves the stem for opcode from the
     * ICommandDefinition.
     * @param opcode the opcode to get the stem for
     * 
     * @return command stem; may be null
     * null or empty if no command definition exist
     */
    public String getStemForOpcode(int opcode);

	/**
	 * Retrieves the map of stem by opcode for all
	 * ICommandDefinitions.
	 * 
	 * @return	Map of stem to opcode string
	 */
	Map<String, String> getStemByOpcodeMap();

	/**
	 * Retrieves the map of opcode by stem for all
	 * ICommandDefinitions.
	 * 
	 * @return Map of opcode string to stem
	 */
	Map<String, String> getOpcodeByStemMap();

	/**
	 * Retrieves the ICommandDefinition for the given 
	 * command stem.
	 * 
	 * @param stem the command stem to get definition for
	 * 
	 * @return ICommandDefinition, null if no matching command definition exists
	 */
	ICommandDefinition getCommandDefinitionForStem(String stem);

	/**
	 * Retrieves the ICommandDefinition for the given 
	 * command opcode.
	 * 
	 * @param opcode the command opcode to get definition for
	 * 
	 * @return ICommandDefinition, null if no matching command definition exists
	 */
	ICommandDefinition getCommandDefinitionForOpcode(String opcode);

	/**
	 * Gets a sorted list of all command stems.
	 * 
	 * @return List of stem strings
	 */
	List<String> getStems();

    /**
     * Returns the FSW or SSE build ID. This is an ID used by the mission to map
     * dictionaries to specific flight or SSE software builds. There is no standard
     * format and this is not a required dictionary field.
     * 
     * @return the build version ID string, may be null
     * 
     *
     */
    public String getBuildVersionId();

    /**
     * Returns the GDS version of the dictionary.  From the AMPCS perspective,
     * this is the name of the dictionary version directory.
     * 
     * @return the dictionary version String, or the UNKNOWN constant if no version defined
     * 
     */
    
    public String getGdsVersionId();

    /**
     * Returns the FSW or SSE release version ID. This is an ID used by the
     * mission to map GDS dictionary versions to specific flight or SSE
     * dictionary versions. There is no standard format and this is not a
     * required dictionary field.
     * 
     * @return the release version ID string
     * 
     *
     */
    public String getReleaseVersionId();

}