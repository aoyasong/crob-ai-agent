SELECT 'CREATE DATABASE crob_agent OWNER postgres'
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'crob_agent')\gexec
