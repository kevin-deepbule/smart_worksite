package com.xd.smartworksite.file.infra;

import java.io.InputStream;
import java.time.Duration;

public interface StorageAdapter {

    StorageObject upload(String objectName, InputStream inputStream, long size, String contentType);

    InputStream openObject(String objectName);
    default InputStream download(String objectName) {
        return openObject(objectName);
    }

    String createAccessUrl(String objectName, Duration expire);

    void delete(String objectName);
}
