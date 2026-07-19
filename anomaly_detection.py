import serial
import joblib
import numpy as np

PORT = "COM5"
BAUD = 9600

model = joblib.load(
    "forgemind_model.pkl"
)

ser = serial.Serial(
    PORT,
    BAUD
)

print("Monitoring...")

while True:

    try:

        line = ser.readline().decode().strip()

        if (
            line.startswith("READY")
            or line.startswith("temp,humidity")
            or line == ""
        ):
            continue

        values = line.split(",")

        if len(values) != 9:
            continue

        features = np.array([
            [
                float(values[0]),
                float(values[1]),
                float(values[2]),
                float(values[3]),
                float(values[4]),
                float(values[5])
            ]
        ])

        prediction = model.predict(features)

        if prediction[0] == -1:

            print("\nANOMALY DETECTED")
            print(values)

            # call LLM here

        else:

            print("Healthy")

    except Exception as e:

        print(e)