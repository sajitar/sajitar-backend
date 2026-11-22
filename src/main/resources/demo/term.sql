-- Term de demonstração: um id publicado em EN, PT e ES (vigente).
-- Insere em draft true e publica com UPDATE, respeitando o gatilho term_draft_requires_all_types.
INSERT INTO term (id, type, content, draft) VALUES ('019c7000-d111-7000-8000-dddddddddddd', 'EN', 'Demo terms of use (EN).', TRUE);
INSERT INTO term (id, type, content, draft) VALUES ('019c7000-d111-7000-8000-dddddddddddd', 'PT', 'Termos de uso de demonstração (PT).', TRUE);
INSERT INTO term (id, type, content, draft) VALUES ('019c7000-d111-7000-8000-dddddddddddd', 'ES', 'Términos de uso de demostración (ES).', TRUE);
UPDATE term SET draft = FALSE WHERE id = '019c7000-d111-7000-8000-dddddddddddd';
