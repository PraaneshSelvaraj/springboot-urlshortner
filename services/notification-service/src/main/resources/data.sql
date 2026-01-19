-- Notification Types
INSERT IGNORE INTO notification_types (name) VALUES ('NEWURL');
INSERT IGNORE INTO notification_types (name) VALUES ('THRESHOLD');
INSERT IGNORE INTO notification_types (name) VALUES ('NEWUSER');

-- Notification Status
INSERT IGNORE INTO notification_statuses (name) VALUES ('PENDING');
INSERT IGNORE INTO notification_statuses (name) VALUES ('SUCCESS');
INSERT IGNORE INTO notification_statuses (name) VALUES ('FAILURE');
