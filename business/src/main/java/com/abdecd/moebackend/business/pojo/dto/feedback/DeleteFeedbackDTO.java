package com.abdecd.moebackend.business.pojo.dto.feedback;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Accessors(chain = true)
@Data
public class DeleteFeedbackDTO {
    @NotNull
    private Long[] ids;
}
