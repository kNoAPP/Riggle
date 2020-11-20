package com.knoban.hih.game;

import com.knoban.hih.player.Player;
import com.knoban.multiplayer.utils.RoomCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Alden Bansemer (kNoAPP)
 */
public class Room {

    public static final short MAX_ROOM_SIZE = 4;
    private static final HashMap<RoomCode, Room> ROOMS = new HashMap<>();

    private Player leader; // The player with the permissions
    private final List<Player> players = new LinkedList<>();

    private RoomCode code;
    private boolean inGame;

    /**
     * Creates an empty Room players can join. Pretty much like a party system. The generated room code
     * will be unique.
     * @param leader The initial host of the lobby
     */
    public Room(@NotNull Player leader) {
        this.leader = leader;
        players.add(leader);

        // Room code generation should be recoded if the game gets popular. This could be potentially a long-running
        // do-while.
        do {
           this.code = new RoomCode();
        } while(getRoomFromCode(code) != null);
        ROOMS.put(code, this);

        this.inGame = false;
    }

    /**
     * @return The leader of the Room. Only null if the room was destroyed, which should be never if you do things right.
     */
    @NotNull
    public Player getLeader() {
        return leader;
    }

    /**
     * @return The 4-letter, no number code for the room.
     */
    @NotNull
    public RoomCode getRoomCode() {
        return code;
    }

    /**
     * Add a player to the room.
     * @param player The player's connection to add.
     * @return True, if the player was added to the room.
     */
    public boolean addPlayer(@NotNull Player player) {
        if(players.contains(player) || players.size() >= MAX_ROOM_SIZE || inGame)
            return false;

        players.add(player);
        return true;
    }

    /**
     * Remove a player from the room.
     * @param player The player's connection to remove.
     * @return True, if the player was previously in the room and left.
     */
    public boolean removePlayer(@NotNull Player player) {
        boolean toRet = players.remove(player);
        if(players.size() == 0) {
            destroy();
            return toRet;
        }

        if(toRet && player.equals(leader))
            leader = players.get(0);

        return toRet;
    }

    /**
     * @return A list of all players in the room. The first in the list is the host.
     */
    public List<Player> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    /**
     * @return False if in lobby. True if in game. Party is disbanded after the game so no post-win state.
     */
    public boolean isInGame() {
        return inGame;
    }

    /**
     * Set if this room is in game or in lobby.
     * @param inGame True, in game. False, in lobby.
     */
    public void setInGame(boolean inGame) {
        this.inGame = inGame;
    }

    /**
     * Destroys the room. The room code is returned to the pool. All players are removed.
     */
    public void destroy() {
        ROOMS.remove(code);

        for(Player player : Collections.unmodifiableList(players))
            removePlayer(player);

        leader = null;
    }

    /**
     * Gets the Room corresponding to the provided code.
     * @param code A 4-character (no number) code. Not case-sensitive.
     * @return The Room or null if it doesn't exist.
     */
    @Nullable
    public static Room getRoomFromCode(@NotNull String code) {
        if(code.length() != 4)
            return null;

        return getRoomFromCode(new RoomCode(code));
    }

    /**
     * Gets the Room corresponding to the provided code.
     * @param code A 4-character (no number) code. Not case-sensitive.
     * @return The Room or null if it doesn't exist.
     */
    @Nullable
    public static Room getRoomFromCode(@NotNull RoomCode code) {
        return ROOMS.get(code);
    }
}
