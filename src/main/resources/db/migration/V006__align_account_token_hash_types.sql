ALTER TABLE password_reset_token
    ALTER COLUMN token_hash TYPE VARCHAR(64);

ALTER TABLE password_reset_token
    ADD CONSTRAINT password_reset_token_hash_length_ck
        CHECK (char_length(token_hash) = 64);

ALTER TABLE email_verification_token
    ALTER COLUMN token_hash TYPE VARCHAR(64);

ALTER TABLE email_verification_token
    ADD CONSTRAINT email_verification_token_hash_length_ck
        CHECK (char_length(token_hash) = 64);
