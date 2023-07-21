package jpl.gds.dictionary.api.client.decom;

import java.util.Map;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.decom.IChannelDecomDefinitionProvider;

public interface IChannelDecomUtilityDictionaryManager extends IChannelDecomDefinitionProvider {

	public void clear();

	void load(Map<String, IChannelDefinition> channelIdMapping) throws DictionaryException;


}
