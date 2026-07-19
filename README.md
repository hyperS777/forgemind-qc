# ForgeMind
## AI-Powered Industrial Predictive Maintenance System

ForgeMind is an intelligent industrial monitoring and predictive maintenance platform that combines IoT sensing, real-time anomaly detection, machine safety automation, LLM-powered diagnostics, and a mobile application to reduce downtime and detect machine failures before they become critical.

# Problem Statement

## Industrial machinery often fails due to:

Excessive vibration
Overheating
Bearing wear
Shaft misalignment
Mechanical imbalance
Poor maintenance schedules

Traditional threshold-based systems only react after a fault becomes severe.

ForgeMind introduces an AI-driven pipeline that continuously monitors machine health, detects anomalies in real time, automatically triggers safety actions, and generates intelligent maintenance insights.

# Features
- Real-Time Sensor Monitoring
  - Temperature Monitoring (DHT11)
  - Humidity Monitoring (DHT11)
  - Vibration Monitoring (ADXL345)
- Machine Protection
  - Automatic Relay Shutdown
  - Emergency Buzzer Alerts
  - Visual Status Indicators (LEDs)
- AI Anomaly Detection
  - Isolation Forest model trained on healthy operating data
  - Detects deviations from normal machine behavior
  - Generates anomaly scores in real time
- Intelligent Diagnostics
  - Raw anomalous sensor packets forwarded to LLM layer
  - Root-cause analysis generation
- Maintenance recommendations
  -Failure explanation
-Mobile Application
  -Image Upload
  -Audio Upload
  -Audio Recording
  -Sensor Data Submission
  -Diagnosis Dashboard

# System Architecture

```mermaid
flowchart TD

    A[Industrial Machine]

    A --> B[ADXL345 Vibration Sensor]
    A --> C[DHT11 Temperature & Humidity Sensor]

    B --> D[Arduino UNO R4 WiFi]
    C --> D

    D --> E[Safety Layer]

    E --> F{Threshold Check}

    F -->|Critical| G[Relay OFF]
    F -->|Critical| H[Buzzer ON]
    F -->|Critical| I[Red LED]

    D --> J[Serial Data Stream]

    J --> K[Python Anomaly Detection]

    K --> L[Isolation Forest Model]

    L -->|Healthy| M[Continue Monitoring]

    L -->|Anomaly| N[Raw Sensor Packet]

    N --> O[LLM Diagnostic Engine]

    O --> P[Root Cause Analysis]
    O --> Q[Maintenance Recommendations]
    O --> R[Severity Assessment]

    P --> S[FastAPI Backend]
    Q --> S
    R --> S

    S --> T[Android Application]

    T --> U[Live Diagnosis Dashboard]
    T --> V[Image Upload]
    T --> W[Audio Upload]
    T --> X[Audio Recording]

```

# System Workflow

- ForgeMind continuously monitors industrial equipment using vibration and environmental sensors connected to an Arduino UNO R4. Sensor data is streamed to an anomaly detection engine powered by an Isolation Forest model trained on healthy operational behavior.

- When abnormal patterns are detected, the raw sensor packet is forwarded to an LLM-powered diagnostic layer running on Qualcomm AI hardware. The model performs root-cause analysis, generates maintenance recommendations, and assigns severity levels.

- Results are exposed through a FastAPI backend and visualized in a Kotlin-based Android application, enabling operators to receive actionable maintenance intelligence in real time.

# Hardware Components

```mermaid
flowchart TB

subgraph Hardware_Components

A[Arduino UNO R4 WiFi<br>Main Controller]

B[ADXL345<br>Vibration Monitoring]

C[DHT11<br>Temperature & Humidity]

D[Relay Module<br>Machine Shutdown]

E[Active Buzzer<br>Emergency Alert]

F[Green LED<br>Healthy Status]

G[Red LED<br>Fault Status]

end
```

# Software Stack

```mermaid
flowchart TB

subgraph Software_Stack

A[Arduino IDE<br>Embedded Firmware]

B[Python<br>Data Processing]

C[Scikit-Learn<br>Isolation Forest]

D[FastAPI<br>Backend Services]

E[Uvicorn<br>API Server]

F[Kotlin<br>Android Application]

G[Jetpack Compose<br>UI Framework]

H[Retrofit<br>API Communication]

I[LLM Engine<br>Root Cause Analysis]

end
```
