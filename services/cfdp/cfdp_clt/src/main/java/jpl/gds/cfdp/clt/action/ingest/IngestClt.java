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

package jpl.gds.cfdp.clt.action.ingest;

import static jpl.gds.cfdp.common.action.EActionCommandType.INGEST;
import static jpl.gds.cfdp.common.action.ingest.EIngestSource.FILE;

import org.apache.commons.cli.ParseException;

import jpl.gds.cfdp.clt.action.AActionClt;
import jpl.gds.cfdp.common.action.ingest.IngestActionRequest;
import jpl.gds.shared.cli.app.BaseCommandOptions;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.filesystem.FileOption;
import jpl.gds.shared.config.GdsSystemProperties;

public class IngestClt extends AActionClt {

	private static final String FILE_SHORT = "f";
	private static final String FILE_LONG = "file";

	private final FileOption ingestFileNameOption = new FileOption(FILE_SHORT, FILE_LONG, "path", "file to ingest",
			true, true);

	private String ingestFileName;
	
	public IngestClt() {
		super(INGEST);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see jpl.gds.shared.cli.app.AbstractCommandLineApp#createOptions()
	 */
	@Override
	public BaseCommandOptions createOptions() {

		if (optionsCreated.get()) {
            return options;
        }

		final BaseCommandOptions options = super.createOptions();
		options.addOption(ingestFileNameOption);
		return options;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * jpl.gds.shared.cli.app.AbstractCommandLineApp#configure(jpl.gds.shared.cli.
	 * cmdline.ICommandLine)
	 */
	@Override
	public void configure(final ICommandLine commandLine) throws ParseException {
		super.configure(commandLine);
		ingestFileName = ingestFileNameOption.parse(commandLine);
	}

	@Override
	public void run() {
		final IngestActionRequest req = new IngestActionRequest();
        req.setRequesterId(GdsSystemProperties.getSystemUserName());
		req.setIngestSource(FILE);
		req.setIngestFileName(ingestFileName);
		postAndPrint(actionType.getRelativeUri(), req);
	}

}