# Patient history and shared PDF header

## A. Audit findings

- Repository: `java-advanced`; branch: `main`. The worktree was clean at the initial audit.
- Existing summary route: `ConsultaWorkflowController.exportarResumoPdf`,
  `GET /api/consultas/{id}/resumo-pdf`, calling `ConsultaResumoPdfService.gerarResumo`.
- The summary already requires consultation read access, status `FI`, and a diagnosis
  with both `confirmado=S` and `validacaoVet=S`. Those requirements remain unchanged.
- History sources: `Animal`, `Consulta`, `Diagnostico`, `Prescricao`, `Veterinario`,
  optional `Clinica`, and current principal tutor through `AnimalResponsavel`.
- `ClinicalAccessService` animal access is broader than consultation access for some
  users. Both checks are necessary; animal access alone cannot authorize the entire history.
- Prescription creation already requires `FI`. Finalization stores the veterinarian's
  final conclusion in `Consulta.observacao`; this field can contain drafts or cancellation
  notes in other statuses and is therefore not exported for those statuses.
- Added one animal-scoped consultation query, two response DTOs, history services/controller,
  and extracted the existing PDF renderer. Existing diagnosis, prescription and tutor queries
  are reused. No schema or Flyway changes are needed.
- The logo asset is a white symbol on transparency. The old renderer drew a blue square
  behind it; neither the asset nor any frontend file needed modification.
- During implementation an unrelated staged deletion of
  `.github/workflows/render-keepalive.yml` appeared. It was not made or changed by this work.

## B. Files changed

Modified:

- `src/main/java/br/com/fiap/arkive/repository/ConsultaRepository.java`
- `src/main/java/br/com/fiap/arkive/service/ClinicalAccessService.java`
- `src/main/java/br/com/fiap/arkive/service/ConsultaResumoPdfService.java`

New:

- `src/main/java/br/com/fiap/arkive/controller/AnimalHistoricoController.java`
- `src/main/java/br/com/fiap/arkive/dto/response/AnimalHistoricoResponse.java`
- `src/main/java/br/com/fiap/arkive/dto/response/PrescricaoResumoResponse.java`
- `src/main/java/br/com/fiap/arkive/service/AnimalHistoricoService.java`
- `src/main/java/br/com/fiap/arkive/service/AnimalHistoricoPdfService.java`
- `src/main/java/br/com/fiap/arkive/service/PdfBrandHeader.java`
- `src/main/java/br/com/fiap/arkive/service/PdfRenderer.java`
- `src/test/java/br/com/fiap/arkive/controller/AnimalHistoricoControllerMvcTest.java`
- `src/test/java/br/com/fiap/arkive/repository/AnimalHistoricoRepositoryTest.java`
- `src/test/java/br/com/fiap/arkive/service/PdfBrandHeaderTest.java`
- `docs/historico-clinico-backend.md`

## C. Endpoints

Both routes use the existing authenticated API session/HTTP Basic mechanism. Neither accepts
a request body or requires query parameters. Both return `Cache-Control: no-store`.

### GET /api/animais/{id}/historico

`200 application/json`. A complete, unpaginated history visible to the requesting user:

```json
{
  "paciente": {
    "id": 50,
    "nome": "Bilu",
    "especie": "Cachorro",
    "raca": "Poodle",
    "sexo": "M",
    "castrado": "S",
    "dataNascimento": "2021-04-17",
    "responsavelNome": "Tutora Ana"
  },
  "consultas": [
    {
      "dataHora": "2026-09-05T14:30:00",
      "modalidade": "PRESENCIAL",
      "endereco": "Rua do Atendimento, 10",
      "motivo": "Retorno para acompanhamento",
      "status": "FI",
      "statusDescricao": "Finalizada",
      "veterinarioNome": "Dra. Vera",
      "crmv": "SP12345",
      "clinicaNome": "Clinica ArkIve",
      "diagnosticoConfirmado": "Dermatite confirmada",
      "severidadeFinal": "LEVE",
      "conclusao": "Retorno em sete dias.",
      "prescricoes": [
        {
          "medicamento": "Medicamento registrado",
          "dosagem": "5 mg",
          "frequencia": "12 horas",
          "viaAdministracao": "ORAL",
          "dataInicio": "2026-09-05",
          "dataFim": "2026-09-12",
          "instrucoes": "Administrar com alimento."
        }
      ]
    }
  ]
}
```

- Patient `id` is numeric; species and breed are names, not nested objects or IDs.
- `sexo` and `castrado` preserve the existing `M/F` and `S/N` codes (nullable).
- `raca`, `dataNascimento` and `responsavelNome` may be null. Birth date is the source
  of truth; neither response nor PDF introduces a persistent or calculated age field.
- `dataHora` retains the existing local date/time contract, without adding a UTC offset.
- `endereco`, `clinicaNome`, `diagnosticoConfirmado`, `severidadeFinal` and `conclusao`
  may be null. Optional prescription fields retain their stored null values.
- `consultas` and `prescricoes` are arrays, including `[]` when empty.
- Ordering is `dataHora DESC, id DESC` in both JSON and PDF; the consultation ID is used
  only for deterministic sorting and is not exposed. Prescriptions retain `id ASC` order.

### GET /api/animais/{id}/historico-pdf

`200 application/pdf`, raw PDF bytes, with:

```http
Content-Disposition: attachment; filename="arkive-historico-50.pdf"
Cache-Control: no-store
```

The filename uses the requested animal ID. IDs are not printed in the PDF body.
The PDF consumes the same authorized response as the JSON endpoint, not a second data query path.

## D. Data inclusion rules

- Include all authorized consultation records, including `AG`, `EP`, `AP`, `FI` and `CA`.
  No date cutoff is applied: future scheduled records are included and clearly labeled
  Agendada, rather than silently lost from the full patient record.
- Every consultation includes appointment details, status, veterinarian name/CRMV and
  optional clinic name. Only a nonblank PRESENCIAL consultation address is included.
  Remote addresses are omitted/null, matching the existing owner-facing summary.
- Only `FI` publishes final clinical details and registered prescriptions. Other statuses
  have null diagnosis, severity and conclusion, and an empty prescriptions array.
- The latest diagnosis with BOTH veterinarian confirmation flags is selected using the
  existing query, ordered by diagnosis ID descending. No fallback to AI hypotheses exists.
- A legacy `FI` without an approved diagnosis still appears, with null diagnosis/severity.
  This does not relax the existing single-consultation summary endpoint's stricter 409 rule.
- Include the current principal tutor's name only when exactly one current principal
  relationship is found. The PDF labels this as the current responsible person, not a
  historical ownership snapshot. No tutor is required; ambiguous principals are omitted.
- Exclude transcription, raw symptoms/narrative, unconfirmed diagnoses, AI insight,
  confidence, sources, disease/workflow/diagnosis/prescription IDs, validation flags,
  logs, processing metadata, tutor email/document/contact data and adherence internals.
- Approved veterinarian text is rendered as recorded; the export does not infer clinical
  decisions from AI or attempt to rewrite veterinarian-authored free text.

## E. PDF rendering

- `PdfRenderer` extracts the existing PDFBox A4 layout, wrapping, pagination, sections,
  footer and medication formatting. Both PDF services use this exact renderer.
- `PdfBrandHeader` reads the existing `static/images/favicon.png` without modifying it.
  It crops transparent padding and applies blue `#3F51B5` to the original alpha mask
  in memory. PDFBox embeds the resulting image losslessly, preserving transparency.
- The old filled rectangle is removed. The symbol is 28 points high, preserving aspect
  ratio; selectable Helvetica Bold `ArkIve` lettering is aligned beside it and sized
  from the font's cap height to match the symbol's visible height.
- Both outputs use the same header on every page. No frontend/static asset, dependency,
  build configuration or deployment change is required.
- Medication headings stay with the start of the medication content across page breaks.
- Representative PDFs were rendered to PNG and visually inspected. The shared header
  region was also compared pixel-for-pixel between the two generated PDFs.

## F. Validation and security

- Existing `SecurityConfig` authentication and forced-password-change behavior remain unchanged.
- First require `ClinicalAccessService.exigirLeituraAnimal`. Existing active-animal and
  veterinarian-owned/previous-consultation/clinic patient rules remain intact.
- Then apply the existing `podeLerConsulta` predicate separately to each consultation,
  before retrieving its diagnosis or prescriptions. That predicate is now public and
  explicitly rejects null principals; its role logic is unchanged.
- SYSADMIN: all consultations of the authorized animal.
- VETERINARIO: only consultations assigned to that veterinarian, even when the animal
  is accessible through the clinic or original registration.
- ADMIN_CLINICA: only consultations belonging to that administrator's clinic, after
  the animal clinic access check. Clinicless/other-clinic consultations are excluded.
- RESPONSAVEL: existing active, currently effective AnimalResponsavel linkage is required.
- Unauthenticated API access: 401. Existing animal without permission: 403.
  Nonexistent animal: 404, consistent with existing entity lookup conventions.
- No consultations, or no readable consultations: 200 with patient info and `consultas: []`;
  PDF remains valid and says no consultation is available in the patient's history.
  No hidden consultation count or identifiers are returned.
- PDF I/O failure: user-safe 500 business error. No write operation, external AI call
  or database mutation is performed by these endpoints.

## G. Tests

26 new cases, plus the existing 11 summary-PDF regression cases:

- MVC with real history/PDF/access services and mocked persistence: successful JSON and
  PDF, all requested data fields, empty history, optional birth/tutor/clinic, remote
  address omission, nonfinalized records, legacy missing diagnosis and multipage output.
- Real authorization policy: unauthenticated, unauthorized, nonexistent, vet filtering,
  all consultations hidden, clinic administrator filtering, SYSADMIN and tutor linkage.
- Leakage assertions on JSON and extracted PDF text; no access to diagnosis/prescription
  repositories for unauthorized consultation records or nonfinalized clinical details.
- H2 JPA tests execute the real animal-scoped ordered query and real confirmed-diagnosis
  selector, including other animals, same-date ties and newer unapproved diagnoses.
- Logo alpha/color assertions and identical rendered header regions in both PDFs.

Optional reproducible visual artifacts (synthetic patient data only):

```powershell
.\mvnw.cmd test "-Dtest=AnimalHistoricoControllerMvcTest" "-Darkive.pdf.visual-check=true"
```

Writes PDFs and first-page PNGs under ignored `target/pdf-review/`.
No live Oracle or deployed endpoint test was performed; persistence query tests use H2.

## H. Maven results

Full command: `.\mvnw.cmd test`

Result: BUILD SUCCESS; 523 tests, 0 failures, 0 errors, 0 skipped.
The previous baseline was 497 tests. Existing authentication, onboarding, clinicless,
clinical workflow, prescription, summary PDF and transcription tests remain green.

## I. Constraints and compatibility

- Only Java backend source, backend tests and this report were changed by this task.
- No Mobile, Web/Thymeleaf, Python, deployment, static asset or dependency changes.
- No migrations or entity schema changes. Existing endpoint contracts are preserved;
  the summary PDF's shared header and medication pagination are the only output changes
  to an existing endpoint.
- No commit, staging, reset, rebase or stash was performed.
- The unrelated staged workflow deletion noted above was left untouched.
