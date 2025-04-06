import boto3
import time
from datetime import datetime

dynamodb = boto3.resource('dynamodb',
                          endpoint_url='http://dynamodb:8000',
                          region_name='dummy',
                          aws_access_key_id='dummy',
                          aws_secret_access_key='dummy')

table_name = 'BorderControl'

existing_tables = dynamodb.meta.client.list_tables()['TableNames']
if table_name in existing_tables:
    table = dynamodb.Table(table_name)
    table.delete()
    time.sleep(5)

response = dynamodb.create_table(
    TableName=table_name,
    KeySchema=[
        {
            'AttributeName': 'PK',
            'KeyType': 'HASH'
        },
        {
            'AttributeName': 'SK',
            'KeyType': 'RANGE'
        }
    ],
    AttributeDefinitions=[
        {
            'AttributeName': 'PK',
            'AttributeType': 'S'
        },
        {
            'AttributeName': 'SK',
            'AttributeType': 'S'
        },
        {
            'AttributeName': 'GSI1PK',
            'AttributeType': 'S'
        },
        {
            'AttributeName': 'GSI1SK',
            'AttributeType': 'S'
        }
    ],
    GlobalSecondaryIndexes=[
        {
            'IndexName': 'GSI1',
            'KeySchema': [
                {
                    'AttributeName': 'GSI1PK',
                    'KeyType': 'HASH'
                },
                {
                    'AttributeName': 'GSI1SK',
                    'KeyType': 'RANGE'
                }
            ],
            'Projection': {
                'ProjectionType': 'ALL'
            },
            'ProvisionedThroughput': {
                'ReadCapacityUnits': 5,
                'WriteCapacityUnits': 5
            }
        }
    ],
    ProvisionedThroughput={
        'ReadCapacityUnits': 5,
        'WriteCapacityUnits': 5
    }
)

table = dynamodb.Table(table_name)
table.meta.client.get_waiter('table_exists').wait(TableName=table_name)

def put_item(item):
    table.put_item(Item=item)

oddzialy = [
    {
        'PK': 'UNIT#1',
        'SK': 'UNIT#1',
        'type': 'UNIT',
        'nazwa': 'Bieszczadzki Oddział SG',
        'GSI1PK': 'UNIT#1',
        'GSI1SK': 'UNIT#1'
    },
    {
        'PK': 'UNIT#2',
        'SK': 'UNIT#2',
        'type': 'UNIT',
        'nazwa': 'Nadwiślański Oddział SG',
        'GSI1PK': 'UNIT#2',
        'GSI1SK': 'UNIT#2'
    },
    {
        'PK': 'UNIT#3',
        'SK': 'UNIT#3',
        'type': 'UNIT',
        'nazwa': 'Podlaski Oddział SG',
        'GSI1PK': 'UNIT#3',
        'GSI1SK': 'UNIT#3'
    }
]

for oddzial in oddzialy:
    put_item(oddzial)

placowki = [
    {
        'PK': 'UNIT#1',
        'SK': 'OUTPOST#1',
        'type': 'OUTPOST',
        'nazwa': 'Placówka SG w Medyce',
        'GSI1PK': 'OUTPOST#1',
        'GSI1SK': 'OUTPOST#1'
    },
    {
        'PK': 'UNIT#1',
        'SK': 'OUTPOST#2',
        'type': 'OUTPOST',
        'nazwa': 'Placówka SG w Korczowej',
        'GSI1PK': 'OUTPOST#2',
        'GSI1SK': 'OUTPOST#2'
    },
    {
        'PK': 'UNIT#2',
        'SK': 'OUTPOST#3',
        'type': 'OUTPOST',
        'nazwa': 'Placówka SG Warszawa-Okęcie',
        'GSI1PK': 'OUTPOST#3',
        'GSI1SK': 'OUTPOST#3'
    }
]

for placowka in placowki:
    put_item(placowka)

dokumenty = [
    {
        'PK': 'DOC_TYPE#1',
        'SK': 'DOC_TYPE#1',
        'type': 'DOC_TYPE',
        'nazwa': 'Paszport',
        'GSI1PK': 'DOC_TYPE#1',
        'GSI1SK': 'DOC_TYPE#1'
    },
    {
        'PK': 'DOC_TYPE#2',
        'SK': 'DOC_TYPE#2',
        'type': 'DOC_TYPE',
        'nazwa': 'Dowód osobisty',
        'GSI1PK': 'DOC_TYPE#2',
        'GSI1SK': 'DOC_TYPE#2'
    },
    {
        'PK': 'DOC_TYPE#3',
        'SK': 'DOC_TYPE#3',
        'type': 'DOC_TYPE',
        'nazwa': 'Wiza',
        'GSI1PK': 'DOC_TYPE#3',
        'GSI1SK': 'DOC_TYPE#3'
    }
]

for dokument in dokumenty:
    put_item(dokument)

rodzaje_przejsc = [
    {
        'PK': 'CROSSING_TYPE#1',
        'SK': 'CROSSING_TYPE#1',
        'type': 'CROSSING_TYPE',
        'nazwa': 'Drogowe',
        'GSI1PK': 'CROSSING_TYPE#1',
        'GSI1SK': 'CROSSING_TYPE#1'
    },
    {
        'PK': 'CROSSING_TYPE#2',
        'SK': 'CROSSING_TYPE#2',
        'type': 'CROSSING_TYPE',
        'nazwa': 'Kolejowe',
        'GSI1PK': 'CROSSING_TYPE#2',
        'GSI1SK': 'CROSSING_TYPE#2'
    },
    {
        'PK': 'CROSSING_TYPE#3',
        'SK': 'CROSSING_TYPE#3',
        'type': 'CROSSING_TYPE',
        'nazwa': 'Lotnicze',
        'GSI1PK': 'CROSSING_TYPE#3',
        'GSI1SK': 'CROSSING_TYPE#3'
    }
]

for rodzaj in rodzaje_przejsc:
    put_item(rodzaj)

pracownicy = [
    {
        'PK': 'OUTPOST#1',
        'SK': 'EMPLOYEE#1',
        'type': 'EMPLOYEE',
        'imie': 'Jan',
        'nazwisko': 'Kowalski',
        'stopien': 'Kapitan'
    },
    {
        'PK': 'OUTPOST#1',
        'SK': 'EMPLOYEE#2',
        'type': 'EMPLOYEE',
        'imie': 'Anna',
        'nazwisko': 'Nowak',
        'stopien': 'Porucznik'
    },
    {
        'PK': 'OUTPOST#3',
        'SK': 'EMPLOYEE#3',
        'type': 'EMPLOYEE',
        'imie': 'Piotr',
        'nazwisko': 'Wiśniewski',
        'stopien': 'Major'
    }
]

for pracownik in pracownicy:
    put_item(pracownik)

przejscia = [
    {
        'PK': 'OUTPOST#1',
        'SK': 'BORDER_CROSSING#1',
        'type': 'BORDER_CROSSING',
        'nazwa': 'Medyka-Szeginie',
        'granica': 'PL-UA',
        'GSI1PK': 'BORDER_CROSSING#1',
        'GSI1SK': 'BORDER_CROSSING#1'
    },
    {
        'PK': 'OUTPOST#3',
        'SK': 'BORDER_CROSSING#2',
        'type': 'BORDER_CROSSING',
        'nazwa': 'Lotnisko Chopina',
        'granica': 'PL-INT',
        'GSI1PK': 'BORDER_CROSSING#2',
        'GSI1SK': 'BORDER_CROSSING#2'
    }
]

for przejscie in przejscia:
    put_item(przejscie)

przejscia_rodzaje = [
    {
        'PK': 'BORDER_CROSSING#1',
        'SK': 'CROSSING_TYPE#1',
        'type': 'CROSSING_TYPE_REL',
    },
    {
        'PK': 'BORDER_CROSSING#2',
        'SK': 'CROSSING_TYPE#3',
        'type': 'CROSSING_TYPE_REL',
    }
]

for rel in przejscia_rodzaje:
    put_item(rel)

osoby = [
    {
        'PK': 'PERSON#1',
        'SK': 'PERSON#1',
        'type': 'PERSON',
        'imie': 'Adam',
        'nazwisko': 'Mickiewicz',
        'narodowosc': 'polska',
        'GSI1PK': 'PERSON#1',
        'GSI1SK': 'PERSON#1'
    },
    {
        'PK': 'PERSON#2',
        'SK': 'PERSON#2',
        'type': 'PERSON',
        'imie': 'John',
        'nazwisko': 'Smith',
        'narodowosc': 'amerykańska',
        'GSI1PK': 'PERSON#2',
        'GSI1SK': 'PERSON#2'
    },
    {
        'PK': 'PERSON#3',
        'SK': 'PERSON#3',
        'type': 'PERSON',
        'imie': 'Olena',
        'nazwisko': 'Petrenko',
        'narodowosc': 'ukraińska',
        'GSI1PK': 'PERSON#3',
        'GSI1SK': 'PERSON#3'
    }
]

for osoba in osoby:
    put_item(osoba)

kontrole = [
    {
        'PK': 'BORDER_CROSSING#1',
        'SK': 'CONTROL#2023-12-01#1',
        'type': 'CONTROL',
        'oddzial_id': 'UNIT#1',
        'data': '2023-12-01',
        'kto': 'Jan Kowalski',
        'kierunek': 'wyjazd',
        'osoba_id': 'PERSON#1',
        'dokument_id': 'DOC_TYPE#2',
        'GSI1PK': 'CONTROL#2023-12-01',
        'GSI1SK': 'BORDER_CROSSING#1'
    },
    {
        'PK': 'BORDER_CROSSING#2',
        'SK': 'CONTROL#2023-12-02#1',
        'type': 'CONTROL',
        'oddzial_id': 'UNIT#2',
        'data': '2023-12-02',
        'kto': 'Piotr Wiśniewski',
        'kierunek': 'wjazd',
        'osoba_id': 'PERSON#2',
        'dokument_id': 'DOC_TYPE#1',
        'GSI1PK': 'CONTROL#2023-12-02',
        'GSI1SK': 'BORDER_CROSSING#2'
    },
    {
        'PK': 'BORDER_CROSSING#1',
        'SK': 'CONTROL#2023-12-03#1',
        'type': 'CONTROL',
        'oddzial_id': 'UNIT#1',
        'data': '2023-12-03',
        'kto': 'Anna Nowak',
        'kierunek': 'wjazd',
        'osoba_id': 'PERSON#3',
        'dokument_id': 'DOC_TYPE#1',
        'GSI1PK': 'CONTROL#2023-12-03',
        'GSI1SK': 'BORDER_CROSSING#1'
    }
]

for kontrola in kontrole:
    put_item(kontrola)

osoba_kontrole = [
    {
        'PK': 'PERSON#1',
        'SK': 'CONTROL#2023-12-01#1',
        'type': 'PERSON_CONTROL',
        'przejscie_id': 'BORDER_CROSSING#1',
        'data': '2023-12-01',
        'kierunek': 'wyjazd'
    },
    {
        'PK': 'PERSON#2',
        'SK': 'CONTROL#2023-12-02#1',
        'type': 'PERSON_CONTROL',
        'przejscie_id': 'BORDER_CROSSING#2',
        'data': '2023-12-02',
        'kierunek': 'wjazd'
    },
    {
        'PK': 'PERSON#3',
        'SK': 'CONTROL#2023-12-03#1',
        'type': 'PERSON_CONTROL',
        'przejscie_id': 'BORDER_CROSSING#1',
        'data': '2023-12-03',
        'kierunek': 'wjazd'
    }
]

for rel in osoba_kontrole:
    put_item(rel)
