CREATE TABLE oddzialy_strazy_granicznej (
    id SERIAL PRIMARY KEY,
    nazwa VARCHAR(255) NOT NULL,
    UNIQUE (nazwa)
);

CREATE TABLE rodzaje_dokumentow (
    id SERIAL PRIMARY KEY,
    nazwa VARCHAR(100) NOT NULL,
    UNIQUE (nazwa)
);

CREATE TABLE rodzaje_przejsci (
    id SERIAL PRIMARY KEY,
    nazwa VARCHAR(100) NOT NULL,
    UNIQUE (nazwa)
);

CREATE TABLE osoby (
    id SERIAL PRIMARY KEY,
    imie VARCHAR(100) NOT NULL,
    nazwisko VARCHAR(100) NOT NULL,
    narodowosc VARCHAR(100) NOT NULL
);

CREATE TABLE placowki_strazy_granicznej (
    id SERIAL PRIMARY KEY,
    nazwa VARCHAR(255) NOT NULL,
    oddzial_id INT NOT NULL,
    UNIQUE (nazwa),
    FOREIGN KEY (oddzial_id) REFERENCES oddzialy_strazy_granicznej(id)
);

CREATE TABLE pracownicy (
    id SERIAL PRIMARY KEY,
    imie VARCHAR(100) NOT NULL,
    nazwisko VARCHAR(100) NOT NULL,
    stopien VARCHAR(50),
    placowka_id INT NOT NULL,
    FOREIGN KEY (placowka_id) REFERENCES placowki_strazy_granicznej(id) ON DELETE CASCADE
);

CREATE TABLE przejscia_graniczne (
    id SERIAL PRIMARY KEY,
    nazwa VARCHAR(255) NOT NULL,
    granica VARCHAR(100) NOT NULL,
    placowka_id INT NOT NULL,
    UNIQUE (nazwa),
    FOREIGN KEY (placowka_id) REFERENCES placowki_strazy_granicznej(id)
);

CREATE TABLE kontrole_graniczne (
    id SERIAL PRIMARY KEY,
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
);

CREATE TABLE przejscia_rodzaje (
    przejscie_id INT NOT NULL,
    rodzaj_id INT NOT NULL,
    PRIMARY KEY (przejscie_id, rodzaj_id),
    FOREIGN KEY (przejscie_id) REFERENCES przejscia_graniczne(id) ON DELETE CASCADE,
    FOREIGN KEY (rodzaj_id) REFERENCES rodzaje_przejsci(id) ON DELETE CASCADE
);
