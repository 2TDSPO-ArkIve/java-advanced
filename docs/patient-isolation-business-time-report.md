# Patient isolation and scheduling business time

## A. Audit findings

Initial `git status`: clean. Branch: `main`, tracking `origin/main`.
Initial `git diff`: empty. Patient history/PDF work was already present and was not redone.

Independent/PJ is not a separate legal classification in the current domain. For this
workflow it is represented by `Veterinario.clinica == null`. The registering veterinarian
is recorded in `Animal.veterinarioCadastro`. No new PJ flag or schema field was added.

The reported broad leak of unrelated active patients between clinicless veterinarians
was NOT reproduced in current code. Both the repository and ClinicalAccessService
already guard clinic sharing with a nonnull clinic ID, and creator/consultation IDs are
scoped to the authenticated veterinarian. A shared role or two null clinic IDs do not grant access.
Production data/deployed revision were not inspected, so the reported incident's origin
cannot be attributed to a specific deployed query from this workspace alone.

A concrete inconsistency WAS found: the generic veterinarian list used the optional
`ativo` filter, allowing inactive patients when omitted or set to N, while direct access
requires active patients. `AnimalRepository.buscarParaVeterinario` now always requires
`a.ativo = 'S'` and a nonnull veterinarian ID, aligning every veterinarian list with the
existing direct-access policy. The separate administrator/responsible queries are unchanged.

Scheduling root cause: ConsultaService's production constructor created
`Clock.systemDefaultZone()`, despite its tests using a Sao Paulo fixed clock. With a UTC
host at `2026-09-09T01:04Z`, it compared a Sao Paulo request against September 9 at 01:04,
instead of September 8 at 22:04. Seconds/nanoseconds could also reject the current minute.
The bug is reproducible from the code; no Render/deployment setting was changed or inspected.

## B. Files changed

Modified:

- `src/main/java/br/com/fiap/arkive/repository/AnimalRepository.java`
- `src/main/java/br/com/fiap/arkive/service/ConsultaService.java`
- `src/test/java/br/com/fiap/arkive/controller/AnimalControllerMvcTest.java`
- `src/test/java/br/com/fiap/arkive/service/ClinicalWorkflowEndToEndServiceTest.java`

New:

- `src/main/java/br/com/fiap/arkive/config/BusinessTimeConfig.java`
- `src/test/java/br/com/fiap/arkive/repository/PatientIsolationTest.java`
- `src/test/java/br/com/fiap/arkive/service/ConsultaBusinessTimeTest.java`
- `docs/patient-isolation-business-time-report.md`

## C. Final patient access rule

Veterinarians can read ACTIVE patients with at least one of:

- Their own `veterinarioCadastro` linkage.
- An existing consultation assigned to them for that animal.
- A genuine, nonnull clinic match between patient and veterinarian's current persisted clinic.

The clinic collaboration rule is intentional in the current services and tests and remains.
Clinicless veterinarians get no clinic-sharing branch. They see only their own patients
and patients with an existing consultation assigned to them.

Important existing behavior preserved: the consultation-link rule does not restrict
status or scheduled date. It includes an already assigned AG consultation, not only FI.
No previous fix restricting planned veterinarian consultations was present. Tightening
this would change existing access used to prepare appointments. Assigning a consultation
through the API still requires prior patient access, so guessing an ID cannot self-grant it.
Future tutor relationships are a different model: they remain ineffective until dataInicio.

The A/B/C/D/E matrix is tested with real H2 queries and real services/access rules:

| Veterinarian | Initial visible patients |
| --- | --- |
| A, clinicless | P1, registered by A |
| B, clinicless | P2, registered by B |
| C, clinic X | P3, clinic X |
| D, clinic X | P3, clinic X |
| E, clinic Y | P4, clinic Y |

After a legitimate consultation assigns P2 to A, A can also see P2; repeated consultations
do not duplicate patient rows or inflate pagination totals.

## D. /api/animais/me

`GET /api/animais/me` remains the canonical veterinarian list. It accepts optional nome,
especieId, racaId and Spring Pageable parameters. Scope comes exclusively from the principal's
veterinarioId and a server-side lookup of that veterinarian's current clinic.
Client-supplied veterinarianId, clinicaId or ativo cannot expand this endpoint's scope.

`/api/animais/me`, `/api/animais/clinica` and the VETERINARIO branch of `/api/animais`
all use `buscarParaVeterinario`. Its DISTINCT, EXISTS, grouping and guarded clinic match
are preserved. Only the mandatory active-patient and nonnull-veterinarian predicates were added.
Filtering `/api/animais?ativo=N` as a veterinarian now returns an empty page rather than
inactive records. Other profile queries retain their existing behavior.

## E. Direct-ID security

No new controller-specific access policy was introduced. ClinicalAccessService remains
the authorization source for reading/updating patients, creating consultations and tutor
link operations. The existing patient history checks animal access and then consultation
access; both history PDF and consultation summary PDF retain those checks.

New integration tests prove an unrelated veterinarian cannot read/update another clinicless
patient, schedule a consultation to self-authorize, list/link tutors or export either PDF.
Denied service operations raise AccessDeniedException, mapped to the existing HTTP 403.
Existing authentication/404 handling is unchanged. The MVC test confirms /me uses the actual
authenticated principal even when conflicting scope parameters are supplied.

## F. Timezone fix

`BusinessTimeConfig.businessClock()` is a Spring bean returning
`Clock.system(ZoneId.of("America/Sao_Paulo"))`. ConsultaService now has one public constructor
that explicitly injects this qualified Clock; it no longer chooses the JVM default timezone.
Tests can inject Clock.fixed. No JVM-wide timezone or serialization configuration was changed.

Old comparison: raw request LocalDateTime against LocalDateTime.now(host-default clock).

New comparison, for create or an actual date/time change:

```java
request.dataHora().truncatedTo(ChronoUnit.MINUTES)
    .isBefore(LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES))
```

An earlier minute is rejected with the existing HTTP 400 business error:
`Data e hora da consulta nao podem estar no passado.`
The current minute and later minutes are accepted. Submitted seconds remain stored and
returned as received; only the scheduling comparison uses minute precision.
An unchanged historical timestamp bypasses this check, as before. Changing a historical
timestamp to another past value remains invalid. Workflow actions do not use this validation.

## G. Same-day test cases

Clock instant: `2026-09-09T01:04:59.900Z`, zone America/Sao_Paulo,
local business time `2026-09-08T22:04:59.900`.

| Requested local date/time | Result |
| --- | --- |
| 08/09 21:59 | Reject |
| 08/09 22:03:59.999999999 | Reject |
| 08/09 22:04:00 | Accept |
| 08/09 22:04:10 | Accept |
| 08/09 22:05 | Accept |
| 08/09 23:40 | Accept |
| 08/09 23:55 | Accept |
| 09/09 00:01 | Accept |

Each case runs with JVM default zones UTC, Asia/Tokyo and America/Sao_Paulo, restoring the
original default in finally. A Spring context test proves the named business Clock is injected.
Additional fixed-clock tests cover 23:59:55 local, midnight rollover, past rescheduling and
unchanged historical edits. Patient isolation fixtures also use a fixed injected scheduling clock.

## H. Role regressions and search interpretation

SYSADMIN remains unrestricted by the veterinarian query. ADMIN_CLINICA retains its own
clinic scope, including existing inactive-patient visibility. RESPONSAVEL retains effective
AnimalResponsavel linkage requirements. Their regression suites pass, as do clinicless,
tutor-link, patient-history and both PDF authorization tests.

Required git grep searches were run for LocalDateTime.now, LocalDate.now, systemDefault,
clinica in services, and animais/me. Results:

- Scheduling now uses the injected business Clock and minute precision exclusively.
- Remaining production systemDefault usages are AnimalService birth-date validation and
  SysAdminDashboardService reporting, outside this scheduling correction.
- Remaining direct LocalDateTime.now usages primarily create audit/event/account/error,
  alert, wellbeing, feedback and adherence timestamps; they do not validate scheduling.
- Date-only LocalDate.now usages include tutor effectiveness/listing, tutor snapshots,
  registration dates and prescription-related checks. Their existing host-date semantics
  were not partially converted here: changing them consistently would be a separate
  date-policy change across services, not required for the appointment LocalDateTime bug.
- No production Instant.now scheduling path was found. No application timezone setting
  previously supplied the scheduling clock.
- The /me mapping is composed from `/api/animais` on AnimalController and `@GetMapping("/me")`;
  its absence as one literal in a Java git grep match does not mean the route is absent.
- Clinic scoping uses the veterinarian entity, not the Usuario.clinica association intended
  for ADMIN_CLINICA. Shared VETERINARIO roles alone confer no patient scope.

## I. Maven results

Command: `.\mvnw.cmd test`

BUILD SUCCESS: **549 tests, 0 failures, 0 errors, 0 skipped**.
Previous baseline: 523. Added 26 cases (12 patient isolation, 13 business-time, 1 MVC scope).
Persistence integration tests use H2; no live Oracle or deployed API validation was performed.
`git diff --check` passed (only existing Windows LF/CRLF conversion warnings).

## J. Backward compatibility and constraints

- No API field/type/route changes. dataHora stays timezone-less LocalDateTime representing
  Sao Paulo business wall-clock time. No OffsetDateTime conversion or data migration.
- Current-minute acceptance is intentional. Other scheduling, immutable associations,
  status transitions, address snapshots and historical-update behavior remain.
- The only patient visibility tightening is inactive rows and null-vet defensive handling
  in the veterinarian query; existing creator/assigned consultation/same-clinic sharing stays.
- Existing PDF/history source and assets were not modified.
- No Mobile, Python, deployment, cron or UptimeRobot changes.
- No commit, staging, reset, rebase or stash was performed.
