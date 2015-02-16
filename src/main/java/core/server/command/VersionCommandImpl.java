package core.server.command;

import core.server.session.Session;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Get the version of the server.
 * <pre>
 *     OpCode: version
 * </pre>
 *
 * @author Thibault Meyer
 * @since 1.1.0
 */
public class VersionCommandImpl implements Command {

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
        return 1;
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
        final InputStream fis = VersionCommandImpl.class.getResourceAsStream("/version.properties");
        try {
            final Properties properties = new Properties();
            properties.load(fis);
            usrSession.outputBuffer.add(String.format("%s %s %s\n",
                    properties.getOrDefault("MAVEN_PROJECT_NAME", "psychic-soul"),
                    properties.getOrDefault("MAVEN_PROJECT_VERSION", "0.0.0"),
                    properties.getOrDefault("MAVEN_PROJECT_BUILD", "1970-01-01T00:00:00CET")));
        } catch (IOException ignore) {
            usrSession.outputBuffer.add(String.format("psychic-soul 0.0.0 %s\n", "1970-01-01T00:00:00CET"));
        }
        try {
            fis.close();
        } catch (IOException ignore) {
        }
    }
}
