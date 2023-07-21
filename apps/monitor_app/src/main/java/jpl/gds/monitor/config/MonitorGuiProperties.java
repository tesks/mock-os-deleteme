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
package jpl.gds.monitor.config;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import jpl.gds.context.api.TopicNameToken;
import jpl.gds.shared.config.GdsHierarchicalProperties;
import jpl.gds.shared.spring.context.flag.SseContextFlag;

/**
 * MonitorGuiProperties manages properties related to the monitor gui
 */
public class MonitorGuiProperties extends GdsHierarchicalProperties {
    
    private static final String PROPERTY_FILE = "monitor_gui.properties";
    
    private static final String PROPERTY_PREFIX = "monitorGui.";
        
    private static final String LAD_BLOCK = PROPERTY_PREFIX + "lad.";
    private static final String INTERNAL_LAD_BLOCK = PROPERTY_PREFIX + "internal.lad.";
    private static final String LAD_DEPTH_PROPERTY = LAD_BLOCK + "depth";
    private static final String LAD_FETCH_INTERVAL_PROPERTY = INTERNAL_LAD_BLOCK + "fetchRetryInterval";
    
    private static final String CHANNEL_BLOCK = PROPERTY_PREFIX + "channel.";
    
    private static final String MAX_CHANNELS = "maxChannels";
    private static final String CHANNEL_PLOT_BLOCK = CHANNEL_BLOCK + "plot.";
    private static final String CHANNEL_PLOT_MAX_CHANS_PROPERTY = CHANNEL_PLOT_BLOCK + MAX_CHANNELS;
    private static final String CHANNEL_PLOT_MAX_LINE_POINTS_PROPERTY = CHANNEL_PLOT_BLOCK + "maxLinePoints";
    private static final String CHANNEL_PLOT_MAX_SHAPE_POINTS_PROPERTY = CHANNEL_PLOT_BLOCK + "maxShapePoints";
    private static final String CHANNEL_PLOT_ENABLE_PROPERTY = PROPERTY_PREFIX + "internal.channel.plot.enable";
    
    private static final String CHANNEL_LIST_BLOCK = CHANNEL_BLOCK + "list.";
    private static final String CHANNEL_LIST_MAX_CHANS_PROPERTY = CHANNEL_LIST_BLOCK + MAX_CHANNELS;
    
    private static final String CHANNEL_ALARM_BLOCK = CHANNEL_BLOCK + "alarm.";
    private static final String CHANNEL_ALARM_MAX_CHANS_PROPERTY = CHANNEL_ALARM_BLOCK + MAX_CHANNELS;
    
    private static final String CHANNEL_HISTORY_BLOCK = CHANNEL_BLOCK + "history.";
    private static final String CHANNEL_HISTORY_MAX_PROPERTY = CHANNEL_HISTORY_BLOCK + "defaultMaxQueryRecords";
    
    private static final String PRODUCT_VIEWER_BLOCK = PROPERTY_PREFIX + "product.viewer.";
    private static final String PRODUCT_VIEWER_MAX_LINES_PROPERTY = PRODUCT_VIEWER_BLOCK + "maxLines";
    private static final String PRODUCT_VIEWER_SCRIPT_PROPERTY = PROPERTY_PREFIX + "internal.product.viewer.script";
    
    private static final String GRID_BLOCK = PROPERTY_PREFIX + "grid.";
    private static final String GRID_ALLOWED_VIEWS_PROPERTY = GRID_BLOCK + "allowedViewTypes";
    private static final String GRID_IMAGE_NAMES_PROPERTY = GRID_BLOCK + "viewImages";
    
    private static final String LIST_BLOCK = PROPERTY_PREFIX + "internal.lists.";
    private static final String LIST_QUEUE_SCALE_PROPERTY = LIST_BLOCK + "queueScaleFactor";
    private static final String LIST_BATCH_SIZE_PROPERTY = LIST_BLOCK + "flushBatchSize";
       
    private static final String DEFAULT_VIEWSET_PROPERTY = PROPERTY_PREFIX + "defaultViewSet";
    private static final String DEFAULT_FLUSH_INTERVAL_PROPERTY = PROPERTY_PREFIX + "defaultFlushInterval";
    private static final String TOPICS_PROPERTY = PROPERTY_PREFIX + "topics";   
    private static final String ALLOWED_VIEWS_PROPERTY = PROPERTY_PREFIX + "allowedViewTypes";
    
    private static final String QUEUE_SIZE_BLOCK = PROPERTY_PREFIX +"internal.subscriber.queueSize.";
    private static final String DEFAULT_QUEUE_SIZE_PROPERTY = QUEUE_SIZE_BLOCK + "default";
    private static final String EVR_QUEUE_SIZE_PROPERTY = QUEUE_SIZE_BLOCK + TopicNameToken.APPLICATION_EVR.getTopicNameComponent();
    private static final String EHA_QUEUE_SIZE_PROPERTY = QUEUE_SIZE_BLOCK + TopicNameToken.APPLICATION_EHA.getTopicNameComponent();
    private static final String PRODUCT_QUEUE_SIZE_PROPERTY = QUEUE_SIZE_BLOCK + TopicNameToken.APPLICATION_PRODUCT.getTopicNameComponent();
    
    private static final long DEFAULT_LAD_FETCH_INTERVAL = 4000;
    private static final int DEFAULT_LAD_DEPTH = 3;
    private static final int DEFAULT_MAX_CHANNELS = 20;
    private static final int DEFAULT_MAX_POINTS = 5000;
    private static final int DEFAULT_MAX_HISTORY = 500;
    private static final int DEFAULT_MAX_PRODUCT_VIEWER = 5000;
    private static final String DEFAULT_PRODUCT_VIEWER_SCRIPT = "chill_dp_view";
    private static final String LIST_DELIM = ",";
    private static final String DEFAULT_ALLOWED_VIEWS = "Status Messages";
    private static final String DEFAULT_VIEW_IMAGES = "schedule.gif";
    private static final int DEFAULT_LIST_QUEUE_SCALE = 2;
    private static final int DEFAULT_LIST_BATCH_SIZE = 50;
    private static final String DEFAULT_VIEWSET = "default_monitor_view_import";
    private static final int DEFAULT_FLUSH_INTERVAL = 5;
    private static final String DEFAULT_TOPICS = "application,sse";
    
    /**
     * Test constructor
     */
    public MonitorGuiProperties() {
        this(new SseContextFlag());

    }

    /**
     * Default Monitor GUI properties
     * 
     * @param sseFlag
     *            The SSE context flag
     */
    public MonitorGuiProperties(final SseContextFlag sseFlag) {
        super(PROPERTY_FILE, sseFlag);
        
    }
    
    /**
     * Gets the time in milliseconds between attempts to fetch LAD in the monitor.
     * @return interval
     */
    public long getLadFetchInterval() {
        return getLongProperty(LAD_FETCH_INTERVAL_PROPERTY, DEFAULT_LAD_FETCH_INTERVAL);    
    }
    
    /**
     * Gets the depth (in number of samples) of the internal monitor channel LAD.
     * @return depth
     */
    public int getLadDepth() {
        return getIntProperty(LAD_DEPTH_PROPERTY, DEFAULT_LAD_DEPTH);    
    }
    
    /**
     * Gets the maximum number of traces allowed on a single channel plot.
     * 
     * @return number of traces
     */
    public int getPlotMaxChannels() {
        return getIntProperty(CHANNEL_PLOT_MAX_CHANS_PROPERTY, DEFAULT_MAX_CHANNELS);
    }
    
    /**
     * Gets the maximum number of points allowed on a single channel plot when drawing
     * only line.
     * 
     * @return number of points
     */
    public int getPlotMaxLinePoints() {
        return getIntProperty(CHANNEL_PLOT_MAX_LINE_POINTS_PROPERTY, DEFAULT_MAX_POINTS);
    }
    
    /**
     * Gets the maximum number of points allowed on a single channel plot when drawing
     * point shapes.
     * 
     * @return number of points
     */
    public int getPlotMaxShapePoints() {
        return getIntProperty(CHANNEL_PLOT_MAX_SHAPE_POINTS_PROPERTY, DEFAULT_MAX_POINTS);
    }
    
    /**
     * Gets the flag indicating whether plot rendering is enabled.
     * @return true if plot rendering enabled, false if not
     */
    public boolean isPlottingEnabled() {
        return getBooleanProperty(CHANNEL_PLOT_ENABLE_PROPERTY, true);
    }
    
    /**
     * Gets the maximum number of channels allowed on a single channel list view.
     * 
     * @return number of channels
     */
    public int getListMaxChannels() {
        return getIntProperty(CHANNEL_LIST_MAX_CHANS_PROPERTY, DEFAULT_MAX_CHANNELS);
    }
    
    
    /**
     * Gets the maximum number of channels allowed on a single channel alarm view.
     * 
     * @return number of channels
     */
    public int getAlarmMaxChannels() {
        return getIntProperty(CHANNEL_ALARM_MAX_CHANS_PROPERTY, DEFAULT_MAX_CHANNELS);
    }
    
    /**
     * Gets the default maximum for number of records queried from the global LAD when
     * displaying channel history.
     * 
     * @return number of records
     */
    public int getHistoryMaxQueryRecords() {
        return getIntProperty(CHANNEL_HISTORY_MAX_PROPERTY, DEFAULT_MAX_HISTORY);
    }
    
    /**
     * Gets the default for maximum number of text lines to display in the product text
     * viewer.
     * 
     * @return maximum number of lines
     */
    public int getProductMaxViewLines() {
        return getIntProperty(PRODUCT_VIEWER_MAX_LINES_PROPERTY, DEFAULT_MAX_PRODUCT_VIEWER);
    }
    
    /**
     * Gets the product viewer script name.
     * 
     * @return product viewer script name relative to $CHILL_GDS
     */
    public String getProductViewScriptName() {
        return getProperty(PRODUCT_VIEWER_SCRIPT_PROPERTY, DEFAULT_PRODUCT_VIEWER_SCRIPT);       
    }
    
    /**
     * Gets the list of view types allowed in a monitor grid view.
     * 
     * @return list of view type names
     */
    public List<String> getGridAllowedViewTypes() {
        return getListProperty(GRID_ALLOWED_VIEWS_PROPERTY, DEFAULT_ALLOWED_VIEWS, LIST_DELIM);
    }
    
    /**
     * Gets the list of image file names that correspond to the allowed grid view types. Image
     * files must be found on the class path.
     * 
     * @return list of image file names
     */
    public List<String> getGridImageNames() {
        return getListProperty(GRID_IMAGE_NAMES_PROPERTY, DEFAULT_VIEW_IMAGES, LIST_DELIM);
    }
    
    /**
     * Gets the scale factor for message queues within scrolling list views.  This factor 
     * is multiplied by the max row setting for the view to determine the internal message
     * queue size.
     * 
     * @return scale factor
     */
    public int getListQueueScaleFactor() {
        return getIntProperty(LIST_QUEUE_SCALE_PROPERTY, DEFAULT_LIST_QUEUE_SCALE);
    }
    
    /**
     * Currently used only by the product status view. Controls how many messages updates
     * are flushed to the display at one time.
     * 
     * @return message count
     */
    public int getListFlushBatchSize() {
        return getIntProperty(LIST_BATCH_SIZE_PROPERTY, DEFAULT_LIST_BATCH_SIZE);
    }
    
    /**
     * Gets the list of default view sets. Each is the base name of an XML file (no .xml)
     * extension that contains the definitions of one or more views to be included in the
     * default chill_monitor perspective.
     * 
     * @return list of view set names
     */
    public List<String> getDefaultViewSet() {
        return getListProperty(DEFAULT_VIEWSET_PROPERTY, DEFAULT_VIEWSET, LIST_DELIM);       
    }
    
    /**
     * Gets the default flush interval in milliseconds. This is the default interval at 
     * which data will be flushed to displays.
     * 
     * @return flush interval
     */
    public int getDefaultFlushInterval() {
        return getIntProperty(DEFAULT_FLUSH_INTERVAL_PROPERTY, DEFAULT_FLUSH_INTERVAL);
    }
    
    /**
     * Gets the list of allowed view types for chill_monitor.
     * 
     * @return list of view type names
     */
    public List<String> getAllowedViewTypes() {
        return getListProperty(ALLOWED_VIEWS_PROPERTY, DEFAULT_ALLOWED_VIEWS, LIST_DELIM);
    }
    
    /**
     * Gets the list of default topic name tokens for chill_monitor to subscribe to.
     * 
     * @return list of topic name tokens
     */
    public List<TopicNameToken> getTopics() {
        final List<String> topicTokenStrs = getListProperty(TOPICS_PROPERTY, DEFAULT_TOPICS, LIST_DELIM);
        final Set<TopicNameToken> nameVals = new TreeSet<>();
        for (final String tokenStr : topicTokenStrs) {
            try {
                final TopicNameToken name = TopicNameToken.valueOf(tokenStr);
                nameVals.add(name);
            } catch (final IllegalArgumentException e) {
                log.error("Illegal topic token name " + topicTokenStrs + 
                        " for configuration property "+ TOPICS_PROPERTY + ". Value will be ignored.");
            }
        }
        
        return new LinkedList<>(nameVals);
    }

    @Override
    public String getPropertyPrefix() {
        return PROPERTY_PREFIX;
    }
    
    /**
     * Gets the default subscriber queue size.
     * 
     * @return queue size as number of messages
     */
    public int getDefaultSubscriberQueueSize() {
        return getIntProperty(DEFAULT_QUEUE_SIZE_PROPERTY, 1024);       
    }
    
    /**
     * Gets the EVR subscriber queue size.
     * 
     * @return queue size as number of messages
     */
    public int getEvrSubscriberQueueSize() {
        return getIntProperty(EVR_QUEUE_SIZE_PROPERTY, getDefaultSubscriberQueueSize());       
    }
    
    /**
     * Gets the EHA subscriber queue size.
     * 
     * @return queue size as number of messages
     */
    public int getEhaSubscriberQueueSize() {
        return getIntProperty(EHA_QUEUE_SIZE_PROPERTY, getDefaultSubscriberQueueSize());       
    }
    
    /**
     * Gets the PRODUCT subscriber queue size.
     * 
     * @return queue size as number of messages
     */
    public int getProductSubscriberQueueSize() {
        return getIntProperty(PRODUCT_QUEUE_SIZE_PROPERTY, getDefaultSubscriberQueueSize());       
    }
}
