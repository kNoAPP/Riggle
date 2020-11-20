package com.knoban.multiplayer.server;

import com.knoban.multiplayer.utils.Tools;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Alden Bansemer (kNoAPP)
 */
public class MultiplayerServer {

    private Class<? extends MultiplayerConnection> connectionDriver;
    private HashSet<MultiplayerConnection> connections = new HashSet<>();
    private final ReadWriteLock rwLockConnections = new ReentrantReadWriteLock();

    private final ServerSocket serverSocket;
    private final int port;
    private Thread connectionListener;
    private volatile boolean isListening;

    private Thread requestProcessor;
    private volatile ProcessingStatus processingStatus;

    /**
     * Create a new MultiplayerServer instance running on a specified port.
     * @param port The port to run on.
     * @throws IOException If the server socket cannot be created.
     */
    public MultiplayerServer(int port) throws IOException {
        this.connectionDriver = MultiplayerConnection.class;
        this.port = port;
        this.serverSocket = new ServerSocket(port);
    }

    /**
     * @return The port this server runs on.
     */
    public int getPort() {
        return port;
    }

    /**
     * This multiplayer server will call this class to create MultiplayerConnections.
     * @param connectionDriver The driver of the multiplayer server.
     */
    public void setConnectionDriver(@NotNull Class<? extends MultiplayerConnection> connectionDriver) {
        this.connectionDriver = connectionDriver;
    }

    /**
     * @return The driver of the multiplayer server. (defaults to MultiplayerConnection.class)
     */
    @NotNull
    public Class<? extends MultiplayerConnection> getConnectionDriver() {
        return connectionDriver;
    }

    /**
     * Begin listening for new connections to the server. Each connection is
     * given its own thread to run on for incoming data.
     */
    public void open() {
        if(isListening)
            return;

        isListening = true;
        connectionListener = new Thread(() -> {
            while(isListening) {
                try {
                    Socket connection = serverSocket.accept();
                    System.out.println("Connected [CLIENT/SERVER]: " + Tools.formatSocket(connection));
                    Constructor<? extends MultiplayerConnection> constructor = connectionDriver.getDeclaredConstructor(Socket.class);
                    constructor.setAccessible(true);
                    MultiplayerConnection conn = constructor.newInstance(connection);
                    constructor.setAccessible(false);

                    rwLockConnections.writeLock().lock();
                    connections.add(conn);
                    rwLockConnections.writeLock().unlock();
                    conn.open();
                } catch(IOException e) {
                    System.out.println("Failed to accept connection: " + e.getMessage());
                } catch(NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
                    System.out.println("Failed to create connection from driver: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
        connectionListener.start();
    }

    private enum ProcessingStatus {
        RUNNING, // Accepting requests
        STOPPING, // Accepting requests until all connections close themselves, then automatically changes to STOPPED
        STOPPED // Not taking any further requests
    }

    /**
     * Begin processing requests from all connections. All pending requests are processed on each connection
     * before moving on to the next. Thus, data handled from processed requests is Thread-safe between connections.
     */
    public void startProcessingRequests() {
        processingStatus = ProcessingStatus.RUNNING;
        requestProcessor = new Thread(() -> {
            while(processingStatus != ProcessingStatus.STOPPED) {
                rwLockConnections.readLock().lock();
                List<MultiplayerConnection> connections = new ArrayList<>(this.connections); // Avoids concurrent modification
                rwLockConnections.readLock().unlock();

                if(connections.isEmpty() && processingStatus == ProcessingStatus.STOPPING) {
                    processingStatus = ProcessingStatus.STOPPED;
                    return;
                }

                connections.forEach((connection) -> {
                    connection.processRequests();

                    Boolean status = connection.isClosed();
                    if(status != null && status) {
                        rwLockConnections.writeLock().lock();
                        this.connections.remove(connection);
                        rwLockConnections.writeLock().unlock();
                    }
                });
            }
        });
        requestProcessor.start();
    }

    /**
     * Stop processing requests. This does not empty the request queue nor does it block additional requests. It only
     * stops the server's processing and response to them. Restarting processing requests will handle all received
     * requests during the time processing was stopped.
     */
    public void stopProcessingRequests() {
        stopProcessingRequests(ProcessingStatus.STOPPED);
    }

    private void stopProcessingRequests(ProcessingStatus status) {
        processingStatus = status;
        try {
            requestProcessor.join();
            requestProcessor = null;
        } catch(InterruptedException e) {
            System.out.println("Got interrupted while stopping request processing: " + e.getMessage());
        }
    }

    /**
     * Close the server. All current connections will be closed and any pending requests will not be handled. The
     * server may be reopened with open() after calling this. New connections will then be accepted.
     */
    public void close() {
        isListening = false;
        try {
            serverSocket.close();
            connectionListener.join();
            connectionListener = null;
        } catch(InterruptedException | IOException e) {
            System.out.println("Got interrupted while stopping incoming connections: " + e.getMessage());
        }

        new ArrayList<>(connections).forEach(MultiplayerConnection::close); // Avoids concurrent modification
        stopProcessingRequests(ProcessingStatus.STOPPING);
    }
}
