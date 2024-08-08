package com.abdecd.moebackend.business.pojo.vo.notice;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.zip.CRC32;

@Accessors(chain = true)
@Data
public class Notice {
    String title;
    String content;
    String date;
    @Setter(AccessLevel.PRIVATE)
    String hash;

    public Notice refreshHash() {
        var crc32 = new CRC32();
        crc32.update(title.getBytes());
        crc32.update(content.getBytes());
        crc32.update(date.getBytes());
        crc32.update(((int) (Math.random() * 100000) + "").getBytes());
        this.hash = String.format("%08X", crc32.getValue());
        return this;
    }
}
