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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.apache.poi.ss.usermodel.*;

import java.awt.*;
import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class FileExportService {

    private static final Logger log = LoggerFactory.getLogger(FileExportService.class);
    private static final DateTimeFormatter US_DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    // --- SHARED METHOD: Header with logo + company info ---
    private void addCompanyHeader(Document document, String logoUrl, String companyName, String companyAddress, String companyPhoneNumber) throws DocumentException {
        // Narrow logo column + left-aligned info = company info sits close to logo
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{1, 6});
        headerTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        // Logo from Cloudinary URL (right-aligned in its cell so it sits next to info)
        try {
            if (logoUrl != null && !logoUrl.isBlank()) {
                Image logo = Image.getInstance(new URL(logoUrl));
                logo.scaleToFit(80, 80);
                PdfPCell logoCell = new PdfPCell(logo);
                logoCell.setBorder(Rectangle.NO_BORDER);
                logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                logoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                headerTable.addCell(logoCell);
            } else {
                PdfPCell placeholderCell = new PdfPCell(new Phrase(""));
                placeholderCell.setBorder(Rectangle.NO_BORDER);
                headerTable.addCell(placeholderCell);
            }
        } catch (Exception e) {
            log.warn("Could not load company logo: {}", e.getMessage());
            PdfPCell placeholderCell = new PdfPCell(new Phrase(""));
            placeholderCell.setBorder(Rectangle.NO_BORDER);
            headerTable.addCell(placeholderCell);
        }

        // Company info — left-aligned, vertically centered, sits right next to the logo
        PdfPCell companyInfoCell = new PdfPCell();
        companyInfoCell.setBorder(Rectangle.NO_BORDER);
        companyInfoCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        companyInfoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        companyInfoCell.setPaddingLeft(8f);
        companyInfoCell.addElement(new Phrase(
                companyName != null ? companyName : "",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
        if (companyAddress != null) {
            companyInfoCell.addElement(new Phrase(companyAddress));
        }
        if (companyPhoneNumber != null) {
            companyInfoCell.addElement(new Phrase("Phone: " + companyPhoneNumber));
        }
        headerTable.addCell(companyInfoCell);

        document.add(headerTable);
        document.add(Chunk.NEWLINE);
    }

    // Helper to build centered cells for table bodies
    private PdfPCell centeredCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4f);
        return cell;
    }

    public ByteArrayInputStream generateConsolidatedPdf(ConsolidatedPayrollReportDto report) throws DocumentException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, out);

        document.open();

        // Header with logo + company info
        addCompanyHeader(document, report.getCompanyLogoUrl(),
                report.getCompanyName(), report.getCompanyAddress(), report.getCompanyPhoneNumber());

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Paragraph title = new Paragraph("Consolidated Payroll Report", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Paragraph period = new Paragraph("Period: " + report.getStartDate().format(US_DATE_FORMAT) + " to " + report.getEndDate().format(US_DATE_FORMAT));
        period.setAlignment(Element.ALIGN_CENTER);
        document.add(period);
        document.add(Chunk.NEWLINE);

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        String[] headers = {"Worker", "Regular Hrs", "Overtime Hrs", "Total Hrs", "Total Pay"};

        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            table.addCell(cell);
        }

        Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
        for (ConsolidatedPayrollEntryDto entry : report.getEntries()) {
            table.addCell(centeredCell(entry.getWorkerName(), bodyFont));
            table.addCell(centeredCell(String.format("%.2f", entry.getTotalRegularHours()), bodyFont));
            table.addCell(centeredCell(String.format("%.2f", entry.getTotalOvertimeHours()), bodyFont));
            table.addCell(centeredCell(String.format("%.2f", entry.getTotalHours()), bodyFont));
            table.addCell(centeredCell(String.format("$%.2f", entry.getTotalPay()), bodyFont));
        }

        document.add(table);

        document.add(Chunk.NEWLINE);
        Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
        Paragraph total = new Paragraph("Grand Total: $" + report.getGrandTotalPay(), totalFont);
        total.setAlignment(Element.ALIGN_CENTER);
        document.add(total);

        document.close();
        return new ByteArrayInputStream(out.toByteArray());
    }

    public ByteArrayInputStream generateConsolidatedExcel(ConsolidatedPayrollReportDto report) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XSSFSheet sheet = workbook.createSheet("Consolidated Payroll");

        String[] headers = {"Worker", "Regular Hrs", "Overtime Hrs", "Total Hrs", "Total Pay"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
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
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, out);
        document.open();

        // Header with logo + company info (from Cloudinary)
        addCompanyHeader(document, report.getCompanyLogoUrl(),
                report.getCompanyName(), report.getCompanyAddress(), report.getCompanyPhoneNumber());

        // Report Title
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Paragraph title = new Paragraph("Detailed Payroll Report", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Paragraph subtitle = new Paragraph("Worker: " + report.getWorkerName() +
                " | Period: " + report.getStartDate().format(US_DATE_FORMAT) + " to " + report.getEndDate().format(US_DATE_FORMAT) +
                " | Generated: " + LocalDate.now().format(US_DATE_FORMAT));
        subtitle.setAlignment(Element.ALIGN_CENTER);
        document.add(subtitle);
        document.add(Chunk.NEWLINE);

        // Weekly hours tables
        for (WeeklyPaySummaryDto weeklySummary : report.getWeeklySummaries()) {
            Font weekFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Paragraph weekHeader = new Paragraph("Week: " + weeklySummary.getWorkWeek().getStartDate().format(US_DATE_FORMAT) + " - " + weeklySummary.getWorkWeek().getEndDate().format(US_DATE_FORMAT), weekFont);
            weekHeader.setAlignment(Element.ALIGN_CENTER);
            document.add(weekHeader);

            PdfPTable table = new PdfPTable(9);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);
            table.setWidths(new float[]{2.5f, 2f, 1f, 1.5f, 1.5f, 1f, 1.5f, 1f, 1.5f});

            String[] headers = {"Date", "Workplace", "Clock In", "Lunch Start", "Lunch End", "Clock Out", "Hours", "Rate", "Daily Total"};
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(Color.LIGHT_GRAY);
                table.addCell(cell);
            }

            List<DailySummaryDto> dailySummaries = report.getDailySummariesByWeek().get(weeklySummary.getWorkWeek().getId());
            if (dailySummaries != null) {
                Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
                for (DailySummaryDto daily : dailySummaries) {
                    table.addCell(centeredCell(daily.getDate().format(US_DATE_FORMAT), bodyFont));
                    table.addCell(centeredCell(daily.getWorkLocationName() != null ? daily.getWorkLocationName() : "N/A", bodyFont));
                    table.addCell(centeredCell(daily.getClockInTime() != null ? daily.getClockInTime().toString().substring(0, 5) : "-", bodyFont));
                    table.addCell(centeredCell(daily.getStartLunchTime() != null ? daily.getStartLunchTime().toString().substring(0, 5) : "-", bodyFont));
                    table.addCell(centeredCell(daily.getEndLunchTime() != null ? daily.getEndLunchTime().toString().substring(0, 5) : "-", bodyFont));
                    table.addCell(centeredCell(daily.getClockOutTime() != null ? daily.getClockOutTime().toString().substring(0, 5) : "-", bodyFont));
                    table.addCell(centeredCell(String.format("%.2f", daily.getTotalHours()), bodyFont));
                    table.addCell(centeredCell(String.format("$%.2f", daily.getDailyRate()), bodyFont));
                    table.addCell(centeredCell(String.format("$%.2f", daily.getTotalPay()), bodyFont));
                }
            }
            document.add(table);

            // Weekly Summary
            PdfPTable summaryTable = new PdfPTable(4);
            summaryTable.setWidthPercentage(100);
            summaryTable.setSpacingBefore(5);
            summaryTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);
            summaryTable.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
            summaryTable.addCell(new Phrase("Regular Hrs: " + String.format("%.2f", weeklySummary.getRegularHours())));
            summaryTable.addCell(new Phrase("Overtime Hrs: " + String.format("%.2f", weeklySummary.getOvertimeHours())));
            summaryTable.addCell(new Phrase("Total Hrs: " + String.format("%.2f", weeklySummary.getTotalHours())));
            summaryTable.addCell(new Phrase("Weekly Pay: " + String.format("$%.2f", weeklySummary.getTotalPay()), FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
            document.add(summaryTable);
            document.add(Chunk.NEWLINE);
        }

        // Signature footer
        document.add(Chunk.NEWLINE);
        document.add(Chunk.NEWLINE);

        PdfPTable signatureTable = new PdfPTable(2);
        signatureTable.setWidthPercentage(80);
        signatureTable.setHorizontalAlignment(Element.ALIGN_CENTER);
        signatureTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);

        PdfPCell elaboratedCell = new PdfPCell(new Paragraph("\n\n\n______________________________\nPrepared by"));
        elaboratedCell.setBorder(Rectangle.NO_BORDER);
        elaboratedCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        signatureTable.addCell(elaboratedCell);

        PdfPCell receivedCell = new PdfPCell(new Paragraph("\n\n\n______________________________\nReceived by (Worker Signature)"));
        receivedCell.setBorder(Rectangle.NO_BORDER);
        receivedCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        signatureTable.addCell(receivedCell);

        document.add(signatureTable);

        document.close();
        return new ByteArrayInputStream(out.toByteArray());
    }

    public ByteArrayInputStream generateDetailedExcel(DetailedPayrollReportDto report) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XSSFSheet sheet = workbook.createSheet("Detailed Report");

        CellStyle headerStyle = workbook.createCellStyle();
        XSSFFont headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        String[] headers = {"Date", "Workplace", "Clock In", "Lunch Start", "Lunch End", "Clock Out", "Hours Worked", "Rate", "Daily Total"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIdx = 1;
        for (WeeklyPaySummaryDto weeklySummary : report.getWeeklySummaries()) {
            List<DailySummaryDto> dailySummaries = report.getDailySummariesByWeek().get(weeklySummary.getWorkWeek().getId());
            if (dailySummaries != null) {
                for (DailySummaryDto daily : dailySummaries) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(daily.getDate().format(US_DATE_FORMAT));
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

            Row summaryRow = sheet.createRow(rowIdx++);
            summaryRow.createCell(5).setCellValue("Weekly Summary:");
            summaryRow.createCell(6).setCellValue(weeklySummary.getTotalHours());
            summaryRow.createCell(8).setCellValue(weeklySummary.getTotalPay().doubleValue());

            sheet.createRow(rowIdx++);
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        workbook.write(out);
        workbook.close();
        return new ByteArrayInputStream(out.toByteArray());
    }
}
