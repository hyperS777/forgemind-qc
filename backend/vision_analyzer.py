"""Vision analysis service for ForgeMind industrial fans.

Uses Google GenAI (Gemini) to inspect images uploaded from the phone
for physical defects like bent blades, dust buildup, or loose mounts.
"""

import os
import json
import logging
from pathlib import Path

from knowledge_base import get_kb_context_string

logger = logging.getLogger("forgemind.vision_analyzer")

VISION_SYSTEM_PROMPT = """You are ForgeMind Vision, an industrial fan visual inspector.
Analyze the provided image of an industrial fan. Look for physical defects such as:
- Bent, broken, or deformed fan blades
- Significant dust or debris buildup restricting airflow
- Loose, missing, or damaged mounting hardware
- Signs of physical wear or structural damage

Based on your findings, match to ONLY one of the known faults, "Normal Operation", or "Unknown visual anomaly".
Reply with ONLY valid JSON: {"vision_fault": "...", "confidence": 0, "severity": "Critical|High|Medium|Low", "summary": "..."}"""

def analyze_image(image_path: str | None) -> dict:
    """Analyze an image using Gemini Vision and return a structured assessment."""
    if not image_path or not Path(image_path).is_file():
        return {
            "available": False,
            "vision_fault": "No image",
            "summary": "No image provided for visual analysis."
        }

    api_key = os.getenv("GEMINI_API_KEY")
    if not api_key:
        logger.warning("GEMINI_API_KEY not set. Vision analysis disabled.")
        return {
            "available": False,
            "vision_fault": "AI Unavailable",
            "summary": "Vision AI key not configured."
        }

    try:
        from google import genai
        client = genai.Client(api_key=api_key)
        
        prompt = (
            f"{get_kb_context_string()}\n\n"
            "Inspect the attached image of the industrial fan and return the required JSON assessment."
        )
        
        # Upload the image
        logger.info(f"Uploading image for vision analysis: {image_path}")
        image_file = client.files.upload(file=image_path)
        
        # Generate content
        response = client.models.generate_content(
            model=os.getenv("GEMINI_VISION_MODEL", "gemini-2.5-flash"),
            contents=[prompt, image_file],
            config={"system_instruction": VISION_SYSTEM_PROMPT, "temperature": 0.1},
        )
        
        # Parse JSON output
        text = response.text.strip().removeprefix("```json").removeprefix("```").removesuffix("```").strip()
        result = json.loads(text)
        result["available"] = True
        return result

    except Exception as error:
        logger.error(f"Vision analysis failed: {error}")
        return {
            "available": False,
            "vision_fault": "Analysis Error",
            "summary": f"Failed to analyze image: {str(error)}"
        }
