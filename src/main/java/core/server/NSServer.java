package core.server;

import core.Settings;
import core.crypto.MD5;
import core.network.DisconnectReason;
import core.network.NIOEventListener;
import core.network.NIOServer;
import core.server.command.Command;
import core.server.database.DBPool;
import core.server.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.time.Instant;
import java.util.*;

/**
 * Netsoul dedicated server.
 *
 * @author Thibault Meyer
 * @version 1.1.2
 * @since 1.0.0
 */
public class NSServer implements NIOEventListener {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(NSServer.class.getName());

    /**
     * Timeout interval for the NIO select (in milliseconds).
     */
    private static final long SELECT_TIMEOUT = 50;

    /**
     * Chunk size.
     */
    private static final int CHUNK_SIZE = 256;

    /**
     * NIO Server instance.
     */
    private NIOServer nioServer;

    /**
     * Connected user.
     */
    private Map<Integer, Session> connectedUserSessions;

    /**
     * All enabled commands.
     */
    private Map<String, Command> enabledCommands;

    /**
     * Information about followers.
     */
    private Map<String, List<Session>> globalFollowers;

    /**
     * Load all enabled commands.
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
                    final Command cmd = (Command) Class.forName(e.getValue().toString()).newInstance();
                    this.enabledCommands.put(e.getKey().toString(), cmd);
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
     * Run the server.
     *
     * @return The status of this method
     */
    public int run() {
        this.connectedUserSessions = new HashMap<>();
        this.enabledCommands = new HashMap<>();
        this.globalFollowers = new HashMap<>();

        LOG.info("Checking commands...");
        this.__loadEnabledCommands();
        LOG.info("{} command(s) enabled", this.enabledCommands.size());

        LOG.info("Testing SQL database connection...");
        try {
            if (!DBPool.getInstance().isOnline()) {
                return 1;
            }
        } catch (ExceptionInInitializerError ignore) {
            System.err.println("Bad configuration! Go to https://github.com/0xbaadf00d/psychic-soul/wiki");
            return 1;
        }

        LOG.info("Starting server...");
        this.nioServer = new NIOServer(this, Settings.socketPort, NSServer.SELECT_TIMEOUT);
        this.nioServer.setSocketTTL(Settings.socketTTL);
        this.nioServer.setMaxConn(Settings.socketMaxConn);
        LOG.info("Ready!");
        this.nioServer.run();
        return 0;
    }

    /**
     * Called each time a new channel is accepted.
     *
     * @param selector The event selector
     * @param socket   The channel socket
     * @throws java.io.IOException If IO operation fail (like read/write on socket)
     */
    @Override
    public void onAcceptableEvent(Selector selector, SocketChannel socket) throws IOException {
        final Session usrSess = new Session();
        final long curTimestamp = System.currentTimeMillis() / 1000;

        usrSess.network.socket = socket;
        usrSess.network.ip = socket.socket().getInetAddress().getHostAddress();
        usrSess.network.port = socket.socket().getPort();
        usrSess.network.address = socket.getRemoteAddress().toString();
        usrSess.network.fd = socket.hashCode();
        usrSess.network.selector = selector;
        usrSess.hash = MD5.hash(String.format("%s%d", socket.toString(), curTimestamp));

        usrSess.addOutputDataAsChunk(String.format("salut %d %s %s %d %d\n",
                usrSess.network.fd,
                usrSess.hash,
                usrSess.network.ip,
                usrSess.network.port,
                curTimestamp));
        usrSess.network.registerWriteEvent();
        this.connectedUserSessions.put(socket.hashCode(), usrSess);
    }

    /**
     * Called each time a channel is ready to read.
     *
     * @param selector The event selector
     * @param socket   The channel socket
     * @return The number of read bytes
     * @throws java.io.IOException If IO operation fail (like read/write on socket)
     */
    @Override
    public int onReadableEvent(Selector selector, SocketChannel socket) throws IOException {
        final Session usrSess = this.connectedUserSessions.get(socket.hashCode());
        final ByteBuffer buffer = ByteBuffer.allocate(NSServer.CHUNK_SIZE);
        int nbRead;

        nbRead = socket.read(buffer);
        if (nbRead > 0) {
            buffer.flip();
            while (buffer.hasRemaining()) {
                final byte[] tmpBuffer = new byte[nbRead >= NSServer.CHUNK_SIZE ? NSServer.CHUNK_SIZE : nbRead];
                buffer.get(tmpBuffer);
                final String tmpBufferCleaned = new String(tmpBuffer, "UTF-8");
                usrSess.inputBuffer.add(tmpBufferCleaned.replaceAll("\\p{Cc}\\p{Cf}\\p{Co}\\p{Cn}", "?").replace("\r", ""));
            }
            buffer.clear();
        }

        return nbRead;
    }

    /**
     * Called each time a channel is ready to write.
     *
     * @param selector The event selector
     * @param socket   The channel socket
     * @return The number of wrote bytes
     * @throws java.io.IOException If IO operation fail (like read/write on socket)
     */
    @Override
    public int onWritableEvent(Selector selector, SocketChannel socket) throws IOException {
        final Session usrSess = this.connectedUserSessions.get(socket.hashCode());
        int nbByteWritten = 0;
        try {
            if (!usrSess.outputBuffer.isEmpty()) {
                final String data = usrSess.outputBuffer.remove(0);
                final int dataSize = data.length();
                nbByteWritten = socket.write(ByteBuffer.wrap(data.getBytes()));
                if (nbByteWritten != dataSize) {
                    usrSess.outputBuffer.add(0, data.substring(nbByteWritten));
                    throw new RuntimeException("Data partially sent");
                }
            }
            if (usrSess.outputBuffer.isEmpty()) {
                usrSess.network.unregisterWriteEvent();
            }
        } catch (RuntimeException ignore) {
        }
        if (usrSess.disconnectReason != null && usrSess.network.socket != null) {
            this.nioServer.addToDisconnect(usrSess.network.socket, usrSess.disconnectReason);
        }
        return nbByteWritten;
    }

    /**
     * Called each time the select() method timeout.
     *
     * @param selector The event selector
     * @throws java.io.IOException If IO operation fail (like read/write on socket)
     */
    @Override
    public void onTimeoutEvent(Selector selector) throws IOException {
        this.onFinalize(selector);
    }

    /**
     * Called each time after all selectors are processed (only when select()
     * returned value is superior to 0).
     *
     * @param selector The event selector
     * @throws java.io.IOException If IO operation fail (like read/write on socket)
     */
    @Override
    public void onFinalize(Selector selector) throws IOException {
        final Instant currentInstant = Instant.now();
        for (Session usrSess : this.connectedUserSessions.values()) {
            final String[] payload = usrSess.getNextPayload();
            if (payload != null) {
                if (LOG.isTraceEnabled()) {
                    String methodName = payload[0];
                    if (payload.length > 1 && (methodName.compareTo("user_cmd") == 0 || methodName.compareTo("cmd") == 0)) {
                        methodName += String.format("::%s", payload[1]);
                    }
                    LOG.trace("Client from {} call the method \"{}\"",
                            String.format("%s (%s)",
                                    usrSess.network.address,
                                    (usrSess.user.login == null) ? "<not_authenticated>" : usrSess.user.login),
                            methodName);
                }
                final Command cmd = this.enabledCommands.get(payload[0]);
                if (cmd != null) {
                    if (cmd.canExecute(usrSess)) {
                        final int minArgs = cmd.getMinimalArgsCountNeeded();
                        final int maxArgs = cmd.getMaximalArgsCountNeeded();
                        if (payload.length >= minArgs && (maxArgs == -1 || payload.length <= maxArgs)) {
                            try {
                                cmd.execute(payload, usrSess, this.connectedUserSessions.values(), this.globalFollowers);
                            } catch (Exception e) {
                                LOG.error("Something goes wrong during the command execution!", e);
                                usrSess.addOutputDataAsChunk("rep 500 -- internal error\n");
                            }
                        } else {
                            if (minArgs == maxArgs) {
                                usrSess.addOutputDataAsChunk(String.format("rep 003 -- cmd bad number of arguments %d should be %d\n", payload.length, minArgs));
                            } else if (maxArgs == -1) {
                                usrSess.addOutputDataAsChunk(String.format("rep 003 -- cmd bad number of arguments %d should be at least %d\n", payload.length, minArgs));
                            } else {
                                usrSess.addOutputDataAsChunk(String.format("rep 003 -- cmd bad number of arguments %d should be between %d and %d\n", payload.length, minArgs, maxArgs));
                            }
                        }
                    } else {
                        if (cmd.getType() == Command.CmdType.AUTHENTICATION) {
                            usrSess.addOutputDataAsChunk("rep 008 -- agent already log\n");
                        } else {
                            usrSess.addOutputDataAsChunk("rep 403 -- forbidden\n");
                        }
                    }
                } else {
                    usrSess.addOutputDataAsChunk("rep 001 -- no such cmd\n");
                }
                usrSess.network.registerWriteEvent();
            }
            if (usrSess.disconnectReason != null && usrSess.network.socket != null && usrSess.outputBuffer.isEmpty()) {
                this.nioServer.addToDisconnect(usrSess.network.socket, usrSess.disconnectReason);
            } else {
                final Instant lastSockActivity = this.nioServer.getInactivityTTL(usrSess.network.socket);
                if (lastSockActivity != null && currentInstant.isAfter(lastSockActivity.plusMillis(Settings.socketTTL * 750)) && usrSess.lastPingSent.isBefore(currentInstant)) {
                    usrSess.addOutputDataAsChunk(String.format("ping %d\n", lastSockActivity.plusSeconds(Settings.socketTTL).minusSeconds(currentInstant.getEpochSecond()).getEpochSecond()));
                    usrSess.network.registerWriteEvent();
                    usrSess.lastPingSent = currentInstant.plusMillis(Settings.socketTTL * 800);
                } else if (lastSockActivity == null) {
                    LOG.warn("Gnarf! Where is the client {}",
                            String.format("%s (%s)",
                                    usrSess.network.address,
                                    (usrSess.user.login == null) ? "<not_authenticated>" : usrSess.user.login),
                            usrSess.network.socket.hashCode());
                    this.nioServer.addToDisconnect(usrSess.network.socket, DisconnectReason.NO_ACTIVITY);
                }
            }
        }
    }

    /**
     * Called when socket channel will be closed.
     *
     * @param socket      The socket channel
     * @param discoReason The disconnection reason
     * @throws java.io.IOException If IO operation fail (like read/write on socket)
     */
    @Override
    public void onDisconnected(SocketChannel socket, DisconnectReason discoReason) throws IOException {
        final Session usrSess = this.connectedUserSessions.get(socket.hashCode());
        if (usrSess != null && usrSess.user.login != null) {
            Command cmdState = this.enabledCommands.get("state");
            if (cmdState != null) {
                cmdState.execute((String[]) Arrays.asList("logout", "offline").toArray(), usrSess, this.connectedUserSessions.values(), this.globalFollowers);
            }
            this.globalFollowers.values().stream().forEach(gf -> gf.remove(usrSess));
        }
        this.connectedUserSessions.remove(socket.hashCode());
    }
}
