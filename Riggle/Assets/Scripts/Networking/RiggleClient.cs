using System;
using System.Collections;
using System.Collections.Generic;
using System.Text;
using UnityEngine;

public class RiggleClient : TCPClient
{
    // TODO Create Packet Codes (sync with server)
    public const short USER_ID = 1;
    public const short JOIN_ROOM = 2;
    public const short LEAVE_ROOM = 3;
    public const short CREATE_ROOM = 4;
    public const short ROOM_INFO = 5;
    public const short SET_USERNAME = 6;
    public const short SET_LOCATION = 7;

    // Parse incoming packets into a form we can use later. This is async.
    public override NetworkedResponse ParseIncomingPacket()
    {
        short request = BitConverter.ToInt16(BlockReceive(2), 0); // Get the request code, convert to a short.
        switch (request) // TODO Parse Recieved Packets
        {
            case USER_ID: // We got a Guid, parse from 1s and 0s to something helpful
                GuidResponse guidResponse = new GuidResponse(); // Make a task container
                guidResponse.Code = request; // Add the task code
                guidResponse.Guid = Guid.Parse(StringFromStream()); // Add the parsed Guid
                return guidResponse;
            case JOIN_ROOM:
            case LEAVE_ROOM:
                StatusResponse statusResponse = new StatusResponse();
                statusResponse.Code = request;
                statusResponse.Status = BlockReceive(1)[0];
                return statusResponse;
            case CREATE_ROOM:
                CreateRoomResponse createRoomResponse = new CreateRoomResponse();
                createRoomResponse.Code = request;
                createRoomResponse.RoomCode = StringFromStream();
                return createRoomResponse;
            case SET_USERNAME:
                SetUsernameResponse setUsernameResponse = new SetUsernameResponse();
                setUsernameResponse.Code = request;
                setUsernameResponse.Username = StringFromStream();
                return setUsernameResponse;
            case ROOM_INFO:
                RoomInfoResponse roomInfoResponse = new RoomInfoResponse();
                roomInfoResponse.Code = request;
                roomInfoResponse.Status = BlockReceive(1)[0];
                if (roomInfoResponse.Status == 0x00)
                {
                    roomInfoResponse.RoomCode = StringFromStream();
                    byte playerCount = BlockReceive(1)[0];
                    roomInfoResponse.Guids = new Guid[playerCount];
                    roomInfoResponse.Usernames = new string[playerCount];
                    for (byte i = 0; i < playerCount; i++)
                    {
                        roomInfoResponse.Guids[i] = Guid.Parse(StringFromStream());
                        roomInfoResponse.Usernames[i] = StringFromStream();
                    }
                        
                }
                return roomInfoResponse;
            case SET_LOCATION:
                SetLocationResponse setLocationResponse = new SetLocationResponse();
                setLocationResponse.Code = request;
                setLocationResponse.Guid = Guid.Parse(StringFromStream());
                byte[] bits = BlockReceive(12);
                setLocationResponse.Location = new Vector3(BitConverter.ToSingle(bits, 0), BitConverter.ToSingle(bits, 4), BitConverter.ToSingle(bits, 8));
                return setLocationResponse;
            default:
                Debug.Log("Unknown request: " + request); // Uhh? Bad request code. We can't handle this.
                return null;
        }
    }

    // TODO Create Request Packets

    // Request our Guid. Call our callback when you get it.
    // Cast to GuidResponse.
    public void RequestUserId(NetworkDelegate callback)
    {
        // Request a GUID
        byte[] buffer = Combine(
            BitConverter.GetBytes(HANDSHAKE),
            BitConverter.GetBytes(USER_ID)
        );
        SendAsyncByteStream(buffer);

        EnqueueCallback(USER_ID, callback);
    }

    // Request a Room. Call our callback when you get it.
    // Cast to StatusResponse.
    // 0x00 - room joined
    // 0x01 - room not found
    // 0x02 - room found, but full
    public void RequestJoinRoom(String roomCode, NetworkDelegate callback)
    {
        byte[] buffer = Combine(
            BitConverter.GetBytes(HANDSHAKE),
            BitConverter.GetBytes(JOIN_ROOM),
            Encoding.ASCII.GetBytes(roomCode + "\0") // strings must be null-terminated.
        );
        SendAsyncByteStream(buffer);

        EnqueueCallback(JOIN_ROOM, callback);
    }

    // Request to leave the Room. Call our callback when you get a response.
    // Cast to StatusResponse.
    // 0x00 - room left
    // 0x01 - not currently in a room
    public void RequestLeaveRoom(NetworkDelegate callback)
    {
        byte[] buffer = Combine(
            BitConverter.GetBytes(HANDSHAKE),
            BitConverter.GetBytes(LEAVE_ROOM)
        );
        SendAsyncByteStream(buffer);

        EnqueueCallback(LEAVE_ROOM, callback);
    }

    // Request to create a new Room. Call our callback when you get a response.
    // Cast to CreateRoomResponse.
    public void RequestCreateRoom(NetworkDelegate callback)
    {
        byte[] buffer = Combine(
            BitConverter.GetBytes(HANDSHAKE),
            BitConverter.GetBytes(CREATE_ROOM)
        );
        SendAsyncByteStream(buffer);

        EnqueueCallback(CREATE_ROOM, callback);
    }

    // Request information on whos in your current room. Call our callback when you get a response.
    // Cast to RoomInfoResponse.
    public void RequestRoomInfo(NetworkDelegate callback)
    {
        byte[] buffer = Combine(
            BitConverter.GetBytes(HANDSHAKE),
            BitConverter.GetBytes(ROOM_INFO)
        );
        SendAsyncByteStream(buffer);

        EnqueueCallback(ROOM_INFO, callback);
    }

    // Request to set your username. Call our callback when you get a response.
    // Cast to SetUsernameResponse.
    public void RequestSetUsername(string username, NetworkDelegate callback)
    {
        byte[] buffer = Combine(
            BitConverter.GetBytes(HANDSHAKE),
            BitConverter.GetBytes(SET_USERNAME),
            Encoding.ASCII.GetBytes(username + "\0") // strings must be null-terminated.
        );
        SendAsyncByteStream(buffer);

        EnqueueCallback(SET_USERNAME, callback);
    }

    public void SendSetLocation(Vector3 location)
    {
        byte[] buffer = Combine(
            BitConverter.GetBytes(HANDSHAKE),
            BitConverter.GetBytes(SET_LOCATION),
            BitConverter.GetBytes(location.x),
            BitConverter.GetBytes(location.y),
            BitConverter.GetBytes(location.z)
        );
        SendAsyncByteStream(buffer);
    }

    public void SetSetLocation(NetworkDelegate callback)
    {
        SetSingletonCallback(SET_LOCATION, callback);
    }
}

// TODO Create Packet Tasks
// These classes are containers that hold data parsed from packets.
public class GuidResponse : NetworkedResponse
{
    public Guid Guid { get; set; }
}

public class StatusResponse : NetworkedResponse
{
    public byte Status { get; set; }
}

public class CreateRoomResponse : NetworkedResponse
{
    public string RoomCode { get; set; }
}

public class SetUsernameResponse : NetworkedResponse
{
    public string Username { get; set; }
}

public class RoomInfoResponse : StatusResponse
{
    public string RoomCode { get; set; }
    public Guid[] Guids { get; set; }
    public string[] Usernames { get; set; }
}

public class SetLocationResponse : GuidResponse
{
    public Vector3 Location { get; set; }
}
