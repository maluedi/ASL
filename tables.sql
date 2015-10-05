DROP TABLE IF EXISTS messages CASCADE;
DROP TABLE IF EXISTS queues CASCADE;
DROP TABLE IF EXISTS users CASCADE;

CREATE TABLE users (
	id serial PRIMARY KEY
);

CREATE TABLE queues (
	id serial PRIMARY KEY
);

CREATE TABLE messages (
	id serial PRIMARY KEY,
	sender integer REFERENCES users(id) ON DELETE CASCADE,
	receiver integer REFERENCES users(id) ON DELETE CASCADE,
	queue integer NOT NULL REFERENCES queues(id) ON DELETE CASCADE,
	entrytime timestamp with time zone DEFAULT now(),
	message character varying
);

CREATE INDEX queue_index ON queues(id);
CREATE INDEX user_index ON users(id);
CREATE INDEX message_index ON messages(id);
CREATE INDEX message_queue_index ON messages(queue);
--CREATE INDEX message_time_index ON messages(entrytime);
CREATE INDEX message_sender_index ON messages(sender);
CREATE INDEX message_receiver_index ON messages(receiver);
CREATE INDEX message_q_t_r_index ON messages(queue,receiver,entrytime);