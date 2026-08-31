-- ============================================================
-- PromptVault : database creation and schema script (MySQL 8+)
-- ------------------------------------------------------------
-- This script creates the database and every table used by the
-- application. The Spring Boot application is configured with
-- spring.jpa.hibernate.ddl-auto=update, so it can also create
-- these tables automatically on first start. This script is
-- provided so the schema can be created manually if preferred.
-- ============================================================

CREATE DATABASE IF NOT EXISTS promptvault
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE promptvault;

-- Drop existing tables (child tables first) so the script is re-runnable.
DROP TABLE IF EXISTS submission_history;
DROP TABLE IF EXISTS prompts;
DROP TABLE IF EXISTS policy_keywords;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS users;

-- ------------------------------------------------------------
-- Users: application accounts (admin and standard users)
-- ------------------------------------------------------------
CREATE TABLE users (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    username    VARCHAR(50)  NOT NULL,
    password    VARCHAR(255) NOT NULL,
    email       VARCHAR(120) NOT NULL,
    role        VARCHAR(20)  NOT NULL,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    first_name  VARCHAR(80)  NOT NULL,
    last_name   VARCHAR(80)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username),
    UNIQUE KEY uk_users_email (email)
) ENGINE = InnoDB;

-- ------------------------------------------------------------
-- Categories: groupings that every prompt belongs to
-- ------------------------------------------------------------
CREATE TABLE categories (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(80)  NOT NULL,
    description VARCHAR(500) NOT NULL DEFAULT '',
    PRIMARY KEY (id),
    UNIQUE KEY uk_categories_name (name)
) ENGINE = InnoDB;

-- ------------------------------------------------------------
-- Policy keywords: words or phrases that flag a prompt for review
-- ------------------------------------------------------------
CREATE TABLE policy_keywords (
    id   BIGINT       NOT NULL AUTO_INCREMENT,
    word VARCHAR(120) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_policy_keywords_word (word)
) ENGINE = InnoDB;

-- ------------------------------------------------------------
-- Prompts: the prompts created by users (private or shared)
-- ------------------------------------------------------------
CREATE TABLE prompts (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    title           VARCHAR(150) NOT NULL,
    prompt_text     TEXT         NOT NULL,
    visibility      VARCHAR(20)  NOT NULL,
    is_flagged      BOOLEAN      NOT NULL DEFAULT FALSE,
    flagged_keyword VARCHAR(120) NULL,
    category_id     BIGINT       NOT NULL,
    user_id         BIGINT       NOT NULL,
    submission_date DATETIME     NULL,
    ai_response     TEXT         NULL,
    version         BIGINT       NULL,
    PRIMARY KEY (id),
    KEY idx_prompt_user (user_id),
    KEY idx_prompt_visibility (visibility),
    KEY idx_prompt_flagged (is_flagged),
    KEY idx_prompt_submission_date (submission_date),
    CONSTRAINT fk_prompt_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT fk_prompt_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB;

-- ------------------------------------------------------------
-- Submission history: a record each time a prompt is sent to the AI
-- ------------------------------------------------------------
CREATE TABLE submission_history (
    id              BIGINT   NOT NULL AUTO_INCREMENT,
    submission_date DATETIME NOT NULL,
    ai_response     TEXT     NULL,
    user_id         BIGINT   NOT NULL,
    prompt_id       BIGINT   NOT NULL,
    PRIMARY KEY (id),
    KEY idx_history_user_date (user_id, submission_date),
    KEY idx_history_prompt (prompt_id),
    CONSTRAINT fk_history_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_history_prompt FOREIGN KEY (prompt_id) REFERENCES prompts (id)
) ENGINE = InnoDB;
