package core.server.command;

import core.server.session.Session;
import core.server.session.SessionStageLevel;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Ping.
 * <pre>
 *     OpCode: ping
 * </pre>
 *
 * @author Thibault Meyer
 * @since 1.0.0
 */
public class PingCommandImpl implements Command {

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
     * Check if this command can by executed at given stage level.
     *
     * @param usl The current user session stage level
     * @return {@code true} is the command can be executed, otherwise, {@code false}
     */
    @Override
    public boolean canExecute(final SessionStageLevel usl) {
        return usl == SessionStageLevel.AUTHENTICATED || usl == SessionStageLevel.AUTHENTICATED_EXTERNAL;
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
    }
}
