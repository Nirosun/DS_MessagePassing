package com.ds18842.meetmenow.locationtest.network;


import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.util.Log;

import com.ds18842.meetmenow.locationtest.common.IMessageHandler;
import com.ds18842.meetmenow.locationtest.common.Packet;
import com.ds18842.meetmenow.locationtest.common.Node;
import com.ds18842.meetmenow.locationtest.logic.LogicManager;
import com.ds18842.meetmenow.locationtest.network.infrastructure.IDevice;
import com.ds18842.meetmenow.locationtest.network.infrastructure.ISocket;
import com.ds18842.meetmenow.locationtest.network.infrastructure.Neighbour;

import java.text.Normalizer;
import java.util.ArrayList;

public class NetworkManager implements IMessageHandler {
    private final Context context;
    private PeerManager peerManager;
    private LogicManager app;
    private Node me;
    private IMessageHandler receiver;
    ArrayList<Node> nodes;

    public static final String TAG = "NetworkManager";

    public NetworkManager(Context context, LogicManager app, PeerManager peerManager){
        this.context = context ;
        this.peerManager = peerManager ;
        this.app = app ;
        this.nodes = new ArrayList<>();
        this.me = app.getSelfNode();
    }

    public void setReceiver(IMessageHandler receiver) { this.receiver = receiver; }

    @Override
    public void receive(Packet msg) {
        if (msg.getType() == Packet.BROADCAST){
            //TODO check if it's a new broadcast to re-do
            //TODO broadcast if #id of packet is new
            //TODO update logic about this broadcast
            this.broadcast(msg);
        }else{
            receiver.receive(msg);
        }
    }

    @Override
    public void send(Packet msg) {
        //TODO reduce TTL by 1

        Node node = msg.getNext() ;
        //Neighbour next = peerManager.getNeighbour(node);
        Node next = peerManager.getNeighbour(node);

        //WifiP2pDevice device = next.getDevice();
        //IDevice device = next.getDevice() ;
        //ISocket socket = next.getSocket();
        //TODO send msg over socket to device

       // NEW--> EXCHANGE--> NORMAL

        // NORMAL --> SEND --> NORMAL

        if (peerManager.getState() != PeerManager.NORMAL) {
            //Log.d(TAG, "Need to be in Normal state when entering this function ");
            Log.d(TAG, "In send: State is not NORMAL, return");
            return;
        }
        peerManager.setState(PeerManager.SENDING);

        Log.d(TAG, "In send: Set state to SENDING");

        peerManager.commWithPeer(next);

        Log.d(TAG, "In send: After commWithPeer");

    }

    @Override
    public void broadcast(Packet msg) {
        /*ArrayList<Neighbour> neighbors = peerManager.getNeighbours() ;
        for (Neighbour neighbor : neighbors) {
            if (msg.getPrev().getName().equals(neighbor.getNode().getName())){
                //Don't broadcast it back to the node you get the message from.
                continue;
            }
            msg.setHop(me, neighbor.getNode());
            send(msg);
        }*/
        ArrayList<Node> neighbors = peerManager.getNeighbours();
        for (Node neighbor : neighbors) {
            if (msg.getPrev().getName().equals(neighbor.getName())){
                //Don't broadcast it back to the node you get the message from.
                continue;
            }
            msg.setHop(me, neighbor);
            send(msg);
        }
    }
}
