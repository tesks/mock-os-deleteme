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
package jpl.gds.eha.channel.api;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;



/**
 * Example of a simple derivation class that writes a file
 * This should not be allowed in a derivation
 * 
 *
 */
public class FileWriteDerivationExample extends DerivationAlgorithmBase {

    @Override
    public Map<String, IChannelValue> deriveChannels(final Map<String, IChannelValue> parentChannelValues)
            throws DerivationException {
        final Map<String, IChannelValue> result = new HashMap<>();

        // should not be allowed
        writeFile();

        return result;
    }

    // write to file
    private void writeFile() {
        final String s = "Hello World! ";
        final byte data[] = s.getBytes();
        final Path p = Paths.get("./file.txt");

        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(p, CREATE, APPEND))) {
            out.write(data, 0, data.length);
            System.out.println("Writting file");
        }
        catch (final IOException e) {
            e.printStackTrace();
        }
    }

}
