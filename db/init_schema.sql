SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;
COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;
COMMENT ON EXTENSION pgcrypto IS 'cryptographic functions';

CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;
COMMENT ON EXTENSION "uuid-ossp" IS 'generate universally unique identifiers (UUIDs)';

SET default_tablespace = '';
SET default_with_oids = false;

CREATE TABLE IF NOT EXISTS public.event (
    uuid uuid PRIMARY KEY DEFAULT '4626b2c2-c303-4433-aba1-e65fd9310945'::uuid NOT NULL,
    "timestamp" timestamp with time zone DEFAULT '2019-01-21 18:55:57.154+00'::timestamp with time zone,
    type text DEFAULT 'UNKNOWN'::character varying,
    data jsonb,
    aggregate text
);

ALTER TABLE public.event OWNER TO postgres;

CREATE TABLE IF NOT EXISTS public.menu_view (
    uuid uuid PRIMARY KEY NOT NULL,
    name text UNIQUE,
    ingredients text[],
    recipe text,
    link text,
    selected_count integer
);

ALTER TABLE public.menu_view OWNER TO postgres;

CREATE TABLE IF NOT EXISTS public.user_view (
    uuid uuid NOT NULL PRIMARY KEY,
    name text,
    email text UNIQUE
);

ALTER TABLE public.user_view OWNER TO postgres;
