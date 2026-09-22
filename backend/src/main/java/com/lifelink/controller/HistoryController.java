package com.lifelink.controller;

import com.lifelink.dto.HistoryResponse;
import com.lifelink.dto.PageResponse;
import com.lifelink.security.CurrentUserService;
import com.lifelink.service.HistoryService;
import com.lifelink.util.QueryParsers;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/history")
public class HistoryController {

    private final HistoryService historyService;
    private final CurrentUserService currentUserService;

    public HistoryController(HistoryService historyService, CurrentUserService currentUserService) {
        this.historyService = historyService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<HistoryResponse>> getHistory(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String page,
            @RequestParam(required = false) String size) {
        return ResponseEntity.ok(historyService.getMyHistory(
                currentUserService.getCurrentUserId(),
                QueryParsers.status(status),
                search,
                QueryParsers.page(page),
                QueryParsers.size(size, 10, 50)));
    }
}
