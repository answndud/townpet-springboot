package com.townpet.media;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

final class MediaImageDimensions {
  private static final int MAX_WIDTH = 8000;
  private static final int MAX_HEIGHT = 8000;
  private static final long MAX_PIXELS = 32_000_000L;

  private MediaImageDimensions() {}

  static Optional<ImageDimensions> inspect(String contentType, byte[] content) {
    if (!contentType.toLowerCase(java.util.Locale.ROOT).startsWith("image/")) {
      return Optional.empty();
    }
    try (ImageInputStream input =
        ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
      if (input == null) return Optional.empty();
      Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
      if (!readers.hasNext()) return Optional.empty();
      ImageReader reader = readers.next();
      try {
        reader.setInput(input, true, true);
        int width = reader.getWidth(0);
        int height = reader.getHeight(0);
        if (width < 1 || height < 1 || width > MAX_WIDTH || height > MAX_HEIGHT) {
          return Optional.empty();
        }
        long pixels = (long) width * height;
        return pixels > MAX_PIXELS
            ? Optional.empty()
            : Optional.of(new ImageDimensions(width, height));
      } finally {
        reader.dispose();
      }
    } catch (IOException | RuntimeException exception) {
      return Optional.empty();
    }
  }

  record ImageDimensions(int width, int height) {}
}
