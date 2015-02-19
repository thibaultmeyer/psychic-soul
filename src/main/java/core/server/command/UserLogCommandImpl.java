package core.server.command;

import core.Settings;
import core.crypto.MD5;
import core.network.DisconnectReason;
import core.server.database.DBPool;
import core.server.session.Session;
import core.server.session.SessionAuthType;
import core.server.session.SessionStageLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Authentication agent for user from external location.
 * <pre>
 *     OpCode: ext_user_log | user_log
 *     Args  : 1. Login
 *             2. MD5("&lt;random md5 hash&gt;-&lt;host client&gt;/&lt;port client&gt;&lt;pass socks&gt;")
 *             3. Client Name URL encoded (max 64 char)
 *             4. Location URL encoded (max 64 char)
 * </pre>
 *
 * @author Thibault Meyer
 * @since 1.0.0
 */
public class UserLogCommandImpl implements Command {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(UserLogCommandImpl.class.getName());


    /**
     * If enabled, this command is used to send notification to followers.
     */
    private Command changeState;

    /**
     * Default constructor.
     */
    public UserLogCommandImpl() {
        try {
            this.changeState = (Command) Class.forName("core.server.command.StateCommandImpl").newInstance();
        } catch (LinkageError | ClassNotFoundException | IllegalAccessException | InstantiationException ex) {
            LOG.warn(String.format("Can't load commands \"%s\"", "core.server.command.StateCommandImpl"), ex);
        }
    }

    /**
     * Get the minimal number of arguments needed. The command OpCode is
     * included in the number of arguments.
     *
     * @return The minimal number of arguments needed
     */
    public int getMinimalArgsCountNeeded() {
        return 5;
    }

    /**
     * Get the maximal number of arguments needed. The command OpCode is
     * included in the number of arguments. If this method return -1, the
     * command can take any number of arguments.
     *
     * @return The maximal number of arguments needed
     */
    public int getMaximalArgsCountNeeded() {
        return 5;
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
        return usrSession.stageLevel == SessionStageLevel.AUTHENTICATION_REQUESTED;
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
        String userGroup = null;
        payload[1] = payload[1].substring(0, payload[1].length() > 35 ? 35 : payload[1].length());
        final Connection dbConn = DBPool.getInstance().getSQLConnection();
        try {
            if (Settings.databaseBuiltIntFunction) {
                final PreparedStatement reqPrepStatement = dbConn.prepareStatement("SELECT `group` FROM `ns_account` WHERE `ns_account`.`username` LIKE ? AND MD5(CONCAT(?, '-', ?, '/', ?, `ns_account`.`password`)) LIKE ? AND `ns_account`.`is_active`=1 LIMIT 1");
                reqPrepStatement.setString(1, payload[1]);
                reqPrepStatement.setString(2, usrSession.hash);
                reqPrepStatement.setString(3, usrSession.network.ip);
                reqPrepStatement.setInt(4, usrSession.network.port);
                reqPrepStatement.setString(5, payload[2]);
                final ResultSet reqResult = reqPrepStatement.executeQuery();
                if (reqResult.next()) {
                    userGroup = reqResult.getString("group");
                }
                reqResult.close();
                reqPrepStatement.close();
            } else {
                final PreparedStatement reqPrepStatement = dbConn.prepareStatement("SELECT `password`, `group` FROM `ns_account` WHERE `ns_account`.`username` LIKE ? AND `ns_account`.`is_active`=1 LIMIT 1");
                reqPrepStatement.setString(1, payload[1]);
                final ResultSet reqResult = reqPrepStatement.executeQuery();
                if (reqResult.next()) {
                    final String hashMd5 = MD5.hash(String.format("%s-%s/%d%s",
                            usrSession.hash,
                            usrSession.network.ip,
                            usrSession.network.port,
                            reqResult.getString("password")));
                    if (hashMd5.compareTo(payload[2]) == 0) {
                        userGroup = reqResult.getString("group");
                    }
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

        if (userGroup != null) {
            if (connectedSessions.stream().filter(us -> us.user.login != null && us.user.login.compareTo(payload[1]) == 0).count() >= Settings.cfgMaxSessionPerLogin) {
                usrSession.outputBuffer.add("rep 737 -- too many sessions opened\n");
                usrSession.disconnectReason = DisconnectReason.TOO_MANY_SESSIONS;
            } else {
                usrSession.user.login = payload[1];
                usrSession.stageLevel = SessionStageLevel.AUTHENTICATED;
                usrSession.user.loginTime = System.currentTimeMillis() / 1000;
                usrSession.user.group = userGroup;
                try {
                    String urlDecodedData = java.net.URLDecoder.decode(payload[3], "UTF-8");
                    if (urlDecodedData.length() > 64) {
                        urlDecodedData = urlDecodedData.substring(0, urlDecodedData.length() > 64 ? 64 : urlDecodedData.length());
                        usrSession.user.location = java.net.URLEncoder.encode(urlDecodedData, "UTF-8").replaceAll("\\+", "%20");
                    } else {
                        usrSession.user.location = payload[3];
                    }
                } catch (UnsupportedEncodingException e) {
                    usrSession.user.location = payload[3].substring(0, payload[3].length() > 64 ? 64 : payload[3].length());
                }
                try {
                    String urlDecodedData = java.net.URLDecoder.decode(payload[4], "UTF-8");
                    if (urlDecodedData.length() > 64) {
                        urlDecodedData = urlDecodedData.substring(0, urlDecodedData.length() > 64 ? 64 : urlDecodedData.length());
                        usrSession.user.clientName = java.net.URLEncoder.encode(urlDecodedData, "UTF-8").replaceAll("\\+", "%20");
                    } else {
                        usrSession.user.clientName = payload[4];
                    }
                } catch (UnsupportedEncodingException e) {
                    usrSession.user.clientName = payload[4].substring(0, payload[4].length() > 64 ? 64 : payload[4].length());
                }
                usrSession.outputBuffer.add("rep 002 -- cmd end\n");
                LOG.debug("Client from {} authenticated as {}", usrSession.network.address, usrSession.user.login);
                if (this.changeState != null) {
                    this.changeState.execute((String[]) Arrays.asList("login", "connection").toArray(), usrSession, connectedSessions, globalFollowers);
                }
            }
        } else {
            usrSession.outputBuffer.add(String.format("rep 033 -- %s identification fail\n",
                    (usrSession.authType == SessionAuthType.EXTERNAL_AUTHENTICATION) ? "ext user" : "user"));
        }
    }
}
