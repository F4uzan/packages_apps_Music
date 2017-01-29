package com.dominionos.music.utils.items;

public class AlbumListItem {

    private final long id;
    private final String name;
    private final String desc;
    private String artString;

    public AlbumListItem(long id, String name, String desc, String artString) {
        this.desc = desc;
        this.id = id;
        this.name = name;
        this.artString = artString;
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

}
