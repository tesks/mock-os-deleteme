package jpl.gds.tc.legacy.impl.scmf;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.shared.checksum.RotatedXorAlgorithm;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.tc.api.IScmf;
import jpl.gds.tc.api.IScmfSfduHeader;
import jpl.gds.tc.api.message.IScmfCommandMessage;
import jpl.gds.tc.api.scmf.IScmfSerializer;
import org.springframework.context.ApplicationContext;

public class LegacyScmfSerializer implements IScmfSerializer {
    
    private final MissionProperties missionProps;

    public LegacyScmfSerializer(final ApplicationContext appContext) {
        this(appContext.getBean(MissionProperties.class));
    }

    public LegacyScmfSerializer(final MissionProperties missionProps) {
        this.missionProps = missionProps;
    }

    public byte[] getHeaderBytes(IScmf scmf) {

        IScmfSfduHeader header = scmf.getSfduHeader();

        int offset = 0;
        final byte[] scmfHeaderBytes = new byte[SCMF_HEADER_BYTE_LENGTH];

        // get file name bytes
        String fName = deNull(header.getFileName());
        if (fName.length() < FILE_NAME_BYTE_LENGTH) {
            fName = GDR.leftFillStr(fName, FILE_NAME_BYTE_LENGTH, ' ');
        } else if (fName.length() > FILE_NAME_BYTE_LENGTH) {
            fName = fName.substring(0, 8);
        }
        GDR.set_string(scmfHeaderBytes, offset, fName);
        offset += FILE_NAME_BYTE_LENGTH;

        // get preparer bytes
        String prep = deNull(scmf.getPreparer());
        if (prep.length() < PREPARER_BYTE_LENGTH) {
            prep = GDR.leftFillStr(prep, PREPARER_BYTE_LENGTH, ' ');
        }
        GDR.set_string(scmfHeaderBytes, offset, prep);
        offset += PREPARER_BYTE_LENGTH;

        // get scmf length
        long scmfLength = scmf.getFileByteSize();
        if (scmfLength <= 0) {
            scmfLength = SCMF_HEADER_BYTE_LENGTH;
            for(IScmfCommandMessage msg : scmf.getCommandMessages()) {
                scmfLength += msg.getMessageByteLength();
            }
        }
        GDR.set_u32(scmfHeaderBytes, offset, scmfLength);
        offset += FILE_SIZE_BYTE_LENGTH;

        // get scmf header length
        GDR.set_u32(scmfHeaderBytes, offset, SCMF_HEADER_BYTE_LENGTH);
        offset += FILE_HEADER_SIZE_BYTE_LENGTH;

        // get mission id
        String midStr = header.getMissionId();
        long mid = 0;
        try{
            mid = GDR.parse_long(midStr);
        } catch(NumberFormatException e) {}

        if (mid <= 0) {
            mid = missionProps.getMissionId();
        }
        GDR.set_u16(scmfHeaderBytes, offset, (int) mid);
        offset += MISSION_ID_BYTE_LENGTH;

        // get spacecraft id
        String scidStr = header.getSpacecraftId();
        long scid = 0;
        try {
            scid = GDR.parse_long(scidStr);
        } catch (NumberFormatException e) {}

        if (scid <= 0) {
            scid = missionProps.mapMnemonicToScid(header.getSpacecraftName());
        }
        GDR.set_u16(scmfHeaderBytes, offset, (int) scid);
        offset += SPACECRAFT_ID_BYTE_LENGTH;

        // get reference number
        final long refNum = scmf.getReferenceNumber();
        GDR.set_u32(scmfHeaderBytes, offset, refNum);
        offset += REFERENCE_NUMBER_BYTE_LENGTH;

        // get bit one radiation time
        String bitOneRadTime = deNull(scmf.getBitOneRadiationTime());
        if (bitOneRadTime.length() < BIT_ONE_RADIATION_TIME_BYTE_LENGTH) {
            bitOneRadTime = GDR.leftFillStr(bitOneRadTime,
                    BIT_ONE_RADIATION_TIME_BYTE_LENGTH, ' ');
        }
        GDR.set_string(scmfHeaderBytes, offset, bitOneRadTime);
        offset += BIT_ONE_RADIATION_TIME_BYTE_LENGTH;

        // get bit rate
        final long br = scmf.getBitRateIndex();
        GDR.set_u32(scmfHeaderBytes, offset, br);
        offset += BIT_RATE_INDEX_BYTE_LENGTH;

        // get comment field
        String commField = deNull(scmf.getCommentField());
        if (commField.length() < COMMENT_FIELD_BYTE_LENGTH) {
            commField = GDR.leftFillStr(commField, COMMENT_FIELD_BYTE_LENGTH,
                    ' ');
        }
        GDR.set_string(scmfHeaderBytes, offset, commField);
        offset += COMMENT_FIELD_BYTE_LENGTH;

        // get creation time
        String createTime = deNull(header.getProductCreationTime());
        if (createTime.length() < CREATION_TIME_BYTE_LENGTH) {
            createTime = GDR.leftFillStr(createTime, CREATION_TIME_BYTE_LENGTH,
                    ' ');
        }
        GDR.set_string(scmfHeaderBytes, offset, createTime);
        offset += CREATION_TIME_BYTE_LENGTH;

        // get title
        String ttl = deNull(scmf.getTitle());
        if (ttl.length() < TITLE_BYTE_LENGTH) {
            ttl = GDR.leftFillStr(ttl, TITLE_BYTE_LENGTH, ' ');
        }
        GDR.set_string(scmfHeaderBytes, offset, ttl);
        offset += TITLE_BYTE_LENGTH;

        // get seqtran version
        String seqtranVers = deNull(scmf.getSeqtranVersion());
        if (seqtranVers.length() < SEQTRAN_VERSION_BYTE_LENGTH) {
            seqtranVers = GDR.leftFillStr(seqtranVers,
                    SEQTRAN_VERSION_BYTE_LENGTH, ' ');
        }
        GDR.set_string(scmfHeaderBytes, offset, seqtranVers);
        offset += SEQTRAN_VERSION_BYTE_LENGTH;

        // get macro version
        String macroVers = deNull(scmf.getMacroVersion());
        if (macroVers.length() < MACRO_VERSION_BYTE_LENGTH) {
            macroVers = GDR.leftFillStr(macroVers, MACRO_VERSION_BYTE_LENGTH,
                    ' ');
        }
        GDR.set_string(scmfHeaderBytes, offset, macroVers);
        offset += MACRO_VERSION_BYTE_LENGTH;

        // insert a temporary checksum (to be calculated later)
        final short checksum = 0x0000;
        GDR.set_u16(scmfHeaderBytes, offset, checksum);

        return (scmfHeaderBytes);
    }

    public byte[] getCommandMessageBytes(final IScmfCommandMessage msg) {
        byte[] data = msg.getData();
        int length = MESSAGE_HEADER_BYTE_LENGTH + data.length;
        byte[] bytes = new byte[length];
        int offset = 0;

        //get the message number
        GDR.set_u32(bytes,offset,msg.getMessageNumber());
        offset += MESSAGE_NUMBER_BYTE_LENGTH;

        //get the message length
        GDR.set_u32(bytes,offset,data.length*8);
        offset += MESSAGE_BIT_LENGTH_BYTE_LENGTH;

        //get the transmission start time
        String transStartTime = deNull(msg.getTransmissionStartTime());
        if(transStartTime.length() < TIME_BYTE_LENGTH)
        {
            transStartTime = GDR.leftFillStr(transStartTime, TIME_BYTE_LENGTH, ' ');
        }
        GDR.set_string(bytes,offset,transStartTime);
        offset += TIME_BYTE_LENGTH;

        //get the open window
        String oWindow = deNull(msg.getOpenWindow());
        if(oWindow.length() < TIME_BYTE_LENGTH)
        {
            oWindow = GDR.leftFillStr(oWindow,TIME_BYTE_LENGTH,' ');
        }
        GDR.set_string(bytes,offset,oWindow);
        offset += TIME_BYTE_LENGTH;

        //get the close window
        String cWindow = deNull(msg.getCloseWindow());
        if(cWindow.length() < TIME_BYTE_LENGTH)
        {
            cWindow = GDR.leftFillStr(cWindow,TIME_BYTE_LENGTH,' ');
        }
        GDR.set_string(bytes,offset,cWindow);
        offset += TIME_BYTE_LENGTH;

        //get the comment field
        String comment = deNull(msg.getMessageComment());
        if(comment.length() < MESSAGE_COMMENT_BYTE_LENGTH)
        {
            comment = GDR.leftFillStr(comment,MESSAGE_COMMENT_BYTE_LENGTH,' ');
        }
        GDR.set_string(bytes,offset,comment);
        offset += MESSAGE_COMMENT_BYTE_LENGTH;

        //calculate the command message checksum
        int calculatedChecksum = RotatedXorAlgorithm.calculate16BitChecksum(data);
        GDR.set_u16(bytes,offset,calculatedChecksum);
        offset += MESSAGE_CHECKSUM_BYTE_LENGTH;

        //get the message data
        System.arraycopy(data,0,bytes,offset,data.length);
        offset += data.length;

        if(length != offset)
        {
            System.err.println("Exception occurred while writing SCMF.  SCMF file may be corrupted.");
        }

        return(bytes);
    }

    private String deNull(String provided) {
        return provided == null ? "" : provided;
    }
}
