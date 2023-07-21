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
package jpl.gds.evr.impl.message;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.dictionary.api.client.evr.IEvrUtilityDictionaryManager;
import jpl.gds.dictionary.api.evr.IEvrDefinition;
import jpl.gds.dictionary.api.evr.IEvrDictionaryFactory;
import jpl.gds.evr.api.EvrMetadata;
import jpl.gds.evr.api.EvrMetadataKeywordEnum;
import jpl.gds.evr.api.IEvr;
import jpl.gds.evr.api.IEvrFactory;
import jpl.gds.evr.api.message.EvrMessageType;
import jpl.gds.evr.api.message.IEvrMessage;
import jpl.gds.serialization.evr.Proto3EvrMessage;
import jpl.gds.serialization.evr.Proto3EvrMessage.HasVcidCase;
import jpl.gds.serialization.messages.Proto3AbstractMessage;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.BaseBinaryMessageParseHandler;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.Message;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.time.*;
import jpl.gds.shared.types.Pair;
import org.springframework.context.ApplicationContext;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
/**
 * EvrMessage is an internal Message class that represents a flight software
 * event report.
 * 
 *
 * 11/09/17 - Updated binary functionality to use protobuf
 * 1/17/18 - Remove unused XML parser class and XML generation method
 */
public class EvrMessage extends Message implements IEvrMessage {

    private static final DateFormat dateFormatter = TimeUtility.getFormatter();

    private IEvr evr;

    protected MissionProperties missionProperties;

    /**
     * Creates an instance of EvrMessage.
     * 
     */
    protected EvrMessage(final MissionProperties missionProperties) {
        super(EvrMessageType.Evr, System.currentTimeMillis());
        this.missionProperties = missionProperties;
    }

    /**
     * Creates an instance of EvrMessage.
     * 
     * @param evr
     *            the EVR object associated with the message.
     */
    public EvrMessage(final IEvr evr, final MissionProperties missionProperties) {
        this(missionProperties);
        setEvr(evr);
    }
    
    /**
     * Creates an instance of EvrMessage
     * 
     * @param msg
     *            a protobuf message containing a fully formed EvrMessage
     * @param evr
     *            the EVR object associated with the message
     * @throws IOException
     *             an error was encountered parsing the protobuf message
     */
    protected EvrMessage(final Proto3EvrMessage msg, final IEvr evr) throws IOException {
    	super(EvrMessageType.Evr, msg.getSuper());
    	this.evr = evr;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.evr.api.message.IEvrMessage#getEvr()
     */
    @Override
    public IEvr getEvr() {
        return this.evr;
    }

    /**
     * Sets the EVR object.
     * 
     * @param evr the EVR to set
     */
    protected final void setEvr(final IEvr evr) {
        this.evr = evr;
        setFromSse(evr.isFromSse());
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.shared.message.IMessage#toString()
     */
    @Override
    public String toString() {
        return "MSG:" + getType() + " " + getEventTimeString() + " evr=" + getEvr();
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.message.Message#setTemplateContext(java.util.Map)
     */
    @Override
    public void setTemplateContext(final Map<String, Object> map) {
        super.setTemplateContext(map);

        if (this.evr != null) {
            map.put("evr", "true");
            this.evr.setTemplateContext(map);
        }

        if(missionProperties != null)
        {
            map.put("missionId", missionProperties.getMissionId());
            map.put("missionName", missionProperties.getMissionLongName());
            final int scid = missionProperties.getDefaultScid();
            map.put("spacecraftName", missionProperties.mapScidToName(scid));
            map.put("spacecraftId", scid);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.shared.interfaces.EscapedCsvSupport#getEscapedCsv()
     */
    @Override
    public String getEscapedCsv() {

        final StringBuilder builder = new StringBuilder(256);
        final DateFormat df = TimeUtility.getFormatterFromPool();

        builder.append("evr");
        builder.append(CSV_SEPARATOR);

        // name,level,event:
        final IEvrDefinition evrDef = evr.getEvrDefinition();
        if (evrDef == null) {

            // builder.append(""); //name
            builder.append(CSV_SEPARATOR);
            // builder.append(""); //level
            builder.append(CSV_SEPARATOR);
            builder.append("0"); // id
            builder.append(CSV_SEPARATOR);
        } else {

            if (evrDef.getName() != null) {
                builder.append(evrDef.getName());
            }
            builder.append(CSV_SEPARATOR);

            if (evrDef.getLevel() != null) {
                builder.append(evrDef.getLevel());
            }
            builder.append(CSV_SEPARATOR);

            builder.append(evrDef.getId());
            builder.append(CSV_SEPARATOR);
        }

        // message:
        final String msg = evr.getMessage();
        if (msg != null) {
            builder.append(msg);
        }

        builder.append(CSV_SEPARATOR);

        // fromSse:
        builder.append(this.fromSse);
        builder.append(CSV_SEPARATOR);

        // eventTime:
        if (getEventTime() != null) {
            builder.append(getEventTimeString());
            builder.append(CSV_SEPARATOR);
            builder.append(getEventTime().getTime());
        }
        // This case should never happen:
        else {
            builder.append(CSV_SEPARATOR);
            builder.append(0);
        }
        builder.append(CSV_SEPARATOR);

        // realTime:
        builder.append(evr.isRealtime() ? "true" : "false");
        builder.append(CSV_SEPARATOR);

        // sclk:
        final ISclk sclk = evr.getSclk();
        if (sclk != null) {
            builder.append(sclk.toString());
            builder.append(CSV_SEPARATOR);
            builder.append(sclk.getCoarse());
            builder.append(CSV_SEPARATOR);
            builder.append(sclk.getFine());
            builder.append(CSV_SEPARATOR);
            builder.append(sclk.getBinaryGdrLong());
            builder.append(CSV_SEPARATOR);

        } else {
            builder.append(CSV_SEPARATOR);
            builder.append("0");
            builder.append(CSV_SEPARATOR);
            builder.append("0");
            builder.append(CSV_SEPARATOR);
            builder.append("0");
            builder.append(CSV_SEPARATOR);
        }

        // scet:
        final IAccurateDateTime scet = evr.getScet();
        if (scet != null) {
        	builder.append(scet.getFormattedScet(true));
            builder.append(CSV_SEPARATOR);
			builder.append(scet.getTime()); 
            builder.append(CSV_SEPARATOR);
			builder.append(scet.getNanoseconds()); 
			builder.append(CSV_SEPARATOR);

        } else {
            builder.append(CSV_SEPARATOR);
            builder.append("0");
            builder.append(CSV_SEPARATOR);
        }

        // sol:
        final ILocalSolarTime sol = evr.getSol();
        if (sol != null) {
            builder.append(sol.getFormattedSol(true));
            builder.append(CSV_SEPARATOR);
            builder.append(sol.getSolExact());
            builder.append(CSV_SEPARATOR);

        } else {
            builder.append(CSV_SEPARATOR);
            builder.append("0");
            builder.append(CSV_SEPARATOR);
        }

        // ert:
        final IAccurateDateTime ert = evr.getErt();
        if (ert != null) {
            builder.append(ert.getFormattedErt(true));
            builder.append(CSV_SEPARATOR);
            builder.append(ert.getTime());
            builder.append(CSV_SEPARATOR);
            builder.append(ert.getNanoseconds());
            builder.append(CSV_SEPARATOR);

        } else {
            builder.append(CSV_SEPARATOR);
            builder.append("0");
            builder.append(CSV_SEPARATOR);
            builder.append("0");
            builder.append(CSV_SEPARATOR);
        }

        // dssId:
        builder.append(evr.getDssId());
        builder.append(CSV_SEPARATOR);

        // vcid:
        if (evr.getVcid() != null) {
            builder.append(evr.getVcid());
        }
        builder.append(CSV_SEPARATOR);

        // module:
        if (evrDef != null) {
            final String module = evrDef.getCategory(IEvrDefinition.MODULE);
            if (module != null) {
                builder.append(module);
            }
        }

        // metadata:
        final EvrMetadata metadata = evr.getMetadata();
        if (!metadata.isEmpty()) {
            builder.append(CSV_SEPARATOR);
            final List<Pair<EvrMetadataKeywordEnum, String>> keyvalues =
                    metadata.asStrings();
            final int size = keyvalues.size();
            int i = 1;
            for (final Pair<EvrMetadataKeywordEnum, String> pair : keyvalues) {

                builder.append(pair.getOne());
                builder.append(CSV_SEPARATOR);
                builder.append(pair.getTwo());
                if (i < size) {
                    builder.append(CSV_SEPARATOR);
                }
                i++;
            }
        }

        TimeUtility.releaseFormatterToPool(df);
        return builder.toString();
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.shared.xml.stax.StaxSerializable#generateStaxXml(javax.xml.stream.XMLStreamWriter)
     */
    @Override
    public void generateStaxXml(final XMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeEmptyElement(MessageRegistry.getDefaultInternalXmlRoot(EvrMessageType.Evr));
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.shared.message.IMessage#getOneLineSummary()
     */
    @Override
    public String getOneLineSummary() {
        if (this.evr == null) {
            return "No EVR";
        }
        synchronized (dateFormatter) {
            return this.evr.getName()
                    + " ("
                    + this.evr.getEventId()
                    + "): "
                    + (this.evr.getMessage() == null ? "No Message" : this.evr
                            .getMessage())
                            + " "
                            + (this.evr.getSclk() == null ? "No SCLK" : "SCLK = "
                                    + this.evr.getSclk().toString())
                                    + " "
                                    + (this.evr.getScet() == null ? "No SCET" : "SCET = "
                                            + dateFormatter.format(this.evr.getScet()))
                                            + " "
                                            + (this.evr.getSol() == null ? "No LST" : "LST = "
                                                    + this.evr.getSol().getFormattedSol(true))
                                                    + " "
                                                    + (this.evr.getErt() == null ? "No ERT" : "ERT = "
                                                            + this.evr.getErt().getFormattedErt(true));
        }
    }


    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.shared.template.FullyTemplatable#getMtakFieldCount()
     */
    @Override
    public int getMtakFieldCount() {
        return 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.shared.template.FullyTemplatable#getXmlRootName()
     */
    @Override
    public String getXmlRootName() {
        return null;
    }

    /**
     * Convert this object to a binary array
     * Added to support serialization into global LAD
     * 
     * @return a byte[] containing the binary contents necessary to recreate
     *         this object using jpl.gds.evr.EvrMessage#parseFromBinary(),
     *         or null if an error was encountered while converting this object
     *         to a byte[]
     * 
     * @see jpl.gds.shared.message.IMessage#toBinary()
     * 
     */
    @Override
    public byte[] toBinary() {
        
        return this.build().toByteArray();
    }
    
    @Override
    public Proto3EvrMessage build() {
        
        if (this.evr == null) {
            throw new IllegalStateException("Cannot convert EvrMessage with null EVR to binary");
        }
        // TimeProperties.getInstance().usesLst() is checked when EvrMessage is created. I think it's redundant here
        final boolean usesSol = this.getEvr().getSol() != null ? true : false;
        
        final Proto3EvrMessage.Builder retVal = Proto3EvrMessage.newBuilder();
        
        retVal.setSuper((Proto3AbstractMessage)super.build());
        
        retVal.setEvrMessage(this.evr.getMessage());
        retVal.setEvrMetadata(this.evr.getMetadata().build());
        // Added null check
        if(evr.getEvrDefinition() != null) {
            retVal.setEvrDefinition(this.evr.getEvrDefinition().build());
        }

        retVal.setRealtime(this.evr.isRealtime());

        retVal.setUsesSol(usesSol);
      
        if(evr.getErt() != null){
        	retVal.setErt(evr.getErt().buildAccurateDateTime());
        }

        if (this.evr.getSclk() != null) {
            retVal.setSclk(this.evr.getSclk().buildSclk());
        }
        
        /*  Use standard method to write SCET */
        if (this.evr.getScet() != null) {
            retVal.setScet(this.evr.getScet().buildAccurateDateTime());
        }

        /*  Do not waste the 10 bytes of bandwidth if SOL
         * times not enabled.
         */
        if (usesSol && this.evr.getSol() != null) {
            /*  Use standard method to write LST */
            retVal.setSol(this.evr.getSol().buildLocalSolarTime());
        }

        // RCT (64-bit int) - the RCT, as number of milliseconds since Unix
        // Epoch
        if (this.evr.getRct() != null) {
            retVal.setRct(this.evr.getRct().buildAccurateDateTime());
        }

        retVal.setDssId(this.evr.getDssId());

        if (this.evr.getVcid() != null) {
            retVal.setVcid(this.evr.getVcid());
        }

        retVal.setIsBad(this.evr.isBadEvr());
        
        return retVal.build();
    }

    /**
     * BinaryParseHandler is the message-specific SAX parse handler for creating
     * this Message from its binary representation.
     * 
     */
    public static class BinaryParseHandler extends BaseBinaryMessageParseHandler {
        
        private final IEvrFactory evrFactory;
        private final int scid;
        private final IEvrUtilityDictionaryManager evrDict;
        private final IEvrDictionaryFactory evrDictFactory;
        
        private final Tracer                       trace;

        /**
         * Constructor.
         * 
         * @param context the current application context
         */
        public BinaryParseHandler(final ApplicationContext context) {
            this.evrFactory = context.getBean(IEvrFactory.class);
            this.scid = context.getBean(IContextIdentification.class).getSpacecraftId();
            this.evrDict = context.getBean(IEvrUtilityDictionaryManager.class);
            this.evrDictFactory = context.getBean(IEvrDictionaryFactory.class);
            trace = TraceManager.getTracer(context, Loggers.UTIL);
        }

        /*
         * Parser to retrieve EvrMessage from
         * byte[] in global LAD
         */
        @Override
        public IMessage[] parse(final List<byte[]> content) {

            for (final byte[] msgBytes : content) {
            	try {
            		
                    final Proto3EvrMessage evrMsg = Proto3EvrMessage.parseFrom(msgBytes);
            		
            		final boolean fromSse = evrMsg.getSuper().getFromSse();
            		final long eventId = evrMsg.getEvrDefinition().getEventId();
            		
                    IEvrDefinition evrDef = fromSse ?
                           evrDict.getSseDefinition(eventId) : evrDict.getFswDefinition(eventId);
                    
                    if(evrDef == null){
                    	evrDef = evrDictFactory.getMultimissionEvrDefinition();
                    	
                    	evrDef.load(evrMsg.getEvrDefinition());
                    }
            		
            		final IAccurateDateTime scet = new AccurateDateTime(evrMsg.getScet());
            		final IAccurateDateTime ert = new AccurateDateTime(evrMsg.getErt());
            		final IAccurateDateTime rct = new AccurateDateTime(evrMsg.getRct());
            		final ISclk sclk = new Sclk(evrMsg.getSclk());
            		
            		ILocalSolarTime sol;
            		if (evrMsg.getUsesSol()) { 
            		    try {
            		        sol = LocalSolarTimeFactory.getNewLst(evrMsg.getSol(), scid);
            		    } catch (final ParseException e) {
            		        sol = LocalSolarTimeFactory.getNewLst(scid);
            		        sol.loadLocalSolarTime(evrMsg.getSol());
            		    }
            		} else { 
            		    sol = null;
            		}
					
            		
            		final String evrMessage = evrMsg.getEvrMessage();
            		final EvrMetadata metadata = new EvrMetadata(evrMsg.getEvrMetadata());
            		
            		final int dssId = evrMsg.getDssId();
            		
            		Integer vcid = null;
            		
            		if(evrMsg.getHasVcidCase().equals(HasVcidCase.VCID)){
            			vcid = Integer.valueOf(evrMsg.getVcid());
            		}
            		
            		
            		final IEvr evr = evrFactory.createEvr(evrDef, scet, ert, rct, sclk, sol, evrMessage,
                            metadata, fromSse, (byte) dssId, vcid);
            		evr.setRealtime(evrMsg.getRealtime());

            		evr.setBadEvr(evrMsg.getIsBad());

            		final EvrMessage message = new EvrMessage(evrMsg, evr);

            		addMessage(message); 
            	}
            	catch (final IOException e){
                    trace.warn("Error parsing EvrMessage from binary buffer: " + e.getMessage());
            	}
            }
            
            return getMessages();
        }
    }

    @Override
    public boolean isRealtime() {
        return this.evr == null ? false : this.evr.isRealtime();
    }

}
