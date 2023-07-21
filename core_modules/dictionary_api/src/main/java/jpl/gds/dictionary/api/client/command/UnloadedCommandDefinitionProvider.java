/**
 * 
 */
package jpl.gds.dictionary.api.client.command;

import java.util.List;
import java.util.Map;

import jpl.gds.dictionary.api.command.ICommandDefinition;
import jpl.gds.dictionary.api.command.ICommandDefinitionProvider;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.dictionary.api.exception.UnloadedDictionaryException;

/**
 * Class UnloadedCommandDefinitionProvider
 */
public class UnloadedCommandDefinitionProvider implements ICommandDefinitionProvider {

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.command.ICommandDefinitionProvider#getStemByOpcodeMap()
	 */
	@Override
	public Map<String, String> getStemByOpcodeMap() {
		throw new UnloadedDictionaryException(DictionaryType.COMMAND);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.command.ICommandDefinitionProvider#getOpcodeByStemMap()
	 */
	@Override
	public Map<String, String> getOpcodeByStemMap() {
		throw new UnloadedDictionaryException(DictionaryType.COMMAND);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.command.ICommandDefinitionProvider#getCommandDefinitionForStem(java.lang.String)
	 */
	@Override
	public ICommandDefinition getCommandDefinitionForStem(String stem) {
		throw new UnloadedDictionaryException(DictionaryType.COMMAND);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.command.ICommandDefinitionProvider#getCommandDefinitionForOpcode(java.lang.String)
	 */
	@Override
	public ICommandDefinition getCommandDefinitionForOpcode(String opcode) {
		throw new UnloadedDictionaryException(DictionaryType.COMMAND);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.command.ICommandDefinitionProvider#getStems()
	 */
	@Override
	public List<String> getStems() {
		throw new UnloadedDictionaryException(DictionaryType.COMMAND);
	}

	@Override
	public int getOpcodeForStem(String stem) {
		throw new UnloadedDictionaryException(DictionaryType.COMMAND);
	}

	@Override
	public String getStemForOpcode(int opcode) {
		throw new UnloadedDictionaryException(DictionaryType.COMMAND);
	}

	@Override
	public String getBuildVersionId() {
		throw new UnloadedDictionaryException(DictionaryType.COMMAND);
	}

	@Override
	public String getGdsVersionId() {
		throw new UnloadedDictionaryException(DictionaryType.COMMAND);
	}

	@Override
	public String getReleaseVersionId() {
		throw new UnloadedDictionaryException(DictionaryType.COMMAND);
	}

}
