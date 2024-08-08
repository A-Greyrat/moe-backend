package com.abdecd.moebackend.business.pojo.dto.notice;

import com.abdecd.moebackend.business.pojo.vo.notice.Notice;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;

@Accessors(chain = true)
@Data
public class NoticeUpdateDTO {
    @NotBlank
    String title;
    @NotBlank
    String content;
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$")
    String date = LocalDate.now().toString();
    @NotNull
    Integer index;

    public Notice toEntity() {
        return new Notice()
            .setTitle(title)
            .setContent(content)
            .setDate(date)
            .refreshHash();
    }
}
