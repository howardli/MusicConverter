package com.xiahaimoyu.musicconverter.model;

/**
 * 音乐元数据
 */
public class MusicMeta {

    private final String album;
    private final String title;
    private final String[] artists;
    private final byte[] coverArt;
    private final String format;
    private final String lyrics;

    private MusicMeta(Builder builder) {
        this.album = builder.album;
        this.title = builder.title;
        this.artists = builder.artists;
        this.coverArt = builder.coverArt;
        this.format = builder.format;
        this.lyrics = builder.lyrics;
    }

    public String getAlbum() { return album; }
    public String getTitle() { return title; }
    public String[] getArtists() { return artists; }
    public byte[] getCoverArt() { return coverArt; }
    public String getFormat() { return format; }
    public String getLyrics() { return lyrics; }

    public String primaryArtist() {
        return artists != null && artists.length > 0 ? artists[0] : null;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String album;
        private String title;
        private String[] artists;
        private byte[] coverArt;
        private String format;
        private String lyrics;

        public Builder album(String album) {
            this.album = album;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder artists(String[] artists) {
            this.artists = artists;
            return this;
        }

        public Builder coverArt(byte[] coverArt) {
            this.coverArt = coverArt;
            return this;
        }

        public Builder format(String format) {
            this.format = format;
            return this;
        }

        public Builder lyrics(String lyrics) {
            this.lyrics = lyrics;
            return this;
        }

        public MusicMeta build() {
            return new MusicMeta(this);
        }
    }
}