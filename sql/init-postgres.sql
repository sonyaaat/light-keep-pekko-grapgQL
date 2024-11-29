CREATE TABLE IF NOT EXISTS text_card
(
    id uuid      NOT NULL,
    created_at   timestamp NOT NULL,
    title       VARCHAR(50),
    content     VARCHAR(100),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS check_card
(
    id uuid      NOT NULL,
    created_at   timestamp NOT NULL,
    title       VARCHAR(50) DEFAULT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS check_card_item
(
    id uuid      NOT NULL,
    created_at   timestamp NOT NULL,
    content       VARCHAR(50) DEFAULT NULL,
    is_checked    BOOLEAN DEFAULT false,
    check_card_id uuid NOT NULL ,
    PRIMARY KEY (id),
    CONSTRAINT fk_check_card_id
        FOREIGN KEY (check_card_id) REFERENCES check_card(id)
);
