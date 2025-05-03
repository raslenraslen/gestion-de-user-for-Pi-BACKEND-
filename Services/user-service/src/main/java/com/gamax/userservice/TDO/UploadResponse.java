package com.gamax.userservice.TDO;

import lombok.Getter;

@Getter
public class UploadResponse {
    private String fileUrl;

    public UploadResponse(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }
}