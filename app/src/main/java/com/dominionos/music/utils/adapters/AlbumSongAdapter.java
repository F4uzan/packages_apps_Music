package com.dominionos.music.utils.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.dominionos.music.R;
import com.dominionos.music.service.MusicService;
import com.dominionos.music.utils.Utils;
import com.dominionos.music.utils.items.SongListItem;

import java.io.File;
import java.util.List;

public class AlbumSongAdapter extends RecyclerView.Adapter<AlbumSongAdapter.SimpleItemViewHolder> {

    private final List<SongListItem> items;
    private final Context context;

    final static class SimpleItemViewHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView desc;
        public final View view;
        public final ImageView menu;

        SimpleItemViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.song_item_name);
            desc = (TextView) itemView.findViewById(R.id.song_item_desc);
            view = itemView;
            menu = (ImageView) itemView.findViewById(R.id.song_item_menu);
        }
    }

    public AlbumSongAdapter(Context context, List<SongListItem> items) {
        this.context = context;
        this.items = items;
    }

    @Override
    public AlbumSongAdapter.SimpleItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).
                inflate(R.layout.song_list_item, parent, false);
        return new SimpleItemViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(SimpleItemViewHolder holder, int position) {
        position = holder.getAdapterPosition();
        holder.title.setText(items.get(position).getName());
        holder.desc.setText(items.get(position).getDesc());
        final int finalPosition = position;
        holder.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent a = new Intent();
                a.setAction(MusicService.ACTION_PLAY_SINGLE);
                a.putExtra("song", items.get(finalPosition));
                context.sendBroadcast(a);
            }
        });

        holder.menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popupMenu = new PopupMenu(context, v);
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        Intent i = new Intent();
                        switch (item.getItemId()) {
                            case R.id.menu_play_next:
                                i.setAction(MusicService.ACTION_PLAY_NEXT);
                                i.putExtra("song", items.get(finalPosition));
                                context.sendBroadcast(i);
                                return true;
                            case R.id.menu_add_playing:
                                i.setAction(MusicService.ACTION_ADD_SONG);
                                i.putExtra("song", items.get(finalPosition));
                                context.sendBroadcast(i);
                                return true;
                            case R.id.menu_add_playlist:
                                addToPlaylist(finalPosition);
                                return true;
                            case R.id.menu_share:
                                Intent share = new Intent(Intent.ACTION_SEND);
                                share.setType("audio/*");
                                share.putExtra(Intent.EXTRA_STREAM, Uri.parse("file:///" + items.get(finalPosition).getPath()));
                                context.startActivity(Intent.createChooser(share, context.getString(R.string.share_song)));
                                return true;
                            case R.id.menu_delete:
                                File file = new File(items.get(finalPosition).getPath());
                                boolean deleted = file.delete();
                                if (deleted) {
                                    Toast.makeText(context, R.string.song_delete_success, Toast.LENGTH_SHORT).show();
                                    context.getContentResolver().delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                            MediaStore.MediaColumns._ID + "='" + items.get(finalPosition).getId() + "'", null);
                                    notifyItemRemoved(finalPosition);
                                } else
                                    Toast.makeText(context, R.string.song_delete_fail, Toast.LENGTH_SHORT).show();
                                return true;
                        }
                        return false;
                    }
                });
                popupMenu.inflate(R.menu.song_popup_menu);
                popupMenu.show();
            }
        });
    }


    private void addToPlaylist(int position) {
        SongListItem item = items.get(position);
        Utils utils = new Utils();
        utils.addToPlaylistDialog(context, item);
    }


    @Override
    public int getItemCount() {
        return this.items.size();
    }
}
