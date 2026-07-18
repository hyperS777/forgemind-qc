from fastapi import FastAPI, Depends
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from routers.analyze import router as analyze_router
from sqlalchemy.orm import Session
from database import get_db, AnomalyEvent
from typing import List, Optional
from pydantic import BaseModel
import time
import os

from state import (
    latest_payload,
    notification_queue,
    push_notification,
    acknowledge_notification,
    clear_all_notifications,
)


def get_bind_host() -> str:
    return os.getenv("FORGEMIND_HOST", "0.0.0.0")


def get_bind_port() -> int:
    return int(os.getenv("FORGEMIND_PORT", "8000"))


app = FastAPI(
    title="ForgeMind Backend"
)

# Allow requests from any origin (Cloudflare Pages, phone, etc.)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Ensure upload directories exist
os.makedirs("uploads/images", exist_ok=True)
os.makedirs("uploads/audio", exist_ok=True)

# Mount static files
app.mount("/uploads", StaticFiles(directory="uploads"), name="uploads")

app.include_router(analyze_router)


@app.get("/")
def root():
    return {
        "message": "ForgeMind Backend Running"
    }


@app.get("/health")
def health():
    return {
        "status": "healthy"
    }


@app.get("/history")
def get_history(db: Session = Depends(get_db)):
    events = db.query(AnomalyEvent).order_by(AnomalyEvent.id.desc()).all()
    return events


@app.get("/latest-payload")
def get_latest_payload():
    return latest_payload


@app.post("/acknowledge-payload")
def acknowledge_payload():
    # Clear the latest payload so clients won't repeatedly alert for the same event
    latest_payload["image"] = None
    latest_payload["audio"] = None
    latest_payload["timestamp"] = 0
    latest_payload["anomaly_score"] = 0.0
    latest_payload["machine_id"] = None
    latest_payload["telemetry"] = {
        "temperature": 0.0,
        "current": 0.0,
        "rpm": 0,
        "anomaly_score": 0.0,
    }
    return {"ok": True}


# ═══════════ NOTIFICATION ENDPOINTS ═══════════


@app.get("/notifications")
def get_notifications():
    """Return all pending notification events (newest first)."""
    return {
        "notifications": notification_queue,
        "count": len(notification_queue),
    }


class NotifyRequest(BaseModel):
    title: str
    message: str
    severity: str = "Info"
    temperature: Optional[float] = None
    current: Optional[float] = None
    rpm: Optional[int] = None
    anomaly_score: Optional[float] = None
    machine_id: Optional[str] = None


@app.post("/notify")
def notify_phone(req: NotifyRequest):
    """Push a notification event that the phone (or any client) can poll for."""
    telemetry = None
    if req.temperature is not None or req.anomaly_score is not None:
        telemetry = {
            "temperature": req.temperature or 0.0,
            "current": req.current or 0.0,
            "rpm": req.rpm or 0,
            "anomaly_score": req.anomaly_score or 0.0,
            "machine_id": req.machine_id or "FAN-01",
        }
    event = push_notification(
        title=req.title,
        message=req.message,
        severity=req.severity,
        telemetry=telemetry,
    )
    return {"ok": True, "notification": event}


class AcknowledgeRequest(BaseModel):
    notification_id: str


@app.post("/notifications/acknowledge")
def ack_notification(req: AcknowledgeRequest):
    """Remove a specific notification by ID."""
    found = acknowledge_notification(req.notification_id)
    return {"ok": found}


@app.post("/notifications/clear")
def clear_notifications():
    """Remove all notifications."""
    clear_all_notifications()
    return {"ok": True}


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("main:app", host=get_bind_host(), port=get_bind_port(), reload=False)