-- ============================================================
-- PromptVault : sample data script (MySQL 8+)
-- ------------------------------------------------------------
-- Run this after schema.sql to populate the database with the
-- same demo content the application seeds automatically on first
-- start. The application also seeds this data on startup if the
-- tables are empty, so running this script is optional.
--
-- Demo login credentials (username / password):
--   admin / admin123       (role ADMIN)
--   alice / password123    (role USER)
--   bob   / password123    (role USER)
--
-- Passwords are stored as PBKDF2 hashes in the format
-- pbkdf2$<iterations>$<base64 salt>$<base64 hash> exactly as the
-- application produces them, so these accounts log in normally.
-- ============================================================

USE promptvault;

-- Clear existing data (child tables first) so the script is re-runnable.
DELETE FROM submission_history;
DELETE FROM prompts;
DELETE FROM policy_keywords;
DELETE FROM categories;
DELETE FROM users;

-- ------------------------------------------------------------
-- Users
-- ------------------------------------------------------------
INSERT INTO users (id, username, password, email, role, is_active, first_name, last_name) VALUES
(1, 'admin', 'pbkdf2$120000$bB7UvlT+ftugERBoHMJPIg==$ZC0TXp6tIvVWsYq1r9qJjJn278x7Ob/Pv2oFtuiX6Aw=', 'admin@promptvault.local', 'ADMIN', TRUE, 'System', 'Administrator'),
(2, 'alice', 'pbkdf2$120000$niTukB5yR6lEZHVygrU9nA==$ilUxIqlmkprYqZuGhHWMBC9/u6a03n5s588hFsGP/js=', 'alice@promptvault.local', 'USER', TRUE, 'Alice', 'Johnson'),
(3, 'bob',   'pbkdf2$120000$N/P29RTbdAuSQlwP3Uld6A==$AU+e2vOvn2DyU4o+bvHXsABQaniaMeyV87GfvHNkydk=', 'bob@promptvault.local', 'USER', TRUE, 'Bob', 'Smith');

-- ------------------------------------------------------------
-- Categories
-- ------------------------------------------------------------
INSERT INTO categories (id, name, description) VALUES
(1, 'Coding', 'Programming, debugging and software development prompts'),
(2, 'Research', 'Academic research, literature reviews and study prompts'),
(3, 'Cybersecurity', 'Security, privacy and safe handling of information'),
(4, 'Legal', 'Contracts, policies and legal drafting prompts'),
(5, 'HR', 'Recruitment, onboarding and people management prompts'),
(6, 'Personal productivity', 'Planning, organisation and day to day productivity');

-- ------------------------------------------------------------
-- Policy keywords
-- ------------------------------------------------------------
INSERT INTO policy_keywords (id, word) VALUES
(1, 'password'),
(2, 'api key'),
(3, 'secret'),
(4, 'credit card'),
(5, 'private key'),
(6, 'confidential'),
(7, 'medical record'),
(8, 'student number');

-- ------------------------------------------------------------
-- Prompts (mix of private and shared; two are flagged)
-- ------------------------------------------------------------
INSERT INTO prompts (id, title, prompt_text, visibility, is_flagged, flagged_keyword, category_id, user_id, submission_date, ai_response) VALUES
(1, 'Refactor a Python function', 'Refactor this Python function to be more readable and add type hints.', 'SHARED', FALSE, NULL, 1, 2, NOW(), 'Simulated AI response: Refactor this Python function to be more readable and add type hints.'),
(2, 'Store an API key safely', 'What is the safest way to store my api key inside a Spring Boot project?', 'PRIVATE', TRUE, 'api key', 3, 2, NOW(), NULL),
(3, 'Draft a simple NDA clause', 'Draft a short non disclosure clause suitable for a student software project.', 'SHARED', FALSE, NULL, 4, 2, NOW(), NULL),
(4, 'Literature review outline', 'Create an outline for a literature review about large language models in education.', 'SHARED', FALSE, NULL, 2, 3, NOW(), 'Simulated AI response: Create an outline for a literature review about large language models in education.'),
(5, 'Reset my password steps', 'List the steps to reset my password and recover access to my account securely.', 'PRIVATE', TRUE, 'password', 6, 3, NOW(), NULL),
(6, 'New starter onboarding checklist', 'Write an onboarding checklist for a new software engineering intern.', 'PRIVATE', FALSE, NULL, 5, 3, NOW(), NULL);

-- ------------------------------------------------------------
-- Submission history (two prior submissions to the simulated AI)
-- ------------------------------------------------------------
INSERT INTO submission_history (id, submission_date, ai_response, user_id, prompt_id) VALUES
(1, NOW(), 'Simulated AI response: Refactor this Python function to be more readable and add type hints.', 2, 1),
(2, NOW(), 'Simulated AI response: Create an outline for a literature review about large language models in education.', 3, 4);
