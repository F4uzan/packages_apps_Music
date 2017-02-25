package com.dominionos.music.utils.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.util.Pair;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.bumptech.glide.request.target.Target;
import com.dominionos.music.R;
import com.dominionos.music.utils.items.AlbumListItem;
import com.dominionos.music.ui.layouts.activity.AlbumActivity;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.io.File;
import java.util.List;

public class AlbumsAdapter extends RecyclerView.Adapter<AlbumsAdapter.SimpleItemViewHolder>
        implements FastScrollRecyclerView.SectionedAdapter {

    private final List<AlbumListItem> items;
    private final Context context;

    @NonNull
    @Override
    public String getSectionName(int position) {
        String character = items.get(position).getName().substring(0, 1);
        if(character.matches("[a-zA-Z]")) {
            return items.get(position).getName().substring(0,1);
        } else {
            return "\u2605";
        }
    }

    final static class SimpleItemViewHolder extends RecyclerView.ViewHolder {
        final TextView albumName;
        final TextView albumDesc;
        final ImageView albumArt;
        final View realBackground;
        final View textHolder;

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
        final int adapterPosition = holder.getAdapterPosition();
        holder.albumName.setText(items.get(adapterPosition).getName());
        String albumDesc;
        String desc = items.get(adapterPosition).getDesc();
        int songCount = items.get(adapterPosition).getSongCount();
        if(songCount != 1) {
            albumDesc = desc + " • " + songCount + " " + context.getString(R.string.songs);
        } else {
            albumDesc = desc + " • " + songCount + " " + context.getString(R.string.song);
        }
        holder.albumDesc.setText(albumDesc);
        int backCardColor = ResourcesCompat.getColor(context.getResources(), R.color.card_background, null);
        if (((ColorDrawable) holder.textHolder.getBackground()).getColor() != backCardColor)
            holder.textHolder.setBackgroundColor(backCardColor);
            Glide.with(context)
                    .load(new File(items.get(adapterPosition).getArtString()))
                    .asBitmap()
                    .error(R.drawable.default_art)
                    .listener(new RequestListener<File, Bitmap>() {
                        @Override
                        public boolean onException(Exception e, File model, Target<Bitmap> target, boolean isFirstResource) {
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Bitmap resource, File model, Target<Bitmap> target, boolean isFromMemoryCache, boolean isFirstResource) {
                            Palette.PaletteAsyncListener paletteAsyncListener = new Palette.PaletteAsyncListener() {
                                @Override
                                public void onGenerated(Palette palette) {
                                    Palette.Swatch swatch;
                                    if(palette.getVibrantSwatch() != null) {
                                        swatch = palette.getVibrantSwatch();
                                        holder.textHolder.setBackgroundColor(swatch.getRgb());
                                        holder.albumName.setTextColor(swatch.getTitleTextColor());
                                        holder.albumDesc.setTextColor(swatch.getBodyTextColor());
                                    } else if(palette.getDominantSwatch() != null) {
                                        swatch = palette.getDominantSwatch();
                                        holder.textHolder.setBackgroundColor(swatch.getRgb());
                                        holder.albumName.setTextColor(swatch.getTitleTextColor());
                                        holder.albumDesc.setTextColor(swatch.getBodyTextColor());
                                    }
                                }
                            };
                            Palette.from(resource).generate(paletteAsyncListener);
                            return false;
                        }
                    })
                    .into(new BitmapImageViewTarget(holder.albumArt));
        holder.realBackground.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, AlbumActivity.class);
                intent.putExtra("albumName", items.get(adapterPosition).getName());
                intent.putExtra("albumId", items.get(adapterPosition).getId());
                String transitionName = "albumArt";
                Pair albumArt = new Pair<View, String>(holder.albumArt, transitionName);
                ActivityOptionsCompat options =
                        ActivityOptionsCompat.makeSceneTransitionAnimation((Activity) context, albumArt);
                ActivityCompat.startActivity(context, intent, options.toBundle());
            }
        });
    }


    @Override
    public int getItemCount() {
        return this.items.size();
    }
}

