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

CREATE TABLE IF NOT EXISTS ministry_access_tokens (
    id          BIGSERIAL PRIMARY KEY,
    token       VARCHAR(64) UNIQUE NOT NULL,
    ministere   VARCHAR(255) NOT NULL,
    libelle     VARCHAR(255),
    actif       BOOLEAN DEFAULT true,
    cree_le     TIMESTAMP,
    cree_par    VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS site_supervision_settings (
    site_id                     VARCHAR(255) PRIMARY KEY REFERENCES sites(site_id),
    intervalle_actualisations   INTEGER,
    debit_minimal_mbps          DOUBLE PRECISION,
    latence_maximale_ms         DOUBLE PRECISION,
    notifications_actives       BOOLEAN,
    notif_panne_anptic          BOOLEAN,
    notif_panne_lan             BOOLEAN,
    notif_retablissement        BOOLEAN,
    modifie_le                  TIMESTAMP,
    modifie_par                 VARCHAR(255)
);