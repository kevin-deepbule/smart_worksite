package com.xd.smartworksite.file.infra;

public class StorageObject {

    private final String objectName;
    private final String bucket;
    private final String contentType;
    private final long size;

    public StorageObject(String objectName, String bucket, String contentType, long size) {
        this.objectName = objectName;
        this.bucket = bucket;
        this.contentType = contentType;
        this.size = size;
    }

    public String getObjectName() {
        return objectName;
    }

    public String getBucket() {
        return bucket;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSize() {
        return size;
    }
}
