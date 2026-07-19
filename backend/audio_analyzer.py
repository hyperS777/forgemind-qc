"""Audio-first anomaly detection for ForgeMind industrial fans.

The first tier is deliberately local and explainable: it extracts acoustic
features from PCM WAV recordings, matches them against the fault list in
``knowledge_base.py``, then combines that evidence with telemetry.  Sarvam is
used only for an optional spoken technician note; speech-to-text is not a
reliable way to classify the sound of a bearing or fan.  If the local evidence
is inconclusive, Gemini can inspect the original recording as a second opinion.
"""

from __future__ import annotations

import json
import logging
import math
import os
import subprocess
import tempfile
import wave
from pathlib import Path

from knowledge_base import FAULT_PROFILES, HEALTHY_PROFILE, get_kb_context_string
from yamnet_classifier import classify_fan_audio as _yamnet_classify

logger = logging.getLogger("forgemind.audio_analyzer")

# A lower confidence is intentionally escalated to the multimodal model.  The
# local detector is useful offline, but it must not pretend to know a fault it
# cannot distinguish from a short/noisy phone recording.
KB_CONFIDENCE_THRESHOLD = 68
MIN_AUDIO_SECONDS = 0.35
MAX_ANALYSIS_SECONDS = 12


def _clamp(value: float, low: float = 0.0, high: float = 1.0) -> float:
    return max(low, min(high, value))


def _read_pcm_wav(audio_path: str, allow_transcode: bool = True) -> tuple[list[float], int] | None:
    """Read PCM WAV, transcoding phone formats through the bundled decoder."""
    try:
        with wave.open(audio_path, "rb") as wav:
            channels = wav.getnchannels()
            sample_width = wav.getsampwidth()
            sample_rate = wav.getframerate()
            frames = min(wav.getnframes(), sample_rate * MAX_ANALYSIS_SECONDS)
            raw = wav.readframes(frames)

        if channels < 1 or sample_rate < 1 or sample_width not in (1, 2, 3, 4):
            return None

        frame_width = channels * sample_width
        if not raw or len(raw) < frame_width:
            return None
        full_scale = 128.0 if sample_width == 1 else float(1 << (sample_width * 8 - 1))
        samples = []
        for frame_start in range(0, len(raw) - frame_width + 1, frame_width):
            channel_values = []
            for channel in range(channels):
                start = frame_start + channel * sample_width
                chunk = raw[start : start + sample_width]
                # PCM WAV stores 8-bit values as unsigned, all wider formats
                # as little-endian signed integers.
                value = chunk[0] - 128 if sample_width == 1 else int.from_bytes(chunk, "little", signed=True)
                channel_values.append(value / full_scale)
            samples.append(sum(channel_values) / channels)
        return samples, sample_rate
    except (wave.Error, EOFError, OSError):
        if not allow_transcode:
            return None
        try:
            # imageio-ffmpeg ships an ffmpeg executable on supported desktop
            # platforms, so the Android AAC/M4A recorder works without asking
            # the deployment machine to install a separate system package.
            import imageio_ffmpeg

            with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as output:
                decoded_path = output.name
            command = [
                imageio_ffmpeg.get_ffmpeg_exe(), "-y", "-v", "error", "-i", audio_path,
                "-t", str(MAX_ANALYSIS_SECONDS), "-ac", "1", "-ar", "16000", decoded_path,
            ]
            subprocess.run(command, check=True, capture_output=True, timeout=30)
            return _read_pcm_wav(decoded_path, allow_transcode=False)
        except (ImportError, OSError, subprocess.SubprocessError):
            return None
        finally:
            if "decoded_path" in locals():
                Path(decoded_path).unlink(missing_ok=True)


def _band_energy(samples: list[float], sample_rate: int, frequencies: tuple[int, ...]) -> float:
    """Small Goertzel bank used instead of a heavyweight ML/DSP dependency."""
    if not samples:
        return 0.0
    n = min(len(samples), 4096)
    if n < 64:
        return 0.0
    # One representative window is enough for the broad diagnostic bands below.
    offset = max(0, (len(samples) - n) // 2)
    window = samples[offset : offset + n]
    total = 0.0
    for frequency in frequencies:
        if frequency >= sample_rate / 2:
            continue
        omega = 2.0 * math.pi * frequency / sample_rate
        coefficient = 2.0 * math.cos(omega)
        q0 = q1 = q2 = 0.0
        for sample in window:
            q0 = coefficient * q1 - q2 + sample
            q2, q1 = q1, q0
        total += max(0.0, q1 * q1 + q2 * q2 - coefficient * q1 * q2)
    return total / n


def _envelope_periodicity(frame_rms: list[float]) -> float:
    """Return the strongest repeating envelope pattern at practical fan rates."""
    if len(frame_rms) < 12:
        return 0.0
    average = sum(frame_rms) / len(frame_rms)
    centered = [value - average for value in frame_rms]
    energy = sum(value * value for value in centered)
    if energy <= 1e-12:
        return 0.0
    # Compare repeating patterns from roughly 0.1 to 2 seconds.
    maximum = 0.0
    for lag in range(4, min(len(centered) // 2, 80)):
        numerator = sum(centered[i] * centered[i - lag] for i in range(lag, len(centered)))
        maximum = max(maximum, numerator / energy)
    return _clamp(maximum)


def extract_audio_features(audio_path: str | None) -> dict:
    """Extract explainable acoustic indicators from a WAV recording."""
    if not audio_path or not Path(audio_path).is_file():
        return {"available": False, "reason": "no_audio"}

    decoded = _read_pcm_wav(audio_path)
    if not decoded:
        suffix = Path(audio_path).suffix.lower() or "unknown"
        return {
            "available": False,
            "reason": f"unsupported_{suffix}_audio",
            "hint": "Install backend dependencies to decode this audio locally; AI fallback can still inspect the original recording.",
        }

    samples, sample_rate = decoded
    duration = len(samples) / sample_rate
    if duration < MIN_AUDIO_SECONDS:
        return {"available": False, "reason": "recording_too_short", "duration_s": round(duration, 2)}

    frame_size = max(256, int(sample_rate * 0.10))
    rms_frames: list[float] = []
    peaks: list[float] = []
    zero_crossings = 0
    previous = samples[0]
    for start in range(0, len(samples) - frame_size + 1, frame_size):
        frame = samples[start : start + frame_size]
        rms_frames.append(math.sqrt(sum(value * value for value in frame) / len(frame)))
        peaks.append(max(abs(value) for value in frame))
        for value in frame:
            if (value >= 0) != (previous >= 0):
                zero_crossings += 1
            previous = value

    rms = math.sqrt(sum(value * value for value in samples) / len(samples))
    peak = max(abs(value) for value in samples)
    mean_frame_rms = sum(rms_frames) / len(rms_frames)
    frame_deviation = math.sqrt(
        sum((value - mean_frame_rms) ** 2 for value in rms_frames) / len(rms_frames)
    )
    dynamics = frame_deviation / max(mean_frame_rms, 1e-9)
    crest_factor = peak / max(rms, 1e-9)
    # The individual frequency bins make the score robust to a slightly
    # different motor speed, unlike assuming one exact bearing frequency.
    low = _band_energy(samples, sample_rate, (50, 60, 100, 120, 180, 240))
    mid = _band_energy(samples, sample_rate, (400, 700, 1000, 1500))
    high = _band_energy(samples, sample_rate, (2000, 3000, 4000, 6000))
    total_band = low + mid + high + 1e-12

    return {
        "available": True,
        "duration_s": round(duration, 2),
        "sample_rate_hz": sample_rate,
        "rms_dbfs": round(20 * math.log10(max(rms, 1e-9)), 1),
        "crest_factor": round(crest_factor, 2),
        "dynamic_variation": round(dynamics, 3),
        "periodicity": round(_envelope_periodicity(rms_frames), 3),
        "zero_crossing_rate": round(zero_crossings / len(samples), 4),
        "low_band_ratio": round(low / total_band, 3),
        "mid_band_ratio": round(mid / total_band, 3),
        "high_band_ratio": round(high / total_band, 3),
    }


def _audio_fault_scores(features: dict) -> dict[str, float]:
    """Score only the named faults in the knowledge base from sound features."""
    if not features.get("available"):
        return {}

    periodic = features["periodicity"]
    impulses = _clamp((features["crest_factor"] - 2.2) / 5.0)
    dynamics = _clamp(features["dynamic_variation"] / 0.65)
    high = _clamp((features["high_band_ratio"] - 0.14) / 0.45)
    low = features["low_band_ratio"]
    hum = _clamp((low - 0.30) / 0.40)
    roughness = _clamp((features["zero_crossing_rate"] - 0.03) / 0.20)

    return {
        "bent_blade": _clamp(0.45 * periodic + 0.35 * impulses + 0.20 * dynamics),
        "dust_buildup": _clamp(0.50 * low + 0.25 * (1 - high) + 0.25 * (1 - dynamics)),
        "worn_bearing": _clamp(0.50 * high + 0.30 * roughness + 0.20 * impulses),
        "loose_mount": _clamp(0.45 * periodic + 0.35 * dynamics + 0.20 * impulses),
        "motor_overload": _clamp(0.55 * hum + 0.25 * (1 - dynamics) + 0.20 * low),
    }


def _telemetry_score(telemetry: dict, fault_profile: dict) -> float:
    ranges = fault_profile["telemetry"]
    aliases = {
        "temp_c": ("temperature", "temp_c"),
        "current_a": ("current", "current_a"),
        "rpm": ("rpm",),
        "anomaly_score": ("anomaly_score",),
    }
    scores = []
    for key, names in aliases.items():
        value = next((telemetry[name] for name in names if telemetry.get(name) is not None), None)
        if value is None:
            continue
        low, high = ranges[key]
        midpoint, half_span = (low + high) / 2, max((high - low) / 2, 1.0)
        scores.append(_clamp(1.0 - abs(float(value) - midpoint) / (half_span * 3)))
    return sum(scores) / len(scores) if scores else 0.0


def _audio_keyword_score(transcript: str | None, fault_profile: dict) -> float:
    if not transcript:
        return 0.0
    keywords = fault_profile.get("audio_signatures", [])
    hits = sum(keyword.lower() in transcript.lower() for keyword in keywords)
    return _clamp(hits / max(1, len(keywords) * 0.30))


def _transcribe_audio_sarvam(audio_path: str) -> str | None:
    """Optional Sarvam transcription of a spoken technician observation."""
    api_key = os.getenv("SARVAM_API_KEY")
    if not api_key:
        return None
    try:
        from sarvamai import SarvamAI

        client = SarvamAI(api_subscription_key=api_key)
        with open(audio_path, "rb") as recording:
            response = client.speech_to_text.transcribe(
                file=recording, model="saaras:v3", mode="transcribe"
            )
        return getattr(response, "transcript", None)
    except Exception as error:
        logger.warning("Sarvam STT unavailable: %s", error)
        return None


def _audio_evidence(features: dict, fault_id: str) -> str:
    if not features.get("available"):
        return "No locally decodable acoustic signal."
    return (
        f"{fault_id} acoustic score derived from {features['duration_s']}s recording; "
        f"periodicity {features['periodicity']}, crest {features['crest_factor']}, "
        f"high-band energy {features['high_band_ratio']}."
    )


def analyze_with_kb(telemetry: dict, audio_path: str | None = None) -> dict:
    features = extract_audio_features(audio_path)
    transcript = _transcribe_audio_sarvam(audio_path) if audio_path else None
    acoustic_scores = _audio_fault_scores(features)

    # ── YAMNet classification (complementary ML-based signal) ──
    yamnet_result = _yamnet_classify(audio_path)
    yamnet_available = yamnet_result.get("available", False)
    yamnet_fault_scores = yamnet_result.get("fault_scores", {})
    logger.info("YAMNet result: available=%s, abnormal=%s, evidence=%s",
                yamnet_available, yamnet_result.get("is_abnormal"),
                yamnet_result.get("yamnet_evidence", "N/A"))

    scored_faults = []
    for fault in FAULT_PROFILES:
        telemetry_score = _telemetry_score(telemetry, fault)
        acoustic_score = acoustic_scores.get(fault["id"], 0.0)
        yamnet_score = yamnet_fault_scores.get(fault["id"], 0.0)
        note_score = _audio_keyword_score(transcript, fault)

        # Three-tier weighting: telemetry + spectral + YAMNet + transcript.
        # Weights adapt based on which signals are actually available.
        if features.get("available") and yamnet_available:
            combined = (0.45 * telemetry_score + 0.30 * acoustic_score
                        + 0.20 * yamnet_score + 0.05 * note_score)
        elif features.get("available"):
            combined = 0.58 * telemetry_score + 0.37 * acoustic_score + 0.05 * note_score
        elif yamnet_available:
            combined = 0.55 * telemetry_score + 0.40 * yamnet_score + 0.05 * note_score
        else:
            combined = 0.90 * telemetry_score + 0.10 * note_score
        scored_faults.append((combined, fault, telemetry_score, acoustic_score, yamnet_score))

    best_score, best_fault, telemetry_score, acoustic_score, yamnet_score = max(
        scored_faults, key=lambda item: item[0]
    )
    healthy_score = _telemetry_score(telemetry, HEALTHY_PROFILE)

    # Check if YAMNet flagged abnormality — this can override a "healthy" telemetry reading
    yamnet_says_abnormal = yamnet_available and yamnet_result.get("is_abnormal", False)
    max_acoustic = max(acoustic_scores.values(), default=0)
    max_yamnet = max(yamnet_fault_scores.values(), default=0)

    # A normal telemetry baseline needs sound evidence before being overruled.
    if (healthy_score >= 0.82
            and (not features.get("available") or max_acoustic < 0.58)
            and (not yamnet_says_abnormal or max_yamnet < 0.50)):
        return {
            "fault_id": "healthy", "title": "Normal Operation", "confidence": int(healthy_score * 100),
            "severity": "Low", "root_cause": "Telemetry is within the normal operating baseline.",
            "recommendation": "No action required. Continue routine monitoring.", "source": "knowledge_base",
            "transcript": transcript, "audio_features": features,
            "yamnet_result": yamnet_result,
        }

    return {
        "fault_id": best_fault["id"], "title": best_fault["title"],
        "confidence": int(best_score * 100), "severity": best_fault["severity"],
        "root_cause": best_fault["root_cause"], "recommendation": best_fault["recommendation"],
        "repair_steps": best_fault.get("repair_steps", []), "source": "acoustic_knowledge_base",
        "transcript": transcript, "audio_features": features,
        "audio_evidence": _audio_evidence(features, best_fault["id"]),
        "telemetry_match": round(telemetry_score, 2), "acoustic_match": round(acoustic_score, 2),
        "yamnet_match": round(yamnet_score, 2),
        "yamnet_result": yamnet_result,
    }


GEMINI_SYSTEM_PROMPT = """You are ForgeMind, an industrial fan acoustics expert.
Diagnose ONLY one of the listed known faults, "Normal Operation", or "Unknown acoustic anomaly".
Do not invent a fault outside that list. Treat local acoustic features as evidence, not certainty.
Reply with only valid JSON: {"fault":"...","confidence":0,"severity":"Critical|High|Medium|Low","summary":"...","recommendation":"..."}."""


def analyze_with_gemini(telemetry: dict, audio_path: str | None, kb_result: dict) -> dict | None:
    api_key = os.getenv("GEMINI_API_KEY")
    if not api_key:
        return None
    try:
        from google import genai

        feature_text = json.dumps(kb_result.get("audio_features", {}), indent=2)
        yamnet_text = ""
        yamnet_r = kb_result.get("yamnet_result", {})
        if yamnet_r.get("available"):
            yamnet_text = (
                f"\nYAMNet classification: {yamnet_r.get('yamnet_evidence', 'N/A')}\n"
                f"YAMNet detected sounds: {json.dumps(yamnet_r.get('detected_sounds', [])[:5])}\n"
            )
        prompt = (
            f"{get_kb_context_string()}\n\nTelemetry: {json.dumps(telemetry)}\n"
            f"Local acoustic features: {feature_text}\n{yamnet_text}"
            f"Local tentative result: {kb_result['title']} at {kb_result['confidence']}%.\n"
            "Assess the attached recording (if provided) and return the required JSON."
        )
        client = genai.Client(api_key=api_key)
        contents = [prompt]
        if audio_path and Path(audio_path).is_file():
            contents.append(client.files.upload(file=audio_path))
        response = client.models.generate_content(
            model=os.getenv("GEMINI_AUDIO_MODEL", "gemini-2.5-flash"), contents=contents,
            config={"system_instruction": GEMINI_SYSTEM_PROMPT, "temperature": 0.1},
        )
        text = response.text.strip().removeprefix("```json").removeprefix("```").removesuffix("```").strip()
        result = json.loads(text)
        result["source"] = "gemini_audio_fallback"
        return result
    except Exception as error:
        logger.warning("Gemini audio fallback unavailable: %s", error)
        return None


def run_audio_analysis(telemetry: dict, audio_path: str | None = None) -> dict:
    """Return a diagnosis from local acoustic/telemetry matching or Gemini."""
    kb_result = analyze_with_kb(telemetry, audio_path)
    logger.info("Local analysis: %s at %s%%", kb_result["title"], kb_result["confidence"])

    # Extract YAMNet evidence for the summary
    yamnet_r = kb_result.get("yamnet_result", {})
    yamnet_evidence = yamnet_r.get("yamnet_evidence", "")
    yamnet_suffix = f" | {yamnet_evidence}" if yamnet_evidence else ""

    if kb_result["confidence"] >= KB_CONFIDENCE_THRESHOLD:
        return {
            "fault": kb_result["title"], "confidence": kb_result["confidence"],
            "severity": kb_result["severity"],
            "summary": f"[Acoustic + YAMNet + telemetry match] {kb_result['root_cause']} {kb_result.get('audio_evidence', '')}{yamnet_suffix}",
            "recommendation": kb_result["recommendation"], "source": kb_result["source"],
        }

    ai_result = analyze_with_gemini(telemetry, audio_path, kb_result)
    if ai_result:
        return {
            "fault": ai_result.get("fault", "Unknown acoustic anomaly"),
            "confidence": int(ai_result.get("confidence", 60)),
            "severity": ai_result.get("severity", "Medium"),
            "summary": f"[AI second opinion] {ai_result.get('summary', 'Inconclusive local result reviewed by AI.')}{yamnet_suffix}",
            "recommendation": ai_result.get("recommendation", "Inspect the equipment before continued operation."),
            "source": ai_result["source"],
        }

    format_hint = kb_result.get("audio_features", {}).get("hint")
    suffix = f" {format_hint}" if format_hint else ""
    return {
        "fault": kb_result["title"], "confidence": kb_result["confidence"],
        "severity": kb_result["severity"],
        "summary": f"[Low-confidence acoustic match] {kb_result['root_cause']}.{suffix}{yamnet_suffix}",
        "recommendation": kb_result["recommendation"], "source": "local_acoustic_fallback",
    }
