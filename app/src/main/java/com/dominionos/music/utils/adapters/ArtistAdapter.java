package com.dominionos.music.utils.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.dominionos.music.R;
import com.dominionos.music.ui.layouts.activity.ArtistActivity;
import com.dominionos.music.utils.ArtistImgHandler;
import com.dominionos.music.utils.Utils;
import com.dominionos.music.utils.items.ArtistListItem;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.io.File;
import java.util.List;

public class ArtistAdapter extends RecyclerView.Adapter<ArtistAdapter.SimpleItemViewHolder>
        implements FastScrollRecyclerView.SectionedAdapter {

    private final List<ArtistListItem> items;
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
        final TextView artistName;
        final TextView artistDesc;
        final ImageView artistImg;
        final View view;

        SimpleItemViewHolder(View itemView) {
            super(itemView);
            artistName = (TextView) itemView.findViewById(R.id.artist_name);
            artistDesc = (TextView) itemView.findViewById(R.id.artist_desc);
            artistImg = (ImageView) itemView.findViewById(R.id.artist_image);
            view = itemView;
        }
    }

    public ArtistAdapter(Context context, List<ArtistListItem> items) {
        this.items = items;
        this.context = context;
    }

    @Override
    public ArtistAdapter.SimpleItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).
                inflate(R.layout.artist_list_item, parent, false);
        return new SimpleItemViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(SimpleItemViewHolder holder, int position) {
        position = holder.getAdapterPosition();
        int albumCount = items.get(position).getNumOfAlbums();
        int songCount = items.get(position).getNumOfTracks();
        String artistItemsCount;
        holder.artistName.setText(items.get(position).getName());
        getArtistImg(holder, position);

        if(albumCount == 1 && songCount == 1) {
            artistItemsCount = (albumCount + " " + context.getString(R.string.album) + " • " + songCount + " " + context.getString(R.string.song));
        } else if (albumCount == 1) {
            artistItemsCount = (albumCount + " " + context.getString(R.string.album) + " • " + songCount + " " + context.getString(R.string.songs));
        } else if (songCount == 1) {
            artistItemsCount = (albumCount + " " + context.getString(R.string.albums) + " • " + songCount + " " + context.getString(R.string.song));
        } else {
            artistItemsCount = (albumCount + " " + context.getString(R.string.albums) + " • " + songCount + " " + context.getString(R.string.songs));
        }
        holder.artistDesc.setText(artistItemsCount);

        final int finalPosition = position;
        holder.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(context, ArtistActivity.class);
                i.putExtra("artistName", items.get(finalPosition).getName());
                context.startActivity(i);
            }
        });
    }

    private void getArtistImg(final SimpleItemViewHolder holder, int position) {
        ArtistImgHandler imgHandler = new ArtistImgHandler(context) {
            @Override
            public void onDownloadComplete(final String url) {
                if (url != null)
                    ((Activity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setImageToView(url, holder);
                        }
                    });
            }
        };
        String path = imgHandler.getArtistImgFromDB("name");
        if (path != null && !path.matches("")) {
            setImageToView(path, holder);
        } else {
            String urlIfAny = imgHandler.getArtistArtWork(items.get(position).getName(), position);
            setImageToView(urlIfAny, holder);
        }
    }

    private void setImageToView(String url, final SimpleItemViewHolder holder) {
        int px = Utils.dpToPx(context, 48);
        Glide.with(context).load(new File(url)).error(R.drawable.default_artwork_dark).centerCrop().override(px, px).into(holder.artistImg);
    }

    @Override
    public int getItemCount() {
        return this.items.size();
    }
}
