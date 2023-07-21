/**
 * 
 */
package jpl.gds.common.spring.context;

/**
 * Controlling interface to cache spring context objects. 
 * 
 *
 */
public interface IContextCache {
	
	/**
	 * Retrieves the context keyed by contextKey from the cache.  This will call the contextKey.generateContextKey
	 * method to get the key to lookup. 
	 * 
	 * @param key key to retrieve context.
	 * @return the context or null if none found keyed by key.
	 */
	public IContextContainer getContextContainer(String key);

	/**
	 * @param key lookup key
	 * @return true if container is contained else false.
	 */
	public boolean containsContextContainer(String key);
	
	/**
	 * @param key
	 * @param container
	 */
	public void addContextContainer(String key, IContextContainer container);
	
	/**
	 * @param key
	 */
	public void removeContextContainer(String key);
	
	public void stopAllChildStores();
	
	public void stopAllChildMessagePortals();
	
	public void stopAllChildRequiredServices();
}
