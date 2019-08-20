package com.shass.emailserver.Tables;

public class Received {
    public static final String TABLE_NAME = "Received";
    public static final String COLUMN_NAME_ID = "idMail";
    public static final String COLUMN_NAME_DATE = "date";
    public static final String COLUMN_NAME_SUBJECT = "subject";
    public static final String COLUMN_NAME_BODY = "body";
    public static final String COLUMN_NAME_FROM = "from";
    public static final String COLUMN_NAME_TO = "to";
    public static final String COLUMN_NAME_TYPE = "type";
    public static final String COLUMN_NAME_IP = "ip";

    public int id;
    public String date;
    public String subject;
    public String body;
    public String from;
    public String to;
    public String ip;
    public int type;

    public Received(int ID,String _date,String _subject,String _body,String _from,String _to, String _ip, int type){
        this.ip=_ip;
        this.id=ID;
        this.date=_date;
        this.subject=_subject;
        this.body=_body;
        this.from=_from;
        this.to=_to;
        this.type=type;
    }
}
