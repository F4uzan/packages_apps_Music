package com.dominionos.music.ui.layouts.fragments;

import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.dominionos.music.R;
import com.dominionos.music.utils.SpacesItemDecoration;
import com.dominionos.music.utils.adapters.AlbumsAdapter;
import com.dominionos.music.utils.items.AlbumListItem;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class AlbumsFragment extends Fragment {

    private View mainView;
    private FastScrollRecyclerView gv;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_albums, container, false);
        this.mainView = v;
        gv = (FastScrollRecyclerView) mainView.findViewById(R.id.album_grid);
        Handler mainHandler = new Handler(mainView.getContext().getMainLooper());

        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                getAlbumList();
            }
        };
        mainHandler.post(myRunnable);

        return v;
    }

    private void getAlbumList() {
        final ArrayList<AlbumListItem> albumList = new ArrayList<>();
        System.gc();
        final String orderBy = MediaStore.Audio.Albums.ALBUM;
        Cursor musicCursor = mainView.getContext().getContentResolver().
                query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, null, null, null, orderBy);

        if (musicCursor != null && musicCursor.moveToFirst()) {
            //get columns
            int titleColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Albums.ALBUM);
            int idColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Albums._ID);
            int artistColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Albums.ARTIST);
            int numOfSongsColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Albums.NUMBER_OF_SONGS);
            int albumArtColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Albums.ALBUM_ART);
            //add albums to list
            do {
                albumList.add(new AlbumListItem(musicCursor.getLong(idColumn),
                        musicCursor.getString(titleColumn),
                        musicCursor.getString(artistColumn),
                        musicCursor.getString(albumArtColumn)
                ));
            }
            while (musicCursor.moveToNext());
        }
        Collections.sort(albumList, new Comparator<AlbumListItem>() {
            @Override
            public int compare(AlbumListItem albumListItem, AlbumListItem t1) {
                return albumListItem.getName().compareToIgnoreCase(t1.getName());
            }
        });
        if (musicCursor != null) {
            musicCursor.close();
        }
        GridLayoutManager gridLayoutManager = new GridLayoutManager(mainView.getContext(), 2);
        gridLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        gridLayoutManager.scrollToPosition(0);
        gv.setLayoutManager(gridLayoutManager);
        gv.addItemDecoration(new SpacesItemDecoration(8, 2));
        gv.setHasFixedSize(true);
        gv.setAdapter(new AlbumsAdapter(mainView.getContext(), albumList));

    }
}