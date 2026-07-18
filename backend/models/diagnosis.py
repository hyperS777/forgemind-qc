from pydantic import BaseModel

class Diagnosis(BaseModel):
    fault: str
    confidence: int
    severity: str
    summary: str
    recommendation: str