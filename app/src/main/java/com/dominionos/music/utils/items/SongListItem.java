package com.dominionos.music.utils.items;

import java.io.Serializable;

public class SongListItem implements Serializable {

    private final long id;
    private final long albumId;
    private final String name;
    private final String desc;
    private final String path;
    private final String albumName;
    private final Boolean fav;

    public SongListItem(long id, String name, String desc, String path,
                        Boolean fav, long albumId, String albumName) {
        this.desc = desc;
        this.fav = fav;
        this.path = path;
        this.id = id;
        this.name = name;
        this.albumId = albumId;
        this.albumName = albumName;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public String getAlbumName() {
        return albumName;
    }

    public String getDesc() {
        return desc;
    }

    public long getAlbumId() {
        return albumId;
    }


    public Boolean getFav() {
        return fav;
    }
}
