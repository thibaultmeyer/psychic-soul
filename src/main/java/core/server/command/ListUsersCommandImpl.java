package core.server.command;

import core.server.session.Session;
import core.server.toolbox.ListLoginParser;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * List all registered users.
 * <pre>
 *     OpCode: list_users
 *     Args  : 1. (OPTIONAL) login or list of login
 * </pre>
 *
 * @author Thibault Meyer
 * @since 1.0.0
 */
public class ListUsersCommandImpl implements Command {

    /**
     * Output format
     */
    private static final String LIST_USERS_FORMAT = "%d %s %s %d %d %d %d %s %s %s %s:%d %s\n";

    /**
     * Get the minimal number of arguments needed. The command OpCode is
     * included in the number of arguments.
     *
     * @return The minimal number of arguments needed
     */
    public int getMinimalArgsCountNeeded() {
        return 1;
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
        return true;
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
        final List<String> filterLogin = (payload.length == 2) ? ListLoginParser.parse(payload[1], connectedSessions) : null;
        usrSession.outputBuffer.addAll(connectedSessions.stream()
                .filter(us -> us.user.login != null)
                .filter(us -> (filterLogin == null) || filterLogin.contains(us.user.login))
                .map(us -> String.format(ListUsersCommandImpl.LIST_USERS_FORMAT,
                        us.network.fd,
                        us.user.login,
                        us.network.ip,
                        us.user.loginTime,
                        us.user.stateModifiedAt,
                        us.user.trustLevelClient,
                        us.user.trustLevelUser,
                        us.user.operatingSystem,
                        us.user.location,
                        us.user.group,
                        us.user.state,
                        us.user.stateModifiedAt,
                        us.user.clientName))
                .collect(Collectors.toList()));
        usrSession.outputBuffer.add("rep 002 -- cmd end\n");
    }
}
