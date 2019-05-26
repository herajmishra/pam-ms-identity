package com.sunrich.pam.pammsidentity.util;

import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.Writer;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.sunrich.pam.common.domain.User;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

import static com.google.zxing.BarcodeFormat.QR_CODE;

@Slf4j
public class CodeUtil {

  private static final String HOST_LABEL = "sunrichtech.com";

  private CodeUtil() {
  }

  /**
   * Generates QR Code and writes it to the given response
   *
   * @param user     - user object
   * @param response - response to write QR code image
   */
  public static void generateCode(User user, HttpServletResponse response) {
    String data = getQRBarcodeURL(user.getUserName(), HOST_LABEL, user.getSecret());

    BitMatrix matrix = null;
    Writer writer = new MultiFormatWriter();
    try {
      Map<EncodeHintType, String> hints = new EnumMap<>(EncodeHintType.class);
      hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
      matrix = writer.encode(data, QR_CODE, 400, 400, hints);
    } catch (com.google.zxing.WriterException e) {
      log.error(e.getMessage());
    }

    try {
      MatrixToImageWriter.writeToStream(matrix, "PNG", response.getOutputStream());
    } catch (IOException e) {
      log.error(e.getMessage());
    }
  }

  private static String getQRBarcodeURL(String user, String host, String secret) {
    String format = "otpauth://totp/%s@%s?secret=%s";
    return String.format(format, user, host, secret);
  }
}
