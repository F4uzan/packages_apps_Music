package com.dominionos.music.ui.layouts.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.dominionos.music.R;
import com.dominionos.music.utils.MySQLiteHelper;
import com.dominionos.music.utils.SpacesItemDecoration;
import com.dominionos.music.utils.adapters.PlaylistAdapter;
import com.dominionos.music.utils.items.Playlist;

import java.util.List;

public class PlaylistFragment extends Fragment {

    private View mainView;
    private SharedPreferences settingsPref;
    private RecyclerView gv;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.albums_fragment, container, false);
        this.mainView = v;
        init();
        Handler mainHandler = new Handler(mainView.getContext().getMainLooper());

        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                getPlaylistList();
            }
        };
        mainHandler.post(myRunnable);
        return v;
    }

    private void getPlaylistList() {
        MySQLiteHelper helper = new MySQLiteHelper(mainView.getContext());
        List<Playlist> playlistList = helper.getAllPlaylist();
        playlistList.add(new Playlist(-1, "Create new"));
        GridLayoutManager gridLayoutManager = new GridLayoutManager(mainView.getContext(),
                settingsPref.getInt("pref_grid_num", 2));
        gridLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        gridLayoutManager.scrollToPosition(0);
        gv.setLayoutManager(gridLayoutManager);
        gv.addItemDecoration(new SpacesItemDecoration(8, settingsPref.getInt("pref_grid_num", 2)));
        gv.setHasFixedSize(true);
        gv.setAdapter(new PlaylistAdapter(mainView.getContext(), playlistList));
    }

    private void init() {
        settingsPref = PreferenceManager.getDefaultSharedPreferences(mainView.getContext());
        gv = (RecyclerView) mainView.findViewById(R.id.album_grid);
    }

}
