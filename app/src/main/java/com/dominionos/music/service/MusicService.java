package com.dominionos.music.service;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.dominionos.music.R;
import com.dominionos.music.task.ChangeNotificationDetails;
import com.dominionos.music.ui.layouts.activity.MusicPlayer;
import com.dominionos.music.utils.MusicPlayerDBHelper;
import com.dominionos.music.utils.MySQLiteHelper;
import com.dominionos.music.utils.items.SongListItem;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MusicService extends Service {

    private MediaPlayer mediaPlayer;

    private Notification notificationCompat;
    private NotificationManager notificationManager;
    private String songName, songDesc, songPath, albumName;
    private long albumId;
    private boolean singleSong;
    private int currentPlaylistSongId = -1, pausedSongPlaylistId = -1, pausedSongSeek;
    private SongListItem pausedSong;
    private MusicPlayerDBHelper playList;
    private AudioManager audioManager;
    private MediaSession mediaSession;
    private ArrayList<SongListItem> songList;

    private AudioManager.OnAudioFocusChangeListener afChangeListener =
            new AudioManager.OnAudioFocusChangeListener() {
                public void onAudioFocusChange(int focusChange) {
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                        if(mediaPlayer.isPlaying()) {
                            mediaPlayer.pause();
                        }
                    } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                        if(!mediaPlayer.isPlaying()) {
                            mediaPlayer.start();
                        }
                    } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                        audioManager.abandonAudioFocus(afChangeListener);
                        stopMusic();
                    }
                }
            };

    public static final int NOTIFICATION_ID = 104;
    public static final String ACTION_PLAY = "play";
    public static final String ACTION_PREV = "prev";
    public static final String ACTION_NEXT = "next";
    public static final String ACTION_STOP = "stop";
    public static final String ACTION_PLAY_ALBUM = "player_play_album";
    public static final String ACTION_PLAY_ALL_SONGS = "play_all_songs";
    public static final String ACTION_MENU_FROM_PLAYLIST = "player_menu_from_playlist";
    public static final String ACTION_PLAY_FROM_PLAYLIST = "player_play_from_playlist";
    public static final String ACTION_PLAY_PLAYLIST = "player_play_playlist";
    public static final String ACTION_PLAY_NEXT = "player_play_next";
    public static final String ACTION_REMOVE_SERVICE = "player_remove_service";
    public static final String ACTION_PLAY_SINGLE = "play_single_song";
    public static final String ACTION_ADD_SONG = "add_song_to_playlist";
    public static final String ACTION_ADD_SONG_MULTI = "add_song_to_playlist_multi";
    public static final String ACTION_REQUEST_SONG_DETAILS = "player_request_song_details";
    public static final String ACTION_SEEK_TO = "player_seek_to_song";
    public static final String ACTION_SEEK_GET = "player_seek_get_song";
    public static final String ACTION_SHUFFLE_PLAYLIST = "player_shuffle_playlist";

    public static final String ACTION_MENU_PLAY_NEXT = "menu_play_next";
    public static final String ACTION_MENU_REMOVE_FROM_QUEUE = "menu_from_queue";
    public static final String ACTION_MENU_SHARE = "menu_share";
    public static final String ACTION_MENU_DELETE = "menu_delete";

    private final BroadcastReceiver musicPlayer = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            handleBroadcastReceived(context, intent);
        }

    };

    private void handleBroadcastReceived(Context context, Intent intent) {
        switch(intent.getAction()) {
            case ACTION_PLAY_SINGLE:
                playList.clearPlayingList();
                pausedSongSeek = 0;
                playMusic((int) intent.getLongExtra("songId", -1), intent.getStringExtra("songPath"), intent.getStringExtra("songName"),
                        intent.getStringExtra("songDesc"), intent.getStringExtra("songArt"),
                        intent.getLongExtra("songAlbumId", 0), intent.getStringExtra("songAlbumName"), true);
                playList.addSong(new SongListItem(intent.getLongExtra("songId", 0), intent.getStringExtra("songName"), intent.getStringExtra("songDesc"),
                        intent.getStringExtra("songPath"), false,
                        intent.getLongExtra("songAlbumId", 0), intent.getStringExtra("songAlbumName"), 0));
                currentPlaylistSongId = 0;
                pausedSongSeek = 0;
                break;
            case ACTION_PLAY_ALBUM:
                pausedSongSeek = 0;
                playList.clearPlayingList();
                Cursor musicCursor;
                String where = MediaStore.Audio.Media.ALBUM_ID + "=?";
                String whereVal[] = {intent.getLongExtra("albumId", 0) + ""};
                String orderBy = MediaStore.Audio.Media._ID;

                musicCursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        null, where, whereVal, orderBy);
                if (musicCursor != null && musicCursor.moveToFirst()) {
                    //get columns
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
                    int albumNameColumn = musicCursor.getColumnIndex
                            (MediaStore.Audio.Media.ALBUM);
                    int count = 0;
                    do {
                        count++;
                        addSong(new SongListItem(musicCursor.getLong(idColumn),
                                musicCursor.getString(titleColumn),
                                musicCursor.getString(artistColumn),
                                musicCursor.getString(pathColumn), false,
                                musicCursor.getLong(albumIdColumn),
                                musicCursor.getString(albumNameColumn),
                                count));
                    }
                    while (musicCursor.moveToNext());
                }
                if (musicCursor != null) {
                    musicCursor.close();
                }
                playMusic(0);
                break;
            case ACTION_REMOVE_SERVICE:
                MusicService.this.stopSelf();
                ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                if (am != null) {
                    List<ActivityManager.AppTask> tasks = am.getAppTasks();
                    if (tasks != null) {
                        try {
                            tasks.get(0).finishAndRemoveTask();
                        } catch (RuntimeException e) {
                            Log.e("MusicService", "Failed to remove tasks");
                        }
                    }
                }
                    unregisterReceiver(musicPlayer);
                break;
            case ACTION_NEXT:
                pausedSongSeek = 0;
                playMusic(playList.getNextSong(currentPlaylistSongId));
                updateCurrentPlaying();
                break;
            case ACTION_PREV:
                if (mediaPlayer.getCurrentPosition() >= 5000) {
                    mediaPlayer.seekTo(0);
                } else {
                    pausedSongSeek = 0;
                    playMusic(playList.getPrevSong(currentPlaylistSongId));
                }
                updateCurrentPlaying();
                break;
            case ACTION_REQUEST_SONG_DETAILS:
                if (songPath == null) {
                    MusicPlayerDBHelper helper = new MusicPlayerDBHelper(context);
                    ArrayList<SongListItem> playback = helper.getCurrentPlayingList();
                    if (playback.size() != 0) {
                        songPath = playback.get(0).getPath();
                        songName = playback.get(0).getName();
                        songDesc = playback.get(0).getDesc();
                        albumId = playback.get(0).getAlbumId();
                        albumName = playback.get(0).getAlbumName();
                        Intent sendDetails = new Intent(MusicService.this, MusicPlayer.class);
                        sendDetails.putExtra("songPath", songPath);
                        sendDetails.putExtra("songName", songName);
                        sendDetails.putExtra("songDesc", songDesc);
                        sendDetails.putExtra("songAlbumId", albumId);
                        sendDetails.putExtra("songAlbumName", albumName);
                        try {
                            sendDetails.putExtra("songDuration", mediaPlayer.getDuration());
                            sendDetails.putExtra("songCurrTime", mediaPlayer.getCurrentPosition());
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                        sendDetails.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(sendDetails);
                    } else
                        Toast.makeText(MusicService.this, "Nothing to Play", Toast.LENGTH_SHORT).show();
                } else {
                    Intent sendDetails = new Intent(MusicService.this, MusicPlayer.class);
                    sendDetails.putExtra("songPath", songPath);
                    sendDetails.putExtra("songName", songName);
                    sendDetails.putExtra("songDesc", songDesc);
                    sendDetails.putExtra("songAlbumId", albumId);
                    sendDetails.putExtra("songAlbumName", albumName);
                    try {
                        sendDetails.putExtra("songDuration", mediaPlayer.getDuration());
                        sendDetails.putExtra("songCurrTime", mediaPlayer.getCurrentPosition());
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }
                    sendDetails.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(sendDetails);
                }
                break;
            case ACTION_STOP:
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    if (!singleSong) {
                        pausedSong = playList.getSong(currentPlaylistSongId);
                        pausedSongPlaylistId = (int) pausedSong.getId();
                    }
                    pausedSongSeek = mediaPlayer.getCurrentPosition();
                    stopMusic();
                    stopForeground(true);
                    changeNotificationDetails(pausedSong.getPath(), pausedSong.getName(),
                            pausedSong.getDesc(), pausedSong.getAlbumId(), pausedSong.getAlbumName());
                } else {
                    if (!singleSong) {
                        playMusic(pausedSongPlaylistId);
                    } else {
                        playMusic(pausedSong);
                    }
                }
                Intent i = new Intent(MusicPlayer.ACTION_GET_PLAY_STATE);
                if (mediaPlayer != null && mediaPlayer.isPlaying())
                    i.putExtra("isPlaying", true);
                sendBroadcast(i);
                break;
            case ACTION_SEEK_TO:
                try {
                    mediaPlayer.seekTo(intent.getIntExtra("changeSeek", 0));
                } catch (NullPointerException e) {
                    e.printStackTrace();
                    pausedSongSeek = intent.getIntExtra("changeSeek", 0);
                    playMusic(pausedSong);
                    pausedSongSeek = 0;
                }
                break;
            case ACTION_SEEK_GET:
                Intent i1 = new Intent();
                i1.setAction(MusicPlayer.ACTION_GET_SEEK_VALUE);
                if (mediaPlayer != null && mediaPlayer.isPlaying())
                    i1.putExtra("isPlaying", true);
                if (mediaPlayer != null)
                    i1.putExtra("songSeekVal", mediaPlayer.getCurrentPosition());
                sendBroadcast(i1);
                updatePlaylist();
                break;
            case ACTION_SHUFFLE_PLAYLIST:
                String currentPlayingId = playList.getSong(currentPlaylistSongId).getName();
                playList.shuffleRows();
                updatePlaylist();
                ArrayList<SongListItem> songsList = playList.getCurrentPlayingList();
                for (int num = 0; num < playList.getPlaybackTableSize(); num++) {
                    if (currentPlayingId.matches(songsList.get(num).getName())) {
                        currentPlaylistSongId = (int) songsList.get(num).getId();
                        break;
                    }
                }
                break;
            case ACTION_PLAY_NEXT:
                if (currentPlaylistSongId == playList.getLastSong().getId() && currentPlaylistSongId != -1) {
                    playList.addSong(new SongListItem(intent.getIntExtra("songId", 0), intent.getStringExtra("songName"), intent.getStringExtra("songDesc"),
                            intent.getStringExtra("songPath"), false,
                            intent.getLongExtra("songAlbumId", 0), intent.getStringExtra("songAlbumName"), 0));
                } else {
                    intent.setAction(ACTION_PLAY_SINGLE);
                    sendBroadcast(intent);
                }
                break;
            case ACTION_ADD_SONG:
                if (playList.getPlaybackTableSize() != 0 && currentPlaylistSongId != -1) {
                    playList.addSong(new SongListItem(intent.getLongExtra("songId", 0), intent.getStringExtra("songName"), intent.getStringExtra("songDesc"),
                            intent.getStringExtra("songPath"), false,
                            intent.getLongExtra("songAlbumId", 0), intent.getStringExtra("songAlbumName"), 0));
                } else {
                    intent.setAction(ACTION_PLAY_SINGLE);
                    sendBroadcast(intent);
                }
                break;
            case ACTION_PLAY_FROM_PLAYLIST:
                pausedSongSeek = 0;
                playMusic(Integer.parseInt(intent.getStringExtra("playListId")));
                updateCurrentPlaying();
                break;
            case ACTION_MENU_FROM_PLAYLIST:
                String action = intent.getStringExtra("action");
                if (action.matches(ACTION_MENU_PLAY_NEXT)) {
                    SongListItem item = playList.getSong(intent.getIntExtra("count", -1));
                    playList.addSong(item);
                    updatePlaylist();
                } else if (action.matches(ACTION_MENU_REMOVE_FROM_QUEUE)) {
                    playList.removeSong(intent.getIntExtra("count", -1));
                    updatePlaylist();
                } else if (action.matches(ACTION_MENU_SHARE)) {
                    Intent share = new Intent(Intent.ACTION_SEND);
                    share.setType("audio/*");
                    share.putExtra(Intent.EXTRA_STREAM, Uri.parse("file:///" +
                            playList.getSong(intent.getIntExtra("count", -1)).getPath()));
                    share.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(share);
                } else if (action.matches(ACTION_MENU_DELETE)) {
                    int pos = intent.getIntExtra("count", -1);
                    SongListItem song = playList.getSong(pos);
                    File file = new File(song.getPath());
                    boolean deleted = file.delete();
                    if (deleted) {
                        Toast.makeText(context, getString(R.string.song_delete_success), Toast.LENGTH_SHORT).show();
                        context.getContentResolver().delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                MediaStore.MediaColumns._ID + "='" + song.getId() + "'", null);
                        playList.removeSong(pos);
                        updatePlaylist();
                    } else
                        Toast.makeText(context, getString(R.string.song_delete_fail), Toast.LENGTH_SHORT).show();
                }
                break;
            case ACTION_PLAY_PLAYLIST:
                MySQLiteHelper helper = new MySQLiteHelper(context);
                playList.clearPlayingList();
                playList.addSongs(helper.getPlayListSongs(intent.getIntExtra("playlistId", -1)));
                playMusic(playList.getFirstSong());
                break;
            case ACTION_PLAY_ALL_SONGS:
                if(songList != null) {
                    playList.clearPlayingList();
                    playList.addSongs(songList);
                    pausedSongSeek = 0;
                    playMusic(0);
                } else {
                    Toast.makeText(context, getString(R.string.service_generate_list_warning), Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    private void updateCurrentPlaying() {
        Intent sendDetails = new Intent();
        sendDetails.putExtra("songPath", songPath);
        sendDetails.putExtra("songName", songName);
        sendDetails.putExtra("songDesc", songDesc);
        sendDetails.putExtra("songAlbumId", albumId);
        sendDetails.putExtra("songAlbumName", albumName);
        sendDetails.putExtra("songDuration", mediaPlayer.getDuration());
        sendDetails.putExtra("songCurrTime", mediaPlayer.getCurrentPosition());
        sendDetails.setAction(MusicPlayer.ACTION_GET_PLAYING_DETAIL);
        sendBroadcast(sendDetails);
    }

    private void updatePlaylist() {
        Intent playlistIntent = new Intent();
        playlistIntent.setAction(MusicPlayer.ACTION_GET_PLAYING_LIST);
        sendBroadcast(playlistIntent);
    }

    private void addSong(SongListItem song) {
        playList.addSong(song);
        pausedSongSeek = 0;
    }

    private void stopMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            setNotificationPlayer(true);
        }
    }

    private void playMusic(int playingPos) {
        if (playList.getPlaybackTableSize() > 0) {
            try {
                SongListItem song = playList.getSong(playingPos);
                playMusic((int) song.getId(), song.getPath(), song.getName(),
                        song.getDesc(), "", song.getAlbumId(),
                        song.getAlbumName(), false);
                currentPlaylistSongId = playingPos;
            } catch (NullPointerException e) {
                playMusic(playList.getFirstSong());
            }
        } else {
            Toast.makeText(MusicService.this, getString(R.string.nothing_to_play), Toast.LENGTH_SHORT).show();
        }
    }

    private void playMusic(SongListItem song) {
        playMusic((int) song.getId(), song.getPath(), song.getName(), song.getDesc(), "", song.getAlbumId(), song.getAlbumName(), true);
    }

    private void playMusic(final int songId, final String songPath, final String songName, final String songDesc,
                           final String songArt, final long albumId, final String albumName, boolean singlePlay) {

        int result = audioManager.requestAudioFocus(afChangeListener,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        if(result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            if (singlePlay) {
                currentPlaylistSongId = -1;
                pausedSong = new SongListItem(0, songName, songDesc, songPath, false, albumId, albumName, 0);
                pausedSongPlaylistId = -1;
                singleSong = true;
            } else {
                singleSong = false;
            }
            try {
                stopMusic();
                mediaPlayer = new MediaPlayer();
                mediaSession = new MediaSession(this, "MusicService");
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mediaPlayer.setDataSource(songPath);
                mediaPlayer.prepare();
                currentPlaylistSongId = songId;
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        if (currentPlaylistSongId != playList.getPlaybackTableSize()) {
                            pausedSongSeek = 0;
                            SongListItem song = playList.getNextSong(currentPlaylistSongId);
                            playMusic((int) song.getId(), song.getPath(), song.getName(),
                                    song.getDesc(), "", song.getAlbumId(),
                                    song.getAlbumName(), false);
                            updateCurrentPlaying();
                        } else {
                            currentPlaylistSongId = -1;
                            pausedSongPlaylistId = -1;
                            pausedSong = new SongListItem(0, songName, songDesc, songPath, false, albumId, albumName, 0);
                            pausedSongSeek = 0;
                            stopMusic();
                        }
                    }
                });
                mediaPlayer.start();
                mediaPlayer.seekTo(pausedSongSeek);
                setNotificationPlayer(false);
                changeNotificationDetails(songPath, songName, songDesc, albumId, albumName);
                notificationCompat.bigContentView.setImageViewResource(R.id.noti_play_button,
                        R.drawable.ic_pause);
            } catch (IOException e) {
                Toast.makeText(MusicService.this, getString(R.string.file_invalid), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(MusicService.this, getString(R.string.unable_gain_focus), Toast.LENGTH_SHORT).show();
        }
    }

    private void setNotificationPlayer(boolean stop) {
        if (stop)
            notificationCompat = createBuilderNotificationRemovable().build();
        else
            notificationCompat = createBuiderNotification().build();
        notificationCompat.contentView = new RemoteViews(getPackageName(), R.layout.notification_collapsed_layout);
        notificationCompat.bigContentView = new RemoteViews(getPackageName(), R.layout.notification_layout);
        notificationCompat.bigContentView.setImageViewResource(R.id.noti_play_button,
                R.drawable.ic_play);
        notificationCompat.priority = Notification.PRIORITY_MAX;
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        startForeground(NOTIFICATION_ID, notificationCompat);
        notificationManager.notify(NOTIFICATION_ID, notificationCompat);
    }

    private void changeNotificationDetails(String songPathArg, String songNameArg, String songDescArg,
                                           long albumIdArg, String albumNameArg) {
        this.songName = songNameArg;
        this.albumName = albumNameArg;
        this.songDesc = songDescArg;
        this.songPath = songPathArg;
        this.albumId = albumIdArg;

        notificationCompat.bigContentView.setTextViewText(R.id.noti_song_name, songName);
        notificationCompat.bigContentView.setTextViewText(R.id.noti_song_artist, songDesc);
        notificationCompat.bigContentView.setTextViewText(R.id.noti_song_album, albumName);
        notificationCompat.contentView.setTextViewText(R.id.noti_song_name, songName);
        notificationCompat.contentView.setTextViewText(R.id.noti_song_artist, songDesc);
        Intent playClick = new Intent();
        playClick.setAction(MusicService.ACTION_STOP);
        PendingIntent playClickIntent = PendingIntent.getBroadcast(MusicService.this, 21021, playClick, 0);
        notificationCompat.bigContentView.setOnClickPendingIntent(R.id.noti_play_button, playClickIntent);
        notificationCompat.contentView.setOnClickPendingIntent(R.id.noti_play_button, playClickIntent);
        Intent prevClick = new Intent();
        prevClick.setAction(MusicService.ACTION_PREV);
        PendingIntent prevClickIntent = PendingIntent.getBroadcast(MusicService.this, 21121, prevClick, 0);
        notificationCompat.bigContentView.setOnClickPendingIntent(R.id.noti_prev_button, prevClickIntent);
        notificationCompat.contentView.setOnClickPendingIntent(R.id.noti_prev_button, prevClickIntent);
        Intent nextClick = new Intent();
        nextClick.setAction(MusicService.ACTION_NEXT);
        PendingIntent nextClickIntent = PendingIntent.getBroadcast(MusicService.this, 21221, nextClick, 0);
        notificationCompat.bigContentView.setOnClickPendingIntent(R.id.noti_next_button, nextClickIntent);
        notificationCompat.contentView.setOnClickPendingIntent(R.id.noti_next_button, nextClickIntent);
        notificationManager.notify(NOTIFICATION_ID, notificationCompat);

        new ChangeNotificationDetails(MusicService.this, albumId, notificationManager, notificationCompat).execute();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        playList = new MusicPlayerDBHelper(this);
        IntentFilter commandFilter = new IntentFilter();
        commandFilter.addAction(ACTION_PLAY);
        commandFilter.addAction(ACTION_STOP);
        commandFilter.addAction(ACTION_PLAY_SINGLE);
        commandFilter.addAction(ACTION_PLAY_ALL_SONGS);
        commandFilter.addAction(ACTION_ADD_SONG);
        commandFilter.addAction(ACTION_REQUEST_SONG_DETAILS);
        commandFilter.addAction(ACTION_SEEK_TO);
        commandFilter.addAction(ACTION_SEEK_GET);
        commandFilter.addAction(ACTION_NEXT);
        commandFilter.addAction(ACTION_MENU_FROM_PLAYLIST);
        commandFilter.addAction(ACTION_PLAY_NEXT);
        commandFilter.addAction(ACTION_PLAY_FROM_PLAYLIST);
        commandFilter.addAction(ACTION_PLAY_PLAYLIST);
        commandFilter.addAction(ACTION_PREV);
        commandFilter.addAction(ACTION_REMOVE_SERVICE);
        commandFilter.addAction(ACTION_ADD_SONG_MULTI);
        commandFilter.addAction(ACTION_PLAY_ALBUM);
        commandFilter.addAction(ACTION_SHUFFLE_PLAYLIST);
        registerReceiver(musicPlayer, commandFilter);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        new Runnable() {
            @Override
            public void run() {
                songList = new ArrayList<>();
                Cursor musicCursor2;
                final String where2 = MediaStore.Audio.Media.IS_MUSIC + "=1";
                final String orderBy2 = MediaStore.Audio.Media.TITLE;
                musicCursor2 = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        null, where2, null, orderBy2);
                if (musicCursor2 != null && musicCursor2.moveToFirst()) {
                    int titleColumn = musicCursor2.getColumnIndex
                            (android.provider.MediaStore.Audio.Media.TITLE);
                    int idColumn = musicCursor2.getColumnIndex
                            (android.provider.MediaStore.Audio.Media._ID);
                    int artistColumn = musicCursor2.getColumnIndex
                            (android.provider.MediaStore.Audio.Media.ARTIST);
                    int pathColumn = musicCursor2.getColumnIndex
                            (MediaStore.Audio.Media.DATA);
                    int albumIdColumn = musicCursor2.getColumnIndex
                            (MediaStore.Audio.Media.ALBUM_ID);
                    int albumColumn = musicCursor2.getColumnIndex
                            (MediaStore.Audio.Media.ALBUM);
                    int i2 = 0;
                    do {
                        i2++;
                        songList.add(new SongListItem(musicCursor2.getLong(idColumn),
                                musicCursor2.getString(titleColumn),
                                musicCursor2.getString(artistColumn),
                                musicCursor2.getString(pathColumn), false,
                                musicCursor2.getLong(albumIdColumn),
                                musicCursor2.getString(albumColumn),
                                i2));
                    }
                    while (musicCursor2.moveToNext());
                }
                if (musicCursor2 != null) {
                    musicCursor2.close();
                }
            }
        }.run();
        return START_STICKY;
    }

    private NotificationCompat.Builder createBuiderNotification() {
        Intent notificationIntent = new Intent();
        notificationIntent.setAction(MusicService.ACTION_REQUEST_SONG_DETAILS);
        PendingIntent contentIntent = PendingIntent.getBroadcast(MusicService.this, 0, notificationIntent, 0);
        Intent deleteIntent = new Intent();
        deleteIntent.setAction(MusicService.ACTION_REMOVE_SERVICE);
        PendingIntent deletePendingIntent = PendingIntent.getBroadcast(MusicService.this, 0, deleteIntent, 0);
        return new NotificationCompat.Builder(this)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_audiotrack)
                .setContentIntent(contentIntent)
                .setDeleteIntent(deletePendingIntent);
    }

    private NotificationCompat.Builder createBuilderNotificationRemovable() {
        Intent notificationIntent = new Intent();
        notificationIntent.setAction(MusicService.ACTION_REQUEST_SONG_DETAILS);
        PendingIntent contentIntent = PendingIntent.getActivity(MusicService.this, 0, notificationIntent, 0);
        Intent deleteIntent = new Intent();
        deleteIntent.setAction(MusicService.ACTION_REMOVE_SERVICE);
        PendingIntent deletePendingIntent = PendingIntent.getBroadcast(MusicService.this, 0, deleteIntent, 0);
        return new NotificationCompat.Builder(this)
                .setOngoing(false)
                .setSmallIcon(R.drawable.ic_audiotrack)
                .setContentIntent(contentIntent)
                .setDeleteIntent(deletePendingIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        audioManager.abandonAudioFocus(afChangeListener);
        if(mediaSession != null) {
            mediaSession.release();
        }
        if(mediaPlayer != null) {
            mediaPlayer.release();
        }
    }

}
