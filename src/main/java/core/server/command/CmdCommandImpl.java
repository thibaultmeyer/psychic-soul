package core.server.command;

import core.Settings;
import core.server.session.Session;
import core.server.session.SessionAuthType;
import core.server.session.SessionStageLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

/**
 * Execute "user-land" command; User must be authenticated. This is a hack to
 * be compliant with the stupid "old-school" RFC.
 * <pre>
 *     OpCode: cmd (from the PIE) or user_cmd (external)
 *     Args  : 1.   Commands
 *             2..n (OPTIONAL) Args
 * </pre>
 *
 * @author Thibault Meyer
 * @version 1.1.0
 * @since 1.0.0
 */
public class CmdCommandImpl implements Command {

    /**
     * Logger.
     *
     * @since 1.0.0
     */
    private static final Logger LOG = LoggerFactory.getLogger(CmdCommandImpl.class.getName());

    /**
     * All enabled commands.
     *
     * @since 1.0.0
     */
    private final Map<String, Command> enabledCommands;

    /**
     * Default constructor.
     *
     * @since 1.0.0
     */
    public CmdCommandImpl() {
        this.enabledCommands = new HashMap<>();
        this.__loadEnabledCommands();
    }

    /**
     * Load all enabled commands.
     *
     * @since 1.0.0
     */
    private void __loadEnabledCommands() {
        InputStream fis = null;
        try {
            if (System.getProperty("core.commands", null) != null) {
                URL u = new URL(System.getProperty("core.commands"));
                fis = u.openStream();
            } else {
                fis = Settings.class.getResourceAsStream("/commands.properties");
            }
            Properties properties = new Properties();
            properties.load(fis);

            for (Map.Entry<Object, Object> e : properties.entrySet()) {
                try {
                    if (this.getClass().getCanonicalName().compareTo(e.getValue().toString()) != 0) {
                        final Command cmd = (Command) Class.forName(e.getValue().toString()).newInstance();
                        this.enabledCommands.put(e.getKey().toString(), cmd);
                    }
                } catch (LinkageError | ClassNotFoundException ex) {
                    LOG.warn(String.format("Can't load commands \"%s\"", e.getKey()), ex);
                }
            }

        } catch (Throwable e) {
            LOG.error("Can't open commands file", e);
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException ignore) {
            }
        }
    }

    /**
     * Get the minimal number of arguments needed. The command OpCode is
     * included in the number of arguments.
     *
     * @return The minimal number of arguments needed
     * @since 1.0.0
     */
    @Override
    public int getMinimalArgsCountNeeded() {
        return 2;
    }

    /**
     * Get the maximal number of arguments needed. The command OpCode is
     * included in the number of arguments. If this method return -1, the
     * command can take any number of arguments.
     *
     * @return The maximal number of arguments needed
     * @since 1.0.0
     */
    @Override
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
        if (payload[0].compareTo((usrSession.authType == SessionAuthType.INTERNAL_AUTHENTICATION) ? "cmd" : "user_cmd") != 0) {
            usrSession.addOutputDataAsChunk("rep 403 -- forbidden\n");
        } else {
            final Command cmd = this.enabledCommands.get(payload[1]);
            if (cmd != null) {
                if (cmd.canExecute(usrSession)) {
                    final int minArgs = cmd.getMinimalArgsCountNeeded();
                    final int maxArgs = cmd.getMaximalArgsCountNeeded();
                    final int curArgs = payload.length - 1;
                    if (curArgs >= minArgs && (maxArgs == -1 || curArgs <= maxArgs)) {
                        try {
                            cmd.execute((String[]) Arrays.asList(Arrays.copyOfRange(payload, 1, payload.length)).toArray(), usrSession, connectedSessions, globalFollowers);
                        } catch (Exception e) {
                            LOG.error("Something goes wrong during the command execution!", e);
                            usrSession.addOutputDataAsChunk("rep 500 -- internal error\n");
                        }
                    } else {
                        if (minArgs == maxArgs) {
                            usrSession.addOutputDataAsChunk(String.format("rep 003 -- cmd bad number of arguments %d should be %d\n", curArgs, minArgs));
                        } else if (maxArgs == -1) {
                            usrSession.addOutputDataAsChunk(String.format("rep 003 -- cmd bad number of arguments %d should be at least %d\n", curArgs, minArgs));
                        } else {
                            usrSession.addOutputDataAsChunk(String.format("rep 003 -- cmd bad number of arguments %d should be between %d and %d\n", curArgs, minArgs, maxArgs));
                        }
                    }
                } else {
                    if (cmd.getType() == Command.CmdType.AUTHENTICATION) {
                        usrSession.addOutputDataAsChunk("rep 008 -- agent already log\n");
                    } else {
                        usrSession.addOutputDataAsChunk("rep 403 -- forbidden\n");
                    }
                }
            } else {
                usrSession.addOutputDataAsChunk("rep 001 -- no such cmd\n");
            }
        }
    }
}
