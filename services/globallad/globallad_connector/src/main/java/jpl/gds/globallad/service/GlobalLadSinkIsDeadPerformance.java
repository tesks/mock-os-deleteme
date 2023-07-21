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
package jpl.gds.globallad.service;

import java.util.Arrays;
import java.util.List;

import jpl.gds.shared.config.PerformanceProperties;
import jpl.gds.shared.performance.BinaryStatePerformanceData;
import jpl.gds.shared.performance.IPerformanceData;
import jpl.gds.shared.performance.IPerformanceProvider;

/**
 * Performance provider used when the global lad feature has been shut off due to errors 
 * adding events to the ring buffer.
 */
public class GlobalLadSinkIsDeadPerformance implements IPerformanceProvider {
	public static final String PROVIDER_NAME = "GlobalLadSink";

	public static class GlobalLadSinkPerformanceData extends BinaryStatePerformanceData {

        /**
         * @param isBadRed
         * @param perfProps
         */
		public GlobalLadSinkPerformanceData(boolean isBadRed, PerformanceProperties perfProps) {
			super(perfProps, PROVIDER_NAME, isBadRed);
            new PerformanceProperties();
		}

		/**
		 * {@inheritDoc}
		 * @see jpl.gds.shared.performance.BinaryStatePerformanceData#toLogString()
		 */
		@Override
		public String toLogString() {
			return new StringBuilder(super.toLogString()) 
					.append(" has failed and is not pushing data to the Global LAD server.")
					.toString();
		}
	}

	private List<IPerformanceData> data;
	
	/**
	 * @param data
	 */
	public GlobalLadSinkIsDeadPerformance(GlobalLadSinkPerformanceData data) {
		super();
		this.data = Arrays.asList(data);
	}

	@Override
	public String getProviderName() {
		return PROVIDER_NAME;
	}

	@Override
	public List<IPerformanceData> getPerformanceData() {
		return data;
	}
}
