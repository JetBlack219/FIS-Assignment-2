package com.jasper.report.service;

import net.sf.jasperreports.engine.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@Service
public class JasperReportService {

    @Autowired
    private DataSource dataSource;

    public byte[] exportReport(String reportFormat) throws FileNotFoundException, JRException, SQLException {
        // Define template path
        String templatePath = "templates/Employees.jrxml";

        // Load and compile the JRXML file from classpath
        JasperReport jasperReport;
        try {
            ClassPathResource resource = new ClassPathResource(templatePath);
            if (!resource.exists()) {
                throw new FileNotFoundException("JRXML template not found at: " + templatePath);
            }

            // Load from input stream instead of file path
            try (InputStream inputStream = resource.getInputStream()) {
                jasperReport = JasperCompileManager.compileReport(inputStream);
            }
        } catch (Exception e) {
            throw new FileNotFoundException("Failed to load JRXML template: " + e.getMessage());
        }

        try {
            // Create parameters
            Map<String, Object> parameters = new HashMap<>();

            // Get database connection
            try (Connection connection = dataSource.getConnection()) {
                // Fill the report using the database connection
                JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, connection);

                // Create ByteArrayOutputStream for output
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

                // Export based on format
                switch (reportFormat.toLowerCase()) {
                    case "xlsx":
                        // Excel export using JRXlsxExporter
                        net.sf.jasperreports.export.SimpleExporterInput exporterInput =
                                new net.sf.jasperreports.export.SimpleExporterInput(jasperPrint);
                        net.sf.jasperreports.export.SimpleOutputStreamExporterOutput exporterOutput =
                                new net.sf.jasperreports.export.SimpleOutputStreamExporterOutput(outputStream);

                        net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter xlsxExporter =
                                new net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter();
                        xlsxExporter.setExporterInput(exporterInput);
                        xlsxExporter.setExporterOutput(exporterOutput);

                        // Configure Excel export settings
                        net.sf.jasperreports.export.SimpleXlsxReportConfiguration xlsxConfig =
                                new net.sf.jasperreports.export.SimpleXlsxReportConfiguration();
                        xlsxConfig.setOnePagePerSheet(false);
                        xlsxConfig.setDetectCellType(true);
                        xlsxConfig.setCollapseRowSpan(false);
                        xlsxConfig.setWhitePageBackground(false);
                        xlsxExporter.setConfiguration(xlsxConfig);

                        xlsxExporter.exportReport();
                        break;
                    case "pdf":
                        JasperExportManager.exportReportToPdfStream(jasperPrint, outputStream);
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported report format: " + reportFormat + ". Supported formats: xlsx, pdf");
                }

                return outputStream.toByteArray();

            } catch (SQLException e) {
                throw new SQLException("Failed to establish database connection: " + e.getMessage(), e);
            }
        } catch (JRException e) {
            throw new JRException("Failed to compile or generate report: " + e.getMessage(), e);
        }
    }
}