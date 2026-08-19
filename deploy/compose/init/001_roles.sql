CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS citext;

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'townpet_app') THEN
        CREATE ROLE townpet_app LOGIN PASSWORD 'townpet_local_dev' NOSUPERUSER NOCREATEDB NOCREATEROLE;
    END IF;
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'townpet_migration') THEN
        CREATE ROLE townpet_migration LOGIN PASSWORD 'townpet_migration_local_dev' NOSUPERUSER NOCREATEDB NOCREATEROLE;
    END IF;
END
$$;

GRANT CONNECT ON DATABASE townpet TO townpet_app;
GRANT CONNECT ON DATABASE townpet TO townpet_migration;
GRANT USAGE ON SCHEMA public TO townpet_app;
GRANT USAGE, CREATE ON SCHEMA public TO townpet_migration;
ALTER DEFAULT PRIVILEGES FOR ROLE townpet_migration IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO townpet_app;
ALTER DEFAULT PRIVILEGES FOR ROLE townpet_migration IN SCHEMA public GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO townpet_app;
