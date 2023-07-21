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

package jpl.gds.cfdp.processor.mib;

import static jpl.gds.cfdp.processor.mib.MibManager.LOCAL_ENTITY_MIB_PROPERTY_PREFIX;
import static jpl.gds.cfdp.processor.mib.MibManager.REMOTE_ENTITY_MIB_PROPERTY_PREFIX;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.context.ApplicationContext;

import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;

public class MibLoader {

    private final Tracer log;

    private String mibFile;
    private final Map<String, Properties> mibLocal = new ConcurrentHashMap<>();
    private final Map<String, Properties> mibRemote = new ConcurrentHashMap<>();

    Pattern localEntityIdPattern = Pattern.compile(LOCAL_ENTITY_MIB_PROPERTY_PREFIX + "([0-9]+)\\.(.+)$");
    Pattern remoteEntityIdPattern = Pattern.compile(REMOTE_ENTITY_MIB_PROPERTY_PREFIX + "([0-9]+)\\.(.+)$");

    public MibLoader(final ApplicationContext appContext) {
        log = TraceManager.getTracer(appContext, Loggers.CFDP);
    }

    public MibLoader load(final String mibFile) {
        mibLocal.clear();
        mibRemote.clear();

        this.mibFile = mibFile;
        final Properties all = new Properties();

        try (InputStream is = new FileInputStream(mibFile)) {
            all.load(is);
        } catch (final IOException e) {
            log.error("Error loading MIB properties from " + mibFile + ": " + ExceptionTools.getMessage(e), e);
        }

        for (final String keyStr : all.stringPropertyNames()) {
            Matcher matcher = remoteEntityIdPattern.matcher(keyStr);

            if (matcher.find()) {
                final String remoteEntityId = matcher.group(1);

                if (!mibRemote.containsKey(remoteEntityId)) {
                    mibRemote.put(remoteEntityId, new Properties());
                }

                mibRemote.get(remoteEntityId).setProperty(matcher.group(2), all.getProperty(keyStr));
            } else {
                matcher = localEntityIdPattern.matcher(keyStr);

                if (matcher.find()) {
                    final String localEntityId = matcher.group(1);

                    if (!mibLocal.containsKey(localEntityId)) {
                        mibLocal.put(localEntityId, new Properties());
                    }

                    mibLocal.get(localEntityId).setProperty(matcher.group(2), all.getProperty(keyStr));
                } else {
                    log.warn("Unrecognized property key in " + mibFile + ": " + keyStr);
                }

            }

        }

        return this;
    }

    /**
     * @return the mibLocal
     */
    public Map<String, Properties> getMibLocal() {
        return mibLocal;
    }

    /**
     * @return the mibRemote
     */
    public Map<String, Properties> getMibRemote() {
        return mibRemote;
    }

}