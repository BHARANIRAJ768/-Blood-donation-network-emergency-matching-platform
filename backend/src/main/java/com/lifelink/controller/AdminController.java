package com.lifelink.controller;

import com.lifelink.dto.AdminStatsResponse;
import com.lifelink.dto.AdminUserResponse;
import com.lifelink.dto.BloodRequestResponse;
import com.lifelink.dto.HistoryResponse;
import com.lifelink.dto.PageResponse;
import com.lifelink.service.AdminService;
import com.lifelink.service.HistoryService;
import com.lifelink.util.QueryParsers;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final HistoryService historyService;

    public AdminController(AdminService adminService, HistoryService historyService) {
        this.adminService = adminService;
        this.historyService = historyService;
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> stats() {
        return ResponseEntity.ok(adminService.getStats());
    }

    @GetMapping("/users")
    public ResponseEntity<PageResponse<AdminUserResponse>> users(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String page,
            @RequestParam(required = false) String size) {
        return ResponseEntity.ok(adminService.listUsers(
                QueryParsers.role(role),
                search,
                QueryParsers.page(page),
                QueryParsers.size(size, 10, 50)));
    }

    @GetMapping("/requests")
    public ResponseEntity<PageResponse<BloodRequestResponse>> requests(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String page,
            @RequestParam(required = false) String size) {
        return ResponseEntity.ok(adminService.listRequests(
                QueryParsers.status(status),
                search,
                QueryParsers.page(page),
                QueryParsers.size(size, 10, 50)));
    }

    @GetMapping("/history")
    public ResponseEntity<PageResponse<HistoryResponse>> history(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String page,
            @RequestParam(required = false) String size) {
        return ResponseEntity.ok(historyService.getHistory(
                QueryParsers.status(status),
                search,
                QueryParsers.page(page),
                QueryParsers.size(size, 10, 50)));
    }
}
