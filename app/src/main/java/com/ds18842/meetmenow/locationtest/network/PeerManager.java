package com.ds18842.meetmenow.locationtest.network;

import com.ds18842.meetmenow.locationtest.common.IMessageHandler;
import com.ds18842.meetmenow.locationtest.common.Node;
import com.ds18842.meetmenow.locationtest.common.Packet;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.util.Log;

public class PeerManager /*extends BroadcastReceiver*/ implements PeerListListener, WifiP2pManager.ConnectionInfoListener {
    //TODO Discovery
    //TODO establish connection : exchange location
    //TODO after exchanging location, add that to list of neighbors and update logic about the new node
    //TODO keep list of neighbours updated

    private final Context context;

    private List<WifiP2pDevice> peers;
    //private HashMap<String, Node> ipToNodes;

    //private ArrayList<Neighbour> neighbours;

    private ArrayList<Node> neighbours;

    private WifiP2pDevice device;
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private WifiP2pInfo info;
    private WiFiDirectBroadcastReceiver broadcastReceiver;
    private IMessageHandler receiver;


    private final IntentFilter intentFilter = new IntentFilter();

    //private WifiP2pManager.ConnectionInfoListener connListener;

    //private AddrCapsule destAddr;

    private Node me;

    public static String TAG = "PeerManager";
    private static final int SOCKET_TIMEOUT = 5000;

    private int SERVER_PORT = 8988;

    public static final int NEW = 0, EXCHANGE = 1, NORMAL = 2, SENDING = 3;
    private int state = NEW;
    //private boolean


    private boolean success = false;

    //public static boolean peerSuccess = false;

    public final Semaphore connSem = new Semaphore(1);
    //public final ReentrantLock connLock = new ReentrantLock();

    public PeerManager(Context context) {
        this.context = context;

        peers = new ArrayList<>();

        neighbours = new ArrayList<>();
        //ipToNodes = new HashMap<>();
        manager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(context, context.getMainLooper(), null);

        broadcastReceiver = new WiFiDirectBroadcastReceiver(manager, channel, this);

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        context.registerReceiver(broadcastReceiver, intentFilter);

        //connListener = new WifiP2pManager.

        //state = NEW;
    }

    /*public ArrayList<Neighbour> getNeighbours() {
        //TODO return all neighbour
        return this.neighbours;
    }

    public Neighbour getNeighbour(Node next) {
        //TODO return neighbour from node

        for (Neighbour neighbour : neighbours) {
            if (neighbour.getNode().getName().equals(next.getName())) {
                return neighbour;
            }
        }

        return null ;
    }*/

    public ArrayList<Node> getNeighbours() {
        //TODO return all neighbour
        return this.neighbours;
    }

    public Node getNeighbour(Node next) {
        //TODO return neighbour from node

        for (Node neighbour : neighbours) {
            if (neighbour.getName().equals(next.getName())) {
                return neighbour;
            }
        }

        return null ;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public void setReceiver(IMessageHandler receiver) {
        this.receiver = receiver;
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerList) {
        if (state == EXCHANGE) return;

        Log.d(TAG, "Enter onPeersAvailable");
        peers.clear();
        peers.addAll(peerList.getDeviceList());
        if (peers.size() == 0) {
            Log.d(TAG, "No devices found");

            if (state == NEW) {
                state = NORMAL;
            }

            return;
        }

        Log.d(TAG, "Peers: " + peers.size());

        if (state == NEW) {
            state = EXCHANGE;
            Thread thread = new Thread() {
                public void run() {
                    Log.d(TAG, "Before exchange");
                    exchangeWithPeers();
                }
            };
            thread.start();
        }

    }

    /**
     * Update UI for this device.
     *
     * @param device WifiP2pDevice object
     */
    public void updateThisDevice(WifiP2pDevice device) {
        this.device = device;

    }


    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {

        Log.d(TAG, "Enter onConnectionInfo");
        Log.d(TAG, "isGroup:" + String.valueOf(info.groupFormed));

        this.info = info;

        if (info.groupFormed) {

            //String ownerAddr = info.groupOwnerAddress.getHostName();

            if (state == EXCHANGE) {

                if (info.isGroupOwner) {
                    newNodeOwnerOperation();
                }
                else {
                    newNodeClientOperation();
                }
            }
            else if (state == NORMAL) {
                if (info.isGroupOwner) {
                    normalNodeOwnerOperation();
                }
                else {
                    normalNodeClientOperation();
                }
            }

            /*if (info.isGroupOwner) {
                Log.d(TAG, "is group owner");
                Thread server = new Thread() {
                    public void run() {
                        try {
                            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
                            Socket clientSocket = serverSocket.accept();
                            Log.d(TAG, clientSocket.getInetAddress().getHostAddress());

                            InputStream in = clientSocket.getInputStream();
                            ObjectInputStream inputStream = new ObjectInputStream(in);
                            Packet inPacket = (Packet) inputStream.readObject();

                            //in.close();
                            //inputStream.close();

                            //Toast.makeText((MainActivity) context, inPacket.getPayload(),
                            //        Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Receive: " + inPacket.getPayload());

                            OutputStream out = clientSocket.getOutputStream();
                            ObjectOutputStream outputStream = new ObjectOutputStream(out);
                            Packet outPacket = new Packet(null, null, Packet.EXCHANGE, "World!");

                            outputStream.writeObject(outPacket);

                            Log.d(TAG, "Sending World!");

                            in.close();
                            inputStream.close();
                            serverSocket.close();
                            clientSocket.close();

                        }
                        catch(Exception e) {
                            e.printStackTrace();
                        }

                        connSem.release();
                    }
                };
                server.start();

            }
            else {
                Log.d(TAG, "not group owner");
                try {
                   Thread.sleep(2000);
                }
                catch(Exception e) {
                    e.printStackTrace();
                }

                Thread client = new Thread() {
                    public void run() {

                        try {
                            Socket socket = new Socket();
                            String host = info.groupOwnerAddress.getHostAddress();
                            int port = SERVER_PORT;

                            Log.d(TAG, "Opening client socket - ");
                            socket.bind(null);
                            socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);

                            Log.d(TAG, "Client socket - " + socket.isConnected());
                            OutputStream out = socket.getOutputStream();
                            ObjectOutputStream outStream = new ObjectOutputStream(out);

                            Packet outPacket = new Packet(null, null, Packet.INITIAL_EXCHANGE, "Hello");

                            outStream.writeObject(outPacket);

                            Log.d(TAG, "Sending Hello");

                            //out.close();
                            //outStream.close();

                            InputStream in = socket.getInputStream();
                            ObjectInputStream inStream = new ObjectInputStream(in);

                            Packet inPacket = (Packet) inStream.readObject();

                            //Toast.makeText(context, inPacket.getPayload(),
                            //        Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Receive: " + inPacket.getPayload());

                            in.close();
                            inStream.close();
                            out.close();
                            outStream.close();

                            socket.close();

                            Log.d(TAG, "Before release connSem");



                            connSem.release();
                        }
                        catch(Exception e) {
                            e.printStackTrace();
                        }

                    }
                };
                client.start();


            }*/
        }

        Log.d(TAG, "Leave onConnInfo");
    }


    public void newNodeOwnerOperation() {
        Log.d(TAG, "New Node Owner");
        Thread server = new Thread() {
            public void run() {
                try {
                    ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
                    Socket clientSocket = serverSocket.accept();
                    Log.d(TAG, clientSocket.getInetAddress().getHostAddress());

                    InputStream in = clientSocket.getInputStream();
                    ObjectInputStream inputStream = new ObjectInputStream(in);
                    Packet initPacket = (Packet) inputStream.readObject();

                    Log.d(TAG, "Receive: " + initPacket.getPayload());

                    OutputStream out = clientSocket.getOutputStream();
                    ObjectOutputStream outputStream = new ObjectOutputStream(out);
                    Node src = new Node(device.deviceName, null, device.deviceAddress);
                    Packet outPacket = new Packet(src, null, Packet.EXCHANGE, "New Location");

                    outputStream.writeObject(outPacket);

                    Log.d(TAG, "Sending Location");

                    Packet rePacket = (Packet) inputStream.readObject();

                    neighbours.add(rePacket.getSrc());

                    Log.d(TAG, "Receive: " + rePacket.getPayload());

                    in.close();
                    inputStream.close();
                    serverSocket.close();
                    clientSocket.close();

                }
                catch(Exception e) {
                    e.printStackTrace();
                }

                for (Node node : neighbours) {
                    Log.d(TAG, node.getName() + " " + node.getAddress());
                }

                Log.d(TAG, "Before release connSem");

                connSem.release();
            }
        };
        server.start();
    }


    public void newNodeClientOperation() {
        Log.d(TAG, "New Node Client");
        try {
            Thread.sleep(2000);
        }
        catch(Exception e) {
            e.printStackTrace();
        }

        Thread client = new Thread() {
            public void run() {

                try {
                    Socket socket = new Socket();
                    String host = info.groupOwnerAddress.getHostAddress();
                    int port = SERVER_PORT;

                    Log.d(TAG, "Opening client socket - ");
                    socket.bind(null);
                    socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);

                    Log.d(TAG, "Client socket - " + socket.isConnected());
                    OutputStream out = socket.getOutputStream();
                    ObjectOutputStream outStream = new ObjectOutputStream(out);

                    Node src = new Node(device.deviceName, null, device.deviceAddress);
                    Packet outPacket = new Packet(src, null, Packet.EXCHANGE, "New Location");

                    outStream.writeObject(outPacket);

                    Log.d(TAG, "Sending Location");

                    InputStream in = socket.getInputStream();
                    ObjectInputStream inStream = new ObjectInputStream(in);

                    Packet inPacket = (Packet) inStream.readObject();
                    neighbours.add(inPacket.getSrc());


                    //Toast.makeText(context, inPacket.getPayload(),
                    //        Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Receive: " + inPacket.getPayload());

                    in.close();
                    inStream.close();
                    out.close();
                    outStream.close();

                    socket.close();

                    for (Node node : neighbours) {
                        Log.d(TAG, node.getName() + " " + node.getAddress());
                    }

                    Log.d(TAG, "Before release connSem");

                    connSem.release();
                }
                catch(Exception e) {
                    e.printStackTrace();
                }


            }
        };
        client.start();

    }


    public void normalNodeOwnerOperation() {
        Log.d(TAG, "Old Node Owner");
        Thread server = new Thread() {
            public void run() {
                try {
                    ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
                    Socket clientSocket = serverSocket.accept();
                    Log.d(TAG, clientSocket.getInetAddress().getHostAddress());

                    InputStream in = clientSocket.getInputStream();
                    ObjectInputStream inputStream = new ObjectInputStream(in);
                    Packet inPacket = (Packet) inputStream.readObject();

                    neighbours.add(inPacket.getSrc());

                    Log.d(TAG, "Receive: " + inPacket.getPayload());

                    OutputStream out = clientSocket.getOutputStream();
                    ObjectOutputStream outputStream = new ObjectOutputStream(out);
                    Node src = new Node(device.deviceName, null, device.deviceAddress);
                    Packet outPacket = new Packet(src, null, Packet.EXCHANGE, "Old Location");

                    outputStream.writeObject(outPacket);

                    Log.d(TAG, "Sending Location");

                    in.close();
                    inputStream.close();
                    serverSocket.close();
                    clientSocket.close();

                }
                catch(Exception e) {
                    e.printStackTrace();
                }

                for (Node node : neighbours) {
                    Log.d(TAG, node.getName() + " " + node.getAddress());
                }

                Log.d(TAG, "Server exit");
            }
        };
        server.start();
    }


    public void normalNodeClientOperation() {
        Log.d(TAG, "Old Node Client");
        try {
            Thread.sleep(2000);
        }
        catch(Exception e) {
            e.printStackTrace();
        }

        Thread client = new Thread() {
            public void run() {

                try {
                    Socket socket = new Socket();
                    String host = info.groupOwnerAddress.getHostAddress();
                    int port = SERVER_PORT;

                    Log.d(TAG, "Opening client socket - ");
                    socket.bind(null);
                    socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);

                    Log.d(TAG, "Client socket - " + socket.isConnected());
                    OutputStream out = socket.getOutputStream();
                    ObjectOutputStream outStream = new ObjectOutputStream(out);

                    Packet initPacket = new Packet(null, null, Packet.EXCHANGE, "Initial");

                    outStream.writeObject(initPacket);

                    Log.d(TAG, "Sending Initial Packet");

                    InputStream in = socket.getInputStream();
                    ObjectInputStream inStream = new ObjectInputStream(in);

                    /*while (inStream.available() == 0) {
                        try{
                            Thread.sleep(100);
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }*/
                    Packet inPacket = (Packet) inStream.readObject();

                    neighbours.add(inPacket.getSrc());

                    //Toast.makeText(context, inPacket.getPayload(),
                    //        Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Receive: " + inPacket.getPayload());

                    Node src = new Node(device.deviceName, null, device.deviceAddress);
                    Packet outPacket = new Packet(src, null, Packet.EXCHANGE, "Old Location");

                    outStream.writeObject(outPacket);

                    in.close();
                    inStream.close();
                    out.close();
                    outStream.close();

                    socket.close();

                    for (Node node : neighbours) {
                        Log.d(TAG, node.getName() + " " + node.getAddress());
                    }

                }
                catch(Exception e) {
                    e.printStackTrace();
                }

                Log.d(TAG, "Client exit");

            }
        };
        client.start();
    }


    public void discoverPeers() {

        final PeerManager peerManager = this;
        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Discovery initiated");

                Thread t = new Thread() {
                    public void run() {
                        try {
                            Thread.sleep(5000);
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                        Log.d(TAG, "After initial sleep " + String.valueOf(state));
                        if (state == NEW) {
                            Log.d(TAG, "Request for empty peers");
                            manager.requestPeers(channel, peerManager);
                        }
                    }
                };
                t.start();

            }

            @Override
            public void onFailure(int reasonCode) {
                Log.d(TAG, "Discovery Failed : " + reasonCode);
            }
        });

        Log.d(TAG, "Before requestPeers");

        //manager.requestPeers(channel, this);

    }


    public void exchangeWithPeers() {
        Log.d(TAG, "Enter exchangeWithPeers");

        //while(!peerSuccess);
        //peerSuccess = false;

        for (WifiP2pDevice peerDevice : peers) {

            commWithPeer(new Node(peerDevice.deviceName, null, peerDevice.deviceAddress));
            /*final WifiP2pConfig config = new WifiP2pConfig();
            config.deviceAddress = peerDevice.deviceAddress;
            config.wps.setup = WpsInfo.PBC;

            Log.d(TAG, "Connecting to :" + peerDevice.deviceName);

            Thread thread = new Thread(){
                public void run(){
                    connect(config);
                    Log.d(TAG, "Thread finished");
                }
            };

            thread.start();

            try {
                Log.d(TAG, "Exchange Before sleep");
                Thread.sleep(1000);
                Log.d(TAG, "Exchange After sleep");

            }
            catch (Exception e) {
                e.printStackTrace();
            }

            //connLock.lock();
            try {
                connSem.acquire();
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            Log.d(TAG, "Before disconnect");

            disconnect();*/

        }

        Log.d(TAG, "Loop ended. Setting state to NORMAL");
        state = NORMAL;

    }

    public void commWithPeer(Node node) {
        final WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = node.getAddress();
        config.wps.setup = WpsInfo.PBC;

        Log.d(TAG, "Connecting to :" + node.getName());

        Thread thread = new Thread(){
            public void run(){
                connect(config);
                Log.d(TAG, "Thread finished");
            }
        };

        thread.start();

        try {
            Log.d(TAG, "Exchange Before sleep");
            Thread.sleep(1000);
            Log.d(TAG, "Exchange After sleep");

        }
        catch (Exception e) {
            e.printStackTrace();
        }

        //connLock.lock();
        try {
            connSem.acquire();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        Log.d(TAG, "Before disconnect");

        disconnect();
    }

    public WifiP2pManager getWifiP2pManager() {
        return this.manager;
    }

    public WifiP2pManager.Channel getWifiP2pChannel() {
        return this.channel;
    }


    public void connect(WifiP2pConfig config) {
        Log.d(TAG, "Enter connect");

        try {
           connSem.acquire();
        }
        catch(Exception e) {
            e.printStackTrace();
        }

        Log.d(TAG, "After acquire connSem in connect");

        manager.connect(channel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
                Log.d(TAG, "connect onSuccess.");
            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG, "Connect failed. Retry.");
            }
        });

        Log.d(TAG, "Leaving connect");

    }

    public void disconnect() {
        Log.d(TAG, "Enter disconnect");
        manager.removeGroup(channel, new WifiP2pManager.ActionListener() {

            @Override
            public void onFailure(int reasonCode) {
                Log.d(TAG, "Disconnect failed. Reason :" + reasonCode);

                try {
                    Thread.sleep(1000);
                }
                catch(Exception e) {
                    e.printStackTrace();
                }

                manager.removeGroup(channel, this);
            }

            @Override
            public void onSuccess() {
                Log.d(TAG, "Disconnect succeeded");

                connSem.release();
            }
        });
        Log.d(TAG, "Leave disconnect");
    }


}
