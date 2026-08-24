-- Schema initial GéoPortail RESINA
-- Flyway execute ce script automatiquement au premier demarrage
-- sur tout nouvel environnement (dev, recette, production).
-- Ne pas modifier ce fichier apres qu'il a ete applique :
-- creer un V2__... pour les evolutions.

CREATE TABLE IF NOT EXISTS sites (
    site_id                 VARCHAR(255) PRIMARY KEY,
    nom                     VARCHAR(255),
    ville                   VARCHAR(255),
    province                VARCHAR(255),
    region_administrative   VARCHAR(255),
    batiment                VARCHAR(255),
    latitude                DOUBLE PRECISION,
    longitude               DOUBLE PRECISION,
    contact_dsi_nom         VARCHAR(255),
    contact_dsi_telephone   VARCHAR(255),
    contact_dsi_email       VARCHAR(255),
    netxms_node_id          INTEGER,
    niveaux                 INTEGER,
    actif                   BOOLEAN,
    info_au_survol          VARCHAR(255),
    ministere               VARCHAR(255),
    structure               VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS admin_users (
    id                  BIGSERIAL PRIMARY KEY,
    login               VARCHAR(255) NOT NULL UNIQUE,
    nom_complet         VARCHAR(255),
    mot_de_passe_hash   VARCHAR(255) NOT NULL,
    role                VARCHAR(255) NOT NULL,
    actif               BOOLEAN,
    cree_le             TIMESTAMP
);

CREATE TABLE IF NOT EXISTS admin_user_sites (
    admin_user_id   BIGINT NOT NULL REFERENCES admin_users(id),
    site_id         VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS audit_log_entries (
    id           BIGSERIAL PRIMARY KEY,
    auteur       VARCHAR(255),
    action       VARCHAR(255),
    details      VARCHAR(1000),
    horodatage   TIMESTAMP
);

CREATE TABLE IF NOT EXISTS equipments (
    id                     BIGSERIAL PRIMARY KEY,
    site_id                VARCHAR(255) REFERENCES sites(site_id),
    etage_label            VARCHAR(255),
    type                   VARCHAR(255),
    libelle_affiche        VARCHAR(255),
    nom_technique_netxms   VARCHAR(255),
    netxms_object_id       INTEGER,
    -- "ANPTIC" = equipement du reseau WAN (liaison nationale RESINA)
    -- null ou autre = equipement du reseau local du batiment (LAN)
    -- Source : colonne propriete de public.geo_equipement dans netxmsdb
    propriete              VARCHAR(255),
    CONSTRAINT equipments_type_check CHECK (type IN (
        'BORNE_WIFI','COMMUTATEUR','ROUTEUR','PTP','PMP',
        'CPE','ONDULEUR','SERVEUR','PYLONE','AUTRE'
    ))
);

CREATE TABLE IF NOT EXISTS notification_tokens (
    id              BIGSERIAL PRIMARY KEY,
    site_id         VARCHAR(255) REFERENCES sites(site_id),
    profil          VARCHAR(255),
    plateforme      VARCHAR(255),
    token           VARCHAR(255) UNIQUE,
    actif           BOOLEAN,
    enregistre_le   TIMESTAMP
);

-- Ancien systeme de liens securises par ministere.
-- Conserve pour compatibilite historique.
-- Remplace par le systeme JWT decideur_users.
CREATE TABLE IF NOT EXISTS ministry_access_tokens (
    id          BIGSERIAL PRIMARY KEY,
    token       VARCHAR(64) UNIQUE NOT NULL,
    ministere   VARCHAR(255) NOT NULL,
    libelle     VARCHAR(255),
    actif       BOOLEAN DEFAULT true,
    cree_le     TIMESTAMP,
    cree_par    VARCHAR(255)
);

-- Parametres de supervision par site (§3.2.6b du CDC)
-- intervalle_actualisations : nom exact attendu par Hibernate
-- (annotation @Column(name="intervalle_actualisation_s") dans l'entite)
CREATE TABLE IF NOT EXISTS site_supervision_settings (
    site_id                     VARCHAR(255) PRIMARY KEY REFERENCES sites(site_id),
    intervalle_actualisation_s   INTEGER,
    debit_minimal_mbps          DOUBLE PRECISION,
    latence_maximale_ms         DOUBLE PRECISION,
    notifications_actives       BOOLEAN,
    notif_panne_anptic          BOOLEAN,
    notif_panne_lan             BOOLEAN,
    notif_retablissement        BOOLEAN,
    modifie_le                  TIMESTAMP,
    modifie_par                 VARCHAR(255)
);

-- Comptes utilisateurs cote decideur (authentification JWT).
-- Deux roles : DECIDEUR (acces filtre par ministere) et
-- LAMBDA (consultation uniquement, statut global 🟢/🔴, tous les sites).
CREATE TABLE IF NOT EXISTS decideur_users (
    id                  BIGSERIAL PRIMARY KEY,
    login               VARCHAR(255) NOT NULL UNIQUE,
    nom_complet         VARCHAR(255),
    mot_de_passe_hash   VARCHAR(255) NOT NULL,
    role                VARCHAR(20) NOT NULL,   -- 'DECIDEUR' ou 'LAMBDA'
    ministere           VARCHAR(255),           -- null si role = LAMBDA
    actif               BOOLEAN DEFAULT true,
    cree_le             TIMESTAMP,
    cree_par            VARCHAR(255)
);

-- Destinataires email pour les alertes par site.
-- Un destinataire peut etre rattache a plusieurs sites.
-- (Fonctionnalite notifications email - a implementer)
CREATE TABLE IF NOT EXISTS email_destinataire (
    id          BIGSERIAL PRIMARY KEY,
    site_id     VARCHAR(255) REFERENCES sites(site_id),
    email       VARCHAR(255) NOT NULL,
    nom         VARCHAR(255),
    actif       BOOLEAN DEFAULT true,
    cree_le     TIMESTAMP,
    cree_par    VARCHAR(255)
);

-- Incidents actifs : un seul incident ouvert par site + type.
-- Ferme automatiquement quand le statut revient a OK.
-- Sert a detecter les transitions (ouverture/fermeture) pour
-- l'envoi des emails d'alerte (1 email a l'ouverture + 1 a la resolution).
-- (Fonctionnalite historique des incidents - a implementer)
CREATE TABLE IF NOT EXISTS incident_actif (
    id                          BIGSERIAL PRIMARY KEY,
    site_id                     VARCHAR(255) NOT NULL REFERENCES sites(site_id),
    type_incident               VARCHAR(20) NOT NULL,  -- 'ANPTIC' ou 'LAN'
    statut                      VARCHAR(20) NOT NULL,  -- 'KO' ou 'WARN'
    ouvert_le                   TIMESTAMP NOT NULL,
    email_ouverture_envoye      BOOLEAN DEFAULT false,
    UNIQUE(site_id, type_incident)
);