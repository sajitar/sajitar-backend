-- Term de demonstração: um id publicado antigo e um id vigente em EN, PT e ES.
-- Insere em draft true e publica com UPDATE, respeitando o gatilho term_draft_requires_all_types.
-- Cada id entra em term antes das linhas de term_detail.
INSERT INTO term (id) VALUES ('019c7000-0111-7000-8000-111111111111');
INSERT INTO term_detail (term_id, type, content, draft) VALUES ('019c7000-0111-7000-8000-111111111111', 'EN', 'Previous demo terms of use (EN).', TRUE);
INSERT INTO term_detail (term_id, type, content, draft) VALUES ('019c7000-0111-7000-8000-111111111111', 'PT', 'Termos de uso anteriores de demonstração (PT).', TRUE);
INSERT INTO term_detail (term_id, type, content, draft) VALUES ('019c7000-0111-7000-8000-111111111111', 'ES', 'Términos de uso anteriores de demostración (ES).', TRUE);
UPDATE term_detail SET draft = FALSE WHERE term_id = '019c7000-0111-7000-8000-111111111111';
INSERT INTO term (id) VALUES ('019c7000-d111-7000-8000-dddddddddddd');
INSERT INTO term_detail (term_id, type, content, draft) VALUES ('019c7000-d111-7000-8000-dddddddddddd', 'EN', 'Demo terms of use (EN).', TRUE);
INSERT INTO term_detail (term_id, type, content, draft) VALUES ('019c7000-d111-7000-8000-dddddddddddd', 'PT', 'Termos de uso de demonstração (PT).', TRUE);
INSERT INTO term_detail (term_id, type, content, draft) VALUES ('019c7000-d111-7000-8000-dddddddddddd', 'ES', 'Términos de uso de demostración (ES).', TRUE);
UPDATE term_detail SET draft = FALSE WHERE term_id = '019c7000-d111-7000-8000-dddddddddddd';
UPDATE profile SET term_id = '019c7000-d111-7000-8000-dddddddddddd' WHERE type <> 'MASTER' AND email <> 'demo.user1@example.com';
UPDATE profile SET term_id = '019c7000-0111-7000-8000-111111111111' WHERE email = 'demo.user2@example.com';
