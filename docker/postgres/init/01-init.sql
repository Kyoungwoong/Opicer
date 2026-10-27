-- Runs once on first container start (empty volume).
CREATE EXTENSION IF NOT EXISTS vector;

CREATE DATABASE opicer_test;
\c opicer_test
CREATE EXTENSION IF NOT EXISTS vector;
