-- Massa de Term para os testes de integração.
-- id antigo: publicado em EN, PT e ES, menor que o vigente.
-- id A: vigente (EN, PT e ES publicados). id B: rascunho completo (EN, PT e ES em draft).
-- id C: rascunho incompleto (só EN). O gatilho exige os três type antes do draft false,
-- então a publicação insere em draft true e depois faz UPDATE para draft false.
-- Cada id entra em term antes das linhas de term_detail.
INSERT INTO term (id) VALUES ('019c6000-0111-7000-8000-111111111111');
INSERT INTO term_detail (term_id, type, content, draft) VALUES ('019c6000-0111-7000-8000-111111111111', 'EN', 'Previous terms of use (EN).', TRUE);
INSERT INTO term_detail (term_id, type, content, draft) VALUES ('019c6000-0111-7000-8000-111111111111', 'PT', 'Termos de uso anteriores (PT).', TRUE);
INSERT INTO term_detail (term_id, type, content, draft) VALUES ('019c6000-0111-7000-8000-111111111111', 'ES', 'Términos de uso anteriores (ES).', TRUE);
UPDATE term_detail SET draft = FALSE WHERE term_id = '019c6000-0111-7000-8000-111111111111';
INSERT INTO term (id) VALUES ('019c6000-a111-7000-8000-aaaaaaaaaaaa');
INSERT INTO term_detail (term_id, type, content, draft) VALUES ('019c6000-a111-7000-8000-aaaaaaaaaaaa', 'EN', 'Terms of use (EN).', TRUE);
INSERT INTO term_detail (term_id, type, content, draft) VALUES ('019c6000-a111-7000-8000-aaaaaaaaaaaa', 'PT', 'Termos de uso (PT).', TRUE);
INSERT INTO term_detail (term_id, type, content, draft) VALUES ('019c6000-a111-7000-8000-aaaaaaaaaaaa', 'ES', 'Términos de uso (ES).', TRUE);
UPDATE term_detail SET draft = FALSE WHERE term_id = '019c6000-a111-7000-8000-aaaaaaaaaaaa';
INSERT INTO term (id) VALUES ('019c6000-b222-7000-8000-bbbbbbbbbbbb');
INSERT INTO term_detail (term_id, type, content, draft) VALUES ('019c6000-b222-7000-8000-bbbbbbbbbbbb', 'EN', 'Draft terms (EN).', TRUE);
INSERT INTO term_detail (term_id, type, content, draft) VALUES ('019c6000-b222-7000-8000-bbbbbbbbbbbb', 'PT', 'Rascunho de termos (PT).', TRUE);
INSERT INTO term_detail (term_id, type, content, draft) VALUES ('019c6000-b222-7000-8000-bbbbbbbbbbbb', 'ES', 'Borrador de términos (ES).', TRUE);
INSERT INTO term (id) VALUES ('019c6000-c333-7000-8000-cccccccccccc');
INSERT INTO term_detail (term_id, type, content, draft) VALUES ('019c6000-c333-7000-8000-cccccccccccc', 'EN', 'Only EN draft.', TRUE);
UPDATE profile SET term_id = '019c6000-a111-7000-8000-aaaaaaaaaaaa' WHERE type <> 'MASTER' AND email NOT IN ('marcelo@example.com', 'zelia.hermes@example.com', 'larissa@example.com', 'ana.isidoro@example.com');
UPDATE profile SET term_id = '019c6000-0111-7000-8000-111111111111' WHERE email IN ('larissa@example.com', 'ana.isidoro@example.com');
