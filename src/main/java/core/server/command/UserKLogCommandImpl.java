package core.server.command;

import core.Settings;
import core.network.DisconnectReason;
import core.server.database.DBPool;
import core.server.session.Session;
import core.server.session.SessionAuthType;
import core.server.session.SessionStageLevel;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.security.auth.callback.*;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.PrivilegedAction;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Authentication agent for user from external location.
 * <pre>
 *     OpCode: ext_user_klog | user_klog
 *     Args  : 1. Token Kerberos Base64 encoded
 *             2. System URL encoded (max 64 char)
 *             3. Location URL encoded (max 64 char)
 *             4. Group (max 20 char)
 *             5. Client Name URL encoded (max 64 char)
 * </pre>
 *
 * @author Thibault Meyer
 * @since 1.2.0
 */
public class UserKLogCommandImpl implements Command {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(UserKLogCommandImpl.class.getName());


    /**
     * If enabled, this command is used to send notification to followers.
     */
    private Command changeState;

    /**
     * Default constructor.
     */
    public UserKLogCommandImpl() {
        try {
            this.changeState = (Command) Class.forName("core.server.command.StateCommandImpl").newInstance();
        } catch (LinkageError | ClassNotFoundException | IllegalAccessException | InstantiationException ex) {
            LOG.warn(String.format("Can't load commands \"%s\"", "core.server.command.StateCommandImpl"), ex);
        }

        if (Settings.krb5Realm != null && Settings.krb5Kdc != null && Settings.krb5JaasFile != null) {
            System.setProperty("sun.security.krb5.debug", Settings.krb5Debug ? "true" : "false");
            System.setProperty("java.security.krb5.realm", Settings.krb5Realm);
            System.setProperty("java.security.krb5.kdc", Settings.krb5Kdc);
            System.setProperty("java.security.auth.login.config", Settings.krb5JaasFile);
            System.setProperty("javax.security.auth.useSubjectCredsOnly", "true");
        }
    }

    /**
     * Get the minimal number of arguments needed. The command OpCode is
     * included in the number of arguments.
     *
     * @return The minimal number of arguments needed
     */
    public int getMinimalArgsCountNeeded() {
        return 6;
    }

    /**
     * Get the maximal number of arguments needed. The command OpCode is
     * included in the number of arguments. If this method return -1, the
     * command can take any number of arguments.
     *
     * @return The maximal number of arguments needed
     */
    public int getMaximalArgsCountNeeded() {
        return 6;
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
        if (payload[0].compareTo((usrSession.authType == SessionAuthType.INTERNAL_AUTHENTICATION) ? "user_log" : "ext_user_log") != 0) {
            usrSession.outputBuffer.add("rep 403 -- forbidden\n");
        } else {
            final String userName = this.__verifyKerberosTicket(Base64.getDecoder().decode(payload[1]));
            if (userName != null) {
                boolean canLogin = false;
                final Connection dbConn = DBPool.getInstance().getSQLConnection();
                try {
                    if (Settings.databaseBuiltIntFunction) {
                        final PreparedStatement reqPrepStatement = dbConn.prepareStatement("SELECT 1 FROM `ns_account` WHERE `ns_account`.`username` LIKE ? AND `ns_account`.`is_active`=1 LIMIT 1");
                        reqPrepStatement.setString(1, userName);
                        final ResultSet reqResult = reqPrepStatement.executeQuery();
                        if (reqResult.next()) {
                            canLogin = true;
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

                if (canLogin) {
                    if (connectedSessions.stream().filter(us -> us.user.login != null && us.user.login.compareTo(userName) == 0).count() >= Settings.cfgMaxSessionPerLogin) {
                        usrSession.outputBuffer.add("rep 737 -- too many sessions opened\n");
                        usrSession.disconnectReason = DisconnectReason.TOO_MANY_SESSIONS;
                    } else {
                        usrSession.user.login = userName;
                        usrSession.stageLevel = SessionStageLevel.AUTHENTICATED;
                        usrSession.user.loginTime = System.currentTimeMillis() / 1000;
                        try {
                            String urlDecodedData = java.net.URLDecoder.decode(payload[4], "UTF-8");
                            if (urlDecodedData.length() > 64) {
                                urlDecodedData = urlDecodedData.substring(0, urlDecodedData.length() > 64 ? 64 : urlDecodedData.length());
                                usrSession.user.group = java.net.URLEncoder.encode(urlDecodedData, "UTF-8").replaceAll("\\+", "%20");
                            } else if (urlDecodedData.length() == 0) {
                                usrSession.user.group = usrSession.authType == SessionAuthType.EXTERNAL_AUTHENTICATION ? "ext" : "int";
                            } else {
                                usrSession.user.group = payload[4];
                            }
                        } catch (UnsupportedEncodingException e) {
                            usrSession.user.group = payload[4].substring(0, payload[4].length() > 64 ? 64 : payload[4].length());
                        }
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
                            String urlDecodedData = java.net.URLDecoder.decode(payload[5], "UTF-8");
                            if (urlDecodedData.length() > 64) {
                                urlDecodedData = urlDecodedData.substring(0, urlDecodedData.length() > 64 ? 64 : urlDecodedData.length());
                                usrSession.user.clientName = java.net.URLEncoder.encode(urlDecodedData, "UTF-8").replaceAll("\\+", "%20");
                            } else {
                                usrSession.user.clientName = payload[5];
                            }
                        } catch (UnsupportedEncodingException e) {
                            usrSession.user.clientName = payload[5].substring(0, payload[5].length() > 64 ? 64 : payload[5].length());
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
            } else {
                usrSession.outputBuffer.add(String.format("rep 033 -- %s identification fail\n",
                        (usrSession.authType == SessionAuthType.EXTERNAL_AUTHENTICATION) ? "ext user" : "user"));
            }
        }
    }

    /**
     * Verify a kerberos ticket and return the owner username.
     *
     * @param krbTicket The kerberos ticket to verify
     * @return The ticket's owner username
     * @since 1.2.0
     */
    private String __verifyKerberosTicket(final byte[] krbTicket) {
        try {
            final LoginContext loginCtx = new LoginContext("Server", new LoginCallbackHandler(Settings.krb5Password));
            loginCtx.login();

            return Subject.doAs(loginCtx.getSubject(), new PrivilegedAction<String>() {
                public String run() {
                    try {
                        GSSManager manager = GSSManager.getInstance();
                        GSSContext context = manager.createContext((GSSCredential) null);
                        context.acceptSecContext(krbTicket, 0, krbTicket.length);
                        return context.getSrcName().toString();
                    } catch (GSSException e) {
                        LOG.warn("Can't verify ticket", e);
                        return null;
                    }
                }
            });
        } catch (LoginException e) {
            LOG.warn("Can't authenticate against the KDC", e);
        }
        return null;
    }

    /**
     * Password callback handler for resolving password/usernames
     * for a JAAS login.
     *
     * @author Thibault Meyer
     * @since 1.2.0
     */
    private class LoginCallbackHandler implements CallbackHandler {

        private String password;
        private String username;

        public LoginCallbackHandler(String password) {
            super();
            this.username = null;
            this.password = password;
        }

        /**
         * Handles the callbacks, and sets the user/password detail.
         *
         * @param callbacks the callbacks to handle
         * @throws IOException if an input or output error occurs.
         */
        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            for (Callback callback : callbacks) {
                if (callback instanceof NameCallback && username != null) {
                    NameCallback nc = (NameCallback) callback;
                    nc.setName(username);
                } else if (callback instanceof PasswordCallback) {
                    PasswordCallback pc = (PasswordCallback) callback;
                    pc.setPassword(password.toCharArray());
                }
            }
        }
    }
}
