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
package jpl.gds.cfdp.processor.util;

import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * {@code CfdpFileUtil} is a utility class for common file-based operations.
 *
 * @since 8.2
 */
@Service
public class CfdpFileUtil {

    private Tracer log;

    @Autowired
    private ApplicationContext appContext;

    @PostConstruct
    public void init() {
        log = TraceManager.getTracer(appContext, Loggers.CFDP);
    }

    /**
     * Create parent directory and its parents if it/they don't exist. Throw {@code IOException} if unable to create
     * them, perhaps due to permission issues. If parent directory is root, don't do anything.
     *
     * @param path path of the file or directory whose parent(s) need to be created if not there yet
     * @throws IOException thrown when unable to create the directories
     */
    public void createParentDirectoriesIfNotExist(final String path) throws IOException {
        final Path p = Paths.get(path);

        if (p.getParent() != null && Files.notExists(p.getParent())) {

            /*
            Allow this utility to be used outside of Spring Framework, in which case 'log' would not have been
            instantiated on the first call.
             */
            if (log == null) {
                log = TraceManager.getTracer(Loggers.CFDP);
            }

            log.info("Attempting to create missing directories ", p.getParent());
            Files.createDirectories(p.getParent());
        }

    }

    /**
     * Create directory and its parents if it/they don't exist. Throw {@code IOException} if unable to create
     * them, perhaps due to permission issues. If directory is root, don't do anything.
     *
     * @param path path of the directory to create if not there yet
     * @throws IOException thrown when unable to create the directories
     */
    public void createDirectoriesIfNotExist(final String path) throws IOException {
        final Path p = Paths.get(path);

        if (p.getParent() != null && Files.notExists(p)) {

            /*
            Allow this utility to be used outside of Spring Framework, in which case 'log' would not have been
            instantiated on the first call.
             */
            if (log == null) {
                log = TraceManager.getTracer(Loggers.CFDP);
            }

            log.info("Attempting to create missing directories ", p);
            Files.createDirectories(p);
        }

    }

}