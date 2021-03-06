package core.server.command;

import core.server.session.Session;
import core.server.session.SessionAuthType;
import core.server.session.SessionStageLevel;
import core.server.toolbox.ListLoginParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Send message.
 * <pre>
 *     OpCode: msg_user
 *     Args  : 1. login or list of login
 *             2. msg (hard-coded)
 *             3. the message to send (URL encoded)
 * </pre>
 *
 * @author Thibault Meyer
 * @version 1.1.0
 * @since 1.0.0
 */
public class MsgUserCommandImpl implements Command {

    /**
     * Logger.
     *
     * @since 1.0.0
     */
    private static final Logger LOG = LoggerFactory.getLogger(MsgUserCommandImpl.class.getName());

    /**
     * Get the minimal number of arguments needed. The command OpCode is
     * included in the number of arguments.
     *
     * @return The minimal number of arguments needed
     * @since 1.0.0
     */
    public int getMinimalArgsCountNeeded() {
        return 3;
    }

    /**
     * Get the maximal number of arguments needed. The command OpCode is
     * included in the number of arguments. If this method return -1, the
     * command can take any number of arguments.
     *
     * @return The maximal number of arguments needed
     * @since 1.0.0
     */
    public int getMaximalArgsCountNeeded() {
        return -1;
    }

    /**
     * Get the type of this command.
     *
     * @return The command type
     * @since 1.0.0
     */
    @Override
    public CmdType getType() {
        return CmdType.COMMAND;
    }

    /**
     * Check if this command can by executed by this user session.
     *
     * @param usrSession The current user session
     * @return {@code true} is the command can be executed, otherwise, {@code false}
     * @since 1.1.0
     */
    @Override
    public boolean canExecute(final Session usrSession) {
        return usrSession.stageLevel == SessionStageLevel.AUTHENTICATED;
    }

    /**
     * Execute the command. The first entry (0) of the payload always
     * contain the command OpCode.
     *
     * @param payload           The command arguments
     * @param usrSession        The user session who call this command
     * @param connectedSessions The collection of connected sessions
     * @param globalFollowers   The map of all followers
     * @throws IndexOutOfBoundsException if payload don't contain enough arguments
     * @since 1.0.0
     */
    @Override
    public void execute(final String[] payload, final Session usrSession, final Collection<Session> connectedSessions, final Map<String, List<Session>> globalFollowers) throws ArrayIndexOutOfBoundsException {
        final List<Session> lstSessDest = ListLoginParser.parseToSession(payload[1], connectedSessions);
        final String cmdHeader = String.format("%s %d:user:%d/%d:%s@%s:%s:%s:ext",
                (usrSession.authType == SessionAuthType.EXTERNAL_AUTHENTICATION) ? "user_cmd" : "cmd",
                usrSession.network.fd,
                usrSession.user.trustLevelClient,
                usrSession.user.trustLevelUser,
                usrSession.user.login,
                usrSession.network.ip,
                usrSession.user.operatingSystem,
                usrSession.user.location);
        for (final Session s : lstSessDest) {
            if (payload[2].compareToIgnoreCase("msg") == 0 && payload.length >= 3) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace(String.format("Client from %s (%s) send message to %s (%s): %s",
                            usrSession.network.address,
                            usrSession.user.login,
                            s.network.address,
                            s.user.login,
                            payload[3]
                    ));
                }
                final String cmdFormat = String.format("%s | msg %s\n", cmdHeader, payload[3]);
                s.addOutputDataAsChunk(cmdFormat);
            } else {
                String data = "";
                for (int i = 3; i < payload.length; ++i) {
                    data = " " + payload[i];
                }
                final String cmdFormat = String.format("%s | %s%s\n", cmdHeader, payload[2], data);
                s.addOutputDataAsChunk(cmdFormat);
            }
            s.network.registerWriteEvent();
        }
    }
}
