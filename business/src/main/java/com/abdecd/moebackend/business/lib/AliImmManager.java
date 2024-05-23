package com.abdecd.moebackend.business.lib;

import com.abdecd.moebackend.business.common.property.AliProperties;
import com.aliyun.tea.TeaException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AliImmManager {
    @Autowired
    AliProperties aliProperties;

    private static final Map<String, Integer> fontSizeMap = Map.of("640x360", 15, "1280x720", 30, "1920x1080", 45);

    /**
     * 使用AK&SK初始化账号Client
     * @return Client
     */
    private com.aliyun.imm20200930.Client createClient() {
        // 工程代码泄露可能会导致 AccessKey 泄露，并威胁账号下所有资源的安全性。以下代码示例仅供参考。
        // 建议使用更安全的 STS 方式，更多鉴权访问方式请参见：https://help.aliyun.com/document_detail/378657.html。
        com.aliyun.teaopenapi.models.Config config = new com.aliyun.teaopenapi.models.Config()
                // 必填，请确保代码运行环境设置了环境变量 ALIBABA_CLOUD_ACCESS_KEY_ID。
                .setAccessKeyId(aliProperties.getAccessKeyId())
                // 必填，请确保代码运行环境设置了环境变量 ALIBABA_CLOUD_ACCESS_KEY_SECRET。
                .setAccessKeySecret(aliProperties.getAccessKeySecret());
        // Endpoint 请参考 https://api.aliyun.com/product/imm
        config.endpoint = "imm.cn-hangzhou.aliyuncs.com";
        try {
            return new com.aliyun.imm20200930.Client(config);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 视频转码
     * @param username 用户名
     * @param originPath 原始视频路径 如 video/2022/01/01/1.mp4
     * @param targetPath 目标视频路径 如 video/2022/01/01/video
     * @param widthAndHeight 视频宽高 如 1920*1080
     * @return 阿里云任务id
     */
    public String transformVideo(String username, String originPath, String targetPath, String widthAndHeight) {
        com.aliyun.imm20200930.Client client = createClient();
        com.aliyun.imm20200930.models.TargetVideo.TargetVideoFilterVideoWatermarks targets0TargetVideoFilterVideoWatermarks0 = new com.aliyun.imm20200930.models.TargetVideo.TargetVideoFilterVideoWatermarks()
//                .setType("file")
//                .setDx(10F)
//                .setDy(10F)
//                .setHeight(0.10F)
//                .setURI("oss://" + aliProperties.getBucketName() + "/" + aliProperties.getWatermark());
                .setType("text")
                .setDx(Float.valueOf(fontSizeMap.get(widthAndHeight)))
                .setDy(Float.valueOf(fontSizeMap.get(widthAndHeight)))
                .setContent(username)
                .setFontSize(fontSizeMap.get(widthAndHeight))
                .setFontColor("#ffffff")
                .setFontApha(0.7F);
        com.aliyun.imm20200930.models.TargetVideo.TargetVideoFilterVideo targets0TargetVideoFilterVideo = new com.aliyun.imm20200930.models.TargetVideo.TargetVideoFilterVideo()
                .setWatermarks(java.util.Arrays.asList(
                    targets0TargetVideoFilterVideoWatermarks0
                ));
        com.aliyun.imm20200930.models.TargetVideo.TargetVideoTranscodeVideo targets0TargetVideoTranscodeVideo = new com.aliyun.imm20200930.models.TargetVideo.TargetVideoTranscodeVideo()
                .setResolution(widthAndHeight)
                .setCodec("h264")
                .setAdaptiveResolutionDirection(true)
                .setCRF(18F);
        com.aliyun.imm20200930.models.TargetVideo targets0TargetVideo = new com.aliyun.imm20200930.models.TargetVideo()
                .setDisableVideo(false)
                .setTranscodeVideo(targets0TargetVideoTranscodeVideo)
                .setFilterVideo(targets0TargetVideoFilterVideo)
                .setStream(java.util.Arrays.asList(
                    0
                ));
        com.aliyun.imm20200930.models.CreateMediaConvertTaskRequest.CreateMediaConvertTaskRequestTargets targets0 = new com.aliyun.imm20200930.models.CreateMediaConvertTaskRequest.CreateMediaConvertTaskRequestTargets()
                .setURI("oss://" + aliProperties.getBucketName() + "/" + targetPath)
                .setVideo(targets0TargetVideo)
                .setContainer("mp4");
        com.aliyun.imm20200930.models.CreateMediaConvertTaskRequest.CreateMediaConvertTaskRequestSources sources0 = new com.aliyun.imm20200930.models.CreateMediaConvertTaskRequest.CreateMediaConvertTaskRequestSources()
                .setURI("oss://" + aliProperties.getBucketName() + "/" + originPath);
        com.aliyun.imm20200930.models.CreateMediaConvertTaskRequest createMediaConvertTaskRequest = new com.aliyun.imm20200930.models.CreateMediaConvertTaskRequest()
                .setProjectName("video_test")
                .setSources(java.util.Arrays.asList(
                    sources0
                ))
                .setTargets(java.util.Arrays.asList(
                    targets0
                ));
        com.aliyun.teautil.models.RuntimeOptions runtime = new com.aliyun.teautil.models.RuntimeOptions();
        try {
            // 复制代码运行请自行打印 API 的返回值
            return client.createMediaConvertTaskWithOptions(createMediaConvertTaskRequest, runtime).getBody().getTaskId();
        } catch (TeaException error) {
            // 此处仅做打印展示，请谨慎对待异常处理，在工程项目中切勿直接忽略异常。
            // 错误 message
            System.out.println(error.getMessage());
            // 诊断地址
            System.out.println(error.getData().get("Recommend"));
            com.aliyun.teautil.Common.assertAsString(error.message);
            throw new RuntimeException();
        } catch (Exception _error) {
            TeaException error = new TeaException(_error.getMessage(), _error);
            // 此处仅做打印展示，请谨慎对待异常处理，在工程项目中切勿直接忽略异常。
            // 错误 message
            System.out.println(error.getMessage());
            // 诊断地址
            System.out.println(error.getData().get("Recommend"));
            com.aliyun.teautil.Common.assertAsString(error.message);
            throw new RuntimeException();
        }
    }

    /**
     * 获取转码结果
     * @return "Succeeded" or "Failed"
     */
    public String getTransformResult(String aliTaskId) {
        com.aliyun.imm20200930.Client client = createClient();
        com.aliyun.imm20200930.models.GetTaskRequest getTaskRequest = new com.aliyun.imm20200930.models.GetTaskRequest()
                .setTaskId(aliTaskId)
                .setProjectName("video_test")
                .setTaskType("MediaConvert");
        com.aliyun.teautil.models.RuntimeOptions runtime = new com.aliyun.teautil.models.RuntimeOptions();
        try {
            // 复制代码运行请自行打印 API 的返回值
            return client.getTaskWithOptions(getTaskRequest, runtime).getBody().getStatus();
        } catch (TeaException error) {
            // 此处仅做打印展示，请谨慎对待异常处理，在工程项目中切勿直接忽略异常。
            // 错误 message
            System.out.println(error.getMessage());
            // 诊断地址
            System.out.println(error.getData().get("Recommend"));
            com.aliyun.teautil.Common.assertAsString(error.message);
            throw new RuntimeException();
        } catch (Exception _error) {
            TeaException error = new TeaException(_error.getMessage(), _error);
            // 此处仅做打印展示，请谨慎对待异常处理，在工程项目中切勿直接忽略异常。
            // 错误 message
            System.out.println(error.getMessage());
            // 诊断地址
            System.out.println(error.getData().get("Recommend"));
            com.aliyun.teautil.Common.assertAsString(error.message);
            throw new RuntimeException();
        }
    }
}