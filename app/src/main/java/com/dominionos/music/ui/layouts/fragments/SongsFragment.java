package com.dominionos.music.ui.layouts.fragments;

import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.dominionos.music.R;
import com.dominionos.music.utils.adapters.SongsAdapter;
import com.dominionos.music.utils.items.SongListItem;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class SongsFragment extends Fragment {

    private View mainView;
    private FastScrollRecyclerView rv;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_songs, container, false);
        mainView = v;

        init();
        Handler mainHandler = new Handler(mainView.getContext().getMainLooper());

        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                setSongList();
            }
        };
        mainHandler.post(myRunnable);

        return v;
    }

    private void init() {
        rv = (FastScrollRecyclerView) mainView.findViewById(R.id.songs_fragment_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(mainView.getContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        layoutManager.scrollToPosition(0);
        rv.setLayoutManager(layoutManager);
        rv.addItemDecoration(new DividerItemDecoration(rv.getContext(), layoutManager.getOrientation()));
    }

    private void setSongList() {
        final ArrayList<SongListItem> songList = new ArrayList<>();
        final String where = MediaStore.Audio.Media.IS_MUSIC + "=1";
        final String orderBy = MediaStore.Audio.Media.TITLE;
        Cursor musicCursor = mainView.getContext().getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
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
                songList.add(new SongListItem(musicCursor.getLong(idColumn),
                        musicCursor.getString(titleColumn),
                        musicCursor.getString(artistColumn),
                        musicCursor.getString(pathColumn), false,
                        musicCursor.getLong(albumIdColumn),
                        musicCursor.getString(albumColumn)));
            }
            while (musicCursor.moveToNext());
            Collections.sort(songList, new Comparator<SongListItem>() {
                @Override
                public int compare(SongListItem songListItem, SongListItem t1) {
                    return songListItem.getName().compareToIgnoreCase(t1.getName());
                }
            });
        }
        if (musicCursor != null) {
            musicCursor.close();
        }
        if(songList.size() != 0) {
            rv.setAdapter(new SongsAdapter(mainView.getContext(), songList));
        } else {
            getActivity().findViewById(R.id.no_songs).setVisibility(View.VISIBLE);
        }
    }
}
