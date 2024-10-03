package com.abdecd.moebackend.business.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Configuration
public class JacksonConfiguration {
    private static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public LocalDateTimeSerializer localDateTimeSerializer() {
        return new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATETIME_FORMAT));
    }

    public ToStringSerializer localDateTime2TimestampSerializer() {
        return new ToStringSerializer() {
            @Override
            public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeNumber(((LocalDateTime) value).toInstant(ZoneOffset.UTC).toEpochMilli());
            }
        };
    }

    public LocalDateTimeDeserializer timestamp2LocalDateTimeDeserializer() {
        return new LocalDateTimeDeserializer() {
            @Override
            public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
                return LocalDateTime.ofInstant(new Date(Long.parseLong(parser.getText())).toInstant(), ZoneOffset.UTC);
            }
        };
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        return builder -> {
            // 把 Long 类型序列化为 String
            builder.serializerByType(Long.class, ToStringSerializer.instance);
//                .serializerByType(LocalDateTime.class, localDateTime2TimestampSerializer())
//                .deserializerByType(LocalDateTime.class, timestamp2LocalDateTimeDeserializer());
        };
    }
}