# AI Semantic Log — Android take-home kickoff prompt

You are a senior Android engineer pairing with me on a take-home challenge. Build a
prototype of an **AI-Driven Log Viewer**: fetch a large JSON dataset of unstructured log
entries, process them for near-instant search, and render them in a polished Jetpack
Compose UI. The reviewer will judge architecture, performance, code readability, and
tests, not feature count. Prefer fewer things done well over more things done roughly.

## Hard requirements (from the assignment)

- Kotlin, Jetpack Compose, MVVM (unidirectional data flow: ViewModel exposes a single
  `StateFlow<UiState>`, UI sends events up).
- Multi-module Gradle project with clear layer boundaries.
- Retrofit + OkHttp for networking. Use a `@Url` parameter on the Retrofit service method so the encoded `%2F` in the
  path is not double-encoded. Keep the URL in one constant in the network module.
- Hilt for dependency injection.
- Kotlin Coroutines and Flow for all async work. No RxJava, no LiveData.
- Search-as-you-type across 5,000+ entries with no visible lag.
- Transform the raw API response into UI models grouped by 1-minute timestamp bucket
  (sticky headers). The payload has a single `session_id` for all entries, so grouping by it
  would yield one group; show the session id once in the screen header instead.
- Smooth-scrolling list.
- A custom **Severity Indicator** drawn with Compose `Canvas` that reflects the
  density of error logs (a segmented ring or bar is fine).
- A **Details** bottom sheet for a tapped log entry.
- Unit tests for the core business logic and ViewModel state.
- `README.md` with setup instructions and a screen recording.
- `PROMPTS.md` documenting every AI-assisted step.

## Target setup

- minSdk 26, targetSdk/compileSdk 36.1 (newest androidx versions need compileSdk 37, which
  is not installed; stay on current stable androidx versions), Kotlin 2.x, Compose BOM,
  Gradle version catalog (`libs.versions.toml`), KSP for Hilt.
- Modules:
  - `:app` — Hilt application, single activity, DI wiring.
  - `:core:model` — pure Kotlin JVM domain models (`LogEntry`, `Severity`, `LogLoadException`).
    `Severity` holds its colour as a raw `argb: Long`; no logic and no tests in this module.
  - `:core:network` — Retrofit service, DTOs, kotlinx.serialization, OkHttp client.
  - `:core:data` — repository, DTO→domain mappers, in-memory cache, search index.
  - `:core:designsystem` — theme, `SeverityIndicator` and its ring geometry, shared
    composables. Builds each severity `Color` once from `argb`, in an array by ordinal.
  - `:feature:logs` — `LogsViewModel`, `LogsScreen`, list, search bar, details sheet.
- Dependency direction: `app → feature → data → network/model`. `feature` must not
  depend on `network`. `model` depends on nothing. Use `implementation`, never `api`, so
  the direction is enforced by the compiler.
- Every module has its own short `build.gradle.kts` (no build-logic convention plugin).
  All modules except `:core:model` are Android libraries (`:app` is the application).

## Step 0 before writing any code

Download the endpoint once to `tools/sample/logs_5k.json` (gitignored; the single
`.gitignore` line `tools/sample/` is approved). Never print the raw payload into the
conversation. The root is an object `{ total_count, session_id, data: [...] }`, so inspect
it with `jq 'keys'`, `jq '.data | length'`, `jq '.data[0] | keys'`, and
`jq '[.data[].severity] | unique'`. Report the shape in under 20 lines:
field names, types, total count, how severity and session ID are represented. Propose
the domain model and the grouping key based on what you actually see. Wait for my
confirmation before scaffolding.

## Performance design (non-negotiable)

- Fetch once, keep the full list in memory in the repository. Never refetch on search.
- Parse the response as a stream: declare the call as `@Url` + `@Streaming` returning
  `ResponseBody`, and pass `body.byteStream()` to `decodeFromStream` instead of reading
  the body into a `String` first. No Retrofit converter dependency is needed.
- Map DTOs to domain models once and drop the DTO list. Do not keep both alive. (The
  payload root is an object, so entries cannot be streamed one by one; the DTO list exists
  only until mapping finishes. Note this in KDoc.)
- Build a lightweight search index at load time: one precomputed lowercase searchable
  `String` per entry (message + tag + severity + session id + entry id), stored in an array
  parallel to the master list. Search is a substring or whitespace-separated AND-token
  match over that string, run on `Dispatchers.Default`. The search text lives in the index,
  not on `LogEntry`.
- Filtering returns indices into the master list, not copied `LogEntry` objects.
  Build the display list only for the current result.
- In the ViewModel: `query: MutableStateFlow<String>`, debounce ~150 ms,
  `distinctUntilChanged`, `mapLatest` into the filtered + grouped result, `stateIn`
  with `WhileSubscribed(5_000)`. The UI thread must never do filtering. A blank query
  bypasses the debounce so clearing the field is instant (comment this in the code).
- Group once per filter result in the ViewModel. Do not call `groupBy` inside a
  composable. Severity density is computed there too, not in the composable.
- Use `Long`/`Instant` for timestamps internally. Format to display strings in the UI
  mapper with a single cached `DateTimeFormatter`, never per-composition. Build the
  `LogRowUi` array once at load (parallel to the master list) so a search only selects rows
  by index and reuses the same instances, which lets rows skip recomposition.
- `Severity` is an enum with a precomputed colour (`argb: Long` in the model, a `Color`
  array by ordinal in the design system); no string comparison at render.
- `LazyColumn` with stable `key` and `contentType` per item. Group headers via
  `stickyHeader`. Row composables take only primitives or `@Immutable` data so they
  skip recomposition. No `Modifier.height(IntrinsicSize)` or nested scrolling in rows; use
  a fixed-size severity dot rather than a full-height edge bar.
- Lambdas passed to list rows must be stable: pass the item id and a single
  `onClick: (String) -> Unit` from the screen level (`viewModel::onLogClick`). The
  ViewModel resolves the id to an entry with a linear scan on tap.
- The repository is a singleton that caches the loaded dataset; concurrent fetches are
  guarded by a mutex and a failed load is not cached.
- Target cold start to first content under 2 s on an emulator. If parsing takes
  longer, show the list progressively, do not block on the index being ready. Measure
  parse + map + index time on the emulator first; only build progressive loading if the
  budget is exceeded.

## UI/UX bar

- Material 3. Support light and dark, including a `values-night` window theme so dark mode
  does not flash a light window.
- States: loading (skeleton or spinner), error with retry, empty search result, content.
- Search bar pinned at top with a text clear button and the result count ("1,234 of 5,000")
  inside the field. No icon library.
- Row: severity dot, timestamp, truncated message. Tap opens `ModalBottomSheet` with full
  message, metadata, and one Copy button that copies the full entry as text.
- `SeverityIndicator` in the header card (first list item, with legend): `Canvas`-drawn
  ring segmented by FATAL/ERROR/WARN/INFO/DEBUG proportions (FATAL is about 20% of the
  data, so it must not be merged away), animated with `animateFloatAsState` when the
  filter changes, with a `contentDescription` for accessibility.

## Tests

- JUnit 4 + `kotlinx-coroutines-test` + Turbine. Use a `MainDispatcherRule`.
- Pure logic tests (in `:core:data`): DTO→domain mapper, search index matching (case,
  partial, empty query, no match), grouping by minute bucket, severity density math,
  repository fetches once and does not cache failures.
- Share the `runTest` scheduler with any dispatcher passed to code under test; a separate
  `StandardTestDispatcher()` on another scheduler stalls the test.
- `LogsViewModel` tests with a fake repository: initial loading state, success, error +
  retry, query debounce producing filtered state, details selection open/close.
- No Robolectric, Compose UI tests, or instrumentation tests. No tests that assert on
  Compose internals.

## Scope control

- Do not add features not in the assignment: no persistence, no Room, no server
  pagination, no settings screen, no log export, no themes beyond light/dark. If you
  think one is worth it, say so in one line and move on.
- Do not add Timber, Coil, Paging 3, Navigation Compose, or DataStore. A single screen
  plus a bottom sheet needs none of them. Also no Retrofit converter, no material-icons
  library and no OkHttp logging interceptor.
- Do not add Baseline Profiles, R8 tuning, or release signing. Debug build only.
- Ask before adding any dependency not listed here. Approved additions:
  `androidx.hilt:hilt-lifecycle-viewmodel-compose` 1.3.0 (for `hiltViewModel()`) and
  `androidx.lifecycle:lifecycle-runtime-compose` (for `collectAsStateWithLifecycle()`).
- Do not write `README.md` content until Phase 6. `PROMPTS.md` only gets the short per-phase
  entries described under Working style, and is rewritten in Phase 6 (assumption: this
  resolves the conflict between "append before each phase" and "nothing until Phase 6").

## Working style

- Work in phases and stop for my review at the end of each:
  1. Step 0 (inspect JSON) and project scaffold that compiles. The existing single-module
     code is moved into the modules rather than rewritten.
  2. Network + data layer + mappers + tests.
  3. ViewModel + search pipeline + tests.
  4. Compose UI: list, search, states.
  5. SeverityIndicator and details sheet.
  6. README and PROMPTS.md.
- In Phase 1, scaffold all modules with empty `build.gradle.kts` first and confirm the
  graph compiles before writing any real code.
- Run `./gradlew assembleDebug testDebugUnitTest` after every phase. Do not report a
  phase done if it does not compile or tests fail.
- Keep comments purposeful: KDoc on public types and on any non-obvious decision
  (why the index is built this way, why debounce is 150 ms). No comments that restate
  the code.
- Before each phase, append an entry to `PROMPTS.md`: date, tool, the prompt used, what
  was accepted vs. rewritten by hand, and why. This kickoff prompt is entry #1.
- When something is ambiguous but low-stakes, pick the conventional option, note the
  assumption in one line, and keep going. Only stop for choices that change
  architecture or public API.

## Git

- Never run `git commit`, `git push`, or any command that rewrites history (`rebase`,
  `reset --hard`, `checkout -- <file>`, `stash drop`) without my explicit permission
  for that specific action. Permission for one commit does not carry over to the next.
- At the end of each phase, show me `git status` and `git diff --stat`, then propose a
  commit message and wait. I decide when to commit and what goes in it. (The folder is not
  a git repository yet; I will run `git init` myself, and until then skip `git status`.)
- Never create a GitHub repo, open a PR, or push to any remote on my behalf.
- Do not touch `.gitignore` or `.git/` config without asking. The only approved
  `.gitignore` change is the single line `tools/sample/`.

## Context and token discipline

- Do not echo file contents back to me after writing them. Show only the path and a
  one-line summary.
- Prefer targeted edits over regenerating whole files. For a 3-line change, show the
  3 lines.
- Do not re-read files you wrote in this session unless something failed.
- Build output: run Gradle with `-q --console=plain` and show me only the last 30
  lines, and only on failure. On success, one line: "assembleDebug + unit tests green".

## Build time

- Never run `./gradlew clean` or the full `build` task; `assembleDebug` and
  `testDebugUnitTest` are enough.
- Enable `org.gradle.configuration-cache=true`, `org.gradle.caching=true`, and
  `org.gradle.parallel=true` in `gradle.properties` in Phase 1.
- Run only the affected module's tests while iterating:
  `./gradlew :core:data:testDebugUnitTest --tests "*SearchIndexTest*"`. Run the full
  unit suite only at phase boundaries.

## Definition of done

- Fresh clone builds with `./gradlew assembleDebug` and no local secrets.
- Typing in the search field updates the list with no dropped frames on a mid-range
  emulator with 5,000+ entries.
- All unit tests pass.
- README has setup steps, module description, known limitations, and an embedded
  screen recording.
- PROMPTS.md is complete and honest.
- Git history is a readable sequence of commits per phase, each one authored by me.

---

# AI usage log

Tool for every entry: Cursor agent (Claude). Date: 2026-10-18 unless stated otherwise.
"Decisions" records what I chose, changed or rejected, as far as it happened in the chat.
Entry #1 is the kickoff prompt above, written by me.

## #2 Assignment summary and first plan

- **Prompt:** "Read this doc and summarize what I need to do."
- **Produced:** a summary of the take-home and an implementation plan.
- **Decisions:** the plan was only a starting point. The kickoff prompt (#1) replaced it.

## #3 Aligning the implementation with my kickoff prompt

- **Prompt:** I supplied the kickoff prompt and asked the AI to improve its current version.
  I then told it to replace its version with mine and to list where the code differed. We
  went through 13 differences one by one.
- **Produced:** a diff between the kickoff prompt and the code, and the changes needed to
  close each gap.
- **Decisions (mine):**
  - `:core:model` is pure Kotlin JVM with no tests; each module keeps its own short build file.
  - `Severity` holds its colour as `argb: Long`; the ring has five segments, including FATAL.
  - Logs are grouped in 1-minute buckets with sticky headers; the session id is shown once
    in the header; `LogSession` was dropped.
  - Stay on compileSdk 36.1; `tools/sample/` is in `.gitignore`; I run `git init` myself.
  - Streaming JSON decode with no Retrofit converter and no logging interceptor.
  - Search index, `LogRowUi` precomputation and the ring card inside the list were accepted as proposed.
  - Two extra dependencies approved: `hilt-lifecycle-viewmodel-compose` and `lifecycle-runtime-compose`.
  - The Copy button copies the full entry.
  - PROMPTS.md is my kickoff prompt edited in place; the endpoint URL and the recap rule
    were removed at my request.

## #4 Phase 1: module scaffold

- **Prompt:** kickoff prompt, Phase 1.
- **Produced:** the six-module graph (`app`, `core:model`, `core:network`, `core:data`,
  `core:designsystem`, `feature:logs`), version catalog and Gradle setup.
- **Decisions:** `feature:logs` uses `implementation` only. The AI proved it cannot see
  `core.network` by adding a probe reference that failed to compile.

## #5 Phase 2: network and data layer

- **Prompt:** kickoff prompt, Phase 2.
- **Produced:** the Retrofit `@Streaming` API with `decodeFromStream`, DTO-to-domain
  mapping, a repository with a mutex-guarded cache that does not cache failures,
  `SearchIndex`, minute grouping, severity stats, and 41 tests.
- **Decisions:** the data design was accepted as proposed (see #3).

## #6 Phase 3: ViewModel and search pipeline

- **Prompt:** kickoff prompt, Phase 3.
- **Produced:** `LogsViewModel` with a 150 ms debounce that blank queries skip,
  `mapLatest` on an injected dispatcher, `stateIn(WhileSubscribed(5_000))`, and 19 tests
  using hand-written fakes and Turbine.
- **Decisions:** one `StateFlow<LogsUiState>`; rows are built once at load and reused.

## #7 Phases 4 and 5: Compose UI, severity ring, details sheet

- **Prompt:** kickoff prompt, Phases 4 and 5.
- **Produced:** the list screen, the Canvas severity ring with tested geometry, the details
  bottom sheet, and the night theme.
- **Decisions:** checked on an emulator afterwards (see "How it was verified").

## #8 Unit tests

- **Prompt:** kickoff prompt, Tests section.
- **Produced:** 67 unit tests across `core:data`, `core:network`, `core:designsystem` and
  `feature:logs`. No mocked network, no Robolectric, no UI tests.

## #9 Code review and fix session

- **Prompts:** "review code in current repo and tell me what could be improved", then "help me
  fix them", then three small UI requests from me (count text overlapping the search border;
  hide the session id; keep the search bar on one line), then "help me review again".
- **Produced:** a prioritized review, then these changes:
  - `LogsUiState` became `query` plus a sealed `Loading | Error | Loaded` content.
  - New `LogDataset.search(query)` so the ViewModel no longer works with positional indices.
  - `LogDataset.droppedCount` reports entries discarded while mapping.
  - The search query is kept in `SavedStateHandle`; the details sheet closes on reload.
  - The ring intro animation plays once instead of every time it scrolls back into view.
  - The Copy text is built from string resources; "Raw metadata" was relabeled "Metadata"
    because it shows only the fields the app models.
  - The "Copied" label resets after 2 seconds; rows have a click label and button role.
  - Cleanup: template code in the theme files, unused strings, catalog entries and colors,
    `allowBackup=false`, a redundant manifest label, an obsolete `mipmap-anydpi-v26` folder.
  - The search field is disabled while loading or after a failed load.
  - Row times and group headers use `DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM)` and a
    localized date plus short time, so 12-hour locales show AM/PM. `LogFormatters` takes a
    `Locale` so tests are deterministic.
  - 13 new tests (80 in total), including search pipeline, dropped entries, formatters,
    saved query and details closing on retry.
- **Decisions (mine):**
  - Hide the session id and shorten the search placeholder to "Search logs". This overrides
    the kickoff prompt, which asked for the session id in the screen header.
  - I made the commit myself.
  - Chose the locale-based time style over following the device's 12/24-hour toggle.
  - Deliberately left alone: R8 and release tuning, Gradle convention plugins and Compose UI
    tests (all excluded by the kickoff prompt), dependency upgrades (compileSdk 37 is not
    installed), and the 150 ms debounce on a restored query. The demo video was re-recorded
    and shrunk from a 26 MB WebM to a 0.7 MB H.264 MP4 (`docs/demo.mp4`, audio removed) with
    `ffmpeg`; the old 34 MB `docs/demo.webm` was removed from the working tree but stays in git
    history.
- **Where the AI was wrong:** it first called the ViewModel's second `flowOn` redundant. It is
  not: the repository's `flowOn` only covers the repository, and the ViewModel's covers row
  formatting. The AI retracted the claim and left the code unchanged.
- **Checked:** `./gradlew testDebugUnitTest assembleDebug lintDebug` passes with 0 lint
  errors. These changes were not re-run on the emulator, so the UI edits are not verified
  on a device.

**Hand edits by me:** _fill in before submitting: anything I wrote or changed myself, and
any other AI tools I used._

## Where the AI was wrong, and what I overrode

- Technical fixes made after the AI's first attempt:
  - `hilt-lifecycle-viewmodel-compose` 1.4.0 needs compileSdk 37, so it was pinned to 1.3.0.
  - Compiler warnings were fixed: annotation targets on constructor parameters and a
    deprecated `hiltViewModel` import.
  - `LogsViewModel` had a fully qualified type and an upcast.
  - A test asserted a locale-dependent month name.
  - A ViewModel test expected an emission that never happens when state is unchanged.
- Process mistakes I corrected:
  - The AI built several phases past my review checkpoints.
  - It printed raw payload bytes into the chat.
  - It put the sample payload in `/tmp` instead of the project.
  - It first wrote its own PROMPTS.md instead of editing mine.

## How it was verified

- `./gradlew assembleDebug testDebugUnitTest`: build succeeds and 80 tests pass, 0 failures
  (67 before the review session in #9). `lintDebug` reports 0 errors.
- Module boundary checked with a probe that fails to compile.
- Run on the `Medium_Phone` emulator (API 37, arm64, 1080x2400) with the real 5,000-entry endpoint:
  - Load: fetch + decode 1.5-1.75 s (mostly network), map + sort + index about 55 ms, over four
    cold starts. Under the 2 s threshold, so no progressive loading was built.
  - Scroll: 274 frames over eight fast flings, 1.09% janky, p99 20 ms.
  - Search: typing "cache" (1,595 of 5,000 match) shows no frame over about 37 ms; the worst case
    is one two-frame hitch when a new result set is applied.
  - Checked on screen: ring and counts follow the filter, the no-results state, and the
    details sheet (severity chip, Copy, local and UTC time, raw metadata).
- The emulator measurements and on-screen checks above predate the review session (#9) and were
  not repeated afterwards.
- Not yet checked: offline/error state on device, rotation, light theme, and the Copy button's
  clipboard content. Numbers come from an emulator, not a physical device.
