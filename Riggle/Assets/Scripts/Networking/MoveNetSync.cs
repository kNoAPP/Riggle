using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class MoveNetSync : MonoBehaviour
{
    public RiggleClient client;
    public float syncPeriod = 0.1f;

    // Start is called before the first frame update
    void Start()
    {
        client.TryConnect();
        InvokeRepeating("SendPosition", syncPeriod, syncPeriod);
    }

    public void SendPosition()
    {
        client.SendSetLocation(transform.position);
    }
}
