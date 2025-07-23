package com.bmt.MyApp.controllers;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.bmt.MyApp.models.AppUser;
import com.bmt.MyApp.models.Transactions;
import com.bmt.MyApp.models.Transactions.TransactionStatus;
import com.bmt.MyApp.repositories.AppUserRepository;
import com.bmt.MyApp.services.TransactionExcelService;
import com.bmt.MyApp.services.TransactionService;
import com.bmt.MyApp.services.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for transaction history, statistics, and Excel export.
 */
@Controller
public class TransactionController {

    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TransactionExcelService excelService;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private LogService logService;

    /**
     * Exports a single transaction to Excel.
     * @param id the transaction ID
     * @return the Excel file as a ResponseEntity
     */
    @GetMapping("/lichsugiaodich/export-single/{id}")
    public ResponseEntity<byte[]> exportSingleTransactionToExcel(@PathVariable Long id, Principal principal) {
        logger.info("Bắt đầu xuất Excel cho giao dịch ID: {}", id);
        try {
            Optional<Transactions> optionalTransaction = transactionService.getTransactionById(id);
            if (optionalTransaction.isEmpty()) {
                logger.warn("Không tìm thấy giao dịch với ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            Transactions transaction = optionalTransaction.get();
            logger.info("Tìm thấy giao dịch - VnpTxnRef: {}, User: {}, Amount: {}",
                       transaction.getVnpTxnRef(),
                       transaction.getUser().getEmail(),
                       transaction.getAmount());
            byte[] excelData = excelService.exportSingleTransactionToExcel(transaction);
            logger.info("Tạo file Excel thành công - Kích thước: {} bytes", excelData.length);
            String filename = "GiaoDich_" + transaction.getVnpTxnRef() + "_" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelData.length);
            logger.info("Xuất Excel đơn lẻ thành công - File: {}", filename);
            // Ghi log xuất excel
            String username = principal != null ? principal.getName() : (transaction.getUser() != null ? transaction.getUser().getEmail() : "unknown");
            logService.log(username, "Xuất Excel", "Xuất Excel giao dịch đơn lẻ: " + transaction.getVnpTxnRef());
            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Lỗi khi xuất Excel cho giao dịch ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Lỗi khi tạo file Excel: " + e.getMessage()).getBytes());
        }
    }

    /**
     * Exports user's transactions to Excel.
     */
    @GetMapping("/user_transactions/export")
    public ResponseEntity<byte[]> exportUserTransactionsToExcel(Principal principal) {
        String userEmail = principal.getName();
        logger.info("Bắt đầu xuất Excel giao dịch cho user: {}", userEmail);
        try {
            AppUser currentUser = appUserRepository.findByEmail(userEmail).orElse(null);
            if (currentUser == null) {
                logger.warn("User không tồn tại: {}", userEmail);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Người dùng không tồn tại".getBytes());
            }
            List<Transactions> transactions = transactionService.findAllByEmail(userEmail);
            logger.info("Tìm thấy {} giao dịch cho user: {}", transactions.size(), userEmail);
            byte[] excelData = excelService.exportTransactionsToExcel(transactions);
            logger.info("Tạo file Excel thành công cho user {} - Kích thước: {} bytes",
                       userEmail, excelData.length);
            String filename = "LichSuGiaoDich_" + currentUser.getFullName().replaceAll("\\s+", "_") + "_" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelData.length);
            logger.info("Xuất Excel giao dịch user thành công - File: {}, User: {}", filename, userEmail);
            // Ghi log xuất excel
            logService.log(userEmail, "Xuất Excel", "Xuất Excel lịch sử giao dịch cá nhân");
            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Lỗi khi xuất Excel giao dịch cho user {}: {}", userEmail, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Lỗi khi tạo file Excel: " + e.getMessage()).getBytes());
        }
    }

    /**
     * Exports a single user transaction to Excel.
     */
    @GetMapping("/user_transactions/export-single/{id}")
    public ResponseEntity<byte[]> exportSingleUserTransactionToExcel(@PathVariable Long id, Principal principal) {
        String userEmail = principal.getName();
        logger.info("Bắt đầu xuất Excel giao dịch đơn lẻ - ID: {}, User: {}", id, userEmail);
        try {
            Optional<Transactions> optionalTransaction = transactionService.getTransactionById(id);
            if (optionalTransaction.isEmpty()) {
                logger.warn("Không tìm thấy giao dịch với ID: {} cho user: {}", id, userEmail);
                return ResponseEntity.notFound().build();
            }
            Transactions transaction = optionalTransaction.get();
            if (!transaction.getUser().getEmail().equals(userEmail)) {
                logger.warn("User {} không có quyền truy cập giao dịch ID: {} (thuộc về user: {})",
                           userEmail, id, transaction.getUser().getEmail());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Bạn không có quyền truy cập giao dịch này".getBytes());
            }
            logger.info("Xác thực thành công - Giao dịch ID: {} thuộc về user: {}", id, userEmail);
            byte[] excelData = excelService.exportSingleTransactionToExcel(transaction);
            logger.info("Tạo file Excel thành công cho giao dịch ID: {} - Kích thước: {} bytes",
                       id, excelData.length);
            String filename = "GiaoDich_" + transaction.getVnpTxnRef() + "_" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelData.length);
            logger.info("Xuất Excel giao dịch đơn lẻ thành công - File: {}, User: {}", filename, userEmail);
            // Ghi log xuất excel
            logService.log(userEmail, "Xuất Excel", "Xuất Excel giao dịch đơn lẻ: " + transaction.getVnpTxnRef());
            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Lỗi khi xuất Excel giao dịch đơn lẻ ID {} cho user {}: {}",
                        id, userEmail, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Lỗi khi tạo file Excel: " + e.getMessage()).getBytes());
        }
    }

    /**
     * Displays the paginated, filterable transaction history.
     */
    @GetMapping("/lichsugiaodich")
    public String lichSuGiaoDich(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "10") int size,
                                 @RequestParam(defaultValue = "createdAt") String sortBy,
                                 @RequestParam(defaultValue = "desc") String sortDir,
                                 @RequestParam(required = false) String search,
                                 @RequestParam(required = false) String startDate,
                                 @RequestParam(required = false) String endDate,
                                 @RequestParam(required = false) String expireStartDate,
                                 @RequestParam(required = false) String expireEndDate,
                                 Model model) {

        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Transactions> transactionPage = transactionService.searchTransactions(search, startDate, endDate, expireStartDate, expireEndDate, pageable);

        model.addAttribute("transactions", transactionPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", transactionPage.getTotalPages());
        model.addAttribute("totalElements", transactionPage.getTotalElements());
        model.addAttribute("hasNext", transactionPage.hasNext());
        model.addAttribute("hasPrevious", transactionPage.hasPrevious());
        model.addAttribute("size", size);

        model.addAttribute("search", search);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("expireStartDate", expireStartDate);
        model.addAttribute("expireEndDate", expireEndDate);

        return "lichsugiaodich";
    }

    /**
     * Displays the transaction history for the current user.
     */
    @GetMapping("/user_transactions")
    public String lichSuGiaoDichCuaToi(@RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "10") int size,
                                        Principal principal,
                                        Model model) {
            String userEmail = principal.getName();
            
            // Lấy user từ database để đảm bảo tồn tại
            AppUser currentUser = appUserRepository.findByEmail(userEmail).orElse(null);
            if (currentUser == null) {
                return "redirect:/login";
            }

            Page<Transactions> transactionPage = transactionService.findByEmail(userEmail, PageRequest.of(page, size, Sort.by("createdAt").descending()));

            model.addAttribute("transactions", transactionPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", transactionPage.getTotalPages());
            model.addAttribute("totalElements", transactionPage.getTotalElements());
            model.addAttribute("hasNext", transactionPage.hasNext());
            model.addAttribute("hasPrevious", transactionPage.hasPrevious());
            model.addAttribute("size", size);

            return "user_transactions";
        }

    /**
     * Exports all filtered transactions to Excel.
     */
    @GetMapping("/lichsugiaodich/export")
    public ResponseEntity<byte[]> exportToExcel(@RequestParam(required = false) String search,
                                                @RequestParam(required = false) String startDate,
                                                @RequestParam(required = false) String endDate,
                                                @RequestParam(required = false) String expireStartDate,
                                                @RequestParam(required = false) String expireEndDate,
                                                Principal principal) {
        logger.info("Bắt đầu xuất Excel tất cả giao dịch với filter - Search: {}, StartDate: {}, EndDate: {}, ExpireStartDate: {}, ExpireEndDate: {}",
                   search, startDate, endDate, expireStartDate, expireEndDate);
        try {
            List<Transactions> transactions = transactionService.searchTransactionsForExport(search, startDate, endDate, expireStartDate, expireEndDate);
            logger.info("Tìm thấy {} giao dịch phù hợp với điều kiện filter", transactions.size());
            byte[] excelData = excelService.exportTransactionsToExcel(transactions);
            logger.info("Tạo file Excel thành công cho tất cả giao dịch - Kích thước: {} bytes", excelData.length);
            String filename = "LichSuGiaoDich_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelData.length);
            logger.info("Xuất Excel tất cả giao dịch thành công - File: {}, Số giao dịch: {}", filename, transactions.size());
            // Ghi log xuất excel
            String username = principal != null ? principal.getName() : "unknown";
            logService.log(username, "Xuất Excel", "Xuất Excel tất cả giao dịch");
            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Lỗi khi xuất Excel tất cả giao dịch với filter [Search: {}, StartDate: {}, EndDate: {}]: {}",
                        search, startDate, endDate, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Lỗi khi tạo file Excel: " + e.getMessage()).getBytes());
        }
    }

    /**
     * Displays the transaction statistics page.
     */
    @GetMapping("/thongkegiaodich")
    public String thongKeGiaoDich(@RequestParam(required = false) String search,
                                  @RequestParam(required = false) String startDate,
                                  @RequestParam(required = false) String endDate,
                                  @RequestParam(required = false) String year,
                                  @RequestParam(required = false) String month,
                                  Model model) {

        long tongGiaoDichThanhCong = transactionService.countTransactionsByStatus(TransactionStatus.SUCCESS);
        BigDecimal tongTienThanhCong = transactionService.sumAmountByStatus(TransactionStatus.SUCCESS);
        long tongGiaoDich = transactionService.getTotalTransactions();
        long giaoDichThatBai = transactionService.countTransactionsByStatus(TransactionStatus.FAILED);

        model.addAttribute("tongGiaoDich", tongGiaoDich);
        model.addAttribute("tongGiaoDichThanhCong", tongGiaoDichThanhCong);
        model.addAttribute("giaoDichThatBai", giaoDichThatBai);
        model.addAttribute("tongTienThanhCong", tongTienThanhCong);

        model.addAttribute("search", search);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("year", year);
        model.addAttribute("month", month);

        int currentYear = LocalDate.now().getYear();
        model.addAttribute("years", IntStream.rangeClosed(2020, currentYear + 1).boxed().toList());

        return "thongkegiaodich";
    }

    /**
     * Returns monthly statistics for charting.
     */
    @GetMapping("/api/thongke-theo-thang")
    @ResponseBody
    public Map<String, Object> thongKeTheoThang(@RequestParam int year) {
        Map<String, Object> response = new LinkedHashMap<>();
        Map<String, BigDecimal> revenueData = new LinkedHashMap<>();
        Map<String, Long> transactionData = new LinkedHashMap<>();

        for (int month = 1; month <= 12; month++) {
            YearMonth ym = YearMonth.of(year, month);
            LocalDate start = ym.atDay(1);
            LocalDate end = ym.atEndOfMonth();

            BigDecimal revenue = transactionService.sumByStatusAndDateRange(TransactionStatus.SUCCESS, start.atStartOfDay(), end.atTime(23, 59, 59));
            long count = transactionService.countByStatusAndDateRange(TransactionStatus.SUCCESS, start.atStartOfDay(), end.atTime(23, 59, 59));

            String monthKey = String.format("Tháng %02d", month);
            revenueData.put(monthKey, revenue != null ? revenue : BigDecimal.ZERO);
            transactionData.put(monthKey, count);
        }

        response.put("revenue", revenueData);
        response.put("transactions", transactionData);
        return response;
    }

    /**
     * Returns yearly statistics for charting.
     */
    @GetMapping("/api/thongke-theo-nam")
    @ResponseBody
    public Map<String, Object> thongKeTheoNam() {
        Map<String, Object> response = new LinkedHashMap<>();
        Map<String, BigDecimal> revenueData = new LinkedHashMap<>();
        Map<String, Long> transactionData = new LinkedHashMap<>();

        int currentYear = LocalDate.now().getYear();
        for (int year = currentYear - 3; year <= currentYear; year++) {
            LocalDate start = LocalDate.of(year, 1, 1);
            LocalDate end = LocalDate.of(year, 12, 31);

            BigDecimal revenue = transactionService.sumByStatusAndDateRange(TransactionStatus.SUCCESS, start.atStartOfDay(), end.atTime(23, 59, 59));
            long count = transactionService.countByStatusAndDateRange(TransactionStatus.SUCCESS, start.atStartOfDay(), end.atTime(23, 59, 59));

            revenueData.put(String.valueOf(year), revenue != null ? revenue : BigDecimal.ZERO);
            transactionData.put(String.valueOf(year), count);
        }

        response.put("revenue", revenueData);
        response.put("transactions", transactionData);
        return response;
    }

    /**
     * Returns custom statistics (daily or weekly) for a date range.
     */
    @GetMapping("/api/thongke-tuy-chinh")
    @ResponseBody
    public Map<String, Object> thongKeTuyChinhTheoNgay(@RequestParam String startDate,
                                                       @RequestParam String endDate) {
        Map<String, Object> response = new LinkedHashMap<>();
        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            long daysBetween = start.until(end).getDays();

            Map<String, BigDecimal> revenueData = new LinkedHashMap<>();
            Map<String, Long> transactionData = new LinkedHashMap<>();

            if (daysBetween <= 31) {
                LocalDate current = start;
                while (!current.isAfter(end)) {
                    BigDecimal revenue = transactionService.sumByStatusAndDateRange(TransactionStatus.SUCCESS, current.atStartOfDay(), current.atTime(23, 59, 59));
                    long count = transactionService.countByStatusAndDateRange(TransactionStatus.SUCCESS, current.atStartOfDay(), current.atTime(23, 59, 59));
                    revenueData.put(current.toString(), revenue != null ? revenue : BigDecimal.ZERO);
                    transactionData.put(current.toString(), count);
                    current = current.plusDays(1);
                }
            } else {
                LocalDate current = start;
                int weekNumber = 1;
                while (!current.isAfter(end)) {
                    LocalDate weekEnd = current.plusDays(6);
                    if (weekEnd.isAfter(end)) weekEnd = end;

                    BigDecimal revenue = transactionService.sumByStatusAndDateRange(TransactionStatus.SUCCESS, current.atStartOfDay(), weekEnd.atTime(23, 59, 59));
                    long count = transactionService.countByStatusAndDateRange(TransactionStatus.SUCCESS, current.atStartOfDay(), weekEnd.atTime(23, 59, 59));

                    String weekKey = String.format("Tuần %d", weekNumber++);
                    revenueData.put(weekKey, revenue != null ? revenue : BigDecimal.ZERO);
                    transactionData.put(weekKey, count);

                    current = weekEnd.plusDays(1);
                }
            }

            response.put("revenue", revenueData);
            response.put("transactions", transactionData);
            response.put("period", daysBetween <= 31 ? "daily" : "weekly");

        } catch (Exception e) {
            response.put("error", "Lỗi xử lý dữ liệu: " + e.getMessage());
        }
        return response;
    }

    /**
     * Returns summary statistics for a given period.
     */
    @GetMapping("/api/thongke-tong-hop")
    @ResponseBody
    public Map<String, Object> thongKeTongHop(@RequestParam(required = false) String startDate,
                                              @RequestParam(required = false) String endDate,
                                              @RequestParam(required = false) Integer year,
                                              @RequestParam(required = false) Integer month) {
        Map<String, Object> result = new LinkedHashMap<>();
        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;
        try {
            if (year != null && month != null) {
                YearMonth ym = YearMonth.of(year, month);
                startDateTime = ym.atDay(1).atStartOfDay();
                endDateTime = ym.atEndOfMonth().atTime(23, 59, 59);
            } else if (year != null) {
                startDateTime = LocalDate.of(year, 1, 1).atStartOfDay();
                endDateTime = LocalDate.of(year, 12, 31).atTime(23, 59, 59);
            } else if (startDate != null && endDate != null) {
                startDateTime = LocalDate.parse(startDate).atStartOfDay();
                endDateTime = LocalDate.parse(endDate).atTime(23, 59, 59);
            }

            if (startDateTime != null && endDateTime != null) {
                long successCount = transactionService.countByStatusAndDateRange(TransactionStatus.SUCCESS, startDateTime, endDateTime);
                long failedCount = transactionService.countByStatusAndDateRange(TransactionStatus.FAILED, startDateTime, endDateTime);
                BigDecimal totalRevenue = transactionService.sumByStatusAndDateRange(TransactionStatus.SUCCESS, startDateTime, endDateTime);

                result.put("successCount", successCount);
                result.put("failedCount", failedCount);
                result.put("totalCount", successCount + failedCount);
                result.put("totalRevenue", totalRevenue != null ? totalRevenue : BigDecimal.ZERO);
                result.put("averageRevenue", successCount > 0 && totalRevenue != null ? totalRevenue.divide(BigDecimal.valueOf(successCount), 2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO);
            } else {
                long totalSuccess = transactionService.countTransactionsByStatus(TransactionStatus.SUCCESS);
                long totalFailed = transactionService.countTransactionsByStatus(TransactionStatus.FAILED);
                BigDecimal totalRevenue = transactionService.sumAmountByStatus(TransactionStatus.SUCCESS);

                result.put("successCount", totalSuccess);
                result.put("failedCount", totalFailed);
                result.put("totalCount", totalSuccess + totalFailed);
                result.put("totalRevenue", totalRevenue != null ? totalRevenue : BigDecimal.ZERO);
                result.put("averageRevenue", totalSuccess > 0 && totalRevenue != null ? totalRevenue.divide(BigDecimal.valueOf(totalSuccess), 2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO);
            }
        } catch (Exception e) {
            result.put("error", "Lỗi xử lý dữ liệu: " + e.getMessage());
        }
        return result;
    }
}
