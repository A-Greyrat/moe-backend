package com.abdecd.moebackend.business.service.report;

import com.abdecd.moebackend.business.dao.entity.PlainUserDetail;
import com.abdecd.moebackend.business.dao.entity.Report;
import com.abdecd.moebackend.business.dao.entity.UserComment;
import com.abdecd.moebackend.business.dao.entity.Video;
import com.abdecd.moebackend.business.dao.mapper.PlainUserDetailMapper;
import com.abdecd.moebackend.business.dao.mapper.ReportMapper;
import com.abdecd.moebackend.business.dao.mapper.UserCommentMapper;
import com.abdecd.moebackend.business.dao.mapper.VideoMapper;
import com.abdecd.moebackend.business.exceptionhandler.BaseException;
import com.abdecd.moebackend.business.pojo.dto.report.AddReportDTO;
import com.abdecd.moebackend.business.pojo.vo.comment.UserCommentVO;
import com.abdecd.moebackend.business.pojo.vo.comment.UserCommentVOBasic;
import com.abdecd.moebackend.business.pojo.vo.plainuser.UploaderVO;
import com.abdecd.moebackend.business.pojo.vo.report.ReportCommentTotalVO;
import com.abdecd.moebackend.business.pojo.vo.report.ReportCommentVO;
import com.abdecd.moebackend.business.pojo.vo.report.ReportVideoTotalVO;
import com.abdecd.moebackend.business.pojo.vo.report.ReportVideoVO;
import com.abdecd.moebackend.business.service.video.VideoService;
import com.abdecd.moebackend.business.tokenLogin.common.UserContext;
import com.abdecd.moebackend.common.constant.MessageConstant;
import com.abdecd.moebackend.common.constant.StatusConstant;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private ReportMapper reportMapper;

    @Resource
    private PlainUserDetailMapper plainUserDetailMapper;

    @Resource
    private VideoMapper videoMapper;

    @Autowired
    private UserCommentMapper userCommentMapper;

    @Autowired
    private VideoService videoService;

    @Override
    public Long addReport(AddReportDTO addReportDTO) {
        if (addReportDTO.getType() == Report.Type.VIDEO && videoService.getVideoBase(addReportDTO.getTargetId()) == null)
            throw new BaseException(MessageConstant.ARG_ERROR);
        if (addReportDTO.getType() == Report.Type.COMMENT && userCommentMapper.selectById(addReportDTO.getTargetId()) == null)
            throw new BaseException(MessageConstant.ARG_ERROR);
        Report report = new Report();
        BeanUtils.copyProperties(addReportDTO, report);
        report.setUserId(UserContext.getUserId());
        report.setCreateTime(LocalDateTime.now());
        report.setStatus(Integer.valueOf(StatusConstant.ENABLE)); // 设置初始状态为等待处理

        reportMapper.insert(report);
        return report.getId();
    }

    @Override
    public void auditReport(Long id) {
        reportMapper.auditReport(id);
    }

    @Override
    public ReportVideoTotalVO getReportVideoVO(Integer page, Integer pageSize) {
        ReportVideoTotalVO reportVideoTotalVO = new ReportVideoTotalVO();

        ArrayList<Report> reports = reportMapper.getVideoReportPage((page - 1) * pageSize, pageSize);
        var count = reportMapper.countByType(0);
        reportVideoTotalVO.setTotal(count);
        reportVideoTotalVO.setRecords(new ArrayList<>());

        for (Report report : reports) {
            ReportVideoVO reportVideoVO = new ReportVideoVO()
                    .setId(Math.toIntExact(report.getId()))
                    .setReason(report.getReason())
                    .setCreateTime(String.valueOf(report.getCreateTime()))
                    .setStatus(report.getStatus());

            PlainUserDetail plainUserDetail = plainUserDetailMapper.selectByUid(report.getUserId());
            reportVideoVO.setUser(
                    new UploaderVO()
                            .setId(plainUserDetail.getUserId())
                            .setAvatar(plainUserDetail.getAvatar())
                            .setNickname(plainUserDetail.getNickname())
            );

            Video video = videoMapper.selectById(report.getTargetId());
            if (video == null) continue;
            reportVideoVO.setVideo(videoService.getVideoForce(video.getId()));

            reportVideoTotalVO.getRecords().add(reportVideoVO);
        }
        return reportVideoTotalVO;
    }

    @Override
    public ReportCommentTotalVO getReportCommentVO(Integer page, Integer pageSize) {
        ReportCommentTotalVO reportCommentTotalVO = new ReportCommentTotalVO();

        ArrayList<Report> comments = reportMapper.getCommentReportPage((page - 1) * pageSize, pageSize);
        var count = reportMapper.countByType(1);

        reportCommentTotalVO.setTotal(count);
        reportCommentTotalVO.setRecords(new ArrayList<>());

        for (Report report : comments) {
            ReportCommentVO reportCommentVO = new ReportCommentVO()
                    .setId(Math.toIntExact(report.getId()))
                    .setReason(report.getReason())
                    .setCreateTime(String.valueOf(report.getCreateTime()))
                    .setStatus(report.getStatus());

            PlainUserDetail plainUserDetail = plainUserDetailMapper.selectByUid(report.getUserId());

            reportCommentVO.setUserDetail(new UploaderVO()
                    .setId(plainUserDetail.getUserId())
                    .setAvatar(plainUserDetail.getAvatar())
                    .setNickname(plainUserDetail.getNickname())
            );

            UserComment userComment = userCommentMapper.selectById(report.getTargetId());

            UserCommentVO userCommentVO = new UserCommentVO();

            PlainUserDetail user = plainUserDetailMapper.selectByUid(userComment.getUserId());
            if (user == null) continue;

            userCommentVO.setTimestamp(userComment.getTimestamp());
            userCommentVO.setId(userComment.getId());
            userCommentVO.setContent(userComment.getContent());
            userCommentVO.setToId(userComment.getToId());

            UserCommentVOBasic.UserDetail userDetail1 = new UserCommentVOBasic.UserDetail();
            userDetail1.setAvatar(user.getAvatar());
            userDetail1.setNickname(user.getNickname());
            userDetail1.setId(Math.toIntExact(user.getUserId()));
            userCommentVO.setUserDetail(userDetail1);

            if (userComment.getToId() != -1L) {
                var comment2 = userCommentMapper.selectById(userComment.getToId());
                if (comment2 != null) {
                    PlainUserDetail touser = plainUserDetailMapper.selectByUid(comment2.getUserId());

                    UserCommentVOBasic.UserDetail userDetail2 = new UserCommentVOBasic.UserDetail();
                    userDetail2.setAvatar(touser.getAvatar());
                    userDetail2.setNickname(touser.getNickname());
                    userDetail2.setId(Math.toIntExact(touser.getUserId()));
                    userCommentVO.setToUserDetail(userDetail2);
                }
            }

            reportCommentVO.setComment(userCommentVO);
            reportCommentTotalVO.getRecords().add(reportCommentVO);
        }

        return reportCommentTotalVO;
    }

    @Override
    public void deleteReport(Long[] ids) {
        reportMapper.deleteBatchIds(Arrays.asList(ids));
    }
}

