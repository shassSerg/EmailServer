package com.shass.emailserver;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.shass.emailserver.Adapters.ConnectedAdapter;
import com.shass.emailserver.Adapters.MailAdapter;
import com.shass.emailserver.EmailServer.DataMail;
import com.shass.emailserver.EmailServer.DomainUtils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class EditMailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_mail);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        Bundle bundle = getIntent().getExtras();
        if (bundle!=null) {
            String value="";
            if (bundle.containsKey(getString(R.string.MAIL_DOMAIN))) {
                value = bundle.getString(getString(R.string.MAIL_DOMAIN));
                ((EditText) findViewById(R.id.etDomain)).setText(value, TextView.BufferType.NORMAL);
            }

            if (bundle.containsKey(getString(R.string.MAIL_FROM))) {
                value = bundle.getString(getString(R.string.MAIL_FROM));
                ((EditText) findViewById(R.id.etFrom)).setText(value, TextView.BufferType.NORMAL);
            }

            if (bundle.containsKey(getString(R.string.MAIL_TO))) {
                value = bundle.getString(getString(R.string.MAIL_TO));
                ((EditText) findViewById(R.id.etTo)).setText(value, TextView.BufferType.NORMAL);
            }

            if (bundle.containsKey(getString(R.string.MAIL_SUBJECT))) {
                value = bundle.getString(getString(R.string.MAIL_SUBJECT));
                ((EditText) findViewById(R.id.etSubject)).setText(value, TextView.BufferType.NORMAL);
            }

            if (bundle.containsKey(getString(R.string.MAIL_BODY))) {
                value = bundle.getString(getString(R.string.MAIL_BODY));
                ((EditText) findViewById(R.id.etBody)).setText(value, TextView.BufferType.NORMAL);
            }

            if (bundle.containsKey(getString(R.string.MAIL_DATE))) {
                value  = bundle.getString(getString(R.string.MAIL_DATE));
            }else {
                SimpleDateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");
                value=format.format(new Date());
            }
            ((TextView) findViewById(R.id.tvDate)).setText(value);

        }else{
            SimpleDateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");
            String value=format.format(new Date());
            ((TextView) findViewById(R.id.tvDate)).setText(value);
        }

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

        ((EditText)findViewById(R.id.etDomain)).addTextChangedListener(textWatcher);
        ((EditText)findViewById(R.id.etFrom)).addTextChangedListener(textWatcher);
        ((EditText)findViewById(R.id.etTo)).addTextChangedListener(textWatcher);

        checkAllValues();
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkAllValues())
                {
                    Intent intent = new Intent(MainActivity.SENT_MAIL_SERVER_EVENT);
                    intent.putExtra(getString(R.string.MAIL_DOMAIN), ((EditText) findViewById(R.id.etDomain)).getText().toString());
                    intent.putExtra(getString(R.string.MAIL_FROM), ((EditText) findViewById(R.id.etFrom)).getText().toString());
                    intent.putExtra(getString(R.string.MAIL_TO), ((EditText) findViewById(R.id.etTo)).getText().toString());
                    intent.putExtra(getString(R.string.MAIL_SUBJECT), ((EditText) findViewById(R.id.etSubject)).getText().toString());
                    intent.putExtra(getString(R.string.MAIL_BODY), ((EditText) findViewById(R.id.etBody)).getText().toString());
                    sendBroadcast(intent);

                    EditMailActivity.this.finish();
                }
            }
        });
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {

            case android.R.id.home:
                EditMailActivity.this.finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    long delay = 300; // 1 seconds after user stops typing
    long last_text_edit = 0;
    Handler handler = new Handler();

    private Runnable input_finish_checker = new Runnable() {
        public void run() {
            //if (System.currentTimeMillis() > (last_text_edit + delay)) {
            checkAllValues();
            //}
        }
    };
    private boolean checkAllValues(){

        boolean result=true;

        String from=((EditText)findViewById(R.id.etFrom)).getText().toString();
        String to=((EditText)findViewById(R.id.etTo)).getText().toString();
        String domain=((EditText)findViewById(R.id.etDomain)).getText().toString();

        if (domain==null || domain.isEmpty() || !DomainUtils.isValidDomainName(domain)) {
            ((ImageView) findViewById(R.id.ivDomain)).setImageResource(android.R.drawable.presence_busy);
            result = false;
        }else
            ((ImageView) findViewById(R.id.ivDomain)).setImageResource(android.R.drawable.presence_online);

        DataMail mail=new DataMail();
        if (from==null || from.isEmpty()) {
            ((ImageView) findViewById(R.id.ivFrom)).setImageResource(android.R.drawable.presence_busy);
            result = false;
        }
        else {
            try {
                mail.setMAIL_FROM(from);
                ((ImageView)findViewById(R.id.ivFrom)).setImageResource(android.R.drawable.presence_online);
            }catch (Exception e){
                ((ImageView)findViewById(R.id.ivFrom)).setImageResource(android.R.drawable.presence_busy);
                result=false;
            }
        }
        if (to==null || to.isEmpty()) {
            ((ImageView) findViewById(R.id.ivTo)).setImageResource(android.R.drawable.presence_busy);
            result=false;
        }
        else {
            try {
                mail.setRCPT_TO(to);
                ((ImageView)findViewById(R.id.ivTo)).setImageResource(android.R.drawable.presence_online);
            }catch (Exception e){
                ((ImageView)findViewById(R.id.ivTo)).setImageResource(android.R.drawable.presence_busy);
                result=false;
            }
        }

        if (((EditText)findViewById(R.id.etSubject)).getText().toString().isEmpty() || ((EditText)findViewById(R.id.etBody)).getText().toString().isEmpty()){
            Snackbar.make(getWindow().getDecorView().getRootView(), getString(R.string.MAIL_SUBJECT)+"/"+getString(R.string.MAIL_BODY), Snackbar.LENGTH_LONG).show();
            result=false;
        }


        return result;
    }
}
