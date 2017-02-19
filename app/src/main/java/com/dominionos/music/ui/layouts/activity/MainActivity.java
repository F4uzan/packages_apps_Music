package com.dominionos.music.ui.layouts.activity;

import android.Manifest;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.dominionos.music.R;
import com.dominionos.music.ui.layouts.fragments.PlaylistFragment;
import com.dominionos.music.utils.MusicPlayerDBHelper;
import com.dominionos.music.utils.MySQLiteHelper;
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

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt;

public class MainActivity extends AppCompatActivity {

    public static final String ACTION_GET_PLAY_STATE = "get_play_state";
    public static final String ACTION_GET_PLAYING_LIST = "get_playing_list";
    public static final String ACTION_GET_PLAYING_DETAIL = "get_playing_detail";
    public static final String ACTION_UPDATE_REPEAT = "update_repeat";
    public static final String ACTION_UPDATE_SHUFFLE = "update_shuffle";

    private boolean musicStopped = true, missingDuration = true;
    private RecyclerView rv;
    private Handler handler;
    private Timer timer;
    private TextView songName, songDesc, currentTime, totalTime;
    private ImageView playToolbar, play, albumArt, miniAlbumArt, repeatButton, shuffleButton;
    private SeekBar seekBar;
    private SlidingUpPanelLayout slidingPanel, secondPanel;
    private Toolbar toolbar;
    private ViewPager viewPager;
    private AudioManager audioManager;
    private FloatingActionButton fab;
    private RelativeLayout miniController, controlHolder;
    private SharedPreferences preferences;
    private int seekProgress;
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch(intent.getAction()) {
                case ACTION_GET_PLAY_STATE:
                    if(intent.getBooleanExtra("isPlaying", false)) {
                        playToolbar.setImageResource(R.drawable.ic_pause);
                        play.setImageResource(R.drawable.ic_pause_circle);
                        musicStopped = false;
                    } else {
                        playToolbar.setImageResource(R.drawable.ic_play);
                        play.setImageResource(R.drawable.ic_play_circle);
                        musicStopped = true;
                    }
                    break;
                case ACTION_GET_PLAYING_DETAIL:
                    changePlayerDetails(intent.getStringExtra("songName"), intent.getStringExtra("songDesc"),
                            intent.getIntExtra("songCurrTime", 0), intent.getIntExtra("songDuration", 0),
                            intent.getLongExtra("songAlbumId", 0)
                    );
                    break;
                case ACTION_GET_PLAYING_LIST:
                    MusicPlayerDBHelper helper = new MusicPlayerDBHelper(context);
                    PlayingSongAdapter adapter = new PlayingSongAdapter(context, helper.getCurrentPlayingList());
                    if(rv.getAdapter() == null) {
                        LinearLayoutManager layoutManager = new LinearLayoutManager(context,
                                LinearLayoutManager.VERTICAL, false);
                        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
                        rv.setLayoutManager(layoutManager);
                        rv.addItemDecoration(new DividerItemDecoration(rv.getContext(), layoutManager.getOrientation()));
                        rv.setAdapter(adapter);
                    } else {
                        rv.swapAdapter(adapter, false);
                    }
                    break;
                case ACTION_UPDATE_REPEAT:
                    updateRepeat(intent.getStringExtra("repeat"));
                    break;
                case ACTION_UPDATE_SHUFFLE:
                    updateShuffle(intent.getBooleanExtra("shuffle", false));
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
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.MEDIA_CONTENT_CONTROL,
                        Manifest.permission.INTERNET}, 1);
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
        filter.addAction(ACTION_GET_PLAY_STATE);
        filter.addAction(ACTION_GET_PLAYING_LIST);
        filter.addAction(ACTION_GET_PLAYING_DETAIL);
        filter.addAction(ACTION_UPDATE_REPEAT);
        filter.addAction(ACTION_UPDATE_SHUFFLE);
        registerReceiver(broadcastReceiver, filter);

        handler = new Handler();

        preferences = getSharedPreferences("com.dominionos.music", MODE_PRIVATE);

        setupViewPager(viewPager);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.main_tab_layout);
        tabLayout.setTabMode(TabLayout.MODE_FIXED);
        tabLayout.setupWithViewPager(viewPager);

        setDrawer();

        setDynamicShortcuts();

        setupPlayer();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(MusicService.ACTION_REQUEST_SONG_DETAILS);
                sendBroadcast(intent);
            }
        }, 1000);
    }

    private void setDynamicShortcuts() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
            ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
            ShortcutInfo searchShortcut = new ShortcutInfo.Builder(this, "search")
                    .setShortLabel(getString(R.string.search))
                    .setIcon(Icon.createWithResource(MainActivity.this, R.drawable.ic_shortcut_search))
                    .setIntent(new Intent(Intent.ACTION_MAIN, Uri.EMPTY, this, SearchActivity.class))
                    .build();
            shortcutManager.setDynamicShortcuts(Collections.singletonList(searchShortcut));
        }
    }

    private void setupViewPager(final ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFrag(new SongsFragment(), getResources().getString(R.string.songs));
        adapter.addFrag(new AlbumsFragment(), getResources().getString(R.string.album));
        adapter.addFrag(new ArtistsFragment(), getResources().getString(R.string.artist));
        adapter.addFrag(new PlaylistFragment(), getResources().getString(R.string.playlist));
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(0);
        viewPager.setOffscreenPageLimit(4);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new MaterialDialog.Builder(MainActivity.this)
                        .title(R.string.add_playlist)
                        .inputType(InputType.TYPE_CLASS_TEXT)
                        .input(getString(R.string.playlist_example), null, new MaterialDialog.InputCallback() {

                            @Override
                            public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                                if(!input.toString().equals("")) {
                                    MySQLiteHelper helper = new MySQLiteHelper(MainActivity.this);
                                    helper.createNewPlayList(input.toString());
                                } else {
                                    Toast.makeText(MainActivity.this, R.string.playlist_name_empty_warning, Toast.LENGTH_SHORT).show();
                                }
                            }
                        })
                        .positiveText(getString(R.string.ok))
                        .negativeText(getString(R.string.cancel)).show();
            }
        });
    }

    private void setDrawer() {
        AccountHeader headerResult = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.color.colorPrimary)
                .build();

        PrimaryDrawerItem songs = new PrimaryDrawerItem().withIdentifier(1).withName(R.string.songs).withIcon(GoogleMaterial.Icon.gmd_audiotrack);
        PrimaryDrawerItem albums = new PrimaryDrawerItem().withIdentifier(2).withName(R.string.albums).withIcon(GoogleMaterial.Icon.gmd_library_music);
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
                switch (position) {
                    case 0:
                        fab.hide();
                        break;
                    case 1:
                        fab.hide();
                        break;
                    case 2:
                        fab.hide();
                        break;
                    case 3:
                        fab.show();
                        if(!preferences.getBoolean("hasSeenCreatePlaylist", false)) {
                            new MaterialTapTargetPrompt.Builder(MainActivity.this)
                                    .setTarget(fab)
                                    .setPrimaryText("Create a Playlist")
                                    .setSecondaryText("Make a new playlist by tapping here!")
                                    .setAnimationInterpolator(new FastOutSlowInInterpolator())
                                    .setBackgroundColour(ContextCompat.getColor(MainActivity.this, R.color.colorPrimary))
                                    .setOnHidePromptListener(new MaterialTapTargetPrompt.OnHidePromptListener() {
                                        @Override
                                        public void onHidePrompt(MotionEvent event, boolean tappedTarget) {
                                            if(tappedTarget) {
                                                preferences.edit().putBoolean("hasSeenCreatePlaylist", true).apply();
                                            }
                                        }

                                        @Override
                                        public void onHidePromptComplete() {}
                                    }).show();
                        }
                        break;
                    default:
                        fab.hide();
                        break;
                }
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
        currentTime = (TextView) findViewById(R.id.player_current_time);
        totalTime = (TextView) findViewById(R.id.player_total_time);
        seekBar = (SeekBar) findViewById(R.id.player_seekbar);
        songName = (TextView) findViewById(R.id.song_name_toolbar);
        songDesc = (TextView) findViewById(R.id.song_desc_toolbar);
        slidingPanel = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        secondPanel = (SlidingUpPanelLayout) findViewById(R.id.playing_list_panel);
        slidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
        playToolbar = (ImageView) findViewById(R.id.player_play_toolbar);
        ImageView rewind = (ImageView) findViewById(R.id.player_rewind);
        ImageView forward = (ImageView) findViewById(R.id.player_forward);
        repeatButton = (ImageView) findViewById(R.id.player_repeat);
        repeatButton.getDrawable().setAlpha(140);
        rv = (RecyclerView) findViewById(R.id.playing_list);
        play = (ImageView) findViewById(R.id.player_play);
        miniController = (RelativeLayout) findViewById(R.id.mini_controller);
        controlHolder = (RelativeLayout) findViewById(R.id.control_holder);
        shuffleButton = (ImageView) findViewById(R.id.player_shuffle);
        shuffleButton.getDrawable().setAlpha(140);
        albumArt = (ImageView) findViewById(R.id.album_art);
        miniAlbumArt = (ImageView) findViewById(R.id.mini_album_art);
        secondPanel.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {}

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
                if(newState.equals(SlidingUpPanelLayout.PanelState.EXPANDED)) {
                    slidingPanel.setEnabled(false);
                    slidingPanel.setClickable(false);
                } else if(newState.equals(SlidingUpPanelLayout.PanelState.COLLAPSED)) {
                    slidingPanel.setEnabled(true);
                    slidingPanel.setClickable(true);
                }
            }
        });
        shuffleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MusicService.ACTION_SHUFFLE_PLAYLIST);
                sendBroadcast(intent);
            }
        });
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser) {
                    Intent changeCurrentTime = new Intent(MusicService.ACTION_SEEK_TO);
                    changeCurrentTime.putExtra("changeSeek", progress);
                    sendBroadcast(changeCurrentTime);
                    musicStopped = false;
                } else {
                    if(seekBar.getProgress() == seekBar.getMax()) {
                        musicStopped = true;
                    }

                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        forward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MusicService.ACTION_NEXT);
                seekBar.setProgress(0);
                sendBroadcast(intent);
            }
        });
        rewind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MusicService.ACTION_PREV);
                seekBar.setProgress(0);
                sendBroadcast(intent);
            }
        });
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MusicService.ACTION_STOP);
                sendBroadcast(intent);
                if(missingDuration) {
                    Intent intent1 = new Intent(MusicService.ACTION_REQUEST_SONG_DETAILS);
                    sendBroadcast(intent1);
                }
            }
        });
        playToolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MusicService.ACTION_STOP);
                sendBroadcast(intent);
            }
        });
        slidingPanel.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {
                miniController.setAlpha(1 - slideOffset);
            }

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
                if(newState == SlidingUpPanelLayout.PanelState.EXPANDED) {
                    miniController.setVisibility(View.GONE);
                } else {
                    miniController.setVisibility(View.VISIBLE);
                }
            }
        });
        repeatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MusicService.ACTION_REPEAT);
                sendBroadcast(intent);
            }
        });
    }

    private void changePlayerDetails(String songNameString, String songDetailsString,
                                     int currentTime, final int totalTime, long albumId) {
        songName.setText(songNameString);
        if(!songName.getText().equals("")) {
            Cursor cursor = getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART},
                    MediaStore.Audio.Albums._ID + "=?",
                    new String[]{String.valueOf(albumId)},
                    null);
            String songArt = null;
            if(cursor != null && cursor.moveToFirst()) songArt = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
            if(cursor != null) cursor.close();
            if(songArt != null) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = false;
                options.inPreferredConfig = Bitmap.Config.RGB_565;
                Bitmap albumArt = BitmapFactory.decodeFile(songArt, options);
                this.albumArt.setImageBitmap(albumArt);
                miniAlbumArt.setImageBitmap(albumArt);
                Palette.PaletteAsyncListener paletteAsyncListener = new Palette.PaletteAsyncListener() {
                    @Override
                    public void onGenerated(Palette palette) {
                        if(palette.getVibrantSwatch() != null) {
                            Drawable background = controlHolder.getBackground();
                            int colorFrom = ((ColorDrawable) background).getColor();
                            int colorTo = palette.getVibrantSwatch().getRgb();
                            ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
                            colorAnimation.setDuration(500); // milliseconds
                            colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                                @Override
                                public void onAnimationUpdate(ValueAnimator animator) {
                                    controlHolder.setBackgroundColor(((int) animator.getAnimatedValue()));
                                }

                            });
                            colorAnimation.start();
                        }
                    }
                };
                Palette.from(albumArt).generate(paletteAsyncListener);
            }
            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int height = size.y;
            int albumArtHeight = albumArt.getHeight();
            secondPanel.setPanelHeight(height - albumArtHeight);
            songDesc.setText(songDetailsString);
            if(currentTime != 0 && totalTime != 0) {
                if (timer != null) timer.cancel();
                missingDuration = false;
                this.currentTime.setText(new SimpleDateFormat("mm:ss", Locale.getDefault()).format(new Date(currentTime)));
                this.totalTime.setText(new SimpleDateFormat("mm:ss", Locale.getDefault()).format(new Date(totalTime)));
                seekBar.setMax(totalTime);
                seekBar.setProgress(currentTime);
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(!musicStopped) {
                                    seekProgress = seekBar.getProgress();
                                    if (seekProgress < totalTime) {
                                        seekBar.setProgress(seekProgress + 100);
                                    } else {
                                        seekBar.setProgress(100);
                                    }
                                    MainActivity.this.currentTime.setText(new SimpleDateFormat("mm:ss", Locale.getDefault())
                                            .format(new Date(seekBar.getProgress())));
                                }
                            }
                        });
                    }
                }, 0, 100);
            } else {
                missingDuration = true;
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(MusicService.ACTION_REQUEST_SONG_DETAILS);
                        sendBroadcast(intent);
                    }
                }, 500);
            }
            if(slidingPanel.getPanelState() == SlidingUpPanelLayout.PanelState.HIDDEN) slidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        } else {
            slidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
        }
    }

    private void updateShuffle(boolean shuffle) {
        if(shuffle) {
            shuffleButton.getDrawable().setAlpha(255);
        } else {
            shuffleButton.getDrawable().setAlpha(140);
        }
    }

    private void updateRepeat(String repeatMode) {
        switch (repeatMode) {
            case "all":
                repeatButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_repeat_all));
                repeatButton.getDrawable().setAlpha(255);
                break;
            case "one":
                repeatButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_repeat_one));
                repeatButton.getDrawable().setAlpha(255);
                break;
            case "none":
                repeatButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_repeat_all));
                repeatButton.getDrawable().setAlpha(140);
                break;
            default:
                repeatButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_repeat_all));
                repeatButton.getDrawable().setAlpha(140);
                break;
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
    public void onBackPressed() {
        if(secondPanel.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
            secondPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        } else if(slidingPanel.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
            slidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        } else {
            super.onBackPressed();
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

}
