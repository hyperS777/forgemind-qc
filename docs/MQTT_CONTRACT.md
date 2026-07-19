# ForgeMind MQTT Contract

Everyone builds against THIS today. Nobody needs real hardware to agree on message shapes —
tomorrow you just make the real device publish/subscribe to these exact topics instead of
a test script.

Broker: Mosquitto running on the AI PC (Snapdragon X Elite). Default port `1883`.
For local testing tonight, run a broker on your own laptop:

```
# macOS: brew install mosquitto && mosquitto -v
# Windows: install from mosquitto.org, then: mosquitto -v
# Or just use test.mosquitto.org for tonight (public, no setup)
```

---

## Topic: `forgemind/machine/telemetry`
Published by: Arduino UNO Q (continuously, ~every 1s)
Subscribed by: AI PC dashboard

```json
{
  "machine_id": "FAN-01",
  "temp_c": 47.2,
  "current_a": 0.41,
  "rpm": 2420,
  "timestamp": "2026-07-18T14:32:10Z"
}
```

## Topic: `forgemind/machine/anomaly`
Published by: Arduino UNO Q (only when TinyML model flags anomaly)
Subscribed by: AI PC

```json
{
  "machine_id": "FAN-01",
  "anomaly_score": 6.8,
  "status": "anomaly_detected",
  "timestamp": "2026-07-18T14:32:11Z"
}
```

## Topic: `forgemind/machine/findings`
Published by: AI PC (after running vision + audio analysis, triggered by phone upload)
Subscribed by: Phone app

```json
{
  "machine_id": "FAN-01",
  "vision": {
    "blade_status": "bent",
    "dust_level": "low"
  },
  "audio": {
    "pattern": "periodic_scraping",
    "spectral_centroid_hz": 3400,
    "periodic": true
  },
  "telemetry": {
    "temp_c": 47.2,
    "current_a": 0.41,
    "rpm": 2420
  },
  "timestamp": "2026-07-18T14:33:00Z"
}
```

## Topic: `forgemind/machine/resolved`
Published by: Phone app (technician marks fixed)
Subscribed by: AI PC dashboard, Arduino UNO Q (resets baseline)

```json
{
  "machine_id": "FAN-01",
  "resolved": true,
  "fault_type": "bent_blade",
  "timestamp": "2026-07-18T15:10:00Z"
}
```

---

## Tonight's build order (no hardware needed for any of this)

1. **RAG server** (`server/rag_server.py`) — takes a `findings` JSON (shape above), retrieves
   matching manual sections, returns a diagnosis. Test it by POSTing fake findings with curl/Postman.
2. **Phone web app** (`app/index.html`) — form that sends findings to the RAG server, displays
   diagnosis, has a Resolve button. Test entirely in a browser, no phone needed tonight.
3. **Audio analysis script** (`scripts/audio_analysis.py`) — record a real "scraping" sound
   with your own laptop mic + a normal fan, run the script, confirm it tells them apart.

Tomorrow, the ONLY thing that changes: the phone app fetches findings from MQTT instead of
a form, and the Arduino/AI PC publish real messages matching these exact shapes.
