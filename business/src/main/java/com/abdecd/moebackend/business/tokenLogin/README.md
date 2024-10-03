## TokenLogin v0.2.1

### 使用

常用内容均在TokenLoginService中  
密码校验相关在PwdUtil中

### 依赖

```xml
<dependencies>
<dependency>
    <groupId>com.auth0</groupId>
    <artifactId>java-jwt</artifactId>
</dependency>
<dependency>
    <groupId>com.password4j</groupId>
    <artifactId>password4j</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
</dependencies>
```

### 配置

common/TokenLoginProp

```java
/**
 * jwt过期时间 默认30天
 */
private Integer jwtTtlSeconds = 2592000;
/**
 * jwt密钥 默认随机生成
 */
private String jwtSecretKey;
/**
 不拦截的请求，如可以这样配置：
 - /error
 - /doc.html/**
 - /swagger-ui.html/**
 - /v3/api-docs/**
 - /webjars/**
 */
private String[] excludePatterns = new String[0];
/**
 * 测试模式; true 时启用虚拟用户允许所有请求 详见 interceptor/LoginInterceptor.inTest()
 */
private Boolean test = false;
```
例子
```yml
token-login:
  jwtTtlSeconds: 86400
  exclude-patterns:
    - /error
    - /doc.html/**
    - /swagger-ui.html/**
    - /v3/api-docs/**
    - /webjars/**
    - /login
    - /register
```

### 常量
common/TokenLoginConstant

```java
String K_USER_ID = "id";  
String K_PERMISSION = "permission";  
String JWT_TOKEN_NAME = "token";  
Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*?&]{6,20}$");  
String LOGIN_TOKEN_BLACKLIST = "xxx:loginTokenBlacklist:";  
```
