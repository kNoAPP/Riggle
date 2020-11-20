using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class DisplayMoveNet : MonoBehaviour
{

    public RiggleClient client;
    public GameObject playerPrefab;
    public float confirmPlayers = 3f;

    private Dictionary<Guid, PlayerInfo> playerInfo;

    private class PlayerInfo
    {
        public GameObject gameObject { get; set; }
        public Vector3 targetLocation { get; set; }
    }

    void Awake()
    {
        playerInfo = new Dictionary<Guid, PlayerInfo>();
    }

    // Start is called before the first frame update
    void Start()
    {
        client.TryConnect();
        client.SetSetLocation((response) =>
        {
            SetLocationResponse setLocationResponse = (SetLocationResponse) response;

            PlayerInfo player;
            if(!playerInfo.TryGetValue(setLocationResponse.Guid, out player))
            {
                player = new PlayerInfo();
                player.gameObject = Instantiate(playerPrefab, setLocationResponse.Location, Quaternion.identity);
                playerInfo.Add(setLocationResponse.Guid, player);
            }

            player.targetLocation = setLocationResponse.Location;
        });

        InvokeRepeating("ConfirmPlayers", confirmPlayers, confirmPlayers);
    }

    void Update()
    {
        foreach(PlayerInfo player in playerInfo.Values)
        {
            player.gameObject.transform.position = Vector3.Lerp(player.gameObject.transform.position, player.targetLocation, 10*Time.deltaTime);
        }
    }

    public void ConfirmPlayers()
    {
        client.RequestRoomInfo((response) =>
        {
            RoomInfoResponse roomInfoResponse = (RoomInfoResponse) response;
            Dictionary<Guid, PlayerInfo> livingPrefabs = new Dictionary<Guid, PlayerInfo>();
            foreach(Guid guid in roomInfoResponse.Guids)
            {
                PlayerInfo player;
                if(playerInfo.TryGetValue(guid, out player))
                {
                    livingPrefabs.Add(guid, player);
                }
            }

            foreach(Guid guid in playerInfo.Keys)
            {
                if (livingPrefabs.ContainsKey(guid))
                    continue;

                PlayerInfo player = playerInfo[guid];
                Destroy(player.gameObject);
            }

            playerInfo = livingPrefabs;
        });
    }
}
