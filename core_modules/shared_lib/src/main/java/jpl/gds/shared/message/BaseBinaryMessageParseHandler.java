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
package jpl.gds.shared.message;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.xml.sax.helpers.DefaultHandler;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;

import jpl.gds.shared.metadata.ISerializableMetadata;
import jpl.gds.shared.metadata.MetadataMap;

/**
 * BaseBinaryMessageParseHandler is a base class for performing parsing of
 * binary messages using protobuf. It provides common functionality and convenience
 * methods to its subclasses.
 * 
 *
 *  Added helper functions for protobuf message parsing.
 *  Reads a protobuf "int32" from the first 1-5 bytes of the supplied data source
 */
public abstract class BaseBinaryMessageParseHandler extends DefaultHandler implements IBinaryMessageParseHandler {
    /**
     * All messages that were added from parsing.
     */
    private List<IMessage> messages;

    /**
     * Latest added message.
     */
    private IMessage currentMessage;
    
    private ISerializableMetadata contextHeader;
   
    /**
     * Sets the message object that resulted from the parse. Also sets the
     * current message.
     * @param m the message to set
     */
    protected void addMessage(final IMessage m) {
        if (this.messages == null) {
            this.messages = new ArrayList<IMessage>();
        }
      
        this.messages.add(m);
        this.currentMessage = m;
        if (this.contextHeader != null) {
            if (!this.contextHeader.isIdOnly()) {
            	this.currentMessage.setContextHeader(this.contextHeader);
            }
            this.currentMessage.setContextKey(this.contextHeader.getContextKey());
        }
    }

    /**
     * Gets the message current being constructed.
     * @return the current message
     */
    protected IMessage getCurrentMessage() {
        return this.currentMessage;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.message.IBinaryMessageParseHandler#getMessages()
     */
    @Override
    public IMessage[] getMessages() {
        if (this.messages == null) {
            return null;
        }
        final IMessage[] result = new IMessage[this.messages.size()];
        return this.messages.toArray(result);
    }

    /**
     * Clears the current message and metadata context header.
     */
    protected void clearCurrentMessage() {
        this.currentMessage = null;
        this.contextHeader = null;
    }
    
    /**
     * Parses the message metadata context header from a binary stream and stores the
     * context header locally.
     * 
     * @param body
     *            the byte array to parse from
     * @param off
     *            the starting offset in the byte array
     * @return the next offset in the byte array
     * @throws IOException
     *             if there is a parsing issue
     */
    protected int parseContext(final byte[] body, final int off) throws IOException {

        this.contextHeader =  new MetadataMap();

        final int newOff = this.contextHeader.fromContextBinary(body, off);
        if (this.contextHeader.getContextId() != null && !this.contextHeader.getContextId().isEmpty()
                && this.currentMessage != null) {
            if (!this.contextHeader.isIdOnly()) {
                this.currentMessage.setContextHeader(this.contextHeader);
            }
            this.currentMessage.setContextKey(this.contextHeader.getContextKey());
        }
        return newOff;
        
    }
    
    /**
     * Gets the current message metadata context header.
     * 
     * @return ISerializableContext, may be null if no header has been parsed
     */
    protected ISerializableMetadata getContextHeader() {
        return this.contextHeader;
    }
    
    /**
     * Parses the size of the next Protobuf item from the input stream. The
     * current position of the input stream is retained for proper parsing by
     * the protobuf message's parseDelimitedFrom function. The length is stored
     * as per google protobuf standards. If not enough bytes are available to
     * read the length, -1 is returned.
     * 
     * @param input
     *            the ByteArrayInputStream containing the protobuf message
     * @return The length of the next protobuf message, or -1 if not enough
     *         bytes are available to read
     * @throws IOException
     *             an error was encountered while reading the next message size.
     */
    public static int nextMessageSize(final ByteArrayInputStream input) throws IOException{
    	//size is never more than 5 bytes
    	input.mark(5);
    	final int firstByte = input.read();
    	int size = -1;
    	try {
    	    size = CodedInputStream.readRawVarint32(firstByte, input);
    	} finally{
    	    input.reset();
    	}
		
        return size + CodedOutputStream.computeUInt32SizeNoTag(size);
    }
    
    /**
     * Parses the size of the next Protobuf item from the byte array at the
     * provided offset. The offset is not adjusted to allow or proper parsing by
     * the protobuf message's parseDelimitedFrom function. The length is stored
     * as per google protobuf standards. If not enough bytes are available to
     * read the length, -1 is returned
     * 
     * @param input
     *            the byte array containing the protobuf message
     * @param offset
     *            the number of bytes to be skipped over to find the desired
     *            protobuf message length
     * @return the length of the next protobuf message, or -1 if not enough
     *         bytes are available to read
     * @throws IOException
     *             an error was encountered while reading the next message size.
     */
    public static int nextMessageSize(final byte[] input, final int offset) throws IOException{
    	if(offset > input.length || offset < 0){
    		return -1;
    	}
    	final ByteArrayInputStream bais = new ByteArrayInputStream(input);
    	if(offset > 0){
    		try {
    			final byte[] discard = new byte[offset];
				bais.read(discard);
			} catch (final IOException e) {
				return -1;
			}
    	}
    	return nextMessageSize(bais);
    }
    

}
