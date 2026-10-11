CREATE TABLE IF NOT EXISTS room_types (
    code          VARCHAR(20)   NOT NULL PRIMARY KEY,
    name          VARCHAR(60)   NOT NULL,
    price_per_day DECIMAL(10,2) NOT NULL,
    capacity      TINYINT       NOT NULL,
    description   VARCHAR(255)  NULL,
    sort_order    INT           NOT NULL DEFAULT 0
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS rooms (
    room_number INT         NOT NULL PRIMARY KEY,
    type_code   VARCHAR(20) NOT NULL,
    CONSTRAINT fk_room_type FOREIGN KEY (type_code) REFERENCES room_types (code)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS bookings (
    id             BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    room_number    INT           NOT NULL,
    status         ENUM('ACTIVE','CHECKED_OUT') NOT NULL DEFAULT 'ACTIVE',
    guest1_name    VARCHAR(100)  NOT NULL,
    guest1_contact VARCHAR(20)   NOT NULL,
    guest1_gender  VARCHAR(10)   NOT NULL,
    guest2_name    VARCHAR(100)  NULL,
    guest2_contact VARCHAR(20)   NULL,
    guest2_gender  VARCHAR(10)   NULL,
    check_in       DATETIME      NOT NULL,
    check_out      DATETIME      NULL,
    total_amount   DECIMAL(10,2) NULL,
    CONSTRAINT fk_booking_room FOREIGN KEY (room_number) REFERENCES rooms (room_number),
    INDEX idx_booking_room_status (room_number, status)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS menu_items (
    id       INT           NOT NULL PRIMARY KEY,
    name     VARCHAR(60)   NOT NULL,
    price    DECIMAL(10,2) NOT NULL,
    category VARCHAR(20)   NOT NULL DEFAULT 'FOOD',
    active   TINYINT(1)    NOT NULL DEFAULT 1
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS food_orders (
    id         BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT        NOT NULL,
    item_id    INT           NOT NULL,
    quantity   INT           NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    line_total DECIMAL(10,2) NOT NULL,
    ordered_at DATETIME      NOT NULL,
    CONSTRAINT fk_food_booking FOREIGN KEY (booking_id) REFERENCES bookings (id),
    CONSTRAINT fk_food_item FOREIGN KEY (item_id) REFERENCES menu_items (id)
) ENGINE=InnoDB;

INSERT IGNORE INTO room_types (code, name, price_per_day, capacity, description, sort_order) VALUES
    ('LUXURY_DOUBLE', 'Luxury Double', 4000.00, 2, '1 double bed, AC, free breakfast', 1),
    ('DELUXE_DOUBLE', 'Deluxe Double', 3000.00, 2, '1 double bed, non-AC, free breakfast', 2),
    ('LUXURY_SINGLE', 'Luxury Single', 2200.00, 1, '1 single bed, AC, free breakfast', 3),
    ('DELUXE_SINGLE', 'Deluxe Single', 1200.00, 1, '1 single bed, non-AC, free breakfast', 4);
