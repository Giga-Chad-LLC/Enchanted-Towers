---- GAME AUTH-RELATED TABLES ----

-- Create user table
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL
);

-- Create JWT tokens table
CREATE TABLE IF NOT EXISTS jwt_tokens (
    id SERIAL PRIMARY KEY,
    user_id INTEGER UNIQUE REFERENCES users(id),
    token VARCHAR(255) NOT NULL
);

-- Create game session tokens table
CREATE TABLE IF NOT EXISTS game_session_tokens (
    id SERIAL PRIMARY KEY,
    user_id INTEGER UNIQUE REFERENCES users(id),
    token VARCHAR(255) NOT NULL
);

---- GAME MODELS TABLES ----

-- Create positions table
CREATE TABLE IF NOT EXISTS positions (
    id SERIAL PRIMARY KEY,
    x DOUBLE PRECISION NOT NULL,
    y DOUBLE PRECISION NOT NULL,
    tower_id INT
);

-- Create towers table
CREATE TABLE IF NOT EXISTS towers (
    id SERIAL PRIMARY KEY,
    position_id INT REFERENCES positions(id) NOT NULL,
    owner_id INT REFERENCES users(id) DEFAULT NULL,
    owner_username TEXT DEFAULT NULL,
    last_protection_wall_modification_timestamp TIMESTAMP DEFAULT NULL,
    is_under_protection_walls_installation BOOLEAN DEFAULT false NOT NULL,
    is_under_capture_lock BOOLEAN DEFAULT false NOT NULL,
    is_under_attack BOOLEAN DEFAULT false NOT NULL
);

-- Alter positions table to add constaint to tower_id field
ALTER TABLE positions
ADD CONSTRAINT fk_positions_towers
FOREIGN KEY (tower_id) REFERENCES towers(id)
DEFERRABLE INITIALLY DEFERRED;


-- Create protection wall states table
CREATE TABLE protection_wall_states (
    id SERIAL PRIMARY KEY,
    broken BOOLEAN DEFAULT false NOT NULL,
    enchanted BOOLEAN DEFAULT false NOT NULL,
    protection_wall_id INT
);

-- Create protection walls table
CREATE TABLE IF NOT EXISTS protection_walls (
    id SERIAL PRIMARY KEY,
    tower_id INT REFERENCES towers(id) NOT NULL,
    state_id INT REFERENCES protection_wall_states(id) NOT NULL
);

-- Alter protection_wall_states table to add constaint to protection_wall_id field
ALTER TABLE protection_wall_states
ADD CONSTRAINT fk_protection_wall_states_protection_walls
FOREIGN KEY (protection_wall_id) REFERENCES protection_walls(id)
DEFERRABLE INITIALLY DEFERRED;