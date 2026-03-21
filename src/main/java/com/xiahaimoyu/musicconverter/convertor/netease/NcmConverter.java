package com.xiahaimoyu.musicconverter.convertor.netease;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.xiahaimoyu.musicconverter.convertor.AbstractConverter;
import com.xiahaimoyu.musicconverter.convertor.ConversionContext;
import com.xiahaimoyu.musicconverter.model.MusicMeta;
import com.xiahaimoyu.musicconverter.util.ImageUtils;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.StandardArtwork;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;

/**
 * 网易云音乐 NCM 格式转换器
 * <p>
 * NCM 是网易云音乐的加密格式，使用 AES + RC4 加密
 *
 */
public final class NcmConverter extends AbstractConverter {

    private static final Set<String> EXTENSIONS = Collections.singleton("ncm");

    @Override
    public Set<String> supportedExtensions() {
        return EXTENSIONS;
    }

    @Override
    protected ConversionContext doConvert(Path source, Path tempFile) throws Exception {
        try (RandomAccessFile raf = new RandomAccessFile(source.toFile(), "r")) {
            if (!NcmFormat.isValid(raf)) {
                return null;
            }

            int[] sbox = NcmFormat.readKeyAndBuildSbox(raf);
            JSONObject metaJson = NcmFormat.readMetadata(raf);
            byte[] coverArt = NcmFormat.readCoverArt(raf);

            if (coverArt == null) {
                return null;
            }

            MusicMeta meta = parseMetadata(metaJson, coverArt);
            Path output = Paths.get(tempFile + "." + meta.getFormat());

            decryptAudio(raf, sbox, output);

            return ConversionContext.of(output, meta);
        }
    }

    @Override
    public void postProcess(ConversionContext context) throws Exception {
        MusicMeta meta = context.metadata();
        if (meta == null) return;

        AudioFile audio = AudioFileIO.read(context.tempFile().toFile());
        Tag tag = audio.getTagOrCreateAndSetDefault();

        tag.setField(FieldKey.ALBUM, nullSafe(meta.getAlbum()));
        tag.setField(FieldKey.TITLE, nullSafe(meta.getTitle()));
        tag.setField(FieldKey.ARTIST, nullSafe(meta.primaryArtist()));

        writeCoverArt(tag, meta.getCoverArt());
        audio.commit();
    }

    private MusicMeta parseMetadata(JSONObject json, byte[] coverArt) {
        return MusicMeta.builder()
                .album(json.getString("album"))
                .title(json.getString("musicName"))
                .artists(extractArtists(json.getJSONArray("artist")))
                .format(json.getString("format"))
                .coverArt(coverArt)
                .build();
    }

    private String[] extractArtists(JSONArray arr) {
        if (arr == null || arr.isEmpty()) return null;
        String[] names = new String[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            names[i] = arr.getJSONArray(i).getString(1);
        }
        return names;
    }

    private void writeCoverArt(Tag tag, byte[] data) throws Exception {
        if (data == null) return;

        byte[] image = ImageUtils.resize(data, 800, 800).orElse(data);
        ImageUtils.detectMimeType(image).ifPresent(mime -> {
            try {
                tag.deleteArtworkField();
                StandardArtwork artwork = new StandardArtwork();
                artwork.setBinaryData(image);
                artwork.setMimeType(mime);
                tag.setField(artwork);
            } catch (Exception ignored) {}
        });
    }

    private void decryptAudio(RandomAccessFile raf, int[] sbox, Path output) throws Exception {
        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(output.toFile()))) {
            byte[] buf = new byte[0x8000];
            int len;

            while ((len = raf.read(buf)) != -1) {
                for (int i = 0; i < len; i++) {
                    int j = (i + 1) & 0xFF;
                    buf[i] ^= sbox[(sbox[j] + sbox[(sbox[j] + j) & 0xFF]) & 0xFF];
                }
                out.write(buf, 0, len);
            }
        }
    }

    private static String nullSafe(String s) {
        return s != null ? s : "";
    }
}