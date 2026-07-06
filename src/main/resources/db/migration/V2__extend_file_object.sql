ALTER TABLE file_object
  ADD COLUMN file_ext VARCHAR(32) NULL COMMENT 'File extension' AFTER file_name,
  ADD COLUMN storage_bucket VARCHAR(128) NULL COMMENT 'Storage bucket' AFTER object_name,
  ADD COLUMN preview_supported TINYINT NOT NULL DEFAULT 0 COMMENT 'Whether preview is supported' AFTER metadata,
  ADD KEY idx_file_hash (file_hash),
  ADD KEY idx_file_created_at (created_at);
