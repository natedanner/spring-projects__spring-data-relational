CREATE TABLE ENTITY_WITH_COLLECTIBLES
(
    ID BIGINT GENERATED BY DEFAULT AS IDENTITY ( START WITH 1 ) PRIMARY KEY
);

CREATE TABLE OTHER_ENTITY
(
    ID   BIGINT GENERATED BY DEFAULT AS IDENTITY ( START WITH 1 ) PRIMARY KEY,
    NAME VARCHAR(100)
);

CREATE TABLE SET_COLLECTIBLE
(
    ENTITY_WITH_COLLECTIBLES_ID INTEGER NOT NULL REFERENCES ENTITY_WITH_COLLECTIBLES (ID),
    OTHER_ENTITY_ID INTEGER NOT NULL REFERENCES OTHER_ENTITY (ID),
);

CREATE TABLE LIST_COLLECTIBLE
(
    ENTITY_WITH_COLLECTIBLES_ID INTEGER NOT NULL REFERENCES ENTITY_WITH_COLLECTIBLES (ID),
    INDEX INTEGER NOT NULL,
    NAME VARCHAR(100)
);

CREATE TABLE MAP_COLLECTIBLE
(
    ENTITY_WITH_COLLECTIBLES_ID INTEGER NOT NULL REFERENCES ENTITY_WITH_COLLECTIBLES (ID),
    NAME VARCHAR(100),
    VALUE INTEGER
);