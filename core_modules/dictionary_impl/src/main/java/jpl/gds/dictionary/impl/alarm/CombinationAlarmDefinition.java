/*
 * Copyright 2006-2018. California Institute of Technology.
 * ALL RIGHTS RESERVED.
 * U.S. Government sponsorship acknowledged.
 *
 * This software is subject to U. S. export control laws and
 * regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 * extent that the software is subject to U.S. export control laws
 * and regulations, the recipient has the responsibility to obtain
 * export licenses or other export authority as may be required
 * before exporting such information to foreign countries or
 * providing access to foreign nationals.
 */
package jpl.gds.dictionary.impl.alarm;

import java.util.LinkedList;
import java.util.List;

import jpl.gds.dictionary.api.Categories;
import jpl.gds.dictionary.api.KeyValueAttributes;
import jpl.gds.dictionary.api.alarm.AlarmDefinitionFactory;
import jpl.gds.dictionary.api.alarm.AlarmLevel;
import jpl.gds.dictionary.api.alarm.ICombinationAlarmDefinition;
import jpl.gds.dictionary.api.alarm.ICombinationGroup;
import jpl.gds.dictionary.api.alarm.ICombinationGroupMember;
import jpl.gds.dictionary.api.alarm.ICombinationSource;
import jpl.gds.dictionary.api.alarm.ICombinationTarget;

/**
 * This class represents the dictionary definition of a combination
 * (multi-channel) alarm. A combination alarm definition maintains a top-level
 * combination source group definition, which in turn contains single-channel
 * source alarm definitions and nested combination source group definitions.
 * Sources define the conditions that trigger the alarm. It also keeps a
 * list of single channel target alarm definitions. Targets define which
 * channels the source alarm state is applied to.
 * 
 *
 */
public class CombinationAlarmDefinition implements ICombinationAlarmDefinition  {
	private final String alarmId;
	private final AlarmLevel alarmLevel;
	private ICombinationGroup source;
	private final List<ICombinationSource> flatSources = new LinkedList<ICombinationSource>();
	private final List<ICombinationTarget> targets = new LinkedList<ICombinationTarget>();
	private boolean built;
	private boolean sourcesAreTargets;
	private String alarmDescription;
	private Categories categoryMap = new Categories();
	private KeyValueAttributes keyValueAttr = new KeyValueAttributes();

	/**
	 * Constructor.
	 * 
	 * @param id the alarm identifier from the dictionary
	 * @param level the level of the combination alarm, to be applied to all targets
	 * 
	 */
	CombinationAlarmDefinition(final String id, final AlarmLevel level) {
		this.alarmId = id;
		this.alarmLevel = level;
	}
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.ICombinationAlarmDefinition#getAlarmId()
	 */
	@Override
	public String getAlarmId() {
		return alarmId;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.ICombinationAlarmDefinition#getAlarmLevel()
	 */
	@Override
	public AlarmLevel getAlarmLevel() {
		return alarmLevel;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.ICombinationAlarmDefinition#setAlarmDescription(java.lang.String)
	 */
	@Override
	public void setAlarmDescription(final String desc) {
	    this.alarmDescription = desc;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.ICombinationAlarmDefinition#getAlarmDescription()
	 */
	@Override
	public String getAlarmDescription() {
	    return alarmDescription;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.ICombinationAlarmDefinition#setCategory(java.lang.String, java.lang.String)
	 */
	@Override
	public void setCategory(String catName, String catValue) {
		categoryMap.setCategory(catName, catValue);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.ICombinationAlarmDefinition#getCategory(java.lang.String)
	 */
	@Override
	public String getCategory(String name) {
		return categoryMap.getCategory(name);
	}
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.IAlarmDefinition#getCategories()
	 */
	@Override
	public Categories getCategories() {
		return categoryMap;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.ICombinationAlarmDefinition#setCategories(jpl.gds.dictionary.impl.impl.api.Categories)
	 */
	@Override
 	public void setCategories(Categories cat) {
 		categoryMap.copyFrom(cat);
 	}
		
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.IAttributesSupport#setKeyValueAttribute(java.lang.String, java.lang.String)
	 */
	@Override	
	public void setKeyValueAttribute(String key, String value) {
		keyValueAttr.setKeyValue(key, value);
	}
	
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.IAttributesSupport#getKeyValueAttribute(java.lang.String)
	 */
	@Override	
	public String getKeyValueAttribute(String key) {
		return keyValueAttr.getValueForKey(key);
	}
	
	/**
	 *  {@inheritDoc}
	 *  @see jpl.gds.dictionary.impl.impl.api.alarm.IAlarmDefinition#getKeyValueAttributes()
	 * 
	 */
	@Override	
	public KeyValueAttributes getKeyValueAttributes() {
		return keyValueAttr;
	}
	

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.IAttributesSupport#setKeyValueAttributes(jpl.gds.dictionary.impl.impl.api.KeyValueAttributes)
	 */
	@Override	
	public void setKeyValueAttributes(KeyValueAttributes kvAttr) {
		keyValueAttr.copyFrom(kvAttr);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.ICombinationAlarmDefinition#setSourceGroup(jpl.gds.dictionary.impl.impl.api.alarm.ICombinationGroup)
	 */
	@Override
	public void setSourceGroup(final ICombinationGroup src) throws IllegalStateException {

		if (built) {
			throw new IllegalStateException(
					"The combination alarm has already been validated, and it is illegal to modify its source element");
		}

		source = src;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.ICombinationAlarmDefinition#addTarget(ICombinationTarget)
	 */
	@Override
	public synchronized void addTarget(final ICombinationTarget target)
			throws IllegalStateException {

		if (built) {
			throw new IllegalStateException(
					"The combination alarm has already been validated, and it is illegal to add a target channel");
		}

		/*
		 * Do not add more than one target to the combination for the same
		 * channel ID, level, and DN/EU flag
		 */
		for (ICombinationTarget existingTarget : targets) {
			if (existingTarget.getAlarmLevel() == target.getAlarmLevel()
					&& existingTarget.getChannelId().equals(
							target.getChannelId())
							&& existingTarget.isCheckOnDn() == target.isCheckOnDn()) {
				return;
			}
		}
		targets.add(target);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.ICombinationAlarmDefinition#build()
	 */
	@Override
	public synchronized void build() throws IllegalStateException {

		if (built) {
			throw new IllegalStateException(
					"Combination alarm "
							+ alarmId
							+ " is already built, and it is illegal to build more than once");
		}

		if (source == null) {
			throw new IllegalStateException("Combination alarm " + alarmId
					+ " validation failed: No source element");
		}

		sourcesAreTargets = targets.isEmpty();
		buildSourceProxiesList(source);

		built = true;
	}

	/**
	 * Build a flat list of source proxies from the hierarchical source element.
	 * If <code>sourcesAreTargets</code> is true, also build target proxies from
	 * the source elements. This method can be invoked recursively for the
	 * nested sources.
	 * 
	 * @param src
	 *            the source element to crawl
	 */
	private void buildSourceProxiesList(
			final ICombinationGroup src) {

		List<ICombinationGroupMember> operands = src.getOperands();

		for (ICombinationGroupMember operand : operands) {

			if (operand instanceof ICombinationSource) {
				ICombinationSource sourceProxy = (ICombinationSource)operand;

				/*
				 * Found a source proxy. Add to the flat source proxies list.
				 */
				flatSources.add(sourceProxy);

				if (sourcesAreTargets) {
					/*
					 * We also want to create a target proxy from this source.
					 * Create a target based on it and add to list of targets.
					 */
					String chanId = sourceProxy.getChannelId();
					boolean isDn = sourceProxy.isCheckOnDn();
					addTarget(AlarmDefinitionFactory.createCombinationTargetAlarm(this, chanId, isDn));

				}

			} else {
				/*
				 * Found a boolean group. Recursively crawl its operands.
				 */
				buildSourceProxiesList((ICombinationGroup) operand);

			}

		}
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.ICombinationAlarmDefinition#getFlatSources()
	 */
	@Override
	public synchronized List<ICombinationSource> getFlatSources() {

		if (!built) {
			throw new IllegalStateException(
					"Source proxies list has not been built yet; call \"build\" first");
		}

		return flatSources;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.ICombinationAlarmDefinition#getTargets()
	 */
	@Override
	public synchronized List<ICombinationTarget> getTargets() {

		if (!built) {
			throw new IllegalStateException(
					"Target proxies list has not been built yet; call \"build\" first");
		}

		return targets;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.ICombinationAlarmDefinition#getSourcesAreTargets()
	 */
	@Override
	public boolean getSourcesAreTargets() {
		return this.sourcesAreTargets;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.ICombinationAlarmDefinition#getSourceGroup()
	 */
	@Override
	public ICombinationGroup getSourceGroup() {
		return this.source;
	}
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.alarm.ICombinationAlarmDefinition#clearKeyValue()
	 */
	@Override
	public void clearKeyValue() {
		keyValueAttr.clearKeyValue();		
	}
}
