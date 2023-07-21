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
package jpl.gds.monitor.perspective;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.client.channel.IChannelUtilityDictionaryManager;
import jpl.gds.monitor.perspective.view.ChannelViewConfiguration;
import jpl.gds.perspective.DisplayConfiguration;
import jpl.gds.perspective.PerspectiveConfiguration;
import jpl.gds.perspective.config.PerspectiveProperties;
import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.perspective.view.IViewConfigurationContainer;
import jpl.gds.perspective.view.ViewReference;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;

/**
 * Can check perspectives, directories and files for missing or undefined 
 * channels.  Missing channels are channels that are defined in the dictionary 
 * but do not appear in the file/directory. Undefined channels are channels 
 * that are being used in the files but are not defined in the dictionary.
 * 
 */
public class PerspectiveDictionaryChecker {

    /**
     * Provides a logging and tracing capability
     */
    protected final Tracer                  trace;


    private final IChannelUtilityDictionaryManager chanTable;
    private boolean hasNullChanIds;

	private final ApplicationContext appContext;

    /**
     * Constructor: adds the channels specified in the default dictionary to 
     * the channel table
     */
    public PerspectiveDictionaryChecker(final ApplicationContext appContext) {
    	this.appContext = appContext;
        trace = TraceManager.getDefaultTracer(appContext);
    	this.chanTable = this.appContext.getBean(IChannelUtilityDictionaryManager.class);
        try {
            this.chanTable.loadFsw(true);
        } catch (final DictionaryException e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks all the fixed layout files in the given perspective path and
     * return any channels that are missing from the dictionary
     * 
     * @param perspectivePath the perspective that is to be searched
     * @return list of missing channels in a given perspective
     */
    public List<String> findMissingChannelsInPerspective(
            final String perspectivePath) {
        final List<String> result = new ArrayList<String>();
        final List<DisplayConfiguration> displays = new ArrayList<DisplayConfiguration>();

        if (perspectivePath != null) {
            final File dir = new File(perspectivePath);
            if (dir.isDirectory()) {
                final String pathName = dir.getPath();

                // if directory contains a perspective
                if (PerspectiveConfiguration.perspectiveExists(appContext.getBean(PerspectiveProperties.class), pathName)) {
                    // then add display configuration to list of
                    // displayConfigurations
                    final DisplayConfiguration displayConfig = new DisplayConfiguration();
                    displayConfig.load(appContext, pathName, "MonitorDisplay.xml");
                    displays.add(displayConfig);
                } else {
                    trace.error("The given perspective does not exist");
                    return null;
                }
            } else {
                trace.error("The given perspective path is not a directory");
                return null;
            }
        }

        if (displays != null) {
            for (final DisplayConfiguration display : displays) {
                result.addAll(findMissingChannels(display));
            }
        }
        return removeDuplicates(result);
    }

    /**
     * Finds all .xml files in the given directory and searches through them for
     * channels
     * 
     * @param directory all xml files in this directory will be searched for 
     *              missing channels
     * @return list of missing channels in the given directory
     */
    public List<String> findMissingChannelsInDir(final String directory) {
        final List<String> result = new ArrayList<String>();
        final List<IViewConfiguration> viewConfigurations = new ArrayList<IViewConfiguration>();

        final File dir = new File(directory);

        // filter the list of returned files: only return any files that end
        // with '.xml'
        final FilenameFilter filter = new FilenameFilter() {
            @Override
			public boolean accept(final File dir, final String name) {
                return name.endsWith(".xml");
            }
        };

        final String[] children = dir.list(filter);
        if (children != null) {
            for (int i = 0; i < children.length; i++) {
                // Get filename of file or directory
                final String filename = children[i];
                final ViewReference viewReference = new ViewReference();

                viewReference.setPath(directory + "/" + filename);
                try {
                    final IViewConfiguration viewConfig = viewReference
                            .parseAndThrow(appContext);
                    if (viewConfig != null) {
                        viewConfigurations.add(viewConfig);
                    }
                } catch (final NumberFormatException e) {
                   trace.error("The view configuration in " + filename
                            + " encountered a number format exception: "
                            + (e.getMessage() != null ? e.getMessage() : ""));
                } catch (final SAXException e) {
                    trace.error("The view configuration in " + filename
                            + " encountered an xml error: "
                            + (e.getMessage() != null ? e.getMessage() : ""));
                } catch (final Exception e) {
                    trace.error("The view configuration in " + filename
                            + " encountered an exception: "
                            + (e.getMessage() != null ? e.getMessage() : ""));
                }
            }
        }

        if (viewConfigurations != null) {
            for (final IViewConfiguration viewConfig : viewConfigurations) {
                result.addAll(findMissingChannels(viewConfig));
            }
        }
        return removeDuplicates(result);
    }

    /**
     * Finds all the missing channels in a file
     * 
     * @param filename
     *            method will return if it's not an xml file
     * @return list of missing channels in a given display configuration
     */
    public List<String> findMissingChannelsInFile(final String filename) {
        final List<String> result = new ArrayList<String>();

        if (!filename.endsWith(".xml")) {
            trace.error("The filename provided does not end with the extension \".xml\"");
            return null;
        }
        if (!new File(filename).exists()) {
            trace.error("The filename provided does not exist");
            return null;
        }

        final ViewReference viewReference = new ViewReference();
        viewReference.setPath(filename);
        try {
            final IViewConfiguration viewConfig = viewReference.parseAndThrow(appContext);

            if (viewConfig != null) {
                result.addAll(findMissingChannels(viewConfig));
            }

        } catch (final NumberFormatException e) {
            System.out.println("The view configuration in " + filename
                    + " encountered a number format exception. "
                    + (e.getMessage() != null ? e.getMessage() : ""));
        } catch (final SAXException e) {
            System.out.println("The view configuration in " + filename
                    + " encountered an xml error: "
                    + (e.getMessage() != null ? e.getMessage() : ""));
        } catch (final Exception e) {
            System.out.println("The view configuration in " + filename
                    + " encountered an exception: "
                    + (e.getMessage() != null ? e.getMessage() : ""));
        }

        return removeDuplicates(result);
    }

    /**
     * Finds all the missing channels in a display configuration
     * 
     * @param display display configuration that will be searched for missing 
     *              channels
     * @return list of missing channels in a given display configuration
     */
    public List<String> findMissingChannels(final DisplayConfiguration display) {
        final List<String> result = new ArrayList<String>();
        final List<IViewConfiguration> configs = display.getViewConfigs();
        if (configs != null) {
            for (final IViewConfiguration config : configs) {
                result.addAll(findMissingChannels(config));
            }
        }
        return removeDuplicates(result);
    }

    /**
     * Finds all the missing channels in a view configuration by comparing
     * channels in the config with those in the channel definition table
     * 
     * @param viewConfig view configuration that will be searched for missing channels
     * @return list of missing channels in a given view configuration
     */
    public List<String> findMissingChannels(final IViewConfiguration viewConfig) {

        final List<String> result = new ArrayList<String>();
        if (viewConfig instanceof ChannelViewConfiguration) {
            // check which channels are in the dictionary but not the viewConfig
            this.hasNullChanIds = ((ChannelViewConfiguration) viewConfig)
                    .containsNullChannelIds();
            final SortedSet<String> channelIds = this.chanTable.getChanIds();
            final List<String> chans = ((ChannelViewConfiguration) viewConfig)
                    .getReferencedChannelIds();

            boolean found = false;
            for (final String id : channelIds) {
                for (final String chanInView : chans) {
                    if (id.equals(chanInView)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    result.add(id);
                } else {
                    found = false;
                }
            }
        }
        if (viewConfig instanceof IViewConfigurationContainer) {
            final List<IViewConfiguration> configs = ((IViewConfigurationContainer) viewConfig)
                    .getViews();
            for (final IViewConfiguration child : configs) {
                result.addAll(findMissingChannels(child));
            }
        }
        return removeDuplicates(result);
    }

    /**
     * Checks all the fixed layout files in the given perspective path and
     * return any channels that are not in the current dictionary
     * 
     * @param perspectivePath perspective that will be searched for undefined channels
     * @return list of undefined channels in a given perspective
     */
    public List<String> findUndefinedChannelsInPerspective(
            final String perspectivePath) {
        final List<String> result = new ArrayList<String>();
        final List<DisplayConfiguration> displays = new ArrayList<DisplayConfiguration>();

        if (perspectivePath != null) {
            final File dir = new File(perspectivePath);
            if (dir.isDirectory()) {
                final String pathName = dir.getPath();

                // if directory contains a perspective
                if (PerspectiveConfiguration.perspectiveExists(appContext.getBean(PerspectiveProperties.class), pathName)) {
                    // then add display configuration to list of
                    // displayConfigurations
                    final DisplayConfiguration displayConfig = new DisplayConfiguration();
                    displayConfig.load(appContext, pathName, "MonitorDisplay.xml");
                    displays.add(displayConfig);
                } else {
                    trace.error("The given perspective does not exist");
                    return null;
                }
            } else {
                trace.error("The given perspective path is not a directory");
                return null;
            }
        }

        if (displays != null) {
            for (final DisplayConfiguration display : displays) {
                result.addAll(findUndefinedChannels(display));
            }
        }
        return removeDuplicates(result);
    }

    /**
     * Finds all .xml files in the given directory and searches through them for
     * channels
     * 
     * @param directory all xml files in this folder will be searched for undefined channels
     * @return list of undefined channels in the given directory
     */
    public List<String> findUndefinedChannels(final String directory) {
        final List<String> result = new ArrayList<String>();
        final List<IViewConfiguration> viewConfigurations = new ArrayList<IViewConfiguration>();

        final File dir = new File(directory);

        // filter the list of returned files: only return any files that end
        // with '.xml'
        final FilenameFilter filter = new FilenameFilter() {
            @Override
			public boolean accept(final File dir, final String name) {
                return name.endsWith(".xml");
            }
        };

        final String[] children = dir.list(filter);
        if (children != null) {
            for (int i = 0; i < children.length; i++) {
                // Get filename of file or directory
                final String filename = children[i];
                final ViewReference viewReference = new ViewReference();

                viewReference.setPath(directory + "/" + filename);
                try {
                    final IViewConfiguration viewConfig = viewReference
                            .parseAndThrow(appContext);
                    if (viewConfig != null) {
                        viewConfigurations.add(viewConfig);
                    }
                } catch (final NumberFormatException e) {
                    System.out.println("The view configuration in " + filename
                            + " encountered a number format exception: "
                            + (e.getMessage() != null ? e.getMessage() : ""));
                } catch (final SAXException e) {
                    System.out.println("The view configuration in " + filename
                            + " encountered an xml error: "
                            + (e.getMessage() != null ? e.getMessage() : ""));
                } catch (final Exception e) {
                    System.out.println("The view configuration in " + filename
                            + " encountered an exception: "
                            + (e.getMessage() != null ? e.getMessage() : ""));
                }
            }
        }

        if (viewConfigurations != null) {
            for (final IViewConfiguration viewConfig : viewConfigurations) {
                result.addAll(findUndefinedChannels(viewConfig));
            }
        }
        return removeDuplicates(result);
    }

    /**
     * Finds all the undefined channels in a file
     * 
     * @param filename
     *            method will return if it's not an xml file
     * @return list of undefined channels in a given display configuration
     */
    public List<String> findUndefinedChannelsInFile(final String filename) {
        final List<String> result = new ArrayList<String>();

        if (!filename.endsWith(".xml")) {
            trace.error("The filename provided does not end with the extension \".xml\"");
            return null;
        }
        if (!new File(filename).exists()) {
            trace.error("The filename provided does not exist");
            return null;
        }

        final ViewReference viewReference = new ViewReference();
        viewReference.setPath(filename);
        try {
            final IViewConfiguration viewConfig = viewReference.parseAndThrow(appContext);

            if (viewConfig != null) {
                result.addAll(findUndefinedChannels(viewConfig));
            }

        } catch (final NumberFormatException e) {
            trace.error("The view configuration in " + filename
                    + " encountered a number format exception. "
                    + (e.getMessage() != null ? e.getMessage() : ""));
        } catch (final SAXException e) {
            trace.error("The view configuration in " + filename
                    + " encountered an xml error: "
                    + (e.getMessage() != null ? e.getMessage() : ""));
        } catch (final Exception e) {
            trace.error("The view configuration in " + filename
                    + " encountered an exception: "
                    + (e.getMessage() != null ? e.getMessage() : ""));
        }

        return removeDuplicates(result);
    }

    /**
     * Finds all the undefined channels in a display configuration
     * 
     * @param display all view configurations in this display configuration 
     *              will be searched for undefined channels
     * @return list of undefined channels in a given display configuration
     */
    public List<String> findUndefinedChannels(final DisplayConfiguration display) {
        final List<String> result = new ArrayList<String>();
        final List<IViewConfiguration> configs = display.getViewConfigs();
        if (configs != null) {
            for (final IViewConfiguration config : configs) {
                result.addAll(findUndefinedChannels(config));
            }
        }
        return removeDuplicates(result);
    }

    /**
     * Finds all the undefined channels in a view configuration by comparing
     * channels in the config with those in the channel definition table
     * 
     * @param viewConfig channel view configurations within this view 
     *              configuration will be searched for undefined channels
     * @return list of undefined channels in a given view configuration
     */
    public List<String> findUndefinedChannels(final IViewConfiguration viewConfig) {
        final List<String> result = new ArrayList<String>();
        if (viewConfig instanceof ChannelViewConfiguration) {
            // check if any of the ChannelViewConfiguration don't have a channel
            // specified
            this.hasNullChanIds = ((ChannelViewConfiguration) viewConfig)
                    .containsNullChannelIds();

            final List<String> chans = ((ChannelViewConfiguration) viewConfig)
                    .getReferencedChannelIds();
            for (final String chan : chans) {
                if (this.chanTable.getDefinitionFromChannelId(chan) == null) {
                    result.add(chan);
                }
            }
        }
        if (viewConfig instanceof IViewConfigurationContainer) {
            final List<IViewConfiguration> configs = ((IViewConfigurationContainer) viewConfig)
                    .getViews();
            for (final IViewConfiguration child : configs) {
                result.addAll(findUndefinedChannels(child));
            }
        }

        return removeDuplicates(result);
    }

    /**
     * Removes all duplicate strings from a given list of strings
     * 
     * @param strings
     *            a list of string objects
     * @return a list of strings that does not contain duplicates
     */
    private List<String> removeDuplicates(final List<String> strings) {
        final List<String> result = new ArrayList<String>(strings.size());

        for (final String str : strings) {
            if (!result.contains(str)) {
                result.add(str);
            }
        }
        return result;
    }

    /**
     * Gets value of hasNullChannelIds flag
     * 
     * @return true if file(s)/directory being searched have null channel IDs, 
     *              false otherwise
     */
    public boolean hasNullChannelIds() {
        return this.hasNullChanIds;
    }

//    /**
//     * Checks the given directory for xml files that contain viewConfigs and
//     * prints a list of channels that are not defined in the dictionary
//     * 
//     * @param args
//     *            must contain a single string that is the name of a directory
//     */
//    public static void main(final String[] args) {
//        final PerspectiveDictionaryCheckerApp app = new PerspectiveDictionaryCheckerApp();
//        try {
//            final CommandLine commandLine = ReservedOptions.parseCommandLine(args, app);
//            app.configure(commandLine);
//
//        } catch (final ParseException e) {
//            Log4jTracer.getDefaultTracer().fatal(e.getMessage());

//            System.exit(1);
//        }
//        app.run();
//        System.exit(0);
//    }
}
