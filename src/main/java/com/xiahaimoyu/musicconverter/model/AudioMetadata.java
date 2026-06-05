package com.xiahaimoyu.musicconverter.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 音频元数据
 * <p>
 * 包含标题、专辑、艺术家、封面等信息，使用 Builder 模式创建
 */
public final class AudioMetadata {

    private final String title;
    private final String album;
    private final List<String> artists;
    private final byte[] coverArt;
    private final String coverMimeType;
    private final String format;
    private final String lyrics;

    private AudioMetadata(Builder builder) {
        this.title = builder.title;
        this.album = builder.album;
        this.artists = builder.artists != null
                ? Collections.unmodifiableList(new ArrayList<>(builder.artists))
                : Collections.emptyList();
        this.coverArt = builder.coverArt;
        this.coverMimeType = builder.coverMimeType;
        this.format = builder.format;
        this.lyrics = builder.lyrics;
    }

    public String title() {
        return title != null ? title : "";
    }

    public String album() {
        return album != null ? album : "";
    }

    public List<String> artists() {
        return artists;
    }

    public byte[] coverArt() {
        return coverArt != null ? coverArt.clone() : null;
    }

    public String coverMimeType() {
        return coverMimeType;
    }

    public String format() {
        return format != null ? format : "";
    }

    public String lyrics() {
        return lyrics;
    }

    /**
     * 获取主要艺术家（第一个）
     */
    public String primaryArtist() {
        return artists.isEmpty() ? "" : artists.get(0);
    }

    /**
     * 获取艺术家显示字符串（逗号分隔）
     */
    public String artistDisplay() {
        return String.join(", ", artists);
    }

    /**
     * 判断是否有封面图片
     */
    public boolean hasCoverArt() {
        return coverArt != null && coverArt.length > 0;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder 内部类
     */
    public static final class Builder {
        private String title;
        private String album;
        private List<String> artists;
        private byte[] coverArt;
        private String coverMimeType;
        private String format;
        private String lyrics;

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder album(String album) {
            this.album = album;
            return this;
        }

        public Builder artists(List<String> artists) {
            this.artists = artists;
            return this;
        }

        public Builder artists(String[] artists) {
            this.artists = artists != null ? Arrays.asList(artists) : null;
            return this;
        }

        public Builder coverArt(byte[] coverArt) {
            this.coverArt = coverArt;
            return this;
        }

        public Builder coverMimeType(String coverMimeType) {
            this.coverMimeType = coverMimeType;
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

        public AudioMetadata build() {
            return new AudioMetadata(this);
        }
    }
}