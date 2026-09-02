CREATE INDEX IF NOT EXISTS profile_email_index ON profile USING GIN (email gin_trgm_ops);
CREATE INDEX IF NOT EXISTS profile_name_purified_index ON profile USING GIN (name_purified gin_trgm_ops);
CREATE INDEX IF NOT EXISTS profile_name_purified_id_index ON profile(name_purified, id);
