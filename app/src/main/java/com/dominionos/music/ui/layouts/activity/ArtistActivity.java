package com.dominionos.music.ui.layouts.activity;


import android.content.Context;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.MenuItem;

import com.dominionos.music.R;
import com.dominionos.music.utils.adapters.SongsAdapter;
import com.dominionos.music.utils.items.SongListItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ArtistActivity extends AppCompatActivity {

    private AudioManager audioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist);
        String artistName = getIntent().getStringExtra("artistName");

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        Toolbar toolbar = (Toolbar) findViewById(R.id.artist_toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(artistName);
        }

        RecyclerView rv = (RecyclerView) findViewById(R.id.artist_recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        layoutManager.scrollToPosition(0);
        rv.setLayoutManager(layoutManager);

        final ArrayList<SongListItem> songList = new ArrayList<>();
        final String where = MediaStore.Audio.Media.IS_MUSIC + "=1";
        final String orderBy = MediaStore.Audio.Media.TITLE;
        Cursor musicCursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null, where, null, orderBy);
        if (musicCursor != null && musicCursor.moveToFirst()) {
            int titleColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);
            int pathColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.DATA);
            int albumIdColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.ALBUM_ID);
            int albumColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media.ALBUM);
            do {
                if(artistName.equals(musicCursor.getString(artistColumn))) {
                    songList.add(new SongListItem(musicCursor.getLong(idColumn),
                            musicCursor.getString(titleColumn),
                            musicCursor.getString(artistColumn),
                            musicCursor.getString(pathColumn), false,
                            musicCursor.getLong(albumIdColumn),
                            musicCursor.getString(albumColumn)));
                }
            }
            while (musicCursor.moveToNext());
            Collections.sort(songList, new Comparator<SongListItem>() {
                @Override
                public int compare(SongListItem songListItem, SongListItem t1) {
                    return songListItem.getName().compareTo(t1.getName());
                }
            });
            rv.setAdapter(new SongsAdapter(this, songList));
        }
        if (musicCursor != null) {
            musicCursor.close();
        }

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch(keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                break;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                break;
            case KeyEvent.KEYCODE_VOLUME_MUTE:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_TOGGLE_MUTE, AudioManager.FLAG_SHOW_UI);
                }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}
