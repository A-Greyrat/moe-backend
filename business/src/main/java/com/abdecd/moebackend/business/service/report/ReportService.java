package com.abdecd.moebackend.business.service.report;


import com.abdecd.moebackend.business.pojo.dto.report.AddReportDTO;

import com.abdecd.moebackend.business.pojo.vo.report.ReportCommentTotalVO;
import com.abdecd.moebackend.business.pojo.vo.report.ReportVideoTotalVO;

public interface ReportService {
    Long addReport(AddReportDTO addReportDTO);

    void auditReport(Long id);

    ReportVideoTotalVO getReportVideoVO(Integer page, Integer pageSize);

    ReportCommentTotalVO getReportCommentVO(Integer page, Integer pageSize);

    void deleteReport(Long[] ids);
}
