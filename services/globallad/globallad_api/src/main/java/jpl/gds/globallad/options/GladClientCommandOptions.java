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
package jpl.gds.globallad.options;

import org.apache.commons.cli.ParseException;

import jpl.gds.globallad.GlobalLadProperties;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.ICommandLineOption;
import jpl.gds.shared.cli.options.StringOptionParser;
import jpl.gds.shared.cli.options.numeric.UnsignedIntOptionParser;
import jpl.gds.shared.types.UnsignedInteger;

/**
 * A class containing client command line option for the Global LAD,
 * which will set entered values into a GlobalLadProperties object.
 */
public class GladClientCommandOptions {
    
    /** Command option for GLAD socket port */
    public final GladSocketPortOption SOCKET_PORT_OPTION;
    /** Command option for GLAD REST port */
    public final GladRestPortOption REST_PORT_OPTION;
    /** Command option for GLAD server host */
    public final GladServerHostOption SERVER_HOST_OPTION;
    
    private final GlobalLadProperties gladProperties;
    
    /**
     * Constructor.
     * 
     * @param gladProps the GlobalLadProperties object to set values into
     */
    public GladClientCommandOptions(final GlobalLadProperties gladProps) {
        
        gladProperties = gladProps;
        
        SOCKET_PORT_OPTION = new GladSocketPortOption(false, 
                UnsignedInteger.valueOf(gladProps.getSocketServerPort()));
        SOCKET_PORT_OPTION.setParser(new GladSocketPortOptionParser());
        
        REST_PORT_OPTION = new GladRestPortOption(false, 
                UnsignedInteger.valueOf(gladProps.getRestPort()));
        REST_PORT_OPTION.setParser(new GladRestPortOptionParser());
          
        SERVER_HOST_OPTION = new GladServerHostOption(false, gladProps.getServerHost());
        SERVER_HOST_OPTION.setParser(new GladHostOptionParser());
        
    }
    
    /**
     * Parser class for GLAD socket port. Sets the parsed value into the GlobalLadProperties
     * object.
     */
    private class GladSocketPortOptionParser extends UnsignedIntOptionParser {
        
        /**
         * Constructor.
         */
        public GladSocketPortOptionParser() {
            setDefaultValue(UnsignedInteger.valueOf(gladProperties.getSocketServerPort()));
        }
        
        /**
         * @{inheritDoc
         * @see jpl.gds.shared.cli.options.numeric.UnsignedIntOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption)
         */
        @Override
        public UnsignedInteger parse(final ICommandLine commandLine,
                final ICommandLineOption<UnsignedInteger> opt) throws ParseException {

            final UnsignedInteger port = super.parse(commandLine, opt);
            if (port != null) {
                gladProperties.setGlobalLadSocketServerPort(port.intValue());
            }
            return port;

        }
        
        /**
         * @{inheritDoc
         * @see jpl.gds.shared.cli.options.AbstractOptionParser#parseWithDefault(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption, boolean, boolean)
         */
        @Override
        public UnsignedInteger parseWithDefault(final ICommandLine commandLine,
                final ICommandLineOption<UnsignedInteger> opt, final boolean required,
                final boolean useDefaults) throws ParseException {
            UnsignedInteger port = super.parse(commandLine, opt,
                    required);
            
            if (port == null && useDefaults) {
                port = getDefaultValue();
            }
            if (port != null) {
                gladProperties.setGlobalLadSocketServerPort(port.intValue());
            }
            return port;
        }

    }
    
    /**
     * Parser class for GLAD REST port. Sets the parsed value into the GlobalLadProperties
     * object.
     */
    private class GladRestPortOptionParser extends UnsignedIntOptionParser {
             
        /**
         * Constructor.
         */
        public GladRestPortOptionParser() {
            setDefaultValue(UnsignedInteger.valueOf(gladProperties.getRestPort()));
        }
        
        /**
         * @{inheritDoc
         * @see jpl.gds.shared.cli.options.numeric.UnsignedIntOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption)
         */
        @Override
        public UnsignedInteger parse(final ICommandLine commandLine,
                final ICommandLineOption<UnsignedInteger> opt) throws ParseException {

            final UnsignedInteger port = super.parse(commandLine, opt);
            if (port != null) {
                gladProperties.setGlobalLadRestServerPort(port.intValue());
            }
            return port;

        }
        /**
         * @{inheritDoc
         * @see jpl.gds.shared.cli.options.AbstractOptionParser#parseWithDefault(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption, boolean, boolean)
         */
        @Override
        public UnsignedInteger parseWithDefault(final ICommandLine commandLine,
                final ICommandLineOption<UnsignedInteger> opt, final boolean required,
                final boolean useDefaults) throws ParseException {
            UnsignedInteger port = super.parse(commandLine, opt,
                    required);
            
            if (port == null && useDefaults) {
                port = getDefaultValue();
            }

            if (port != null) {
                gladProperties.setGlobalLadRestServerPort(port.intValue());
            }
            return port;
        }
    }
    
    /**
     * Parser class for GLAD server host. Sets the parsed value into the GlobalLadProperties
     * object.
     */
    private class GladHostOptionParser extends StringOptionParser {
        
        /**
         * Constructor.
         */
        public GladHostOptionParser() {
            setDefaultValue(gladProperties.getServerHost());
        }
        
        /**
         * @{inheritDoc
         * @see jpl.gds.shared.cli.options.StringOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption)
         */
        @Override
        public String parse(final ICommandLine commandLine,
                final ICommandLineOption<String> opt) throws ParseException {
            final String name = super.parse(commandLine, opt);
            if (name != null) {
                gladProperties.setServerHost(name);
            }

            return name;
        }
        
        /**
         * @{inheritDoc
         * @see jpl.gds.shared.cli.options.AbstractOptionParser#parseWithDefault(jpl.gds.shared.cli.cmdline.ICommandLine,
         *      jpl.gds.shared.cli.options.ICommandLineOption, boolean, boolean)
         */
        @Override
        public String parseWithDefault(final ICommandLine commandLine,
                final ICommandLineOption<String> opt, final boolean required,
                final boolean useDefaults) throws ParseException {
            String host = super.parse(commandLine, opt, required);
            
            if (host == null && useDefaults) {
                host = getDefaultValue();
            }
            if (host != null) {
                gladProperties.setServerHost(host);
            }

            return host;
        }

    }

}
