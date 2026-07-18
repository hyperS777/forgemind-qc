from fastapi import APIRouter, UploadFile, File, Form, Depends
import shutil
from sqlalchemy.orm import Session
from models.diagnosis import Diagnosis
from database import get_db, AnomalyEvent
from audio_analyzer import run_audio_analysis
import state
import time
import logging

router = APIRouter()
logger = logging.getLogger("forgemind.analyze")


@router.post("/analyze", response_model=Diagnosis)
async def analyze(
    image: UploadFile | None = File(default=None),
    audio: UploadFile | None = File(default=None),
    temperature: float = Form(...),
    current: float = Form(...),
    rpm: int = Form(...),
    anomaly_score: float = Form(...),
    machine_id: str = Form(default="FAN-01"),
    db: Session = Depends(get_db)
):

    print("Image :", image.filename if image else "None")
    print("Audio :", audio.filename if audio else "None")

    print(
        temperature,
        current,
        rpm,
        anomaly_score
    )

    image_path = None
    audio_path = None

    # Save image
    if image:
        image_path = f"uploads/images/{image.filename}"

        with open(image_path, "wb") as buffer:
            shutil.copyfileobj(image.file, buffer)

    # Save audio
    if audio:
        audio_path = f"uploads/audio/{audio.filename}"

        with open(audio_path, "wb") as buffer:
            shutil.copyfileobj(audio.file, buffer)

    # Build telemetry snapshot
    telemetry_snapshot = {
        "temperature": float(temperature),
        "current": float(current),
        "rpm": int(rpm),
        "anomaly_score": float(anomaly_score),
    }

    # Update latest payload state so phone can react to new packages
    state.latest_payload["image"] = image_path
    state.latest_payload["audio"] = audio_path
    state.latest_payload["timestamp"] = time.time()
    state.latest_payload["anomaly_score"] = float(anomaly_score)
    state.latest_payload["machine_id"] = machine_id
    state.latest_payload["telemetry"] = telemetry_snapshot

    # ═══════ RUN TWO-TIER AUDIO ANALYSIS ═══════
    logger.info(f"Running audio analysis for {machine_id}")
    analysis = run_audio_analysis(
        telemetry=telemetry_snapshot,
        audio_path=audio_path,
    )
    logger.info(f"Analysis result: {analysis}")

    diag = Diagnosis(
        fault=analysis.get("fault", "Unknown Fault"),
        confidence=analysis.get("confidence", 50),
        severity=analysis.get("severity", "Medium"),
        summary=analysis.get("summary", "Analysis complete."),
        recommendation=analysis.get("recommendation", "Inspect the equipment."),
    )

    # Save to history DB
    new_event = AnomalyEvent(
        temperature=temperature,
        current=current,
        rpm=rpm,
        anomaly_score=anomaly_score,
        vision_summary="Vision Payload Ready" if image_path else "No image",
        audio_summary=f"[{analysis.get('source', 'unknown')}] {analysis.get('fault', 'N/A')}",
        fault=diag.fault,
        severity=diag.severity,
        confidence=diag.confidence,
        recommendation=diag.recommendation
    )
    db.add(new_event)
    db.commit()
    db.refresh(new_event)

    # Auto-push a notification event so the phone picks it up
    severity = "High" if anomaly_score > 5 else ("Warning" if anomaly_score > 3 else "Info")
    state.push_notification(
        title=f"Anomaly Payload from {machine_id}",
        message=f"Score {anomaly_score:.1f} — {diag.fault}. Temp {temperature}°C, {current}A, {rpm} RPM.",
        severity=severity,
        telemetry=telemetry_snapshot,
        diagnosis={
            "fault": diag.fault,
            "confidence": diag.confidence,
            "severity": diag.severity,
            "summary": diag.summary,
            "recommendation": diag.recommendation,
        },
    )

    return diag