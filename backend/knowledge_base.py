"""
ForgeMind — Structured Knowledge Base for Industrial Fan Fault Detection.

Each fault profile contains:
  - Telemetry signature ranges (temp, current, rpm, anomaly_score)
  - Audio keywords that Sarvam STT or Gemini might detect
  - Severity, root cause, repair steps, and recommendations
"""

FAULT_PROFILES = [
    {
        "id": "bent_blade",
        "title": "Bent Fan Blade",
        "audio_signatures": [
            "scraping", "periodic", "clicking", "tick", "ticking",
            "rubbing", "metallic", "cyclical", "uneven",
        ],
        "telemetry": {
            "temp_c": (42, 50),
            "current_a": (0.38, 0.48),
            "rpm": (2300, 2500),
            "anomaly_score": (5.0, 8.0),
        },
        "severity": "High",
        "root_cause": "Deformed or bent blades contacting housing or guard.",
        "repair_steps": [
            "Power down and lockout the machine.",
            "Remove the fan grille / housing cover.",
            "Visually inspect each blade for bending or deformation.",
            "Straighten the blade with pliers or replace the fan assembly.",
            "Reassemble and test.",
        ],
        "recommendation": "Immediately stop the fan. Inspect blades for bending. Straighten or replace the damaged blade before restarting.",
    },
    {
        "id": "dust_buildup",
        "title": "Dust Buildup / Airflow Restriction",
        "audio_signatures": [
            "muffled", "whooshing", "reduced airflow", "straining",
            "labored", "whirring", "low hum", "restricted",
        ],
        "telemetry": {
            "temp_c": (48, 56),
            "current_a": (0.35, 0.42),
            "rpm": (2100, 2350),
            "anomaly_score": (2.5, 4.5),
        },
        "severity": "Medium",
        "root_cause": "Accumulated dust/debris on blades or in housing restricting airflow.",
        "repair_steps": [
            "Power down the unit.",
            "Use compressed air to blow dust off blades and housing.",
            "Wipe down blades with a damp cloth.",
            "Clean or replace the air filter if applicable.",
            "Reassemble and verify airflow is restored.",
        ],
        "recommendation": "Schedule a cleaning cycle. Remove dust from blades and housing. Check and replace air filters.",
    },
    {
        "id": "worn_bearing",
        "title": "Worn Bearing",
        "audio_signatures": [
            "grinding", "squealing", "high-pitched", "whining",
            "continuous", "rumble", "rough", "growl", "screech",
        ],
        "telemetry": {
            "temp_c": (50, 62),
            "current_a": (0.40, 0.65),
            "rpm": (1800, 2200),
            "anomaly_score": (4.0, 7.0),
        },
        "severity": "High",
        "root_cause": "Motor shaft bearings are worn, causing metal-on-metal friction.",
        "repair_steps": [
            "Power down and lockout.",
            "Disassemble the motor housing.",
            "Remove the worn bearing using a bearing puller.",
            "Press-fit a new bearing of the same specification.",
            "Lubricate, reassemble, and test.",
        ],
        "recommendation": "Replace the worn bearing immediately. Continued operation risks motor seizure and permanent damage.",
    },
    {
        "id": "loose_mount",
        "title": "Loose Mounting / Vibration",
        "audio_signatures": [
            "rattling", "vibrating", "buzzing", "shaking",
            "loose", "clanking", "wobble", "resonance", "chattering",
        ],
        "telemetry": {
            "temp_c": (36, 43),
            "current_a": (0.32, 0.40),
            "rpm": (2350, 2550),
            "anomaly_score": (5.5, 8.5),
        },
        "severity": "High",
        "root_cause": "Mounting bolts or bracket have loosened, causing excessive vibration.",
        "repair_steps": [
            "Power down the unit.",
            "Check all mounting bolts and brackets for looseness.",
            "Tighten bolts to the specified torque.",
            "Add thread-locking compound if bolts have loosened repeatedly.",
            "Add vibration-dampening pads if needed.",
        ],
        "recommendation": "Tighten all mounting bolts immediately. Add vibration dampeners if the issue recurs.",
    },
    {
        "id": "motor_overload",
        "title": "Motor Overload / Electrical Fault",
        "audio_signatures": [
            "humming", "buzzing", "straining", "electrical",
            "surging", "pulsing", "intermittent", "stalling",
            "struggling", "overheating",
        ],
        "telemetry": {
            "temp_c": (58, 80),
            "current_a": (0.70, 1.50),
            "rpm": (800, 1600),
            "anomaly_score": (7.0, 10.0),
        },
        "severity": "Critical",
        "root_cause": "Motor drawing excessive current due to electrical fault, seized component, or overload.",
        "repair_steps": [
            "IMMEDIATELY power down the unit.",
            "Check the motor windings for shorts or damage.",
            "Verify the power supply voltage is within spec.",
            "Check for mechanical obstructions preventing rotation.",
            "Replace the motor if windings are damaged.",
        ],
        "recommendation": "CRITICAL: Power down immediately. Risk of fire or permanent motor damage. Inspect windings and power supply before restarting.",
    },
]

# Healthy baseline for comparison
HEALTHY_PROFILE = {
    "id": "healthy",
    "title": "Normal Operation",
    "telemetry": {
        "temp_c": (35, 42),
        "current_a": (0.30, 0.40),
        "rpm": (2400, 2600),
        "anomaly_score": (0.0, 1.0),
    },
}


def get_all_faults():
    """Return all fault profiles."""
    return FAULT_PROFILES


def get_fault_by_id(fault_id: str):
    """Look up a specific fault profile by ID."""
    for f in FAULT_PROFILES:
        if f["id"] == fault_id:
            return f
    return None


def get_kb_context_string():
    """Build a textual summary of the entire knowledge base for LLM prompting."""
    lines = ["KNOWN INDUSTRIAL FAN FAULTS:"]
    for f in FAULT_PROFILES:
        t = f["telemetry"]
        lines.append(
            f"\n## {f['title']} (ID: {f['id']})\n"
            f"  Severity: {f['severity']}\n"
            f"  Audio Signatures: {', '.join(f['audio_signatures'])}\n"
            f"  Telemetry Ranges: Temp {t['temp_c'][0]}-{t['temp_c'][1]}°C, "
            f"Current {t['current_a'][0]}-{t['current_a'][1]}A, "
            f"RPM {t['rpm'][0]}-{t['rpm'][1]}, "
            f"Anomaly {t['anomaly_score'][0]}-{t['anomaly_score'][1]}\n"
            f"  Root Cause: {f['root_cause']}\n"
            f"  Recommendation: {f['recommendation']}"
        )
    return "\n".join(lines)
