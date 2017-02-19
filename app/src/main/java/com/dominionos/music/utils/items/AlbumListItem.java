package com.dominionos.music.utils.items;

public class AlbumListItem {

    private final long id;
    private final String name;
    private final String desc;
    private final String artString;
    private final int songCount;

    public AlbumListItem(long id, String name, String desc, String artString, int songCount) {
        this.desc = desc;
        this.id = id;
        this.name = name;
        this.artString = artString;
        this.songCount = songCount;
    }

    public long getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getDesc() {
        return this.desc;
    }

    public String getArtString() {
        return this.artString;
    }

    public int getSongCount() {
        return songCount;
    }

}
