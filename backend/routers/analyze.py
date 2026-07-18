from fastapi import APIRouter, UploadFile, File, Form
import shutil
from models.diagnosis import Diagnosis

router = APIRouter()


@router.post("/analyze", response_model=Diagnosis)
async def analyze(
    image: UploadFile | None = File(default=None),
    audio: UploadFile | None = File(default=None),
    temperature: float = Form(...),
    current: float = Form(...),
    rpm: int = Form(...),
    anomaly_score: float = Form(...)
):

    print("Image :", image.filename if image else "None")
    print("Audio :", audio.filename if audio else "None")

    print(
        temperature,
        current,
        rpm,
        anomaly_score
    )

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

    return Diagnosis(
        fault="Bent Cooling Fan Blade",
        confidence=96,
        severity="High",
        summary="Abnormal vibration caused by a bent cooling fan blade.",
        recommendation="Replace the damaged blade and inspect the fan assembly."
    )