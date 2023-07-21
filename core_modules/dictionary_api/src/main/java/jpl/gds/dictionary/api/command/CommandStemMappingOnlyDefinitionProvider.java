/**
 * 
 */
package jpl.gds.dictionary.api.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jpl.gds.dictionary.api.OpcodeUtil;
import jpl.gds.dictionary.api.config.DictionaryProperties;

/**
 * Special command definition provider to be used by processes that only care 
 * about the opcode to step / stem to opcode mappings.  
 * 
 *
 */
public class CommandStemMappingOnlyDefinitionProvider implements ICommandDefinitionProvider {
	private final Map<String, String> stemByOpcode;
	private final Map<String, String> opcodeByStem;
	private final OpcodeUtil opcodeUtil;
	private final String gdsVersionId;
	private final String buildVersionId;
	private final String releaseVersionId;
	

	/**
	 * Gets the mappings from rootProvider
	 * 
	 * @param rootProvider
	 */
	public CommandStemMappingOnlyDefinitionProvider(final DictionaryProperties dictConfig, final ICommandDefinitionProvider rootProvider) {
		this.stemByOpcode = rootProvider.getStemByOpcodeMap();
		this.opcodeByStem = rootProvider.getOpcodeByStemMap();
        this.opcodeUtil = new OpcodeUtil(dictConfig);
        
        this.gdsVersionId = rootProvider.getGdsVersionId();
        this.buildVersionId = rootProvider.getBuildVersionId();
        this.releaseVersionId = rootProvider.getReleaseVersionId();
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.command.ICommandDictionary#getOpcodeByStemMap()
	 */
	@Override
    public Map <String, String> getOpcodeByStemMap() {
		return new HashMap<String, String> (opcodeByStem);
	}

	
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.command.ICommandDictionary#getStemByOpcodeMap()
	 */
	@Override
    public Map <String, String> getStemByOpcodeMap() {
		return new HashMap<String, String> (stemByOpcode);
	}


	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.command.ICommandDefinitionProvider#getOpcodeForStem(java.lang.String)
	 */
	@Override
	public int getOpcodeForStem(final String stem) {
		final String opcode = opcodeByStem.get(stem.toUpperCase());	
		if (opcode == null) {
			return NO_OPCODE;
		}

        return opcodeUtil.parseOpcodeFromHex(opcode);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.command.ICommandDefinitionProvider#getStemForOpcode(int)
	 */
	@Override
	public String getStemForOpcode(final int opcode) {
		return stemByOpcode.get(opcodeUtil.formatOpcode(opcode, false));
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.command.ICommandDefinitionProvider#getCommandDefinitionForStem(java.lang.String)
	 */
	@Override
	public ICommandDefinition getCommandDefinitionForStem(final String stem) {
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.command.ICommandDefinitionProvider#getCommandDefinitionForOpcode(java.lang.String)
	 */
	@Override
	public ICommandDefinition getCommandDefinitionForOpcode(final String opcode) {
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.command.ICommandDefinitionProvider#getStems()
	 */
	@Override
	public List<String> getStems() {
		return new ArrayList<String>(stemByOpcode.keySet());
	}

	@Override
	public String getBuildVersionId() {
		return buildVersionId;
	}

	@Override
	public String getGdsVersionId() {
		return gdsVersionId;
	}

	@Override
	public String getReleaseVersionId() {
		return releaseVersionId;
	}

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.dictionary.api.IDefinitionProviderLoadStatus#isLoaded()
     */
    @Override
    public boolean isLoaded() {
        return true;
    }
}
