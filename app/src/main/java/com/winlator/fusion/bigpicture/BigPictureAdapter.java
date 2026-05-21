package com.winlator.fusion.bigpicture;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.winlator.fusion.R;
import com.winlator.fusion.container.Shortcut;

import java.util.ArrayList;

public class BigPictureAdapter extends RecyclerView.Adapter<BigPictureAdapter.ViewHolder> {
    private ArrayList<Shortcut> shortcuts = new ArrayList<>();

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView iconView;

        public ViewHolder(View view) {
            super(view);
            iconView = view.findViewById(R.id.IVCoverArt);
        }
    }

    public void setShortcuts(ArrayList<Shortcut> shortcuts) {
        this.shortcuts = shortcuts;
        notifyDataSetChanged();
    }

    public Shortcut getItem(int position) {
        return shortcuts.get(position);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.big_picture_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Shortcut shortcut = shortcuts.get(position);
        if (shortcut.icon != null) {
            holder.iconView.setImageBitmap(shortcut.icon);
        }
        holder.itemView.setFocusable(true);
        holder.itemView.setClickable(true);
    }

    @Override
    public int getItemCount() {
        return shortcuts.size();
    }
}
