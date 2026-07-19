"""YAMNet-based audio classifier for industrial fan anomaly detection.

Loads a YAMNet TFLite model and maps its 521-class predictions to
fan-specific fault categories defined in ``knowledge_base.py``.

The classifier acts as a complementary signal alongside the existing
spectral feature extraction in ``audio_analyzer.py``.
"""

from __future__ import annotations

import csv
import logging
import os
import subprocess
import tempfile
from pathlib import Path

import numpy as np

logger = logging.getLogger("forgemind.yamnet")

# ─── YAMNet class indices that matter for fan diagnostics ───────────────

# Normal fan operation indicators
NORMAL_FAN_CLASSES = {
    406: "Mechanical fan",
    407: "Air conditioning",
    482: "Whir",
}

# Anomaly classes mapped to fault IDs from knowledge_base.py
ANOMALY_CLASS_MAP: dict[int, tuple[str, str]] = {
    # (yamnet_index): (fault_id, description)
    # ── Worn bearing indicators ──
    479: ("worn_bearing", "Squeal"),
    355: ("worn_bearing", "Squeak"),
    480: ("worn_bearing", "Creak"),
    487: ("worn_bearing", "Rumble"),
    # ── Bent blade / scraping indicators ──
    469: ("bent_blade", "Scrape"),
    485: ("bent_blade", "Clicking"),
    486: ("bent_blade", "Clickety-clack"),
    # ── Loose mount / vibration indicators ──
    517: ("loose_mount", "Vibration"),
    130: ("loose_mount", "Rattle"),
    516: ("loose_mount", "Throbbing"),
    483: ("loose_mount", "Clatter"),
    # ── Motor / electrical fault indicators ──
    490: ("motor_overload", "Hum"),
    510: ("motor_overload", "Mains hum"),
    125: ("motor_overload", "Buzz"),
    392: ("motor_overload", "Buzzer"),
    337: ("motor_overload", "Engine"),
    # ── Generic mechanical anomaly ──
    398: ("dust_buildup", "Mechanisms"),
    403: ("dust_buildup", "Gears"),
    404: ("dust_buildup", "Pulleys"),
}

# Minimum average score for an anomaly class to be considered significant
ANOMALY_SCORE_THRESHOLD = 0.08
# Minimum normal-fan score to consider the recording as containing fan sound
NORMAL_FAN_THRESHOLD = 0.05

# ─── Model loading ─────────────────────────────────────────────────────

_interpreter = None
_input_details = None
_output_details = None
_class_names: list[str] = []
_model_available = False


def _load_class_map(csv_path: str) -> list[str]:
    """Load YAMNet class names from the CSV file."""
    names = []
    if os.path.exists(csv_path):
        with open(csv_path, "r", encoding="utf-8") as f:
            reader = csv.DictReader(f)
            for row in reader:
                names.append(row["display_name"])
    return names


def _init_model():
    """Lazily load the YAMNet TFLite model on first use."""
    global _interpreter, _input_details, _output_details, _class_names, _model_available

    if _model_available or _interpreter is not None:
        return

    model_dir = Path(__file__).parent
    model_path = model_dir / "yamnet.tflite"
    class_map_path = model_dir / "yamnet_class_map.csv"

    if not model_path.exists():
        logger.warning("yamnet.tflite not found in %s — YAMNet disabled.", model_dir)
        return

    if not class_map_path.exists():
        logger.warning("yamnet_class_map.csv not found — YAMNet disabled.")
        return

    try:
        import ai_edge_litert.interpreter as tflite

        _interpreter = tflite.Interpreter(model_path=str(model_path))
        _interpreter.allocate_tensors()
        _input_details = _interpreter.get_input_details()
        _output_details = _interpreter.get_output_details()
        _class_names = _load_class_map(str(class_map_path))
        _model_available = True
        logger.info("YAMNet model loaded successfully (%d classes).", len(_class_names))
    except Exception as e:
        logger.warning("Failed to load YAMNet model: %s — YAMNet disabled.", e)
        _model_available = False


def is_available() -> bool:
    """Return True if the YAMNet model was loaded successfully."""
    _init_model()
    return _model_available


# ─── Audio loading ─────────────────────────────────────────────────────

MAX_DURATION_S = 12


def _load_audio_as_float32(audio_path: str) -> np.ndarray | None:
    """Load audio file and resample to 16 kHz mono float32.

    Tries librosa first; falls back to imageio-ffmpeg transcoding for
    phone formats (AAC/M4A) that librosa can't handle natively.
    """
    try:
        import librosa

        waveform, _ = librosa.load(audio_path, sr=16000, mono=True, duration=MAX_DURATION_S)
        return waveform.astype(np.float32)
    except Exception:
        pass

    # Fallback: transcode with ffmpeg, then retry
    try:
        import imageio_ffmpeg

        with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
            decoded_path = tmp.name
        cmd = [
            imageio_ffmpeg.get_ffmpeg_exe(),
            "-y", "-v", "error", "-i", audio_path,
            "-t", str(MAX_DURATION_S), "-ac", "1", "-ar", "16000",
            decoded_path,
        ]
        subprocess.run(cmd, check=True, capture_output=True, timeout=30)

        import librosa

        waveform, _ = librosa.load(decoded_path, sr=16000, mono=True)
        return waveform.astype(np.float32)
    except Exception as e:
        logger.warning("Could not load audio for YAMNet: %s", e)
        return None
    finally:
        if "decoded_path" in locals():
            Path(decoded_path).unlink(missing_ok=True)


# ─── Inference ─────────────────────────────────────────────────────────

def _run_yamnet(waveform: np.ndarray) -> np.ndarray | None:
    """Run YAMNet inference and return the per-frame score matrix.

    Returns shape (num_frames, 521) or None on failure.
    """
    _init_model()
    if not _model_available or _interpreter is None:
        return None

    try:
        _interpreter.resize_tensor_input(_input_details[0]["index"], waveform.shape)
        _interpreter.allocate_tensors()
        _interpreter.set_tensor(_input_details[0]["index"], waveform)
        _interpreter.invoke()

        scores = _interpreter.get_tensor(_output_details[0]["index"])
        return scores
    except Exception as e:
        logger.warning("YAMNet inference failed: %s", e)
        return None


# ─── Fan anomaly classification ────────────────────────────────────────

def classify_fan_audio(audio_path: str | None) -> dict:
    """Classify an audio file and return fan-specific anomaly assessment.

    Returns a dict with:
        available (bool): Whether classification ran successfully.
        is_abnormal (bool): Whether unusual noise was detected.
        abnormal_confidence (float): 0.0–1.0 confidence in abnormality.
        fault_scores (dict): Per-fault-id scores from YAMNet classes.
        detected_sounds (list): Top detected YAMNet classes with scores.
        yamnet_evidence (str): Human-readable evidence string.
    """
    if not audio_path or not Path(audio_path).is_file():
        return {"available": False, "reason": "no_audio"}

    _init_model()
    if not _model_available:
        return {"available": False, "reason": "yamnet_model_not_loaded"}

    waveform = _load_audio_as_float32(audio_path)
    if waveform is None or len(waveform) < 1600:  # < 0.1s at 16kHz
        return {"available": False, "reason": "audio_too_short_or_unreadable"}

    scores = _run_yamnet(waveform)
    if scores is None:
        return {"available": False, "reason": "yamnet_inference_failed"}

    # Average scores across all frames
    if len(scores.shape) > 1:
        avg_scores = np.mean(scores, axis=0)
    else:
        avg_scores = scores

    # ── Compute normal fan score ──
    normal_fan_score = sum(
        float(avg_scores[idx]) for idx in NORMAL_FAN_CLASSES if idx < len(avg_scores)
    )

    # ── Compute per-fault anomaly scores ──
    fault_scores: dict[str, float] = {}
    anomaly_details: list[tuple[float, str, str]] = []  # (score, fault_id, class_name)

    for class_idx, (fault_id, class_name) in ANOMALY_CLASS_MAP.items():
        if class_idx >= len(avg_scores):
            continue
        score = float(avg_scores[class_idx])
        if score > ANOMALY_SCORE_THRESHOLD:
            anomaly_details.append((score, fault_id, class_name))
            fault_scores[fault_id] = max(fault_scores.get(fault_id, 0.0), score)

    # ── Build top-N detected sounds (any class) ──
    top_n = 10
    top_indices = np.argsort(avg_scores)[::-1][:top_n]
    detected_sounds = []
    for idx in top_indices:
        name = _class_names[idx] if idx < len(_class_names) else f"Class {idx}"
        detected_sounds.append({
            "class": name,
            "index": int(idx),
            "score": round(float(avg_scores[idx]), 4),
        })

    # ── Determine if abnormal ──
    # Abnormal if any anomaly class scores significantly relative to normal fan
    total_anomaly_score = sum(fault_scores.values())
    is_abnormal = (
        total_anomaly_score > ANOMALY_SCORE_THRESHOLD * 2
        or (total_anomaly_score > ANOMALY_SCORE_THRESHOLD and normal_fan_score < 0.15)
    )

    # Normalize fault_scores to 0–1 range for integration with audio_analyzer
    max_anomaly = max(fault_scores.values()) if fault_scores else 0.0
    normalized_fault_scores = {}
    for fid, score in fault_scores.items():
        # Scale: a YAMNet score of 0.3+ is very strong evidence
        normalized_fault_scores[fid] = min(1.0, score / 0.30)

    # ── Confidence in abnormality ──
    if is_abnormal:
        abnormal_confidence = min(1.0, total_anomaly_score / 0.25)
    else:
        abnormal_confidence = 0.0

    # ── Build evidence string ──
    anomaly_details.sort(key=lambda x: x[0], reverse=True)
    if anomaly_details:
        top_anomalies = ", ".join(
            f"{name} ({score:.2f})" for score, _, name in anomaly_details[:5]
        )
        evidence = f"YAMNet detected: {top_anomalies}. Normal fan score: {normal_fan_score:.2f}."
    else:
        evidence = f"YAMNet: no anomaly classes detected. Normal fan score: {normal_fan_score:.2f}."

    if is_abnormal:
        evidence += " ⚠ Unusual noise pattern detected."

    return {
        "available": True,
        "is_abnormal": is_abnormal,
        "abnormal_confidence": round(abnormal_confidence, 3),
        "normal_fan_score": round(normal_fan_score, 4),
        "fault_scores": normalized_fault_scores,
        "detected_sounds": detected_sounds,
        "yamnet_evidence": evidence,
    }
