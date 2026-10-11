package com.Practice.URL.service;

import com.Practice.URL.entity.Url;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class QrCodeService {

    private final UrlService urlService;
    private final String baseUrl;

    public QrCodeService(UrlService urlService, @Value("${app.base-url}") String baseUrl) {
        this.urlService = urlService;
        this.baseUrl = baseUrl;
    }

    @Transactional(readOnly = true)
    public byte[] generateQrCode(Long urlId, String currentUserEmail) {
        Url url = urlService.getOwnedUrl(urlId, currentUserEmail);
        String shortUrl = baseUrl + "/" + url.getShortCode();

        try {
            BitMatrix matrix = new QRCodeWriter().encode(shortUrl, BarcodeFormat.QR_CODE, 300, 300);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return output.toByteArray();
        } catch (WriterException | IOException ex) {
            throw new IllegalStateException("Could not generate QR code", ex);
        }
    }
}
