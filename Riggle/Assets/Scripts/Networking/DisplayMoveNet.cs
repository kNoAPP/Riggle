using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class DisplayMoveNet : MonoBehaviour
{

    public RiggleClient client;
    public GameObject playerPrefab;
    public float confirmPlayers = 3f;

    private Dictionary<Guid, GameObject> prefabs;

    void Awake()
    {
        prefabs = new Dictionary<Guid, GameObject>();
    }

    // Start is called before the first frame update
    void Start()
    {
        client.TryConnect();
        client.SetSetLocation((response) =>
        {
            SetLocationResponse setLocationResponse = (SetLocationResponse) response;

            GameObject player;
            if(!prefabs.TryGetValue(setLocationResponse.Guid, out player))
            {
                player = Instantiate(playerPrefab, setLocationResponse.Location, Quaternion.identity);
                prefabs.Add(setLocationResponse.Guid, player);
            }

            player.transform.position = setLocationResponse.Location;
        });

        InvokeRepeating("ConfirmPlayers", confirmPlayers, confirmPlayers);
    }

    public void ConfirmPlayers()
    {
        client.RequestRoomInfo((response) =>
        {
            RoomInfoResponse roomInfoResponse = (RoomInfoResponse) response;
            Dictionary<Guid, GameObject> livingPrefabs = new Dictionary<Guid, GameObject>();
            foreach(Guid guid in roomInfoResponse.Guids)
            {
                GameObject player;
                if(prefabs.TryGetValue(guid, out player))
                {
                    livingPrefabs.Add(guid, player);
                }
            }

            foreach(Guid guid in prefabs.Keys)
            {
                if (livingPrefabs.ContainsKey(guid))
                    continue;

                GameObject player = prefabs[guid];
                Destroy(player);
            }

            prefabs = livingPrefabs;
        });
    }
}
