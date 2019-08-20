package com.shass.emailserver;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.shass.emailserver.EmailServer.DataMail;
import com.shass.emailserver.Tables.Connected;
import com.shass.emailserver.Tables.Received;

import java.net.Socket;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {
    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_FIRST =
            "CREATE TABLE IF NOT EXISTS  " + Connected.TABLE_NAME + " (" +
                    "'"+Connected.COLUMN_NAME_ID+"'" + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "'"+Connected.COLUMN_NAME_IP+"'" + TEXT_TYPE + COMMA_SEP +
                    "'"+Connected.COLUMN_NAME_DATE+"' INTEGER );";
    private static final String SQL_CREATE_SECOND =
            " CREATE TABLE IF NOT EXISTS "+Received.TABLE_NAME + " (" +
                    "'"+Received.COLUMN_NAME_ID +"'"+ " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "'"+Received.COLUMN_NAME_DATE +"' INTEGER" + COMMA_SEP +
                    "'"+Received.COLUMN_NAME_SUBJECT+"'" + TEXT_TYPE + COMMA_SEP +
                    "'"+Received.COLUMN_NAME_BODY+"'" + TEXT_TYPE + COMMA_SEP +
                    "'"+Received.COLUMN_NAME_FROM+"'" + TEXT_TYPE + COMMA_SEP +
                    "'"+Received.COLUMN_NAME_TO+"'" + TEXT_TYPE + COMMA_SEP +
                    "'"+Received.COLUMN_NAME_IP+"'" + TEXT_TYPE + COMMA_SEP +
                    "'"+Received.COLUMN_NAME_TYPE+"'" +" INTEGER );";
            ;


    private static final String SQL_DELETE_FIRST =
            " DROP TABLE IF EXISTS " + Connected.TABLE_NAME +";";
    private static final String SQL_DELETE_SECOND =
            " DROP TABLE IF EXISTS " + Received.TABLE_NAME +";" ;


    public static final int DATABASE_VERSION = 18;
    public static final String DATABASE_NAME = "DataBase.db";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_FIRST);
        db.execSQL(SQL_CREATE_SECOND);
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_FIRST);
        db.execSQL(SQL_DELETE_SECOND);
        onCreate(db);
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }


    public long getCountConnected(){
        SQLiteDatabase db = this.getReadableDatabase();
        long count = DatabaseUtils.queryNumEntries(db, Connected.TABLE_NAME);
        return count;
    }

    public long getCountReceived(){
        SQLiteDatabase db = this.getReadableDatabase();
        long count = DatabaseUtils.queryNumEntries(db, Received.TABLE_NAME);
        return count;
    }

    public long getSentCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        long count = DatabaseUtils.queryNumEntries(db, Received.TABLE_NAME,Received.COLUMN_NAME_TYPE+"=0");
        return count;
    }
    public long getReceivedCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        long count = DatabaseUtils.queryNumEntries(db, Received.TABLE_NAME,Received.COLUMN_NAME_TYPE+"<>0");
        return count;
    }

    public long addConnected(String ip){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues initialValues = new ContentValues();

        initialValues.put("'"+Connected.COLUMN_NAME_IP+"'",ip );
        initialValues.put("'"+Connected.COLUMN_NAME_DATE+"'",(int)(System.currentTimeMillis()/1000));

        return db.insert(Connected.TABLE_NAME, null, initialValues);
    }

    public long addMail(final Socket socket, final DataMail mail, boolean type){
        SQLiteDatabase db = this.getWritableDatabase();

        if (mail.getDate()==0)
            mail.upDate();

        ContentValues initialValues = new ContentValues();

        initialValues.put("'"+Received.COLUMN_NAME_SUBJECT+"'",mail.getSubject());
        initialValues.put("'"+Received.COLUMN_NAME_BODY+"'",mail.getBody());
        initialValues.put("'"+Received.COLUMN_NAME_FROM+"'",mail.getMAIL_FROM());
        initialValues.put("'"+Received.COLUMN_NAME_TO+"'",mail.getRCPT_TO());
        initialValues.put("'"+Received.COLUMN_NAME_DATE+"'",(int)(mail.getDate()/1000));
        initialValues.put("'"+Received.COLUMN_NAME_IP+"'",socket.getInetAddress().getHostName());
        initialValues.put("'"+Received.COLUMN_NAME_TYPE+"'",(type?1:0));

        return db.insert(Received.TABLE_NAME, null, initialValues);
    }


    public List<Connected> getConnected() {
        List<Connected> connectedList = new ArrayList<>();
        String selectQuery = "SELECT  * FROM "+Connected.TABLE_NAME+" ORDER BY "+Connected.COLUMN_NAME_DATE+" DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        SimpleDateFormat format=new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");
        if (cursor.moveToFirst()) {
            do {
                long count = DatabaseUtils.queryNumEntries(db, Connected.TABLE_NAME+" WHERE "+Connected.COLUMN_NAME_IP+"='"+cursor.getString(1)+"'");
                Connected connected = new Connected(cursor.getInt(0), count, cursor.getString(1), format.format(new Date((long)cursor.getInt(2)*(long)1000)));
                connectedList.add(connected);
            } while (cursor.moveToNext());
        }

        return connectedList;
    }


    public void clearConnected(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM "+Connected.TABLE_NAME);
    }
    public void clearMail(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM "+Received.TABLE_NAME);
    }

    public List<Received> getMail() {
        List<Received> receivedList = new ArrayList<>();
        String selectQuery = "SELECT  * FROM "+Received.TABLE_NAME+" ORDER BY "+Received.COLUMN_NAME_DATE+" DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        SimpleDateFormat format=new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");

        if (cursor.moveToFirst()) {
            do {
                Received received = new Received(cursor.getInt(0), format.format(new Date((long)cursor.getInt(1)*(long)1000)), cursor.getString(2), cursor.getString(3),cursor.getString(4),cursor.getString(5),cursor.getString(6),cursor.getInt(7));
                receivedList.add(received);
            } while (cursor.moveToNext());
        }

        return receivedList;
    }



}