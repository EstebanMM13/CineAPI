CREATE TABLE IF NOT EXISTS genres (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS movies (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    movie_year INT,
    rating DOUBLE,
    votes INT,
    image_url VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS reviews (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    movie_id BIGINT NOT NULL,
    comment TEXT,
    created_at DATETIME,
    FOREIGN KEY (movie_id) REFERENCES movies(id)
);

CREATE TABLE IF NOT EXISTS votes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    movie_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    rating DOUBLE,
    voted_at DATETIME,
    UNIQUE KEY uk_user_movie_vote (user_id, movie_id),
    FOREIGN KEY (movie_id) REFERENCES movies(id)
);

CREATE TABLE IF NOT EXISTS movie_genres (
    movie_id BIGINT NOT NULL,
    genre_id BIGINT NOT NULL,
    PRIMARY KEY (movie_id, genre_id),
    FOREIGN KEY (movie_id) REFERENCES movies(id),
    FOREIGN KEY (genre_id) REFERENCES genres(id)
);

INSERT INTO genres (name) VALUES
    ('Acción'), ('Drama'), ('Comedia'), ('Ciencia Ficción'),
    ('Terror'), ('Aventura'), ('Animación'), ('Romance');

INSERT INTO movies (title, description, movie_year, rating, votes, image_url) VALUES
    ('Inception', 'Un ladrón que roba secretos corporativos a través de sueños.', 2010, 0.0, 0, 'https://image.tmdb.org/t/p/w500/9gk7adHYeDvHkCSEqAvQNLV5Uge.jpg'),
    ('The Matrix', 'Un programador descubre que la realidad es una simulación.', 1999, 0.0, 0, 'https://image.tmdb.org/t/p/w500/f89U3ADr1oiB1s9GkdPOEpXUk5H.jpg'),
    ('Interstellar', 'Exploradores viajan a través de un agujero de gusano en busca de un nuevo hogar.', 2014, 0.0, 0, 'https://image.tmdb.org/t/p/w500/gEU2ibni4qzHp6gKjZHtB6nPE1F.jpg'),
    ('The Dark Knight', 'Batman lucha contra el Joker en Gotham City.', 2008, 0.0, 0, 'https://image.tmdb.org/t/p/w500/qJ2tW6WMUDux911r6m7haRef0WH.jpg'),
    ('Pulp Fiction', 'Historias entrelazadas de crimen en Los Ángeles.', 1994, 0.0, 0, 'https://image.tmdb.org/t/p/w500/d5iIlFn5s0ImszYzBPb8JPIfbXD.jpg');

INSERT INTO movie_genres (movie_id, genre_id) VALUES
    (1, 4), (1, 1),
    (2, 4), (2, 1),
    (3, 4), (3, 6),
    (4, 1), (4, 2),
    (5, 2), (5, 3);