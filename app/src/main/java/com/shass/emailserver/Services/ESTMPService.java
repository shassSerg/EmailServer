package com.shass.emailserver.Services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

import com.shass.emailserver.DBHelper;
import com.shass.emailserver.EmailServer.DataMail;
import com.shass.emailserver.EmailServer.EmailServer;
import com.shass.emailserver.MainActivity;
import com.shass.emailserver.R;

import java.net.Socket;


public class ESTMPService extends Service {

    private EmailServer server;
    private NotificationManager nm;
    private static int NOTIFICATION_ID=0;


    private void onEndLoadServer(Exception e){
        Intent intent = new Intent(MainActivity.END_LOAD_SERVER_EVENT);
        if (e!=null)
        intent.putExtra(MainActivity.MESSAGE_TAG,e.getLocalizedMessage() );
        sendBroadcast(intent);
    }

    private final IBinder mBinder = new ESTMPBinder();

    public class ESTMPBinder extends Binder {
        public ESTMPService getService() {
            return ESTMPService.this;
        }
    }

    public boolean isActive(){
        return this.server!=null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private void showNotificate(){
        Intent resultIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent=PendingIntent.getActivity(getApplicationContext(),1,resultIntent,PendingIntent.FLAG_UPDATE_CURRENT);

        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification n  = new Notification.Builder(this)
                .setContentTitle("ESTMP")
                .setContentText("Server is active!")
                .setSmallIcon(android.R.drawable.presence_online)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .getNotification();
        nm.notify(NOTIFICATION_ID, n);

    }

    //DB
    DBHelper mDbHelper;
    SQLiteDatabase db;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        try{
            registerReceiver();
            mDbHelper = new DBHelper(getApplicationContext());
            db = mDbHelper.getWritableDatabase();

        Bundle extras = intent.getExtras();
        final boolean ssl=extras.getBoolean(getString(R.string.SETTINGS_SSL));

        final String domain=extras.getString(getString(R.string.SETTINGS_DOMAIN));
        final int port=extras.getInt(getString(R.string.SETTINGS_PORT));

        if (ssl){
            final String path=extras.getString(getString(R.string.SETTINGS_PATH));
            final String pass=extras.getString(getString(R.string.SETTINGS_PASS));
            final int port_ssl=extras.getInt(getString(R.string.SETTINGS_PORT_SSL));

            new Thread(new Runnable() {
                @Override
                public void run() {
                    Exception exception=null;
                    try {
                        server=new EmailServer(domain,port,port_ssl,path,pass );
                        server.setServerListener(new EmailServer.OnServerListener() {
                            @Override
                            public void onConnectedToServer(final Socket client) {
                                mDbHelper.addConnected( client.getInetAddress().getHostAddress());
                            }

                            @Override
                            public void onReceivedMail(final Socket socket,final DataMail data) {
                                mDbHelper.addMail(socket,data,true);
                            }

                            @Override
                            public void onEndSendMail(final Socket socket,final DataMail data) {
                                mDbHelper.addMail( socket,data,false);
                            }

                            @Override
                            public boolean onValidateUser(String name, String password) {
                                return true;
                            }

                            @Override
                            public boolean onValidateMailFrom(String _mailFrom) {
                                return true;
                            }

                            @Override
                            public boolean onValidateRcptTo(String _rcptTo) {
                                return true;
                            }

                            @Override
                            public boolean onValidateMail(String data) {
                                return true;
                            }
                        });
                        server.start();

                        showNotificate();
                    }catch (Exception e){
                        exception=e;
                        stopAndDestroyServer();
                    }

                    onEndLoadServer(exception);
                }
            }).start();


        }else {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Exception exception = null;
                    try {
                        server = new EmailServer(domain, port);
                        server.setServerListener(new EmailServer.OnServerListener() {
                            @Override
                            public void onConnectedToServer(final Socket client) {
                                mDbHelper.addConnected( client.getInetAddress().getHostAddress());
                            }

                            @Override
                            public void onReceivedMail(final Socket socket,final DataMail data) {
                                mDbHelper.addMail(socket,data,true);
                            }

                            @Override
                            public void onEndSendMail(final Socket socket,final DataMail data) {
                                mDbHelper.addMail( socket,data,false);
                            }

                            @Override
                            public boolean onValidateUser(String name, String password) {
                                return true;
                            }

                            @Override
                            public boolean onValidateMailFrom(String _mailFrom) {
                                return true;
                            }

                            @Override
                            public boolean onValidateRcptTo(String _rcptTo) {
                                return true;
                            }

                            @Override
                            public boolean onValidateMail(String data) {
                                return true;
                            }
                        });

                        server.start();

                        showNotificate();
                    } catch (Exception e) {
                        exception = e;
                        stopAndDestroyServer();
                    }
                    onEndLoadServer(exception);
                }
            }).start();

        }
        }catch (Exception e){
            onEndLoadServer(e);
        }
        return START_STICKY;
    }

    public void stopAndDestroyServer() {
        try {
            this.server.stop();
        }catch (Exception e){
        }finally {
            this.server=null;
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            nm.cancel(NOTIFICATION_ID);
            if (db!=null) db.close();
            if(estmpReceiver != null)
                unregisterReceiver(estmpReceiver);
            this.server.stop();
            this.server=null;
        }catch (Exception e){}
    }
    private BroadcastReceiver estmpReceiver;
    private void registerReceiver() {
        estmpReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, final Intent _intent) {
                if (_intent.getAction().equals(MainActivity.SENT_MAIL_SERVER_EVENT)) {
                    try{
                        DataMail dm=new DataMail();
                        String domain=_intent.getExtras().getString(getString(R.string.MAIL_DOMAIN));
                        String from=_intent.getExtras().getString(getString(R.string.MAIL_FROM));
                        String to=_intent.getExtras().getString(getString(R.string.MAIL_TO));
                        String subject=_intent.getExtras().getString(getString(R.string.MAIL_SUBJECT));
                        String body=_intent.getExtras().getString(getString(R.string.MAIL_BODY));
                        dm.setMAIL_FROM(from);
                        dm.setRCPT_TO(to);
                        dm.setSubject(subject);
                        dm.setBody(body);
                        dm.upDate();
                        dm.setCharSet("UTF-8");

                        server.send(domain, dm);
                    }catch (Exception e){}
                }
            }
        };
        registerReceiver(estmpReceiver, new IntentFilter(MainActivity.SENT_MAIL_SERVER_EVENT));
    }
}