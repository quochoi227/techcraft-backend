package com.techcraft.techcraftbackend.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddCartPcCaseRequest {

    @NotNull(message = "ID cấu hình PC (pc_case_id) không được để trống")
    @JsonAlias({"pcCaseId", "pc_case_id"})
    private UUID pcCaseId;

    @Min(value = 1, message = "Số lượng cấu hình PC phải lớn hơn hoặc bằng 1")
    @JsonAlias({"quantity"})
    @Builder.Default
    private Integer quantity = 1;
}
