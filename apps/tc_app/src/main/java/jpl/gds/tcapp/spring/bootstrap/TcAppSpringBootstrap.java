/*
 * Copyright 2006-2019. California Institute of Technology.
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
package jpl.gds.tcapp.spring.bootstrap;

import jpl.gds.ccsds.api.CcsdsApiBeans;
import jpl.gds.ccsds.api.cfdp.ICfdpPduFactory;
import jpl.gds.shared.checksum.CcsdsCrc16ChecksumAdaptor;
import jpl.gds.shared.checksum.IChecksumCalculator;
import jpl.gds.tc.api.config.CommandFrameProperties;
import jpl.gds.tc.api.frame.ITcTransferFrameParser;
import jpl.gds.tcapp.app.reverse.cltu.CltuWriter;
import jpl.gds.tcapp.app.reverse.cltu.ICltuWriter;
import jpl.gds.tcapp.app.reverse.frame.FrameWriter;
import jpl.gds.tcapp.app.reverse.frame.IFrameWriter;
import jpl.gds.tcapp.app.reverse.pdu.IPduParser;
import jpl.gds.tcapp.app.reverse.pdu.IPduWriter;
import jpl.gds.tcapp.app.reverse.pdu.PduParser;
import jpl.gds.tcapp.app.reverse.pdu.PduWriter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.*;

import jpl.gds.tcapp.app.gui.factory.IUplinkTabFactory;
import jpl.gds.tcapp.app.gui.factory.UplinkTabFactory;

import java.io.PrintWriter;

/**
 * The SpringBootstrap for TC application classes
 * 
 *
 *
 */
@Configuration
public class TcAppSpringBootstrap {

	public static final String UPLINK_TAB_FACTORY = "UPLINK_TAB_FACTORY";
	public static final String CLTU_WRITER = "CLTU_WRITER";
	public static final String FRAME_WRITER = "FRAME_WRITER";
	public static final String PDU_WRITER = "PDU_WRITER";
	public static final String PDU_PARSER = "PDU_PARSER";
	public static final String PRINT_WRITER = "PRINT_WRITER";
	public static final String CRC16_CHECKSUM_CALCULATOR = "CRC_16_CHECKSUM_CALC";

	/**
	 * Get the uplink tab factory
	 * @return the factory that makes uplink tabs
	 */
	@ConditionalOnMissingBean(name = UPLINK_TAB_FACTORY)
	@Bean(name = UPLINK_TAB_FACTORY)
	@Scope("singleton")
	@Lazy(value = true)
	public IUplinkTabFactory getUplinkTabFactory() {
		return new UplinkTabFactory();
	}

	/**
	 * Gets the singleton CltuWriter bean.
	 *
	 * @param parser
	 *          class to parse bytes into transfer frames
	 * @param frameWriter
	 *          class to write frame contents
	 * @param printWriter
	 *          print utility
	 * @return CltuWriter bean
	 */
	@Bean(name = CLTU_WRITER)
	@Scope("singleton")
	@Lazy
	public ICltuWriter createCltuWriter(final ITcTransferFrameParser parser, final IFrameWriter frameWriter, final PrintWriter printWriter, final CommandFrameProperties commandFrameProperties){
		return new CltuWriter(parser, frameWriter, printWriter, commandFrameProperties);
	}

	/**
	 * Gets the singleton FrameWriter bean.
	 *
	 * @param pduWriter
	 *          class to write pdu contents
	 * @param checksumCalculator
	 *          utility that calculates a checksum based on a set of bytes
	 * @param pduParser
	 *          class to parse PDUs from bytes
	 * @param printWriter
	 *          print utility
	 * @return FrameWriter bean
	 */
	@Bean(name = FRAME_WRITER)
	@Scope("singleton")
	@Lazy
	public IFrameWriter createFrameWriter(final IPduWriter pduWriter, final IChecksumCalculator checksumCalculator,
	                                      final IPduParser pduParser, final PrintWriter printWriter,
	                                      final CommandFrameProperties commandFrameProperties){
		return new FrameWriter(pduWriter, checksumCalculator, pduParser, printWriter, commandFrameProperties);
	}

	/**
	 * For now, we're using the CcsdsCrc16ChecksumAdaptor to calculate CRC-16.
	 * @return IChecksumCalculator
	 */
	@Bean(CRC16_CHECKSUM_CALCULATOR)
	@Lazy
	@Scope("singleton")
	public IChecksumCalculator getCrc16ChecksumCalculator() {
		return new CcsdsCrc16ChecksumAdaptor();
	}

	/**
	 * Gets the singleton PduWriter bean.
	 *
	 * @param printWriter
	 *          print utility
	 * @return PduWriter bean
	 */
	@Bean(name = PDU_WRITER)
	@Scope("singleton")
	@DependsOn(PRINT_WRITER)
	@Lazy
	public IPduWriter createPduWriter(final PrintWriter printWriter){
		return new PduWriter(printWriter);
	}

	/**
	 * Gets the singleton PrintWriter bean.
	 * For "reversal" apps, it's helpful for the all of the nested data writers to
	 * share the same PrintWriter, in order to ensure the correct order for data display.
	 * Injecting a PrintWriter also has the benefit of allowing the use of a differently-configured one
	 *
	 * @return PrintWriter bean
	 */
	@Bean(name = PRINT_WRITER)
	@Scope("singleton")
	@Lazy
	public PrintWriter createPrintWriter(){
		return new PrintWriter(System.out);
	}

	/**
	 * Gets the singleton PduParser bean.
	 *
	 * @param pduFactory
	 *          class that can parse a single PDU from byte data
	 * @return PduWriter bean
	 */
	@Bean(name = PDU_PARSER)
	@Scope("singleton")
	@Lazy
	@DependsOn(CcsdsApiBeans.CFDP_PDU_FACTORY)
	public IPduParser createPduParser(final ICfdpPduFactory pduFactory){
		return new PduParser(pduFactory);
	}


}
