package com.abdecd.moebackend.business.lib;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ListObjectsRequest;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.auth.sts.AssumeRoleRequest;
import com.aliyuncs.auth.sts.AssumeRoleResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnProperty(name = "ali.oss.enable", havingValue = "true")
public class AliStsManager {
    @Value("${ali.oss.bucket-name}")
    String bucketName;
    @Value("${ali.oss.access-key-id}")
    String accessKeyId;
    @Value("${ali.oss.access-key-secret}")
    String accessKeySecret;
    @Getter
    @Value("${ali.oss.endpoint:empty}")
    String endpoint;
    @Value("${ali.oss.sts-endpoint}")
    String stsEndpoint;
    @Value("${ali.oss.sts-region-id}")
    String stsRegionId;
    @Value("${ali.oss.sts-role-arn}")
    String stsRoleArn;
    @Value("${ali.oss.sts-duration-seconds}")
    Long stsDurationSeconds;
    @Value("${ali.oss.sts-max-size}")
    Long stsMaxSize;

    public String getPolicy(long userId, String fileName) {
        return """
                {
                  "Version": "1",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Action": ["oss:PutObject"],
                      "Resource": ["acs:oss:*:*:bucketName/tmp/userId/fileName"],
                      "Condition": {}
                    }
                  ]
                }
                """
                .replace("bucketName", bucketName)
                .replace("userId", String.valueOf(userId))
                .replace("fileName", fileName);
    }

    public AssumeRoleResponse.Credentials getSts(long userId, String fileName) throws ClientException {
        DefaultProfile.addEndpoint(stsRegionId, "Sts", stsEndpoint);
        // 构造default profile。
        IClientProfile profile = DefaultProfile.getProfile(stsRegionId, accessKeyId, accessKeySecret);
        // 构造client。
        DefaultAcsClient client = new DefaultAcsClient(profile);
        final AssumeRoleRequest request = new AssumeRoleRequest();

        String roleSessionName = "MoeSts" + userId;
        String policy = getPolicy(userId, fileName);
        System.out.println(policy);
        request.setSysMethod(MethodType.POST);
        request.setRoleArn(stsRoleArn);
        request.setRoleSessionName(roleSessionName);
        request.setPolicy(policy);
        request.setDurationSeconds(stsDurationSeconds);
        final AssumeRoleResponse response = client.getAcsResponse(request);
//        System.out.println("Expiration: " + response.getCredentials().getExpiration());
//        System.out.println("Access Key Id: " + response.getCredentials().getAccessKeyId());
//        System.out.println("Access Key Secret: " + response.getCredentials().getAccessKeySecret());
//        System.out.println("Security Token: " + response.getCredentials().getSecurityToken());
//        System.out.println("RequestId: " + response.getRequestId());
        return response.getCredentials();
    }

    /**
     * 检查用户临时空间是否超出限制
     * @param userId :
     */
    public boolean getAvailable(long userId) {
        long size = 0L;
        ObjectListing objectListing = null;
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        do {
            // MaxKey默认值为100，最大值为1000。
            ListObjectsRequest request = new ListObjectsRequest(bucketName).withPrefix("tmp/" + userId).withMaxKeys(1000);
            if (objectListing != null) {
                request.setMarker(objectListing.getNextMarker());
            }
            objectListing = ossClient.listObjects(request);
            List<OSSObjectSummary> sums = objectListing.getObjectSummaries();
            for (OSSObjectSummary s : sums) {
                size += s.getSize();
            }
        } while (objectListing.isTruncated());
        ossClient.shutdown();
        return size < stsMaxSize;
    }
}