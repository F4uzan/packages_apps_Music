package com.dominionos.music.utils.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.CountDownTimer;
import android.provider.MediaStore;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.dominionos.music.R;
import com.dominionos.music.service.MusicService;
import com.dominionos.music.utils.MySQLiteHelper;
import com.dominionos.music.utils.items.SongListItem;

import java.io.File;
import java.util.List;


public class PlaylistActivityAdapter extends RecyclerView.Adapter<PlaylistActivityAdapter.MainViewHolder> {

    private final Context context;
    private final List<SongListItem> data;
    private final int playlistId;

    public PlaylistActivityAdapter(Context context, List<SongListItem> data, int playlistId) {
        this.data = data;
        this.context = context;
        this.playlistId = playlistId;
    }

    @Override
    public MainViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.song_list_item, parent, false);
        return new MainViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final MainViewHolder holder, int position) {
        position = holder.getAdapterPosition();
        holder.songName.setText(data.get(position).getName());
        holder.songDesc.setText(data.get(position).getDesc());
        final int finalPosition = position;
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
                                i.putExtra("song", data.get(finalPosition));
                                context.sendBroadcast(i);
                                return true;
                            case R.id.menu_add_playing:
                                i.setAction(MusicService.ACTION_ADD_SONG);
                                i.putExtra("song", data.get(finalPosition));
                                context.sendBroadcast(i);
                                return true;
                            case R.id.menu_remove_playlist:
                                MySQLiteHelper helper = new MySQLiteHelper(context);
                                helper.removeSong(data.get(finalPosition).getId(), playlistId);
                                data.remove(finalPosition);
                                notifyItemRemoved(finalPosition);
                                updateListWithInterval();
                                return true;
                            case R.id.menu_share:
                                Intent share = new Intent(Intent.ACTION_SEND);
                                share.setType("audio/*");
                                share.putExtra(Intent.EXTRA_STREAM, Uri.parse("file:///" + data.get(finalPosition).getPath()));
                                context.startActivity(Intent.createChooser(share, context.getString(R.string.share_song)));
                                return true;
                            case R.id.menu_delete:
                                File file = new File(data.get(finalPosition).getPath());
                                boolean deleted = file.delete();
                                if (deleted) {
                                    Toast.makeText(context, R.string.song_delete_success, Toast.LENGTH_SHORT).show();
                                    context.getContentResolver().delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                            MediaStore.MediaColumns._ID + "='" + data.get(finalPosition).getId() + "'", null);
                                    data.remove(finalPosition);
                                    notifyItemRemoved(finalPosition);
                                    updateListWithInterval();
                                } else
                                    Toast.makeText(context, R.string.song_delete_fail, Toast.LENGTH_SHORT).show();
                                return true;
                        }
                        return false;
                    }
                });
                popupMenu.inflate(R.menu.play_list_popup_menu);
                popupMenu.show();
            }
        });
        holder.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent a = new Intent();
                a.setAction(MusicService.ACTION_PLAY_SINGLE);
                a.putExtra("song", data.get(finalPosition));
                context.sendBroadcast(a);
            }
        });
    }

    private void updateListWithInterval() {
        new CountDownTimer(400, 1000) {
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                notifyDataSetChanged();
            }
        }.start();
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class MainViewHolder extends RecyclerView.ViewHolder {

        final View view;
        final TextView songName;
        final TextView songDesc;
        public final View menu;

        MainViewHolder(View itemView) {
            super(itemView);
            view = itemView;
            songName = (TextView) itemView.findViewById(R.id.song_item_name);
            songDesc = (TextView) itemView.findViewById(R.id.song_item_desc);
            menu = itemView.findViewById(R.id.song_item_menu);
        }
    }
}