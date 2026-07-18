# ForgeMind App (Phone)

Kotlin Multiplatform + Compose, mirroring the layered architecture used by
QSense-App: `domain/model`, `domain/usecase`, `domain/service`, `data/*`,
`presentation/*`, `di/AppContainer.kt`. Content and logic (fault taxonomy,
audio+vision fusion, prompt design) are ForgeMind's own.

## What's built right now (works tonight, no hardware/network needed)

- **`domain/model`** — `Findings`, `Diagnosis`, `ResolvedAck` matching the
  MQTT contract in `../docs/MQTT_CONTRACT.md`
- **`domain/knowledge` + `data/knowledge/InMemoryKnowledgeBase.kt`** — 5 fault
  entries (bent blade, dust, worn bearing, loose mount, motor overload) with
  keyword-based retrieval. This IS your RAG — no vector DB needed for a demo
  this size.
- **`domain/prompt/DiagnosisPrompt.kt`** — builds the LLM prompt that fuses
  telemetry + vision + audio evidence, this is ForgeMind's actual
  differentiator vs. single-signal detection
- **`domain/parse/DiagnosisParser.kt`** — parses model output, with a safe
  fallback to the top RAG match if the model returns malformed JSON (small
  models do this often — don't let it break your demo)
- **`domain/usecase/GenerateDiagnosisUseCase.kt`** — the whole pipeline in
  one place: retrieve → prompt → generate → parse
- **`data/mqtt/FakeMqttGateway.kt`** + **`data/llm/FakeTextGenerator.kt`** —
  let the entire app run end-to-end with zero broker and zero model
- **`presentation/diagnosis/`** — ViewModel + Compose screen showing the
  diagnosis, with a Resolve button
- **`di/AppContainer.kt`** — single file wiring everything together; this is
  the ONLY file that changes tomorrow

## Run it tonight

**Fastest — plain console, no emulator, no Android Studio:**
```bash
cd App
./gradlew :shared:run
```
This runs `ConsoleDemo.kt`, which feeds all 4 sample scenarios through the
real pipeline and prints which fault each one resolves to. If `bent_blade`
comes back for the bent-blade scenario, your retrieval + prompt + parse logic
is confirmed working before you've touched any hardware.

> **No `gradlew` wrapper jar is in this scaffold yet** (built offline, no
> network here). First thing tonight, from a machine with internet:
> ```bash
> cd App
> gradle wrapper --gradle-version 8.7
> ```
> That generates `gradlew`, `gradlew.bat`, and the wrapper jar. Do this once
> and commit it.

**On an actual phone/emulator:** open `App/` in Android Studio, run the
`androidApp` module. You'll see a "Simulate anomaly" button — tap it, watch
the diagnosis appear, tap Resolve. Delete that button tomorrow once real
findings arrive over MQTT.

## Tomorrow — exactly what changes, in order

1. **`AppContainer.mqttGateway`**: replace `FakeMqttGateway()` with a real
   implementation (Eclipse Paho or HiveMQ MQTT client for Android) pointed at
   the AI PC's broker IP. Implement the same `MqttGateway` interface —
   nothing else in the app needs to know.
2. **`AppContainer.textGenerator`**: replace `FakeTextGenerator()` with an
   HTTP call to LM Studio running on the AI PC (safe fallback — no NPU export
   needed). Only reach for on-device Genie/NPU if there's time left; it's the
   riskiest piece per the AI Hub developer guide.
3. Remove the "Simulate anomaly" test button in `MainActivity.kt`.
4. Everything in `domain/` and `presentation/` should not need to change. If
   you find yourself editing a use case to make the real MQTT/LLM work,
   that's a sign the interface leaked — fix the interface instead.

## Testing without touching the UI

`demo/SampleFindings.kt` has 4 pre-built scenarios (bent blade, dust, worn
bearing, motor overload) matching the entries in the knowledge base. Use
these to sanity-check retrieval any time you change `InMemoryKnowledgeBase.kt`
— just call `AppContainer.generateDiagnosisUseCase.invoke(SampleFindings.X)`
from `ConsoleDemo.kt` or a unit test.

## Prepare repository for GitHub (cleanup generated files)

Follow these steps to ensure only source and build configuration are tracked.

- Dry-run: inspect current tracked generated files
```bash
cd App
git status --porcelain
git ls-files | grep -E "(^\.idea/|\.iml$|/build/|local.properties$|google-services.json$|\.keystore$)" || true
```

- Safe untrack (recommended): run the helper script corresponding to your shell:

- Bash/WSL/macOS:
```bash
./scripts/untrack_generated_files.sh
```

- PowerShell (Windows):
```powershell
.\scripts\untrack_generated_files.ps1
```

- Manual commands (if you prefer to run them directly):
```bash
# from repo root
git add App/.gitignore
git commit -m "chore: add Android Studio .gitignore"

# untrack common IDE/generated files (keeps them locally)
git rm -r --cached --ignore-unmatch App/.idea || true
git rm --cached --ignore-unmatch "App/*.iml" || true
git rm -r --cached --ignore-unmatch App/**/build || true
git rm --cached --ignore-unmatch App/local.properties || true
git rm --cached --ignore-unmatch "App/**/*.keystore" || true
git rm --cached --ignore-unmatch "App/**/google-services.json" || true
git add -A
git commit -m "chore: remove generated/IDE files from tracking"
```

After running the script or commands, verify with:
```bash
git status
```

### Verification (open & build)
- Open `App/` in Android Studio.
- Let Gradle sync finish. If prompted about missing wrapper jar, run:
```bash
cd App
gradle wrapper --gradle-version 8.7
```
- Build → Rebuild Project. Or from command line:
```bash
cd App
./gradlew clean assembleDebug    # use gradlew.bat on Windows
```

If build succeeds, repository is clean and buildable.
