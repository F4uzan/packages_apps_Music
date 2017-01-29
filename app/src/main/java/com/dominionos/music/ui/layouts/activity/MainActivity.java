package com.dominionos.music.ui.layouts.activity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.dominionos.music.R;
import com.dominionos.music.ui.layouts.fragments.PlaylistFragment;
import com.dominionos.music.utils.MusicPlayerDBHelper;
import com.dominionos.music.utils.adapters.PlayingSongAdapter;
import com.dominionos.music.utils.adapters.ViewPagerAdapter;
import com.dominionos.music.service.MusicService;
import com.dominionos.music.ui.layouts.fragments.AlbumsFragment;
import com.dominionos.music.ui.layouts.fragments.ArtistsFragment;
import com.dominionos.music.ui.layouts.fragments.SongsFragment;
import com.mikepenz.aboutlibraries.Libs;
import com.mikepenz.aboutlibraries.LibsBuilder;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

public class MainActivity extends AppCompatActivity {

    public static final String ACTION_GET_PLAY_STATE = "get_play_state";
    public static final String ACTION_GET_SEEK_VALUE = "get_seek_value";
    public static final String ACTION_GET_PLAYING_LIST = "get_playing_list";
    public static final String ACTION_GET_PLAYING_DETAIL = "get_playing_detail";

    private TextView songName, songDesc;
    private ImageView playToolbar, play, forward, rewind;

    private SlidingUpPanelLayout slidingPanel;
    private Toolbar toolbar;
    private ViewPager viewPager;
    private AudioManager audioManager;
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch(intent.getAction()) {
                case ACTION_GET_PLAY_STATE:
                    if(intent.getBooleanExtra("isPlaying", false)) {
                        playToolbar.setImageResource(R.drawable.ic_pause);
                        play.setImageResource(R.drawable.ic_pause);
                    } else {
                        playToolbar.setImageResource(R.drawable.ic_play);
                        play.setImageResource(R.drawable.ic_play);
                    }
                    break;
                case ACTION_GET_SEEK_VALUE:
                    break;
                case ACTION_GET_PLAYING_DETAIL:
                    changePlayerDetails(intent.getStringExtra("songName"), intent.getStringExtra("songDesc"));
                    break;
                case ACTION_GET_PLAYING_LIST:
                    RecyclerView rv = (RecyclerView) findViewById(R.id.playing_list);
                    MusicPlayerDBHelper helper = new MusicPlayerDBHelper(context);
                    PlayingSongAdapter adapter = new PlayingSongAdapter(context, helper.getCurrentPlayingList());
                    LinearLayoutManager layoutManager = new LinearLayoutManager(context,
                            LinearLayoutManager.VERTICAL, false);
                    layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
                    rv.setLayoutManager(layoutManager);
                    rv.setHasFixedSize(true);
                    rv.setAdapter(adapter);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_Main);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        overridePendingTransition(0, 0);
        toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        viewPager = (ViewPager) findViewById(R.id.main_viewpager);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                init();
            } else {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        } else {
            init();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item) {
        int id = item.getItemId();
        switch(id) {
            case R.id.play_all:
                Intent i = new Intent();
                i.setAction(MusicService.ACTION_PLAY_ALL_SONGS);
                sendBroadcast(i);
                break;
            case R.id.search_item:
                Intent intent = new Intent(MainActivity.this, SearchActivity.class);
                startActivity(intent);
                break;
        }
        return true;
    }

    private void init() {

        Intent i = new Intent(MainActivity.this, MusicService.class);
        startService(i);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_GET_SEEK_VALUE);
        filter.addAction(ACTION_GET_PLAY_STATE);
        filter.addAction(ACTION_GET_PLAYING_LIST);
        filter.addAction(ACTION_GET_PLAYING_DETAIL);
        registerReceiver(broadcastReceiver, filter);

        setupViewPager(viewPager);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.main_tab_layout);
        tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
        tabLayout.setupWithViewPager(viewPager);

        setDrawer();

        setupPlayer();
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFrag(new SongsFragment(), getResources().getString(R.string.songs));
        adapter.addFrag(new AlbumsFragment(), getResources().getString(R.string.album));
        adapter.addFrag(new ArtistsFragment(), getResources().getString(R.string.artist));
        adapter.addFrag(new PlaylistFragment(), getResources().getString(R.string.playlist));
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(0);
    }

    private void setDrawer() {
        AccountHeader headerResult = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.color.colorPrimary)
                .build();

        PrimaryDrawerItem songs = new PrimaryDrawerItem().withIdentifier(1).withName(R.string.songs).withIcon(GoogleMaterial.Icon.gmd_audiotrack);
        PrimaryDrawerItem albums = new PrimaryDrawerItem().withIdentifier(2).withName(R.string.album).withIcon(GoogleMaterial.Icon.gmd_library_music);
        PrimaryDrawerItem artists = new PrimaryDrawerItem().withIdentifier(3).withName(R.string.artist).withIcon(GoogleMaterial.Icon.gmd_account_circle);
        PrimaryDrawerItem playlist = new PrimaryDrawerItem().withIdentifier(4).withName(R.string.playlist).withIcon(GoogleMaterial.Icon.gmd_queue_music);
        SecondaryDrawerItem about = new SecondaryDrawerItem().withIdentifier(5).withName(R.string.about).withSelectable(false).withIcon(GoogleMaterial.Icon.gmd_info_outline);

        final Drawer drawer = new DrawerBuilder()
                .withActivity(this)
                .withAccountHeader(headerResult)
                .withToolbar(toolbar)
                .withCloseOnClick(true)
                .addDrawerItems(
                        songs,
                        albums,
                        artists,
                        playlist,
                        new DividerDrawerItem(),
                        about
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {

                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        int drawerIdentifier = (int) drawerItem.getIdentifier();
                        switch(drawerIdentifier) {
                            case 1:
                                viewPager.setCurrentItem(0);
                                break;
                            case 2:
                                viewPager.setCurrentItem(1);
                                break;
                            case 3:
                                viewPager.setCurrentItem(2);
                                break;
                            case 4:
                                viewPager.setCurrentItem(3);
                                break;
                            case 5:
                                new LibsBuilder()
                                        .withActivityStyle(Libs.ActivityStyle.LIGHT_DARK_TOOLBAR)
                                        .withActivityTheme(R.style.AppTheme_Main)
                                        .withActivityTitle(getString(R.string.about))
                                        .withAboutIconShown(true)
                                        .withAboutVersionShown(true)
                                        .withAboutDescription(getString(R.string.about_text))
                                        .start(MainActivity.this);
                                break;
                        }
                        return true;
                    }
                })
                .build();
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                drawer.setSelectionAtPosition(position + 1);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    private void setupPlayer() {
        songName = (TextView) findViewById(R.id.song_name_toolbar);
        songDesc = (TextView) findViewById(R.id.song_desc_toolbar);
        slidingPanel = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        playToolbar = (ImageView) findViewById(R.id.player_play_toolbar);
        rewind = (ImageView) findViewById(R.id.player_rewind);
        forward = (ImageView) findViewById(R.id.player_forward);
        play = (ImageView) findViewById(R.id.player_play);
        forward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MusicService.ACTION_NEXT);
                sendBroadcast(intent);
            }
        });
        rewind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MusicService.ACTION_PREV);
                sendBroadcast(intent);
            }
        });
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MusicService.ACTION_STOP);
                sendBroadcast(intent);
            }
        });
        playToolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MusicService.ACTION_STOP);
                sendBroadcast(intent);
            }
        });
        Intent intent = new Intent(MusicService.ACTION_REQUEST_SONG_DETAILS);
        sendBroadcast(intent);
    }

    private void changePlayerDetails(String songNameString, String songDetailsString) {
        if(songNameString != null) {
            slidingPanel.setEnabled(true);
            songName.setText(songNameString);
            songDesc.setText(songDetailsString);
        } else {
            slidingPanel.setEnabled(false);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch(requestCode) {
            case 1:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    init();
                } else {
                    finish();
                }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
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

}
