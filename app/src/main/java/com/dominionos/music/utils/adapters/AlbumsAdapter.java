package com.dominionos.music.utils.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.util.Pair;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.dominionos.music.R;
import com.dominionos.music.utils.items.AlbumListItem;
import com.dominionos.music.task.ColorGridTask;
import com.dominionos.music.ui.layouts.activity.AlbumActivity;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.List;

public class AlbumsAdapter extends RecyclerView.Adapter<AlbumsAdapter.SimpleItemViewHolder>
        implements FastScrollRecyclerView.SectionedAdapter {

    private final List<AlbumListItem> items;
    private final Context context;

    @NonNull
    @Override
    public String getSectionName(int position) {
        return items.get(position).getName().substring(0,1);
    }

    public final static class SimpleItemViewHolder extends RecyclerView.ViewHolder {
        public final TextView albumName;
        public final TextView albumDesc;
        final ImageView albumArt;
        final View realBackground;
        public final View textHolder;

        SimpleItemViewHolder(View view) {
            super(view);

            albumName = (TextView) view.findViewById(R.id.grid_name);
            albumDesc = (TextView) view.findViewById(R.id.grid_desc);
            albumArt = (ImageView) view.findViewById(R.id.grid_art);
            textHolder = view.findViewById(R.id.text_holder);
            realBackground = view.findViewById(R.id.real_background);
        }
    }

    public AlbumsAdapter(Context context, List<AlbumListItem> items) {
        this.context = context;
        this.items = items;
    }

    @Override
    public AlbumsAdapter.SimpleItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).
                inflate(R.layout.grid_item, parent, false);


        return new SimpleItemViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final SimpleItemViewHolder holder, int position) {
        position = holder.getAdapterPosition();
        holder.albumName.setText(items.get(position).getName());
        holder.albumDesc.setText(items.get(position).getDesc());
        int backCardColor = ResourcesCompat.getColor(context.getResources(), R.color.card_background, null);
        final int finalPosition = position;
        if (((ColorDrawable) holder.textHolder.getBackground()).getColor() != backCardColor)
            holder.textHolder.setBackgroundColor(backCardColor);
        try {
            Picasso.with(context).load(new File(items.get(position).getArtString()))
                    .error(R.drawable.default_artwork_dark)
                    .into(holder.albumArt, new Callback() {
                        @Override
                        public void onSuccess() {
                            new ColorGridTask(context, items.get(finalPosition).getArtString(), holder).execute();
                        }

                        @Override
                        public void onError() {

                        }
                    });
        } catch (Exception e) {
            Picasso.with(context).load(R.drawable.default_artwork_dark)
                    .into(holder.albumArt);
        }
        holder.realBackground.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, AlbumActivity.class);
                intent.putExtra("albumName", items.get(finalPosition).getName());
                intent.putExtra("albumId", items.get(finalPosition).getId());
                String transitionName = "albumArt";
                ActivityOptionsCompat options =
                        ActivityOptionsCompat.makeSceneTransitionAnimation((Activity) context,
                                new Pair<View, String>(holder.albumArt, transitionName)
                        );
                ActivityCompat.startActivity(context, intent, options.toBundle());
            }
        });
    }


    @Override
    public int getItemCount() {
        return this.items.size();
    }
}

