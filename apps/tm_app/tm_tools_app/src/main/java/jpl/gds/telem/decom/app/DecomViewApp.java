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
package jpl.gds.telem.decom.app;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.apache.commons.cli.ParseException;
import org.springframework.context.ApplicationContext;

import jpl.gds.ccsds.api.packet.IPacketFormatDefinition;
import jpl.gds.ccsds.api.packet.ISpacePacketHeader;
import jpl.gds.ccsds.api.packet.PacketHeaderFactory;
import jpl.gds.ccsds.api.tm.packet.ISecondaryPacketHeader;
import jpl.gds.ccsds.api.tm.packet.ISecondaryPacketHeaderExtractor;
import jpl.gds.ccsds.api.tm.packet.ISecondaryPacketHeaderLookup;
import jpl.gds.common.config.dictionary.options.DictionaryCommandOptions;
import jpl.gds.common.config.types.TelemetryInputType;
import jpl.gds.decom.DecomEngine;
import jpl.gds.decom.exception.DecomException;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.apid.ApidContentType;
import jpl.gds.dictionary.api.apid.IApidDefinition;
import jpl.gds.dictionary.api.apid.IApidDefinitionProvider;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.decom.IChannelDecomDefinitionProvider;
import jpl.gds.dictionary.api.decom.IDecomMapDefinition;
import jpl.gds.eha.api.service.channel.IDecomListenerFactory;
import jpl.gds.shared.cli.app.AbstractCommandLineApp;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.DynamicEnumOptionParser;
import jpl.gds.shared.cli.options.FlagOption;
import jpl.gds.shared.cli.options.ICommandLineOption;
import jpl.gds.shared.cli.options.filesystem.FileOption;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.SpringContextFactory;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.time.ISclk;

/**
 * This app performs generic decom of a packet input file and outputs a textual
 * representation of the data.
 * 
 *
 */
public class DecomViewApp extends AbstractCommandLineApp {
	private static final String FILE_SHORT_OPT = "f";
    private static final String FILE_LONG_OPT  = "inputFile";
    
    private static final String SSE_LONG_OPT   = "SSE";
    private final Tracer             log;

	private String filename;
	private DictionaryCommandOptions dictOpts;
	@SuppressWarnings("unused")
    private TelemetryInputType inputFormat;
	private final ApplicationContext appContext;
	
	/**
	 * Constructor.
	 */
	public DecomViewApp() {
		appContext = SpringContextFactory.getSpringContext(true);
        log = TraceManager.getTracer(appContext, Loggers.TLM_EHA);
	}

	@Override
	public BaseCommandOptions createOptions() {
	    
	    if (optionsCreated.get()) {
            return options;
        }
	    
		super.createOptions(appContext.getBean(BaseCommandOptions.class, this));
		
		dictOpts = new DictionaryCommandOptions(appContext.getBean(DictionaryProperties.class));
		
		options.addOption(
				new FlagOption(null, SSE_LONG_OPT, "Run the application as SSE", false)
				);
		options.addOption(
				new FileOption(FILE_SHORT_OPT, FILE_LONG_OPT, "filename", "RAW_PKT Data file to be decommutated.", true, true)
				);
		
		options.addOptions(
				dictOpts.getAllOptions()
				);
		
		/** Add this back in when multiple input types are supported. 
        EnumOption<TelemetryInputType> DOWNLINK_INPUT_TYPE = new EnumOption<TelemetryInputType>(
                TelemetryInputType.class,
                "f",
                "inputFormat",
                "format",
                "source format of telemetry input; defaults based upon venue type",
                false);

        DOWNLINK_INPUT_TYPE.setParser(new InputFormatOptionParser());
        options.addOption(DOWNLINK_INPUT_TYPE);
        */
  		return options;
	}

	@Override
	public void configure(final ICommandLine commandLine) throws ParseException {
		super.configure(commandLine);
		
		if (commandLine.hasOption(FILE_LONG_OPT)) {
			this.filename = commandLine.getOptionValue(FILE_LONG_OPT);
			if (! (filename.endsWith(TelemetryInputType.RAW_PKT.name()) || filename.endsWith(TelemetryInputType.RAW_PKT.name().toLowerCase())) ) {
				throw new ParseException("Input file must be of type " + TelemetryInputType.RAW_PKT.name());
			}
		} else {
			throw new ParseException("You must supply an input file");
		}
		if (commandLine.hasOption(SSE_LONG_OPT)) { 
            appContext.getBean(SseContextFlag.class).setApplicationIsSse(true);
		}
		// Parse FSW and SSE DictionaryConfiguration options (directories)
		dictOpts.parseAllOptionsAsOptional(commandLine); 
		// Get the DictionaryConfiguration, then check it
		final DictionaryProperties dictConfig = dictOpts.getDictionaryConfiguration();
		
		if (commandLine.hasOption(dictOpts.FSW_DICTIONARY_DIRECTORY.getOpt())) { 
			final Path dir = Paths.get(dictConfig.getFswDictionaryDir());
			
			if (!(Files.exists(dir) || Files.isReadable(dir)))  { 
				throw new ParseException("Invalid FSW directory '" + dictConfig.getFswDictionaryDir() 
							+ "' specified by --" + dictOpts.FSW_DICTIONARY_DIRECTORY.getLongOpt() );
			}
		}
		
		if (commandLine.hasOption(dictOpts.FSW_VERSION.getOpt())) { 
			if (!dictConfig.getAvailableFswVersions().contains(dictConfig.getFswVersion())) { 
				throw new ParseException("Invalid FSW version '" + dictConfig.getFswVersion() 
							+ "' specified by --" + dictOpts.FSW_VERSION.getLongOpt() + ". does not exist");
			}
		}
		
		if (commandLine.hasOption(dictOpts.SSE_DICTIONARY_DIRECTORY.getOpt())) { 
			final Path dir = Paths.get(dictConfig.getSseDictionaryDir());
			
			if (!(Files.exists(dir) || Files.isReadable(dir)))  { 
				throw new ParseException("Invalid SSE FSW directory '" + dictConfig.getSseDictionaryDir() 
						+ "' specified by --" + dictOpts.SSE_DICTIONARY_DIRECTORY.getLongOpt() );
			}
		}
		if (commandLine.hasOption(dictOpts.SSE_VERSION.getOpt())) { 
			if (!dictConfig.getAvailableSseVersions().contains(dictConfig.getSseVersion())) { 
				throw new ParseException("Invalid SSE FSW version '" + dictConfig.getSseVersion() 
				+ "' specified by --" + dictOpts.SSE_VERSION.getLongOpt() + ". does not exist");
			}
		}

	}

	/**
	 * Prints information about a packet.
	 * 
	 * @param header the packet header object
	 * @param pktSclk the packet time as SCLK
	 */
	public void printPacketInfo(final ISpacePacketHeader header, final ISclk pktSclk) {
		System.out.println("Decommutating packet:"+ 
				" APID=" + header.getApid() + 
				",SPSC=" + header.getSourceSequenceCount() + 
				",SCLK=" + pktSclk + 
				",Data Length=" + header.getPacketDataLength() +
				",Version=" + header.getVersionNumber() + 
				",Type=" + header.getPacketType() + 
				",Grouping Flags=" + header.getGroupingFlags());
	}

	/**
	 * Determine whether an apid is defined as a decommable by map.
	 * @param apidDefs dictionary to lookup apid definition from
	 * @param apid the packet apid being checked
	 * @return true if apid indicates a packet is decommable by a map
	 */
	public boolean isDecomPacket(final IApidDefinitionProvider apidDefs, final short apid) {
		final IApidDefinition def = apidDefs.getApidDefinition(Short.toUnsignedInt(apid));
		if (def == null) {
			return false;
		}
		
		return def.getContentType() == ApidContentType.DECOM_FROM_MAP;
	}

	/**
	 * Performs decom of the input file contents.
	 * 
	 * @param fis the file input stream
	 * @throws DictionaryException of there is a problem loading dictionaries
	 * @throws DecomException if there is a problem with the decom
	 * @throws IOException if there is a problem reading input or writing output
	 */
    public void decomPacketFile(final FileInputStream fis) throws DictionaryException, DecomException, IOException {
		// We need a packet header object for the current mission
	    final ISpacePacketHeader header = PacketHeaderFactory.create(IPacketFormatDefinition.TypeName.CCSDS);
		final byte[] data = new byte[ISpacePacketHeader.MAX_PACKET];    	
		final int headerLen = header.getPrimaryHeaderLength();
		int len = fis.read(data, 0, headerLen);
		final IApidDefinitionProvider apidDefs = appContext.getBean(IApidDefinitionProvider.class);
		final ISecondaryPacketHeaderLookup secHeaderLookup = appContext.getBean(ISecondaryPacketHeaderLookup.class);

		final IChannelDecomDefinitionProvider decomDict = appContext.getBean(IChannelDecomDefinitionProvider.class);
		final DecomEngine engine = new DecomEngine(appContext);
		final PrintWriter writer = new PrintWriter(System.out);
		engine.addListener(appContext.getBean(IDecomListenerFactory.class).createDecomPrinterListener(
		        writer, appContext));
		while (len == headerLen) {
			header.setHeaderValuesFromBytes(data, 0);

			final int expectedBytes = header.getPacketDataLength() + 1;
			len = fis.read(data, headerLen, expectedBytes);
			if (len != expectedBytes) {
				System.out.println("Packet body not complete. Expected " + expectedBytes + "bytes, found " + len);
				break;
			}

			/** Check that this is a generic decom packet. Skip processing if it is not */
			if (isDecomPacket(apidDefs, header.getApid())) {

			final ISecondaryPacketHeaderExtractor extractor = secHeaderLookup.lookupExtractor(header);
			final ISecondaryPacketHeader secondaryHeader = extractor.extract(data, header.getPrimaryHeaderLength());

			final ISclk pktSclk = secondaryHeader.getSclk();
				printPacketInfo(header, pktSclk);

			final IDecomMapDefinition map = decomDict.getDecomMapByApid(header.getApid());
				if (map == null) {
					System.err.println(String.format("Encountered decom packet with APID=%s, but no corresponding decom map found",
							Integer.toUnsignedString(Short.toUnsignedInt(header.getApid()))
					));
				} else {
					engine.decom(map, data, 0, (header.getPacketDataLength() + header.getPrimaryHeaderLength() + 1) * Byte.SIZE);
					writer.flush();
				}
			}
				
				
			len = fis.read(data, 0, headerLen);
		}
		
	}

	/**
	 * Starts the decom processing.
	 * 
     * @throws DictionaryException of there is a problem loading dictionaries
     * @throws DecomException if there is a problem with the decom
     * @throws IOException if there is a problem reading input or writing output
	 */
	public void start() throws IOException, DictionaryException, DecomException {
			
			// Open the output file    	
			final FileInputStream fis = new FileInputStream(this.filename);
			decomPacketFile(fis);
			fis.close();

	}
	
	/**
	 * Main application entry point.
	 * @param args command line arguments
	 */
	public static void main(final String[] args) {
		final DecomViewApp app = new DecomViewApp();
		try {
			final ICommandLine commandLine = app.createOptions().parseCommandLine(args, true);
			app.configure(commandLine);
		} catch (final ParseException e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}
		
		try {
			app.start();
		} catch (IOException | DictionaryException | DecomException e) {
			System.err.println("Decommutation failed: " + e.getMessage());
			System.exit(1);
		}
		
	}

	/**
     * An option parser for the input format command line option.
     */
    protected class InputFormatOptionParser extends DynamicEnumOptionParser<TelemetryInputType> {

        /**
         * Constructor.
         */
        public InputFormatOptionParser() {
            super(TelemetryInputType.class, Arrays.asList(TelemetryInputType.RAW_PKT));
        }

        /**
         * @{inheritDoc
         */
        @Override
        public TelemetryInputType parse(final ICommandLine commandLine,
            final ICommandLineOption<TelemetryInputType> opt) throws ParseException {

            TelemetryInputType type = super.parse(commandLine, opt);
            if (type != null) {
                DecomViewApp.this.inputFormat = type;
            } else {
            	type = TelemetryInputType.RAW_PKT;
            }

            return type;
        }
    }

}
