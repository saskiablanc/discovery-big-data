CREATE TABLE person (
    id          BIGSERIAL    PRIMARY KEY,
    first_name  VARCHAR(50)  NOT NULL,
    last_name   VARCHAR(50)  NOT NULL,
    nationality VARCHAR(100) NOT NULL,
    age         INT          NOT NULL,
    picture_url VARCHAR(255) NOT NULL
);