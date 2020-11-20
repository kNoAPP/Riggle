package com.knoban.multiplayer.server;

import com.knoban.multiplayer.requests.GeneralRequestCode;
import com.knoban.multiplayer.requests.RequestFulfillment;
import com.knoban.multiplayer.streams.CSInputStream;
import com.knoban.multiplayer.streams.CSOutputStream;
import com.knoban.multiplayer.utils.Pair;
import com.knoban.multiplayer.utils.Tools;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Alden Bansemer (kNoAPP)
 */
public class MultiplayerConnection {

    protected final Socket connection;
    private Thread listener;
    protected CSInputStream in;
    protected CSOutputStream out;
    private volatile Boolean isClosed;

    private final Queue<Pair<Short, RequestFulfillment>> queuedRequests = new LinkedList<>();
    private final ReadWriteLock queuedRequestsLock = new ReentrantReadWriteLock();

    protected UUID uuid;

    /**
     * Creates a new MultiplayerConnection. This should only be called by MultiplayerServer.
     * @param connection The connection from which data is received and sent.
     */
    protected MultiplayerConnection(@NotNull Socket connection) {
        this.connection = connection;
        this.uuid = UUID.randomUUID(); // Assign this connection a unique UUID.

        // Alert Connected
        queuedRequestsLock.writeLock().lock();
        queuedRequests.offer(new Pair<>(GeneralRequestCode.CONNECTED, null));
        queuedRequestsLock.writeLock().unlock();
    }

    /**
     * @return The unique UUID of this MultiplayerConnection.
     */
    @NotNull
    public UUID getUUID() {
        return uuid;
    }

    /**
     * @return True, if connection is closed. False, if connection is open. Null, if the connection can be opened.
     */
    @Nullable
    public Boolean isClosed() {
        return isClosed;
    }

    /**
     * Connections may only be opened ONCE! Once opened, they process requests from the Socket until the
     * connection is closed by the client or server. This call runs on its own Thread.
     */
    public void open() {
        if(isClosed != null)
            return;

        isClosed = false;
        listener = new Thread(() -> {
            while(!isClosed) {
                try {
                    in = new CSInputStream(connection.getInputStream());
                    out = new CSOutputStream(connection.getOutputStream());

                    top: while(!isClosed) {
                        if(in.readS16() == GeneralRequestCode.HANDSHAKE) {
                            short requestCode = in.readS16();
                            /*
                             * Enqueue requests here with close attention to race conditions. Use the header's
                             * request code to determine the request type. If data must be passed, use the
                             * RequestFulfillment interface to create a class that can carry specific types
                             * of data to the processing queue.
                             *
                             * It is okay, safe, and encouraged to use null for the RequestFulfillment if no
                             * extra data must be passed to the handler.
                             */

                            try {
                                Pair<Short, RequestFulfillment> request = new Pair<>(requestCode, decodeRequest(requestCode));

                                queuedRequestsLock.writeLock().lock();
                                queuedRequests.offer(request);
                                queuedRequestsLock.writeLock().unlock();
                            } catch(IOException e) {
                                System.out.println("Failed to decode request: " + e.getMessage());
                            }
                        }
                    }
                } catch(SocketException | EOFException e) {
                    // Client issued disconnect.
                    System.out.println("Disconnect [Client]: " + Tools.formatSocket(connection));
                    close(); // Formally close this connection.
                } catch(IOException e) {
                    System.out.println("Failed to read header: " + e.getMessage());
                }
            }
        });
        listener.start();
    }

    /**
     * Immediately process the request queue. If called from the server's request processing, this is thread safe.
     * Can be called elsewhere with caution for race conditions.
     */
    public void processRequests() {
        while(!queuedRequests.isEmpty()) {
            queuedRequestsLock.writeLock().lock();
            Pair<Short, RequestFulfillment> request = queuedRequests.poll();
            queuedRequestsLock.writeLock().unlock();

            Short requestCode = request.getKey();
            RequestFulfillment data = request.getValue();

            try {
                handleRequest(requestCode, data);
            } catch(IOException e) {
                System.out.println("Unable to handle request IO: " + e.getMessage());
            }
        }
    }

    /**
     * Decode requests here using the in variable to get more data from client. This runs on its own thread
     * so be careful of race conditions. After decoding, encode a RequestFulfillment object and return it. This
     * object will be passed to your thread-safe handleRequest method.
     * @param requestCode The requestCode received from the client.
     * @throws IOException If at any point the server/client have trouble communicating. Handled automatically.
     * @return The decoded request or null if no data.
     */
    @Nullable
    public RequestFulfillment decodeRequest(short requestCode) throws IOException {
        RequestFulfillment request = null;
        switch(requestCode) {
            default:
                break;
        }

        return request;
    }

    /**
     * Override this method to supply your own request handling.
     * @param requestCode The short requestCode
     * @param data The data that comes with it. (You may want to cast this to your own classes)
     * @throws IOException If at any point the server/client have trouble communicating. Handled automatically.
     */
    public void handleRequest(short requestCode, @Nullable RequestFulfillment data) throws IOException {
        switch(requestCode) {
            case GeneralRequestCode.CONNECTED:
                break;
            case GeneralRequestCode.DISCONNECT:
                break;
            default:
                System.out.println("Got unknown request: " + requestCode);
                break;
        }
    }

    /**
     * Close the connection. Once closed, this MultiplayerConnection may not be reopened. A new connection must be created
     * from the client-side of things. This will also run all disconnect callbacks immediately on caller's thread.
     */
    public void close() {
        if(isClosed == null || isClosed)
            return;

        // Alert Disconnect
        queuedRequestsLock.writeLock().lock();
        queuedRequests.offer(new Pair<>(GeneralRequestCode.DISCONNECT, null));
        queuedRequestsLock.writeLock().unlock();

        isClosed = true;
        System.out.println("Disconnect [Server]: " + Tools.formatSocket(connection));

        try {
            connection.close();
            listener.join();
        } catch(IOException | InterruptedException e) {
            System.out.println("Error closing connection: " + e.getMessage());
        }
    }

    /*
     * House keeping to properly identify and compare this class.
     */

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof MultiplayerConnection))
            return false;

        MultiplayerConnection player = (MultiplayerConnection) o;
        return player.uuid.equals(uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }
}
