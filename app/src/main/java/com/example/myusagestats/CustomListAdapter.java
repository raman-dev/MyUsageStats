package com.example.myusagestats;

import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

class CustomListAdapter extends RecyclerView.Adapter<CustomListAdapter.MyViewHolder> {

    private ArrayList<AppInfoWrapper> list;

    public CustomListAdapter(ArrayList<AppInfoWrapper> list) {
        this.list = list;
    }

    class MyViewHolder extends RecyclerView.ViewHolder{
        private TextView nameTextView;
        private ImageView appIconView;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            appIconView = itemView.findViewById(R.id.appIconView);
        }
    }

    @NonNull
    @Override
    public CustomListAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        return new MyViewHolder(layoutInflater.inflate(R.layout.list_item,parent,false));
    }

    @Override
    public void onBindViewHolder(@NonNull CustomListAdapter.MyViewHolder holder, int position) {
        AppInfoWrapper app = list.get(position);
        holder.nameTextView.setText(app.appName);
        holder.appIconView.setImageDrawable(app.icon);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }


}
