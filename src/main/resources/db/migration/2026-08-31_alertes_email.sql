-- Alertes email pour les decideurs (31/08/2026)
-- A executer manuellement sur chaque environnement (dev, prod...) -
-- ces changements ne passent PAS par Flyway (le projet n'a pas
-- d'historique de migrations coherent, voir notes dans le code).

-- Email de contact du decideur, pour l'envoi des alertes ciblees par ministere.
ALTER TABLE geoportail_resina.decideur_users ADD COLUMN IF NOT EXISTS email VARCHAR(255);

-- Preference du decideur : a-t-il active la reception des alertes email
-- (bandeau "Activer les alertes" cote frontend decideur) ? Par defaut
-- desactive pour tous les comptes existants.
ALTER TABLE geoportail_resina.decideur_users ADD COLUMN IF NOT EXISTS alertes_activees BOOLEAN NOT NULL DEFAULT FALSE;