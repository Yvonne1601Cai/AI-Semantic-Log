# AI Semantic Log

An Android log viewer. It loads about 5,000 log entries from a JSON endpoint, groups them by
time, and lets you search them as you type. A Canvas ring shows how many of the visible entries
are errors, and tapping a row opens its details.

Kotlin, Jetpack Compose (Material 3), Hilt, Retrofit/OkHttp, kotlinx.serialization,
Coroutines and Flow.

## Screen recording

[Watch the demo](docs/demo.webm) (about 33 MB, WebM).

## Setup

Requirements: Android Studio with a JDK 17 or newer (the Android Studio bundled JDK 21 was used),
and an Android SDK with API 36.1 installed. `minSdk` is 26.

From a terminal, Gradle needs a JDK on the path. If `./gradlew` reports "Unable to locate a Java
Runtime", point `JAVA_HOME` at Android Studio's bundled one, for example
`export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`.

```bash
./gradlew assembleDebug        # build
./gradlew installDebug         # install on a connected device or emulator
./gradlew testDebugUnitTest    # run all unit tests
```

Or open the folder in Android Studio and run the `app` configuration. The app needs internet
access. There are no API keys or local secrets; the endpoint is a single constant in
`core/network/.../LogEndpoint.kt`.

## Architecture

Multi-module Clean Architecture with MVVM and a single `StateFlow` of UI state. The state is a
`LogsUiState(query, content)` where `content` is a sealed `Loading | Error | Loaded`, so an invalid
combination (for example loading and error at once) cannot be represented. The search query is kept
in `SavedStateHandle`, so it survives process death.

```
app -> feature:logs -> core:data -> core:network
                 \
                  +--> core:designsystem
```

`core:network`, `core:data`, `core:designsystem` and `feature:logs` all depend on `core:model`.
`app` also depends on `core:data`, `core:network` and `core:designsystem` so Hilt can aggregate
their modules.

| Module | Responsibility |
|---|---|
| `:app` | Hilt application, single activity, theme wiring. |
| `:core:model` | Pure Kotlin domain types: `LogEntry`, `Severity`, `LogLoadException`. |
| `:core:network` | Retrofit service, DTOs, the remote data source. Hides all HTTP and serialization types. |
| `:core:data` | Repository and cache, search index, minute grouping, severity stats, DTO-to-domain mapping. `LogDataset.search(query)` runs search, grouping and stats in one call. |
| `:core:designsystem` | Theme, severity colors, and the Canvas `SeverityIndicator`. |
| `:feature:logs` | `LogsViewModel`, UI state, and the Compose screen: list, search field, ring card, details sheet. |

`feature:logs` depends on `core:data` through `implementation`, so it cannot see any network type.
This was checked with a probe reference that failed to compile.

## How it works

**Loading.** One streaming GET (`@Streaming`) decodes the body straight from the byte stream with
`decodeFromStream`, so the 1.4 MB response is never held as a `String`. The payload root is an
object, so entries cannot be decoded one by one. The repository maps, sorts newest first and
indexes the result once, then caches it in memory. Entries that cannot be used (blank id, unparsable timestamp, duplicate id) are dropped and counted in `LogDataset.droppedCount`. Failed loads are not cached, so retry works.

**Search.** Each entry gets one lowercase text built from its message, tag, severity, session id
and id. A query is split on whitespace and every term must match (AND). The result is an array of
positions into the master list, in ascending order. Because the list is already sorted, grouping
the matches is a single linear pass. The ViewModel debounces typing by 150 ms (a blank query skips
the debounce, so Clear is instant), drops stale queries with `mapLatest`, and runs the work on an
injected dispatcher.

**Grouping.** Entries are grouped into 1-minute buckets with sticky headers showing the count and
the error share. The payload has a single `session_id`, so it is not a grouping level and is not
shown in the UI (it is still searchable).

**Severity indicator.** A custom Canvas ring with five segments (Debug, Info, Warn, Error, Fatal).
The center shows the error percentage (Error plus Fatal). It always describes the entries that
match the current search. The arc geometry is a pure function with its own unit tests.

**Details.** Tapping a row opens a `ModalBottomSheet` with the severity, message, id, local and UTC
time, tag, latency, AI flag and the metadata fields the app models, shown as JSON. Copy puts the
whole entry on the clipboard as text with localized labels.

**List performance.** Rows are formatted once at load and reused, so they skip recomposition when
the filter changes. `LazyColumn` items have stable keys and content types, and the row click
callback takes a plain id.

## Testing

`./gradlew testDebugUnitTest` runs 78 JVM unit tests, all passing:

| Module | Tests | Covers |
|---|---|---|
| `core:data` | 40 | search index, grouping, severity stats, mapping, repository caching and failure handling |
| `core:network` | 6 | remote data source against a fake API, including error mapping |
| `core:designsystem` | 7 | ring arc geometry |
| `feature:logs` | 25 | ViewModel state (loading, debounce, latest query wins, filtered ring, errors and retry, details) and UI mapping |

Tests use hand-written fakes and Turbine. There is no mocking of the network, and no
Robolectric, Compose UI or instrumentation tests.

## Measured performance

Run on the `Medium_Phone` emulator (API 37, arm64) against the real endpoint:

- Load: fetch and decode 1.5 to 1.75 s (mostly network), map, sort and index about 55 ms.
- Scrolling: 274 frames over eight fast flings, 1.09% janky, p99 20 ms.
- Search: typing "cache" (1,595 of 5,000 match) showed no frame over about 37 ms.

These are emulator numbers, not a physical device.

## Known limitations

- The whole payload is loaded in one request and kept in memory. The endpoint has no pages, and
  search needs every entry, so there is no pagination and no on-disk cache. A failed load shows an
  error state with a Retry button (implemented and unit-tested at the ViewModel level, but not
  yet exercised on a device).
- Search is case-insensitive substring matching, with every space-separated term required. It is
  not fuzzy and does not rank results.
- The payload has one session, so "group by session" is not exercised.
- Checked on an emulator in portrait with the dark theme. Rotation, light theme and a physical
  device were not checked.
- The UI itself (list, sheet, ring drawing) is verified by hand, not by automated UI tests.

## AI usage

See [`PROMPTS.md`](PROMPTS.md) for the kickoff prompt and a log of where AI tools were used.
