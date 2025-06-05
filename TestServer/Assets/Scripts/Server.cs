using System;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Threading.Tasks;
using UnityEngine;

// TCP server setup in Unity
public class Server : MonoBehaviour
{

    // TCP listener object
    private TcpListener server;

    // Server running flag
    private bool running = true;

    // Current connected client
    // This is the watch
    private TcpClient currentClient;

    // Stream for client
    // This is where we write data
    private NetworkStream currentStream;

    // Lock object for thread safety
    private object streamLock = new object();

    // Specify port
    public int port = 56411;

    // for testing
    private float timer = 0f;



    void Start() {
    
        // Start server
        StartServer();
    }

    // this is just for testing
    void Update() {
        timer += Time.deltaTime;
        if (timer >= 2f) {
            SendMessageToClient("Hello, World!");
            timer = 0f;
        }
    }

    void OnApplicationQuit() {

        // Stop server and cleanup on exit
        running = false;
        server?.Stop();


        currentStream?.Close();
        currentClient?.Close();
        currentStream = null;
        currentClient = null;
    }

    async void StartServer() {

        // Bind server to everything on specified port
        server = new TcpListener(IPAddress.Any, port);
        server.Start();

        Debug.Log("Server listening on port " + port);

        while (running) {
            // Wait for incoming client connection
            TcpClient client = await server.AcceptTcpClientAsync();

            // Set current client and stream
            currentClient = client;
            currentStream = client.GetStream();

            Debug.Log("Client connected");


            // handle client in background
            _ = HandleClient(client, currentStream);

        }

    }

    // Waits for the client to disconnect
    async Task HandleClient(TcpClient client, NetworkStream stream) {

        try {

            // This is basically just checking to see
            // if the client is still connected
            byte[] buffer = new byte[1024];
            int bytesRead = await stream.ReadAsync(buffer, 0, buffer.Length);

        } catch (Exception e) {

            Debug.Log("Client exception: " + e);
        
        }
    }

    // Send message to the currently connected client
    public async void SendMessageToClient(string message) {

        if (currentStream != null && currentClient != null && currentClient.Connected) {

            // needs a new line character or the message will not be read properly
            // since on android we call "readLine"
            Debug.Log("Attempting to send message: " + message);
            byte[] data = Encoding.UTF8.GetBytes(message+'\n');
            try {

                await currentStream.WriteAsync(data, 0, data.Length);
                Debug.Log("Message sent: " + message);
            
            } catch (Exception e) {
                Debug.Log("Send failed: " + e);
            }

        } else {
            Debug.Log("No connected client");
        }
    }
}