# Database Management Systems Comparison

## Prerequisites

- [Docker](https://www.docker.com/get-started)
- [Docker Compose](https://docs.docker.com/compose/install/)

## Setup

1. Clone Repository:
```sh
git clone https://github.com/jakub-gorzkowski/dbms_comparison.git
```

2. Create the following `.env` file within the project directory:
```env
# Credentials
DATABASE=your_database_name
USER=your_username
PASSWORD=strong_password
ROOT_PASSWORD=root_password

# Inner ports
POSTGRES_INNER_PORT=your_postgres_port
MYSQL_INNER_PORT=your_mysql_port
MONGO_INNER_PORT=your_mongo_port
DYNAMO_INNER_PORT=your_dynamo_port

# Outer ports
POSTGRES_OUTER_PORT=your_postgres_port
MYSQL_OUTER_PORT=your_mysql_port
MONGO_OUTER_PORT=your_mongo_port
DYNAMO_OUTER_PORT=your_dynamo_port
```

3. Build images:
```sh
docker compose build
```

4. Start containers:
```sh
docker compose up -d
```

5. Stop and remove containers:
```sh
docker compose down
```
