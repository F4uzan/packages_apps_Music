package com.dominionos.music.ui.layouts.fragments;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.dominionos.music.R;
import com.dominionos.music.utils.SimpleItemListDivider;
import com.dominionos.music.utils.SpacesItemDecoration;
import com.dominionos.music.utils.adapters.ArtistAdapter;
import com.dominionos.music.utils.items.ArtistListItem;

import java.util.ArrayList;

public class ArtistsFragment extends Fragment {

    View mainView;
    RecyclerView gv;
    private SharedPreferences settingsPref;
    int currentFabPos;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.artists_fragment, container, false);
        this.mainView = v;

        init();
        Handler mainHandler = new Handler(mainView.getContext().getMainLooper());

        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                getArtistList();
            }
        };
        mainHandler.post(myRunnable);

        return v;
    }

    private void init() {
        settingsPref = PreferenceManager.getDefaultSharedPreferences(mainView.getContext());
        gv = (RecyclerView) mainView.findViewById(R.id.artist_list);
    }

    private void getArtistList() {
        ArrayList<ArtistListItem> albumList = new ArrayList<>();
        System.gc();
        final String orderBy = MediaStore.Audio.Artists.ARTIST;
        Cursor musicCursor = mainView.getContext().getContentResolver().
                query(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, null, null, null, orderBy);

        if (musicCursor != null && musicCursor.moveToFirst()) {
            //get columns
            int titleColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Artists.ARTIST);
            int idColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Artists._ID);
            int numOfAlbumsColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Artists.NUMBER_OF_ALBUMS);
            int numOfTracksColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Artists.NUMBER_OF_TRACKS);
            int artistKeyColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Artists.ARTIST_KEY);
            //add albums to list
            do {
                albumList.add(new ArtistListItem(musicCursor.getLong(idColumn),
                        musicCursor.getString(titleColumn),
                        musicCursor.getInt(numOfTracksColumn),
                        musicCursor.getInt(numOfAlbumsColumn)));
            }
            while (musicCursor.moveToNext());
        }
        if (musicCursor != null) {
            musicCursor.close();
        }

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(mainView.getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        linearLayoutManager.scrollToPosition(0);
        gv.setLayoutManager(linearLayoutManager);
        gv.addItemDecoration(new SimpleItemListDivider(mainView.getContext(), 0));
        gv.setHasFixedSize(true);
        gv.setAdapter(new ArtistAdapter(albumList));
        gv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                currentFabPos = dy;
            }
        });
    }
}