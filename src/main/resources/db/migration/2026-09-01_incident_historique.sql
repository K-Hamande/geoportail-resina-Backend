-- Historique des incidents (01/09/2026)
-- A executer manuellement sur chaque environnement (dev, prod...) -
-- ce fichier ne passe PAS par Flyway (voir sql/2026-08-31_alertes_email.sql
-- pour le contexte : l'historique de migration Flyway du projet est deja
-- incoherent, on evite donc d'y ajouter de nouveaux fichiers).

CREATE TABLE IF NOT EXISTS geoportail_resina.incident_historique (
    id            BIGSERIAL PRIMARY KEY,
    incident_key  VARCHAR(255) NOT NULL,
    type          VARCHAR(255),
    site_id       VARCHAR(255),
    site_nom      VARCHAR(255),
    ville         VARCHAR(255),
    ministere     VARCHAR(255),
    statut        VARCHAR(255),
    message       VARCHAR(1000),
    debut_le      TIMESTAMP NOT NULL,
    fin_le        TIMESTAMP
);

-- Utilisee a chaque passage du scheduler pour retrouver l'incident OUVERT
-- (fin_le IS NULL) correspondant a une cle donnee.
CREATE INDEX IF NOT EXISTS idx_incident_historique_key_ouvert
    ON geoportail_resina.incident_historique (incident_key)
    WHERE fin_le IS NULL;

-- Utilisee pour la page "Historique des incidents" (fenetre par date).
CREATE INDEX IF NOT EXISTS idx_incident_historique_debut_le
    ON geoportail_resina.incident_historique (debut_le);