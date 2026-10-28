ALTER TABLE TB_ARKIVE_ANIMAL ADD DS_OBS_RESP_PENDENTE CLOB NULL;

ALTER TABLE TB_ARKIVE_CONSULTA ADD DS_OBS_RESPONSAVEL CLOB NULL;

COMMENT ON COLUMN TB_ARKIVE_ANIMAL.DS_OBS_RESP_PENDENTE IS
    'Observacao pendente registrada por responsavel vinculado; consumida quando a proxima consulta do animal e iniciada.';

COMMENT ON COLUMN TB_ARKIVE_CONSULTA.DS_OBS_RESPONSAVEL IS
    'Snapshot da observacao do responsavel consumida no inicio desta consulta; preservada no historico.';
