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
package jpl.gds.dictionary.impl.frame;

import java.util.Map.Entry;
import java.util.Optional;

import com.google.protobuf.ByteString;

import jpl.gds.dictionary.api.KeyValueAttributes;
import jpl.gds.dictionary.api.config.IFrameFormatDefinition;
import jpl.gds.dictionary.api.config.IFrameFormatDefinition.TypeName;
import jpl.gds.dictionary.api.frame.EncodingType;
import jpl.gds.dictionary.api.frame.IFrameTimeFieldDefinition;
import jpl.gds.dictionary.api.frame.ITransferFrameDefinition;
import jpl.gds.dictionary.api.frame.IFrameTimeFieldDefinition.TimecodeType;
import jpl.gds.serialization.frame.Proto3TransferFrameDefinition;
import jpl.gds.serialization.frame.Proto3TransferFrameDefinition.Proto3EncodingType;
import jpl.gds.serialization.frame.Proto3TransferFrameDefinition.Proto3FrameFormatDefinition;
import jpl.gds.serialization.frame.Proto3TransferFrameDefinition.Proto3FrameFormatDefinition.Proto3FrameFormatDefinitionTypeName;
import jpl.gds.serialization.frame.Proto3TransferFrameDefinition.Proto3FrameTimeFieldDefinition.Proto3FrameTimeFieldDefinitionTimecodeType;
import jpl.gds.serialization.frame.Proto3TransferFrameDefinition.Proto3FrameTimeFieldDefinition;
import jpl.gds.serialization.primitives.keyvalue.Proto3KeyValue;
import jpl.gds.shared.log.TraceManager;


/**
 * TransferFrameDefinition describes a transfer frame dictionary entry, which
 * defines the format of one transfer frame for a specific project/mission. This
 * is a multimission dictionary definition object. Do not create an instance
 * directly. Go through TransferFrameDefinitionFactory.
 *
 */
public class TransferFrameDefinition implements ITransferFrameDefinition {

	private int caduBitSize; 
	private int encodedCaduBitSize; 
	private int encodingBitSize; 
	private String name;
	private EncodingType encoding = EncodingType.UNENCODED;
	private int asmBitSize; 
	private byte asm[];
	private int primaryHeaderSize;
	private int secondaryHeaderSize;
	private int operationalControlSize;
	private int frameErrorControlSize;
	private int headerErrorControlSize;
	private int dataAreaSize;
	private int pduHeaderSize;
	private boolean arrivesWithAsm = true;
	private IFrameFormatDefinition formatType = new FrameFormatDefinition(IFrameFormatDefinition.TypeName.UNKNOWN);;	
	private KeyValueAttributes attributes = new KeyValueAttributes();
    private String description;
    private IFrameTimeFieldDefinition timeField;


	/**
	 * Constructor. 
	 * 
	 */
	/* package */ TransferFrameDefinition() {}
	    
    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.frame.ITransferFrameDefinition#getTurboRate()
     */
	@Override
    public String getTurboRate() {
	    if (encoding != null && encoding.isTurbo()) {
	        return "1/" + this.encoding.getEncodingRate();
	    } 
	    return null;
	}

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.frame.ITransferFrameDefinition#getASMSizeBytes()
     */
	@Override
    public int getASMSizeBytes() {
		return convertToByteSize(asmBitSize);
	}

	/**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.frame.ITransferFrameDefinition#setASMSizeBits(int)
     */
	@Override
    public void setASMSizeBits(final int size) {
		asmBitSize = size;
	}

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.frame.ITransferFrameDefinition#getASM()
     */
	@Override
    public byte[] getASM() {
		int len = asmBitSize / Byte.SIZE;
		byte[] tbuff = new byte[len];
		System.arraycopy(asm, 0, tbuff, 0, len);
		return tbuff;
	}

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.frame.ITransferFrameDefinition#setASM(byte[])
     */
	@Override
    public void setASM(final byte[] buff) {
	    if (buff == null) {
	        throw new IllegalArgumentException("ASM buffer cannot be null");
	    }
		asm = new byte [ buff.length ]; 
		System.arraycopy(buff, 0, asm, 0, buff.length);
		setASMSizeBits(buff.length * Byte.SIZE);
	}

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.frame.ITransferFrameDefinition#getCADUSizeBytes()
     */
	@Override
    public int getCADUSizeBytes() { 
		return convertToByteSize(caduBitSize); 
	}

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.frame.ITransferFrameDefinition#getCADUSizeBits()
     */
	@Override
    public int getCADUSizeBits() { 
		return caduBitSize; 
	}

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.frame.ITransferFrameDefinition#setCADUSizeBits(int)
     */
	@Override
    public void setCADUSizeBits(final int bits) {
        if (bits % 8 != 0) {
            TraceManager.getDefaultTracer().warn(
                    "CADU size for this Transfer Frame Dictionary is not byte aligned. Processing telemetry may cause errors.");
        }
		caduBitSize = bits; 
	}

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.frame.ITransferFrameDefinition#setEncodedCADUSizeBits(int)
     */
	@Override
    public void setEncodedCADUSizeBits(final int size) {
		encodedCaduBitSize = size;
	}

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.frame.ITransferFrameDefinition#getEncodedCADUSizeBytes()
     */
	@Override
    public double getEncodedCADUSizeBytes() {
		return encodedCaduBitSize/8.0;
	}


	/**
	 * {@inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.frame.ITransferFrameDefinition#getDataAreaSizeBytes()
	 */
	@Override
    public int getDataAreaSizeBytes() {
	    return convertToByteSize(this.dataAreaSize);
	}
	
	/**
	 * @{inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.frame.ITransferFrameDefinition#setDataAreaSizeBits(int)
	 */
	@Override
    public void setDataAreaSizeBits(int size) {
	    this.dataAreaSize = size;
	}

	/**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.frame.ITransferFrameDefinition#getEncoding()
     */
	@Override
    public EncodingType getEncoding() {
		return encoding;
	}

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.frame.ITransferFrameDefinition#setEncoding(jpl.gds.dictionary.impl.impl.api.frame.EncodingType)
     */
	@Override
    public void setEncoding(final EncodingType encoding) {
		this.encoding = encoding;
	}

	/**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.frame.ITransferFrameDefinition#getEncodingSizeBytes()
     */
	@Override
    public int getEncodingSizeBytes() {
		return convertToByteSize(encodingBitSize);
	}
	
	/**
	 * @{inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.frame.ITransferFrameDefinition#getEncodingSizeBits()
	 */
	@Override
    public int getEncodingSizeBits() {
	    return this.encodingBitSize;
	}
        
	/**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.frame.ITransferFrameDefinition#setEncodingSizeBits(int)
     */
	@Override
    public void setEncodingSizeBits(final int encSize) {
		this.encodingBitSize = encSize;
	}

	/**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.frame.ITransferFrameDefinition#getName()
     */
	@Override
    public String getName() {
		return name;
	}

	/**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.frame.ITransferFrameDefinition#setName(java.lang.String)
     */
	@Override
    public void setName(final String name) {
	    if (name == null) {
	        throw new IllegalArgumentException("Frame name may not be null");
	    }
		this.name = name;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuffer ret = new StringBuffer();
		ret.append("Name=" + name + " caduBitSize=" + caduBitSize);
		ret.append(" encodedCaduBitSize=" + encodedCaduBitSize);
		ret.append(" primaryHeaderSize=" + primaryHeaderSize);
		ret.append(" secondaryHeaderSize=" + secondaryHeaderSize);
		ret.append(" FHECF Size=" + headerErrorControlSize);
		ret.append(" pduHeaderSize=" + pduHeaderSize);
	    ret.append(" dataAreaSize=" + dataAreaSize);
	    ret.append(" FECF size=" + frameErrorControlSize);
	    ret.append(" OCF size=" + operationalControlSize);
	    
		switch(encoding) {
		case TURBO_1_2:
		case TURBO_1_3:
		case TURBO_1_6:
			ret.append(" encoding=TURBO");
			break;
		case REED_SOLOMON:
			ret.append(" encoding=RS");
			break;
		case UNENCODED:
			ret.append(" encoding=NONE");
			break;
        default:
            ret.append(" encoding=UNKNOWN");
            break;
		}
		ret.append(" encodingBitSize=" + encodingBitSize);
		ret.append(" asmBitSize=" + asmBitSize);
		if(asm != null)
		{
		    ret.append(' ');
		    for (byte b: asm) {
		        ret.append(Integer.toHexString(b));
		    }
		}
		return new String(ret);    
	}

    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.IAttributesSupport#setKeyValueAttribute(java.lang.String, java.lang.String)
     */
    @Override
    public void setKeyValueAttribute(String key, String value) {
        attributes.setKeyValue(key, value);
        
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.IAttributesSupport#getKeyValueAttribute(java.lang.String)
     */
    @Override
    public String getKeyValueAttribute(String key) {
        return attributes.getValueForKey(key);
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.IAttributesSupport#setKeyValueAttributes(jpl.gds.dictionary.impl.impl.api.KeyValueAttributes)
     */
    @Override
    public void setKeyValueAttributes(KeyValueAttributes toSet) {
        attributes.clearKeyValue();
        attributes.copyFrom(toSet);
        
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.IAttributesSupport#getKeyValueAttributes()
     */
    @Override
    public KeyValueAttributes getKeyValueAttributes() {
        return attributes;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.IAttributesSupport#clearKeyValue()
     */
    @Override
    public void clearKeyValue() {
        attributes.clearKeyValue();
        
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.frame.ITransferFrameDefinition#setOperationalControlSizeBits(int)
     */
    @Override
    public void setOperationalControlSizeBits(int size) {
        this.operationalControlSize = size;
        
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.frame.ITransferFrameDefinition#getOperationalControlSizeBytes()
     */
    @Override
    public int getOperationalControlSizeBytes() {
        return convertToByteSize(this.operationalControlSize);
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.frame.ITransferFrameDefinition#hasOperationalControl()
     */
    @Override
    public boolean hasOperationalControl() {
        return this.operationalControlSize != 0;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.frame.ITransferFrameDefinition#setPrimaryHeaderSizeBits(int)
     */
    @Override
    public void setPrimaryHeaderSizeBits(int size) {
        this.primaryHeaderSize = size;
        
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.frame.ITransferFrameDefinition#getPrimaryHeaderSizeBytes()
     */
    @Override
    public int getPrimaryHeaderSizeBytes() {
        return convertToByteSize(this.primaryHeaderSize);
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.frame.ITransferFrameDefinition#setSecondaryHeaderSizeBits(int)
     */
    @Override
    public void setSecondaryHeaderSizeBits(int size) {
        this.secondaryHeaderSize = size;
        
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.frame.ITransferFrameDefinition#getSecondaryHeaderSizeBytes()
     */
    @Override
    public int getSecondaryHeaderSizeBytes() {
        return convertToByteSize(this.secondaryHeaderSize);
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.frame.ITransferFrameDefinition#hasSecondaryHeader()
     */
    @Override
    public boolean hasSecondaryHeader() {
        return this.secondaryHeaderSize != 0;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.frame.ITransferFrameDefinition#setHeaderErrorControlSizeBits(int)
     */
    @Override
    public void setHeaderErrorControlSizeBits(int size) {
        this.headerErrorControlSize = size;
        
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.frame.ITransferFrameDefinition#getHeaderErrorControlSizeBytes()
     */
    @Override
    public int getHeaderErrorControlSizeBytes() {
        return convertToByteSize(this.headerErrorControlSize);
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.frame.ITransferFrameDefinition#hasHeaderErrorControl()
     */
    @Override
    public boolean hasHeaderErrorControl() {
        return this.headerErrorControlSize != 0;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.frame.ITransferFrameDefinition#setFrameErrorControlSizeBits(int)
     */
    @Override
    public void setFrameErrorControlSizeBits(int size) {
        this.frameErrorControlSize = size;
        
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.frame.ITransferFrameDefinition#getFrameErrorControlSizeBytes()
     */
    @Override
    public int getFrameErrorControlSizeBytes() {
        return convertToByteSize(this.frameErrorControlSize);
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.frame.ITransferFrameDefinition#hasFrameErrorControl()
     */
    @Override
    public boolean hasFrameErrorControl() {
        return this.frameErrorControlSize != 0;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.frame.ITransferFrameDefinition#setDescription(java.lang.String)
     */
    @Override
    public void setDescription(String description) {
        this.description = description;
        
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.frame.ITransferFrameDefinition#getDescription()
     */
    @Override
    public Optional<String> getDescription() {
        return Optional.ofNullable(this.description);
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.frame.ITransferFrameDefinition#setArrivesWithASM(boolean)
     */
    @Override
    public void setArrivesWithASM(boolean enable) {
        this.arrivesWithAsm = enable;
        
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.frame.ITransferFrameDefinition#arrivesWithASM()
     */
    @Override
    public boolean arrivesWithASM() {
        return this.arrivesWithAsm;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.frame.ITransferFrameDefinition#getTotalHeaderSizeBytes()
     */
    @Override
    public int getTotalHeaderSizeBytes() {
        return (this.primaryHeaderSize + this.secondaryHeaderSize + this.headerErrorControlSize + this.pduHeaderSize) / Byte.SIZE;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.frame.ITransferFrameDefinition#setPduHeaderSizeBits(int)
     */
    @Override
    public void setPduHeaderSizeBits(int size) {
        this.pduHeaderSize = size;
        
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.frame.ITransferFrameDefinition#getPduHeaderSizeBytes()
     */
    @Override
    public int getPduHeaderSizeBytes() {
        return convertToByteSize(this.pduHeaderSize);
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.frame.ITransferFrameDefinition#hasPduHeader()
     */
    @Override
    public boolean hasPduHeader() {
        return this.pduHeaderSize != 0;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.frame.ITransferFrameDefinition#setFormat(jpl.gds.dictionary.impl.impl.api.config.IFrameFormatDefinition)
     */
    @Override
    public void setFormat(IFrameFormatDefinition type) {
        if (type == null) {
            this.formatType = new FrameFormatDefinition(IFrameFormatDefinition.TypeName.UNKNOWN);
        } else {
            this.formatType = type;
        }
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.frame.ITransferFrameDefinition#getFormat()
     */
    @Override
    public IFrameFormatDefinition getFormat() {
        return this.formatType;
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.frame.ITransferFrameDefinition#setTimeField(jpl.gds.dictionary.impl.impl.api.frame.IFrameTimeFieldDefinition)
     */
    @Override
    public void setTimeField(IFrameTimeFieldDefinition timeField) {
        this.timeField = timeField;      
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.frame.ITransferFrameDefinition#getTimeField()
     */
    @Override
    public Optional<IFrameTimeFieldDefinition> getTimeField() {
        return Optional.ofNullable(this.timeField);
    }

    private int convertToByteSize(int bitSize) {
        return (int) Math.ceil((double)bitSize/Byte.SIZE);
    }

    @Override
    public Proto3TransferFrameDefinition build() {
        Proto3TransferFrameDefinition.Builder retVal = Proto3TransferFrameDefinition.newBuilder();
        retVal.setCaduBitSize(caduBitSize);
        retVal.setEncodedCaduBitSize(encodedCaduBitSize);
        retVal.setEncodingBitSize(encodingBitSize);
        retVal.setName(name);
        
        switch(encoding) {
            case ANY_TURBO:
                retVal.setEncoding(Proto3EncodingType.ANY_TURBO);
                break;
            case BYPASS:
                retVal.setEncoding(Proto3EncodingType.BYPASS);
                break;
            case REED_SOLOMON:
                retVal.setEncoding(Proto3EncodingType.REED_SOLOMON);
                break;
            case TURBO_1_2:
                retVal.setEncoding(Proto3EncodingType.TURBO_1_2);
                break;
            case TURBO_1_3:
                retVal.setEncoding(Proto3EncodingType.TURBO_1_3);
                break;
            case TURBO_1_4:
                retVal.setEncoding(Proto3EncodingType.TURBO_1_4);
                break;
            case TURBO_1_6:
                retVal.setEncoding(Proto3EncodingType.TURBO_1_6);
                break;
            case UNENCODED:
                retVal.setEncoding(Proto3EncodingType.UNENCODED);
                break;
            default:
                retVal.setEncoding(Proto3EncodingType.UNRECOGNIZED);
                break;
        }
        
        retVal.setAsmBitSize(asmBitSize);
        retVal.setAsm(ByteString.copyFrom(asm));
        retVal.setPrimaryHeaderSize(primaryHeaderSize);
        retVal.setSecondaryHeaderSize(secondaryHeaderSize);
        retVal.setOperationalControlSize(operationalControlSize);
        retVal.setFrameErrorControlSize(frameErrorControlSize);
        retVal.setDataAreaSize(dataAreaSize);
        retVal.setPduHeaderSize(pduHeaderSize);
        retVal.setArrivesWithAsm(arrivesWithAsm);
        
        if (formatType != null) {
            Proto3FrameFormatDefinition.Builder ffdBuilder = Proto3FrameFormatDefinition.newBuilder();
            if (formatType.getType() != TypeName.UNKNOWN) {
                if (formatType.getType() == TypeName.CUSTOM_CLASS) {
                    ffdBuilder.setType(Proto3FrameFormatDefinitionTypeName.CUSTOM_CLASS);
                    ffdBuilder.setFrameHeaderClass(formatType.getFrameHeaderClass());
                    ffdBuilder.setFrameErrorControlClass(formatType.getFrameErrorControlClass());
                } else {
                    switch(formatType.getType()) {
                        case CCSDS_AOS_2_BPDU:
                            ffdBuilder.setType(Proto3FrameFormatDefinitionTypeName.CCSDS_AOS_2_BPDU);
                            break;
                        case CCSDS_AOS_2_MPDU:
                            ffdBuilder.setType(Proto3FrameFormatDefinitionTypeName.CCSDS_AOS_2_MPDU);
                            break;
                        case CCSDS_TM_1:
                            ffdBuilder.setType(Proto3FrameFormatDefinitionTypeName.CCSDS_TM_1);
                            break;
                        default:
                            ffdBuilder.setType(Proto3FrameFormatDefinitionTypeName.UNKNOWN);
                            break;
                    }
                }
            }
            retVal.setFormatType(ffdBuilder.build());
        }
        
        Proto3KeyValue.Builder kvBuilder = Proto3KeyValue.newBuilder();
        for (String key : attributes.getKeys()) {
            kvBuilder.putAttributes(key, attributes.getValueForKey(key));
        }
        
        retVal.setAttributes(kvBuilder.build());
        
        if (this.description != null) {
            retVal.setDescription(description);
        }
        
        if (timeField != null) {
            Proto3FrameTimeFieldDefinition.Builder ftfdBuilder = Proto3FrameTimeFieldDefinition.newBuilder();
            switch(timeField.getType()) {
                case CUSTOM_CLASS:
                    ftfdBuilder.setType(Proto3FrameTimeFieldDefinitionTimecodeType.CUSTOM_CLASS);
                    break;
                case CUSTOM_SCLK:
                    ftfdBuilder.setType(Proto3FrameTimeFieldDefinitionTimecodeType.CUSTOM_SCLK);
                    break;
                case PROJECT_SCLK:
                    ftfdBuilder.setType(Proto3FrameTimeFieldDefinitionTimecodeType.PROJECT_SCLK);
                    break;
                default:
                    ftfdBuilder.setType(Proto3FrameTimeFieldDefinitionTimecodeType.UNRECOGNIZED);
                    break;
            }
            ftfdBuilder.setSize(timeField.getBitSize());
            ftfdBuilder.setOffset(timeField.getBitOffset());
            
            retVal.setTimeField(ftfdBuilder.build());
        }
        
        return retVal.build();
    }

    @Override
    public void load(Proto3TransferFrameDefinition msg) {
        this.caduBitSize = msg.getCaduBitSize();
        this.encodedCaduBitSize = msg.getEncodedCaduBitSize();
        this.encodingBitSize = msg.getEncodingBitSize();
        this.name = msg.getName();
        
        switch(msg.getEncoding()) {
            case ANY_TURBO:
                this.encoding = EncodingType.ANY_TURBO;
                break;
            case BYPASS:
                this.encoding = EncodingType.BYPASS;
                break;
            case REED_SOLOMON:
                this.encoding = EncodingType.REED_SOLOMON;
                break;
            case TURBO_1_2:
                this.encoding = EncodingType.TURBO_1_2;
                break;
            case TURBO_1_3:
                this.encoding = EncodingType.TURBO_1_3;
                break;
            case TURBO_1_4:
                this.encoding = EncodingType.TURBO_1_4;
                break;
            case TURBO_1_6:
                this.encoding = EncodingType.TURBO_1_6;
                break;
            case UNENCODED:
                this.encoding = EncodingType.UNENCODED;
                break;
            case UNRECOGNIZED:
                this.encoding = EncodingType.UNENCODED;
                break;
            default:
                this.encoding = EncodingType.UNENCODED;
                break;
        }
        
        this.asmBitSize = msg.getAsmBitSize();
        this.asm = msg.getAsm().toByteArray();
        this.primaryHeaderSize = msg.getPrimaryHeaderSize();
        this.secondaryHeaderSize = msg.getSecondaryHeaderSize();
        this.operationalControlSize = msg.getOperationalControlSize();
        this.frameErrorControlSize = msg.getFrameErrorControlSize();
        this.dataAreaSize = msg.getDataAreaSize();
        this.pduHeaderSize = msg.getPduHeaderSize();
        this.arrivesWithAsm = msg.getArrivesWithAsm();
        
        Proto3FrameFormatDefinition ffd = msg.getFormatType();
        IFrameFormatDefinition.TypeName ffdType;
        switch(ffd.getType()) {
            case CCSDS_AOS_2_BPDU:
                ffdType = TypeName.CCSDS_AOS_2_BPDU;
                break;
            case CCSDS_AOS_2_MPDU:
                ffdType = TypeName.CCSDS_AOS_2_MPDU;
                break;
            case CCSDS_TM_1:
                ffdType = TypeName.CCSDS_TM_1;
                break;
            case CUSTOM_CLASS:
                ffdType = TypeName.CUSTOM_CLASS;
                break;
            case UNKNOWN:
                ffdType = TypeName.UNKNOWN;
                break;
            case UNRECOGNIZED:
                ffdType = TypeName.UNKNOWN;
                break;
            default:
                ffdType = TypeName.UNKNOWN;
                break;
        }
        
        if (ffdType == TypeName.CUSTOM_CLASS) {
            this.formatType = new FrameFormatDefinition(ffdType, ffd.getFrameHeaderClass(), ffd.getFrameErrorControlClass());
        } else if (ffdType != TypeName.UNKNOWN) {
            this.formatType = new FrameFormatDefinition(ffdType);
        }
        
        this.attributes = new KeyValueAttributes();
        Proto3KeyValue kv = msg.getAttributes();
        for (Entry<String, String> entry : kv.getAttributesMap().entrySet()) {
            this.attributes.setKeyValue(entry.getKey(), entry.getValue());
        }
        
        this.description = msg.getDescription();
        
        if (msg.getTimeField() != null) {
            TimecodeType type;
            switch(msg.getTimeField().getType()) {
                case CUSTOM_CLASS:
                    type = TimecodeType.CUSTOM_CLASS;
                    break;
                case CUSTOM_SCLK:
                    type = TimecodeType.CUSTOM_SCLK;
                    break;
                case PROJECT_SCLK:
                    type = TimecodeType.PROJECT_SCLK;
                    break;
                case UNRECOGNIZED:
                    type = TimecodeType.PROJECT_SCLK;
                    break;
                default:
                    type = TimecodeType.PROJECT_SCLK;
                    break;
                
            }
            this.timeField = new FrameTimeFieldDefinition(type, msg.getTimeField().getOffset(), msg.getTimeField().getSize());
        }
    }
    
    
}
