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

import jpl.gds.dictionary.api.config.IFrameFormatDefinition;
import jpl.gds.dictionary.api.frame.ITransferFrameDefinition;

/**
 * FrameFormat is a utility class used to store details about telemetry frame
 * format, including its type (as an enumeration value) and adaptation classes
 * used to process it. FrameFormat is used in an ITransferFrameDefinition object.
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE ADAPTATION INTERFACE</b><br>
 * <br>
 * <b>This is a controlled interface. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b><br>
 * <br>
 * 
 *
 * @see ITransferFrameDefinition
 */
public class FrameFormatDefinition implements IFrameFormatDefinition {
    
    private String frameHeaderClass;
    private String frameErrorControlClass;
    private IFrameFormatDefinition.TypeName type;
    
    /**
     * Constructor for a built-in frame type. Supporting class names are pulled
     * from the TypeName enumeration value.
     * 
     * @param type TypeName of this frame format
     */
    public FrameFormatDefinition(IFrameFormatDefinition.TypeName type) {
        this.type = type;
        this.frameHeaderClass = type.getDefaultFrameHeaderClass();
        this.frameErrorControlClass = type.getDefaultFrameErrorControlClass();
    }
    
    /**
     * Constructor for a customized frame type. The TypeName may still be one
     * of the built-in types, but the processing classes are overridden with
     * those supplied here.
     * 
     * @param type TypeName of this frame format
     * @param headerClass
     *            the full class name of the frame header (IFrameHeader)
     *            class.
     * @param frameErrorClass
     *            the full class name of the frame error control computation
     *            (IFrameChecksumComputation) class
     */
    public FrameFormatDefinition(IFrameFormatDefinition.TypeName type, String headerClass, String frameErrorClass) {
        this.type = type;
        this.frameHeaderClass = headerClass;
        this.frameErrorControlClass = frameErrorClass;
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.config.IFrameFormatDefinition#getFrameHeaderClass()
     */
    @Override
    public String getFrameHeaderClass() {
        return frameHeaderClass;
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.config.IFrameFormatDefinition#getFrameErrorControlClass()
     */
    @Override
    public String getFrameErrorControlClass() {
        return frameErrorControlClass;
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.config.IFrameFormatDefinition#setFrameHeaderClass(java.lang.String)
     */
    @Override
    public void setFrameHeaderClass(String headerClass) {
        this.frameErrorControlClass = headerClass;
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.config.IFrameFormatDefinition#setFrameErrorControlClass(java.lang.String)
     */
    @Override
    public void setFrameErrorControlClass(String errorClass) {
        this.frameErrorControlClass = errorClass;
    }
    
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.config.IFrameFormatDefinition#getType()
     */
    @Override
    public IFrameFormatDefinition.TypeName getType() {
        return this.type;
    }
}
