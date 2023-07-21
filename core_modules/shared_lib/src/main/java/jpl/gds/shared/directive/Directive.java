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
package jpl.gds.shared.directive;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A base class for implementing process directives/control commands.
 * 
 *
 * @since R8
 *
 */
public class Directive implements IDirective {
    private String ownerId;
    private IDirectiveType type;
    private Map<String, IDirectiveArgument> arguments = new HashMap<String, IDirectiveArgument>();
    private String lastError;
    
    
    /**
     * Constructor.
     * 
     * @param type the type of the directive
     */
    public Directive(IDirectiveType type) {
        this.type = type;
        
    }
    
    /**
     * Constructor.
     * 
     * @param instanceId the service or process instance ID to which this directive applies
     * @param type the type of the directive
     */
    public Directive(String instanceId, IDirectiveType type) {
        this.ownerId = instanceId;
        this.type = type;
        
    }

    @Override
    public void setOwnerId(String instanceId) {
        this.ownerId = instanceId;
        
    }

    @Override
    public String getOwnerId() {
        return this.ownerId;
    }

    @Override
    public IDirectiveType getType() {
        return type;
    }

    @Override
    public void setType(IDirectiveType type) {
        this.type = type;
        
    }

    @Override
    public void setArguments(List<IDirectiveArgument> args) {
        this.arguments.clear();
        if (args == null) {
            return;
        }
        for (IDirectiveArgument arg : args) {
            this.arguments.put(arg.getName(), arg);
        }       
    }

    @Override
    public Map<String, IDirectiveArgument> getArguments() {
        return this.arguments;
    }
    
    @Override
    public Object getArgumentValue(String name) {
        return this.arguments.get(name) == null ? null : this.arguments.get(name).getValue();
    }

    @Override
    public void resetLastMessage() {
        this.lastError = null;
        
    }

    @Override
    public String getLastMessage() {
        return lastError;
    }
    @Override
    public void setLastMessage(String errorString) {
        lastError = errorString;
        
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(this.type.getDirectiveName());
        for (String key: arguments.keySet()) {
            builder.append(" ");
            builder.append(key + "=" + arguments.get(key).getValue());
        }
            
        return builder.toString();
    }

   
    @Override
    public IDirectiveArgument getArgument(String argName) {
        return arguments.get(argName);
    }

    @Override
    public void addArgument(IDirectiveArgument arg) {
        this.arguments.put(arg.getName(), arg);
        
    }

}
