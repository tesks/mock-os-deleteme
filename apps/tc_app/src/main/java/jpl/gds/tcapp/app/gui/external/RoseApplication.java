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
package jpl.gds.tcapp.app.gui.external;

import java.io.File;
import java.io.IOException;

import org.springframework.context.ApplicationContext;

import jpl.gds.context.api.IContextIdentification;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.process.LineHandler;
import jpl.gds.shared.process.ProcessLauncher;
import jpl.gds.tc.api.config.CommandProperties;

/**
 * This is the class that implements the ExternalApplication interface to RoSE, the 
 * Mars planning and sequencing application, which can then be executed by the MPCS uplink.
 * 
 *
 */
public class RoseApplication extends AbstractExternalApplication implements ExternalApplication
{
    private static final String APPLICATION_NAME = "RoSE";

    /**
     * Constructor.
     * 
     * @param appContext the ApplicationContext in which this object is being used
     */
    public RoseApplication(final ApplicationContext appContext)
    {
        super(appContext);

        final CommandProperties cmdConfig = appContext.getBean(CommandProperties.class);

        this.process = null;
        this.menuItem = null;
        this.enabled = cmdConfig.getExternalApplicationEnable(APPLICATION_NAME);
        this.waitForExit = cmdConfig.isExternalApplicationWaitForExit(APPLICATION_NAME);
        this.scriptName = cmdConfig.getExternalApplicationScript(APPLICATION_NAME);
        if(this.scriptName == null || this.scriptName.isEmpty()){
        	this.scriptName = "/bin/internal/launch_rose";
        }
        this.scriptName = GdsSystemProperties.getGdsDirectory() + File.separator + this.scriptName;
       
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tcapp.app.gui.external.ExternalApplication#getName()
     */
    @Override
    public String getName()
    {
        return(APPLICATION_NAME);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.tcapp.app.gui.external.ExternalApplication#launch(jpl.gds.shared.process.LineHandler, jpl.gds.shared.process.LineHandler)
     */
    @Override
    public void launch(final LineHandler stdoutHandler,final LineHandler stderrHandler) throws IOException
    {
        if(!this.enabled)
        {
            return;
        }

        final File scriptFile = new File(this.scriptName);
        if(scriptFile.exists() == false)
        {
            throw new IOException("Could not find the required script file " + this.scriptName + ". " + getName() + " is not be able to be launched.");
        }

        this.process = new ProcessLauncher();
        this.process.setOutputHandler(stdoutHandler);
        this.process.setErrorHandler(stderrHandler);

        String cmdPath = null;
        try {
            /* 
             * Replaced method call to CommandDictionaryFactory to get
             * configured file name with access to DictionaryConfiguration.
             */
            cmdPath = appContext.getBean(DictionaryProperties.class).findFileForSystemMission(DictionaryType.COMMAND);
        } catch (final DictionaryException ex) {
            throw new IOException(ex.getMessage());
        }
        this.process.launch(new String[] {this.scriptName, 
                cmdPath, 
                appContext.getBean(IContextIdentification.class).getNumber().toString(),
                String.valueOf(appContext.getBean(IContextIdentification.class).getSpacecraftId()) });
        if(this.waitForExit)
        {
            final int code = this.process.waitForExit();
            if(code != 0)
            {
                throw new IOException(getName() + " terminated with the non-zero return code value of " + code);
            }
        }
    }
}
