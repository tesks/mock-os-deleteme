package jpl.gds.tc.api.output;

import java.io.File;
import java.util.List;

import jpl.gds.tc.api.ICommandFileLoad;
import jpl.gds.tc.api.IScmf;
import jpl.gds.tc.api.IUplinkResponse;
import jpl.gds.tc.api.command.ICommand;
import jpl.gds.tc.api.command.IFlightCommand;
import jpl.gds.tc.api.command.ISseCommand;
import jpl.gds.tc.api.exception.ScmfParseException;

/**
 * ICommandMessageUtility is the interface class for using the command message utility. This
 * utility is used by uplink output classes to quickly and easily publish messages for commands
 * and their subsequent statuses.
 * 
 *
 * MPCS-10813  - 04/09/19 - created
 */
public interface ICommandMessageUtility {

	/**
	 * Send command messages.
	 *
	 * @param commands       List of command
	 * @param uplinkResponse Response
	 * @param id             Event id
	 */
	public void sendCommandMessages(List<? extends ICommand> commands, IUplinkResponse uplinkResponse, int id);

	/**
	 * Send command message.
	 *
	 * @param command        Command
	 * @param uplinkResponse Response
	 * @param id             Event id
	 */
	public void sendCommandMessage(ICommand command, IUplinkResponse uplinkResponse, int id);

	/**
	 * Send file load messages.
	 *
	 * @param fileLoads      List of command file load
	 * @param uplinkResponse Response
	 * @param id             Event id
	 */
	public void sendFileLoadMessages(List<ICommandFileLoad> fileLoads, IUplinkResponse uplinkResponse, int id);

	/**
	 * Send file load message.
	 *
	 * @param fileLoad           Command file load
	 * @param uplinkResponse Response
	 * @param id             Event id
	 */
	public void sendFileLoadMessage(ICommandFileLoad fileLoad, IUplinkResponse uplinkResponse, int id);

	/**
	 * Send command message.
	 *
	 * @param command         Command
	 * @param uplinkResponse  Response
	 * @param transmitEventId Event id
	 */
	public void sendFlightCommandMessage(IFlightCommand command, IUplinkResponse uplinkResponse, int transmitEventId);

	/**
	 * Send SSE command message.
	 *
	 * @param command SSE  command
	 * @param id           Id
	 * @param isSuccessful True if OK
	 */
	public void sendSseCommandMessage(ISseCommand command, int id, boolean isSuccessful);

	/**
	 * Send SCMF message.
	 *
	 * @param scmf           SCMF
	 * @param uplinkResponse Response
	 * @param id             Id
	 */
	public void sendScmfMessage(File scmf, IUplinkResponse uplinkResponse, int id);

	/**
	 * Send raw uplink data message.
	 *
	 * @param dataFile        Data file
	 * @param uplinkResponse  Response
	 * @param isFaultInjected True if forced
	 * @param id              Id
	 */
	public void sendRawUplinkDataMessage(File dataFile, IUplinkResponse uplinkResponse, boolean isFaultInjected, int id);

	/**
	 * Send SCMF internal messages.
	 *
	 * @param scmf
	 *            SCMF
	 * @param uplinkResponse
	 *            Response
	 * @param id
	 *            transmit event id
	 *
	 * @throws ScmfParseException
	 *             On failure to parse
	 */
	public void sendScmfInternalMessages(IScmf scmf, IUplinkResponse uplinkResponse, int id) throws ScmfParseException;

	/**
	 * Extracts the contents of the provided SCMF and logs them.
	 * 
	 * @param scmf SCMF to parse for contents
	 * @param uplinkResponse object that may contains additional information of
	 *            the SCMF and its contents
	 */
	public void logScmfInternals(IScmf scmf, IUplinkResponse uplinkResponse);

}