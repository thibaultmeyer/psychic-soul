package core.server.command;

import core.server.session.Session;
import core.server.session.SessionAuthType;
import core.server.session.SessionStageLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Change the user's current state.
 * <pre>
 *     OpCode: state
 *     Args  : 1. new state (ie: actif:123456789) : timestamp is optional
 * </pre>
 * <p>
 * In this implementation, timestamp sent by user is ignored.
 * </p>
 *
 * @author Thibault Meyer
 * @version 1.1.0
 * @since 1.0.0
 */
public class StateCommandImpl implements Command {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(StateCommandImpl.class.getName());

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
        final String[] newState = payload[1].split(":");
        try {
            String urlDecodedData = java.net.URLDecoder.decode(newState[0], "UTF-8");
            if (urlDecodedData.length() > 20) {
                urlDecodedData = urlDecodedData.substring(0, urlDecodedData.length() > 20 ? 20 : urlDecodedData.length());
                usrSession.user.state = java.net.URLEncoder.encode(urlDecodedData, "UTF-8").replaceAll("\\+", "%20");
            } else {
                usrSession.user.state = newState[0];
            }
        } catch (UnsupportedEncodingException e) {
            usrSession.user.state = newState[0].substring(0, newState[0].length() > 20 ? 20 : newState[0].length());
        }
        usrSession.user.stateModifiedAt = System.currentTimeMillis() / 1000;

        LOG.debug(String.format("Client from %s (%s) change state from \"%s\" to \"%s\"",
                usrSession.network.address,
                usrSession.user.login,
                usrSession.user.state,
                usrSession.user.state));

        final List<Session> toSendNotification = globalFollowers.get(usrSession.user.login);
        if (toSendNotification != null) {
            final String notifData = String.format("%d:user:%d/%d:%s@%s:%s:%s:%s | %s",
                    usrSession.network.fd,
                    usrSession.user.trustLevelClient,
                    usrSession.user.trustLevelUser,
                    usrSession.user.login,
                    usrSession.network.ip,
                    usrSession.user.operatingSystem,
                    usrSession.user.location,
                    usrSession.user.group,
                    (payload[0].compareTo("state") != 0) ? payload[0] : String.format("state %s:%d",
                            usrSession.user.state,
                            usrSession.user.stateModifiedAt));
            for (Session s : toSendNotification) {
                final String notifPacket = String.format("%s %s\n",
                        s.authType == SessionAuthType.EXTERNAL_AUTHENTICATION ? "user_cmd" : "cmd",
                        notifData);
                s.addOutputDataAsChunk(notifPacket);
                s.network.registerWriteEvent();
                if (LOG.isTraceEnabled()) {
                    LOG.trace(String.format("Send notification to %s (%s) that user %s (%s) is now \"%s\"",
                            s.network.address,
                            s.user.login,
                            usrSession.network.address,
                            usrSession.user.login,
                            usrSession.user.state));
                }
            }
        }
    }
}
