--
-- PostgreSQL database dump
--

-- Dumped from database version 10.9 (Ubuntu 10.9-1.pgdg18.04+1)
-- Dumped by pg_dump version 10.10 (Ubuntu 10.10-1.pgdg18.04+1)

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

--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


--
-- Name: pgcrypto; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;


--
-- Name: EXTENSION pgcrypto; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION pgcrypto IS 'cryptographic functions';


--
-- Name: uuid-ossp; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;


--
-- Name: EXTENSION "uuid-ossp"; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION "uuid-ossp" IS 'generate universally unique identifiers (UUIDs)';


SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: event; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.event (
    uuid uuid DEFAULT '4626b2c2-c303-4433-aba1-e65fd9310945'::uuid NOT NULL,
    "timestamp" timestamp with time zone DEFAULT '2019-01-21 18:55:57.154+00'::timestamp with time zone,
    type text DEFAULT 'UNKNOWN'::character varying,
    data jsonb,
    aggregate text
);


ALTER TABLE public.event OWNER TO postgres;

--
-- Name: menu_view; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.menu_view (
    uuid uuid NOT NULL,
    name text,
    ingredients text[],
    recipe text,
    link text,
    selected_count integer
);


ALTER TABLE public.menu_view OWNER TO postgres;

--
-- Name: user_view; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.user_view (
    uuid uuid NOT NULL,
    name text,
    email text
);


ALTER TABLE public.user_view OWNER TO postgres;

--
-- Name: event event_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.event
    ADD CONSTRAINT event_pkey PRIMARY KEY (uuid);


--
-- Name: menu_view menu_view_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.menu_view
    ADD CONSTRAINT menu_view_name_key UNIQUE (name);


--
-- Name: menu_view menu_view_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.menu_view
    ADD CONSTRAINT menu_view_pkey PRIMARY KEY (uuid);


--
-- Name: user_view user_view_email_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_view
    ADD CONSTRAINT user_view_email_key UNIQUE (email);


--
-- Name: user_view user_view_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_view
    ADD CONSTRAINT user_view_pkey PRIMARY KEY (uuid);


--
-- PostgreSQL database dump complete
--

