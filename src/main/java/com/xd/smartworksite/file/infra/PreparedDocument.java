package com.xd.smartworksite.file.infra;

public class PreparedDocument {

    private final String inputFormat;
    private final String textContent;
    private final String imageDataUrl;
    private final int pageCount;
    private final boolean truncated;

    private PreparedDocument(String inputFormat, String textContent, String imageDataUrl, int pageCount, boolean truncated) {
        this.inputFormat = inputFormat;
        this.textContent = textContent;
        this.imageDataUrl = imageDataUrl;
        this.pageCount = pageCount;
        this.truncated = truncated;
    }

    public static PreparedDocument text(String inputFormat, String textContent, int pageCount, boolean truncated) {
        return new PreparedDocument(inputFormat, textContent, null, pageCount, truncated);
    }

    public static PreparedDocument image(String inputFormat, String imageDataUrl) {
        return new PreparedDocument(inputFormat, null, imageDataUrl, 1, false);
    }

    public String getInputFormat() {
        return inputFormat;
    }

    public String getTextContent() {
        return textContent;
    }

    public String getImageDataUrl() {
        return imageDataUrl;
    }

    public int getPageCount() {
        return pageCount;
    }

    public boolean isTruncated() {
        return truncated;
    }
}
