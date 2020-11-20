package com.knoban.multiplayer.utils;

import org.jetbrains.annotations.NotNull;

import java.net.Socket;
import java.util.Random;

/**
 * @author Alden Bansemer (kNoAPP)
 */
public class Tools {

    /**
     * Generate a random int.
     * @param min Minimum inclusive integer
     * @param max Maximum inclusive integer
     * @return The random number
     */
    public static int randomNumber(int min, int max) {
        Random rand = new Random();
        int val = rand.nextInt(max - min + 1) + min;
        return val;
    }

    /**
     * Output a Socket's IP and port.
     * @param socket The Socket to output from
     * @return A formatted String with the Socket's IP and port.
     */
    @NotNull
    public static String formatSocket(@NotNull Socket socket) {
        return socket.getInetAddress() + ":" + socket.getPort();
    }
}
