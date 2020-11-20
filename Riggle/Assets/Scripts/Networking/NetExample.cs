using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;

public class NetExample : MonoBehaviour
{
    public Text text;

    private RiggleClient client;

    private static bool callOnce = false;

    // Start is called before the first frame update
    void Start()
    {
        if (callOnce)
            return;
        callOnce = true;

        client = GameObject.FindWithTag("Network").GetComponent<RiggleClient>();
        if (!client.TryConnect())
            return;

        client.RequestUserId((response) =>
        {
            GuidResponse guidResponse = (GuidResponse) response;
            text.text = guidResponse.Guid.ToString();
            Debug.Log("Got GUID: " + guidResponse.Guid.ToString());
        });

        client.RequestJoinRoom("AAAA", (response) =>
        {
            StatusResponse statusResponse = (StatusResponse) response;
            if (statusResponse.Status == 0x00)
                Debug.Log("Successfully joined room AAAA.");
            else if (statusResponse.Status == 0x01)
                Debug.Log("Tried to join room AAAA, but it doesn't exist.");
            else if (statusResponse.Status == 0x02)
                Debug.Log("Tried to join room AAAA, but it was full.");
        });

        client.RequestLeaveRoom((response) =>
        {
            StatusResponse statusResponse = (StatusResponse) response;
            if (statusResponse.Status == 0x00)
                Debug.Log("Successfully left the current room.");
            else if (statusResponse.Status == 0x01)
                Debug.Log("Tried to leave the room, but apparently I already wasn't in one.");
        });

        client.RequestCreateRoom((response) =>
        {
            CreateRoomResponse createRoomResponse = (CreateRoomResponse) response;
            Debug.Log("I just created the room " + createRoomResponse.RoomCode + " and am now the host!");
        });

        client.RequestSetUsername("Bob", (response) =>
        {
            SetUsernameResponse setUsernameResponse = (SetUsernameResponse) response;
            Debug.Log("I just set my username to: " + setUsernameResponse.Username);
        });

        client.RequestRoomInfo((response) =>
        {
            RoomInfoResponse roomInfoResponse = (RoomInfoResponse) response;
            if(roomInfoResponse.Status == 0x01)
            {
                Debug.Log("You're not in a room!");
                return;
            }

            Debug.Log("Players in your room (" + roomInfoResponse.RoomCode + "): ");
            foreach(string username in roomInfoResponse.Usernames)
                Debug.Log(username);
        });

        Debug.Log("All outgoing packets sent.");
    }
}
