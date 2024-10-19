-- V2__Add_default_password.sql

ALTER TABLE users
MODIFY password VARCHAR(255) DEFAULT NULL;
