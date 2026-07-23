package com.xd.smartworksite.review.infra;

import com.xd.smartworksite.common.exception.BusinessException;
import com.xd.smartworksite.common.result.ErrorCode;
import com.xd.smartworksite.review.application.ReviewProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MarkdownReviewChunker {
    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+?)\\s*$");
    private static final Pattern PAGE_PATTERN = Pattern.compile("(?i)(?:page|页码)\\s*[:：]?\\s*(\\d+)");
    private static final Pattern TABLE_ROW_PATTERN = Pattern.compile("^\\|\\s*(\\d+)\\s*\\|.*$");

    private final ReviewProperties properties;

    public MarkdownReviewChunker(ReviewProperties properties) {
        this.properties = properties;
    }

    public List<MarkdownReviewChunk> split(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "parsed Markdown is empty");
        }
        String normalized = markdown.replace("\r\n", "\n").replace('\r', '\n').trim();
        ReviewProperties.Chunk config = properties.getChunk();
        if (normalized.length() > config.getMaxDocumentChars()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "parsed Markdown exceeds review size limit");
        }
        int maxChars = Math.max(1000, config.getMaxChars());
        int overlapChars = Math.max(0, Math.min(config.getOverlapChars(), maxChars / 3));
        List<ChunkDraft> drafts = new ArrayList<>();
        String[] lines = normalized.split("\n", -1);
        List<String> headingStack = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        String currentHeading = null;
        String currentSheet = null;

        for (String line : lines) {
            Matcher headingMatcher = HEADING_PATTERN.matcher(line);
            if (headingMatcher.matches()) {
                if (!current.toString().isBlank()) {
                    drafts.add(new ChunkDraft(current.toString().trim(), currentHeading, currentSheet));
                    current.setLength(0);
                }
                int level = headingMatcher.group(1).length();
                while (headingStack.size() >= level) {
                    headingStack.remove(headingStack.size() - 1);
                }
                headingStack.add(headingMatcher.group(2).trim());
                currentHeading = String.join("/", headingStack);
                if (headingMatcher.group(2).trim().toLowerCase().startsWith("sheet:")) {
                    currentSheet = headingMatcher.group(2).trim().substring("sheet:".length()).trim();
                } else if (level == 1) {
                    currentSheet = null;
                }
            }
            String addition = line + "\n";
            if (current.length() > 0 && current.length() + addition.length() > maxChars) {
                drafts.add(new ChunkDraft(current.toString().trim(), currentHeading, currentSheet));
                String overlap = tail(current.toString(), overlapChars);
                current.setLength(0);
                if (!overlap.isBlank()) {
                    current.append(overlap).append('\n');
                }
            }
            if (addition.length() > maxChars) {
                int offset = 0;
                while (offset < addition.length()) {
                    int end = Math.min(addition.length(), offset + maxChars);
                    if (current.length() > 0) {
                        drafts.add(new ChunkDraft(current.toString().trim(), currentHeading, currentSheet));
                        current.setLength(0);
                    }
                    drafts.add(new ChunkDraft(addition.substring(offset, end).trim(), currentHeading, currentSheet));
                    offset = end;
                }
            } else {
                current.append(addition);
            }
        }
        if (!current.toString().isBlank()) {
            drafts.add(new ChunkDraft(current.toString().trim(), currentHeading, currentSheet));
        }
        if (drafts.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "parsed Markdown produced no chunks");
        }
        if (drafts.size() > config.getMaxChunks()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "parsed Markdown chunk count exceeds review limit");
        }

        List<MarkdownReviewChunk> chunks = new ArrayList<>(drafts.size());
        for (int index = 0; index < drafts.size(); index++) {
            ChunkDraft draft = drafts.get(index);
            int chunkNo = index + 1;
            String chunkCode = "CHUNK_%04d".formatted(chunkNo);
            PageRange pages = pageRange(draft.content());
            RowRange rows = rowRange(draft.content());
            chunks.add(new MarkdownReviewChunk(
                    chunkNo,
                    chunkCode,
                    draft.content(),
                    draft.headingPath(),
                    pages.start(),
                    pages.end(),
                    draft.sheetName(),
                    rows.start(),
                    rows.end(),
                    sha256(draft.content()),
                    Math.max(1, (draft.content().length() + 1) / 2)
            ));
        }
        return chunks;
    }

    private String tail(String value, int maxLength) {
        if (maxLength <= 0 || value.isBlank()) {
            return "";
        }
        int start = Math.max(0, value.length() - maxLength);
        int newline = value.indexOf('\n', start);
        return value.substring(newline >= 0 ? newline + 1 : start).trim();
    }

    private PageRange pageRange(String content) {
        Matcher matcher = PAGE_PATTERN.matcher(content);
        Integer start = null;
        Integer end = null;
        while (matcher.find()) {
            int value = Integer.parseInt(matcher.group(1));
            start = start == null ? value : Math.min(start, value);
            end = end == null ? value : Math.max(end, value);
        }
        return new PageRange(start, end);
    }

    private RowRange rowRange(String content) {
        Integer start = null;
        Integer end = null;
        for (String line : content.split("\n")) {
            Matcher matcher = TABLE_ROW_PATTERN.matcher(line.trim());
            if (matcher.matches()) {
                int value = Integer.parseInt(matcher.group(1));
                start = start == null ? value : Math.min(start, value);
                end = end == null ? value : Math.max(end, value);
            }
        }
        return new RowRange(start, end);
    }

    private String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "review chunk hash calculation failed");
        }
    }

    private record ChunkDraft(String content, String headingPath, String sheetName) {}
    private record PageRange(Integer start, Integer end) {}
    private record RowRange(Integer start, Integer end) {}
}
