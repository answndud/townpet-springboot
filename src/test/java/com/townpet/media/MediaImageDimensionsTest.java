package com.townpet.media;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class MediaImageDimensionsTest {
  @Test
  void acceptsSmallImageHeader() throws IOException {
    assertThat(MediaImageDimensions.inspect("image/png", imageWithDimensions(1, 1)))
        .hasValue(new MediaImageDimensions.ImageDimensions(1, 1));
  }

  @Test
  void rejectsOversizedImageDimensionsBeforeFullDecode() throws IOException {
    assertThat(MediaImageDimensions.inspect("image/png", imageWithDimensions(6000, 6000)))
        .isEmpty();
  }

  private static byte[] imageWithDimensions(int width, int height) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), "png", output);
    byte[] png = output.toByteArray();
    ByteBuffer.wrap(png, 16, 4).putInt(width);
    ByteBuffer.wrap(png, 20, 4).putInt(height);
    CRC32 crc = new CRC32();
    crc.update(png, 12, 17);
    ByteBuffer.wrap(png, 29, 4).putInt((int) crc.getValue());
    return png;
  }
}
