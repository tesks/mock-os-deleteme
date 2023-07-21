/**
 * 
 */
package jpl.gds.dictionary.api.client;

import jpl.gds.dictionary.api.config.DictionaryType;

/**
 * Class FlightDictionaryLoadingStrategy
 *
 */
public class FlightDictionaryLoadingStrategy extends AbstractDictionaryLoadingStrategy<FlightDictionaryLoadingStrategy> {

	/**
	 * 
	 */
	public FlightDictionaryLoadingStrategy() {
		super();
	}

	@Override
	public FlightDictionaryLoadingStrategy getThis() {
		return this;
	}
	
	/**
	 * @return boolean if sequence is enabled.
	 */
	public boolean isSequenceEnabled() {
		return isEnabled(DictionaryType.SEQUENCE);
	}
	
	/**
	 * @return this
	 */
	public FlightDictionaryLoadingStrategy enableSequence() {
		return enable(DictionaryType.SEQUENCE);
	}

	/**
	 * @return this
	 */
	public AbstractDictionaryLoadingStrategy<FlightDictionaryLoadingStrategy> disableSequence() {
		return disable(DictionaryType.SEQUENCE);
	}

	/**
     * @param enabled
     *            whether or not to enable loading sequence dictionaries
     * @return this
     */
	public AbstractDictionaryLoadingStrategy<FlightDictionaryLoadingStrategy> setSequence(final boolean enabled) {
		return enabled ? enableSequence() : disableSequence();
	}

	/**
	 * @return boolean if monitor is enabled.
	 */
	public boolean isMonitorEnabled() {
		return isEnabled(DictionaryType.MONITOR);
	}

	/**
	 * @return this
	 */
	public FlightDictionaryLoadingStrategy enableMonitor() {
		return enable(DictionaryType.MONITOR);
	}

	/**
	 * @return this
	 */
	public FlightDictionaryLoadingStrategy disableMonitor() {
		return disable(DictionaryType.MONITOR);
	}
	
	/**
     * @param enabled
     *            whether or not to enable loading monitor channel dictionary
     * @return this
     */
	public FlightDictionaryLoadingStrategy setMonitor(final boolean enabled) {
		return enabled ? enableMonitor() : disableMonitor();
	}
	
	/**
	 * @return boolean if product is enabled.
	 */
	public boolean isProductEnabled() {
		return isEnabled(DictionaryType.PRODUCT);
	}
	/**
	 * @return this
	 */
	public FlightDictionaryLoadingStrategy enableProduct() {
		return enable(DictionaryType.PRODUCT);
	}

	/**
	 * @return this
	 */
	public FlightDictionaryLoadingStrategy disableProduct() {
		return disable(DictionaryType.PRODUCT);
	}
	
	/**
     * @param enabled
     *            whether or not to enable loading product dictionary
     * @return this
     */
	public FlightDictionaryLoadingStrategy setProduct(final boolean enabled) {
		return enabled ? enableProduct() : disableProduct();
	}

	/**
	 * @return boolean if frame is enabled.
	 */
	public boolean isFrameEnabled() {
		return isEnabled(DictionaryType.FRAME);
	}
	/**
	 * @return this
	 */
	public FlightDictionaryLoadingStrategy enableFrame() {
		return enable(DictionaryType.FRAME);
	}

	/**
	 * @return this
	 */
	public FlightDictionaryLoadingStrategy disableFrame() {
		return disable(DictionaryType.FRAME);
	}

	/**
     * @param enabled
     *            whether or not to enable loading monitor frame dictionary
     * @return this
     */
	public FlightDictionaryLoadingStrategy setFrame(final boolean enabled) {
		return enabled ? enableFrame() : disableFrame();
	}

	/**
	 * @return boolean if mapper is enabled.
	 */
	public boolean isMapperEnabled() {
		return isEnabled(DictionaryType.MAPPER);
	}
	/**
	 * @return this
	 */
	public FlightDictionaryLoadingStrategy enableMapper() {
		return enable(DictionaryType.MAPPER);
	}

	/**
	 * @return this
	 */
	public FlightDictionaryLoadingStrategy disableMapper() {
		return disable(DictionaryType.MAPPER);
	}

	/**
     * @param enabled
     *            whether or not to enable loading the fsw to dictionary mapper
     * @return this
     */
	public FlightDictionaryLoadingStrategy setMapper(final boolean enabled) {
		return enabled ? enableMapper() : disableMapper();
	}
}
