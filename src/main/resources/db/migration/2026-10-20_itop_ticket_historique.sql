-- Historique des tickets iTop (20/10/2026)
-- A executer manuellement sur chaque environnement (dev, prod...) - ce
-- fichier ne passe PAS par Flyway, meme convention que
-- 2026-09-01_incident_historique.sql (voir son en-tete pour le contexte).

CREATE TABLE IF NOT EXISTS geoportail_resina.itop_ticket_historique (
    id                  BIGSERIAL PRIMARY KEY,
    ref                 VARCHAR(255) NOT NULL UNIQUE,
    titre               VARCHAR(500),
    ministere           VARCHAR(255),
    statut              VARCHAR(50),
    survenu_le          TIMESTAMP NOT NULL,
    cloture_le          TIMESTAMP,
    derniere_maj_itop   TIMESTAMP
);

-- Utilisee pour la page Backoffice (fenetre par date sur survenu_le).
CREATE INDEX IF NOT EXISTS idx_itop_ticket_historique_survenu_le
    ON geoportail_resina.itop_ticket_historique (survenu_le);

-- Utilisee par la synchronisation incrementale (ne resynchronise que ce
-- qui a change depuis le plus recent derniere_maj_itop deja connu).
CREATE INDEX IF NOT EXISTS idx_itop_ticket_historique_derniere_maj
    ON geoportail_resina.itop_ticket_historique (derniere_maj_itop);
