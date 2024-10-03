FROM bellsoft/liberica-openjdk-alpine-musl:21
ENV TZ=GMT+8
RUN sed -i 's/dl-cdn.alpinelinux.org/mirrors.aliyun.com/g' /etc/apk/repositories
RUN set -xe && apk --no-cache add ttf-dejavu fontconfig
COPY ./target/moe-backend-main.jar .
CMD ["sh","-c","java -jar -Xmx256m -Xss512k -XX:+UseG1GC moe-backend-main.jar --spring.profiles.active=prod"]