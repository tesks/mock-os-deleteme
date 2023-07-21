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
package jpl.gds.shared.spring;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.Resource;

/**
 * A utility class that can generate a reported of Spring beans defined in
 * annotated configuration classes.
 * 
 *
 * @since R8
 */
public class AnnotatedBeanLocator {
	private static final class BeanInfo {
		String beanName;
		String methodName;
		String scope;
		boolean lazy;
		boolean isSingleton;
		private Resource resource;
		private Class<?> returnType;
		
		public BeanInfo(String beanName, BeanDefinition def) throws ClassNotFoundException {
			this.beanName = beanName;
			this.methodName = def.getFactoryMethodName();
			this.scope = def.getScope();
			this.lazy = def.isLazyInit();
			this.isSingleton = def.isSingleton();
			this.resource = ((AbstractBeanDefinition)def).getResource();
			
			// Only do this if there is a method.
			if (this.methodName != null) {
				Class<?> clazz = Class.forName(this.resource.toString());
				for (Method method : clazz.getMethods()) {
					if (method.getName().equals(this.methodName)) {
						this.returnType = method.getReturnType();
					}
				}
			}
		}

		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return StringUtils.join(new Object[] {
					beanName, methodName, this.returnType, scope, lazy, isSingleton, resource
			}, ",");
		}
	}

	private static final String SPRING_PREFIX = "org.springframework";

    private final ConfigurableApplicationContext appContext;
    private final ConfigurableListableBeanFactory factory;
    private final boolean includeSpringBeans;

    /**
     * Constructor.
     * 
     * @param applicationContext the current application context
     * @param includeSpringBeans include beans defined by the spring infrastructure itself
     */
    public AnnotatedBeanLocator(ApplicationContext applicationContext, boolean includeSpringBeans) {
        this.appContext = (ConfigurableApplicationContext) applicationContext;
        this.factory = (ConfigurableListableBeanFactory) appContext.getAutowireCapableBeanFactory();
        this.includeSpringBeans = includeSpringBeans;
    }
    
    /**
     * Build a report detailing all discovered beans.
     * 
     * @return report text
     * @throws ClassNotFoundException if there is a problem locating a spring configuration class.
     */
    public String buildReport() throws ClassNotFoundException {
    	Map<Resource, Collection<BeanInfo>> beans = new HashMap<Resource, Collection<BeanInfo>>();

		for (String beanName : appContext.getBeanDefinitionNames()) {
			if (!includeSpringBeans && beanName.startsWith(SPRING_PREFIX)) {
				continue;
			}

			BeanDefinition bd = factory.getBeanDefinition(beanName);
			BeanInfo beanInfo = new BeanInfo(beanName, bd);
			
			if (!beans.containsKey(beanInfo.resource)) {
				beans.put(beanInfo.resource, new ArrayList<BeanInfo>());
			}
			
			beans.get(beanInfo.resource).add(beanInfo);
		}
		
		StringBuilder builder = new StringBuilder(StringUtils.join(new String[] {
				"Bean Name", "Bean Method Name", "Return Type", "scope", "lazy", "isSingleton", "Configuration Resource"
		}, ","));
		
		builder.append("\n");
		
		beans.values().stream().forEach(beanInfos -> {
			beanInfos.stream().forEach(beanInfo -> {
				builder.append(beanInfo).append("\n");
			});
		});
		
		return builder.toString();
    }

    /**
     * @param outputFilePath full path to the output report file.  If null goes to /tmp/report.csv
     * @throws IOException  if there is a problem writing the output file
     * @throws ClassNotFoundException if there is a problem finding Spring configuration classes
     */
    public void generateReport(String outputFilePath) throws IOException, ClassNotFoundException {
		File opfile = new File(outputFilePath);
		
		try (FileWriter writer = new FileWriter(opfile)){
			writer.write(buildReport());
		}
    }
}
