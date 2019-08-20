package com.shass.emailserver.EmailServer;

import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataMail {

    public DataMail(){
    }

    public boolean setRawData(String _text){
        if (mailFrom==null || rcptTo==null || _text==null || _text.isEmpty())
            return false;
        this.subject="Subject";
        this.body=_text;
        return true;
    }
    public DataMail(String _mailFrom, String _rcptTo, String _subject, String _body) throws Exception{
        this.charSet=Charset.defaultCharset().displayName();
        setMAIL_FROM(_mailFrom);
        setRCPT_TO(_rcptTo);
        setSubject(_subject);
        setBody(_body);
        date=System.currentTimeMillis();
    }

    public long upDate(){
        this.date=System.currentTimeMillis();
        return this.date;
    }

    public boolean isReadyToSend(){
        return ((mailFrom!=null)&&(rcptTo!=null)&&(subject!=null)&&(body!=null));
    }
    private String charSet;
    public String getCharSet(){
        return charSet;
    }
    public void setCharSet(String _charSet) throws Exception{
        if (!Charset.isSupported(_charSet))
            throw new Exception("Not valid charset.");

        this.charSet=_charSet;
    }

    public String getResultMail() throws Exception{
        String mail="";
        mail+="Date: "+getStringDate()+"\r\n";
        mail+="From: "+getMAIL_FROM()+"\r\n";
        mail+="Subject: "+getSubject()+"\r\n";
        mail+="To: "+getRCPT_TO()+"\r\n\r\n";
        mail+=getBody()+"\r\n.\r\n";
        return mail;
    }

    public byte[] getResultMailBytes() throws Exception{
        String mail="";
        mail+="Date: "+getStringDate()+"\r\n";
        mail+="From: "+getMAIL_FROM()+"\r\n";
        mail+="Subject: "+getSubject()+"\r\n";
        mail+="To: "+getRCPT_TO()+"\r\n\r\n";
        mail+=getBody()+"\r\n.\r\n";
        if (this.charSet==null)
            return mail.getBytes();
        else
        return mail.getBytes(this.charSet);
    }

    private boolean isValidText(String _text){
        return  _text.indexOf("\r\n.\r\n")==-1;
    }
    public String getMailFromText(String _text){
        try {
            String regex = "<(.*)>";
            Pattern pattern
                    = Pattern.compile(regex);
            Matcher matcher
                    = pattern
                    .matcher(_text);
            matcher.find();
            return matcher.group(1);
        }catch (Exception e){
            return null;
        }
    }

public void reset(){
        subject=null;
        body=null;
        mailFrom=null;
        rcptTo=null;
}

    private String subject=null;
    private String body=null;
    private long date=0;


    public String getSubject(){return  subject;}
    public void setSubject(String _value) throws Exception{
        if (_value==null || _value.isEmpty() || !isValidText(_value))
            throw new Exception("Not valid subject value.");
        subject=_value;
    }
    public String getBody(){return  body;}
    public void setBody(String _value) throws Exception{
        if (_value==null || _value.isEmpty() || !isValidText(_value))
            throw new Exception("Not valid body value.");
        body=_value;
    }
    public long getDate(){return  this.date;}

    public String getStringDate(){
        Date date=new Date(this.date);
        return date.toString();
    }

    private String mailFrom=null;
    private String rcptTo=null;

    public String getMAIL_FROM(){return  mailFrom;}
    public void setMAIL_FROM(String _value) throws Exception{
        if (_value==null || _value.isEmpty())
            throw new Exception("Not valid mailFrom value.");
        String regex = "^[\\w-_\\.+]*[\\w-_\\.]\\@([\\w]+\\.)+[\\w]+[\\w]$";
        if (_value.matches(regex))
            mailFrom = _value;
        else throw new Exception("Not valid mailFrom value.");
    }
    public String getRCPT_TO(){return  rcptTo;}
    public void setRCPT_TO(String _value) throws Exception{
        if (_value==null || _value.isEmpty())
            throw new Exception("Not valid rcptTo value.");
        String regex = "^[\\w-_\\.+]*[\\w-_\\.]\\@([\\w]+\\.)+[\\w]+[\\w]$";
        if (_value.matches(regex))
            rcptTo = _value;
        else throw new Exception("Not valid rcptTo value.");
    }
}
