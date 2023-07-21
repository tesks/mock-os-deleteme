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
package jpl.gds.perspective.spring.bootstrap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

import jpl.gds.perspective.PerspectiveConfiguration;
import jpl.gds.perspective.PerspectiveCounters;
import jpl.gds.perspective.config.PerspectiveProperties;
import jpl.gds.perspective.message.ChangePerspectiveMessage;
import jpl.gds.perspective.message.ExitPerspectiveMessage;
import jpl.gds.perspective.message.MergePerspectiveMessage;
import jpl.gds.perspective.message.PerspectiveMessageType;
import jpl.gds.perspective.message.SavePerspectiveMessage;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.message.RegisteredMessageConfiguration;
import jpl.gds.shared.spring.context.flag.SseContextFlag;

@Configuration
public class PerspectiveSpringBootstrap {
    
    public PerspectiveSpringBootstrap() {
        MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(
                PerspectiveMessageType.ChangePerspective, ChangePerspectiveMessage.XmlParseHandler.class.getName(),
                null));
        MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(PerspectiveMessageType.ExitPerspective,
                ExitPerspectiveMessage.XmlParseHandler.class.getName(), null));
        MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(PerspectiveMessageType.MergePerspective,
                MergePerspectiveMessage.XmlParseHandler.class.getName(), null));
        MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(PerspectiveMessageType.SavePerspective,
                SavePerspectiveMessage.XmlParseHandler.class.getName(), null));
    }
    
	public static final String PERSPECTIVE_PROPERTIES = "PERSPECTIVE_PROPERTIES";
	public static final String PERSPECTIVE_CONFIGURATION = "PERSPECTIVE_CONFIGURATION";
	public static final String PERSPECTIVE_COUNTERS = "PERSPECTIVE_COUNTERS";
	
    /**
     * @param sseFlag
     *            The SSE context flag
     * @return PerspectiveProperties
     */
	@Bean(name=PERSPECTIVE_PROPERTIES ) 
	@Scope("singleton")
	@Lazy(value = true)
    public PerspectiveProperties getPerspectiveProperties(final SseContextFlag sseFlag) {
        return new PerspectiveProperties(sseFlag);
	}
	
	@Bean(name=PERSPECTIVE_CONFIGURATION ) 
	@Scope("singleton")
	@Lazy(value = true)
	public PerspectiveConfiguration getPerspectiveConfiguration(final ApplicationContext appContext) {
		 return new PerspectiveConfiguration(appContext);
	}
	
	
	@Bean(name=PERSPECTIVE_COUNTERS ) 
	@Scope("singleton")
	@Lazy(value = true)
	public PerspectiveCounters getPerspectiveCounters(final PerspectiveProperties props) {
		 return new PerspectiveCounters(props);
	}
}
