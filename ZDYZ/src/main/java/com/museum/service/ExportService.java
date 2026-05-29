package com.museum.service;

import com.museum.entity.Identity;
import com.museum.entity.Join;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

public interface ExportService {
    void exportJoinRecords(HttpServletResponse response, List<Join> joins);

    void exportBlacklist(HttpServletResponse response, List<Identity> identities);
}
