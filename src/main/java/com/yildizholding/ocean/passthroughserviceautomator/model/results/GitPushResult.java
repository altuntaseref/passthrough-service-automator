package com.yildizholding.ocean.passthroughserviceautomator.model.results;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GitPushResult {
    private boolean attempted;
    private boolean success;
    private String repoUrl;
    private String readmeUrl; // Hesaplanan URL
    private String postmanUrl; // Hesaplanan URL
    private String errorMessage;

     public static GitPushResult skipped(String repoUrl) {
        return GitPushResult.builder().attempted(false).success(false).repoUrl(repoUrl).build();
    }
}