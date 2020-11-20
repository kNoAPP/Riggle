package com.knoban.hih;

import com.knoban.hih.player.Player;
import com.knoban.multiplayer.server.MultiplayerServer;

import java.io.IOException;

/**
 * @author Alden Bansemer (kNoAPP)
 */
public class Main {

    /**
     * Entry into the program.
     * @param args The passed jvm arguments
     */
    public static void main(String args[]) {
        try {
            MultiplayerServer server = new MultiplayerServer(25580);
            server.setConnectionDriver(Player.class);
            server.open();
            server.startProcessingRequests();
        } catch(IOException e) {
            System.out.println("Unable to create the server: " + e.getMessage());
        }
    }
}
