package com.ds18842.meetmenow.locationtest.common;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.ArrayList;

public class Packet implements Parcelable, Serializable {
    public static final int MAX_TTL = 20;
    public static final int BROADCAST = 1, LOCATION = 2, EXCHANGE = 3;
    private static int MSG_NUM = 0 ;

    enum PacketType {
        MULTICAST,
        //NORMAL,
        ROUTING,
        HANDSHAKE,
        ACK
    }

    private Node src, dst, prev, next ;
    private int id, type, ttl ;
    private String payload ;

    //private ArrayList<Node> nodes;

    public Packet(Node src, Node dst, int type, String payload /*ArrayList<Node> nodes*/) {
        this.src = src;
        this.prev = src;
        this.dst = dst;
        this.next = dst;
        this.type = type;
        this.ttl = MAX_TTL;
        this.id = MSG_NUM++;
        this.payload = payload;
        //this.nodes = nodes;
    }

    public Packet(Parcel pc){
        this.src = pc.readParcelable(Node.class.getClassLoader()) ;
        this.prev = pc.readParcelable(Node.class.getClassLoader()) ;
        this.dst = pc.readParcelable(Node.class.getClassLoader()) ;
        this.next = pc.readParcelable(Node.class.getClassLoader()) ;



        this.type = pc.readInt() ;
        this.ttl = pc.readInt() ;
        this.id = pc.readInt() ;
        this.payload = pc.readString() ;

        //this.nodes = pc.readArrayList(Node.class.getClassLoader());


    }

    public void setHop(Node prev, Node next) { this.prev = prev; this.next = next; }

    public Node getSrc() { return src; }
    public Node getDst() { return dst; }
    public Node getNext() { return next; }
    public Node getPrev() { return prev; }

    public int getType() { return type; }
    public int getTTL() { return ttl; }
    public int getId() { return id; }

    public String getPayload() {
        return payload;
    }

    /*public ArrayList<Node> getNodes() {
        return nodes;
    }*/


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(src, flags);
        dest.writeParcelable(prev, flags);
        dest.writeParcelable(dst, flags);
        dest.writeParcelable(next, flags);

        dest.writeInt(type);
        dest.writeInt(ttl);
        dest.writeInt(id);
        dest.writeString(payload);

        //dest.writeList(nodes);
    }

    public  String toString()
    {
        return getSrc()+ ":" + getDst()+ ":" + getType() + ":" + getId();
    }
}
