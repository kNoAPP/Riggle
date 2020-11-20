package com.knoban.hih.requests.impl;

import com.knoban.hih.game.Location;
import com.knoban.multiplayer.requests.RequestFulfillment;

public class SetLocationRequest implements RequestFulfillment {

    private final Location location;

    public SetLocationRequest(Location location) {
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }
}
