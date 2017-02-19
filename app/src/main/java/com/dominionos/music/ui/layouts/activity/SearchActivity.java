package com.dominionos.music.ui.layouts.activity;

import android.database.Cursor;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.dominionos.music.R;
import com.dominionos.music.utils.adapters.SongsAdapter;
import com.dominionos.music.utils.items.SongListItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class SearchActivity extends AppCompatActivity {

    private RecyclerView searchList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        Toolbar toolbar = (Toolbar) findViewById(R.id.search_toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        searchList = (RecyclerView) findViewById(R.id.search_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        layoutManager.scrollToPosition(0);
        searchList.setLayoutManager(layoutManager);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.search_menu, menu);
        final SearchView search = (SearchView) menu.findItem(R.id.action_search).getActionView();
        search.setIconified(false);
        search.setMaxWidth(Integer.MAX_VALUE);
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                final ArrayList<SongListItem> searchResults = new ArrayList<>();
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
                        if(musicCursor.getString(titleColumn).toLowerCase().trim().contains(query.toLowerCase())) {
                            searchResults.add(new SongListItem(musicCursor.getLong(idColumn),
                                    musicCursor.getString(titleColumn),
                                    musicCursor.getString(artistColumn),
                                    musicCursor.getString(pathColumn), false,
                                    musicCursor.getLong(albumIdColumn),
                                    musicCursor.getString(albumColumn)));
                        }
                    }
                    while (musicCursor.moveToNext());
                    Collections.sort(searchResults, new Comparator<SongListItem>() {
                        @Override
                        public int compare(SongListItem songListItem, SongListItem t1) {
                            return songListItem.getName().compareTo(t1.getName());
                        }
                    });
                    searchList.setAdapter(new SongsAdapter(SearchActivity.this, searchResults));
                    musicCursor.close();
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                final ArrayList<SongListItem> searchResults = new ArrayList<>();
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
                        if(musicCursor.getString(titleColumn).toLowerCase().trim().contains(newText.toLowerCase())) {
                            searchResults.add(new SongListItem(musicCursor.getLong(idColumn),
                                    musicCursor.getString(titleColumn),
                                    musicCursor.getString(artistColumn),
                                    musicCursor.getString(pathColumn), false,
                                    musicCursor.getLong(albumIdColumn),
                                    musicCursor.getString(albumColumn)));
                        }
                    }
                    while (musicCursor.moveToNext());
                    Collections.sort(searchResults, new Comparator<SongListItem>() {
                        @Override
                        public int compare(SongListItem songListItem, SongListItem t1) {
                            return songListItem.getName().compareTo(t1.getName());
                        }
                    });
                    SongsAdapter adapter = new SongsAdapter(SearchActivity.this, searchResults);
                    if(searchList.getAdapter() == null) {
                        searchList.setAdapter(adapter);
                    } else {
                        searchList.swapAdapter(adapter, false);
                    }
                    musicCursor.close();
                }
                return true;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}
