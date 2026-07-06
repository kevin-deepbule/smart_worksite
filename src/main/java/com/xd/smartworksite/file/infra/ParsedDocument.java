package com.xd.smartworksite.file.infra;

public class ParsedDocument {

    private final String content;
    private final String resultFormat;
    private final String modelName;
    private final String metadata;

    public ParsedDocument(String content, String resultFormat, String modelName, String metadata) {
        this.content = content;
        this.resultFormat = resultFormat;
        this.modelName = modelName;
        this.metadata = metadata;
    }

    public String getContent() {
        return content;
    }

    public String getResultFormat() {
        return resultFormat;
    }

    public String getModelName() {
        return modelName;
    }

    public String getMetadata() {
        return metadata;
    }
}
