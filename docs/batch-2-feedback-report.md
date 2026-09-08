# ArkIve: Batch 2 Feedback Report

Scope: java-advanced only. Initial branch: main, synchronized with origin/main. Initial git status was clean and git diff empty. No commit, reset, rebase or stash was performed.

## A. Audit Findings

- Animal already supports a nullable clinic and veterinarian ownership through ID_VETERINARIO_CADASTRO (V5). ClinicalAccessService authorizes an active patient through its registering veterinarian, a previous consultation with the veterinarian, or the veterinarian's clinic. These rules and /api/animais/me are preserved.
- Email/CRMV authentication, account provisioning, temporary credentials, forced password change, /api/auth/me and clinicless veterinarians were already implemented. Their existing regression tests remain passing.
- Animal had no birth date. Consulta had neither an address nor server-side scheduling validation. Generic consultation PUT already allowed unrelated historical edits; status changes remain restricted to workflow operations.
- The current PDF is an owner-facing summary of a finalized consultation with confirmed veterinary diagnosis, not longitudinal patient history. An optional appointment address fits its existing scope.
- Raca already had GET/POST/PUT/DELETE, species-filtered JPQL and species-aware cache keys. V1 already has UQ_ARKIVE_RACA_ESPECIE_NOME on (ID_ESPECIE, NM_RACA). SecurityConfig previously allowed every authenticated user to mutate this catalog.
- Both Java animal forms previously loaded all breeds. There is no existing separate Thymeleaf breed registration page. The existing REST breed API now also serves restrained inline creation.
- AnimalResponsavel already models optional, time-aware relationships and demotes previous principals. Its API previously had no patient-level authorization, and principal changes lacked serialization against concurrent requests.
- Responsavel's full API exposed document, phone and other administrative fields to all authenticated users. Its existing broad text search included document and partial email. The new lookup returns a smaller response and restricts email matching.
- Relationship-based clinical reads checked the end date but omitted the start date, allowing future links to grant access early. All affected clinical repository queries now include the start date.
- M4A/AAC already used temporary files and ffmpeg before Azure's WAV-file SDK input. Docker already installs ffmpeg. WebM was missing only from the declared formats; no second transcription architecture was needed.

## B. Files Changed

The following inventory includes production changes, focused tests and this report. Temporary verification tools and screenshots are under ignored target/browser-check, not part of the source change.

- `docs/batch-2-feedback-report.md`
- `README.md`
- `pom.xml`
- `src/main/java/br/com/fiap/arkive/config/SecurityConfig.java`
- `src/main/java/br/com/fiap/arkive/controller/AnimalResponsavelController.java`
- `src/main/java/br/com/fiap/arkive/controller/ResponsavelController.java`
- `src/main/java/br/com/fiap/arkive/controller/web/AdminAnimalController.java`
- `src/main/java/br/com/fiap/arkive/domain/transcricao/SupportedAudioFormat.java`
- `src/main/java/br/com/fiap/arkive/dto/request/AnimalRequest.java`
- `src/main/java/br/com/fiap/arkive/dto/request/ConsultaRequest.java`
- `src/main/java/br/com/fiap/arkive/dto/response/AnimalResponse.java`
- `src/main/java/br/com/fiap/arkive/dto/response/ConsultaResponse.java`
- `src/main/java/br/com/fiap/arkive/dto/response/ResponsavelLookupResponse.java`
- `src/main/java/br/com/fiap/arkive/entity/Animal.java`
- `src/main/java/br/com/fiap/arkive/entity/Consulta.java`
- `src/main/java/br/com/fiap/arkive/repository/AdesaoPrescricaoRepository.java`
- `src/main/java/br/com/fiap/arkive/repository/AnimalRepository.java`
- `src/main/java/br/com/fiap/arkive/repository/AnimalResponsavelRepository.java`
- `src/main/java/br/com/fiap/arkive/repository/ConsultaRepository.java`
- `src/main/java/br/com/fiap/arkive/repository/DiagnosticoRepository.java`
- `src/main/java/br/com/fiap/arkive/repository/PrescricaoRepository.java`
- `src/main/java/br/com/fiap/arkive/repository/RacaRepository.java`
- `src/main/java/br/com/fiap/arkive/repository/ResponsavelRepository.java`
- `src/main/java/br/com/fiap/arkive/service/AnimalResponsavelService.java`
- `src/main/java/br/com/fiap/arkive/service/AnimalService.java`
- `src/main/java/br/com/fiap/arkive/service/ClinicalAccessService.java`
- `src/main/java/br/com/fiap/arkive/service/ConsultaResumoPdfService.java`
- `src/main/java/br/com/fiap/arkive/service/ConsultaService.java`
- `src/main/java/br/com/fiap/arkive/service/RacaService.java`
- `src/main/java/br/com/fiap/arkive/service/ResponsavelService.java`
- `src/main/java/br/com/fiap/arkive/service/speech/AudioConversionService.java`
- `src/main/resources/db/migration/V6__add_animal_birth_date_and_consulta_address.sql`
- `src/main/resources/static/js/animal-racas.js`
- `src/main/resources/templates/admin/animais/editar.html`
- `src/main/resources/templates/admin/animais/formulario.html`
- `src/main/resources/templates/fragments/raca-inline.html`
- `src/test/java/br/com/fiap/arkive/controller/Batch2ApiMvcTest.java`
- `src/test/java/br/com/fiap/arkive/controller/web/AdminClinicaControllerTest.java`
- `src/test/java/br/com/fiap/arkive/domain/transcricao/SupportedAudioFormatTest.java`
- `src/test/java/br/com/fiap/arkive/repository/Batch2RepositoryTest.java`
- `src/test/java/br/com/fiap/arkive/service/AnimalResponsavelServiceTest.java`
- `src/test/java/br/com/fiap/arkive/service/AnimalServiceAuthorizationTest.java`
- `src/test/java/br/com/fiap/arkive/service/ConsultaResumoPdfServiceTest.java`
- `src/test/java/br/com/fiap/arkive/service/ConsultaServiceTest.java`
- `src/test/java/br/com/fiap/arkive/service/RacaServiceTest.java`
- `src/test/java/br/com/fiap/arkive/service/ResponsavelLookupServiceTest.java`
- `src/test/java/br/com/fiap/arkive/service/TranscricaoServiceTest.java`
- `src/test/java/br/com/fiap/arkive/service/speech/AudioConversionServiceTest.java`
- `src/test/java/br/com/fiap/arkive/service/speech/AzureSpeechTranscriptionServiceTest.java`

## C. Flyway Migration

File: src/main/resources/db/migration/V6__add_animal_birth_date_and_consulta_address.sql

```sql
ALTER TABLE TB_ARKIVE_ANIMAL ADD (
    DT_NASCIMENTO DATE NULL
);

ALTER TABLE TB_ARKIVE_CONSULTA ADD (
    DS_ENDERECO VARCHAR2(255) NULL
);
```

No added constraints or indexes; no data updates, deletions or backfill. V1-V5 are unchanged. Both existing tables and rows remain valid. SQL uses the project's Oracle-compatible ADD syntax. This migration was reviewed but was not executed against Oracle. H2 tests use Hibernate-generated schemas and do not substitute for Oracle Flyway execution.

## D. Animal Birth-Date Contract

POST /api/animais and PUT /api/animais/{id} accept the additional optional property:

```json
{
  "nome": "Nina",
  "especieId": 1,
  "racaId": null,
  "sexo": "F",
  "castrado": "N",
  "clinicaId": null,
  "ativo": "S",
  "dataNascimento": "2021-04-17"
}
```

AnimalResponse includes dataNascimento on create, update, detail and list responses, including veterinarian patient lists. It is an ISO date or null. No static or calculated age field was added.

Both create and update reject dataNascimento later than LocalDate.now(clock), with HTTP 400 and "Data de nascimento do animal nao pode estar no futuro." Null, past dates and today are valid. The service follows the existing project's Clock-constructor pattern, with a controllable clock in tests and the JVM default timezone in production.

PUT remains a replacement-style request: omitted/null dataNascimento clears the value. Consumers preserving a date must echo it in PUT. Java edit and reactivation paths carry the existing birth date so those workflows do not erase it. No visible birth-date form UI was added.

## E. Consultation Date Validation

- CREATE: dataHora is required and must be greater than or equal to LocalDateTime.now(clock).
- UPDATE: the same rule applies only when dataHora differs from the stored value. An unchanged historical date permits otherwise-valid field edits.
- Exact equality is accepted; there is no minute rounding or grace period. A client timestamp which has passed by the time the server validates it is rejected.
- Invalid scheduling returns HTTP 400 and "Data e hora da consulta nao podem estar no passado."
- Existing veterinarian ownership, immutable associations and status/workflow rules remain enforced. Workflow methods do not invoke this scheduling validation, so elapsed appointment time does not block clinical progress.
- The API continues using LocalDateTime without an offset. The clock uses the JVM default timezone, matching existing local-time conventions; clients and runtime must agree on that timezone.

## F. Consultation Address Contract

ConsultaRequest and ConsultaResponse add nullable endereco, with a maximum length of 255:

```json
{
  "dataHora": "2099-04-17T10:00:00",
  "modalidade": "PRESENCIAL",
  "motivo": "Retorno",
  "animalId": 1,
  "endereco": "Rua do Atendimento, 10"
}
```

Existing request properties remain available. veterinarianId may still be omitted; the service uses the authenticated veterinarian.

On creation:
- A nonblank explicit address wins and is preserved.
- PRESENCIAL plus null/blank address copies the authenticated veterinarian's clinic address when nonblank, even if request.clinicaId is omitted.
- A clinicless veterinarian or clinic with no address results in null and does not prevent creation.
- REMOTA never automatically receives a clinic address. Explicit nonblank addresses are preserved; blanks normalize to null.

The resolved value is stored in Consulta.DS_ENDERECO. Responses and PDF never derive it dynamically from the clinic. Clinic address changes therefore do not change the recorded appointment address.

On PUT, the request replaces endereco; omitted/null/blank clears it. Defaults are not reapplied. Echo the stored address to preserve it during unrelated edits.

The existing summary PDF includes an Endereco line only for PRESENCIAL with a nonblank snapshot. Null/blank and remote addresses are omitted. No redesign or internal clinical-data exposure was introduced.

## G. WebM Transcription

POST /api/transcricoes remains the same multipart contract, with audio and optional idioma.

Added:
- Extension: .webm.
- MIME: audio/webm.
- Parameterized MIME such as audio/webm;codecs=opus, through existing MIME normalization.

video/webm and arbitrary additional container types were not added. Existing WAV, M4A/MP4 and AAC declarations remain. Known extension/MIME mismatches return HTTP 400.

Pipeline: declared WebM upload -> safe temporary .webm -> existing ffmpeg conversion -> mono 16 kHz signed 16-bit PCM WAV -> existing Azure Speech SDK WAV input. The original filename is never a shell command or output path. No file is renamed to pretend it is M4A.

The ffmpeg invocation uses fixed ProcessBuilder arguments, explicitly requests pcm_s16le, disables video and stdin, and discards process output. The 10 MB upload limit, empty-file validation, 30-second default conversion timeout and PreparedAudio cleanup remain. Timeout/interruption now kills and waits briefly for the converter before best-effort file removal. Interrupt status is preserved.

Errors remain safe:
- Unsupported format or MIME mismatch: 400.
- Corrupt/unconvertible audio or conversion timeout: 400.
- Converter missing/unavailable or interrupted: 503.
- Azure failure uses the existing status mapping (including 503/429/502); no recognized speech remains 422.
- No process output, paths, command details or Azure credentials are returned.

Runtime dependency: ffmpeg on PATH, already installed by Dockerfile; no new runtime Maven library. H2 was added only with test scope.

The canonical format matches the [Microsoft Speech SDK audio format requirements](https://learn.microsoft.com/en-us/azure/ai-services/speech-service/how-to-use-audio-input-streams). Decoder process behavior is tested with controlled processes, and the Azure adapter with its existing fake recognizer pattern. Additionally, a temporary ffmpeg binary under ignored target/browser-check converted locally generated 0.2-second WebM/Opus, M4A, AAC and WAV samples through the real Java AudioConversionService. Java Sound verified each resulting WAV as signed 16-bit PCM, mono, 16000 Hz. Corrupt WebM returned 400 and the temporary-file inventory was unchanged after all conversions. No binary fixture or native dependency was added to tracked source. Docker daemon is unavailable and no live Azure call was made.

## H. Breed Contract

Reuse existing APIs:
- GET /api/racas?especieId={id}&nome={optional}&page=0&size=20.
- GET /api/racas/{id}.
- POST /api/racas with {"nome":"Nova raca","especieId":2,"porte":null}.
- Existing PUT /api/racas/{id} and DELETE /api/racas/{id} remain available to administrators.

Final permissions:
- VETERINARIO: GET and POST.
- SYSADMIN and ADMIN_CLINICA: GET, POST, PUT, DELETE.
- RESPONSAVEL: GET only.

Creation/update requires an existing active species, a nonblank trimmed name of at most 50 characters, and a supported optional porte. Blank porte becomes null; PEQUENO, MEDIO and GRANDE remain supported.

Exact duplicate names within the same species are rejected with 409. Names are compared case-sensitively as in the existing database rule, after trimming incoming values; no case-folding migration or existing-data rewrite was introduced. Identical names in different species are allowed. Concurrent database uniqueness conflicts return a safe 409. Existing inactive names are also reserved by the existing unique key.

A real H2 repository test inserts breeds under two species and verifies isolation in both directions. Species participates in existing cache keys. The API preserves its existing inclusion of inactive breeds; no new active-breed filter was silently introduced.

Java create/edit forms render options for the current species only. JavaScript reloads all pages for that species, clears the selection on species changes and ignores stale responses. Inline creation uses POST /api/racas, associates the chosen species and selects the returned breed without navigating away or losing animal fields. Backend animal/breed species validation remains.

## I. Tutor Link Contract

No responsavelId was added to Animal. Zero active relationships is valid.

Existing relationship routes, now scoped through ClinicalAccessService:

| Method | Route | Purpose |
| --- | --- | --- |
| GET | /api/animais-responsaveis/animal/{animalId} | Active, currently effective relationships; returns a JSON array |
| GET | /api/animais-responsaveis?animalId={id}&ativo=S | Existing paginated listing; ativo filters the stored flag, including future/expired dates |
| POST | /api/animais-responsaveis | Link an existing active Responsavel |
| PUT | /api/animais-responsaveis | Update the existing relationship identified by animalId, responsavelId, dataInicio |
| PATCH | /api/animais-responsaveis/encerrar | Set dataFim and mark the relationship inactive |
| DELETE | /api/animais-responsaveis?animalId={id}&responsavelId={id}&dataInicio=YYYY-MM-DD | Logical removal, preserving the relationship row |
| GET | /api/animais-responsaveis/responsavel/{responsavelId} | SYSADMIN or that authenticated RESPONSAVEL's own relationship history |

Example creation:
```json
{
  "animalId": 1,
  "responsavelId": 20,
  "tipoVinculo": "TUTOR_LEGAL",
  "dataInicio": null,
  "dataFim": null,
  "principal": "S",
  "ativo": "S"
}
```

dataInicio defaults to today. principal defaults to N; ativo defaults to S. Keep the returned dataInicio to address that relationship later. The existing request requires tipoVinculo for POST/PUT/PATCH, and PATCH /encerrar also requires dataFim. dataFim cannot precede dataInicio.

Authorization:
- VETERINARIO may read and manage links only for patients permitted by existing ClinicalAccessService patient access rules, including clinicless veterinarian-owned patients.
- ADMIN_CLINICA may manage patients in its own clinic; SYSADMIN retains global access.
- VETERINARIO and ADMIN_CLINICA must supply animalId for the general relationship listing, and use the patient route instead of unrestricted tutor-history lookup.
- RESPONSAVEL can read links for authorized patients and its own history, but cannot self-grant access or change tutor assignments. Full Responsavel GET is scoped to its own record.
- Full Responsavel administration remains with SYSADMIN/ADMIN_CLINICA. Veterinarians use only the minimal lookup below and cannot provision new tutor records or accounts.
- Active patient access through relationships requires ST_ATIVO=S, dataInicio <= today and no elapsed dataFim, consistently across animal, consultation, diagnosis, prescription and adherence reads.

Principal invariant: every relationship mutation locks the parent animal with PESSIMISTIC_WRITE inside its transaction. Choosing an active principal demotes other ST_ATIVO=S principals. This preserves the existing demotion policy and enforces at most one flag-active principal even under concurrent first-link creation. No new database unique constraint or bulk repair was introduced. Existing legacy inconsistencies are normalized when a principal is next selected; untouched data was not rewritten.

Minimal veterinarian lookup:
```http
GET /api/responsaveis/busca?busca=Ana&page=0&size=20
GET /api/responsaveis/busca?busca=ana%40example.test
```

Requires VETERINARIO, SYSADMIN or ADMIN_CLINICA. Search text must contain 3-200 trimmed characters. Matches a literal case-insensitive name substring OR a full case-insensitive email address, only for active records. SQL wildcard characters do not expand the name search. Pagination is capped at 20 and ordered by nome then id.

Each item contains only:
```json
{"id":20,"nome":"Ana","email":"ana@example.test"}
```

No CPF/document, telephone, notification preferences, credentials or other administrative fields. New Responsavel/account provisioning from Mobile remains out of scope.

## J. Test Results

Final command: `.\mvnw.cmd test`

- Tests: 497, up from the stated baseline of 404 (+93).
- Failures: 0.
- Errors: 0.
- Skipped: 0.
- BUILD SUCCESS.
- git diff --check: clean.

Coverage includes null/past/today/future birth dates, JSON date contract, persistence, exact-now/future/past scheduling, historical updates, address defaults/overrides/nulls/snapshots/PDF omission, WebM MIME normalization and routing, converter errors/timeouts/interruption/cleanup, existing audio formats, real species-filtered queries, role permissions, breed/species mismatch, tutor lookup restrictions, real ClinicalAccessService authorization, principal reassignment and concurrent principal creation.

Browser verification used headless Microsoft Edge against the real Java/Thymeleaf application and REST services, with only fictitious data in an isolated H2 in-memory database. It passed:

1. Select Cachorro: only Bulldog appears.
2. Select Bulldog, then Macaco: Bulldog disappears and selection clears.
3. Create a new breed under Macaco: it becomes selected, and the animal name remains.
4. Submit the animal form: selected species/breed persist.
5. Open edit: correct species/breed return.
6. Inspect desktop and 390x844 mobile viewports: no horizontal overflow or JavaScript errors.

Screenshots: target/browser-check/form-desktop.png and target/browser-check/form-mobile.png. Browser helpers are ignored temporary verification artifacts, not a new application architecture. The in-app browser was unavailable due to a tool initialization error, so installed Edge was used.

Additional native audio check: real ffmpeg decoding passed for WebM/Opus, M4A, AAC and WAV; corrupt WebM returned 400; input/output temporary files were cleaned. Synthetic samples and the ffmpeg binary are ignored artifacts under target/browser-check, not production dependencies or committed fixtures.

Limitations: no production Oracle data was accessed, no Oracle migration applied, and no live Azure recognition executed. H2 integration validates JPQL/persistence/concurrency, not Oracle-specific runtime behavior. Native conversion was verified on Windows, not the deployment's Linux image.

## K. Backward-Compatibility Impact

- New database/JSON fields are nullable and existing payloads remain structurally valid.
- No persistent static age or mandatory tutor/clinic was introduced.
- PUT uses existing replacement semantics for optional fields; clients must echo birth date/address to preserve populated values.
- Past scheduling now returns 400; unchanged historical dates remain editable under existing permissions.
- Generic relationship APIs are now patient-scoped. Unrestricted owner-link access/self-assignment and catalog mutation by RESPONSAVEL are deliberately closed.
- Veterinarians lose unrestricted full tutor records/provisioning and breed PUT/DELETE; GET/POST breed and minimal tutor lookup cover the requested workflow.
- Future tutor links no longer grant clinical access before their start date.
- Existing authentication/onboarding, clinicless ownership, workflow, clinical AI, prescriptions, consultation PDF and Azure regression tests pass.
- No production dependency change; H2 is test-only. Existing migrations are untouched.

## L. Deliberately Left For Batch 3/4

All Mobile changes: birth date/age display, tutor selector, inline breed creation, appointment address, scheduling controls and transcription retry. Also deferred: new tutor/account provisioning from Mobile, same-name patient differentiation, longitudinal history screen and PDF, consultation cards, theme/login polish, AI warmup, GitHub cron and all Python clinical-engine work.

Mobile-Application and the Python repository were not modified.
