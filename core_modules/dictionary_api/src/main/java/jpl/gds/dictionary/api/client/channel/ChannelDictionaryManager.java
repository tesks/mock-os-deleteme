package jpl.gds.dictionary.api.client.channel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.ICategorySupport;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.channel.IChannelDerivation;
import jpl.gds.dictionary.api.channel.IChannelDictionary;
import jpl.gds.dictionary.api.config.DictionaryType;

/**
 * Pulled this implementation out of the channel dictionary client utility in order to support optional loading.
 *
 */
public class ChannelDictionaryManager implements IChannelDictionaryManager {
    private final AtomicBoolean loadFlag = new AtomicBoolean(false); 
    private final DictionaryType type;
    private final Map<String, IChannelDefinition> channelDefs = new HashMap<String, IChannelDefinition>();
    private final SortedSet<String> modules = new TreeSet<String>();
    private final SortedSet<String> subsystems = new TreeSet<String>();
    private final SortedSet<String> opsCategories = new TreeSet<String>();
    private final Map<String, IChannelDerivation> derivationMap = new HashMap<String, IChannelDerivation>(); 
    
    public ChannelDictionaryManager(final DictionaryType type) {
        this.type = type;
    }
    
    /**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.client.channel.IChannelDictionaryManager#load(jpl.gds.dictionary.api.channel.IChannelDictionary, boolean)
	 */
    @Override
	public synchronized void load(final IChannelDictionary dict) throws DictionaryException {
        if (dict == null) {
        	throw new DictionaryException(type + " dictionary is required but could not be found");
        } else {
        	processDictionary(dict);
        	wasLoaded();
        }
    }
    
    /**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.client.channel.IChannelDictionaryManager#clear()
	 */
    @Override
	public synchronized void clear() {
        channelDefs.clear();
        modules.clear();
        subsystems.clear();
        opsCategories.clear();
        derivationMap.clear();
        loadFlag.set(false);
    }
    
    private void processDictionary(final IChannelDictionary dict) {
        if (dict == null) {
            return;
        }
        
        for (final IChannelDefinition def: dict.getChannelDefinitions()) {
            processChannelDefinition(def);
        }
        
        for (final IChannelDerivation derive : dict.getChannelDerivations()) {
            final List<String> children = derive.getChildren();
            for (final String child : children) {
                derivationMap.put(child, derive);
            }
        }
        
    }
            
    /**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.client.channel.IChannelDictionaryManager#processChannelDefinition(jpl.gds.dictionary.api.channel.IChannelDefinition)
	 */
    @Override
	public void processChannelDefinition(final IChannelDefinition def) {
        
        channelDefs.put(def.getId(), def);
        
        if (def.getCategory(ICategorySupport.MODULE) != null) {
            modules.add(def.getCategory(ICategorySupport.MODULE));
        }
        if (def.getCategory(ICategorySupport.SUBSYSTEM) != null) {
            subsystems.add(def.getCategory(ICategorySupport.SUBSYSTEM));
        }
        if (def.getCategory(ICategorySupport.OPS_CAT) != null) {
            opsCategories.add(def.getCategory(ICategorySupport.OPS_CAT));
        }
        
    }
    
    /**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.client.channel.IChannelDictionaryManager#getDefinition(java.lang.String)
	 */
    @Override
	public IChannelDefinition getDefinition(final String chanId) {
        return channelDefs.get(chanId);
    }
    
    /**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.client.channel.IChannelDictionaryManager#getModules()
	 */
    @Override
	public Set<String> getModules() {
        return this.modules;
    }
    
    
    /**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.client.channel.IChannelDictionaryManager#getSubsystems()
	 */
    @Override
	public Set<String> getSubsystems() {
        return this.subsystems;
    }
    
    
    /**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.client.channel.IChannelDictionaryManager#getOpsCategories()
	 */
    @Override
	public Set<String> getOpsCategories() {
        return this.opsCategories;
    }
    
    /**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.client.channel.IChannelDictionaryManager#getChannelIds()
	 */
    @Override
	public Set<String> getChannelIds() {
        return this.channelDefs.keySet();
    }
    
    /**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.client.channel.IChannelDictionaryManager#getChannelDefinitionMap()
	 */
    @Override
	public Map<String, IChannelDefinition> getChannelDefinitionMap() {
        return this.channelDefs;
    }
    
    /**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.client.channel.IChannelDictionaryManager#getChannelDerivationMap()
	 */
    @Override
	public Map<String, IChannelDerivation> getChannelDerivationMap() {
        return this.derivationMap;
    }
    
    /**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.api.client.channel.IChannelDictionaryManager#addDerivation(jpl.gds.dictionary.api.channel.IChannelDerivation)
	 */
    @Override
	public void addDerivation(final IChannelDerivation derive) {
        final List<String> children = derive.getChildren();
        for (final String child : children) {
            derivationMap.put(child, derive);
        }
    }

	@Override
	public boolean isLoaded() {
		return loadFlag.get();
	}

	@Override
	public void wasLoaded() {
		this.loadFlag.getAndSet(true);
		
	}

}

