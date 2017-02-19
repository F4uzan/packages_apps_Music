package com.dominionos.music.utils.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
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
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.io.File;
import java.util.List;

public class SongsAdapter extends RecyclerView.Adapter<SongsAdapter.SimpleItemViewHolder>
        implements FastScrollRecyclerView.SectionedAdapter {

    private final List<SongListItem> items;
    private final Context context;
    private Intent i;

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
        final TextView title;
        final TextView desc;
        final View view;
        final ImageView menu;

        SimpleItemViewHolder(View itemView) {
            super(itemView);
            view = itemView;
            title = (TextView) itemView.findViewById(R.id.song_item_name);
            desc = (TextView) itemView.findViewById(R.id.song_item_desc);
            menu = (ImageView) itemView.findViewById(R.id.song_item_menu);
        }
    }

    public SongsAdapter(Context context, List<SongListItem> items) {
        this.context = context;
        this.items = items;
        i = new Intent();
    }

    @Override
    public SongsAdapter.SimpleItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).
                inflate(R.layout.song_list_item, parent, false);
        return new SimpleItemViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final SimpleItemViewHolder holder, int position) {
        holder.title.setText(items.get(holder.getAdapterPosition()).getName());
        holder.desc.setText(items.get(holder.getAdapterPosition()).getDesc());
        holder.menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popupMenu = new PopupMenu(context, v);
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.menu_play_next:
                                i.setAction(MusicService.ACTION_PLAY_NEXT);
                                i.putExtra("song", items.get(holder.getAdapterPosition()));
                                context.sendBroadcast(i);
                                return true;
                            case R.id.menu_add_playing:
                                i.setAction(MusicService.ACTION_ADD_SONG);
                                i.putExtra("song", items.get(holder.getAdapterPosition()));
                                context.sendBroadcast(i);
                                return true;
                            case R.id.menu_add_playlist:
                                addToPlaylist(holder.getAdapterPosition());
                                return true;
                            case R.id.menu_share:
                                Intent share = new Intent(Intent.ACTION_SEND);
                                share.setType("audio/*");
                                share.putExtra(Intent.EXTRA_STREAM, Uri.parse("file:///" + items.get(holder.getAdapterPosition()).getPath()));
                                context.startActivity(Intent.createChooser(share, context.getString(R.string.share_song)));
                                return true;
                            case R.id.menu_delete:
                                File file = new File(items.get(holder.getAdapterPosition()).getPath());
                                boolean deleted = file.delete();
                                if (deleted) {
                                    Toast.makeText(context, R.string.song_delete_success, Toast.LENGTH_SHORT).show();
                                    context.getContentResolver().delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                            MediaStore.MediaColumns._ID + "='" + items.get(holder.getAdapterPosition()).getId() + "'", null);
                                    notifyItemRemoved(holder.getAdapterPosition());
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
        holder.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    Intent a = new Intent();
                    a.setAction(MusicService.ACTION_PLAY_SINGLE);
                    a.putExtra("song", items.get(holder.getAdapterPosition()));
                    context.sendBroadcast(a);
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
