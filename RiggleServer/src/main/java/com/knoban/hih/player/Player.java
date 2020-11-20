package com.knoban.hih.player;

import com.knoban.hih.game.Room;
import com.knoban.hih.requests.RequestCode;
import com.knoban.hih.requests.impl.JoinRoomRequest;
import com.knoban.hih.requests.impl.SetUsernameRequest;
import com.knoban.multiplayer.requests.GeneralRequestCode;
import com.knoban.multiplayer.requests.RequestFulfillment;
import com.knoban.multiplayer.server.MultiplayerConnection;
import com.knoban.multiplayer.utils.Tools;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.Socket;

/**
 * @author Alden Bansemer (kNoAPP)
 */
public class Player extends MultiplayerConnection {

    private String username;
    private Room room;

    /**
     * Creates a new Player. This should only be called by MultiplayerServer.
     * @param connection - The connection from which data is received and sent.
     */
    protected Player(@NotNull Socket connection) {
        super(connection);
        this.username = "Unnamed Player";
    }

    /**
     * Leaves the room the player is currently in if any.
     * @returns True, if they were in a room and left.
     */
    private boolean leaveRoom() {
        boolean toRet = false;
        if(room != null) {
            toRet = room.removePlayer(this); // Leave current room if thats a thing
            room = null;
        }
        return toRet;
    }

    /**
     * Decode requests here using the in variable to get more data from client. This runs on its own thread
     * so be careful of race conditions. After decoding, encode a RequestFulfillment object and return it. This
     * object will be passed to your thread-safe handleRequest method.
     * @param requestCode The requestCode received from the client.
     * @throws IOException If at any point the server/client have trouble communicating. Handled automatically.s
     */
    @Override
    @Nullable
    public RequestFulfillment decodeRequest(short requestCode) throws IOException {
        switch(requestCode) {
            case RequestCode.JOIN_ROOM:
                return new JoinRoomRequest(in.readString());
            case RequestCode.SET_USERNAME:
                return new SetUsernameRequest(in.readString());
            case RequestCode.USER_ID:
            case RequestCode.LEAVE_ROOM:
            case RequestCode.CREATE_ROOM:
            case RequestCode.ROOM_INFO:
            default:
                return null;
        }
    }

    /**
     * Override this method to supply your own request handling.
     * @param requestCode The short requestCode
     * @param data        The data that comes with it. (You may want to cast this to your own classes)
     * @throws IOException If at any point the server/client have trouble communicating. Handled automatically.
     */
    @Override
    public void handleRequest(short requestCode, @Nullable RequestFulfillment data) throws IOException {
        switch(requestCode) {
            case GeneralRequestCode.CONNECTED:
                return;
            case GeneralRequestCode.DISCONNECT:
                leaveRoom();
                return;
            case RequestCode.USER_ID:
                System.out.println(Tools.formatSocket(connection) + ": USER_ID");
                out.writeS16(GeneralRequestCode.HANDSHAKE);
                out.writeS16(requestCode);
                out.writeString(uuid.toString());
                return;
            case RequestCode.JOIN_ROOM:
                System.out.println(Tools.formatSocket(connection) + ": JOIN_ROOM");
                assert data != null;
                JoinRoomRequest joinRoomRequest = (JoinRoomRequest) data;
                Room joiningRoom = Room.getRoomFromCode(joinRoomRequest.getCode());
                out.writeS16(GeneralRequestCode.HANDSHAKE);
                out.writeS16(requestCode);
                if(joiningRoom == null) {
                    out.writeS8((byte) 1); // Error 0x01 - room not found
                    return;
                }

                if(!joiningRoom.addPlayer(this)) {
                    out.writeS8((byte) 2); // Error 0x02 - room found, but full
                    return;
                }

                leaveRoom();
                room = joiningRoom;
                out.writeS8((byte) 0); // Ok 0x00 - room joined
                return;
            case RequestCode.LEAVE_ROOM:
                System.out.println(Tools.formatSocket(connection) + ": LEAVE_ROOM");
                out.writeS16(GeneralRequestCode.HANDSHAKE);
                out.writeS16(requestCode);

                if(!leaveRoom()) {
                    out.writeS8((byte) 1); // Error 0x01 - not currently in a room
                }

                out.writeS8((byte) 0); // Ok 0x00 - room left
                return;
            case RequestCode.CREATE_ROOM:
                System.out.println(Tools.formatSocket(connection) + ": CREATE_ROOM");

                leaveRoom();
                Room newRoom = new Room(this);
                room = newRoom;

                out.writeS16(GeneralRequestCode.HANDSHAKE);
                out.writeS16(requestCode);
                out.writeString(newRoom.getRoomCode().toString());
                return;
            case RequestCode.ROOM_INFO:
                System.out.println(Tools.formatSocket(connection) + ": ROOM_INFO");

                out.writeS16(GeneralRequestCode.HANDSHAKE);
                out.writeS16(requestCode);
                if(room == null) {
                    out.writeS8((byte) 1); // Error 0x01 - not currently in a room
                    return;
                }

                out.writeS8((byte) 0); // Ok 0x00 - printing room info
                out.writeString(room.getRoomCode().toString());
                out.writeS8((byte) room.getPlayers().size());
                for(Player player : room.getPlayers())
                    out.writeString(player.username); // Prints player names. The first player is the host.
                return;
            case RequestCode.SET_USERNAME:
                System.out.println(Tools.formatSocket(connection) + ": SET_USERNAME");
                assert data != null;
                SetUsernameRequest setUsernameRequest = (SetUsernameRequest) data;
                username = setUsernameRequest.getUsername();

                out.writeS16(GeneralRequestCode.HANDSHAKE);
                out.writeS16(requestCode);
                out.writeString(username);
                return;
            default:
                System.out.println("Got unknown request: " + requestCode);
        }
    }
}
