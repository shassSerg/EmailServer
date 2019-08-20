package com.shass.emailserver.Adapters;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.shass.emailserver.EditMailActivity;
import com.shass.emailserver.MailBoxActivity;
import com.shass.emailserver.R;
import com.shass.emailserver.Tables.Connected;
import com.shass.emailserver.Tables.Received;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.List;

public class MailAdapter extends RecyclerView.Adapter<MailAdapter.ViewHolder> {

    private Context context;
    private List<Received> receivedList;

    public class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView iconMail;
        public TextView tvFrom;
        public TextView tvTo;
        public TextView tvDate;
        public TextView tvId;
        public TextView tvSubject;

        public LinearLayout mainLayout;

        public ViewHolder(View view) {
            super(view);
            iconMail = view.findViewById(R.id.ivMailIcon);
            tvId = view.findViewById(R.id.tvID);
            tvDate = view.findViewById(R.id.tvDate);
            tvFrom = view.findViewById(R.id.tvFrom);
            tvTo = view.findViewById(R.id.tvTo);
            tvSubject = view.findViewById(R.id.tvSubject);
            mainLayout = view.findViewById(R.id.mailLayout);
        }
    }


    public MailAdapter(Context context, List<Received> notesList) {
        this.context = context;
        this.receivedList = notesList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.mail_layout, parent, false);

        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Received received = receivedList.get(position);

        holder.tvDate.setText(received.date);
        holder.tvId.setText("#"+String.valueOf(received.id));
        holder.tvFrom.setText(this.context.getApplicationContext().getText((R.string.tvFrom))+ " "+String.valueOf(received.from));
        holder.tvTo.setText(this.context.getApplicationContext().getText((R.string.tvTo))+ " "+String.valueOf(received.to));
        holder.tvSubject.setText(this.context.getApplicationContext().getText((R.string.tvSubject))+ " "+String.valueOf(received.subject));

        if (received.type!=0)
            holder.iconMail.setImageResource(R.drawable.mail);
        else
            holder.iconMail.setImageResource(R.drawable.mailout);

        holder.mainLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(context,EditMailActivity.class);

                intent.putExtra(context.getApplicationContext().getString(R.string.MAIL_DOMAIN), received.ip);
                intent.putExtra(context.getApplicationContext().getString(R.string.MAIL_FROM), received.from);
                intent.putExtra(context.getApplicationContext().getString(R.string.MAIL_TO), received.to);
                intent.putExtra(context.getApplicationContext().getString(R.string.MAIL_SUBJECT), received.subject);
                intent.putExtra(context.getApplicationContext().getString(R.string.MAIL_DATE), received.date);
                intent.putExtra(context.getApplicationContext().getString(R.string.MAIL_BODY), received.body);

                context.startActivity(intent);
            }
        });

    }
    public void updateData(List<Received> list) {
        receivedList.clear();
        receivedList.addAll(list);
        notifyDataSetChanged();
    }
    @Override
    public int getItemCount() {
        return receivedList.size();
    }
}