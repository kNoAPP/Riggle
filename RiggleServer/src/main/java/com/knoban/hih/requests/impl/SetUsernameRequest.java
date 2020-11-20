package com.knoban.hih.requests.impl;

import com.knoban.multiplayer.requests.RequestFulfillment;
import org.jetbrains.annotations.NotNull;

/**
 * @author Alden Bansemer (kNoAPP)
 */
public class SetUsernameRequest implements RequestFulfillment {

    private String username;

    /**
     * @param username The parsed username from the socket.
     */
    public SetUsernameRequest(@NotNull String username) {
        if(username.length() < 1)
            username = "Unnamed Player";

        username = username.trim();
        if(username.length() > 16)
            username = username.substring(0, 16);

        this.username = username.trim();
    }

    /**
     * @return The username in the request.
     */
    @NotNull
    public String getUsername() {
        return username;
    }
}
