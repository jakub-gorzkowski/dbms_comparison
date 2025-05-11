CREATE TABLE oddzialy_strazy_granicznej (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nazwa VARCHAR(255) NOT NULL,
    UNIQUE KEY (nazwa)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE rodzaje_dokumentow (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nazwa VARCHAR(100) NOT NULL,
    UNIQUE KEY (nazwa)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE rodzaje_przejsc (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nazwa VARCHAR(100) NOT NULL,
    UNIQUE KEY (nazwa)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE osoby (
    id INT AUTO_INCREMENT PRIMARY KEY,
    imie VARCHAR(100) NOT NULL,
    nazwisko VARCHAR(100) NOT NULL,
    narodowosc VARCHAR(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE placowki_strazy_granicznej (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nazwa VARCHAR(255) NOT NULL,
    oddzial_id INT NOT NULL,
    UNIQUE KEY (nazwa),
    FOREIGN KEY (oddzial_id) REFERENCES oddzialy_strazy_granicznej(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE pracownicy (
    id INT AUTO_INCREMENT PRIMARY KEY,
    imie VARCHAR(100) NOT NULL,
    nazwisko VARCHAR(100) NOT NULL,
    stopien VARCHAR(50),
    placowka_id INT NOT NULL,
    FOREIGN KEY (placowka_id) REFERENCES placowki_strazy_granicznej(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE przejscia_graniczne (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nazwa VARCHAR(255) NOT NULL,
    granica VARCHAR(100) NOT NULL,
    placowka_id INT NOT NULL,
    UNIQUE KEY (nazwa),
    FOREIGN KEY (placowka_id) REFERENCES placowki_strazy_granicznej(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE kontrole_graniczne (
    id INT AUTO_INCREMENT PRIMARY KEY,
    przejscie_id INT NOT NULL,
    oddzial_id INT NOT NULL,
    data DATE NOT NULL,
    kto VARCHAR(255) NOT NULL,
    kierunek VARCHAR(50) NOT NULL,
    osoba_id INT NOT NULL,
    dokument_id INT NOT NULL,
    FOREIGN KEY (przejscie_id) REFERENCES przejscia_graniczne(id),
    FOREIGN KEY (oddzial_id) REFERENCES oddzialy_strazy_granicznej(id),
    FOREIGN KEY (osoba_id) REFERENCES osoby(id) ON DELETE CASCADE,
    FOREIGN KEY (dokument_id) REFERENCES rodzaje_dokumentow(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE przejscia_rodzaje (
    przejscie_id INT NOT NULL,
    rodzaj_id INT NOT NULL,
    PRIMARY KEY (przejscie_id, rodzaj_id),
    FOREIGN KEY (przejscie_id) REFERENCES przejscia_graniczne(id) ON DELETE CASCADE,
    FOREIGN KEY (rodzaj_id) REFERENCES rodzaje_przejsc(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
