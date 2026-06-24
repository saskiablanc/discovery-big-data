# Discovery Big Data

English version (French version below 🇫🇷)

---

## 🇬🇧 About

Discovery Big Data is a hands-on project for exploring the big data ecosystem through Apache Kafka and Apache Spark. It implements a real-time data pipeline: synthetic user profiles are generated locally with DataFaker, streamed through Kafka, processed with Spark, stored in PostgreSQL, and pushed live to a web interface.

The project was built by a team of three.

## Architecture

The system is a real-time pipeline split across two independent Maven projects that communicate through PostgreSQL and HTTP.

```
DataFaker -> [PersonGenerator] -> Kafka -> [PersonConsumer] -> PostgreSQL -> [Spring Boot API] -> Frontend
             \________________ pipeline ________________/                    \_____ frontend _____/
```

Both components share the PostgreSQL database `kafkaspark` as their junction point, plus an HTTP call from the consumer to the API for real-time notifications.

| Folder | Role | Stack |
| --- | --- | --- |
| `pipeline/` | Kafka producer and Spark streaming consumer | Java, Spark, Kafka |
| `frontend/` | Spring Boot REST API and web interface | Java, Spring Boot, JPA |

## Components

### pipeline

A single Maven project containing two Spark jobs that share the `PersonEvent` model.

**PersonGenerator (producer)**

- Generates 1 million synthetic people with DataFaker
- Distributes them as an RDD with `sc.parallelize` (20 partitions)
- Builds `PersonEvent` objects (first name, last name, nationality, age, photo URL)
- Applies distributed Spark transformations: nationality normalization (title case) and deduplication on `firstName|lastName`
- Writes the result to the Kafka topic `persons` through `foreachPartition`, creating one `KafkaProducer` per partition rather than per row

**PersonConsumer (consumer, Spark Structured Streaming)**

- Continuously listens to the `persons` topic
- Deserializes the JSON into structured columns through an explicit schema (`from_json`)
- Applies a transformation (last name to upper case)
- For each micro-batch (`foreachBatch`): inserts into PostgreSQL and notifies the frontend

Kafka acts as the message bus between the two jobs, decoupling the generator from the consumer.

### frontend

A Spring Boot application (port 8082) connected to `kafkaspark` through Spring Data JPA, exposing three endpoints.

| Endpoint | Role |
| --- | --- |
| `GET /api/persons` | Returns the list of people as JSON (initial load) |
| `GET /api/persons/stream` | SSE stream: pushes new people in real time |
| `POST /api/persons/notify` | Received from the Spark consumer: triggers an SSE event |

The web interface (vanilla HTML/CSS/JS, served statically) displays people as cards, supports light and dark mode (localStorage), loads existing records on startup through `GET /api/persons`, then subscribes to the SSE stream.

### The real-time loop

This is the key mechanism linking the two components:

1. `PersonConsumer` inserts a person into the database
2. It immediately sends a `POST /api/persons/notify` to the Spring Boot API
3. `PersonService` relays the event to every connected SSE client (`SseEmitter`)
4. The browser receives the event and adds the card without reloading the page

## Tech stack

| Layer | Technologies |
| --- | --- |
| Streaming | Apache Kafka |
| Processing | Apache Spark (RDD and Structured Streaming) |
| Storage | PostgreSQL |
| API | Spring Boot, Spring Data JPA |
| Frontend | HTML, CSS, vanilla JavaScript, Server-Sent Events |
| Build | Maven |
| Data generation | DataFaker |

## Prerequisites

- Java 17
- Maven 3.8 or later
- Apache Kafka
- PostgreSQL
- Apache Spark (if running the jobs through `spark-submit`)

## Database setup

The pipeline expects a PostgreSQL database named `kafkaspark`, a user `postgres` (password `postgres`), and a `person` table. These parameters are hard-coded in `PersonConsumer.java` (`jdbc:postgresql://localhost:5432/kafkaspark`) and in the Spring Boot configuration.

### 1. Install PostgreSQL

| System | Commands |
| --- | --- |
| macOS (Homebrew) | `brew install postgresql@16` then `brew services start postgresql@16` |
| Debian / Ubuntu | `sudo apt install postgresql` then `sudo systemctl start postgresql` |
| Windows | Official installer from postgresql.org, then start the service |

### 2. Create the `postgres` role

Some installations (Homebrew in particular) create a superuser named after your system account rather than `postgres`, so create the expected role:

```sql
CREATE ROLE postgres WITH LOGIN SUPERUSER PASSWORD 'postgres';
```

Run it through `psql` (for example `psql postgres -c "..."` on macOS, or `sudo -u postgres psql -c "..."` on Linux).

### 3. Create the database

```bash
createdb -O postgres kafkaspark
```

### 4. Create the `person` table

Connect with `psql -U postgres -d kafkaspark`, then run:

```sql
CREATE TABLE person (
    id          BIGSERIAL    PRIMARY KEY,
    first_name  VARCHAR(50)  NOT NULL,
    last_name   VARCHAR(50)  NOT NULL,
    nationality VARCHAR(100) NOT NULL,
    age         INT          NOT NULL,
    picture_url VARCHAR(255) NOT NULL
);
```

Or apply the provided script directly:

```bash
psql -U postgres -d kafkaspark -f pipeline/src/main/resources/sql/create_person.sql
```

### 5. Verify

```bash
psql -U postgres -d kafkaspark -c "\dt"
```

The `person` table should appear.

## Running the project

Start the components in this order.

### 1. Start Kafka

Start Kafka (Zookeeper and broker, or KRaft mode), then create the topic:

```bash
kafka-topics.sh --create --topic persons --bootstrap-server localhost:9092
```

### 2. Start the frontend API

```bash
cd frontend
mvn spring-boot:run
```

The interface is available at `http://localhost:8082`.

### 3. Start the consumer

```bash
cd pipeline
mvn clean package
spark-submit --class com.demo.consumer.PersonConsumer target/kafka-spark-demo-1.0-SNAPSHOT.jar
```

If Spark is bundled in the jar (no separate Spark install), run instead: `java -cp target/kafka-spark-demo-1.0-SNAPSHOT.jar com.demo.consumer.PersonConsumer`.

### 4. Run the generator

In a separate terminal:

```bash
cd pipeline
spark-submit --class com.demo.generator.PersonGenerator target/kafka-spark-demo-1.0-SNAPSHOT.jar
```

New people appear in the web interface as cards, in real time. A `reset.sh` script in `pipeline/` resets the environment between runs.

---

## 🇫🇷 À propos

Discovery Big Data est un projet pour découvrir le monde de la big data à travers Apache Kafka et Apache Spark. Il met en place un pipeline de données temps réel : des profils d'utilisateurs synthétiques sont générés localement avec DataFaker, transportés via Kafka, traités avec Spark, stockés dans PostgreSQL, puis poussés en direct vers une interface web.

Le projet a été réalisé à trois.

## Architecture

Le système est un pipeline de données temps réel réparti sur deux projets Maven indépendants qui communiquent via PostgreSQL et HTTP.

```
DataFaker -> [PersonGenerator] -> Kafka -> [PersonConsumer] -> PostgreSQL -> [Spring Boot API] -> Frontend
             \________________ pipeline ________________/                    \_____ frontend _____/
```

Les deux composants partagent la base PostgreSQL `kafkaspark` comme point de jonction, plus un appel HTTP du consommateur vers l'API pour les notifications temps réel.

| Dossier | Rôle | Stack |
| --- | --- | --- |
| `pipeline/` | Producteur Kafka et consommateur Spark streaming | Java, Spark, Kafka |
| `frontend/` | API REST Spring Boot et interface web | Java, Spring Boot, JPA |

## Composants

### pipeline

Un seul projet Maven contenant deux jobs Spark qui partagent le modèle `PersonEvent`.

**PersonGenerator (producteur)**

- Génère 1 million de personnes synthétiques avec DataFaker
- Les distribue en RDD avec `sc.parallelize` (20 partitions)
- Construit des objets `PersonEvent` (prénom, nom, nationalité, âge, URL photo)
- Applique des transformations Spark distribuées : normalisation de la nationalité (title case) et déduplication sur `firstName|lastName`
- Envoie le résultat dans le topic Kafka `persons` via `foreachPartition`, en créant un seul `KafkaProducer` par partition plutôt que par ligne

**PersonConsumer (consommateur, Spark Structured Streaming)**

- Écoute en continu le topic `persons`
- Désérialise le JSON en colonnes structurées via un schéma explicite (`from_json`)
- Applique une transformation (nom de famille en majuscules)
- Pour chaque micro-batch (`foreachBatch`) : insère en PostgreSQL et notifie le frontend

Kafka joue le rôle de bus de messages entre les deux jobs, découplant le générateur du consommateur.

### frontend

Une application Spring Boot (port 8082) connectée à `kafkaspark` via Spring Data JPA, exposant trois endpoints.

| Endpoint | Rôle |
| --- | --- |
| `GET /api/persons` | Retourne la liste des personnes en JSON (chargement initial) |
| `GET /api/persons/stream` | Flux SSE : pousse les nouvelles personnes en temps réel |
| `POST /api/persons/notify` | Reçu depuis le consommateur Spark : déclenche un événement SSE |

L'interface web (HTML/CSS/JS vanilla, servie statiquement) affiche les personnes en cartes, supporte le mode clair et sombre (localStorage), charge l'existant au démarrage via `GET /api/persons`, puis s'abonne au flux SSE.

### La boucle temps réel

C'est le mécanisme clé qui relie les deux composants :

1. `PersonConsumer` insère une personne en base
2. Dans la foulée, il envoie un `POST /api/persons/notify` à l'API Spring Boot
3. `PersonService` relaie l'événement à tous les clients SSE connectés (`SseEmitter`)
4. Le navigateur reçoit l'événement et ajoute la carte sans recharger la page

## Stack technique

| Couche | Technologies |
| --- | --- |
| Streaming | Apache Kafka |
| Traitement | Apache Spark (RDD et Structured Streaming) |
| Stockage | PostgreSQL |
| API | Spring Boot, Spring Data JPA |
| Frontend | HTML, CSS, JavaScript vanilla, Server-Sent Events |
| Build | Maven |
| Génération de données | DataFaker |

## Prérequis

- Java 17
- Maven 3.8 ou plus récent
- Apache Kafka
- PostgreSQL
- Apache Spark (pour lancer les jobs via `spark-submit`)

## Mise en place de la base de données

Le pipeline attend une base PostgreSQL nommée `kafkaspark`, un utilisateur `postgres` (mot de passe `postgres`) et une table `person`. Ces paramètres sont codés en dur dans `PersonConsumer.java` (`jdbc:postgresql://localhost:5432/kafkaspark`) et dans la configuration Spring Boot.

### 1. Installer PostgreSQL

| Système | Commandes |
| --- | --- |
| macOS (Homebrew) | `brew install postgresql@16` puis `brew services start postgresql@16` |
| Debian / Ubuntu | `sudo apt install postgresql` puis `sudo systemctl start postgresql` |
| Windows | Installeur officiel sur postgresql.org, puis démarrer le service |

### 2. Créer le rôle `postgres`

Certaines installations (Homebrew notamment) créent un superutilisateur au nom de votre compte système plutôt que `postgres`. On crée donc le rôle attendu :

```sql
CREATE ROLE postgres WITH LOGIN SUPERUSER PASSWORD 'postgres';
```

À exécuter via `psql` (par exemple `psql postgres -c "..."` sur macOS, ou `sudo -u postgres psql -c "..."` sur Linux).

### 3. Créer la base

```bash
createdb -O postgres kafkaspark
```

### 4. Créer la table `person`

Connectez-vous avec `psql -U postgres -d kafkaspark`, puis exécutez :

```sql
CREATE TABLE person (
    id          BIGSERIAL    PRIMARY KEY,
    first_name  VARCHAR(50)  NOT NULL,
    last_name   VARCHAR(50)  NOT NULL,
    nationality VARCHAR(100) NOT NULL,
    age         INT          NOT NULL,
    picture_url VARCHAR(255) NOT NULL
);
```

Ou appliquez directement le script fourni :

```bash
psql -U postgres -d kafkaspark -f pipeline/src/main/resources/sql/create_person.sql
```

### 5. Vérifier

```bash
psql -U postgres -d kafkaspark -c "\dt"
```

La table `person` doit apparaître.

## Lancer le projet

Démarrez les composants dans cet ordre.

### 1. Démarrer Kafka

Lancez Kafka (Zookeeper et broker, ou mode KRaft), puis créez le topic :

```bash
kafka-topics.sh --create --topic persons --bootstrap-server localhost:9092
```

### 2. Démarrer l'API frontend

```bash
cd frontend
mvn spring-boot:run
```

L'interface est disponible sur `http://localhost:8082`.

### 3. Démarrer le consommateur

```bash
cd pipeline
mvn clean package
spark-submit --class com.demo.consumer.PersonConsumer target/kafka-spark-demo-1.0-SNAPSHOT.jar
```

Si Spark est inclus dans le jar (pas d'installation Spark séparée), lancez plutôt : `java -cp target/kafka-spark-demo-1.0-SNAPSHOT.jar com.demo.consumer.PersonConsumer`.

### 4. Lancer le générateur

Dans un terminal séparé :

```bash
cd pipeline
spark-submit --class com.demo.generator.PersonGenerator target/kafka-spark-demo-1.0-SNAPSHOT.jar
```

Les nouvelles personnes apparaissent dans l'interface web sous forme de cartes, en temps réel. Un script `reset.sh` dans `pipeline/` réinitialise l'environnement entre deux exécutions.
