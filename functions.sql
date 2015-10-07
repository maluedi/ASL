DROP FUNCTION IF EXISTS register_user();
DROP FUNCTION IF EXISTS create_queue();
DROP FUNCTION IF EXISTS delete_queue(int);
DROP FUNCTION IF EXISTS push_message(int,int,int,varchar);
DROP FUNCTION IF EXISTS push_message(int,int,varchar);
DROP FUNCTION IF EXISTS peek_message(int,int);
DROP FUNCTION IF EXISTS poll_message(int,int);
DROP FUNCTION IF EXISTS get_queues(int);
DROP FUNCTION IF EXISTS get_message(int,int);

--create a new user with a new serial key
CREATE OR REPLACE FUNCTION register_user() 
  RETURNS integer AS $$
DECLARE
  newID integer;
BEGIN
  INSERT INTO users(id)
    VALUES(DEFAULT) RETURNING id INTO newID;
  RETURN newID;
END;
$$ LANGUAGE plpgsql;

--create a new queue with a new serial key
CREATE OR REPLACE FUNCTION create_queue() 
  RETURNS integer AS $$
DECLARE
  newQ integer;
BEGIN
  INSERT INTO queues(id)
    VALUES(DEFAULT) RETURNING id INTO newQ;
  RETURN newQ;
END;
$$ LANGUAGE plpgsql;

--delete a queue
CREATE OR REPLACE FUNCTION delete_queue(qID integer) 
  RETURNS void AS $$
BEGIN
  DELETE FROM queues 
    WHERE id = qID ;
  IF NOT FOUND THEN
	 RAISE EXCEPTION 'queue does not exist: %', qID
	   USING ERRCODE = '23101';
  END IF;
END;
$$ LANGUAGE plpgsql;

--push a message on a queue with a receiver specified
CREATE OR REPLACE FUNCTION push_message(qID integer, sID integer, rID integer, message varchar) 
  RETURNS void AS $$
BEGIN
  PERFORM id FROM queues WHERE id = qID;
  IF NOT FOUND THEN
    RAISE EXCEPTION 'queue does not exist: %', qID
	  USING ERRCODE = '23101';
  END IF;
  PERFORM id FROM users WHERE id = sID;
  IF NOT FOUND THEN
    RAISE EXCEPTION 'sender does not exist: %', sID
	  USING ERRCODE = '23102';
  END IF;
  PERFORM id FROM users WHERE id = rID;
  IF NOT FOUND THEN
    RAISE EXCEPTION 'receiver does not exist: %', rID
	  USING ERRCODE = '23103';
  END IF;
  INSERT INTO messages(sender,receiver,queue,message)
    VALUES(sID, rID, qID, message);
 END;
 $$ LANGUAGE plpgsql;
  
 --push a message on a queue with no receiver specified
CREATE OR REPLACE FUNCTION push_message(qID integer, sID integer, message varchar) 
  RETURNS void AS $$
BEGIN
  PERFORM id FROM queues WHERE id = qID;
  IF NOT FOUND THEN
    RAISE EXCEPTION 'queue does not exist: %', qID
	  USING ERRCODE = '23101';
  END IF;
  PERFORM id FROM users WHERE id = sID;
  IF NOT FOUND THEN
    RAISE EXCEPTION 'sender does not exist: %', sID
	  USING ERRCODE = '23102';
  END IF;
  INSERT INTO messages(sender,receiver,queue,message)
    VALUES(sID, NULL, qID, message);
 END;
 $$ LANGUAGE plpgsql;
 
--peek a message from a queue
CREATE OR REPLACE FUNCTION peek_message(qID integer, rID integer)
  RETURNS messages AS $$
DECLARE
  res messages%ROWTYPE;
BEGIN
  PERFORM id FROM queues WHERE id = qID;
  IF NOT FOUND THEN
    RAISE EXCEPTION 'queue does not exist: %', qID
	  USING ERRCODE = '23101';
  END IF;
  PERFORM id FROM users WHERE id = rID;
  IF NOT FOUND THEN
    RAISE EXCEPTION 'receiver does not exist: %', rID
	  USING ERRCODE = '23103';
  END IF;
  SELECT INTO res * FROM messages 
    WHERE queue = qID
	  AND (receiver = rID OR receiver IS NULL)
	ORDER BY entrytime ASC
	LIMIT 1;
  IF NOT FOUND THEN
    RAISE EXCEPTION 'queue is empty: %', qID
	  USING ERRCODE = '23104';
  END IF;
  RETURN res;
END;
$$ LANGUAGE plpgsql;

--peek a message from a queue from a particular sender
CREATE OR REPLACE FUNCTION peek_message(qID integer, sID integer, rID integer)
  RETURNS messages AS $$
DECLARE
  res messages%ROWTYPE;
BEGIN
  PERFORM id FROM queues WHERE id = qID;
  IF NOT FOUND THEN
    RAISE EXCEPTION 'queue does not exist: %', qID
	  USING ERRCODE = '23101';
  END IF;
  PERFORM id FROM users WHERE id = sID;
  IF NOT FOUND THEN
    RAISE EXCEPTION 'sender does not exist: %', sID
	  USING ERRCODE = '23102';
  END IF;
  PERFORM id FROM users WHERE id = rID;
  IF NOT FOUND THEN
    RAISE EXCEPTION 'receiver does not exist: %', rID
	  USING ERRCODE = '23103';
  END IF;
  SELECT INTO res * FROM messages 
    WHERE queue = qID
	  AND sender = sID
	  AND (receiver = rID OR receiver IS NULL)
	ORDER BY entrytime ASC
	LIMIT 1;
  IF NOT FOUND THEN
    RAISE EXCEPTION 'queue is empty: %', qID
	  USING ERRCODE = '23104';
  END IF;
  RETURN res;
END;
$$ LANGUAGE plpgsql;

--poll a message from a queue
CREATE OR REPLACE FUNCTION poll_message(qID integer, rID integer)
  RETURNS messages AS $$
DECLARE
  resID integer;
  res messages%ROWTYPE;
BEGIN
  PERFORM id FROM queues WHERE id = qID;
  IF NOT FOUND THEN
	RAISE EXCEPTION 'queue does not exist: %', qID
	  USING ERRCODE = '23101';
  END IF;
  PERFORM id FROM users WHERE id = rID;
  IF NOT FOUND THEN
	RAISE EXCEPTION 'receiver does not exist: %', rID
	  USING ERRCODE = '23103';
  END IF;
  SELECT INTO resID id FROM messages
	WHERE queue = qID
	  AND (receiver = rID OR receiver IS NULL)
	ORDER BY entrytime ASC
	LIMIT 1;
  IF NOT FOUND THEN
	RAISE EXCEPTION 'queue is empty: %', qID
	  USING ERRCODE = '23104';
  END IF;
  DELETE FROM messages
	WHERE id = resID
	RETURNING * INTO res;
  RETURN res;
END;
$$ LANGUAGE plpgsql;

--poll a message from a queue from a particular sender
CREATE OR REPLACE FUNCTION poll_message(qID integer, sID integer, rID integer)
  RETURNS messages AS $$
DECLARE
  resID integer;
  res messages%ROWTYPE;
BEGIN
  PERFORM id FROM queues WHERE id = qID;
  IF NOT FOUND THEN
	RAISE EXCEPTION 'queue does not exist: %', qID
	  USING ERRCODE = '23101';
  END IF;
  PERFORM id FROM users WHERE id = sID;
  IF NOT FOUND THEN
	RAISE EXCEPTION 'sender does not exist: %', rID
	  USING ERRCODE = '23102';
  END IF;
  PERFORM id FROM users WHERE id = rID;
  IF NOT FOUND THEN
	RAISE EXCEPTION 'receiver does not exist: %', rID
	  USING ERRCODE = '23103';
  END IF;
  SELECT INTO resID id FROM messages
	WHERE queue = qID
	  AND sender = sID
	  AND (receiver = rID OR receiver IS NULL)
	ORDER BY entrytime ASC
	LIMIT 1;
  IF NOT FOUND THEN
	RAISE EXCEPTION 'queue is empty: %', qID
	  USING ERRCODE = '23104';
  END IF;
  DELETE FROM messages
	WHERE id = resID
	RETURNING * INTO res;
  RETURN res;
END;
$$ LANGUAGE plpgsql;

--get queues with messages for a particular receiver
CREATE OR REPLACE FUNCTION get_queues(rID integer)
  RETURNS SETOF queues AS $$
  SELECT DISTINCT queue FROM messages
    WHERE receiver = rID;
$$ LANGUAGE SQL;

--get a message from a particular sender
CREATE OR REPLACE FUNCTION get_message(sID integer, rID integer)
  RETURNS SETOF messages AS $$
  SELECT * FROM messages
    WHERE sender = sID
	  AND (receiver = rID OR receiver IS NULL)
	LIMIT 1;
$$ LANGUAGE SQL;