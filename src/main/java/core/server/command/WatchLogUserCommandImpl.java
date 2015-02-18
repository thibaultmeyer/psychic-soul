package core.server.command;

import core.server.database.DBPool;
import core.server.session.Session;
import core.server.session.SessionStageLevel;
import core.server.toolbox.ListLoginParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Set callback to listen events from some user.
 * <pre>
 *     OpCode: watch_log_user
 *     Args  : 1. login or list of login
 * </pre>
 *
 * @author Thibault Meyer
 * @since 1.0.0
 */
public class WatchLogUserCommandImpl implements Command {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(WatchLogUserCommandImpl.class.getName());

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
        final List<String> lstLoginListen = ListLoginParser.parseToLogin(payload[1], connectedSessions);
        final Connection dbConn = DBPool.getInstance().getSQLConnection();
        try {
            int i = 0;
            while (i < lstLoginListen.size()) {
                final PreparedStatement reqPrepStatement = dbConn.prepareStatement("SELECT 1 FROM `ns_account` WHERE `username` LIKE ?");
                reqPrepStatement.setString(1, lstLoginListen.get(i));
                final ResultSet reqResult = reqPrepStatement.executeQuery();
                if (reqResult.next()) {
                    i += 1;
                } else {
                    lstLoginListen.remove(i);
                }
                reqResult.close();
                reqPrepStatement.close();
            }
            dbConn.close();
        } catch (SQLException e) {
            LOG.warn("Something goes wrong with the database!", e);
            try {
                dbConn.close();
            } catch (SQLException ignore) {
            }
        }
        
        /*
        // DISABLE TO BE COMPLIANT WITH SOME CLIENTS
        // ENABLE TO BE RFC COMPLIANT
        for (final List<Session> followers : globalFollowers.values()) {
            if (followers.contains(usrSession)) {
                followers.remove(usrSession);
            }
        }
        */
        for (final String login : lstLoginListen) {
            globalFollowers.putIfAbsent(login, new ArrayList<Session>());
            if (!globalFollowers.get(login).contains(usrSession)) {
                globalFollowers.get(login).add(usrSession);
            }
        }

        if (LOG.isTraceEnabled()) {
            LOG.trace(String.format("Client from %s (%s) register events callback for %s",
                    usrSession.network.address,
                    usrSession.user.login,
                    lstLoginListen));
        }
    }
}
