CREATE TABLE upload_asset (
    id UUID NOT NULL,
    owner_member_id UUID NOT NULL,
    object_key VARCHAR(255) NOT NULL,
    checksum_sha256 VARCHAR(64) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    byte_size BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    publication_id UUID,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT upload_asset_pk PRIMARY KEY (id),
    CONSTRAINT upload_asset_owner_fk FOREIGN KEY (owner_member_id)
        REFERENCES member_account (id) ON DELETE RESTRICT,
    CONSTRAINT upload_asset_publication_fk FOREIGN KEY (publication_id)
        REFERENCES publication (id) ON DELETE RESTRICT,
    CONSTRAINT upload_asset_object_key_uq UNIQUE (object_key),
    CONSTRAINT upload_asset_status_ck CHECK (status IN ('UPLOADING', 'READY', 'ATTACHED', 'ABANDONED')),
    CONSTRAINT upload_asset_size_ck CHECK (byte_size BETWEEN 1 AND 10485760),
    CONSTRAINT upload_asset_content_type_ck CHECK (char_length(btrim(content_type)) BETWEEN 1 AND 120)
);

CREATE INDEX upload_asset_owner_status_ix
    ON upload_asset (owner_member_id, status, created_at DESC);
CREATE INDEX upload_asset_expiration_ix
    ON upload_asset (status, expires_at);
