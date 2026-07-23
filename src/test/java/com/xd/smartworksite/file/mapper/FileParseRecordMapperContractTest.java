package com.xd.smartworksite.file.mapper;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FileParseRecordMapperContractTest {
    private static final Path MAPPER =
            Path.of("src/main/resources/mapper/file/FileParseRecordMapper.xml");

    @Test
    void selectByIdCanReadTerminalParseRecordsWhileRunningUpdateRemainsStateGuarded() throws Exception {
        String xml = Files.readString(MAPPER, StandardCharsets.UTF_8);
        String selectById = section(xml, "<select id=\"selectById\"", "</select>");
        String updateRunning = section(xml, "<update id=\"updateRunning\"", "</update>");

        assertThat(selectById)
                .contains("where id = #{recordId}")
                .contains("and deleted = 0")
                .doesNotContain("status in ('PENDING', 'RUNNING')");
        assertThat(updateRunning)
                .contains("where id = #{recordId}")
                .contains("and status in ('PENDING', 'RUNNING')")
                .contains("and deleted = 0");
    }

    private String section(String source, String startToken, String endToken) {
        int start = source.indexOf(startToken);
        int end = source.indexOf(endToken, start);
        assertThat(start).isGreaterThanOrEqualTo(0);
        assertThat(end).isGreaterThan(start);
        return source.substring(start, end + endToken.length());
    }
}
