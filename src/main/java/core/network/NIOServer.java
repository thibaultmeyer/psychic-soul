package core.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.time.Instant;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Non blocking socket server using Java NIO.
 *
 * @author Thibault Meyer
 * @since 1.0.0
 */
public class NIOServer implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(NIOServer.class.getName());
    private NIOEventListener eventListener;
    private long selectTimeout;
    private int socketListenPort;
    private int socketTTL;
    private int socketMaxConn;
    private Map<SocketChannel, Instant> connectedSocket;
    private Map<SocketChannel, DisconnectReason> toDisconnectSocket;

    /**
     * Constructor.
     *
     * @param event_listener An event listener instance
     * @param listen_port    The socket listen port
     * @param select_timeout The timeout interval
     */
    public NIOServer(NIOEventListener event_listener, int listen_port, long select_timeout) {
        this.eventListener = event_listener;
        this.socketListenPort = listen_port;
        this.selectTimeout = select_timeout;
        this.connectedSocket = new ConcurrentHashMap<SocketChannel, Instant>();
        this.toDisconnectSocket = new ConcurrentHashMap<SocketChannel, DisconnectReason>();
        this.socketTTL = 15;
        this.socketMaxConn = 50;
    }

    /**
     * Check socket TTL and disconnect flagged socket.
     */
    private void __checkSocketToDisconnect() throws ConcurrentModificationException {
        this.connectedSocket.entrySet().stream().forEach((entry) -> {
            Instant now = Instant.now();
            if (entry.getValue().plusSeconds(this.socketTTL).isBefore(now)) {
                this.toDisconnectSocket.put(entry.getKey(), DisconnectReason.NO_ACTIVITY);
            }
        });

        for (Map.Entry<SocketChannel, DisconnectReason> entry : this.toDisconnectSocket.entrySet()) {
            try {
                SocketChannel socket = entry.getKey();
                if (this.eventListener != null) {
                    try {
                        this.eventListener.onDisconnected(socket, entry.getValue());
                    } catch (IOException ignore) {
                    }
                }
                LOG.debug("Client {} disconnected (reason: {})", socket.getRemoteAddress(), entry.getValue());
                socket.finishConnect();
                socket.close();
            } catch (IOException | ClassCastException ignore) {
            }
            toDisconnectSocket.remove(entry.getKey());
            connectedSocket.remove(entry.getKey());
        }
    }

    /**
     * Run the NIO socket server.
     */
    @Override
    public void run() {
        try {
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.socket().bind(new InetSocketAddress(this.socketListenPort));
            LOG.info("Listen on 0.0.0.0:{}", serverSocketChannel.socket().getLocalPort());

            serverSocketChannel.configureBlocking(false);
            Selector selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            while (true) {
                int readyChannels = selector.select(this.selectTimeout);
                if (readyChannels == 0) {
                    if (this.eventListener != null) {
                        this.eventListener.onTimeoutEvent(selector);
                    }
                    try {
                        __checkSocketToDisconnect();
                    } catch (ConcurrentModificationException ignore) {
                    }
                    continue;
                }
                final Set<SelectionKey> selectedKeys = selector.selectedKeys();
                final Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    if (key.isAcceptable()) {
                        SocketChannel client = serverSocketChannel.accept();
                        LOG.debug("New client connected from {}", client.getRemoteAddress());
                        if (this.connectedSocket.size() >= this.socketMaxConn) {
                            this.toDisconnectSocket.put(client, DisconnectReason.TOO_MANY_CLIENTS);
                        } else {
                            client.configureBlocking(false);
                            client.register(selector, SelectionKey.OP_READ);
                            this.connectedSocket.put(client, Instant.now());
                            if (this.eventListener != null) {
                                try {
                                    this.eventListener.onAcceptableEvent(selector, client);
                                } catch (IOException ignore) {
                                }
                            }
                        }
                    } else if (key.isReadable()) {
                        SocketChannel client = (SocketChannel) key.channel();
                        int nbRead = -1;
                        this.connectedSocket.put(client, Instant.now());
                        if (this.eventListener != null) {
                            try {
                                nbRead = this.eventListener.onReadableEvent(selector, client);
                            } catch (IOException ignore) {
                                nbRead = -1;
                            }
                        }
                        if (nbRead == -1) {
                            this.toDisconnectSocket.put(client, DisconnectReason.CLIENT_GONE_AWAY);
                        }
                    } else if (key.isWritable()) {
                        SocketChannel client = (SocketChannel) key.channel();
                        int nbWrite = -1;
                        if (this.eventListener != null) {
                            try {
                                nbWrite = this.eventListener.onWritableEvent(selector, client);
                            } catch (IOException ex) {
                                LOG.debug("Network Write error", ex);
                                nbWrite = -1;
                            }
                        }
                        if (nbWrite == -1) {
                            this.toDisconnectSocket.put(client, DisconnectReason.CLIENT_GONE_AWAY);
                        }
                    }
                    keyIterator.remove();
                }
                if (this.eventListener != null) {
                    try {
                        this.eventListener.onFinalize(selector);
                    } catch (IOException ex) {
                        LOG.warn("Error on onFinalize() callback", ex);
                    }
                }
                try {
                    this.__checkSocketToDisconnect();
                } catch (ConcurrentModificationException ex) {
                    LOG.warn("Concurrent modification detected!", ex);
                }
            }
        } catch (IOException e) {
            LOG.error("Can't bind server", e);
        }
    }

    /**
     * Change the NIO server timeout value.
     *
     * @param select_timeout The new timeout value
     * @return The new timeout value
     */
    public long setSelectTimeout(long select_timeout) {
        this.selectTimeout = select_timeout;
        return select_timeout;
    }

    /**
     * Reset the socket inactivity.
     *
     * @param socket The socket
     */
    public void resetInactivityTTL(SocketChannel socket) {
        this.connectedSocket.put(socket, Instant.now());
    }

    /**
     * Get the socket TTL.
     *
     * @param socket The socket
     * @return the {@code Instant} when the socket will be closed
     */
    public Instant getInactivityTTL(SocketChannel socket) {
        return this.connectedSocket.get(socket);
    }

    /**
     * Add key to be disconnected by the server.
     *
     * @param socket The socket
     * @param reason Why you have disconnected this client?
     */
    public void addToDisconnect(SocketChannel socket, DisconnectReason reason) {
        this.toDisconnectSocket.put(socket, reason);
    }

    /**
     * Set the time in seconds before inactive socket was closed.
     *
     * @param ttl The TTL in seconds
     */
    public void setSocketTTL(int ttl) {
        this.socketTTL = ttl;
    }

    /**
     * Set the maximum number of connected sockets.
     *
     * @param max The maximum number of connected sockets
     */
    public void setMaxConn(int max) {
        this.socketMaxConn = max;
    }
}
