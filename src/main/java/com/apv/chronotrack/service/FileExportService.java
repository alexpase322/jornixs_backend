package com.apv.chronotrack.service;

import com.apv.chronotrack.DTO.*;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.apache.poi.ss.usermodel.*; // Importante tener este import
import java.awt.*;
import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class FileExportService {

    public ByteArrayInputStream generateConsolidatedPdf(ConsolidatedPayrollReportDto report) throws DocumentException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, out);

        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        document.add(new Paragraph("Reporte de Nómina Consolidado", titleFont));
        document.add(new Paragraph("Periodo: " + report.getStartDate() + " al " + report.getEndDate()));
        document.add(Chunk.NEWLINE);

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        String[] headers = {"Trabajador", "H. Regulares", "H. Extras", "H. Totales", "Total a Pagar"};

        for(String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        for (ConsolidatedPayrollEntryDto entry : report.getEntries()) {
            table.addCell(entry.getWorkerName());
            table.addCell(String.format("%.2f", entry.getTotalRegularHours()));
            table.addCell(String.format("%.2f", entry.getTotalOvertimeHours()));
            table.addCell(String.format("%.2f", entry.getTotalHours()));
            table.addCell(String.format("$%.2f", entry.getTotalPay()));
        }

        document.add(table);

        document.add(Chunk.NEWLINE);
        Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
        Paragraph total = new Paragraph("Total General: $" + report.getGrandTotalPay(), totalFont);
        total.setAlignment(Element.ALIGN_RIGHT);
        document.add(total);

        document.close();
        return new ByteArrayInputStream(out.toByteArray());
    }

    public ByteArrayInputStream generateConsolidatedExcel(ConsolidatedPayrollReportDto report) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XSSFSheet sheet = workbook.createSheet("Reporte Nómina");

        String[] headers = {"Trabajador", "H. Regulares", "H. Extras", "H. Totales", "Total a Pagar"};
        Row headerRow = sheet.createRow(0);
        for(int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
        }

        int rowIdx = 1;
        for (ConsolidatedPayrollEntryDto entry : report.getEntries()) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(entry.getWorkerName());
            row.createCell(1).setCellValue(entry.getTotalRegularHours());
            row.createCell(2).setCellValue(entry.getTotalOvertimeHours());
            row.createCell(3).setCellValue(entry.getTotalHours());
            row.createCell(4).setCellValue(entry.getTotalPay().doubleValue());
        }

        workbook.write(out);
        workbook.close();
        return new ByteArrayInputStream(out.toByteArray());
    }

    public ByteArrayInputStream generateDetailedPdf(DetailedPayrollReportDto report) throws DocumentException, IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // Giramos la página a formato horizontal (A4.rotate()) para que quepan todas las columnas
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, out);
        document.open();

        // --- 1. ENCABEZADO DEL DOCUMENTO ---
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[] {1, 3});
        headerTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        // Espacio para el Logotipo (debes tener una imagen 'logo.png' en 'src/main/resources')
        try {
            Image logo = Image.getInstance(getClass().getClassLoader().getResource("logo.png"));
            logo.scaleToFit(80, 80);
            headerTable.addCell(logo);
        } catch (Exception e) {
            headerTable.addCell("Logo"); // Fallback si no encuentra la imagen
        }

        // Información de la Compañía
        PdfPCell companyInfoCell = new PdfPCell();
        companyInfoCell.setBorder(Rectangle.NO_BORDER);
        companyInfoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        companyInfoCell.addElement(new Phrase(report.getCompanyName(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
        companyInfoCell.addElement(new Phrase("\n" + report.getCompanyAddress()));
        companyInfoCell.addElement(new Phrase("\nTel: " + report.getCompanyPhoneNumber()));
        headerTable.addCell(companyInfoCell);
        document.add(headerTable);

        // Título del Reporte
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Paragraph title = new Paragraph("Reporte de Nómina Detallado", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Paragraph subtitle = new Paragraph("Trabajador: " + report.getWorkerName() +
                " | Periodo: " + report.getStartDate() + " al " + report.getEndDate() +
                " | Generado: " + LocalDate.now().format(DateTimeFormatter.ISO_DATE));
        subtitle.setAlignment(Element.ALIGN_CENTER);
        document.add(subtitle);
        document.add(Chunk.NEWLINE);

        // --- 2. CUERPO DEL DOCUMENTO (TABLAS DE HORAS) ---
        for (WeeklyPaySummaryDto weeklySummary : report.getWeeklySummaries()) {
            Font weekFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            document.add(new Paragraph("Semana: " + weeklySummary.getWorkWeek().getStartDate() + " - " + weeklySummary.getWorkWeek().getEndDate(), weekFont));

            PdfPTable table = new PdfPTable(9);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);
            table.setWidths(new float[] { 2.5f, 2f, 1f, 1.5f, 1.5f, 1f, 1.5f, 1f, 1.5f });

            String[] headers = {"Fecha", "Lugar", "Ingreso", "Ini. Almuerzo", "Fin Almuerzo", "Salida", "H. Lab.", "Tarifa", "Total Día"};
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
            for(String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(Color.LIGHT_GRAY);
                table.addCell(cell);
            }

            List<DailySummaryDto> dailySummaries = report.getDailySummariesByWeek().get(weeklySummary.getWorkWeek().getId());
            if (dailySummaries != null) {
                for (DailySummaryDto daily : dailySummaries) {
                    Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
                    table.addCell(new Phrase(String.valueOf(daily.getDate()), bodyFont));
                    table.addCell(new Phrase(daily.getWorkLocationName() != null ? daily.getWorkLocationName() : "N/A", bodyFont));
                    table.addCell(new Phrase(daily.getClockInTime() != null ? daily.getClockInTime().toString().substring(0, 5) : "-", bodyFont));
                    table.addCell(new Phrase(daily.getStartLunchTime() != null ? daily.getStartLunchTime().toString().substring(0, 5) : "-", bodyFont));
                    table.addCell(new Phrase(daily.getEndLunchTime() != null ? daily.getEndLunchTime().toString().substring(0, 5) : "-", bodyFont));
                    table.addCell(new Phrase(daily.getClockOutTime() != null ? daily.getClockOutTime().toString().substring(0, 5) : "-", bodyFont));
                    table.addCell(new Phrase(String.format("%.2f", daily.getTotalHours()), bodyFont));
                    table.addCell(new Phrase(String.format("$%.2f", daily.getDailyRate()), bodyFont));
                    table.addCell(new Phrase(String.format("$%.2f", daily.getTotalPay()), bodyFont));
                }
            }
            document.add(table);

            // Resumen Semanal
            PdfPTable summaryTable = new PdfPTable(4);
            summaryTable.setWidthPercentage(100);
            summaryTable.setSpacingBefore(5);
            summaryTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);
            summaryTable.getDefaultCell().setHorizontalAlignment(Element.ALIGN_RIGHT);
            summaryTable.addCell(new Phrase("H. Regulares: " + String.format("%.2f", weeklySummary.getRegularHours())));
            summaryTable.addCell(new Phrase("H. Extras: " + String.format("%.2f", weeklySummary.getOvertimeHours())));
            summaryTable.addCell(new Phrase("H. Totales: " + String.format("%.2f", weeklySummary.getTotalHours())));
            summaryTable.addCell(new Phrase("Pago Semanal: " + String.format("$%.2f", weeklySummary.getTotalPay()), FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
            document.add(summaryTable);
            document.add(Chunk.NEWLINE);
        }

        // --- 3. PIE DE PÁGINA PARA FIRMAS ---
        document.add(Chunk.NEWLINE);
        document.add(Chunk.NEWLINE);

        PdfPTable signatureTable = new PdfPTable(2);
        signatureTable.setWidthPercentage(80);
        signatureTable.setHorizontalAlignment(Element.ALIGN_CENTER);
        signatureTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        PdfPCell elaboratedCell = new PdfPCell(new Paragraph("\n\n\n______________________________\nElaborado por"));
        elaboratedCell.setBorder(Rectangle.NO_BORDER);
        elaboratedCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        signatureTable.addCell(elaboratedCell);

        PdfPCell receivedCell = new PdfPCell(new Paragraph("\n\n\n______________________________\nRecibido por (Firma del Trabajador)"));
        receivedCell.setBorder(Rectangle.NO_BORDER);
        receivedCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        signatureTable.addCell(receivedCell);

        document.add(signatureTable);

        document.close();
        return new ByteArrayInputStream(out.toByteArray());
    }

    // --- NUEVO MÉTODO PARA EXCEL DETALLADO ---
    public ByteArrayInputStream generateDetailedExcel(DetailedPayrollReportDto report) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XSSFSheet sheet = workbook.createSheet("Reporte Detallado");

        // Estilo para las cabeceras
        CellStyle headerStyle = workbook.createCellStyle();
        XSSFFont headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        // Cabeceras de la tabla
        String[] headers = {"Fecha", "Lugar de Trabajo", "Ingreso", "Inicio Almuerzo", "Fin Almuerzo", "Salida", "Horas Laboradas", "Tarifa", "Total Día"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIdx = 1;
        // Iteramos sobre cada semana en el reporte
        for (WeeklyPaySummaryDto weeklySummary : report.getWeeklySummaries()) {
            List<DailySummaryDto> dailySummaries = report.getDailySummariesByWeek().get(weeklySummary.getWorkWeek().getId());
            if (dailySummaries != null) {
                for (DailySummaryDto daily : dailySummaries) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(daily.getDate().toString());
                    row.createCell(1).setCellValue(daily.getWorkLocationName() != null ? daily.getWorkLocationName() : "N/A");
                    row.createCell(2).setCellValue(daily.getClockInTime() != null ? daily.getClockInTime().toString().substring(0, 5) : "-");
                    row.createCell(3).setCellValue(daily.getStartLunchTime() != null ? daily.getStartLunchTime().toString().substring(0, 5) : "-");
                    row.createCell(4).setCellValue(daily.getEndLunchTime() != null ? daily.getEndLunchTime().toString().substring(0, 5) : "-");
                    row.createCell(5).setCellValue(daily.getClockOutTime() != null ? daily.getClockOutTime().toString().substring(0, 5) : "-");
                    row.createCell(6).setCellValue(daily.getTotalHours());
                    row.createCell(7).setCellValue(daily.getDailyRate().doubleValue());
                    row.createCell(8).setCellValue(daily.getTotalPay().doubleValue());
                }
            }

            // --- SECCIÓN CORREGIDA: AÑADIMOS FILA DE RESUMEN SEMANAL ---
            Row summaryRow = sheet.createRow(rowIdx++);
            summaryRow.createCell(5).setCellValue("Resumen Semana:");
            summaryRow.createCell(6).setCellValue(weeklySummary.getTotalHours());
            summaryRow.createCell(8).setCellValue(weeklySummary.getTotalPay().doubleValue());

            // Fila en blanco para separar semanas
            sheet.createRow(rowIdx++);
        }

        // Auto-ajustar el ancho de las columnas
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        workbook.write(out);
        workbook.close();
        return new ByteArrayInputStream(out.toByteArray());
    }
}