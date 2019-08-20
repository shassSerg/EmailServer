package com.shass.emailserver.Adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.shass.emailserver.R;
import com.shass.emailserver.Tables.Connected;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.List;

public class ConnectedAdapter extends RecyclerView.Adapter<ConnectedAdapter.ViewHolder> {

    private Context context;
    private List<Connected> connectedList;

    public class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView imView;
        public TextView tvIP;
        public TextView tvDate;
        public TextView tvCount;
        public TextView tvId;



        public ViewHolder(View view) {
            super(view);
            imView = view.findViewById(R.id.imView);
            tvIP = view.findViewById(R.id.tvIP);
            tvId = view.findViewById(R.id.tvID);
            tvDate = view.findViewById(R.id.tvDate);
            tvCount = view.findViewById(R.id.tvCountConnected);
        }
    }


    public ConnectedAdapter(Context context, List<Connected> notesList) {
        this.context = context;
        this.connectedList = notesList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.connected_layout, parent, false);

        return new ViewHolder(itemView);
    }
    public void updateData(List<Connected> list) {
        connectedList.clear();
        connectedList.addAll(list);
        notifyDataSetChanged();
    }
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Connected connected = connectedList.get(position);

        holder.tvDate.setText(connected.date);
        holder.tvId.setText("#"+String.valueOf(connected.id));
        holder.tvCount.setText(this.context.getApplicationContext().getText((R.string.tvconnectedcount))+ " "+String.valueOf(connected.connected_count));
        holder.tvIP.setText(connected.ip);
    }

    @Override
    public int getItemCount() {
        return connectedList.size();
    }
}