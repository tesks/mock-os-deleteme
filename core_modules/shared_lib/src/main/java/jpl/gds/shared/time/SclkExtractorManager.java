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
package jpl.gds.shared.time;

import java.util.Map;
import java.util.Optional;

import jpl.gds.shared.algorithm.AlgorithmConfig;
import jpl.gds.shared.algorithm.AlgorithmManager;

/**
 * This class can be used to look up sclk extractors for various time tags.
 * 
 */
public class SclkExtractorManager extends AlgorithmManager<ISclkExtractor> {

	private static final String ALGORITHM_TYPE = "sclk_extractors";
	
	// This is a map of custom time tag IDs to sclk extractor IDs
	// Used to initialize user sclk extractors lazily
	private final Map<String, String> customTimeMap;
	

	/**
     * This constructor attempts to fetch the AlgorithmConfig singleton and the
     * TimeProperties instance. Make sure the corresponding config and schema
     * files are available for TimeProperties before calling.
     * 
     * @param algoConfig
     *            AlgorithmConfiguration instance containing time-related
     *            algorithm definitions
     * @param timeConfig
     *            TimeProperties instance
     */
	public SclkExtractorManager(final AlgorithmConfig algoConfig, final TimeProperties timeConfig) {
		super(ISclkExtractor.class, ALGORITHM_TYPE, algoConfig);
		// Initialize from non-custom extractors known by Time Configuration.
		// These are only AMPCS built-in ISclkExtractor instances, which are all stateless.
		// Therefore, its ok to share these instances, unlike instances of user algorithms
		// which are unknown.
		this.algorithmsById.putAll(timeConfig.getSclkExtractorMap());
		customTimeMap = timeConfig.getCustomTimeExtractors();
	}
	

	@Override
	public Optional<ISclkExtractor> getAlgorithm(final String timeId) {
		if (customTimeMap.containsKey(timeId)) {
			return super.getAlgorithm(timeId, customTimeMap.get(timeId));
		} else {
			return super.getAlgorithm(timeId);
		}
	}

	@Override
	public String getFailureCauseFor(final String timeId) {
		if (customTimeMap.containsKey(timeId)) {
			return super.getFailureCauseFor(customTimeMap.get(timeId));
		} else {
			return super.getFailureCauseFor(timeId);
		}
	}

}
