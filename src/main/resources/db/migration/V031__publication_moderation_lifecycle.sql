ALTER TABLE publication DROP CONSTRAINT publication_lifecycle_ck;
ALTER TABLE publication ADD CONSTRAINT publication_lifecycle_ck
    CHECK (lifecycle IN ('ACTIVE', 'DELETED', 'HIDDEN'));
