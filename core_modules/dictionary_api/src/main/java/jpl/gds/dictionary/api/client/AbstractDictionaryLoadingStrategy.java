/**
 * 
 */
package jpl.gds.dictionary.api.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.context.ApplicationContext;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.client.alarm.IAlarmDictionaryManager;
import jpl.gds.dictionary.api.client.apid.IApidUtilityDictionaryManager;
import jpl.gds.dictionary.api.client.channel.IChannelUtilityDictionaryManager;
import jpl.gds.dictionary.api.client.command.ICommandUtilityDictionaryManager;
import jpl.gds.dictionary.api.client.decom.IChannelDecomUtilityDictionaryManager;
import jpl.gds.dictionary.api.client.evr.IEvrUtilityDictionaryManager;
import jpl.gds.dictionary.api.client.frame.ITransferFrameUtilityDictionaryManager;
import jpl.gds.dictionary.api.client.sequence.ISequenceUtilityDictionaryManager;
import jpl.gds.dictionary.api.config.DictionaryType;

/**
 * This dictates what dictionaries will be loaded for a context. When created all
 * dictionary types are set as disabled and it is up to the application to enable all
 * the types that should be loaded.
 * 
 * 
 * This class uses a builder-esque model so that enabling / disabling can be chained.
 * 
 * @param <T>
 *            Dictionary loading strategy type (FSW or SSE)
 *
 */
public abstract class AbstractDictionaryLoadingStrategy<T extends AbstractDictionaryLoadingStrategy<T>> {
	private final Map<DictionaryType, Boolean> dictLoadingEnable;

    @SuppressWarnings("javadoc")
    public AbstractDictionaryLoadingStrategy() {
        dictLoadingEnable = new HashMap<>();
		
		for (final DictionaryType type : DictionaryType.values()) {
			set(type, false);
		}
	}
	
	
	private T set(final DictionaryType type, final boolean enabled) {
			dictLoadingEnable.put(type, enabled);
			return getThis();
	}
	
	/**
	 * @return returns this object
	 */
	public abstract T getThis();

	/**
     * Checks if dicitonary type is enabled to be loaded.
     * 
     * @param type
     *            the dictionary type to check whether or not it is enabled
     * @return true if enabled false if disabled.
     */
	public boolean isEnabled(final DictionaryType type) {
		return dictLoadingEnable.get(type);
	}

	
	/**
     * @param type
     *            the dictionary type to enable
     * @return T<AbstractDictionaryLoadingStrategy>
     */
	public T enable(final DictionaryType type) {
		return set(type, true);
	}

	/**
     * @param type
     *            the dictionary type to disable
     * @return T<AbstractDictionaryLoadingStrategy>
     */
	public T disable(final DictionaryType type) {
		return set(type, false);
	}

	/**
	 * @return boolean if header is enabled.
	 */
	public boolean isAlarmEnabled() {
		return isEnabled(DictionaryType.ALARM);
	}

	/**
	 * @return this
	 */
	public T enableAlarm() {
		return enable(DictionaryType.ALARM);
	}

	/**
	 * @return this
	 */
	public T disableAlarm() {
		return disable(DictionaryType.ALARM);
	}

	/**
     * @param enabled
     *            whether or not to enable loading the alarm dictionary
     * @return this
     */
	public T setAlarm(final boolean enabled) {
		return enabled ? enableAlarm() : disableAlarm();
	}

	/**
	 * @return boolean if header is enabled.
	 */
	public boolean isHeaderEnabled() {
		return isEnabled(DictionaryType.HEADER);
	}

	/**
	 * @return this
	 */
	public T enableHeader() {
		return enable(DictionaryType.HEADER);
	}

	/**
	 * @return this
	 */
	public T disableHeader() {
		return disable(DictionaryType.HEADER);
	}
	
	/**
     * @param enabled
     *            whether or not to enable loading the header channel dictionary
     * @return this
     */
	public T setHeader(final boolean enabled) {
		return enabled ? enableHeader() : disableHeader();
	}

	/**
	 * @return boolean if channel is enabled.
	 */
	public boolean isChannelEnabled() {
		return isEnabled(DictionaryType.CHANNEL);
	}


	/**
	 * @return this
	 */
	public T enableChannel() {
		return enable(DictionaryType.CHANNEL);
	}

	/**
	 * @return this
	 */
	public T disableChannel() {
		return disable(DictionaryType.CHANNEL);
	}
	
	/**
     * @param enabled
     *            whether or not to enable loading the channel dictionary
     * @return this
     */
	public T setChannel(final boolean enabled) {
		return enabled ? enableChannel() : disableChannel();
	}

	/**
	 * @return boolean if evr is enabled.
	 */
	public boolean isEvrEnabled() {
		return isEnabled(DictionaryType.EVR);
	}

	/**
	 * @return this
	 */
	public T disableEvr() {
		return disable(DictionaryType.EVR);
	}
	
	
	/**
	 * @return this
	 */
	public T enableEvr() {
		return enable(DictionaryType.EVR);
	}

	/**
     * @param enabled
     *            whether or not to enable loading the evr dictionary
     * @return this
     */
	public T setEvr(final boolean enabled) {
		return enabled ? enableEvr() : disableEvr();
	}

	/**
	 * @return boolean if apid is enabled.
	 */
	public boolean isApidEnabled() {
		return isEnabled(DictionaryType.APID);
	}

	/**
	 * @return this
	 */
	public T disableApid() {
		return disable(DictionaryType.APID);
	}
	
	/**
	 * @return this
	 */
	public T enableApid() {
		return enable(DictionaryType.APID);
	}
	
	/**
     * @param enabled
     *            whether or not to enable loading the apid dictionary
     * @return this
     */
	public T setApid(final boolean enabled) {
		return enabled ? enableApid() : disableApid();
	}

	
	/**
	 * @return boolean if command is enabled.
	 */
	public boolean isCommandEnabled() {
		return isEnabled(DictionaryType.COMMAND);
	}

	/**
	 * @return this
	 */
	public T enableCommand() {
		return enable(DictionaryType.COMMAND);
	}

	/**
	 * @return this
	 */
	public T disableCommand() {
		return disable(DictionaryType.COMMAND);
	}
	
	/**
     * @param enabled
     *            whether or not to enable loading the command dictionary
     * @return this
     */
	public T setCommand(final boolean enabled) {
		return enabled ? enableCommand() : disableCommand();
	}
	
    /**
     * Loads all of the enabled dictionaries.
     * 
     * @param appContext
     *            the current application context
     * 
     * @param includeRequired
     *            whether or not to include required dictionaries
     * @return T<AbstractDictionaryLoadingStrategy>
     * @throws DictionaryException
     *             if an error occurs loading the dictionary
     */
	public T loadAllEnabled(final ApplicationContext appContext, final boolean includeRequired) throws DictionaryException {
		return loadAllEnabled(appContext, includeRequired, false);
	}

	/**
     * Loads all of the enabled dictionaries.
     * 
     * @param appContext
     *            the current application context
     * 
     * @param includeRequired
     *            whether or not to include required dictionaries
     * @param commandsStemMapsOnly
     *            whether or not to only load command step maps
     * @return T<AbstractDictionaryLoadingStrategy>
     * @throws DictionaryException
     *             if an error occurs loading the dictionary
     */
	public T loadAllEnabled(final ApplicationContext appContext, final boolean includeRequired, final boolean commandsStemMapsOnly) throws DictionaryException {

		/**
		 * Since other things are relying on the channel table, do that first.  Note this is going to 
		 * bomb on the alarm or decom if the channel is not enabled.
		 * 
		 * Checking if things are loaded and skipping if they are.
		 */
		try {
			final IChannelUtilityDictionaryManager channelManager = appContext.getBean(IChannelUtilityDictionaryManager.class);
			if (!channelManager.isLoaded()) {
				channelManager.loadAll(includeRequired);
			}
			
			for (final Entry<DictionaryType, Boolean> entry : this.dictLoadingEnable.entrySet()) {
				switch(entry.getKey()) {
				case ALARM:
					final IAlarmDictionaryManager am = appContext.getBean(IAlarmDictionaryManager.class);
					
					if (am != null && !am.isLoaded()) {
						am.loadAll(channelManager.getChannelDefinitionMap());
					}
					break;
				case APID:
					 final IApidUtilityDictionaryManager pm = appContext.getBean(IApidUtilityDictionaryManager.class);
					
					if (pm != null && !pm.isLoaded()) {
						pm.load();
					}

					break;
				case CHANNEL:
					// Already done
					break;
				case COMMAND:
					final ICommandUtilityDictionaryManager cm = appContext.getBean(ICommandUtilityDictionaryManager.class);
					
					if (cm != null && !cm.isLoaded()) {
						cm.load(commandsStemMapsOnly);
					}
					break;
				case DECOM:
					final IChannelDecomUtilityDictionaryManager dm = appContext.getBean(IChannelDecomUtilityDictionaryManager.class);
					
					if (dm != null && !dm.isLoaded()) {
						dm.load(channelManager.getChannelDefinitionMap());
					}
					break;
				case EVR:
					final IEvrUtilityDictionaryManager em = appContext.getBean(IEvrUtilityDictionaryManager.class);
					
					if (em != null && !em.isLoaded()) {
						em.loadAll();
					}
					break;
				case FRAME:
					final ITransferFrameUtilityDictionaryManager tm = appContext.getBean(ITransferFrameUtilityDictionaryManager.class);
					
					if (tm != null && !tm.isLoaded()) {
						tm.load();
						
					}
					break;
				case HEADER:
					// This is done in the channel 
					break;
				case MONITOR:
					// This is done in the channel 
					break;
				case PRODUCT:
					// Not currently working.
					break;
				case SEQUENCE:
					final ISequenceUtilityDictionaryManager sm = appContext.getBean(ISequenceUtilityDictionaryManager.class);
					if (sm != null && !sm.isLoaded()) {
						sm.load(includeRequired);
					}
					break;
				case MAPPER:
					// do nothing
				}
			}
		} catch (final Exception e) {
			e.printStackTrace();
			throw new DictionaryException("Failed to load dictionaries", e);
		}
		
		return getThis();
	}


	/**
	 * Enable the decom dictionary.
	 * @return this
	 * 
	 */
	public T enableDecom() {
	    return enable(DictionaryType.DECOM);
	}


	/**
	 * Disable the decom dictionary.
	 * @return this
	 * 
	 */
	public T disableDecom() {
	    return disable(DictionaryType.DECOM);
	}


	/**
	 * Sets the decom dictionary enable flag.
	 * @param enabled true to enable, false to disable
	 * @return this
	 * 
	 */
	public T setDecom(final boolean enabled) {
	    return enabled ? enableDecom() : disableDecom();
	}
	
	/**
     * @return boolean if decom is enabled.
     */
    public boolean isDecomEnabled() {
        return isEnabled(DictionaryType.DECOM);
    }
}
