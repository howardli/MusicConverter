package com.xiahaimoyu.musicconverter.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

/**
 * 图片工具类
 */
public final class ImageUtils {

    private ImageUtils() {}

    /**
     * 缩放封面图片
     *
     * @param data     原始图片数据
     * @param maxWidth 最大宽度
     * @param maxHeight 最大高度
     * @return 缩放后的 JPEG 数据，无需缩放时返回原数据
     */
    public static Optional<byte[]> resize(byte[] data, int maxWidth, int maxHeight) {
        if (data == null || data.length == 0) {
            return Optional.empty();
        }

        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
            if (image == null) {
                return Optional.of(data);
            }

            int w = image.getWidth(), h = image.getHeight();
            if (w <= maxWidth && h <= maxHeight) {
                return Optional.of(data);
            }

            double ratio = Math.min((double) maxWidth / w, (double) maxHeight / h);
            Image scaled = image.getScaledInstance(
                    (int) (w * ratio), (int) (h * ratio), Image.SCALE_SMOOTH);

            BufferedImage result = new BufferedImage(
                    scaled.getWidth(null), scaled.getHeight(null), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = result.createGraphics();
            g.drawImage(scaled, 0, 0, null);
            g.dispose();

            return Optional.of(toJpegBytes(result));
        } catch (IOException e) {
            return Optional.of(data);
        }
    }

    /**
     * 检测图片 MIME 类型
     */
    public static Optional<String> detectMimeType(byte[] data) {
        if (data == null || data.length < 12) {
            return Optional.empty();
        }

        // JPEG
        if (match(data, 0, 0xFF, 0xD8, 0xFF)) {
            return Optional.of("image/jpeg");
        }
        // PNG
        if (match(data, 0, 0x89, 0x50, 0x4E, 0x47)) {
            return Optional.of("image/png");
        }
        // GIF
        if (match(data, 0, 'G', 'I', 'F')) {
            return Optional.of("image/gif");
        }
        // WebP
        if (match(data, 0, 'R', 'I', 'F', 'F') && match(data, 8, 'W', 'E', 'B', 'P')) {
            return Optional.of("image/webp");
        }
        // BMP
        if (match(data, 0, 'B', 'M')) {
            return Optional.of("image/bmp");
        }

        return Optional.empty();
    }

    private static boolean match(byte[] data, int offset, int... bytes) {
        for (int i = 0; i < bytes.length; i++) {
            if (offset + i >= data.length || (data[offset + i] & 0xFF) != bytes[i]) {
                return false;
            }
        }
        return true;
    }

    private static byte[] toJpegBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", out);
        return out.toByteArray();
    }
}