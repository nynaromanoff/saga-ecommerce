package com.nynaromanoff.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;


public record OrderRequest (
        List<ItemDTO> items
){}