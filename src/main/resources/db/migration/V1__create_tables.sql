CREATE TABLE IF NOT EXISTS sector (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sector VARCHAR(10) NOT NULL UNIQUE,
    base_price DECIMAL(10,2) NOT NULL,
    capacity INT NOT NULL,
    occupied_count INT NOT NULL
);

CREATE TABLE IF NOT EXISTS parking_spot (
    id BIGINT PRIMARY KEY,
    sector VARCHAR(10) NOT NULL,
    lat DOUBLE NOT NULL,
    lng DOUBLE NOT NULL,
    occupied BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (sector) REFERENCES sector(sector)
);

CREATE TABLE IF NOT EXISTS parking_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    license_plate VARCHAR(20) NOT NULL,
    spot_id BIGINT,
    entry_time DATETIME NOT NULL,
    exit_time DATETIME NULL,
    charged_amount DECIMAL(10,2) DEFAULT 0.00,
    sector VARCHAR(10),
    FOREIGN KEY (spot_id) REFERENCES parking_spot(id)
);

CREATE INDEX idx_license_plate ON parking_session(license_plate);