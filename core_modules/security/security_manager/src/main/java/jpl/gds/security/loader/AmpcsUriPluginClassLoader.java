/*
 * Copyright 2006-2017. California Institute of Technology.
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
package jpl.gds.security.loader;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;

/**
 * This is a customized URL classloader designed to take JAR files and place
 * their contents on the classpath. It is intended to interact with a security
 * manager and security policy to prevent classes loaded through this
 * classloader from accessing protected resources (eg, the filesystem and
 * network).
 * 
 * This class should not be instantiated more than once, so it is implemented as
 * a singleton. If more than a single instance of the classloader was
 * instantiated, it could lead to loaded class duplication and inconsistencies.
 * Keep this in mind when modifying in the future.
 * 
 *
 * @see jpl.gds.security.permission.AMPCSPermission
 * @see jpl.gds.security.security_manager.AMPCSSecurityManager
 * @see jpl.gds.security.security_manager.SandboxSecurityPolicy
 *
 */

@Scope(value = "singleton")
@Service("SECURE_CLASS_LOADER")
public class AmpcsUriPluginClassLoader extends URLClassLoader {
    
    private final Map<String, Class<?>> classes = new ConcurrentHashMap<>();

    private final Set<Class<?>> allowedInterfaces;
    
    private static final boolean PERFORM_INTERFACE_CHECKING = false;
    
    private final Tracer                tracer;
    
    /**
     * Explicit constructor, provide parent classloader
     * 
     * @param parent
     *            parent classloader
     * @param log
     *            Tracer to log with
     */
    public AmpcsUriPluginClassLoader(final ClassLoader parent, final Tracer log) {
        super(new URL[0], parent);
        allowedInterfaces = new HashSet<>();
        tracer = log;
    }

    /**
     * Bean singleton constructor.
     * 
     * @param ctx
     *            The application context
     */
    public AmpcsUriPluginClassLoader(final ApplicationContext ctx) {
        super(new URL[0], ctx.getClassLoader());
        allowedInterfaces = new HashSet<>();
        tracer = TraceManager.getTracer(ctx, Loggers.UTIL);
    }


    /**
     * Iterate over directory contents, filtering for jar files to add to a URL
     * array.
     * 
     * @param pluginDirectory
     * @return
     */
    private URL[] getURLs(final URI pluginDirectory) {

        try {
            final List<Path> plugins = new ArrayList<>();
            final Path pluginPath = Paths.get(pluginDirectory);
            try (DirectoryStream<Path> stream = Files
                    .newDirectoryStream(pluginPath, "*.jar")) {
                for (final Path file : stream) {
                    tracer.debug("Found a plugin jar: " + file.toString());
                    plugins.add(file);
                }
            } catch (IOException | DirectoryIteratorException x) {
                tracer.error("An error has occurred while searching for plugin JAR files.", x);
            }

            final URL[] urls = new URL[plugins.size()];

            for (int i = 0; i < plugins.size(); i++) {
                urls[i] = plugins.get(i).toUri().toURL();
            }

            return urls;
        } catch (final MalformedURLException e) {
            return new URL[0];
        }
    }
    
    /**
     * Add a directory as a source of JAR files. The directory's contents will
     * be searched for JARs to add to the classloader's classpath.
     * 
     * This is a convenience method.
     * 
     * @param directory
     *            the directory to add
     */
    public void addDirectory(final File directory) {
        try {
            addDirectory(directory.toURI().toURL());
        } catch (final MalformedURLException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Adds a directory as a source of JAR files. The directory's contents will
     * be searched for JARs to add to the classloader's classpath.
     * 
     * This is a convenience method.
     * 
     * @param url
     *            the url to add to a directory
     */
    public void addDirectory(final URL url) {
        try {
            addDirectory(url.toURI());
        } catch (final URISyntaxException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Add a directory as a source of JAR files. The directory's contents will
     * be searched for JARs to add to this classloader's classpath.
     * 
     * @param pluginDirectory
     */
    synchronized void addDirectory(final URI pluginDirectory) {
        final URL[] urls = getURLs(pluginDirectory);
        if (urls.length > 0) {
            for (final URL url : urls) {
                tracer.info("Adding " + url.toString() + " to secured classpath.");
                addURL(url);
            }
        }
    }

    /**
     * This will attempt to load a class from this classloader. 
     * 1. Attempt to load from cache. 
     * 2. Attempt to load from parent. 
     * 3. Attempt to load from system. 
     * 4. Attempt to load from JARs (add to cache if found).
     */
    @Override
    protected Class<?> loadClass(final String className, final boolean resolveIt)
            throws ClassNotFoundException {

        // Synchronize on the class name. #getClassLoadingLock is built in to ClassLoaders.
        synchronized (getClassLoadingLock(className)) {
            Class<?> result;

            if (!className.matches("^[a-zA-Z].*")) {
                throw new ClassNotFoundException();
            }
            
            // Try to find class in cache
            result = classes.get(className);

            if (result == null) {
                tracer.debug("Didn't find class " + className + " in JAR cache.");
            } else {
                tracer.debug("Previously loaded " + className + " from JAR cache.");
                return result;
            }

            // Try to use parent classloader
            try {
                if (getParent() != null && getParent() != this) {
                    tracer.debug("Trying to find " + className + " with parent classloader " + getParent());
                    result = getParent().loadClass(className);
                    if (result != null) {
                        tracer.debug("Found class " + className + " with parent classloader " + getParent());
                        return result;
                    } else {
                        tracer.debug("Didn't find class " + className + " with parent classloader " + getParent());
                    }
                }
            } catch (NoClassDefFoundError | ClassNotFoundException e) {
                
            }
            
            // Try to use system classloader
            try {
                tracer.debug("Trying to find " + className + " with system classloader.");
                result = super.findSystemClass(className);
                if (result != null) {
                    tracer.debug("Found " + className + " with system classloader.");
                    return result;
                } else {
                    tracer.debug("Didn't find " + className + " with system classloader.");
                }
            } catch (NoClassDefFoundError | ClassNotFoundException e) {

            }

            // Don't check JARs for protected class
			if (className.startsWith("java.") || className.startsWith("sun.")) {
                throw new ClassNotFoundException();
            }

            // Check JARs for class
            result = findClass(className);
            
            if (result != null) {
                tracer.debug("Found " + className + " in JAR.");
                if (resolveIt) {
                    resolveClass(result);
                }
                
                if (PERFORM_INTERFACE_CHECKING) {
                    if (!checkInterfaces(result)) {
                        throw new AccessControlException("Can't load custom classes that don't extend our interfaces!");
                    }
                }
                
                classes.put(className, result);
                return result;
            } else {
                throw new ClassNotFoundException();
            }
        }
    }
    
    /**
     * Interface checking. Not yet implemented.
     * 
     * @param subject
     * @return
     */
    private boolean checkInterfaces(final Class<?> subject) {

        final Class<?>[] interfaces = subject.getInterfaces();
        for (final Class<?> iface : interfaces) {
            if (allowedInterfaces.contains(iface)) {
                tracer.debug("Class " + subject.getName() + " implements an allowed interface: " + iface.getName());
                
                return true;
            }
        }

        tracer.debug("Class " + subject.getName() + " does not implement an allowed interface.");
        return false;
    }

}
