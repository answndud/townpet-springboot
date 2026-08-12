CREATE UNIQUE INDEX moderator_case_open_hospital_flag_uq
    ON moderator_case (case_type, target_id)
    WHERE case_type = 'HOSPITAL_REVIEW' AND status = 'OPEN';
