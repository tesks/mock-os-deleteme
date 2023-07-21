/**
 * 
 */
package jpl.gds.product.context;

import java.util.concurrent.ConcurrentHashMap;

import jpl.gds.common.spring.context.IContextCache;
import jpl.gds.common.spring.context.IContextContainer;

/**
 * A manager class used for R8 sping context driven infrastructure to keep track of session relationships between PDPP parent 
 * products and child product / sessions.  This class takes the place of the session fetch add if absent class.
 * 
 *
 */
public abstract class AbstractContextCache implements IContextCache {
	private final ConcurrentHashMap<String, IContextContainer> cache;

	/**
	 * 
	 */
	public AbstractContextCache() {
		cache = new ConcurrentHashMap<String, IContextContainer>();
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.common.spring.context.IContextCache#getContextContainer(java.lang.String)
	 */
	@Override
	public IContextContainer getContextContainer(String key) {
		return cache.get(key);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.common.spring.context.IContextCache#containsContextContainer(java.lang.String)
	 */
	@Override
	public boolean containsContextContainer(String key) {
		return cache.containsKey(key);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.common.spring.context.IContextCache#addContextContainer(java.lang.String, jpl.gds.common.spring.context.IContextContainer)
	 */
	@Override
	public void addContextContainer(String key, IContextContainer container) {
		cache.put(key, container);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.common.spring.context.IContextCache#removeContextContainer(java.lang.String)
	 */
	@Override
	public void removeContextContainer(String key) {
		cache.remove(key);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.common.spring.context.IContextCache#stopAllChildStores()
	 */
	@Override
	public void stopAllChildStores() {
		for (IContextContainer container : cache.values()) {
			container.stopChildDbStores();
		}
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.common.spring.context.IContextCache#stopAllChildMessagePortals()
	 */
	@Override
	public void stopAllChildMessagePortals() {
		for (IContextContainer container : cache.values()) {
			container.stopChildMessagePortal();
		}
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.common.spring.context.IContextCache#stopAllChildRequiredServices()
	 */
	@Override
	public void stopAllChildRequiredServices() {
		for (IContextContainer container : cache.values()) {
			container.shutdownRequiredServices();
		}
	}
	
	
}
