import time
import uuid

# Global state for latest upload
latest_payload = {
    "image": None,
    "audio": None,
    # Unix timestamp
    "timestamp": 0,
    # anomaly score from telemetry (0.0 - 10.0)
    "anomaly_score": 0.0,
    # optional machine identifier
    "machine_id": None,
    # telemetry snapshot sent alongside the payload
    "telemetry": {
        "temperature": 0.0,
        "current": 0.0,
        "rpm": 0,
        "anomaly_score": 0.0,
    },
}

# Notification queue — list of dicts, newest first.
# Each notification: { id, title, message, severity, timestamp, telemetry, diagnosis }
notification_queue: list[dict] = []

MAX_NOTIFICATIONS = 50


def push_notification(
    title: str,
    message: str,
    severity: str = "Info",
    telemetry: dict | None = None,
    diagnosis: dict | None = None,
):
    """Add a notification event to the queue."""
    event = {
        "id": str(uuid.uuid4()),
        "title": title,
        "message": message,
        "severity": severity,
        "timestamp": time.time(),
        "telemetry": telemetry,
        "diagnosis": diagnosis,
    }
    notification_queue.insert(0, event)
    # Trim old notifications
    while len(notification_queue) > MAX_NOTIFICATIONS:
        notification_queue.pop()
    return event


def acknowledge_notification(notification_id: str) -> bool:
    """Remove a notification by ID. Returns True if found and removed."""
    for i, n in enumerate(notification_queue):
        if n["id"] == notification_id:
            notification_queue.pop(i)
            return True
    return False


def clear_all_notifications():
    """Remove all notifications."""
    notification_queue.clear()
