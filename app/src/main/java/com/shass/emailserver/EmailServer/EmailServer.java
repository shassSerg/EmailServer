package com.shass.emailserver.EmailServer;

import android.net.MailTo;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.StringRes;
import android.util.ArrayMap;
import android.util.Base64;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.CoderMalfunctionError;
import java.nio.charset.UnsupportedCharsetException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


public class EmailServer {
    //параметры сервера
    private String domain=null;

    private ServerSocket socket=null;
    private ServerSocket socketSSL=null;

    private Thread tsocket=null;
    private Thread tsocketSSL=null;

    private SSLSocketFactory sslSocketFactory=null;

    public interface OnServerListener {
        public void onConnectedToServer(final Socket client);
        public void onReceivedMail(final Socket socket,final DataMail data);
        public void onEndSendMail(final Socket socket,final DataMail data);
        public boolean onValidateUser(final String name,final String password);
        public boolean onValidateMailFrom(final String _mailFrom);
        public boolean onValidateRcptTo(final String _rcptTo);
        public boolean onValidateMail(final String data);
    }

    private OnServerListener listener;

    public void setServerListener(OnServerListener listener) {
        this.listener = listener;
    }

    class ServerTask implements Runnable {
        private ServerSocket server;
        private boolean ssl;

        ServerTask(boolean _ssl,ServerSocket _server){
            this.server=_server;
            this.ssl=_ssl;
        }
        public void run() {
            while(true){
                try {
                    Socket client = this.server.accept();
                    Log.d("Client","Connected ["+client.getInetAddress().getHostAddress().toString()+"]");
                    client.setSoTimeout(5000);
                    new Thread(new Client(this.ssl,client)).start();

                    class OnConnectedTask implements Runnable {
                        Socket socket;
                        OnConnectedTask(final Socket _socket){
                            socket=_socket;
                        }
                        @Override
                        public void run() {
                            if (listener!=null)
                                listener.onConnectedToServer(this.socket);
                        }
                    }
                    new Thread(new OnConnectedTask(client)).start();
                }catch (Exception e){
                    Log.d("Server task","Exception: "+e.getLocalizedMessage());
                    try{
                        stop();
                    }finally {
                        return;
                    }

                }
            }
        }
    }


    public EmailServer(final String _domain, int port, int portSSL, final String _pathToCert,final String password) throws IOException,Exception{
        this.listener=null;

            if (DomainUtils.isValidDomainName(_domain))
                domain=_domain;
            else
                throw new Exception("Not valid domain name.");

            KeyStore keyStore = KeyStore.getInstance("PKCS12");//делаем хранилище ключей, аналогичное типу Вашего сертификата
            InputStream in = new BufferedInputStream(new FileInputStream(_pathToCert));// крепим к потоку сам файл сертификата
            try{
                    keyStore.load(in, password.toCharArray());
            } catch (Exception ex) {
                    throw new Exception("Error load KeyStore");
            }finally {
                    in.close();
            }
                KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                kmf.init(keyStore,password.toCharArray());
                KeyManager[] keyManagers = kmf.getKeyManagers();

                //Второй элемент, который "должен" проверять, валидны ли наши сертификаты
                TrustManager[] wrappedTrustManagers = new TrustManager[]{
                        new X509TrustManager() {
                            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                                return null;
                            }

                            public void checkClientTrusted(X509Certificate[] certs, String authType) {
                            }

                            public void checkServerTrusted(X509Certificate[] certs, String authType) {
                            }
                        }
                };
                SSLContext sslContext = SSLContext.getInstance("TLS");//Создаем контекст SSL по типу протокола
                sslContext.init(keyManagers, wrappedTrustManagers, new java.security.SecureRandom());//инициализируем его

            sslSocketFactory = sslContext.getSocketFactory();;

            socket=new ServerSocket(port);
            socketSSL=new ServerSocket(portSSL);

            tsocket=new Thread(new ServerTask(false,socket));
            tsocketSSL=new Thread(new ServerTask(false,socketSSL));
    }

    public EmailServer(final String _domain, int port) throws IOException,Exception{
        this.listener=null;
            if (DomainUtils.isValidDomainName(_domain))
            domain=_domain;
            else throw new Exception("Not valid domain name.");

            socket=new ServerSocket(port);
            tsocket=new Thread(new ServerTask(false,socket));
    }

    public void send(String domain,DataMail mail) throws Exception{
            if (!DomainUtils.isValidDomainName(domain))
                throw new Exception("Not valid domain.");

            if (mail==null || !mail.isReadyToSend())
                throw new Exception("Not valid mail.");

            class SendTask implements Runnable{
                private String domain;
                private DataMail mail;
                SendTask(String _domain,DataMail dm){
                    domain=_domain;
                    mail=dm;
                }
                @Override
                public void run() {
                    try {
                        Socket client = new Socket(domain, 25);
                        client.setSoTimeout(3000);
                        new Thread(new Client(false, client, mail)).start();
                    }catch (Exception e){
                        Log.d("Send task", "Exception: "+e.getLocalizedMessage());
                    }
                }
            }
            new Thread(new SendTask(domain,mail )).start();

    }

    public void start(){
            if (tsocket != null) {
                tsocket.start();
            }
            if (tsocketSSL != null) {
                tsocketSSL.start();
            }
    }
    public void stop() throws IOException,Exception{
            if (tsocket != null && tsocket.isAlive()) {
                socket.close();
                tsocket.interrupt();
            }else tsocket=null;
            if (tsocketSSL != null && tsocketSSL.isAlive()) {
                socketSSL.close();
                tsocketSSL.interrupt();
            }else tsocketSSL=null;
        try {
            if (socket != null) {
                socket.close();
                socket = null;
            }
        }catch (Exception e){}

        try {
            if (socketSSL!=null)
            {
                socketSSL.close();
                socketSSL=null;
            }
        }catch (Exception e){}
    }

    private boolean validateMailFrom(final String _mailFrom){
        if (listener!=null)
            return listener.onValidateMailFrom(_mailFrom);
        else
            return true;
    }
    private boolean validateRcptTo(final String _rcptTo){
        if (listener!=null)
            return listener.onValidateRcptTo(_rcptTo);
        else
            return true;
    }

    private boolean validateUser(final String username,final String password){
        if (listener!=null)
            return listener.onValidateUser(username,password);
        else
        return true;
    }
    private boolean validateData(final String data){
        if (listener!=null)
            return listener.onValidateMail(data);
        else
            return true;
    }

    private String changeArgsInText(final String _text,final Client client){
        String result = _text;
        try {
            result = result.replaceAll("<domain>", "" + this.domain + "");
            result = result.replaceAll("<forward-path>", "<" + client.dataMail.getRCPT_TO() + ">");
            result = result.replaceAll("<backward-path>", "<" + client.dataMail.getMAIL_FROM() + ">");
        }catch (Exception e){Log.d("ChangeArgsInText", e.getLocalizedMessage());}
        return result;
    }

    private void writeReply(final CodesAndCommands.Code _code, final Client client, final String change_desc)   throws Exception{
        String _message=change_desc==null||change_desc.isEmpty()?_code.decription:change_desc;

        if (_message!=null) {
            _message = changeArgsInText(_message, client);
            _message=_code.code+" "+_message+"\r\n";
        }else _message=String.valueOf(_code.code);

        serverWrite(_message,client.ostream);
    }

    private class AlternativeCommand{
        public CodesAndCommands.Command command;
        public String desc;
        AlternativeCommand(CodesAndCommands.Command _command, String _desc){
            command=_command;
            desc=_desc;
        }
    }
    private class AlternativeReply{
        public CodesAndCommands.Code code;
        public String desc;
        AlternativeReply(CodesAndCommands.Code _code, String _desc){
            code=_code;
            desc=_desc;
        }
    }
    private void writeReply(final Client client, final ArrayList<AlternativeReply> messages)  throws Exception{
        for (int i=0;i<messages.size();i++) {
            String _message=messages.get(i).desc==null||messages.get(i).desc.isEmpty()?messages.get(i).code.decription:messages.get(i).desc;

            if (_message!=null) {
                _message = changeArgsInText(_message, client);
                if (i<messages.size()-1)
                    _message=messages.get(i).code.code+"-"+_message+"\r\n";
                else
                    _message=messages.get(i).code.code+" "+_message+"\r\n";
            }else _message=String.valueOf(messages.get(i).code.code);

            serverWrite(_message,client.ostream);
        }
    }


    private void writeCommand(final CodesAndCommands.Command _command, final Client client, final String change_desc) throws Exception {
        String _message = change_desc == null || change_desc.isEmpty() ? _command.decription : change_desc;

        if (_message != null){
            _message = changeArgsInText(_message, client);
            _message = _command.name() + " " + _message + "\r\n";
        }
        else _message=_command.name()+"\r\n";

        serverWrite(_message,client.ostream);
    }



    //запись комманды
    private void writeCommand(final Client client, final ArrayList<AlternativeCommand> messages) throws Exception {
        for (int i=0;i<messages.size();i++) {
            String _message = messages.get(i).desc == null ||messages.get(i).desc.isEmpty() ? messages.get(i).command.decription :messages.get(i).desc;

            if (_message != null) {
                _message = changeArgsInText(_message, client);
                if (i<messages.size()-1)
                    _message=messages.get(i).command.name()+"-"+_message+"\r\n";
                else
                    _message=messages.get(i).command.name()+" "+_message+"\r\n";
            } else _message = messages.get(i).command.name()+"\r\n";

            serverWrite(_message,client.ostream);
        }
    }


    //функция записи сообщения в поток записи
    private static void serverWrite(final String strMessage, OutputStream clientStream)throws UnsupportedEncodingException,IOException,Exception
    {
        byte[] buffer = strMessage.getBytes("US-ASCII");//"US-ASCII"
        clientStream.write(buffer, 0, buffer.length);
        clientStream.flush();
    }
    private static void serverWrite(final String strMessage, OutputStream clientStream,String _charSet)throws UnsupportedEncodingException,IOException,Exception
    {
        String charset=_charSet;
        if (charset==null)
            charset= Charset.defaultCharset().toString();
        byte[] buffer = strMessage.getBytes(charset);//"US-ASCII"
        clientStream.write(buffer, 0, buffer.length);
        clientStream.flush();
    }


    //функция считывания почты из потока
    private static String serverReadData(InputStream clientStream,final int max_size) throws IOException,OutOfMemoryError,Exception
    {
        if (max_size<=0) throw new Exception("Max size have to be more than 0");
        String _endMail="\r\n.\r\n";
        String _newLine="\r\n";

        long time_start=System.currentTimeMillis();
        BufferedReader r = new BufferedReader(new InputStreamReader(clientStream));
        String bufString=null;
        String strMessage="";
        while(!strMessage.endsWith(_endMail)){
            if (System.currentTimeMillis() - time_start >= 3000)
                throw new Exception("Time more than 15 sec.");

            bufString=r.readLine();
            if (bufString==null) return null;
            //если больше чем max size исключение
            if (bufString.length()+strMessage.length()>max_size)
                throw new OutOfMemoryError("Mail length more than max size");
            strMessage+=bufString+_newLine;
        }
        return strMessage;
    }


    //функция считывания строки из потока
    private static String serverRead(InputStream clientStream) throws IOException,Exception
    {
        BufferedReader r = new BufferedReader(new InputStreamReader(clientStream));
        String strMessage=r.readLine();
        return strMessage;
    }
    //функция считывания строк из потока
    private ArrayList<AlternativeReply> serverReadOptions(InputStream clientStream) throws IOException,Exception
    {
        ArrayList<AlternativeReply> result=new ArrayList<AlternativeReply>();
        BufferedReader r = new BufferedReader(new InputStreamReader(clientStream));
        String strMessage;
        long time_start=System.currentTimeMillis();
        do {
            if (System.currentTimeMillis() - time_start >= 3000)
                throw new Exception("Time more than 15 sec.");
            strMessage = r.readLine();
            if (CodesAndCommands.Code.getCodeText(strMessage)!=CodesAndCommands.Code.Okay)
                throw new Exception("Not 250 string.");
            result.add(new AlternativeReply(CodesAndCommands.Code.Okay, CodesAndCommands.Code.getStringFromReply(strMessage)));
        }while (strMessage.charAt(3)=='-');
        return result;
    }




    public class Client implements Runnable {
        private Thread clientTask;

        private SSLSocket socketSSLClient;
        private Socket socketClient;
        private OutputStream ostream;
        private InputStream istream;

        private DataMail dataMail;

        private ArrayList<AlternativeReply> options;
        private int max_size=400000;

        private boolean isServer;

        Client(boolean ssl,Socket _client) throws Exception{
            try {
                isServer=true;
                this.dataMail=new DataMail();
                if (sslSocketFactory!=null){
                this.options=new ArrayList<AlternativeReply>(){{
                    add(new AlternativeReply(CodesAndCommands.Code.Okay,null ));
                    add(new AlternativeReply(CodesAndCommands.Code.Okay, CodesAndCommands.Command.STARTTLS.name()));
                }};}else{
                    this.options=new ArrayList<AlternativeReply>(){{
                        add(new AlternativeReply(CodesAndCommands.Code.Okay,null )); }};
                }
                this.socketClient=_client;
                this.ostream = socketClient.getOutputStream();
                this.istream = socketClient.getInputStream();
                if (ssl)
                    startSSL(isServer);
            }catch (Exception e){
                this.stop();
                Log.d("Client as server create","Exception: "+e.getLocalizedMessage());
            }
        }
        Client(boolean ssl,Socket _client,final DataMail _dm) throws Exception{
            try {
                if (_dm==null)
                    throw new Exception("Data mail cant't be null.");

                isServer=false;
                this.dataMail=_dm;
                this.options=new ArrayList<>();
                this.socketClient=_client;
                this.ostream = socketClient.getOutputStream();
                this.istream = socketClient.getInputStream();
                if (ssl)
                    startSSL(isServer);
            }catch (Exception e){
                this.stop();
                Log.d("Client as client create","Exception: "+e.getLocalizedMessage());
            }
        }

        private boolean haveOption(final String _text){
            for (AlternativeReply _a:this.options)
                if (_a.desc.compareTo(_text)==0)
                    return true;
            return false;
        }

        private void checkReply(Client client, CodesAndCommands.Code status,CodesAndCommands.Code need) throws Exception{
            if (status!= need)
            {
                writeCommand(CodesAndCommands.Command.QUIT, this,null );
                throw new Exception("Client wrong answer.");
            }
        }

        @Override
        public void run() {
                try {
                    long time_start=System.currentTimeMillis();
                    String message = null;
                    String arg = null;

                    if (this.isServer)
                        writeReply(CodesAndCommands.Code.Ready, this, null);

                    boolean isShowOptions = false;
                    boolean startSslFlag=false;
                    int counter=0;
                    while (true) {
                            if (System.currentTimeMillis() - time_start >= 15000)
                                throw new Exception("Time more than 15 sec.");
                            message = null;
                            arg = null;
                            message = serverRead(this.istream);
                        if (this.isServer) {
                            Log.d("Got command:", message);
                            CodesAndCommands.Command _command = CodesAndCommands.Command.getCommandByText(message);
                            switch (_command) {
                                case HELO:
                                    //!!!
                                    if (!isShowOptions){
                                        writeReply(this, this.options);
                                        isShowOptions=true;
                                    }
                                    else
                                        writeReply(CodesAndCommands.Code.Okay, this, null);
                                    break;
                                case EHLO:
                                    if (!isShowOptions){
                                        writeReply(this, this.options);
                                        isShowOptions=true;
                                    }
                                    else
                                        writeReply(CodesAndCommands.Code.Okay, this, null);
                                    break;
                                case MAIL:
                                    arg = dataMail.getMailFromText(message);
                                    if (arg != null) {
                                        if (validateMailFrom(arg))
                                            writeReply(CodesAndCommands.Code.Okay, this, null);
                                        else
                                            writeReply(CodesAndCommands.Code.NotAcceptMail, this, null);
                                    } else
                                        writeReply(CodesAndCommands.Code.NotCommandPar, this, null);
                                    this.dataMail.setMAIL_FROM(arg);
                                    break;
                                case RCPT:
                                    arg = dataMail.getMailFromText(message);
                                    if (arg != null) {
                                        if (validateRcptTo(arg))
                                            writeReply(CodesAndCommands.Code.Okay, this, null);
                                        else
                                            writeReply(CodesAndCommands.Code.NotAcceptMail, this, null);
                                    } else
                                        writeReply(CodesAndCommands.Code.NotCommandPar, this, null);
                                    this.dataMail.setRCPT_TO(arg);
                                    break;
                                case DATA:
                                    writeReply(CodesAndCommands.Code.StartMailInput, this, null);
                                    arg = serverReadData(this.istream, this.max_size);
                                    if (arg!=null) {
                                        if (validateData(arg))
                                            writeReply(CodesAndCommands.Code.Okay, this, null);
                                        else
                                            writeReply(CodesAndCommands.Code.BadSequnceCommand, this, null);
                                    } else
                                        writeReply(CodesAndCommands.Code.NotCommandPar, this, null);

                                    this.dataMail.setRawData(arg);
                                    break;
                                case RSET:
                                    this.dataMail.reset();
                                    writeReply(CodesAndCommands.Code.Okay, this, null);
                                    break;
                                case NOOP:
                                    break;
                                case QUIT:
                                    writeReply(CodesAndCommands.Code.CloasingTC, this, null);
                                    if (this.dataMail!=null){
                                        this.dataMail.upDate();
                                        if (this.dataMail.isReadyToSend()){
                                            class OnReceiveTask implements Runnable {
                                                DataMail dm;
                                                Socket s;
                                                OnReceiveTask(final DataMail _dm,final Socket _s){
                                                    dm=_dm;
                                                    s=_s;
                                                }
                                                @Override
                                                public void run() {
                                                    if (listener!=null)
                                                        listener.onReceivedMail(this.s,this.dm);
                                                }
                                            }
                                            new Thread(new OnReceiveTask(this.dataMail,this.socketClient)).start();
                                        }

                                    }
                                    throw new Exception("Close by client");
                                case STARTTLS:
                                    writeReply(CodesAndCommands.Code.Ready, this, "TLS Go ahead");
                                    this.startSSL(this.isServer);
                                    startSslFlag=true;
                                    break;
                                default:
                                    writeReply(CodesAndCommands.Code.ActionNotTaken2, this, null);
                                    throw new Exception("Unknown command");
                            }
                        } else {
                            //Simple Code To Send Mail
                            Log.d("Got reply:", message);
                            CodesAndCommands.Code _reply = CodesAndCommands.Code.getCodeText(message);
                            switch (counter) {
                                case 0:
                                    checkReply(this,_reply, CodesAndCommands.Code.Ready);
                                    if (startSslFlag)
                                        startSSL(this.isServer);
                                    writeCommand(CodesAndCommands.Command.EHLO, this, null);
                                    if (!isShowOptions) {
                                        this.options = serverReadOptions(this.istream);
                                        isShowOptions = true;
                                    }else {
                                        counter=0;
                                        break;
                                    }
                                case 1:
                                    if (haveOption(CodesAndCommands.Command.STARTTLS.name()) && !startSslFlag){
                                        writeCommand(CodesAndCommands.Command.STARTTLS, this, null);
                                        counter=-1;
                                        startSslFlag=true;
                                        break;
                                    }
                                    counter=2;
                                case 2:
                                    checkReply(this,_reply, CodesAndCommands.Code.Okay);
                                    writeCommand(CodesAndCommands.Command.MAIL, this, null);
                                    break;
                                case 3:
                                    checkReply(this,_reply, CodesAndCommands.Code.Okay);
                                    writeCommand(CodesAndCommands.Command.RCPT, this, null);
                                    break;
                                case 4:
                                    checkReply(this,_reply, CodesAndCommands.Code.Okay);
                                    writeCommand(CodesAndCommands.Command.DATA, this, null);
                                    break;
                                case 5:
                                    checkReply(this,_reply, CodesAndCommands.Code.StartMailInput);
                                    serverWrite(this.dataMail.getResultMail(), this.ostream,this.dataMail.getCharSet());
                                    break;
                                default:
                                    checkReply(this,_reply, CodesAndCommands.Code.Okay);
                                    if (this.dataMail!=null){
                                        this.dataMail.upDate();
                                        if (this.dataMail.isReadyToSend()){
                                            class OnReceiveTask implements Runnable {
                                                DataMail dm;
                                                Socket s;
                                                OnReceiveTask(final DataMail _dm,final Socket _s){
                                                    dm=_dm;
                                                    s=_s;
                                                }
                                                @Override
                                                public void run() {
                                                    if (listener!=null)
                                                        listener.onEndSendMail(this.s,this.dm);
                                                }
                                            }
                                            new Thread(new OnReceiveTask(this.dataMail,this.socketClient)).start();
                                        }

                                    }
                                    writeCommand(CodesAndCommands.Command.QUIT, this, null);
                                    throw new Exception("End send");
                            }
                        }
                        counter++;
                    }
            }
            catch (Exception e){
                Log.d("Client task", "Exception: " + e.getLocalizedMessage());
            }
            finally {
                this.stop();
            }
        }

        public void stop(){
            try {
                this.ostream.close();
                this.istream.close();
                this.socketClient.close();
                this.socketSSLClient.close();
            }
            catch (Exception e) {
                Log.d("Client stop", "Exception: " + e.getLocalizedMessage());
            }
        }

        private void startSSL(boolean _useClientMode) throws IOException,Exception {
            //if (this.socketSSLClient==null) {
                this.socketSSLClient = (SSLSocket) sslSocketFactory.createSocket(this.socketClient, this.socketClient.getInetAddress().getHostAddress(),
                        this.socketClient.getPort(), true);
                this.socketSSLClient.setUseClientMode(!_useClientMode);
                this.ostream = socketSSLClient.getOutputStream();
                this.istream = socketSSLClient.getInputStream();
            /*}else {
                this.socketSSLClient.setUseClientMode(!_useClientMode);
                this.socketSSLClient.startHandshake();
                this.ostream = socketSSLClient.getOutputStream();
                this.istream = socketSSLClient.getInputStream();
            }*/
        }
    }

}
