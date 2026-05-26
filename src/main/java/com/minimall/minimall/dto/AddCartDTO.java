package com.minimall.minimall.dto;

import lombok.Data;

@Data
public class AddCartDTO {
    private Long productId;
    private Integer quantity;
}
