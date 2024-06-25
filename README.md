## moe-backend

application-dev.yml 示例：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/moe_main?rewriteBatchedStatements=true
    username:
    password:
    driver-class-name: com.mysql.cj.jdbc.Driver
  redis:
    redisson:
      config: |
        singleServerConfig:
          address: redis://127.0.0.1:6379
          database: 1
          subscriptionConnectionMinimumIdleSize: 1
          subscriptionConnectionPoolSize: 16
          connectionMinimumIdleSize: 1
          connectionPoolSize: 16
  data:
    redis:
      host: localhost
      port: 6379
      database: 1
  #    elasticsearch:
  #      url: http://127.0.0.1:9200
  cache:
    type: redis
  mail:
    host: xx
    port: xx
    protocol: xx
    default-encoding: UTF-8
    username: xx@xx.com
    password: xx
    test-connection: true
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true

token-login:
  encrypt-str-aes-key: 1111111111192435
  jwt-hash-key: xx
  test: true

moe:
  local-file-service:
    file-base-path: D:/code/xx/local-file-service
    url-prefix: /common/image
  video-base-path: https://resource.xx
  default-video-path: https://resource.xx/video/default.mp4
  bili-session: xx
  bv-url: https://xx/bv
  bv-proxy-prefix: https://bi.xx
  proxy-prefix: https://xx/proxy?pReferer=https://xx.com&pUrl=
# 限制视频转码 redis key    moe:limit:transform_video 有值就行

ali:
  oss:
    enable: true
    access-key-id:
    access-key-secret:
    bucket-name:
    endpoint:
    url-prefix: https://resource.xx
    sts-region-id:
    sts-endpoint:
    sts-role-arn:
    sts-duration-seconds: 900
    sts-max-size: 2_147_483_648 # 2GB
    watermark: common/watermark.png

# mybatis-plus配置控制台打印完整带参数SQL语句
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

springdoc:
  swagger-ui:
    path: /swagger-ui.html
    tags-sorter: alpha
    operations-sorter: alpha
  api-docs:
    path: /v3/api-docs
  group-configs:
    - group: 'base'
      paths-to-match: '/**'
      packages-to-scan: com.abdecd.moebackend.business.controller.base
    - group: 'backstage'
      paths-to-match: '/**'
      packages-to-scan: com.abdecd.moebackend.business.controller.backstage

```