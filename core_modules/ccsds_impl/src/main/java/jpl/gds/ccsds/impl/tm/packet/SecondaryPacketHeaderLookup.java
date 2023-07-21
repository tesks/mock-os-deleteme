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
package jpl.gds.ccsds.impl.tm.packet;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import jpl.gds.ccsds.api.packet.ISpacePacketHeader;
import jpl.gds.ccsds.api.tm.packet.ISecondaryPacketHeader;
import jpl.gds.ccsds.api.tm.packet.ISecondaryPacketHeaderExtractor;
import jpl.gds.ccsds.api.tm.packet.ISecondaryPacketHeaderLookup;
import jpl.gds.dictionary.api.apid.IApidDefinition;
import jpl.gds.dictionary.api.apid.IApidDefinitionProvider;
import jpl.gds.dictionary.api.apid.SecondaryHeaderType;
import jpl.gds.shared.algorithm.AlgorithmConfig;
import jpl.gds.shared.algorithm.AlgorithmManager;
import jpl.gds.shared.time.ISclkExtractor;
import jpl.gds.shared.time.SclkExtractorManager;
import jpl.gds.shared.time.TimeProperties;

/**
 * SecondaryPacketHeaderLookup is used to fetch ISecondaryPacketHeaderExtractors
 * needed by packet processors.
 */
public class SecondaryPacketHeaderLookup extends AlgorithmManager<ISecondaryPacketHeaderExtractor> implements ISecondaryPacketHeaderLookup {
	
	/**
	 * This string should match the XML block used for secondary header algorithms in the algorithm
	 * config file.
	 */
	private static final String ALGORITHM_TYPE = "secondary_packet_header_extractors";
	private final SclkExtractorManager sclkExtractorLookup;
	private final ISecondaryPacketHeaderExtractor nullExtractor = new NullSecondaryHeaderExtractor();
	private final ISecondaryPacketHeaderExtractor canonicalSclkExtractor; 
	/** The apidDict reference may be null if loading the APID dictionary failed. */
	private IApidDefinitionProvider apidDefs; 
	private boolean apidDictFailed = false;
	private String apidDictFailureMessage = "";
	private final Map<Short, String> failuresByApid = new HashMap<>();


	/**
	 * Creates a new instance that utilizes the passed in apid dictionary.
	 * @param timeConfig the current time configuration
	 * @param algoConfig the current algorithm configuration
	 * @param apidDefs dictionary used to lookup secondary headers for each packet APID
	 */
	public SecondaryPacketHeaderLookup(TimeProperties timeConfig, AlgorithmConfig algoConfig, IApidDefinitionProvider apidDefs) {
		super(ISecondaryPacketHeaderExtractor.class, ALGORITHM_TYPE, algoConfig);
		canonicalSclkExtractor = new SclkSecondaryHeaderExtractor(timeConfig.getCanonicalExtractor());
		if (apidDefs == null) {
			apidDictFailed = true;
			apidDictFailureMessage = "APID dictionary was null in constructor";
			this.apidDefs = null;
		} else {
			this.apidDefs = apidDefs;
		}
		sclkExtractorLookup = new SclkExtractorManager(algoConfig, timeConfig);
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.ccsds.api.tm.packet.ISecondaryPacketHeaderLookup#lookupExtractor(jpl.gds.ccsds.api.packet.ISpacePacketHeader)
     */
	@Override
    public ISecondaryPacketHeaderExtractor lookupExtractor(ISpacePacketHeader primaryHeader) {
		// The secondary packet header adaptation is introducing a dependency on apid dictionaries for all packet processors.
		// This raises questions about how things should fail.
		// For now, the only thing to do is to suppress errors 
		// and return the extractor that matches the canonical sclk extractor
		if (primaryHeader.getSecondaryHeaderFlag() == 0) {
			return nullExtractor;
		}

		if (apidDictFailed) {
			return canonicalSclkExtractor;
		}
		
		// This method gets called and used for the map, so avoid completely redundant boxing
		final Short apid = primaryHeader.getApid();
		if (failuresByApid.containsKey(apid)) {
			return canonicalSclkExtractor;
		}

		final IApidDefinition apidDef = apidDefs.getApidDefinition(primaryHeader.getApid());
		if (apidDef == null) {
			failuresByApid.put(primaryHeader.getApid(),
					String.format("Could not find dictionary definition for APID %d", primaryHeader.getApid())
			);
			return canonicalSclkExtractor;
		}
		
		final String secondaryHeaderId = apidDef.getSecondaryHeaderExtractor();
		final SecondaryHeaderType type = apidDef.getSecondaryHeaderType();
		
		if (type == SecondaryHeaderType.TIME) {
			final Optional<ISclkExtractor> extractor = sclkExtractorLookup.getAlgorithm(secondaryHeaderId);
			if (extractor.isPresent()) {
				final ISecondaryPacketHeaderExtractor hdrExtractor = new SclkSecondaryHeaderExtractor(extractor.get());
				algorithmsById.put(secondaryHeaderId, hdrExtractor);
				return hdrExtractor;
			} else {
				algorithmFailures.put(secondaryHeaderId, sclkExtractorLookup.getFailureCauseFor(secondaryHeaderId));
				return canonicalSclkExtractor;
			}
		} else if (type == SecondaryHeaderType.CUSTOM) {
			final Optional<ISecondaryPacketHeaderExtractor> extractor = getAlgorithm(secondaryHeaderId);
			if (extractor.isPresent()) {
				return extractor.get();
			}
			return canonicalSclkExtractor;
		} else {
			failuresByApid.put(primaryHeader.getApid(),
					String.format("APID %d has invalid secondary header type: %s", primaryHeader.getApid(), type)
			);
		}
		return canonicalSclkExtractor;
	}	

	/**
     * @{inheritDoc}
     * @see jpl.gds.ccsds.api.tm.packet.ISecondaryPacketHeaderLookup#failureReasonFor(short)
     */
	@Override
    public String failureReasonFor(short apid) {
		if (apidDictFailed) {
			return apidDictFailureMessage;
		}
		return failuresByApid.getOrDefault(apid, "");
	}

    @Override
    public ISecondaryPacketHeader getNullSecondaryHeaderInstance() {
        return NullSecondaryHeader.INSTANCE;
    }

}
