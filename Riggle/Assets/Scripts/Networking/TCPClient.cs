using System;
using System.Collections;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Threading;
using UnityEngine;
using UnityEngine.UI;

// This is an abstraction class. You might be seeking SlimeClient if you're looking to make modifications.
public abstract class TCPClient : MonoBehaviour
{
    public string host = "127.0.0.1";
    public int port = 25575;

    public delegate void NetworkDelegate(NetworkedResponse response);

    private Socket socket;
    private ConcurrentQueue<NetworkedResponse> responseQueue;
    private Thread thread;
    private List<Queue<NetworkDelegate>> callbacks;

    // Start is called before the first frame update
    public void Awake()
    {
        // Get our callback queues ready to match requests to callbacks
        this.callbacks = new List<Queue<NetworkDelegate>>();

        // Get our async queue ready to receive and process packets
        this.responseQueue = new ConcurrentQueue<NetworkedResponse>();
    }

    // Tries to connect to the server. Returns true if successful
    public bool TryConnect()
    {
        if(IsConnected())
            return true;

        // Connect to the server
        IPHostEntry ipHostInfo = Dns.GetHostEntry(host);
        IPAddress ipAddress = ipHostInfo.AddressList[0];
        IPEndPoint remoteEndpoint = new IPEndPoint(ipAddress, port);

        this.socket = new Socket(ipAddress.AddressFamily, SocketType.Stream, ProtocolType.Tcp);
        socket.Blocking = true; // Block on startup, ensure valid connection
        try
        {
            socket.Connect(remoteEndpoint);
        }
        catch (Exception e)
        {
            Debug.Log("Connection to server failed: " + e.Message);
            socket = null;
            return false;
        }

        Debug.Log("Connected to: " + remoteEndpoint.Address + ":" + remoteEndpoint.Port);

        // Begin listening for data packets
        thread = new Thread(ListenAsycForData);
        thread.Priority = System.Threading.ThreadPriority.AboveNormal;
        thread.IsBackground = true; // This kills the thread when the game is quit
        thread.Start();

        return true;
    }

    // Disconnects the client from the server.
    public void Disconnect()
    {
        if (thread != null)
        {
            thread.Interrupt();
            thread.Abort();
            thread = null;
        }

        if (socket != null)
        {
            socket.Disconnect(false);
            socket = null;
        }
    }

    // Returns true if a server connection is ongoing.
    public bool IsConnected()
    {
        return socket != null && socket.Connected;
    }

    public const short HANDSHAKE = ~(0);   
    // Listen for packets
    private void ListenAsycForData()
    {
        while (IsConnected()) // On a different thread, so this won't block main thread
        {
            byte[] handshakeArr = BlockReceive(2); // Get 2 bytes
            short handshake = BitConverter.ToInt16(handshakeArr, 0); // Convert to short
            while (handshake != HANDSHAKE) // Is it our handshake? (all 1s)
            { // No, so we're not in sync with the server (super rare/almost never happens/TCP failure), get back on track.
                Debug.Log("Correcting offset handshake: " + handshake);

                // Offset our bit stream by one byte. See if we can locate the next handshake.
                handshakeArr[0] = handshakeArr[1];
                handshakeArr[1] = BlockReceive(1)[0];
                handshake = BitConverter.ToInt16(handshakeArr, 0);
            }

            // We passed the handshake and are synced with the server. Let's process the incoming packet.
            NetworkedResponse response = ParseIncomingPacket();
            if(response != null)
                responseQueue.Enqueue(response);
        }
    }

    // Process our task queue on the game thread. This should occurr in a fixed interval. This is thread safe.
    public void FixedUpdate()
    {
        NetworkedResponse response;
        while (responseQueue.TryDequeue(out response)) // Is there a task to process? 
        {
            Queue<NetworkDelegate> queue = callbacks[response.Code]; // Get it's respective callback queue.
            if (queue.Count > 0) // Is there someone waiting for the data?
                queue.Dequeue()(response); // Give it to them. Call the callback.
        }
    }

    // Someone killed the TCPClient. Clean up and close up.
    public void OnDestroy()
    {
        Disconnect();
    }

    // Someone killed the TCPClient. Clean up and close up.
    public void OnApplicationQuit()
    {
        Disconnect();
    }

    // C# has a nasty property where it returns from Socker#Receive too early with too little data. This is a fix for that.
    protected byte[] BlockReceive(int bytes)
    {
        while (socket.Available < bytes);

        byte[] buffer = new byte[bytes];
        socket.Receive(buffer, bytes, SocketFlags.None);
        return buffer;
    }

    // This converts a bit stream into a String terminated by a null terminator (\0).
    protected String StringFromStream()
    {
        byte[] byteBuffer = new byte[1];
        socket.Receive(byteBuffer, 1, SocketFlags.None);
        if (byteBuffer[0] == 0)
            return "";

        StringBuilder sb = new StringBuilder();
        do
        {
            sb.Append((char)byteBuffer[0]);
            socket.Receive(byteBuffer, 1, SocketFlags.None);
        } while (byteBuffer[0] != 0);

        return sb.ToString();
    }

    // This lets us send bytes to the server. It is thread safe.
    protected void SendAsyncByteStream(byte[] byteData)
    {
        socket.BeginSend(byteData, 0, byteData.Length, 0, new AsyncCallback((ar) => {
            socket.EndSend(ar);
        }), socket);
    }

    // Combines byte arrays into one byte array
    protected byte[] Combine(params byte[][] arrays)
    {
        byte[] rv = new byte[arrays.Sum(a => a.Length)];
        int offset = 0;
        foreach (byte[] array in arrays)
        {
            Buffer.BlockCopy(array, 0, rv, offset, array.Length);
            offset += array.Length;
        }
        return rv;
    }

    protected void EnqueueRequestCallback(short request, NetworkDelegate callback)
    {
        for(int i=callbacks.Count; i<=request; i++)
            callbacks.Add(new Queue<NetworkDelegate>());
        callbacks[request].Enqueue(callback);
    }

    // Parse incoming packets into a form we can use later. This is async.
    public abstract NetworkedResponse ParseIncomingPacket();
}

public abstract class NetworkedResponse
{
    public short Code { get; set; }
}
