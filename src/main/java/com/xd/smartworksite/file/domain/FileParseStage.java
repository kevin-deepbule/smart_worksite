package com.xd.smartworksite.file.domain;

public enum FileParseStage {
    CREATED,
    LOADING_SOURCE,
    PREPARING_INPUT,
    CALLING_MODEL,
    NORMALIZING_RESULT,
    STORING_RESULT,
    FINISHED,
    FAILED
}
