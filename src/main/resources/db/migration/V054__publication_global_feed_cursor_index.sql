CREATE INDEX publication_global_feed_cursor_ix
    ON publication (lifecycle, scope, created_at DESC, id DESC);
