using System.Collections;
using System.Collections.Generic;
using UnityEngine;

// Tell Unity to keep one instance of our networking code.
public class RPersist : MonoBehaviour
{
    public static RPersist persist;

    // Start is called before the first frame update
    void Start()
    {
        if(persist != null)
        {
            Destroy(gameObject);
            return;
        }

        DontDestroyOnLoad(gameObject);
        persist = this;

        // Connect to server.
        RiggleClient client = GetComponent<RiggleClient>();
        client.TryConnect();
    }
}
