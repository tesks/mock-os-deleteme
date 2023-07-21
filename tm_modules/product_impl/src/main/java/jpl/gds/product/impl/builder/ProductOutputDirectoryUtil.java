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
package jpl.gds.product.impl.builder;

import jpl.gds.context.api.IGeneralContextInformation;
import jpl.gds.product.api.builder.IProductOutputDirectoryUtil;
import jpl.gds.product.api.config.IProductPropertiesProvider;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.config.ReleaseProperties;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.TimeProperties;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * {@code ProductOutputDirectoryUtil} is an implementation of {@code IProductOutputDirectoryUtil} and provides
 * utility functions related to product output directories.
 *
 * @since 8.1
 */
public class ProductOutputDirectoryUtil implements IProductOutputDirectoryUtil {

    /*
    4/9/19  It required CFDP Product Plugin to instantiate an IProductStorage
    object, with its internal threads firing off and everything. Extracting out the directory determination code
    properly
     */

    private final ApplicationContext appContext;

    // Making this a member field so that it'll be mockable by tests
    private final TimeProperties timeConfig;

    /**
     * Creates an instance of ProductOutputDirectoryUtil.
     *
     * @param appContext application context
     */
    public ProductOutputDirectoryUtil(final ApplicationContext appContext) {
        this.appContext = appContext;
        timeConfig = TimeProperties.getInstance();
    }

    @Override
    public String getProductOutputDir(final String productDirOverrideConfig, final IAccurateDateTime scet,
                                      final String apidName) {

        final StringBuilder dir = new StringBuilder(256);
        String testPath = null;

        if (productDirOverrideConfig != null) {
            dir.append(productDirOverrideConfig);
        } else if (appContext.getBean(IGeneralContextInformation.class).isOutputDirOverridden()) {
            dir.append(appContext.getBean(IGeneralContextInformation.class).getOutputDir());
        } else {
            // get the root directory
            dir.append(GdsSystemProperties.getSystemProperty(GdsSystemProperties.DIRECTORY_PROPERTY,
                    GdsSystemProperties.DEFAULT_ROOT_DIR));

            // get the test directory
            dir.append(File.separator + IGeneralContextInformation.ROOT_OUTPUT_SUBDIR);
        }

        if (scet != null) {

            final boolean useDoyDir = timeConfig.useDoyOutputDirectory();

            if (useDoyDir) {
                // add the year, month, day (GMT)
                final Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
                c.setTimeInMillis(scet.getTime());
                dir.append(File.separator
                        + GDR.fillStr(String.valueOf(c.get(Calendar.YEAR)), 4,
                        '0'));
                dir.append(File.separator
                        + GDR.fillStr(
                        String.valueOf(c.get(Calendar.DAY_OF_YEAR)), 3,
                        '0'));
            } else {
                // add the year, month, day (GMT)
                final Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
                c.setTimeInMillis(scet.getTime());
                dir.append(File.separator
                        + GDR.fillStr(String.valueOf(c.get(Calendar.YEAR)), 4,
                        '0'));
                // we have to add 1 to the month because Calendar.JANUARY equals
                // 0 and Calendar.DECEMBER equals 11
                dir.append(File.separator
                        + GDR.fillStr(
                        String.valueOf(c.get(Calendar.MONTH) + 1), 2,
                        '0'));
                dir.append(File.separator
                        + GDR.fillStr(
                        String.valueOf(c.get(Calendar.DAY_OF_MONTH)),
                        2, '0'));
            }

            // add the mpcs dir
            dir.append(File.separator + ReleaseProperties.getProductLine().toLowerCase()
                    + File.separator
                    + appContext.getBean(IProductPropertiesProvider.class).getStorageSubdir()
                    + File.separator + apidName);
        } else {
            dir.append(File.separator + ReleaseProperties.getProductLine().toLowerCase()
                    + File.separator
                    + appContext.getBean(IProductPropertiesProvider.class).getStorageSubdir()
                    + File.separator + IProductPropertiesProvider.UNDEFINED_PARTIAL_DIR);
        }

        testPath = dir.toString();

        return (testPath);
    }
    
}
