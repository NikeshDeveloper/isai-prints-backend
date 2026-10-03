package com.isaiprints.dto.request;

import lombok.Data;

@Data
public class PushSubscribeRequest {
    private String endpoint;
    private String p256dh;
    private String auth;
}
