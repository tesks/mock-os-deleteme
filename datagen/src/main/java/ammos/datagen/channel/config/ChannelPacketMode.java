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
package ammos.datagen.channel.config;

/**
 * Defines the modes in which the channel packet generator can operate.
 * 
 *
 */
public enum ChannelPacketMode {
	/**
	 * Channel packet generation mode in which the generator rotates among
	 * defined channel packet APIDs and randomly populates them with channels
	 * selected from the dictionary, stopping when a desired file size is
	 * reached.
	 */
	RANDOM,
	/**
	 * Channel packet generation mode in which the generator rotates among
	 * defined channel packet APIDs and populates each APID with a pre-defined
	 * list of channels,stopping when a desired file size is reached.
	 */
	BY_APID,
	/**
	 * Channel packet generation mode in which the generate produces packets in
	 * a configured order, with each packet having an APID and list of channel
	 * values (DNs) defined in the configuration, stopping when the configured
	 * list of expected packets is exhausted.
	 */
	CUSTOM;

}
