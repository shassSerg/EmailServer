package com.shass.emailserver.EmailServer;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class CodesAndCommands {


/*Команда	Успех (S)	Неудача (E)
Организация соединения	220	554
EHLO или HELO	250	504, 550, 502
MAIL	250	552, 451, 452, 550, 553, 503, 455, 555
RCPT	250, 251	550, 551, 552, 553, 450, 451, 452, 503, 455, 555
DATA (промежуточный отклик 354)	250	552, 554, 451, 452, 450, 550 (отказ в соответствии с политикой)
DATA		503,55
RSET	250
VRFY	250, 251, 252	550, 551, 553, 502, 504
EXPN	250, 252	550, 500, 502, 504
HELP	211, 214	502, 504
NOOP	250
QUIT	221*/


    /**
     * 200	(nonstandard success response, see rfc876)
     * 211	System status, or system help reply
     * 214	Help message
     * 220	<domain> Service ready
     * 221	<domain> Service closing transmission channel
     * 250	Requested mail action okay, completed
     * 251	User not local; will forward to <forward-path>
     * 252	Cannot VRFY user, but will accept message and attempt delivery
     * 354	Start mail input; end with <CRLF>.<CRLF>
     * 421	<domain> Service not available, closing transmission channel
     * 450	Requested mail action not taken: mailbox unavailable
     * 451	Requested action aborted: local error in processing
     * 452	Requested action not taken: insufficient system storage
     * 500	Syntax error, command unrecognised
     * 501	Syntax error in parameters or arguments
     * 502	Command not implemented
     * 503	Bad sequence of commands
     * 504	Command parameter not implemented
     * 521	<domain> does not accept mail (see rfc1846)
     * 530	Access denied (???a Sendmailism)
     * 550	Requested action not taken: mailbox unavailable
     * 551	User not local; please try <forward-path>
     * 552	Requested mail action aborted: exceeded storage allocation
     * 553	Requested action not taken: mailbox name not allowed
     * 554	Transaction failed
     * **/
    public static enum Code{
        /**(nonstandard success response, see rfc876)**/
        NonStandartSucces(200,"(nonstandard success response, see rfc876)"),
        /**System status, or system help reply**/
        SystemStatus(211,"System status, or system help reply"),
        /** Help message**/
        Help(214,"Help message"),
        /**<domain> Service ready**/
        Ready(220,"<domain> Service ready"),
        /**<domain> Service closing transmission channel**/
        CloasingTC(221,"<domain> Service closing transmission channel"),
        /**Requested mail action okay, completed**/
        Okay(250,"Requested mail action okay, completed"),
        /**User not local; will forward to <forward-path>**/
        UserNotLocal(251,"User not local; will forward to <forward-path>"),
        /**Cannot VRFY user, but will accept message and attempt delivery**/
        NotVRFY(252,"Cannot VRFY user, but will accept message and attempt delivery"),
        /**Start mail input; end with <CRLF>.<CRLF>**/
        StartMailInput(354,"Start mail input; end with <CRLF>.<CRLF>"),
        /**<domain> Service not available, closing transmission channel**/
        ServiceNotAvailable(421,"<domain> Service not available, closing transmission channel"),
        /**Requested mail action not taken: mailbox unavailable**/
        ActionNotTaken(450,"Requested mail action not taken: mailbox unavailable"),
        /**Requested action aborted: local error in processing**/
        Aborted(451,"Requested action aborted: local error in processing"),
        /**Requested action not taken: insufficient system storage**/
        ReqActionNotTaken(452,"Requested action not taken: insufficient system storage"),
        /**Syntax error, command unrecognised**/
        SyntaxError(500,"Syntax error, command unrecognised"),
        /**Syntax error in parameters or arguments**/
        SyntaxErrorPar(501,"Syntax error in parameters or arguments"),
        /**Command not implemented**/
        NotCommand(502,"Command not implemented"),
        /**Bad sequence of commands**/
        BadSequnceCommand(503,"Bad sequence of commands"),
        /**Command parameter not implemented**/
        NotCommandPar(504,"Command parameter not implemented"),
        /**<domain> does not accept mail (see rfc1846)**/
        NotAcceptMail(521,"<domain> does not accept mail (see rfc1846)"),
        /**Access denied (???a Sendmailism)**/
        AccesDenied(530,"Access denied (???a Sendmailism)"),
        /**Requested action not taken: mailbox unavailable**/
        ActionNotTaken2(550,"Requested action not taken: mailbox unavailable"),
        /**User not local; please try <forward-path>**/
        UserNotLocal2(551,"User not local; please try <forward-path>"),
        /**Requested mail action aborted: exceeded storage allocation**/
        ExceedStorage(552,"Requested mail action aborted: exceeded storage allocation"),
        /**Requested action not taken: mailbox name not allowed**/
        NotAllowedMailboxName(553,"Requested action not taken: mailbox name not allowed"),
        /**Transaction failed**/
        TransactionFailed(554,"Transaction failed");

        private static final Map<Integer, Code> BY_CODE = new HashMap<>();
        // declaring private variable for getting values
        public final int code;
        public final String decription;

        static {
            for (Code e: values()) {
                BY_CODE.put(e.code, e);
            }
        }
        public static Code valueOfCode(int code) {
            return BY_CODE.get(code);
        }

        private Code(int code,String desc)
        {
            this.code = code;
            this.decription = desc;
        }

        public enum  ReplyStatus{
            SUCCESS,
            WAIT,
            FAIL,
            ERROR
        }
        //проверка статуса ответа
        public static ReplyStatus checkReplyStatus(final String message)
        {
            try {
                String substring = message.substring(0, 3);
                Integer.parseInt(substring);
                if (substring.charAt(0)==  '2')
                    return ReplyStatus.SUCCESS;
                if (substring.charAt(0) == '3')
                    return ReplyStatus.WAIT;
                if (substring.charAt(0) == '4')
                    return ReplyStatus.FAIL;
                return ReplyStatus.ERROR;
            }catch (Exception e){
                 return ReplyStatus.ERROR;
            }
        }
        public static Code getCodeText(final String text){
            for (Code a:
                    Code.values()) {
                if (text.startsWith(String.valueOf(a.code)))
                    return a;
            }
            return  null;
        }
        //проверка комманда или ответ

        /**
         * true if reply
         * @param message
         * @return
         */
        public static boolean checkReplyOrCommand(final String message){
            try {
                String substring=message.substring(0,3);
                Integer value=Integer.parseInt(substring);
                if (value<200 || value>=600)
                    return false;
                return true;
            }
            catch (Exception e){
                return false;
            }
        }
        public static String getStringFromReply(final String _message){
            try{
                if (checkReplyOrCommand(_message))
                    return (_message.substring(4)).trim();
                else
                    return _message.trim();
            }catch (Exception e){
                return _message;
            }
        }
    }

    /**	Commands **/
    public static enum Command
    {
        connect(null),
        HELO("<domain>"),
        EHLO("<domain>"),
        MAIL("FROM: <backward-path>"),
        RCPT("TO: <forward-path>"),
        DATA(null),
        received_data(null),
        RSET(null),
        SEND(""),
        SOML(""),
        SAML(""),
        VRFY(""),
        EXPN(""),
        HELP(""),
        NOOP(null),
        QUIT(null),
        TURN(null),
        STARTTLS(null);
        public final String decription;

        private Command(String desc)
        {
            this.decription = desc;
        }

        public static Command getCommandByText(final String text){
                for (Command a:
                        Command.values()) {
                    if (text.startsWith(a.name()))
                        return a;
                }
            return  null;
        }
    }

}
