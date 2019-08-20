package com.shass.emailserver;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import com.shass.emailserver.EmailServer.DomainUtils;
import com.shass.emailserver.Services.ESTMPService;
import com.shass.emailserver.Tables.Received;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;


public class MainActivity extends AppCompatActivity {


    ESTMPService mService;
    boolean mBound = false;

    private BroadcastReceiver estmpReceiver;

    public static final String END_LOAD_SERVER_EVENT = "com.shass.emailserver.END_LOAD_SERVER_EVENT";
    public static final String SENT_MAIL_SERVER_EVENT = "com.shass.emailserver.SENT_MAIL";
    public static final String MESSAGE_TAG = "EXCEPTION";


    private DBHelper mDbHelper;


    private static final NavigableMap<Long, String> suffixes = new TreeMap<>();
    static {
        suffixes.put(1_000L, "k");
        suffixes.put(1_000_000L, "M");
        suffixes.put(1_000_000_000L, "G");
        suffixes.put(1_000_000_000_000L, "T");
        suffixes.put(1_000_000_000_000_000L, "P");
        suffixes.put(1_000_000_000_000_000_000L, "E");
    }

    public static String format(long value) {
        if (value == Long.MIN_VALUE) return format(Long.MIN_VALUE + 1);
        if (value < 0) return "-" + format(-value);
        if (value < 1000) return Long.toString(value);

        Map.Entry<Long, String> e = suffixes.floorEntry(value);
        Long divideBy = e.getKey();
        String suffix = e.getValue();

        long truncated = value / (divideBy / 10);
        boolean hasDecimal = truncated < 100 && (truncated / 10d) != (truncated / 10);
        return hasDecimal ? (truncated / 10d) + suffix : (truncated / 10) + suffix;
    }

    private void updateCounts(){
        long countConnected=mDbHelper.getCountConnected();
        long countSent=mDbHelper.getSentCount();
        long countReceived=mDbHelper.getReceivedCount();

        ((TextView)findViewById(R.id.tvConnectedValue)).setText(format(countConnected));
        ((TextView)findViewById(R.id.tvReceivedValue)).setText(format(countReceived));
        ((TextView)findViewById(R.id.tcSentValue)).setText(format(countSent));
    }

    private void changeSSL(boolean status){
        LinearLayout _etPath=((LinearLayout)findViewById(R.id.layoutPortCert));
        EditText _etPass=((EditText)findViewById(R.id.etPassCert));
        EditText _etPortSsl=((EditText)findViewById(R.id.etPortSsl));
        _etPass.setEnabled(status);
        _etPortSsl.setEnabled(status);
        _etPath.setClickable(status);
        ((LinearLayout) findViewById(R.id.layoutPortCert)).setBackgroundResource(R.drawable.rounded_button_path_gray);
    }
    private void disableAllInput(){
        EditText _etDomain=((EditText)findViewById(R.id.etDomain));
        EditText _etPort=((EditText)findViewById(R.id.etPort));

        ((Switch)findViewById(R.id.swSSL)).setEnabled(false);
        _etDomain.setEnabled(false);
        _etPort.setEnabled(false);
        changeSSL(false);
    }
    private void enableAllInput(){
        EditText _etDomain=((EditText)findViewById(R.id.etDomain));
        EditText _etPort=((EditText)findViewById(R.id.etPort));

        LinearLayout _etPath=((LinearLayout)findViewById(R.id.layoutPortCert));
        EditText _etPass=((EditText)findViewById(R.id.etPassCert));
        EditText _etPortSsl=((EditText)findViewById(R.id.etPortSsl));

        ((Switch)findViewById(R.id.swSSL)).setEnabled(true);
        _etDomain.setEnabled(true);
        _etPort.setEnabled(true);

        changeSSL(((Switch)findViewById(R.id.swSSL)).isChecked());
    }

    private void checkAllParameters(){
       try{
           boolean finalResult=true;

           boolean _SSL = ((Switch)findViewById(R.id.swSSL)).isChecked();

           EditText _etDomain=((EditText)findViewById(R.id.etDomain));
           EditText _etPort=((EditText)findViewById(R.id.etPort));

           TextView _etPath=((TextView)findViewById(R.id.tvCert));
           EditText _etPortSsl=((EditText)findViewById(R.id.etPortSsl));


           if (_etDomain.getText().toString().isEmpty() || !DomainUtils.isValidDomainName(_etDomain.getText().toString())) {
               ((ImageView) findViewById(R.id.ivDomain)).setImageResource(android.R.drawable.presence_busy);
               finalResult=false;
           }
           else
               ((ImageView)findViewById(R.id.ivDomain)).setImageResource(android.R.drawable.presence_online);

           try {
               if (_etPort.getText().toString().isEmpty() || Integer.parseInt(_etPort.getText().toString())<0 || Integer.parseInt(_etPort.getText().toString())>65535){
                   ((ImageView) findViewById(R.id.ivPort)).setImageResource(android.R.drawable.presence_busy);
                   finalResult=false;
               }
               else
                   ((ImageView) findViewById(R.id.ivPort)).setImageResource(android.R.drawable.presence_online);
           }catch (Exception e){
               ((ImageView) findViewById(R.id.ivPort)).setImageResource(android.R.drawable.presence_busy);
               finalResult=false;
           }
           if (_SSL){
               ((ImageView) findViewById(R.id.ivPortSSL)).setVisibility(View.VISIBLE);
               if (_etPath.getText().toString().isEmpty()) {
                   ((LinearLayout) findViewById(R.id.layoutPortCert)).setBackgroundResource(R.drawable.rounded_button_path_negative);
                   finalResult=false;
               }
               else
                   ((LinearLayout)findViewById(R.id.layoutPortCert)).setBackgroundResource(R.drawable.rounded_button_path);

               try {
                   if (_etPortSsl.getText().toString().isEmpty() || Integer.parseInt(_etPortSsl.getText().toString())<0 || Integer.parseInt(_etPortSsl.getText().toString())>65555) {
                       ((ImageView) findViewById(R.id.ivPortSSL)).setImageResource(android.R.drawable.presence_busy);
                       finalResult=false;
                   }
                   else
                       ((ImageView) findViewById(R.id.ivPortSSL)).setImageResource(android.R.drawable.presence_online);
               }catch (Exception e){
                   ((ImageView) findViewById(R.id.ivPortSSL)).setImageResource(android.R.drawable.presence_online);
                   finalResult=false;
               }

           }else {
               ((ImageView) findViewById(R.id.ivPortSSL)).setVisibility(View.GONE);
               ((LinearLayout) findViewById(R.id.layoutPortCert)).setBackgroundResource(R.drawable.rounded_button_path_gray);
           }


           if (mBound) {
               disableAllInput();
               setStateButton(false);
           }else{
               ((Button) findViewById(R.id.bStart)).setEnabled(finalResult);
           }


        }catch (Exception e){
             /*   Toast toast = Toast.makeText(getApplicationContext(),
                        getString(R.string.errorcheckpar)+e.getLocalizedMessage(),
                Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();*/
           Snackbar.make(getWindow().getDecorView().getRootView(), getString(R.string.errorcheckpar)+e.getLocalizedMessage(), Snackbar.LENGTH_LONG).show();
       }
    }

    long delay = 300; // 1 seconds after user stops typing
    long last_text_edit = 0;
    Handler handler = new Handler();

    private Runnable input_finish_checker = new Runnable() {
        public void run() {
            //if (System.currentTimeMillis() > (last_text_edit + delay)) {
                checkAllParameters();
            //}
        }
    };

    private void saveAllParameters(){
        try {
            SharedPreferences sharedPref = MainActivity.this.getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            boolean _SSL = ((Switch)findViewById(R.id.swSSL)).isChecked();

            EditText _etDomain=((EditText)findViewById(R.id.etDomain));
            EditText _etPort=((EditText)findViewById(R.id.etPort));

            TextView _etPath=((TextView)findViewById(R.id.tvCert));
            EditText _etPass=((EditText)findViewById(R.id.etPassCert));
            EditText _etPortSsl=((EditText)findViewById(R.id.etPortSsl));

            editor.putBoolean(getString(R.string.SETTINGS_SSL),_SSL);

            if (!_etDomain.getText().toString().isEmpty())
                editor.putString(getString(R.string.SETTINGS_DOMAIN),_etDomain.getText().toString());

            try {
                if (!_etPort.getText().toString().isEmpty())
                    editor.putInt(getString(R.string.SETTINGS_PORT), Integer.parseInt(_etPort.getText().toString()));
            }catch (Exception e){}

                if (!_etPath.getText().toString().isEmpty())
                    editor.putString(getString(R.string.SETTINGS_PATH),_etPath.getText().toString());

                if (!_etPass.getText().toString().isEmpty())
                    editor.putString(getString(R.string.SETTINGS_PASS),_etPass.getText().toString());

                try{
                if (!_etPortSsl.getText().toString().isEmpty())
                    editor.putInt(getString(R.string.SETTINGS_PORT_SSL),Integer.parseInt(_etPortSsl.getText().toString()));
                }catch (Exception e){}


            editor.commit();
        } catch (Exception e){
            /*Toast toast = Toast.makeText(getApplicationContext(),
                    getString(R.string.errorsavepar)+e.getLocalizedMessage(),
                    Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();*/
            Snackbar.make(getWindow().getDecorView().getRootView(), getString(R.string.errorsavepar)+e.getLocalizedMessage(), Snackbar.LENGTH_LONG).show();
        }
    }

    private void loadAllParameters(){
        try {
            SharedPreferences sharedPref = MainActivity.this.getPreferences(Context.MODE_PRIVATE);
            boolean defaultValue = false;
            boolean _SSL = sharedPref.getBoolean(getString(R.string.SETTINGS_SSL), defaultValue);

            ((Switch)findViewById(R.id.swSSL)).setChecked(_SSL);

            if (sharedPref.contains(getString(R.string.SETTINGS_DOMAIN)))
                ((EditText)findViewById(R.id.etDomain)).setText(sharedPref.getString(getString(R.string.SETTINGS_DOMAIN), ""), TextView.BufferType.NORMAL );

            if (sharedPref.contains(getString(R.string.SETTINGS_PORT)))
                ((EditText)findViewById(R.id.etPort)).setText(String.valueOf(sharedPref.getInt(getString(R.string.SETTINGS_PORT), 0)), TextView.BufferType.NORMAL );

                if (sharedPref.contains(getString(R.string.SETTINGS_PATH)))
                    ((TextView)findViewById(R.id.tvCert)).setText(sharedPref.getString(getString(R.string.SETTINGS_PATH), ""), TextView.BufferType.NORMAL );

                if (sharedPref.contains(getString(R.string.SETTINGS_PASS)))
                    ((EditText)findViewById(R.id.etPassCert)).setText(sharedPref.getString(getString(R.string.SETTINGS_PASS), ""), TextView.BufferType.NORMAL );

                if (sharedPref.contains(getString(R.string.SETTINGS_PORT_SSL)))
                    ((EditText)findViewById(R.id.etPortSsl)).setText(String.valueOf(sharedPref.getInt(getString(R.string.SETTINGS_PORT_SSL), 0)), TextView.BufferType.NORMAL );

            changeSSL(_SSL);

            checkAllParameters();
        }catch (Exception e){
            /*Toast toast = Toast.makeText(getApplicationContext(),
                    getString(R.string.errorloadpar)+e.getLocalizedMessage(),
                    Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();*/
            Snackbar.make(getWindow().getDecorView().getRootView(), getString(R.string.errorloadpar)+e.getLocalizedMessage(), Snackbar.LENGTH_LONG).show();
        }

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);

        fab.hide();

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,EditMailActivity.class);
                MainActivity.this.startActivity(intent);
            }
        });

        Switch switchSSL=(Switch)findViewById(R.id.swSSL);
        switchSSL.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                changeSSL(b);
                checkAllParameters();
            }

        });

        LinearLayout openDialog=findViewById(R.id.layoutPortCert);
        openDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                OpenFileDialog openFileDialog=new OpenFileDialog(MainActivity.this);
                openFileDialog.setOpenDialogListener(new OpenFileDialog.OpenDialogListener() {
                    @Override
                    public void OnSelectedFile(String fileName) {
                            ((TextView) findViewById(R.id.tvCert)).setText(fileName);
                            checkAllParameters();
                    }
                    });
                openFileDialog.show();
            }
        });

        LinearLayout openMailBox=findViewById(R.id.layoutMailBox);
        openMailBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent _intent=new Intent(MainActivity.this,MailBoxActivity.class);
                MainActivity.this.startActivity(_intent);
            }
        });


        EditText _etDomain=((EditText)findViewById(R.id.etDomain));
        EditText _etPort=((EditText)findViewById(R.id.etPort));

        EditText _etPortSsl=((EditText)findViewById(R.id.etPortSsl));

        TextWatcher textWatcher=new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                handler.removeCallbacks(input_finish_checker);
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() > 0) {
                    last_text_edit = System.currentTimeMillis();
                    handler.postDelayed(input_finish_checker, delay);
                }
            }
        };
        _etDomain.addTextChangedListener(textWatcher);
        _etPort.addTextChangedListener(textWatcher);
        _etPortSsl.addTextChangedListener(textWatcher);

        Button button=findViewById(R.id.bStart);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    ((Button) findViewById(R.id.bStart)).setClickable(false);
                    if (!mBound) {
                        disableAllInput();
                        saveAllParameters();
                        ((Button) findViewById(R.id.bStart)).setText("");
                        ((ProgressBar) findViewById(R.id.progressBar)).setVisibility(View.VISIBLE);
                        if (mIntentService==null)
                            mIntentService=new Intent(MainActivity.this,ESTMPService.class);
                        mIntentService.putExtra(getString(R.string.SETTINGS_SSL), ((Switch) findViewById(R.id.swSSL)).isChecked());
                        mIntentService.putExtra(getString(R.string.SETTINGS_DOMAIN), ((EditText) findViewById(R.id.etDomain)).getText().toString());
                        mIntentService.putExtra(getString(R.string.SETTINGS_PORT), Integer.parseInt(((EditText) findViewById(R.id.etPort)).getText().toString()));
                        if (((Switch) findViewById(R.id.swSSL)).isChecked()){
                            mIntentService.putExtra(getString(R.string.SETTINGS_PATH), ((TextView) findViewById(R.id.tvCert)).getText().toString());
                            mIntentService.putExtra(getString(R.string.SETTINGS_PASS), ((EditText) findViewById(R.id.etPassCert)).getText().toString());
                            mIntentService.putExtra(getString(R.string.SETTINGS_PORT_SSL), Integer.parseInt(((EditText) findViewById(R.id.etPortSsl)).getText().toString()));
                        }

                        registerReceiver();
                        MainActivity.this.startService(mIntentService);
                    }
                    else {
                        try {
                            if (mIntentService!=null)
                                MainActivity.this.stopService(mIntentService);
                        }catch (Exception e){
                            /*Toast toast = Toast.makeText(getApplicationContext(),
                            getString(R.string.errorstop) + e.getLocalizedMessage(),
                            Toast.LENGTH_SHORT);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();*/
                            Snackbar.make(getWindow().getDecorView().getRootView(), getString(R.string.errorstop) + e.getLocalizedMessage(), Snackbar.LENGTH_LONG).show();
                        }finally {
                            setStateButton(true);
                            enableAllInput();
                        }
                    }

            }
        });

        mIntentService=new Intent(MainActivity.this, ESTMPService.class);
        mConnection= new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName className,
                                           final IBinder service) {
                mBound = true;
                 disableAllInput();
                 setStateButton(false);
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                if(estmpReceiver != null)
                    unregisterReceiver(estmpReceiver);

                mService=null;
                unbindService(mConnection);
                bindService(mIntentService,mConnection ,0 );
                mBound = false;
                setStateButton(true);
            }
        };
        bindService(mIntentService, mConnection, 0);

        mDbHelper = new DBHelper(getApplicationContext());
        updateCounts();

        loadAllParameters();

    }

    private void setStateButton(final boolean status){
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
        ((ProgressBar) findViewById(R.id.progressBar)).setVisibility(View.GONE);
        if (!status){
            ((Button) findViewById(R.id.bStart)).setBackgroundResource(R.drawable.rounded_button_negative);
            ((Button) findViewById(R.id.bStart)).setText(getString(R.string.stop));
            ((Button) findViewById(R.id.bStart)).setClickable(true);
            ((FloatingActionButton)findViewById(R.id.fab)).show();
        }else {
            ((Button) findViewById(R.id.bStart)).setClickable(true);
            ((Button) findViewById(R.id.bStart)).setBackgroundResource(R.drawable.rounded_button);
            ((Button) findViewById(R.id.bStart)).setText(getString(R.string.start));
            ((FloatingActionButton)findViewById(R.id.fab)).hide();
        }
            }});
    }
    Intent mIntentService;
    private ServiceConnection mConnection;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBound){
            unbindService(mConnection);
        }
    }
    private void registerReceiver() {
        estmpReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, final Intent _intent) {
                if (_intent.getAction().equals(END_LOAD_SERVER_EVENT)) {
                    MainActivity.this
                            .runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (_intent.hasExtra(MainActivity.MESSAGE_TAG)) {
                                        setStateButton(true);
                                        enableAllInput();
                                    }else {
                                        setStateButton(false);
                                    }
                                }
                            });
                }
            }
        };
        registerReceiver(estmpReceiver, new IntentFilter(END_LOAD_SERVER_EVENT));
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_update) {
            updateCounts();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
