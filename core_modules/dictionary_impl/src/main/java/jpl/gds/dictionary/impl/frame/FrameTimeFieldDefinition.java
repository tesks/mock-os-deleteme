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

import java.util.HashMap;
import java.util.Map;

import jpl.gds.dictionary.api.frame.IFrameTimeFieldDefinition;
import jpl.gds.shared.reflect.ReflectionException;
import jpl.gds.shared.reflect.ReflectionToolkit;
import jpl.gds.shared.time.ISclkExtractor;


/**
 * Definition object for a frame timecode field.  
 * 
 *
 *
 */
public class FrameTimeFieldDefinition implements IFrameTimeFieldDefinition {

    private int offset;
    private int size;
    private IFrameTimeFieldDefinition.TimecodeType type;
    private ISclkExtractor extractor;
    private String extractorClass;
    private Map<String, Object> params = new HashMap<String, Object>();

    /**
     * Constructor.
     * 
     * @param type the TimecodeType of this time field
     * @param bitOffset the offset of the time field relative to the first bit of the 
     *        primary header
     * @param bitLen the bit size of the time field
     */
    public FrameTimeFieldDefinition(IFrameTimeFieldDefinition.TimecodeType type, int bitOffset, int bitLen) {
        this.offset = bitOffset;
        this.size = bitLen;
        this.extractorClass = type.getDefaultExtractionClass();
        this.type = type;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.frame.IFrameTimeFieldDefinition#setBitOffset(int)
     */
    @Override
    public void setBitOffset(int offset) {
        this.offset = offset;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.frame.IFrameTimeFieldDefinition#getBitOffset()
     */
    @Override
    public int getBitOffset() {
        return offset;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.frame.IFrameTimeFieldDefinition#getType()
     */
    @Override
    public IFrameTimeFieldDefinition.TimecodeType getType() {
        return type;
    }


    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.frame.IFrameTimeFieldDefinition#getExtractor()
     */
    @Override
    public ISclkExtractor getExtractor() throws ReflectionException {
        if (this.extractor == null) {
            this.extractor = (ISclkExtractor) ReflectionToolkit.createObject(this.extractorClass);
            this.extractor.setStaticArgs(params);
        }
        return this.extractor;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.frame.IFrameTimeFieldDefinition#setExtractorClass(java.lang.String)
     */
    @Override
    public void setExtractorClass(String className) {
        this.extractorClass = className;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.frame.IFrameTimeFieldDefinition#getExtractorClass()
     */
    @Override
    public String getExtractorClass() {
        return this.extractorClass;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.frame.IFrameTimeFieldDefinition#getBitSize()
     */
    @Override
    public int getBitSize() {
        return size;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.frame.IFrameTimeFieldDefinition#setBitSize(int)
     */
    @Override
    public void setBitSize(int size) {
        this.size = size;
        
    }

    @Override
    public void setParameterMap(Map<String, Object> params) {
       this.params = params;
        
    }

    @Override
    public Map<String, Object> getParameterMap() {
        return this.params;
    }

}
