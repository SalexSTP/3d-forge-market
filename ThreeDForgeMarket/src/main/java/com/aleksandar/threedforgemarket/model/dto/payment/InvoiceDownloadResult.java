package com.aleksandar.threedforgemarket.model.dto.payment;

public record InvoiceDownloadResult(
        Type type,
        byte[] pdfBytes,
        String redirectUrl,
        String filename
) {

    public static InvoiceDownloadResult pdf(byte[] pdfBytes, String filename) {
        return new InvoiceDownloadResult(Type.PDF, pdfBytes, null, filename);
    }

    public static InvoiceDownloadResult redirect(String redirectUrl) {
        return new InvoiceDownloadResult(Type.REDIRECT, null, redirectUrl, null);
    }

    public enum Type {
        PDF,
        REDIRECT
    }
}
