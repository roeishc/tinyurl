CREATE KEYSPACE IF NOT EXISTS tiny_keyspace
WITH replication = {'class':'SimpleStrategy', 'replication_factor' : 1};
