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
package jpl.gds.globallad.data;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.protobuf.ByteString;

import jpl.gds.globallad.data.json.ByteArrayDeSerializer;
import jpl.gds.globallad.data.json.ByteArraySerializer;
import jpl.gds.globallad.data.json.views.GlobalLadSerializationViews;
import jpl.gds.serialization.globallad.data.Proto3EvrGladData;
import jpl.gds.serialization.globallad.data.Proto3GlobalLadTransport;

/**
 * The EVR channel type implementation.  
 */
public class EvrGlobalLadData extends AbstractGlobalLadData {
	/**
	 * Adding EVR metadata fields and all getters / setters related to them.
	 */
	
	/**
	 * Store everything as byte[]
	 */
	@JsonView(GlobalLadSerializationViews.SerializationView.class)
	@JsonSerialize(using = ByteArraySerializer.class)
	@JsonDeserialize(using = ByteArrayDeSerializer.class)
	private byte[] taskNameRaw;
	
	@JsonView(GlobalLadSerializationViews.SerializationView.class)
	@JsonSerialize(using = ByteArraySerializer.class)
	@JsonDeserialize(using = ByteArrayDeSerializer.class)
	private byte[] sequenceIdRaw;
	
	@JsonView(GlobalLadSerializationViews.SerializationView.class)
	@JsonSerialize(using = ByteArraySerializer.class)
	@JsonDeserialize(using = ByteArrayDeSerializer.class)
	private byte[] categorySequenceIdRaw;
	
	@JsonView(GlobalLadSerializationViews.SerializationView.class)
	@JsonSerialize(using = ByteArraySerializer.class)
	@JsonDeserialize(using = ByteArrayDeSerializer.class)
	private byte[] addressStackRaw;
	
	@JsonView(GlobalLadSerializationViews.SerializationView.class)
	@JsonSerialize(using = ByteArraySerializer.class)
	@JsonDeserialize(using = ByteArrayDeSerializer.class)
	private byte[] sourceRaw;
	
	@JsonView(GlobalLadSerializationViews.SerializationView.class)
	@JsonSerialize(using = ByteArraySerializer.class)
	@JsonDeserialize(using = ByteArrayDeSerializer.class)
	private byte[] taskIdRaw;
	
	@JsonView(GlobalLadSerializationViews.SerializationView.class)
	@JsonSerialize(using = ByteArraySerializer.class)
	@JsonDeserialize(using = ByteArrayDeSerializer.class)
	private byte[] errnoRaw;
	
	@JsonProperty("identifier")
	@JsonView(GlobalLadSerializationViews.SerializationView.class)
	private String evrLevel;
	
	@JsonView(GlobalLadSerializationViews.SerializationView.class)
	@JsonSerialize(using = ByteArraySerializer.class)
	@JsonDeserialize(using = ByteArrayDeSerializer.class)
	private byte[] evrNameRaw;
	
	@JsonView(GlobalLadSerializationViews.GlobalView.class)
	private long evrId;
	
	@JsonView(GlobalLadSerializationViews.SerializationView.class)
	@JsonSerialize(using = ByteArraySerializer.class)
	@JsonDeserialize(using = ByteArrayDeSerializer.class)
	private byte[] messageRaw;
	
	@JsonView(GlobalLadSerializationViews.GlobalView.class)
	private boolean isRealTime;

	@JsonView(GlobalLadSerializationViews.GlobalView.class)
	private boolean isFsw;

	/**
	 * Marshaling constructor.
	 */
	public EvrGlobalLadData() {
		super();
		this.evrLevel = null;
		this.evrNameRaw = null;
		this.evrId = -1;
		this.messageRaw = null;
		this.isRealTime = false;
		this.isFsw = false;
	}

	/**
	 * @param proto
	 * @throws GlobalLadDataException
	 */
	public EvrGlobalLadData(Proto3EvrGladData proto) throws GlobalLadDataException {
		super(proto.getBase());

		this.evrId = proto.getEvrId();
		this.evrLevel = proto.getLevel();
		this.evrNameRaw = proto.getEvrNameRaw().toByteArray();
		this.messageRaw = proto.getMessageRaw().toByteArray();
		this.taskNameRaw = proto.getTaskNameRaw().toByteArray();
		this.sequenceIdRaw = proto.getSequenceIdRaw().toByteArray();
		this.categorySequenceIdRaw = proto.getCategorySequenceIdRaw().toByteArray();
		this.addressStackRaw= proto.getAddressStackRaw().toByteArray();
		this.sourceRaw = proto.getSourceRaw().toByteArray();
		this.taskIdRaw = proto.getTaskIdRaw().toByteArray();
		this.errnoRaw = proto.getErrnoRaw().toByteArray();
		this.isFsw = !proto.getIsSse();
		this.isRealTime = proto.getIsRealtime();
	}
	
	/**
	 * When using this constructor the user data type is not set because the UDT mapping has been decoupled with the data objects and has been added to the 
	 * data factories.  A call to setUserDataType must be done with the UDT or the generateDataWord method will throw because the UDT is not known.  
	 * 
	 * @param evrId
	 * @param evrLevel
	 * @param evrName
	 * @param isRealTime
	 * @param isFsw
	 * @param message
	 * @param taskName
	 * @param sequenceId
	 * @param categorySequenceId
	 * @param addressStack
	 * @param source
	 * @param taskId
	 * @param errno
	 * @param sclkCoarse
	 * @param sclkFine
	 * @param ertMilliseconds
	 * @param ertNanoseconds
	 * @param scetMilliseconds
	 * @param scetNanoseconds
	 * @param sessionNumber
	 * @param scid
	 * @param venue
	 * @param dssId
	 * @param vcid
	 * @param host
	 */
	public EvrGlobalLadData(long evrId, String evrLevel, String evrName, 
			boolean isRealTime, boolean isFsw,
			String message, 
			String taskName, String sequenceId, String categorySequenceId, String addressStack,
			String source, String taskId, String errno,
			long sclkCoarse, long sclkFine, 
			long ertMilliseconds, long ertNanoseconds, 
			long scetMilliseconds, long scetNanoseconds, 
			long sessionNumber, int scid, String venue, byte dssId,
			byte vcid, String host) {
		super(sclkCoarse, sclkFine, ertMilliseconds, ertNanoseconds, scetMilliseconds, scetNanoseconds, 
				sessionNumber, scid, venue, dssId, vcid, host);
		this.evrId = evrId;
		this.evrLevel = evrLevel;
		this.evrNameRaw = evrName.getBytes();
		this.messageRaw = message.getBytes();
		this.isRealTime = isRealTime; 
		this.isFsw = isFsw;
		
		/**
		 * adding new values.  Convert to bytes.
		 */
		this.taskNameRaw = taskName == null ? new byte[0] : taskName.getBytes();
		this.sequenceIdRaw = sequenceId == null ? new byte[0] : sequenceId.getBytes();
		this.categorySequenceIdRaw = categorySequenceId == null ? new byte[0] : categorySequenceId.getBytes();
		this.addressStackRaw = addressStack == null ? new byte[0] : addressStack.getBytes();
		this.sourceRaw = source == null ? new byte[0] : source.getBytes();
		this.taskIdRaw = taskId == null ? new byte[0] : taskId.getBytes();
		this.errnoRaw = errno == null ? new byte[0] : errno.getBytes();
	}
	
	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.IGlobalLADData#getContainerIdentifier()
	 */
	@Override
	@JsonIgnore
	public Object getIdentifier() {
		return evrLevel;
	}
	
	/**
	 * Creates a protobuff representation of this.
	 * @return proto buffer object
	 */
	public Proto3EvrGladData serializeEvr() {
		Proto3EvrGladData evr = Proto3EvrGladData.newBuilder()
				.setBase(serializeBase())
				.setEvrId(evrId)
				.setLevel(evrLevel)
				.setEvrNameRaw(ByteString.copyFrom(evrNameRaw))
				.setMessageRaw(ByteString.copyFrom(messageRaw))
				.setTaskNameRaw(ByteString.copyFrom(taskNameRaw))
				.setSequenceIdRaw(ByteString.copyFrom(sequenceIdRaw))
				.setCategorySequenceIdRaw(ByteString.copyFrom(categorySequenceIdRaw))
				.setAddressStackRaw(ByteString.copyFrom(addressStackRaw))
				.setSourceRaw(ByteString.copyFrom(sourceRaw))
				.setTaskIdRaw(ByteString.copyFrom(taskIdRaw))
				.setErrnoRaw(ByteString.copyFrom(errnoRaw))
				.setIsRealtime(isRealTime)
				.setIsSse(!isFsw)
				.build();
		
		return evr;
	}
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.globallad.data.IGlobalLADData#serialize()
	 */
	@Override
	public Proto3GlobalLadTransport serialize() {
		Proto3GlobalLadTransport transport = Proto3GlobalLadTransport.newBuilder()
				.setEvr(serializeEvr())
				.build();
		
		return transport;
	}

	/**
	 * @return the evrId
	 */
	public long getEvrId() {
		return evrId;
	}
	
	/**
	 * @return the evrLevel
	 */
	@JsonView(GlobalLadSerializationViews.RestRequestView.class)
	@JsonProperty("evrLevel")
	public String getEvrLevel() {
		return evrLevel;
	}
	
	/**
	 * @return the evrName
	 */
	@JsonView(GlobalLadSerializationViews.RestRequestView.class)
	@JsonProperty("evrName")
	public String getEvrName() {
		return getStringValueAndIntern(evrNameRaw);
	}

	/**
	 * @return the message
	 */
	@JsonView(GlobalLadSerializationViews.RestRequestView.class)
	@JsonProperty("message")
	public String getMessage() {
		return getStringValueAndIntern(messageRaw);
	}

	public byte[] getMessageRaw() {
		return messageRaw;
	}

	@JsonView(GlobalLadSerializationViews.RestRequestView.class)
	@JsonProperty("TaskName")
	public String getTaskName() {
		return getStringValueAndIntern(taskNameRaw);
	}
	
	@JsonView(GlobalLadSerializationViews.RestRequestView.class)
	@JsonProperty("SequenceId")
	public String getSequenceId() {
		return getStringValueAndIntern(sequenceIdRaw);
	}

	@JsonView(GlobalLadSerializationViews.RestRequestView.class)
	@JsonProperty("CategorySequenceId")
	public String getCategorySequenceId() {
		return getStringValueAndIntern(categorySequenceIdRaw);
	}
	
	@JsonView(GlobalLadSerializationViews.RestRequestView.class)
	@JsonProperty("AddressStack")
	public String getAddressStack() {
		return getStringValueAndIntern(addressStackRaw);
	}
	
	@JsonView(GlobalLadSerializationViews.RestRequestView.class)
	@JsonProperty("Source")
	public String getSource() {
		return getStringValueAndIntern(sourceRaw);
	}
	
	@JsonView(GlobalLadSerializationViews.RestRequestView.class)
	@JsonProperty("TaskId")
	public String getTaskId() {
		return getStringValueAndIntern(taskIdRaw);
	}
	
	@JsonView(GlobalLadSerializationViews.RestRequestView.class)
	@JsonProperty("errno")
	public String getErrno() {
		return getStringValueAndIntern(errnoRaw);
	}

	
	/**
	 * @return the isRealTime
	 */
	public boolean isRealTime() {
		return isRealTime;
	}

	/**
	 * @return the isFsw
	 */
	public boolean isFsw() {
		return this.isFsw;
	}
	
	/**
	 * @param evrId the evrId to set
	 */
	public void setEvrId(long evrId) {
		this.evrId = evrId;
	}

	/**
	 * @param evrLevel the evrLevel to set.
	 */
	@JsonProperty("identifier")
	@JsonView(GlobalLadSerializationViews.SerializationView.class)
	public void setEvrLevel(String evrLevel) {
		this.evrLevel = evrLevel;
	}
	
	/**
	 * @param evrName evrName to set.
	 */
	@JsonProperty("nameRaw")
	public void setEvrName(byte[] evrName) {
		this.evrNameRaw = evrName;
	}
	
	/**
	 * @param message the message to set
	 */
	@JsonProperty("messageRaw")
	public void setMessage(byte[] message) {
		this.messageRaw = message;
	}

	/**
	 * @param isRealTime the isRealTime to set
	 */
	public void setRealTime(boolean isRealTime) {
		this.isRealTime = isRealTime;
	}
	
	/**
	 * @param isFsw the isFsw to set.
	 */
	public void setFsw(boolean isFsw) {
		this.isFsw = isFsw;
	}
	
	/**
	 * @return the raw evr name as a byte array.
	 */
	public byte[] getEvrNameRaw() {
		return evrNameRaw;
	}
	
	/**
	 * @return the taskNameRaw
	 */
	public byte[] getTaskNameRaw() {
		return taskNameRaw;
	}

	/**
	 * @param taskNameRaw the taskNameRaw to set
	 */
	public void setTaskNameRaw(byte[] taskNameRaw) {
		this.taskNameRaw = taskNameRaw;
	}

	/**
	 * @return the sequenceIdRaw
	 */
	public byte[] getSequenceIdRaw() {
		return sequenceIdRaw;
	}

	/**
	 * @param sequenceIdRaw the sequenceIdRaw to set
	 */
	public void setSequenceIdRaw(byte[] sequenceIdRaw) {
		this.sequenceIdRaw = sequenceIdRaw;
	}

	/**
	 * @return the categorySequenceIdRaw
	 */
	public byte[] getCategorySequenceIdRaw() {
		return categorySequenceIdRaw;
	}

	/**
	 * @param categorySequenceIdRaw the categorySequenceIdRaw to set
	 */
	public void setCategorySequenceIdRaw(byte[] categorySequenceIdRaw) {
		this.categorySequenceIdRaw = categorySequenceIdRaw;
	}

	/**
	 * @return the addressStackRaw
	 */
	public byte[] getAddressStackRaw() {
		return addressStackRaw;
	}

	/**
	 * @param addressStackRaw the addressStackRaw to set
	 */
	public void setAddressStackRaw(byte[] addressStackRaw) {
		this.addressStackRaw = addressStackRaw;
	}

	/**
	 * @return the sourceRaw
	 */
	public byte[] getSourceRaw() {
		return sourceRaw;
	}

	/**
	 * @param sourceRaw the sourceRaw to set
	 */
	public void setSourceRaw(byte[] sourceRaw) {
		this.sourceRaw = sourceRaw;
	}

	/**
	 * @return the taskIdRaw
	 */
	public byte[] getTaskIdRaw() {
		return taskIdRaw;
	}

	/**
	 * @param taskIdRaw the taskIdRaw to set
	 */
	public void setTaskIdRaw(byte[] taskIdRaw) {
		this.taskIdRaw = taskIdRaw;
	}

	/**
	 * @return the errnoRaw
	 */
	public byte[] getErrnoRaw() {
		return errnoRaw;
	}

	/**
	 * @param errnoRaw the errnoRaw to set
	 */
	public void setErrnoRaw(byte[] errnoRaw) {
		this.errnoRaw = errnoRaw;
	}

	/**
	 * @param evrNameRaw the evrNameRaw to set
	 */
	public void setEvrNameRaw(byte[] evrNameRaw) {
		this.evrNameRaw = evrNameRaw;
	}

	/**
	 * @param messageRaw the messageRaw to set
	 */
	public void setMessageRaw(byte[] messageRaw) {
		this.messageRaw = messageRaw;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("EvrGlobalLadData [evrLevel=");
		builder.append(evrLevel);
		builder.append(", evrNameRaw=");
		builder.append(Arrays.toString(evrNameRaw));
		builder.append(", evrId=");
		builder.append(evrId);
		builder.append(", messageRaw=");
		builder.append(Arrays.toString(messageRaw));
		builder.append(", isRealTime=");
		builder.append(isRealTime);
		builder.append(", isFsw=");
		builder.append(isFsw);
		builder.append(", eventTime=");
		builder.append(eventTime);
		builder.append(", sclkCoarse=");
		builder.append(sclkCoarse);
		builder.append(", sclkFine=");
		builder.append(sclkFine);
		builder.append(", ertMilliseconds=");
		builder.append(ertMilliseconds);
		builder.append(", ertNanoseconds=");
		builder.append(ertNanoseconds);
		builder.append(", scetMilliseconds=");
		builder.append(scetMilliseconds);
		builder.append(", scetNanoseconds=");
		builder.append(scetNanoseconds);
		builder.append(", venue=");
		builder.append(venue);
		builder.append(", sessionNumber=");
		builder.append(sessionNumber);
		builder.append(", scid=");
		builder.append(scid);
		builder.append(", dssId=");
		builder.append(dssId);
		builder.append(", vcid=");
		builder.append(vcid);
		builder.append(", host=");
		builder.append(host);
		builder.append(", userDataType=");
		builder.append(userDataType);
		builder.append(", insertNumber=");
		builder.append(insertNumber);
		builder.append(", primaryTime=");
		builder.append(primaryTime);
		builder.append("]");
		return builder.toString();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Arrays.hashCode(addressStackRaw);
		result = prime * result + Arrays.hashCode(categorySequenceIdRaw);
		result = prime * result + Arrays.hashCode(errnoRaw);
		result = prime * result + (int) (evrId ^ (evrId >>> 32));
		result = prime * result + ((evrLevel == null) ? 0 : evrLevel.hashCode());
		result = prime * result + Arrays.hashCode(evrNameRaw);
		result = prime * result + (isFsw ? 1231 : 1237);
		result = prime * result + (isRealTime ? 1231 : 1237);
		result = prime * result + Arrays.hashCode(messageRaw);
		result = prime * result + Arrays.hashCode(sequenceIdRaw);
		result = prime * result + Arrays.hashCode(sourceRaw);
		result = prime * result + Arrays.hashCode(taskIdRaw);
		result = prime * result + Arrays.hashCode(taskNameRaw);
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (!(obj instanceof EvrGlobalLadData)) {
			return false;
		}
		EvrGlobalLadData other = (EvrGlobalLadData) obj;
		if (!Arrays.equals(addressStackRaw, other.addressStackRaw)) {
			return false;
		}
		if (!Arrays.equals(categorySequenceIdRaw, other.categorySequenceIdRaw)) {
			return false;
		}
		if (!Arrays.equals(errnoRaw, other.errnoRaw)) {
			return false;
		}
		if (evrId != other.evrId) {
			return false;
		}
		if (evrLevel == null) {
			if (other.evrLevel != null) {
				return false;
			}
		} else if (!evrLevel.equals(other.evrLevel)) {
			return false;
		}
		if (!Arrays.equals(evrNameRaw, other.evrNameRaw)) {
			return false;
		}
		if (isFsw != other.isFsw) {
			return false;
		}
		if (isRealTime != other.isRealTime) {
			return false;
		}
		if (!Arrays.equals(messageRaw, other.messageRaw)) {
			return false;
		}
		if (!Arrays.equals(sequenceIdRaw, other.sequenceIdRaw)) {
			return false;
		}
		if (!Arrays.equals(sourceRaw, other.sourceRaw)) {
			return false;
		}
		if (!Arrays.equals(taskIdRaw, other.taskIdRaw)) {
			return false;
		}
		if (!Arrays.equals(taskNameRaw, other.taskNameRaw)) {
			return false;
		}
		return true;
	}
}
