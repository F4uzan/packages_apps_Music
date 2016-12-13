package com.dominionos.music.utils.adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.dominionos.music.R;
import com.dominionos.music.service.MusicService;
import com.dominionos.music.utils.MySQLiteHelper;
import com.dominionos.music.utils.items.Playlist;
import com.dominionos.music.utils.items.SongListItem;

import java.util.List;

public class PlayingSongAdapter extends RecyclerView.Adapter<PlayingSongAdapter.SimpleItemViewHolder> {

    private final List<SongListItem> songs;
    private Context context;

    final static class SimpleItemViewHolder extends RecyclerView.ViewHolder {
        TextView title, desc;
        public View view;
        public ImageView menu;

        SimpleItemViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.song_item_name);
            desc = (TextView) itemView.findViewById(R.id.song_item_desc);
            view = itemView;
            menu = (ImageView) itemView.findViewById(R.id.song_item_menu);
        }
    }

    public PlayingSongAdapter(Context context, List<SongListItem> songs) {
        this.context = context;
        this.songs = songs;
    }

    @Override
    public PlayingSongAdapter.SimpleItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).
                inflate(R.layout.song_list_item, parent, false);
        return new SimpleItemViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(SimpleItemViewHolder holder, final int position) {
        holder.title.setText(songs.get(position).getName());
        holder.desc.setText(songs.get(position).getDesc());
        holder.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent a = new Intent();
                a.setAction(MusicService.ACTION_PLAY_FROM_PLAYLIST);
                a.putExtra("playListId", songs.get(position).getId() + "");
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
                        switch (item.getItemId()) {
                            case R.id.menu_play_next:
                                Intent a = new Intent();
                                a.setAction(MusicService.ACTION_MENU_FROM_PLAYLIST);
                                a.putExtra("count", position);
                                a.putExtra("action", MusicService.ACTION_MENU_PLAY_NEXT);
                                context.sendBroadcast(a);
                                return true;
                            case R.id.menu_remove_playing:
                                Intent b = new Intent();
                                b.setAction(MusicService.ACTION_MENU_FROM_PLAYLIST);
                                b.putExtra("count", position);
                                b.putExtra("action", MusicService.ACTION_MENU_REMOVE_FROM_QUEUE);
                                context.sendBroadcast(b);
                                notifyItemRemoved(position);
                                return true;
                            case R.id.menu_add_playlist:
                                addToPlaylist(position);
                                return true;
                            case R.id.menu_share:
                                Intent c = new Intent();
                                c.setAction(MusicService.ACTION_MENU_FROM_PLAYLIST);
                                c.putExtra("count", (int) songs.get(position).getId());
                                c.putExtra("action", MusicService.ACTION_MENU_SHARE);
                                context.sendBroadcast(c);
                                return true;
                            case R.id.menu_delete:
                                Intent d = new Intent();
                                d.setAction(MusicService.ACTION_MENU_FROM_PLAYLIST);
                                d.putExtra("count", position);
                                d.putExtra("action", MusicService.ACTION_MENU_DELETE);
                                context.sendBroadcast(d);
                                notifyItemRemoved(position);
                                return true;
                        }
                        return false;
                    }
                });
                popupMenu.inflate(R.menu.playing_popup_menu);
                popupMenu.show();
            }
        });
    }

    private void addToPlaylist(int position) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AppTheme_Main));
        alertDialogBuilder.setTitle("Choose playlist");
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.dialog_listview, null);
        MySQLiteHelper sqLiteHelper = new MySQLiteHelper(context);
        List<Playlist> playlists = sqLiteHelper.getAllPlaylist();
        playlists.add(new Playlist(-1, "+ Create new Playlist"));
        RecyclerView gv = (RecyclerView) view.findViewById(R.id.dialog_playlist_rv);
        LinearLayoutManager gridLayoutManager = new LinearLayoutManager(context);
        gridLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        gridLayoutManager.scrollToPosition(0);
        gv.setLayoutManager(gridLayoutManager);
        gv.setHasFixedSize(true);
        alertDialogBuilder.setView(view);
        alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = alertDialogBuilder.create();
        DialogPlaylistAdapter adapter = new DialogPlaylistAdapter(context,
                playlists, new SongListItem(-1, songs.get(position).getName(),
                null, null, null, -1, null, -1), dialog);
        gv.setAdapter(adapter);
        dialog.show();
    }

    @Override
    public int getItemCount() {
        return this.songs.size();
    }

    @Override
    public long getItemId(int position) {
        return songs.get(position).getId();
    }
}
