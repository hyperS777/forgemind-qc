# forgemind-qc
ForgeMind Qualcomm Hackathon 18-19 July 2026

## Sound anomaly detection

`POST /analyze` now measures uploaded fan recordings locally and matches the
acoustic signal with the listed faults: bent blade, dust buildup, worn bearing,
loose mounting, and motor overload. It combines that result with temperature,
current, RPM, and anomaly score.

From `backend`, install dependencies with `pip install -r requirements.txt`.
The app records M4A on Android; `imageio-ffmpeg` converts it for local feature
analysis. PCM WAV is also accepted directly.

- `SARVAM_API_KEY` is optional and transcribes a spoken technician observation.
- `GEMINI_API_KEY` is optional. It is invoked only when the local acoustic and
  telemetry match is below 68%, and is restricted to the same known-fault list.
