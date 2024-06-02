package com.abdecd.moebackend.business.pojo.dto.plainuser;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class UpdateUserPermissionDTO {
    @NotNull
    private Long id;

    @Pattern(regexp = "^\\d+(,\\d+)*$")
    private String permission;
}
