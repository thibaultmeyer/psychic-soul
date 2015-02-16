package core.server.command;

import core.server.session.Session;
import core.server.session.SessionAuthType;
import core.server.session.SessionStageLevel;
import core.server.toolbox.ListLoginParser;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Get information about connected users.
 * <pre>
 *     OpCode: who
 *     Args  : 1. login or list of login
 * </pre>
 *
 * @author Thibault Meyer
 * @since 1.0.0
 */
public class WhoCommandImpl implements Command {

    /**
     * Get the minimal number of arguments needed. The command OpCode is
     * included in the number of arguments.
     *
     * @return The minimal number of arguments needed
     */
    public int getMinimalArgsCountNeeded() {
        return 2;
    }

    /**
     * Get the maximal number of arguments needed. The command OpCode is
     * included in the number of arguments. If this method return -1, the
     * command can take any number of arguments.
     *
     * @return The maximal number of arguments needed
     */
    public int getMaximalArgsCountNeeded() {
        return 2;
    }

    /**
     * Get the type of this command.
     *
     * @return The command type
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
     */
    @Override
    public void execute(final String[] payload, final Session usrSession, final Collection<Session> connectedSessions, final Map<String, List<Session>> globalFollowers) throws ArrayIndexOutOfBoundsException {
        final List<String> lstLoginToWho = ListLoginParser.parse(payload[1], connectedSessions);
        final long currentTimestamp = System.currentTimeMillis() / 1000;
        final String cmdHeader = String.format("%s %d:user:%d/%d:%s@%s:%s:%s:%s",
                (usrSession.authType == SessionAuthType.EXTERNAL_AUTHENTICATION) ? "user_cmd" : "cmd",
                usrSession.network.fd,
                usrSession.user.trustLevelClient,
                usrSession.user.trustLevelUser,
                usrSession.user.login,
                usrSession.network.ip,
                usrSession.user.operatingSystem,
                usrSession.user.location,
                usrSession.user.group);
        connectedSessions.stream().filter(s -> s.user.login != null && lstLoginToWho.contains(s.user.login)).forEach(s -> {
            final String cmdFormat = String.format("%s | who %d %s %s %d %d %d %d %s %s %s %s:%d %s\n",
                    cmdHeader,
                    s.network.fd,
                    s.user.login,
                    s.network.ip,
                    s.user.loginTime,
                    currentTimestamp,
                    s.user.trustLevelClient,
                    s.user.trustLevelUser,
                    s.user.operatingSystem,
                    s.user.location,
                    s.user.group,
                    s.user.state,
                    s.user.stateModifiedAt,
                    s.user.clientName);
            usrSession.outputBuffer.add(cmdFormat);
        });
        usrSession.outputBuffer.add(String.format("%s | who rep 002 -- cmd end\n", cmdHeader));
    }
}
