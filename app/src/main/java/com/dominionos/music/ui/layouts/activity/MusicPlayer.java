package com.dominionos.music.ui.layouts.activity;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.transition.Fade;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.dominionos.music.R;
import com.dominionos.music.utils.MusicPlayerDBHelper;
import com.dominionos.music.utils.adapters.PlayingSongAdapter;
import com.dominionos.music.service.MusicService;
import com.dominionos.music.task.ChangeSeekDetailUpdater;
import com.dominionos.music.task.ColorAnimateAlbumView;

import java.util.Timer;
import java.util.TimerTask;

import app.minimize.com.seek_bar_compat.SeekBarCompat;

public class MusicPlayer extends AppCompatActivity {

    public static final String ACTION_GET_PLAY_STATE = "get_play_state";
    public static final String ACTION_GET_SEEK_VALUE = "gte_seek_value";
    public static final String ACTION_GET_PLAYING_LIST = "get_playing_list";
    public static final String ACTION_GET_PLAYING_DETAIL = "get_playing_detail";
    private static int mainColor, mainColorAlt;
    private String songName, songArt;
    private TextView currentTimeHolder, totalTimeHolder;
    private long albumId;
    private Timer timer;
    private LinearLayout detailHolder, controlHolder;
    private CollapsingToolbarLayout collapsingToolbarLayout;
    private ImageView playButton, rewindButton,
            nextButton, shuffleButton, repeatButton, header;
    private Drawable upButton;
    private SeekBarCompat seekBar;
    private int duration, currentDuration;
    private boolean musicStopped;
    private AudioManager audioManager;

    private final BroadcastReceiver musicPlayer = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_GET_SEEK_VALUE)) {
                seekBar.setProgress(intent.getIntExtra("songSeekVal", 0));
                currentDuration = intent.getIntExtra("songSeekVal", 0);
                if (intent.getBooleanExtra("isPlaying", false)) {
                    playButton.setImageResource(R.drawable.ic_pause);
                    musicStopped = false;
                } else {
                    playButton.setImageResource(R.drawable.ic_play);
                    musicStopped = true;
                }
            } else if (intent.getAction().equals(ACTION_GET_PLAY_STATE)) {
                if (intent.getBooleanExtra("isPlaying", false)) {
                    playButton.setImageResource(R.drawable.ic_pause);
                    musicStopped = false;
                } else {
                    playButton.setImageResource(R.drawable.ic_play);
                    musicStopped = true;
                }
            } else if (intent.getAction().equals(ACTION_GET_PLAYING_LIST)) {
                RecyclerView rv = (RecyclerView) findViewById(R.id.player_playlist);
                MusicPlayerDBHelper helper = new MusicPlayerDBHelper(context);
                PlayingSongAdapter adapter = new PlayingSongAdapter(context, helper.getCurrentPlayingList());
                LinearLayoutManager layoutManager = new LinearLayoutManager(context,
                        LinearLayoutManager.VERTICAL, false);
                layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
                rv.setLayoutManager(layoutManager);
                rv.setHasFixedSize(true);
                rv.setAdapter(adapter);
            } else if (intent.getAction().equals(ACTION_GET_PLAYING_DETAIL)) {
                songName = intent.getStringExtra("songName");
                albumId = intent.getLongExtra("songAlbumId", 0);
                duration = intent.getIntExtra("songDuration", 0);
                currentDuration = 0;
                musicStopped = false;
                updateSeeker();
                updateView();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_player);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_GET_SEEK_VALUE);
        filter.addAction(ACTION_GET_PLAY_STATE);
        filter.addAction(ACTION_GET_PLAYING_LIST);
        filter.addAction(ACTION_GET_PLAYING_DETAIL);
        registerReceiver(musicPlayer, filter);

        getWindow().setEnterTransition(new Fade());

        songName = getIntent().getStringExtra("songName");
        albumId = getIntent().getLongExtra("songAlbumId", 0);
        duration = getIntent().getIntExtra("songDuration", 0);
        currentDuration = getIntent().getIntExtra("songCurrTime", 0);

        init();

        setButtons();

        updateSeeker();

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar_player));
        updateView();

    }

    private void updateView() {
        Cursor cursor = getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART},
                MediaStore.Audio.Albums._ID + "=?",
                new String[]{String.valueOf(albumId)},
                null);
        if (cursor != null && cursor.moveToFirst()) {
            songArt = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
        }
        if (cursor != null) {
            cursor.close();
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = false;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        Drawable artWork = new BitmapDrawable(this.getResources(), BitmapFactory.decodeFile(songArt, options));
        upButton = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_arrow_back, null);
        ((ImageView) findViewById(R.id.header)).setImageDrawable(artWork);
        if(getSupportActionBar() != null) {
            collapsingToolbarLayout.setTitle(songName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(upButton);
        }

        if (songArt != null) {
            Palette.PaletteAsyncListener paletteListener = new Palette.PaletteAsyncListener() {
                public void onGenerated(Palette palette) {
                    int defaultColor = 0x000000;
                    mainColor = palette.getVibrantColor(defaultColor);
                    mainColorAlt = palette.getDominantColor(defaultColor);
                    if (mainColor != 0) {
                        ActivityManager.TaskDescription taskDescription = new ActivityManager.TaskDescription(getResources().getString(R.string.app_name),
                                BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher),
                                palette.getVibrantColor(defaultColor));
                        setTaskDescription(taskDescription);
                    } else if (mainColorAlt != 0) {
                        ActivityManager.TaskDescription taskDescription = new ActivityManager.TaskDescription(getResources().getString(R.string.app_name),
                                BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher),
                                palette.getDominantColor(defaultColor));
                        setTaskDescription(taskDescription);
                    }
                    new ColorAnimateAlbumView(detailHolder, palette).execute();
                    new ColorAnimateAlbumView(controlHolder, palette).execute();
                    if(mainColor != 0) {
                        if (palette.getVibrantSwatch() != null) {
                            int bodyColor = palette.getVibrantSwatch().getBodyTextColor();
                            collapsingToolbarLayout.setContentScrimColor(mainColor);
                            collapsingToolbarLayout.setStatusBarScrimColor(getAutoStatColor(mainColor));
                            collapsingToolbarLayout.setCollapsedTitleTextColor(bodyColor);
                            upButton.setTintList(ColorStateList.valueOf(bodyColor));
                            seekBar.setProgressBackgroundColor(getAutoStatColor(mainColor));
                        }
                    } else if (mainColorAlt != 0) {
                        if (palette.getDominantSwatch() != null) {
                            int titleColor = palette.getDominantSwatch().getTitleTextColor();
                            int bodyColor = palette.getDominantSwatch().getBodyTextColor();
                            collapsingToolbarLayout.setContentScrimColor(mainColorAlt);
                            collapsingToolbarLayout.setStatusBarScrimColor(getAutoStatColor(mainColorAlt));
                            collapsingToolbarLayout.setCollapsedTitleTextColor(bodyColor);
                            upButton.setTintList(ColorStateList.valueOf(bodyColor));
                            seekBar.setProgressBackgroundColor(titleColor);
                            playButton.setImageTintList(ColorStateList.valueOf(bodyColor));
                            nextButton.setImageTintList(ColorStateList.valueOf(bodyColor));
                            rewindButton.setImageTintList(ColorStateList.valueOf(bodyColor));
                            shuffleButton.setImageTintList(ColorStateList.valueOf(bodyColor));
                            repeatButton.setImageTintList(ColorStateList.valueOf(bodyColor));
                            currentTimeHolder.setTextColor(titleColor);
                            totalTimeHolder.setTextColor(titleColor);
                        }
                    } else {
                        mainColor = ResourcesCompat.getColor(getResources(), R.color.colorPrimary, null);
                        collapsingToolbarLayout.setContentScrimColor(mainColor);
                        collapsingToolbarLayout.setStatusBarScrimColor(getAutoStatColor(mainColor));
                    }
                    }
            };
            try {
                Palette.from(BitmapFactory.decodeFile(songArt, options)).generate(paletteListener);
            } catch (IllegalArgumentException ignored) {
                header.setImageResource(R.drawable.default_artwork_dark);
                mainColor = ResourcesCompat.getColor(getResources(), R.color.colorPrimary, null);
                collapsingToolbarLayout.setContentScrimColor(mainColor);
                collapsingToolbarLayout.setStatusBarScrimColor(getAutoStatColor(mainColor));
            }
        } else {
            header.setImageResource(R.drawable.default_artwork_dark);
            mainColor = ResourcesCompat.getColor(getResources(), R.color.colorPrimary, null);
            collapsingToolbarLayout.setContentScrimColor(mainColor);
            collapsingToolbarLayout.setStatusBarScrimColor(getAutoStatColor(mainColor));
        }
    }

    private void updateSeeker() {
        seekBar.setMax(duration);
        seekBar.setProgress(currentDuration);
        String totalDurationSeconds = String.valueOf((duration / 1000) % 60);
        String totalDurationMinutes = String.valueOf((duration / 1000) / 60);
        if(totalDurationMinutes.length() == 1 &&  totalDurationSeconds.length() == 1) {
            totalTimeHolder.setText("0" + totalDurationMinutes + ":" + "0" + totalDurationSeconds);
        } else if(totalDurationSeconds.length() == 1) {
            totalTimeHolder.setText(totalDurationMinutes + ":" + "0" + totalDurationSeconds);
        } else if(totalDurationMinutes.length() == 1) {
            totalTimeHolder.setText("0" + totalDurationMinutes + ":" + totalDurationSeconds);
        } else {
            totalTimeHolder.setText(totalDurationMinutes + ":" + totalDurationSeconds);
        }
        musicStopped = false;
        if (timer != null)
            timer.cancel();
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (!musicStopped) {
                            int seekProgress = seekBar.getProgress();
                            if (seekProgress < duration)
                                seekBar.setProgress(seekProgress + 100);
                            else
                                seekBar.setProgress(100);
                            new ChangeSeekDetailUpdater(seekProgress, currentTimeHolder).execute();
                        }
                    }
                });

            }
        }, 0, 100);
    }

    private void setButtons() {

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent stopMusic = new Intent();
                stopMusic.setAction(MusicService.ACTION_STOP);
                sendBroadcast(stopMusic);
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent nextMusic = new Intent();
                nextMusic.setAction(MusicService.ACTION_NEXT);
                sendBroadcast(nextMusic);
            }
        });

        rewindButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent prevMusic = new Intent();
                prevMusic.setAction(MusicService.ACTION_PREV);
                sendBroadcast(prevMusic);
            }
        });

        shuffleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent shuffleMusic = new Intent();
                shuffleMusic.setAction(MusicService.ACTION_SHUFFLE_PLAYLIST);
                sendBroadcast(shuffleMusic);
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    Intent changeCurrentTime = new Intent();
                    changeCurrentTime.setAction(MusicService.ACTION_SEEK_TO);
                    changeCurrentTime.putExtra("changeSeek", progress);
                    sendBroadcast(changeCurrentTime);
                    musicStopped = false;
                } else {
                    if (seekBar.getProgress() == duration) {
                        seekBar.setProgress(100);
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

    }

    private void init() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        header = (ImageView) findViewById(R.id.header);
        playButton = (ImageView) findViewById(R.id.player_play);
        rewindButton = (ImageView) findViewById(R.id.player_rewind);
        nextButton = (ImageView) findViewById(R.id.player_forward);
        shuffleButton = (ImageView) findViewById(R.id.player_shuffle);
        repeatButton = (ImageView) findViewById(R.id.player_repeat);
        seekBar = (SeekBarCompat) findViewById(R.id.player_seekbar);
        currentTimeHolder = (TextView) findViewById(R.id.player_current_time);
        totalTimeHolder = (TextView) findViewById(R.id.player_total_time);
        detailHolder = (LinearLayout) findViewById(R.id.detail_holder);
        controlHolder = (LinearLayout) findViewById(R.id.control_holder);
        collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapsingtoolbarlayout_player);
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(musicPlayer);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        Intent i = new Intent();
        i.setAction(MusicService.ACTION_SEEK_GET);
        sendBroadcast(i);
    }

    private int getAutoStatColor(int baseColor) {
        float[] hsv = new float[3];
        Color.colorToHSV(baseColor, hsv);
        hsv[2] *= 0.8f;
        return Color.HSVToColor(hsv);
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
