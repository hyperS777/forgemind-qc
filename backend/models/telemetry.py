from pydantic import BaseModel

class Telemetry(BaseModel):
    temperature: float
    current: float
    rpm: int
    anomaly_score: float