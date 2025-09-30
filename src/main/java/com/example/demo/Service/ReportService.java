package com.example.demo.Service;

import com.example.demo.Model.Glucose;
import com.example.demo.Repository.GlucoseRepository;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Date;
import java.util.List;

import com.itextpdf.layout.Document;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;

@Service
public class ReportService {

    @Autowired
    GlucoseRepository repository;

    public ByteArrayOutputStream generatePdfDataStream(Date startDate, Date endDate) {


        List<Glucose> data = repository.findByDateTimeBetween(startDate, endDate);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        addHeader(document, "Glucose Readings Report");
        addTable(document, data);
        addFooter(document);

        document.close();
        return outputStream;

    }

    private void addHeader(Document document, String headerText) {
        Paragraph header = new Paragraph(headerText)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(16);
        document.add(header);
    }

    private void addFooter(Document document) {
        Paragraph footer = new Paragraph("You are not defined by your numbers. You are defined by your courage to check them, understand them, and keep moving forward every single day.")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(10)
                .setFixedPosition(30, 30, UnitValue.createPercentValue(100));
        document.add(footer);
    }

    private void addTable(Document document, List<Glucose> tableData) {
        float[] columnWidths = {1, 5};
        Table table = new Table(columnWidths);

        // Adding table headers
        table.addHeaderCell(createStyledCell("Reading"));
        table.addHeaderCell(createStyledCell("Time"));

        // Adding table rows
        for (Glucose rowData : tableData) {
            table.addCell(createStyledCell(rowData.getGlucose().toString()));
            table.addCell(createStyledCell(rowData.getDateTime().toString()));
        }

        table.setHorizontalAlignment(HorizontalAlignment.CENTER);
        table.setWidth(UnitValue.createPercentValue(80));  // 80% of page width

        document.add(table);
    }

    private Cell createStyledCell(String content) {
        return new Cell().add(new Paragraph(content))
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setTextAlignment(TextAlignment.CENTER);
    }

}
