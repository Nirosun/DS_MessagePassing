package com.ds18842.meetmenow.locationtest.network.infrastructure;

import android.net.wifi.p2p.WifiP2pDevice;

import com.ds18842.meetmenow.locationtest.common.* ;

public class Neighbour{
    Node node ;
    WifiP2pDevice device;
    //IDevice device ;
    //ISocket socket ;

    public Neighbour(Node node, WifiP2pDevice device) {
        this.node = node;
        this.device = device;
    }

    public Node getNode() {
        return node;
    }

    public WifiP2pDevice getDevice() {
        return device;
    }

    /*public ISocket getSocket() {
        return socket;
    }*/



}
