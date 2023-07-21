package jpl.gds.dictionary.api.client.decom;

import java.util.Map;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.dictionary.api.decom.IChannelDecomDefinitionProvider;
import jpl.gds.dictionary.api.decom.IDecomMapDefinition;
import jpl.gds.dictionary.api.decom.IDecomMapId;
import jpl.gds.dictionary.api.exception.UnloadedDictionaryException;

public class UnloadedDecomChannelDefinitionProvider implements IChannelDecomDefinitionProvider {

	@Override
	public IDecomMapDefinition addDecomPacketMap(int apid) throws DictionaryException {
		throw new UnloadedDictionaryException(DictionaryType.DECOM);
	}

	@Override
	public IDecomMapDefinition getDecomMapByApid(int apid) {
		throw new UnloadedDictionaryException(DictionaryType.DECOM);
	}

	@Override
	public IDecomMapDefinition getGeneralDecomMap() {
		throw new UnloadedDictionaryException(DictionaryType.DECOM);
	}

	@Override
	public Map<Integer, IDecomMapDefinition> getAllDecomMaps() {
		throw new UnloadedDictionaryException(DictionaryType.DECOM);
	}

	@Override
	public void setGeneralMap(IDecomMapDefinition map) {
		throw new UnloadedDictionaryException(DictionaryType.DECOM);
	}

	@Override
	public IDecomMapDefinition addDecomMapFromFile(String filename) throws DictionaryException {
		throw new UnloadedDictionaryException(DictionaryType.DECOM);
	}

	@Override
	public void clear() {
		// no op
	}

	@Override
	public void setChannelMap(Map<String, IChannelDefinition> chanMap) {
		throw new UnloadedDictionaryException(DictionaryType.DECOM);
	}

	@Override
	public void addMap(int apid, IDecomMapDefinition map) {
		throw new UnloadedDictionaryException(DictionaryType.DECOM);
	}

	@Override
	public IDecomMapDefinition getDecomMapById(IDecomMapId id) {
		throw new UnloadedDictionaryException(DictionaryType.DECOM);
	}

}
