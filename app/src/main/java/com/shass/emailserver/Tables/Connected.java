package com.shass.emailserver.Tables;

public class Connected {
    public static final String TABLE_NAME = "connected";
    public static final String COLUMN_NAME_ID = "idClient";
    public static final String COLUMN_NAME_IP = "ip";
    public static final String COLUMN_NAME_DATE = "date";

    public String ip;
    public long connected_count;
    public int id;
    public String date;

    public Connected(int ID,long _connected_count,String _ip,String _date){
        this.ip=_ip;
        this.connected_count=_connected_count;
        this.id=ID;
        this.date=_date;
    }
}
