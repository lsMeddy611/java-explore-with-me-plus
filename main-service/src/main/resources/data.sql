CREATE TABLE IF NOT EXISTS categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS events (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    annotation TEXT NOT NULL,
    category_id BIGINT REFERENCES categories(id),
    initiator_id BIGINT REFERENCES users(id),
    event_date TIMESTAMP,
    created_on TIMESTAMP,
    published_on TIMESTAMP,
    confirmed_requests BIGINT,
    request_moderation BOOLEAN,
    paid BOOLEAN NOT NULL DEFAULT FALSE,
    participant_limit INTEGER NOT NULL DEFAULT 0,
    state VARCHAR(50),
    location_lat DOUBLE PRECISION,
    location_lon DOUBLE PRECISION
);

CREATE TABLE IF NOT EXISTS compilations (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    pinned BOOLEAN
);

CREATE TABLE IF NOT EXISTS compilation_events (
    compilation_id BIGINT REFERENCES compilations(id),
    event_id BIGINT REFERENCES events(id),
    PRIMARY KEY (compilation_id, event_id)
);

CREATE TABLE IF NOT EXISTS requests (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT REFERENCES events(id),
    requester_id BIGINT REFERENCES users(id),
    created TIMESTAMP,
    status VARCHAR(50)
);