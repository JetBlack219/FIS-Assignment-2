package com.jasper.report;

import com.jasper.report.entity.Employee;
import com.jasper.report.repository.EmployeeRepository;
import com.jasper.report.service.JasperReportService;
import net.sf.jasperreports.engine.JRException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.List;

@SpringBootApplication
@RestController
public class ReportApplication {

	@Autowired
	private EmployeeRepository employeeRepository;

	@Autowired
	private JasperReportService jasperReportService;

	@GetMapping("/getEmployees")
	public List<Employee> getEmployees() {
		return employeeRepository.findAll();
	}

	@GetMapping("/report/{format}")
	public ResponseEntity<byte[]> generateReport(@PathVariable String format) throws JRException, FileNotFoundException, SQLException {
		// Generate report as byte array
		byte[] reportBytes = jasperReportService.exportReport(format);

		// Set up response headers based on format
		HttpHeaders headers = new HttpHeaders();
		String filename = "employees." + format.toLowerCase();

		switch (format.toLowerCase()) {
			case "pdf":
				headers.setContentType(MediaType.APPLICATION_PDF);
				headers.setContentDispositionFormData("inline", filename); // "inline" for preview, "attachment" for download
				break;
			case "xlsx":
				headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
				headers.setContentDispositionFormData("attachment", filename); // Excel files are better downloaded
				break;
			default:
				return ResponseEntity.badRequest().build();
		}

		return new ResponseEntity<>(reportBytes, headers, HttpStatus.OK);
	}

	// Optional: Add a separate endpoint for downloading the report
	@GetMapping("/report/{format}/download")
	public ResponseEntity<byte[]> downloadReport(@PathVariable String format) throws JRException, FileNotFoundException, SQLException {
		// Generate report as byte array
		byte[] reportBytes = jasperReportService.exportReport(format);

		// Set up response headers for download
		HttpHeaders headers = new HttpHeaders();
		String filename = "employees." + format.toLowerCase();

		switch (format.toLowerCase()) {
			case "pdf":
				headers.setContentType(MediaType.APPLICATION_PDF);
				headers.setContentDispositionFormData("attachment", filename); // Forces download
				break;
			case "xlsx":
				headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
				headers.setContentDispositionFormData("attachment", filename);
				break;
			default:
				return ResponseEntity.badRequest().build();
		}

		return new ResponseEntity<>(reportBytes, headers, HttpStatus.OK);
	}

	public static void main(String[] args) {
		SpringApplication.run(ReportApplication.class, args);
	}
}