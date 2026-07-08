# Geoportail RESINA - Backend

Backend API du Géoportail RESINA pour Décideurs (ANPTIC Burkina Faso).

## Technologies utilisées

- **Java 17**
- **Spring Boot 3.3.4**
  - Spring Web (API REST)
  - Spring Data JPA (persistance)
  - Spring Security (authentification & autorisation)
- **PostgreSQL** (base de données)
- **Lombok** (réduction du code boilerplate)
- **Maven** (gestion des dépendances)

## Prérequis

Avant de lancer le projet, assurez-vous d'avoir installé :

| Outil | Version | Lien de téléchargement |
|-------|---------|------------------------|
| JDK | 17 ou supérieur | [Adoptium](https://adoptium.net/) |
| Maven | 3.8+ | [Maven](https://maven.apache.org/download.cgi) |
| PostgreSQL | 14+ | [PostgreSQL](https://www.postgresql.org/download/) |

> **Note IDE** : Si vous utilisez IntelliJ IDEA ou Eclipse, installez le plugin **Lombok** et activez l'annotation processing, sinon le projet ne compilera pas dans l'IDE.

## Installation et exécution

### 1. Cloner le projet

```bash
git clone https://github.com/K-Hamande/GeoPortailBackend.git
cd geoportail-resina-backend
```

### 2. Créer la base de données PostgreSQL

```sql
CREATE DATABASE geoportail_resina;
```

### 3. Configurer la connexion à la base de données

Modifier le fichier `src/main/resources/application.properties` avec vos identifiants :

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/geoportail_resina
spring.datasource.username=postgres
spring.datasource.password=votre_mot_de_passe

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

### 4. Installer les dépendances et lancer l'application

```bash
mvn clean install
mvn spring-boot:run
```

> Toutes les dépendances (Spring Web, JPA, Security, Lombok, driver PostgreSQL...)
> sont gérées automatiquement par Maven via le fichier `pom.xml`.
> Aucune installation manuelle de dépendance n'est nécessaire.

L'application démarre sur : **http://localhost:8080**

## Sécurité

Le projet utilise **Spring Security**. Par défaut, tous les endpoints sont protégés.
Consultez la configuration de sécurité du projet pour connaître les identifiants
ou le mécanisme d'authentification utilisé (JWT, session, etc.).

## Tests

```bash
mvn test
```

## Structure du projet

```
geoportail-resina-backend/
├── src/
│   ├── main/
│   │   ├── java/bf/anptic/...        # Code source
│   │   └── resources/
│   │       └── application.properties # Configuration
│   └── test/                          # Tests unitaires
├── pom.xml                            # Dépendances Maven
└── README.md
```

## Auteur

**ANPTIC** - Agence Nationale de Promotion des TIC (Burkina Faso)

---

*Projet : Geoportail RESINA - Version 1.0.0*