package com.dianping.dto;

import lombok.Data;

@Data
public class NearbyShopDTO {
    private Long id;
    private String name;
    private String address;
    private Double score;
    private Long avgPrice;
    private Integer sold;
    private Double distanceKm;
}
