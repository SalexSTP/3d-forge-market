package com.aleksandar.threedforgemarket.service.payment;

import com.aleksandar.threedforgemarket.integration.customprint.CustomPrintRequestDetailsClientDto;
import com.aleksandar.threedforgemarket.model.entity.CustomerOrder;
import com.aleksandar.threedforgemarket.model.entity.PaymentTransaction;
import com.aleksandar.threedforgemarket.model.entity.User;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class InvoicePdfService {
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);

    public byte[] generateProductOrderInvoice(
            PaymentTransaction payment,
            User customer,
            CustomerOrder order
    ) {
        BigDecimal unitPrice = order.getTotalPrice()
                .divide(BigDecimal.valueOf(order.getQuantity()), 2, RoundingMode.HALF_UP);

        return generate(
                payment,
                customer,
                new InvoiceLine(order.getProduct().getName(), order.getQuantity(), unitPrice, order.getTotalPrice())
        );
    }

    public byte[] generateCustomPrintInvoice(
            PaymentTransaction payment,
            User customer,
            CustomPrintRequestDetailsClientDto request
    ) {
        return generate(
                payment,
                customer,
                new InvoiceLine(request.title(), 1, request.quotedPrice(), request.quotedPrice())
        );
    }

    private byte[] generate(PaymentTransaction payment, User customer, InvoiceLine invoiceLine) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                PdfCanvas canvas = new PdfCanvas(content);
                LocalDate issueDate = invoiceDate(payment);
                String dueDate = LONG_DATE.format(issueDate);

                canvas.accentLine();
                canvas.text("Invoice", 48, 744, 24, PdfCanvas.BOLD, Color.BLACK);
                canvas.textRight("3DForgeMarket", 548, 744, 20, PdfCanvas.BOLD, PdfCanvas.MUTED);

                canvas.labelValue("Invoice number", invoiceNumber(payment), 48, 692);
                canvas.labelValue("Date of issue", SHORT_DATE.format(issueDate), 48, 676);
                canvas.labelValue("Date due", SHORT_DATE.format(issueDate), 48, 660);

                canvas.text("3DForgeMarket", 48, 616, 11, PdfCanvas.BOLD, Color.BLACK);
                canvas.text("Bulgaria", 48, 598, 11, PdfCanvas.REGULAR, Color.BLACK);

                canvas.text("Bill to", 300, 616, 11, PdfCanvas.BOLD, Color.BLACK);
                canvas.text(customer.getUsername(), 300, 598, 11, PdfCanvas.REGULAR, Color.BLACK);
                canvas.text("Bulgaria", 300, 580, 11, PdfCanvas.REGULAR, Color.BLACK);
                canvas.text(customer.getEmail(), 300, 562, 11, PdfCanvas.REGULAR, Color.BLACK);

                canvas.text(money(payment.getAmount()) + " due " + dueDate, 48, 500, 18, PdfCanvas.BOLD, Color.BLACK);

                canvas.text("Description", 48, 438, 9, PdfCanvas.REGULAR, Color.BLACK);
                canvas.textRight("Qty", 420, 438, 9, PdfCanvas.REGULAR, Color.BLACK);
                canvas.textRight("Unit price", 486, 438, 9, PdfCanvas.REGULAR, Color.BLACK);
                canvas.textRight("Amount", 548, 438, 9, PdfCanvas.REGULAR, Color.BLACK);
                canvas.horizontalLine(48, 548, 426, Color.BLACK, 0.7f);

                canvas.text(invoiceLine.description(), 48, 408, 11, PdfCanvas.REGULAR, Color.BLACK);
                canvas.textRight(String.valueOf(invoiceLine.quantity()), 420, 408, 11, PdfCanvas.REGULAR, Color.BLACK);
                canvas.textRight(money(invoiceLine.unitPrice()), 486, 408, 11, PdfCanvas.REGULAR, Color.BLACK);
                canvas.textRight(money(invoiceLine.amount()), 548, 408, 11, PdfCanvas.REGULAR, Color.BLACK);

                canvas.horizontalLine(300, 548, 374, PdfCanvas.LIGHT_LINE, 0.5f);
                canvas.totalRow("Subtotal", money(invoiceLine.amount()), 300, 358, false);
                canvas.horizontalLine(300, 548, 346, PdfCanvas.LIGHT_LINE, 0.5f);
                canvas.totalRow("Total", money(invoiceLine.amount()), 300, 334, false);
                canvas.horizontalLine(300, 548, 322, PdfCanvas.LIGHT_LINE, 0.5f);
                canvas.totalRow("Amount due", money(payment.getAmount()), 300, 310, true);

            }

            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not generate invoice PDF.", exception);
        }
    }

    private String invoiceNumber(PaymentTransaction payment) {
        String id = payment.getId() == null ? "UNKNOWN" : payment.getId().toString().replace("-", "");
        String suffix = id.length() <= 8 ? id : id.substring(0, 8);
        return "INV-" + suffix.toUpperCase(Locale.ROOT);
    }

    private LocalDate invoiceDate(PaymentTransaction payment) {
        return payment.getCreatedOn() == null
                ? LocalDate.now()
                : payment.getCreatedOn().toLocalDate();
    }

    private String money(BigDecimal amount) {
        BigDecimal safeAmount = amount == null ? BigDecimal.ZERO : amount;
        return "€" + safeAmount.setScale(2, RoundingMode.HALF_UP);
    }

    private record InvoiceLine(String description, int quantity, BigDecimal unitPrice, BigDecimal amount) {
    }

    private static class PdfCanvas {
        private static final PDType1Font REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        private static final PDType1Font BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        private static final Color ACCENT = new Color(15, 118, 110);
        private static final Color MUTED = new Color(115, 122, 132);
        private static final Color LIGHT_LINE = new Color(220, 226, 232);

        private final PDPageContentStream content;

        private PdfCanvas(PDPageContentStream content) {
            this.content = content;
        }

        private void accentLine() throws IOException {
            content.setNonStrokingColor(ACCENT);
            content.addRect(0, 838, 595, 4);
            content.fill();
        }

        private void labelValue(String label, String value, float x, float y) throws IOException {
            text(label, x, y, 10, BOLD, Color.BLACK);
            text(value, x + 96, y, 10, BOLD, Color.BLACK);
        }

        private void totalRow(String label, String value, float x, float y, boolean bold) throws IOException {
            PDType1Font font = bold ? BOLD : REGULAR;
            text(label, x, y, 10, font, Color.BLACK);
            textRight(value, 548, y, 10, font, Color.BLACK);
        }

        private void horizontalLine(float fromX, float toX, float y, Color color, float width) throws IOException {
            content.setStrokingColor(color);
            content.setLineWidth(width);
            content.moveTo(fromX, y);
            content.lineTo(toX, y);
            content.stroke();
        }

        private void text(String text, float x, float y, int size, PDType1Font font, Color color) throws IOException {
            content.beginText();
            content.setNonStrokingColor(color);
            content.setFont(font, size);
            content.newLineAtOffset(x, y);
            content.showText(sanitize(text));
            content.endText();
        }

        private void textRight(String text, float rightX, float y, int size, PDType1Font font, Color color) throws IOException {
            String sanitized = sanitize(text);
            float width = font.getStringWidth(sanitized) / 1000 * size;
            text(sanitized, rightX - width, y, size, font, color);
        }

        private String sanitize(String text) {
            if (text == null) {
                return "";
            }

            return text.replaceAll("[^\\x20-\\x7E€]", " ")
                    .replaceAll("[\\r\\n\\t]", " ");
        }
    }
}
