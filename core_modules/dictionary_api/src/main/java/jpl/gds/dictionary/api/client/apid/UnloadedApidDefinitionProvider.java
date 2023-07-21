/**
 * 
 */
package jpl.gds.dictionary.api.client.apid;

import java.util.SortedSet;

import jpl.gds.dictionary.api.apid.IApidDefinition;
import jpl.gds.dictionary.api.apid.IApidDefinitionProvider;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.dictionary.api.exception.UnloadedDictionaryException;

/**
 * An empty definition provider that is used by the apid dictionary manager so 
 * that an anonymous inner class in not created and null checks are not needed in 
 * each method.  
 * 
 *
 */
public class UnloadedApidDefinitionProvider implements IApidDefinitionProvider {

    /**
     * Default Constructor
     */
    public UnloadedApidDefinitionProvider() {
    }


	@Override
	public IApidDefinition getApidDefinition(final int apid) {
		throw new UnloadedDictionaryException(DictionaryType.APID);
	}

	@Override
	public IApidDefinition getApidDefinition(final String apid) {
		throw new UnloadedDictionaryException(DictionaryType.APID);
	}

	@Override
	public SortedSet<Integer> getChannelApids() {
		throw new UnloadedDictionaryException(DictionaryType.APID);
	}

	@Override
	public SortedSet<Integer> getDecomApids() {
		throw new UnloadedDictionaryException(DictionaryType.APID);
	}

	@Override
	public SortedSet<Integer> getEvrApids() {
		throw new UnloadedDictionaryException(DictionaryType.APID);
	}

	@Override
	public SortedSet<Integer> getProductApids() {
		throw new UnloadedDictionaryException(DictionaryType.APID);
	}

    @Override
    public SortedSet<Integer> getCfdpApids() {
        throw new UnloadedDictionaryException(DictionaryType.APID);
    }

	@Override
	public boolean isDefinedApid(final int apid) {
		throw new UnloadedDictionaryException(DictionaryType.APID);
	}

	@Override
	public String getApidName(final int apid) {
		throw new UnloadedDictionaryException(DictionaryType.APID);
	}

}
