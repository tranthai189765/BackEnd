package com.example.demo.controller.export;

import com.example.demo.entity.Contribution;
import com.example.demo.entity.ContributionType;
import com.example.demo.repository.ContributionRepository;
import com.example.demo.repository.ContributionTypeRepository;
import com.example.demo.util.ExcelExportUtil;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/export")
@PreAuthorize("hasRole('ADMIN')")
public class ContributionExcelExportController {

    @Autowired
    private ContributionRepository contributionRepository;
    
    @Autowired
    private ContributionTypeRepository contributionTypeRepository;

    @GetMapping("/contributions")
    public void exportContributionsToExcel(HttpServletResponse response) throws IOException {
        List<Contribution> contributions = contributionRepository.findAll();
        
        Map<Long, String> contributionTypeNames = contributionTypeRepository.findAll()
                .stream()
                .collect(Collectors.toMap(
                        ContributionType::getId,
                        ContributionType::getName
                ));

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String filename = ExcelExportUtil.generateExcelFilename("Danh_sach_dong_gop");
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);
//        response.setHeader("Content-Disposition", "attachment; filename=.xlsx");

        Workbook workbook = ExcelExportUtil.createWorkbook();
        Sheet sheet = ExcelExportUtil.createSheet(workbook, "Danh sách đóng góp");

        ExcelExportUtil.createReportTitleRow(sheet, "DANH SÁCH KHOẢN ĐÓNG GÓP", 9);
        ExcelExportUtil.createExportInfoRow(sheet, 9);

        List<String> headers = Arrays.asList(
                "ID", "Loại đóng góp", "Tiêu đề", "Mô tả", 
                "Ngày bắt đầu", "Ngày kết thúc", "Số tiền mục tiêu", 
                "Ngày tạo", "Trạng thái"
        );

        ExcelExportUtil.createHeaderRow(sheet, headers, 3);

        List<List<Object>> data = new ArrayList<>();

        for (Contribution contribution : contributions) {
            List<Object> rowData = new ArrayList<>();
            rowData.add(contribution.getId());
            rowData.add(contributionTypeNames.getOrDefault(contribution.getContributionTypeId(), "Không xác định"));
            rowData.add(contribution.getTitle());
            rowData.add(contribution.getDescription());
            rowData.add(contribution.getStartDate());
            rowData.add(contribution.getEndDate());
            rowData.add(contribution.getTargetAmount());
            rowData.add(contribution.getCreatedAt());
            rowData.add(formatContributionStatus(contribution.getStatus() != null ? contribution.getStatus().name() : ""));

            data.add(rowData);
        }

        ExcelExportUtil.createDataRows(sheet, data, 4);

        ExcelExportUtil.autoSizeColumns(sheet, headers.size());

        workbook.write(response.getOutputStream());
        workbook.close();
    }

    private String formatContributionStatus(String status) {
        if (status == null) return "";
        
        switch (status) {
            case "ACTIVE":
                return "Đang hoạt động";
            case "CLOSED":
                return "Đã kết thúc";
            case "CANCELED":
                return "Đã hủy";
            default:
                return status;
        }
    }
}