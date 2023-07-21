/*
 * Copyright 2006-2019. California Institute of Technology.
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

package jpl.gds.tc.mps.impl.ctt.repo;

import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.tc.api.config.CommandProperties;
import jpl.gds.tc.api.exception.CommandFileParseException;
import org.apache.commons.io.FileUtils;

import javax.xml.bind.DatatypeConverter;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Repository utility for retrieving a command dictionary file path for the CTS command translation table library.
 * <p>
 * This file repository implementation uses a temp directory as the file repository. XML command dictionaries are hashed
 * to determine uniqueness; compiled command dictionaries are named with the unique hash combined with the CTS version
 * string to determine uniqueness across dictionary and CTS compiler versions. Compiled command dictionaries will be
 * created if the files do not exist; otherwise the pre-existing files will be used.
 * <p>
 * This class comes with an auto-initializing constructor, and one that does not initialize. If auto initialization is
 * not desired, ensure the file repository is initialized before use.
 *
 */
public class CommandTranslationTableFileRepository implements ICommandTranslationTableRepository {


    private static final String DEFAULT_COMPILED_REPO_PATH = GdsSystemProperties.getSystemProperty(
            "java.io.tmpdir",
            File.separator + "tmp" + File.separator);

    private final Tracer  tracer;
    private final String  cmdDictPath;
    private final String  repoPath;
    private final String  compileXmlBinary;
    private final Boolean validateSchema;

    private CommandTranslationTablePaths paths;
    // xml command dictionary file hash (sha-256)
    private String                       hash;
    private ICtsCxml                     compiler;


    /**
     * Constructor, provide a path to the command dictionary file.
     *
     * @param cmdDictPath       command dictionary path. should end in ".xml"
     * @param commandProperties command properties
     * @param tracer            log tracer
     */
    public CommandTranslationTableFileRepository(final String cmdDictPath, final CommandProperties commandProperties,
                                                 final Tracer tracer) throws CommandFileParseException {
        this(cmdDictPath, DEFAULT_COMPILED_REPO_PATH, commandProperties, tracer);
    }

    /**
     * Constructor, provide both a path to both the command dictionary file, and the compiled command dictionary
     * repository.
     *
     * @param cmdDictPath       command dictionary path. should end in ".xml"
     * @param repoPath          command dictionary repository. should end in the file separator, but does not need to.
     * @param commandProperties command properties
     * @param tracer            log tracer
     */
    public CommandTranslationTableFileRepository(final String cmdDictPath, final String repoPath,
                                                 final CommandProperties commandProperties, final Tracer tracer) throws
                                                                                                                 CommandFileParseException {
        this(cmdDictPath, repoPath, commandProperties, tracer, true);
    }

    /**
     * Constructor, provide both a path to both the command dictionary file, and the compiled command dictionary
     * repository.
     *
     * @param cmdDictPath       command dictionary path. should end in ".xml"
     * @param repoPath          command dictionary repository. should end in the file separator, but does not need to.
     * @param commandProperties command properties
     * @param tracer            log tracer
     * @param init              auto initialize
     */
    public CommandTranslationTableFileRepository(final String cmdDictPath, final String repoPath,
                                                 final CommandProperties commandProperties, final Tracer tracer,
                                                 final boolean init) throws
                                                                     CommandFileParseException {
        this.cmdDictPath = cmdDictPath;
        this.tracer = tracer == null ? TraceManager.getTracer(Loggers.UPLINK) : tracer;
        compileXmlBinary = commandProperties.getCtsCommandDictCompilerPath();
        validateSchema = commandProperties.getCtsValidateSchema();
        this.repoPath = repoPath.endsWith(File.separator) ? repoPath : repoPath + File.separator;
        if (init) {
            init();
        }
    }

    @Override
    public void init() throws CommandFileParseException {
        if (compiler == null) {
            compiler = new CtsCxml(compileXmlBinary);
        }
        this.hash = hashCommandDictionary();
        final String basename = this.hash + "_" + compiler.getCtsVersion();
        this.paths = compiler.compileCommandDictionary(cmdDictPath, validateSchema, this.repoPath, basename);
    }

    /**
     * Set the CXML command dictionary compiler
     *
     * @param compiler
     */
    void setCompiler(ICtsCxml compiler) {
        this.compiler = compiler;
    }

    /**
     * Return the command dictionary path. This method will return the compiled command dictionary path if it was able
     * to be located or compiled. If not, it will return the XML file path.
     *
     * @return the command translation table path
     */
    @Override
    public String getForwardTranslationTablePath() {
        return paths.getForwardTranslationPath();
    }

    /**
     * Return the command dictionary path. This method will return the compiled command dictionary path if it was able
     * to be located or compiled. If not, it will return the XML file path.
     *
     * @return the command translation table path
     */
    @Override
    public String getReverseTranslationTablePath() {
        return paths.getReverseTranslationPath();
    }

    /**
     * Hashes the provided command dictionary.
     */
    private String hashCommandDictionary() throws CommandFileParseException {
        try (final DigestInputStream dis = new DigestInputStream(
                new BufferedInputStream(FileUtils.openInputStream(new File(cmdDictPath))),
                MessageDigest.getInstance("SHA-256"))) {
            final byte[] buf = new byte[1024];
            //noinspection StatementWithEmptyBody
            while (dis.read(buf) != -1) {
                // consume the entire file
            }

            // retrieve the digest
            final MessageDigest md     = dis.getMessageDigest();
            final byte[]        digest = md.digest();
            return DatatypeConverter.printHexBinary(digest);
        } catch (final IOException | NoSuchAlgorithmException e) {
            tracer.error(e);
            throw new CommandFileParseException(
                    "An error occurred retrieving a hash for the command dictionary file: " + cmdDictPath);
        }
    }
}
