/**
 * 
 */
package jpl.gds.common.spring.context;

import org.apache.commons.lang.StringUtils;
import org.springframework.context.ApplicationContext;

/**
 * Interface IContextContainer
 */
public interface IContextContainer {
	public static String keySeparator = "+";
	
	/**
	 * Joins all the objects into a string using string utils join method with a "+" as separator.  This is just an easy way 
	 * of making the call without having to create the array and can be variable length arguments.
	 * 
	 * @param stuff
	 * @return
	 */
	public default String join(Object... stuff) {
		return StringUtils.join(stuff, keySeparator);
	}
	
	/**
	 * @return the application context
	 */
	public ApplicationContext getApplicationContext();
	
	public void startChildDbStores();
	
	public void stopChildDbStores();
	
	public void startChildMessagePortal();
	
	public void stopChildMessagePortal();
	
	public void startRequiredServices();
	
	public void shutdownRequiredServices();
	
}

