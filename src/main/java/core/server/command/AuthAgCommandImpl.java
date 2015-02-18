package core.server.command;

import core.server.session.Session;
import core.server.session.SessionAuthType;
import core.server.session.SessionStageLevel;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Select the authentication agent to use.
 * <pre>
 *     OpCode: auth_ag
 *     Args  : 1. Authentication type : "ext_user" (external) or "user" (internal)
 *             2. none
 *             3. -
 * </pre>
 *
 * @author Thibault Meyer
 * @since 1.0.0
 */
public class AuthAgCommandImpl implements Command {

    /**
     * Get the minimal number of arguments needed. The command OpCode is
     * included in the number of arguments.
     *
     * @return The minimal number of arguments needed
     */
    public int getMinimalArgsCountNeeded() {
        return 4;
    }

    /**
     * Get the maximal number of arguments needed. The command OpCode is
     * included in the number of arguments. If this method return -1, the
     * command can take any number of arguments.
     *
     * @return The maximal number of arguments needed
     */
    public int getMaximalArgsCountNeeded() {
        return 4;
    }

    /**
     * Get the type of this command.
     *
     * @return The command type
     */
    @Override
    public CmdType getType() {
        return CmdType.AUTHENTICATION;
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
        return usrSession.stageLevel == SessionStageLevel.NOT_AUTHENTICATED;
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
        if (payload[1].compareTo("ext_user") == 0) {
            usrSession.stageLevel = SessionStageLevel.AUTHENTICATION_REQUESTED;
            usrSession.authType = SessionAuthType.EXTERNAL_AUTHENTICATION;
            usrSession.user.trustLevelClient = 3;
            usrSession.user.trustLevelUser = 1;
            usrSession.outputBuffer.add("rep 002 -- cmd end\n");
        } else if (payload[1].compareTo("user") == 0) {
            if (payload[2].compareTo("none") != 0) {
                usrSession.stageLevel = SessionStageLevel.AUTHENTICATION_REQUESTED;
                usrSession.authType = SessionAuthType.INTERNAL_AUTHENTICATION;
                usrSession.user.trustLevelClient = 1;
                usrSession.user.trustLevelUser = 3;
                usrSession.outputBuffer.add("rep 002 -- cmd end\n");
            } else {
                usrSession.outputBuffer.add("rep 005 -- no such auth\n");
            }
        } else {
            usrSession.outputBuffer.add("rep 004 -- no such agent");
        }
    }
}
