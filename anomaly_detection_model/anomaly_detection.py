import serial
import pandas as pd
import joblib
from datetime import datetime

# ==========================
# CONFIG
# ==========================

PORT = "COM5"
BAUD = 115200

# ==========================
# LOAD MODEL
# ==========================

print("Loading model...")

model = joblib.load("models/anomaly_model.pkl")

print("Model loaded.")

# ==========================
# SERIAL CONNECTION
# ==========================

print(f"Connecting to {PORT}...")

ser = serial.Serial(PORT, BAUD, timeout=1)

print("Connected.")
print("Listening for sensor data...\n")

# ==========================
# DUMMY LLM FUNCTION
# Replace later with your
# friend's actual LLM code
# ==========================

def send_to_llm(sensor_packet):

    print("\n" + "=" * 60)
    print("🚨 ANOMALY DETECTED")
    print("=" * 60)

    print("Sending RAW sensor packet to LLM:\n")

    for key, value in sensor_packet.items():
        print(f"{key}: {value}")

    print("\nLLM processing would happen here...")
    print("=" * 60 + "\n")


# ==========================
# MAIN LOOP
# ==========================

while True:

    try:

        line = ser.readline().decode(
            "utf-8",
            errors="ignore"
        ).strip()

        if not line:
            continue

        values = line.split(",")

        if len(values) != 5:
            continue

        temperature = float(values[0])
        humidity = float(values[1])
        accel_x = float(values[2])
        accel_y = float(values[3])
        accel_z = float(values[4])

        sample = pd.DataFrame(
            [[
                temperature,
                humidity,
                accel_x,
                accel_y,
                accel_z
            ]],
            columns=[
                "temperature",
                "humidity",
                "accel_x",
                "accel_y",
                "accel_z"
            ]
        )

        prediction = model.predict(sample)[0]
        score = model.decision_function(sample)[0]

        timestamp = datetime.now().strftime(
            "%Y-%m-%d %H:%M:%S"
        )

        sensor_packet = {
            "timestamp": timestamp,
            "temperature": temperature,
            "humidity": humidity,
            "accel_x": accel_x,
            "accel_y": accel_y,
            "accel_z": accel_z,
            "anomaly_score": round(float(score), 6)
        }

        if prediction == -1:

            print(
                f"🚨 ANOMALY | Score: {score:.6f}"
            )

            send_to_llm(sensor_packet)

        else:

            print(
                f"✅ HEALTHY | Score: {score:.6f}"
            )

    except KeyboardInterrupt:

        print("\nStopping...")
        ser.close()
        break

    except Exception as e:

        print("Error:", e)