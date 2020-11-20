package com.knoban.hih.requests.impl;

import com.knoban.multiplayer.requests.RequestFulfillment;
import org.jetbrains.annotations.NotNull;

/**
 * @author Alden Bansemer (kNoAPP)
 */
public class JoinRoomRequest implements RequestFulfillment {

    private String code;

    /**
     * Create a RoomRequest
     * @param code The code of the room being requested.
     */
    public JoinRoomRequest(@NotNull String code) {
        code = code.trim();
        for(int i=code.length(); i<4; i++)
            code += 'A'; // because why not

        if(code.length() > 4)
            code = code.substring(0, 4);

        this.code = code.toUpperCase();
    }

    /**
     * @return The code of the room being requested.
     */
    @NotNull
    public String getCode() {
        return code;
    }
}
