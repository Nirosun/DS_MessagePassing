package com.ds18842.meetmenow.locationtest;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;

import com.ds18842.meetmenow.locationtest.logic.GeoLocationManager;
import com.ds18842.meetmenow.locationtest.logic.LogicManager;
import com.ds18842.meetmenow.locationtest.network.NetworkManager;
import com.ds18842.meetmenow.locationtest.network.PeerManager;
import com.ds18842.meetmenow.locationtest.network.WiFiDirectBroadcastReceiver;
import com.ds18842.meetmenow.locationtest.routing.RoutingManager;

public class MeetMeNow extends Application{
    private GeoLocationManager geoLocationManager;
    private LogicManager logicManager;
    private NetworkManager networkManager;
    private RoutingManager routingManager;

    private PeerManager peerManager;

    private final IntentFilter intentFilter = new IntentFilter();
    private BroadcastReceiver receiver = null;

    public MeetMeNow(){

    }

    public GeoLocationManager getGeoLocationProvider() {
        return geoLocationManager;
    }


    @Override
    public void onCreate() {
        super.onCreate();

        logicManager = new LogicManager(this);
        //geoLocationManager = new GeoLocationManager(this);

        networkManager = new NetworkManager(this, logicManager, peerManager);
        routingManager = new RoutingManager(this, logicManager, peerManager);
        peerManager = new PeerManager(this);

        //========================================= Set Location Handler
        //geoLocationManager.setLocationHandler(logicManager);
        //========================================= Set Upstream Message
        peerManager.setReceiver(networkManager);
        networkManager.setReceiver(routingManager);
        routingManager.setReceiver(logicManager);
        //========================================= Set Downstream Message
        logicManager.setSender(routingManager);
        routingManager.setSender(networkManager);

        peerManager.discoverPeers();
        //peerManager.exchangeWithNeighbors();



    }
}
