const dbName = "straz_graniczna";
db = db.getSiblingDB(dbName);

db.createCollection("oddzialy_strazy_granicznej");
db.createCollection("rodzaje_dokumentow");
db.createCollection("rodzaje_przejsc");
db.createCollection("osoby");
db.createCollection("placowki_strazy_granicznej");
db.createCollection("pracownicy");
db.createCollection("przejscia_graniczne");
db.createCollection("kontrole_graniczne");

db.oddzialy_strazy_granicznej.createIndex({ "nazwa": 1 }, { unique: true });
db.rodzaje_dokumentow.createIndex({ "nazwa": 1 }, { unique: true });
db.rodzaje_przejsc.createIndex({ "nazwa": 1 }, { unique: true });
db.placowki_strazy_granicznej.createIndex({ "nazwa": 1 }, { unique: true });
db.przejscia_graniczne.createIndex({ "nazwa": 1 }, { unique: true });

const oddzialy = [
    { nazwa: "Bieszczadzki Oddział SG" },
    { nazwa: "Nadwiślański Oddział SG" },
    { nazwa: "Podlaski Oddział SG" }
];
db.oddzialy_strazy_granicznej.insertMany(oddzialy);

const dokumenty = [
    { nazwa: "Paszport" },
    { nazwa: "Dowód osobisty" },
    { nazwa: "Wiza" },
    { nazwa: "Karta pobytu" }
];
db.rodzaje_dokumentow.insertMany(dokumenty);

const rodzaje_przejsc = [
    { nazwa: "Drogowe" },
    { nazwa: "Kolejowe" },
    { nazwa: "Lotnicze" },
    { nazwa: "Morskie" }
];
db.rodzaje_przejsc.insertMany(rodzaje_przejsc);

const oddzialBieszczadzki = db.oddzialy_strazy_granicznej.findOne({ nazwa: "Bieszczadzki Oddział SG" })._id;
const oddzialNadwislanski = db.oddzialy_strazy_granicznej.findOne({ nazwa: "Nadwiślański Oddział SG" })._id;

const placowki = [
    { nazwa: "Placówka SG w Medyce", oddzial_id: oddzialBieszczadzki },
    { nazwa: "Placówka SG w Korczowej", oddzial_id: oddzialBieszczadzki },
    { nazwa: "Placówka SG Warszawa-Okęcie", oddzial_id: oddzialNadwislanski }
];
db.placowki_strazy_granicznej.insertMany(placowki);

const placowkaMedyka = db.placowki_strazy_granicznej.findOne({ nazwa: "Placówka SG w Medyce" })._id;
const placowkaOkecie = db.placowki_strazy_granicznej.findOne({ nazwa: "Placówka SG Warszawa-Okęcie" })._id;

const pracownicy = [
    { imie: "Jan", nazwisko: "Kowalski", stopien: "Kapitan", placowka_id: placowkaMedyka },
    { imie: "Anna", nazwisko: "Nowak", stopien: "Porucznik", placowka_id: placowkaMedyka },
    { imie: "Piotr", nazwisko: "Wiśniewski", stopien: "Major", placowka_id: placowkaOkecie }
];
db.pracownicy.insertMany(pracownicy);

const przejscia = [
    { nazwa: "Medyka-Szeginie", granica: "PL-UA", placowka_id: placowkaMedyka },
    { nazwa: "Lotnisko Chopina", granica: "PL-INT", placowka_id: placowkaOkecie }
];
db.przejscia_graniczne.insertMany(przejscia);

const przejscieMedyka = db.przejscia_graniczne.findOne({ nazwa: "Medyka-Szeginie" })._id;
const przejscieOkecie = db.przejscia_graniczne.findOne({ nazwa: "Lotnisko Chopina" })._id;

const rodzajDrogowe = db.rodzaje_przejsc.findOne({ nazwa: "Drogowe" })._id;
const rodzajLotnicze = db.rodzaje_przejsc.findOne({ nazwa: "Lotnicze" })._id;

db.przejscia_graniczne.updateOne(
    { _id: przejscieMedyka },
    { $set: { rodzaje: [rodzajDrogowe] } }
);
db.przejscia_graniczne.updateOne(
    { _id: przejscieOkecie },
    { $set: { rodzaje: [rodzajLotnicze] } }
);

const osoby = [
    { imie: "Adam", nazwisko: "Mickiewicz", narodowosc: "polska" },
    { imie: "John", nazwisko: "Smith", narodowosc: "amerykańska" },
    { imie: "Olena", nazwisko: "Petrenko", narodowosc: "ukraińska" }
];
db.osoby.insertMany(osoby);

const osobaAdam = db.osoby.findOne({ nazwisko: "Mickiewicz" })._id;
const osobaJohn = db.osoby.findOne({ nazwisko: "Smith" })._id;
const osobaOlena = db.osoby.findOne({ nazwisko: "Petrenko" })._id;

const paszport = db.rodzaje_dokumentow.findOne({ nazwa: "Paszport" })._id;
const dowod = db.rodzaje_dokumentow.findOne({ nazwa: "Dowód osobisty" })._id;

const kontrole = [
    {
        przejscie_id: przejscieMedyka,
        oddzial_id: oddzialBieszczadzki,
        data: new Date("2023-12-01"),
        kto: "Jan Kowalski",
        kierunek: "wyjazd",
        osoba_id: osobaAdam,
        dokument_id: dowod
    },
    {
        przejscie_id: przejscieOkecie,
        oddzial_id: oddzialNadwislanski,
        data: new Date("2023-12-02"),
        kto: "Piotr Wiśniewski",
        kierunek: "wjazd",
        osoba_id: osobaJohn,
        dokument_id: paszport
    },
    {
        przejscie_id: przejscieMedyka,
        oddzial_id: oddzialBieszczadzki,
        data: new Date("2023-12-03"),
        kto: "Anna Nowak",
        kierunek: "wjazd",
        osoba_id: osobaOlena,
        dokument_id: paszport
    }
];
db.kontrole_graniczne.insertMany(kontrole);
