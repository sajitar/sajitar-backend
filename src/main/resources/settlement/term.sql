-- Massa de Term para os testes de integração.
-- id A: vigente (EN, PT e ES publicados). id B: rascunho completo (EN, PT e ES em draft).
-- id C: rascunho incompleto (só EN). O gatilho exige os três type antes do draft false,
-- então a publicação do id A insere em draft true e depois faz UPDATE para draft false.
INSERT INTO term (id, type, content, draft) VALUES ('019c6000-a111-7000-8000-aaaaaaaaaaaa', 'EN', 'Terms of use (EN).', TRUE);
INSERT INTO term (id, type, content, draft) VALUES ('019c6000-a111-7000-8000-aaaaaaaaaaaa', 'PT', 'Termos de uso (PT).', TRUE);
INSERT INTO term (id, type, content, draft) VALUES ('019c6000-a111-7000-8000-aaaaaaaaaaaa', 'ES', 'Términos de uso (ES).', TRUE);
UPDATE term SET draft = FALSE WHERE id = '019c6000-a111-7000-8000-aaaaaaaaaaaa';
INSERT INTO term (id, type, content, draft) VALUES ('019c6000-b222-7000-8000-bbbbbbbbbbbb', 'EN', 'Draft terms (EN).', TRUE);
INSERT INTO term (id, type, content, draft) VALUES ('019c6000-b222-7000-8000-bbbbbbbbbbbb', 'PT', 'Rascunho de termos (PT).', TRUE);
INSERT INTO term (id, type, content, draft) VALUES ('019c6000-b222-7000-8000-bbbbbbbbbbbb', 'ES', 'Borrador de términos (ES).', TRUE);
INSERT INTO term (id, type, content, draft) VALUES ('019c6000-c333-7000-8000-cccccccccccc', 'EN', 'Only EN draft.', TRUE);
