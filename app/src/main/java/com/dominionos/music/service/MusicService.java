package com.dominionos.music.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.NotificationCompat;
import android.widget.Toast;

import com.dominionos.music.R;
import com.dominionos.music.ui.layouts.activity.MainActivity;
import com.dominionos.music.utils.MusicPlayerDBHelper;
import com.dominionos.music.utils.MySQLiteHelper;
import com.dominionos.music.utils.items.SongListItem;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class MusicService extends Service {

    private MediaPlayer mediaPlayer;

    private boolean repeatOne = false, repeatAll = false, shuffle = false;
    private int currentPlaylistSongId = -1, pausedSongSeek;
    private SongListItem pausedSong;
    private MusicPlayerDBHelper playList;
    private AudioManager audioManager;
    private MediaSessionCompat mediaSession;
    private ArrayList<SongListItem> songList, preShuffle, playingList;
    private NotificationManagerCompat notificationManager;
    private SongListItem currentSong;

    private final AudioManager.OnAudioFocusChangeListener afChangeListener =
            new AudioManager.OnAudioFocusChangeListener() {
                public void onAudioFocusChange(int focusChange) {
                    if(mediaPlayer != null) {
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
                }
            };

    private static final int NOTIFICATION_ID = 596;
    public static final String ACTION_PREV = "prev";
    public static final String ACTION_NEXT = "next";
    public static final String ACTION_TOGGLE_PLAY = "toggle_play";
    private static final String ACTION_CANCEL_NOTIFICATION = "cancel_notification";
    public static final String ACTION_PLAY_ALBUM = "player_play_album";
    public static final String ACTION_PLAY_ALL_SONGS = "play_all_songs";
    public static final String ACTION_MENU_FROM_PLAYLIST = "player_menu_from_playlist";
    public static final String ACTION_PLAY_FROM_PLAYLIST = "player_play_from_playlist";
    public static final String ACTION_PLAY_PLAYLIST = "player_play_playlist";
    public static final String ACTION_PLAY_NEXT = "player_play_next";
    public static final String ACTION_PLAY_SINGLE = "play_single_song";
    public static final String ACTION_ADD_SONG = "add_song_to_playlist";
    public static final String ACTION_REQUEST_SONG_DETAILS = "player_request_song_details";
    public static final String ACTION_SEEK_TO = "player_seek_to_song";
    private static final String ACTION_SEEK_GET = "player_seek_get_song";
    public static final String ACTION_SHUFFLE_PLAYLIST = "player_shuffle_playlist";
    public static final String ACTION_REPEAT = "repeat";

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
        SongListItem song;
        switch(intent.getAction()) {
            case ACTION_TOGGLE_PLAY:
                if(mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                } else if(mediaPlayer != null && !mediaPlayer.isPlaying()) {
                    mediaPlayer.start();
                } else if(mediaPlayer == null) {
                    playMusic(playingList.get(0));
                }
                updatePlayState();
                break;
            case ACTION_PLAY_SINGLE:
                pausedSongSeek = 0;
                song = (SongListItem) intent.getSerializableExtra("song");
                playSingle(song);
                currentPlaylistSongId = 0;
                pausedSongSeek = 0;
                playList.clearPlayingList();
                playList.addSongs(playingList);
                Intent requestSongDetails = new Intent();
                requestSongDetails.setAction(ACTION_REQUEST_SONG_DETAILS);
                sendBroadcast(requestSongDetails);
                break;
            case ACTION_CANCEL_NOTIFICATION:
                stopNotification();
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
                    do {
                        playList.addSong(new SongListItem(musicCursor.getLong(idColumn),
                                musicCursor.getString(titleColumn),
                                musicCursor.getString(artistColumn),
                                musicCursor.getString(pathColumn), false,
                                musicCursor.getLong(albumIdColumn),
                                musicCursor.getString(albumNameColumn)));
                    }
                    while (musicCursor.moveToNext());
                }
                if (musicCursor != null) {
                    musicCursor.close();
                }
                playMusic(0);
                requestSongDetails = new Intent();
                requestSongDetails.setAction(ACTION_REQUEST_SONG_DETAILS);
                sendBroadcast(requestSongDetails);
                break;
            case ACTION_NEXT:
                pausedSongSeek = 0;
                playMusic(playList.getNextSong(currentPlaylistSongId));
                updateCurrentPlaying();
                break;
            case ACTION_PREV:
                if (mediaPlayer != null && mediaPlayer.getCurrentPosition() >= 5000) {
                    mediaPlayer.seekTo(0);
                } else {
                    pausedSongSeek = 0;
                    playMusic(playList.getPrevSong(currentPlaylistSongId));
                }
                updateCurrentPlaying();
                break;
            case ACTION_REQUEST_SONG_DETAILS:
                updateCurrentPlaying();
                updatePlaylist();
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
            case ACTION_SHUFFLE_PLAYLIST:
                if(playList.getPlaybackTableSize() > 1) {
                    if(!shuffle) {
                        String currentPlayingId = playList.getSong(currentPlaylistSongId).getName();
                        preShuffle = playList.getCurrentPlayingList();
                        playList.shuffleRows();
                        updatePlaylist();
                        ArrayList<SongListItem> songsList = playList.getCurrentPlayingList();
                        for (int num = 0; num < playList.getPlaybackTableSize(); num++) {
                            if (currentPlayingId.matches(songsList.get(num).getName())) {
                                currentPlaylistSongId = (int) songsList.get(num).getId();
                                break;
                            }
                        }
                        shuffle = true;
                    } else {
                        if(preShuffle != null) {
                            String currentPlayingId = playList.getSong(currentPlaylistSongId).getName();
                            playList.clearPlayingList();
                            playList.addSongs(preShuffle);
                            updatePlaylist();
                            ArrayList<SongListItem> songsList = playList.getCurrentPlayingList();
                            for (int num = 0; num < playList.getPlaybackTableSize(); num++) {
                                if (currentPlayingId.matches(songsList.get(num).getName())) {
                                    currentPlaylistSongId = (int) songsList.get(num).getId();
                                    break;
                                }
                            }
                        }
                        shuffle = false;
                    }
                    updateShuffle();
                }
                break;
            case ACTION_PLAY_NEXT:
                int insertPos = playingList.indexOf(currentSong) + 1;
                song = (SongListItem) intent.getSerializableExtra("song");
                playingList.add(insertPos, song);
                playList.clearPlayingList();
                playList.addSongs(playingList);
                updatePlaylist();
                break;
            case ACTION_ADD_SONG:
                song = (SongListItem) intent.getSerializableExtra("song");
                if (playList.getPlaybackTableSize() != 0 && currentPlaylistSongId != -1) {
                    playingList.add(song);
                    playList.clearPlayingList();
                    playList.addSongs(playingList);
                    updatePlaylist();
                } else {
                    intent.setAction(ACTION_PLAY_SINGLE);
                    sendBroadcast(intent);
                }
                break;
            case ACTION_PLAY_FROM_PLAYLIST:
                pausedSongSeek = 0;
                song = (SongListItem) intent.getSerializableExtra("song");
                playSingle(song);
                updateCurrentPlaying();
                requestSongDetails = new Intent();
                requestSongDetails.setAction(MusicService.ACTION_REQUEST_SONG_DETAILS);
                sendBroadcast(requestSongDetails);
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
                    SongListItem song2 = playList.getSong(pos);
                    File file = new File(song2.getPath());
                    boolean deleted = file.delete();
                    if (deleted) {
                        Toast.makeText(context, getString(R.string.song_delete_success), Toast.LENGTH_SHORT).show();
                        context.getContentResolver().delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                MediaStore.MediaColumns._ID + "='" + song2.getId() + "'", null);
                        playList.removeSong(pos);
                        updatePlaylist();
                    } else
                        Toast.makeText(context, getString(R.string.song_delete_fail), Toast.LENGTH_SHORT).show();
                }
                break;
            case ACTION_PLAY_PLAYLIST:
                MySQLiteHelper helper = new MySQLiteHelper(context);
                playList.clearPlayingList();
                playingList = helper.getPlayListSongs(intent.getIntExtra("playlistId", -1));
                playList.addSongs(playingList);
                playMusic(playList.getFirstSong());
                requestSongDetails = new Intent();
                requestSongDetails.setAction(MusicService.ACTION_REQUEST_SONG_DETAILS);
                sendBroadcast(requestSongDetails);
                break;
            case ACTION_PLAY_ALL_SONGS:
                if(songList != null) {
                    playList.clearPlayingList();
                    playList.addSongs(songList);
                    playingList = songList;
                    pausedSongSeek = 0;
                    playMusic(playingList.get(0));
                    requestSongDetails = new Intent();
                    requestSongDetails.setAction(MusicService.ACTION_REQUEST_SONG_DETAILS);
                    sendBroadcast(requestSongDetails);
                } else {
                    Toast.makeText(context, getString(R.string.service_generate_list_warning), Toast.LENGTH_LONG).show();
                }
                break;
            case ACTION_REPEAT:
                if(!repeatAll && !repeatOne) {
                    repeatAll = true;
                    repeatOne = false;
                } else if(repeatAll) {
                    repeatAll = false;
                    repeatOne = true;
                } else {
                    repeatAll = false;
                    repeatOne = false;
                }
                updateRepeat();
                break;
        }
    }

    private void updateRepeat() {
        Intent intent = new Intent(MainActivity.ACTION_UPDATE_REPEAT);
        if(repeatAll) {
            intent.putExtra("repeat", "all");
        } else if (repeatOne) {
            intent.putExtra("repeat", "one");
        } else {
            intent.putExtra("repeat", "none");
        }
        sendBroadcast(intent);
    }

    private void updateShuffle() {
        Intent intent = new Intent(MainActivity.ACTION_UPDATE_SHUFFLE);
        intent.putExtra("shuffle", shuffle);
        sendBroadcast(intent);
        updatePlaylist();
    }

    private void updateCurrentPlaying() {
        Intent intent = new Intent(MainActivity.ACTION_GET_PLAYING_DETAIL);
        if(currentSong != null) {
            intent.putExtra("song", currentSong);
        } else if(playingList.size() > 0){
            intent.putExtra("song", playingList.get(0));
        }
        if(mediaPlayer != null) {
            intent.putExtra("songDuration", mediaPlayer.getDuration());
            intent.putExtra("songCurrTime", mediaPlayer.getCurrentPosition());
        }
        sendBroadcast(intent);
        updatePlayState();
        if(currentSong != null) updateSession("metadata");
    }

    private void updatePlayState() {
        Intent intent = new Intent(MainActivity.ACTION_GET_PLAY_STATE);
        intent.putExtra("isPlaying", mediaPlayer != null && mediaPlayer.isPlaying());
        sendBroadcast(intent);
        if(mediaPlayer != null && currentSong != null) updateNotification();
        updateSession("state");
    }

    private void updatePlaylist() {
        Intent playlistIntent = new Intent(MainActivity.ACTION_GET_PLAYING_LIST);
        sendBroadcast(playlistIntent);
    }

    private void stopMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        updateCurrentPlaying();
    }

    private void playMusic(int playingPos) {
        if (playList.getPlaybackTableSize() > 0) {
            try {
                SongListItem song = playList.getSong(playingPos);
                playMusic(song);
                currentPlaylistSongId = playingPos;
            } catch (NullPointerException e) {
                playMusic(playList.getFirstSong());
            }
        } else {
            Toast.makeText(MusicService.this, getString(R.string.nothing_to_play), Toast.LENGTH_SHORT).show();
        }
    }

    private void playSingle(SongListItem song) {
        playingList.clear();
        playingList.add(song);
        playMusic(song);
    }

    private void playMusic(final SongListItem song) {

        int result = audioManager.requestAudioFocus(afChangeListener,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        if(result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            currentSong = song;
            try {
                stopMusic();
                mediaPlayer = new MediaPlayer();
                notificationManager = NotificationManagerCompat.from(this);
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mediaPlayer.setDataSource(song.getPath());
                mediaPlayer.prepare();
                currentPlaylistSongId =  (int) song.getId();
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        if(!repeatOne && !repeatAll) {
                            if(playingList.size() == 1) {
                                currentPlaylistSongId = -1;
                                pausedSong = song;
                                pausedSongSeek = 0;
                                stopMusic();
                            } else {
                                pausedSongSeek = 0;
                                SongListItem song = playList.getNextSong(currentPlaylistSongId);
                                playMusic(song);
                                updateCurrentPlaying();
                            }
                        } else if(repeatOne) {
                            playMusic(song);
                        } else {
                            if(playingList.size() == 1) {
                                playMusic(song);
                                updateCurrentPlaying();
                            } else if(playingList.size() != 1) {
                                pausedSongSeek = 0;
                                SongListItem song = playList.getNextSong(currentPlaylistSongId);
                                playMusic(song);
                                updateCurrentPlaying();
                            } else if(playingList.size() == playingList.indexOf(song)) {
                                playMusic(playingList.get(0));
                            }
                        }
                        updateSession("metadata");
                    }
                });
                mediaPlayer.start();
                mediaPlayer.seekTo(pausedSongSeek);
                startForeground(NOTIFICATION_ID, createNotification());
                updateSession("state");
                updateSession("metadata");
            } catch (IOException e) {
                Toast.makeText(MusicService.this, getString(R.string.file_invalid), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(MusicService.this, getString(R.string.unable_gain_focus), Toast.LENGTH_SHORT).show();
        }

        Intent intent = new Intent(MainActivity.ACTION_GET_PLAY_STATE);
        intent.putExtra("isPlaying", mediaPlayer != null && mediaPlayer.isPlaying());
        sendBroadcast(intent);
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        playList = new MusicPlayerDBHelper(this);
        IntentFilter commandFilter = new IntentFilter();
        commandFilter.addAction(ACTION_TOGGLE_PLAY);
        commandFilter.addAction(ACTION_CANCEL_NOTIFICATION);
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
        commandFilter.addAction(ACTION_PLAY_ALBUM);
        commandFilter.addAction(ACTION_SHUFFLE_PLAYLIST);
        commandFilter.addAction(ACTION_REPEAT);
        registerReceiver(musicPlayer, commandFilter);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        mediaSession = new MediaSessionCompat(this, "MusicService");
        updateSession("state");
        MediaButtonReceiver.handleIntent(mediaSession, intent);
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                Intent intent = new Intent(ACTION_TOGGLE_PLAY);
                sendBroadcast(intent);
                super.onPlay();
            }
            @Override
            public void onPause() {
                Intent intent = new Intent(ACTION_TOGGLE_PLAY);
                sendBroadcast(intent);
                super.onPause();
            }
            @Override
            public void onSkipToNext() {
                Intent intent = new Intent(ACTION_NEXT);
                sendBroadcast(intent);
                super.onSkipToNext();
            }
            @Override
            public void onSkipToPrevious() {
                Intent intent = new Intent(ACTION_PREV);
                sendBroadcast(intent);
                super.onSkipToPrevious();
            }
        });
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setActive(true);

        ArrayList<SongListItem> databaseList = playList.getCurrentPlayingList();
        if(databaseList.size() != 0) {
            playingList = databaseList;
        } else {
            playingList = new ArrayList<>();
        }

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
                    do {
                        songList.add(new SongListItem(musicCursor2.getLong(idColumn),
                                musicCursor2.getString(titleColumn),
                                musicCursor2.getString(artistColumn),
                                musicCursor2.getString(pathColumn), false,
                                musicCursor2.getLong(albumIdColumn),
                                musicCursor2.getString(albumColumn)));
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

    private void updateSession(String changed) {

        int playState = mediaPlayer != null && mediaPlayer.isPlaying()
                ? PlaybackStateCompat.STATE_PLAYING
                : PlaybackStateCompat.STATE_PAUSED;

        long playBackStateActions = PlaybackStateCompat.ACTION_PLAY |
                PlaybackStateCompat.ACTION_PAUSE |
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
        if(changed.equals("metadata")) {
            mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, currentSong.getAlbumName())
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentSong.getName())
                    .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, getAlbumArt())
                    .build());
        } else if(changed.equals("state")) {
            mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                    .setActions(playBackStateActions)
                    .setState(playState, mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0, 1.0f)
                    .build());
        }
    }

    private Bitmap getAlbumArt() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = false;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap albumArt;
        Cursor cursor = getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART},
                MediaStore.Audio.Albums._ID + "=?",
                new String[]{String.valueOf(currentSong.getAlbumId())},
                null);
        String songArt = "";
        if (cursor != null && cursor.moveToFirst()) {
            songArt = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
        }
        try {
            albumArt = BitmapFactory.decodeFile(songArt, options);
        } catch (IllegalArgumentException e) {
            albumArt = BitmapFactory.decodeResource(getResources(), R.drawable.default_art, options);
        }
        if(cursor != null) cursor.close();
        return albumArt;
    }

    private Notification createNotification() {
        PendingIntent playIntent = PendingIntent.getBroadcast(this, 100,
                new Intent(ACTION_TOGGLE_PLAY).setPackage(getPackageName()), PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent prevIntent = PendingIntent.getBroadcast(this, 100,
                new Intent(ACTION_PREV).setPackage(getPackageName()), PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent nextIntent = PendingIntent.getBroadcast(this, 100,
                new Intent(ACTION_NEXT).setPackage(getPackageName()), PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent cancelIntent = PendingIntent.getBroadcast(this, 100,
                new Intent(ACTION_CANCEL_NOTIFICATION).setPackage(getPackageName()), PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent launchAppIntent = PendingIntent.getActivity(this, 100,
                new Intent(getApplicationContext(), MainActivity.class).setPackage(getPackageName()), PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this);
        notificationBuilder
                .setStyle(new NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(1))
                .setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary))
                .setContentIntent(launchAppIntent)
                .setSmallIcon(R.drawable.ic_audiotrack)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentTitle(currentSong.getName())
                .setContentText(currentSong.getDesc())
                .setLargeIcon(getAlbumArt());
        NotificationCompat.Action prev = new NotificationCompat.Action(R.drawable.ic_skip_previous, getString(R.string.previous), prevIntent);
        NotificationCompat.Action playPause;
        notificationBuilder.addAction(prev);
        if(mediaPlayer.isPlaying()) {
            playPause = new NotificationCompat.Action(R.drawable.ic_pause, getString(R.string.play), playIntent);
        } else {
            playPause = new NotificationCompat.Action(R.drawable.ic_play, getString(R.string.play), playIntent);
        }
        notificationBuilder.addAction(playPause);
        if(playingList.size() > 1) {
            NotificationCompat.Action next = new NotificationCompat.Action(R.drawable.ic_skip_next, getString(R.string.next), nextIntent);
            notificationBuilder.addAction(next);
        }
        NotificationCompat.Action cancel = new NotificationCompat.Action(R.drawable.ic_remove, getString(R.string.cancel), cancelIntent);
        notificationBuilder.addAction(cancel);
        return notificationBuilder.build();
    }

    private void updateNotification() {
        notificationManager.notify(NOTIFICATION_ID, createNotification());
    }

    private void stopNotification() {
        stopMusic();
        notificationManager.cancelAll();
        stopForeground(true);
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
        notificationManager.cancelAll();
        stopForeground(true);
    }

}
