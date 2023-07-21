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
package jpl.gds.globallad.rest;


import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;

import jpl.gds.globallad.data.container.IGlobalLadSerializable;
import jpl.gds.globallad.data.utilities.JsonUtilities;

/**
 * Used by jersey to convert global lad data into JSON.  
 * 
 * This is used if you let jersey figure out how to do the serialization for you.  It will use the object mapper
 * without the streaming api so this is not ideal.  This is set up as a fallback but is not used by the global lad
 * query algorithms or the dump algorithms.
 */
@Provider
public class GlobalLadObjectMapperProvider implements ContextResolver<ObjectMapper> {

    final ObjectMapper requestObjectMapper;
    final ObjectMapper serializationObjectMapper;
    final ObjectMapper newMapper;

    /**
     * 
     */
    public GlobalLadObjectMapperProvider() {
        requestObjectMapper = JsonUtilities.createRequestObjectMapperWithIndent();
        serializationObjectMapper = JsonUtilities.createSerializerObjectMapperWithIndent();
        newMapper = JsonUtilities.createNewObjectMapperWithIndent();
    }

    /**
     * When jersey gets the context, which is the object mapper to use for serializing objects 
     * as an output this will decide the mapper to use.  The mappers are configured with views
     * depending of the output request.  
     * 
     * 
     * @see javax.ws.rs.ext.ContextResolver#getContext(java.lang.Class)
     */
    @Override
    public ObjectMapper getContext(final Class<?> type) {
    		/**
    		 * This is not an ideal way to do this.  The new mapper is set up 
    		 * to use the dehydrate method
    		 */
    		if (type == IGlobalLadSerializable.class) {
    			return newMapper;
    		} else {
    			return requestObjectMapper;
    		}
    }
}