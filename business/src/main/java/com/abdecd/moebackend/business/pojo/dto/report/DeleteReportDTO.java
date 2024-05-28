package com.abdecd.moebackend.business.pojo.dto.report;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Accessors(chain = true)
@Data
public class DeleteReportDTO {
    @NotNull
    private Long[] ids;
}
