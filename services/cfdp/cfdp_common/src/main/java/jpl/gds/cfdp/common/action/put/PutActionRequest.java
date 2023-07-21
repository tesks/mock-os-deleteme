/*
 * Copyright 2006-2019. California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government sponsorship acknowledged.
 *
 *  This software is subject to U. S. export control laws and
 *  regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 *  extent that the software is subject to U.S. export control laws
 *  and regulations, the recipient has the responsibility to obtain
 *  export licenses or other export authority as may be required
 *  before exporting such information to foreign countries or
 *  providing access to foreign nationals.
 */

package jpl.gds.cfdp.common.action.put;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jpl.gds.cfdp.common.GenericRequest;
import jpl.gds.shared.types.UnsignedLong;

import java.util.Collection;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PutActionRequest extends GenericRequest {

    private long destinationEntity;
    private String sourceFileName;
    private byte serviceClass;
    private String destinationFileName;
    private UnsignedLong sessionKey;
    private Collection<String> messagesToUser;
    private byte[] uploadFile;

    /**
     * Shallow-copy this POJO with uploadFile filtered out to a simple empty array. This method is primarily used for
     * logging/publishing/archiving purposes, since we don't want the lengthy uploadFile bytes included.
     *
     * @return cloned (shallow-copy) object with uploadFile always set to empty array
     */
    public PutActionRequest cloneWithUploadFileFilteredOut() {
        final PutActionRequest clone = new PutActionRequest();
        copyFieldsTo(clone);
        clone.setDestinationEntity(this.getDestinationEntity());
        clone.setSourceFileName(this.getSourceFileName());
        clone.setServiceClass(this.getServiceClass());
        clone.setDestinationFileName(this.getDestinationFileName());
        clone.setSessionKey(this.getSessionKey());
        clone.setMessagesToUser(this.getMessagesToUser());
        clone.setUploadFile(new byte[0]);
        return clone;
    }

    /**
     * @return the destinationEntity
     */
    public long getDestinationEntity() {
        return destinationEntity;
    }

    /**
     * @param destinationEntity the destinationEntity to set
     */
    public void setDestinationEntity(final long destinationEntity) {
        this.destinationEntity = destinationEntity;
    }

    /**
     * @return the sourceFileName
     */
    public String getSourceFileName() {
        return sourceFileName;
    }

    /**
     * @param sourceFileName the sourceFileName to set
     */
    public void setSourceFileName(final String sourceFileName) {
        this.sourceFileName = sourceFileName;
    }

    /**
     * @return the serviceClass
     */
    public byte getServiceClass() {
        return serviceClass;
    }

    /**
     * @param serviceClass the serviceClass to set
     */
    public void setServiceClass(final byte serviceClass) {
        this.serviceClass = serviceClass;
    }

    /**
     * @return the destinationFileName
     */
    public String getDestinationFileName() {
        return destinationFileName;
    }

    /**
     * @param destinationFileName the destinationFileName to set
     */
    public void setDestinationFileName(final String destinationFileName) {
        this.destinationFileName = destinationFileName;
    }

    /**
     * @return the sessionKey
     */
    public UnsignedLong getSessionKey() {
        return sessionKey;
    }

    /**
     * @param sessionKey the sessionKey to set
     */
    public void setSessionKey(final UnsignedLong sessionKey) {
        this.sessionKey = sessionKey;
    }

    /**
     * @return Messages to User list
     */
    public Collection<String> getMessagesToUser() {
        return messagesToUser;
    }

    /**
     * @param messagesToUser Messages to User list to set
     */
    public void setMessagesToUser(final Collection<String> messagesToUser) {
        this.messagesToUser = messagesToUser;
    }

    /**
     * @return upload file
     */
    public byte[] getUploadFile() {
        return this.uploadFile;
    }

    /**
     * @param uploadFile the upload file to set
     */
    public void setUploadFile(final byte[] uploadFile) {
        this.uploadFile = uploadFile;
    }

}