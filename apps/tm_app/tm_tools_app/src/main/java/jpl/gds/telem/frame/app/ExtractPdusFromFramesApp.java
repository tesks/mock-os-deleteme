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
package jpl.gds.telem.frame.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.cli.ParseException;
import org.springframework.context.ApplicationContext;

import jpl.gds.ccsds.api.cfdp.ICfdpEofPdu;
import jpl.gds.ccsds.api.cfdp.ICfdpFileDataPdu;
import jpl.gds.ccsds.api.cfdp.ICfdpMetadataPdu;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.options.VcidOption;
import jpl.gds.context.api.IContextFilterInformation;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.client.FlightDictionaryLoadingStrategy;
import jpl.gds.dictionary.api.client.frame.ITransferFrameUtilityDictionaryManager;
import jpl.gds.dictionary.api.config.IFrameFormatDefinition.TypeName;
import jpl.gds.dictionary.api.frame.ITransferFrameDefinition;
import jpl.gds.message.api.status.IStatusMessageFactory;
import jpl.gds.shared.cli.app.AbstractCommandLineApp;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.FlagOption;
import jpl.gds.shared.cli.options.filesystem.DirectoryOption;
import jpl.gds.shared.cli.options.filesystem.FileOption;
import jpl.gds.shared.interfaces.IService;
import jpl.gds.shared.log.IPublishableLogMessage;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.CommonMessageType;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.message.MessageSubscriber;
import jpl.gds.shared.spring.context.SpringContextFactory;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.station.api.IStationInfoFactory;
import jpl.gds.station.api.IStationTelemInfo;
import jpl.gds.tm.service.api.TmServiceMessageType;
import jpl.gds.tm.service.api.cfdp.ICfdpPduMessage;
import jpl.gds.tm.service.api.cfdp.IPduExtractService;
import jpl.gds.tm.service.api.frame.IFrameMessageFactory;
import jpl.gds.tm.service.api.frame.IFrameSyncService;
import jpl.gds.tm.service.api.frame.IPresyncFrameMessage;
import jpl.gds.tm.service.api.packet.IPacketExtractService;

/**
 * An application class for extracting CFDP PDUs from telemetry frames.
 * 
 * @since R8
 */
public class ExtractPdusFromFramesApp extends AbstractCommandLineApp implements MessageSubscriber {
    /** frameFlile option */
    public static final String FRAME_LONG = "frameFile";
    /** outputDir option */
    public static final String OUTPUT_DIR_LONG = "outputDir";
    /** outputFile option */
    public static final String OUTPUT_FILE_LONG = "outputFile";
    /** dropData option */
    public static final String DROP_DATA_LONG = "dropData";
    /** dropMetadata option */
    public static final String DROP_METADATA_LONG = "dropMetadata";
    /** dropEof option */
    public static final String DROP_EOF_LONG = "dropEof";
    
    private final FileOption frameFileOpt = new FileOption(null, FRAME_LONG, "file path", "input frame file", true, true);
    private VcidOption vcidOpt;
    private final FileOption outputFileOpt = new FileOption(null, OUTPUT_FILE_LONG, "file path", "binary PDU output file", false, false);
    /*
	 * Josh Choi 5/31/2016: Adding an option to save PDU files to a directory
	 * instead. User will only be able to select either the outputFile option or
	 * the outputDir option. When outputDir option is selected, each PDU will be
	 * saved as an individual file in the output directory.
	 */
    private final DirectoryOption outputDirOpt = new DirectoryOption(null, OUTPUT_DIR_LONG, "directory path", "directory to save PDU output files", false, false);
    private final FlagOption dropDataOption = new FlagOption(null, DROP_DATA_LONG, "randomly drop file data PDUs", false);
    private final FlagOption dropMetaOption = new FlagOption(null, DROP_METADATA_LONG, "drop all metadata PDUs", false);
    private final FlagOption dropEofOption = new FlagOption(null, DROP_EOF_LONG, "drop all EOF PDUs", false);
    
    private String inputFile;
    private int vcid;
    private FileOutputStream outputFile;
    private File outputDirFile;

    /*
     * Josh Choi 5/31/2016: fileCounter will keep track of the number of PDU files created.
     */
    private int fileCounter;
    private boolean dropData;
    private boolean dropMetadata;
    private boolean dropEof;
    private final Random random = new Random();
    private boolean first = true;
	private final ApplicationContext commonContext;
	private final Tracer tracer;

    private String outputFileName;
    private String outputDirName;

    /**
     * Constructor.
     */
    public ExtractPdusFromFramesApp() {
    	fileCounter = 0;
    	
    	commonContext = SpringContextFactory.getSpringContext(true);
        this.tracer = TraceManager.getDefaultTracer(commonContext);
    }
    
    
    /**
     * Creates command line options enclosed in a BaseCommandOptions object. This default
     * implementation adds HELP and VERSION options and defines the BaseCommandOptions
     * to support aliasing.
     * 
     * @{inheritDoc}
     * @see jpl.gds.shared.cli.app.ICommandLineApp#createOptions()
     */
    @Override
    public BaseCommandOptions createOptions() {
        
        if (optionsCreated.get()) {
            return options;
        }
        
        super.createOptions(commonContext.getBean(BaseCommandOptions.class, this));
        
        options.addOption(frameFileOpt);
        vcidOpt = new VcidOption(commonContext.getBean(MissionProperties.class), true);
        options.addOption(vcidOpt);
        options.addOption(outputFileOpt);
        options.addOption(outputDirOpt);
        options.addOption(dropDataOption);
        options.addOption(dropMetaOption);
        options.addOption(dropEofOption);
        
        return options;

    }
    
    @Override
    public void configure(final ICommandLine commandLine) throws ParseException {
        inputFile = frameFileOpt.parse(commandLine);
        vcid = vcidOpt.parse(commandLine).intValue();
        
        outputFileName = outputFileOpt.parse(commandLine);
        outputDirName = outputDirOpt.parse(commandLine);
        
		if (outputFileName != null && outputDirName != null) {
			throw new ParseException("Cannot supply both " + outputFileOpt.getLongOpt() + " and "
					+ outputDirOpt.getLongOpt() + " options");
		}
       
        if (outputFileName != null) {
            try {
               outputFile = new FileOutputStream(outputFileName); 
            } catch (final IOException e) {
                throw new ParseException("IO Error opening output file " + outputFileName + ": " + e.toString());
            }
        } else if (outputDirName != null) {
        	outputDirFile = new File(outputDirName);
        	
        	if (!outputDirFile.exists()) {
        		
        		if (!outputDirFile.mkdir()) {
        			throw new ParseException("Failed to create directory " + outputDirName);
        		}	
        	}
        }
        
        
        this.dropData = dropDataOption.parse(commandLine);
        this.dropMetadata = dropMetaOption.parse(commandLine);
        this.dropEof = dropEofOption.parse(commandLine);

        try {
            commonContext.getBean(FlightDictionaryLoadingStrategy.class).enableFrame().enableApid()
                         .loadAllEnabled(commonContext, false);
        }
        catch (final DictionaryException e) {
            throw new ParseException("Could not load Frame dictionary: " + e.getMessage());
        }

    }
    
    /**
     * Executes the main body of logic: extracts PDUs from input frame file and writes them out.
     */
    public void run() {
        
        tracer.info("Processing transfer frame file " + inputFile + " for VC " + vcid);
        
     
        final IMessagePublicationBus bus = commonContext.getBean(IMessagePublicationBus.class);
        bus.subscribe(TmServiceMessageType.CfdpPdu, this);
        bus.subscribe(TmServiceMessageType.BadTelemetryFrame, this);
        bus.subscribe(TmServiceMessageType.FrameSequenceAnomaly, this);
        bus.subscribe(CommonMessageType.EndOfData, this);
        
        final List<IService> services = new ArrayList<>();
        final IStationInfoFactory infoFactory = commonContext.getBean(IStationInfoFactory.class);
        
        commonContext.getBean(IContextFilterInformation.class).setVcid(vcid);
        
        try {
        
            // commonContext.getBean(ITransferFrameDefinitionProvider.class);
            services.add(commonContext.getBean(IFrameSyncService.class));
            TypeName type = null;
            for(final ITransferFrameDefinition frameDef : commonContext.getBean(ITransferFrameUtilityDictionaryManager.class).getFrameDefinitions()){
                final TypeName tn = frameDef.getFormat().getType();
                //missions don't mix V1 and V2 frames, but check to make sure they don't use V2_BPDU and any future frame types that aren't supported yet
                if(tn.equals(TypeName.CCSDS_TM_1) || tn.equals(TypeName.CCSDS_AOS_2_MPDU)){
                    type = tn;
                    break;
                }
            }
            if (type.equals(TypeName.CCSDS_AOS_2_MPDU)) {
                services.add(commonContext.getBean(IPacketExtractService.class, vcid));
            }
            services.add(commonContext.getBean(IPduExtractService.class, type.toString(), vcid));
            
            for(final IService service : services){
                service.startService();
            }
            
            
        } catch (final Exception e) {
            e.printStackTrace();
            return;
        }
        
        final IFrameMessageFactory msgFactory = commonContext.getBean(IFrameMessageFactory.class);
        
        final byte[] buffer = new byte[1024];
        
        try (FileInputStream fis = new FileInputStream(inputFile)) {
            int len = fis.read(buffer);
        
            while (len != -1) {
                final IStationTelemInfo dsn = infoFactory.create(0.0, len * 8, new AccurateDateTime(), 0);
                final IPresyncFrameMessage pfm = msgFactory.createPresyncFrameMessage(dsn, buffer, 0, len* 8, dsn.getErt());
                bus.publish(pfm);
                tracer.debug(pfm);
                len = fis.read(buffer);
            }
            final IPublishableLogMessage msg = commonContext.getBean(IStatusMessageFactory.class)
                    .createEndOfDataMessage();
            bus.publish(msg);
            this.tracer.log(msg);
            
        } catch (final IOException e) {
            e.printStackTrace();
            return;
            
        } finally {
            for (final IService service : services) {
                if (service != null) {
                    service.stopService();
                }
            }
            if (outputFile != null) {
                try { 
                    outputFile.close(); 
                } catch (final IOException e2) {}
            
            }
            bus.unsubscribeAll();
        }
        
        tracer.info("Done processing transfer frame file " + inputFile + " for VC " + vcid);
        
    }
    
    @Override
    public void handleMessage(final IMessage m) {
        if (m instanceof ICfdpPduMessage) {
            final ICfdpPduMessage pduM = (ICfdpPduMessage)m;
            if (dropMetadata && pduM.getPdu() instanceof ICfdpMetadataPdu) {
                System.out.println("DROPPED: " + m.getOneLineSummary());
                return;
            }
            if (dropEof && pduM.getPdu() instanceof ICfdpEofPdu) {
                System.out.println("DROPPED: " + m.getOneLineSummary());
                return;
            }
            if (dropData && pduM.getPdu() instanceof ICfdpFileDataPdu) {
                // We have to make sure we drop at least one and we don't know
                // how many there are, so we always drop the first one
                if (first) {
                    first = false;
                    System.out.println("DROPPED: " + m.getOneLineSummary());
                    return;
                } else {
                    // Otherwise, drop them about 25% of the time
                    if (random.nextInt(100) <= 25) {
                        System.out.println("DROPPED: " + m.getOneLineSummary());
                        return;
                    }
                }
            }
            if (outputFile != null) {
                try {
                    
                    outputFile.write(pduM.getPdu().getData());
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            } else if (outputDirFile != null) {
            	FileOutputStream out = null;
            	
            	try {
    				out = new FileOutputStream(outputDirFile.getAbsolutePath() + File.separator + String.format("%03d", fileCounter++) + ".pdu");
    				out.write(pduM.getPdu().getData());
    			} catch (final FileNotFoundException fnfe) {
    				fnfe.printStackTrace();
    			} catch (final IOException ie) {
    				ie.printStackTrace();
    			} finally {
    				
    				try {
						out.close();
					} catch (final IOException e) {
						e.printStackTrace();
					}
    				
    			}

            }
            
        }
        System.out.println(m.getOneLineSummary());
    }
    
    /**
     * The main application entry point.
     * 
     * @param args command line arguments
     */
    public static void main(final String[] args) {
        final ExtractPdusFromFramesApp theApp = new ExtractPdusFromFramesApp();
        try {
            /*
             * Use createOptions() rather than
             * creating a new reserved/base options object.
             */
            final ICommandLine commandLine = theApp.createOptions()
                    .parseCommandLine(args, true);
            theApp.configure(commandLine);

            theApp.run();


        } catch (final ParseException e) {
            if (e.getMessage() == null) {
                TraceManager.getDefaultTracer().error(e.toString());
            } else {
                TraceManager.getDefaultTracer().error(e.getMessage());
            }
            System.exit(1);
        } catch (final Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.exit(0);
    }

    // package private getters for tests

    String getOutputFileName() {
        return outputFileName;
    }

    String getOutputDirName() {
        return outputDirName;
    }

    boolean isDropData() {
        return dropData;
    }

    boolean isDropMetadata() {
        return dropMetadata;
    }

    boolean isDropEof() {
        return dropEof;
    }

    ApplicationContext getApplicationContext() {
        return commonContext;
    }
}
