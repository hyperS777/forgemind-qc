import serial
import csv

PORT = "COM5"
BAUD = 9600

ser = serial.Serial(PORT, BAUD)

csv_file = open("healthy.csv", "w", newline="")

writer = csv.writer(csv_file)

writer.writerow([
    "temperature",
    "humidity",
    "ax",
    "ay",
    "az",
    "delta",
    "tempFault",
    "vibFault",
    "status"
])

print("Collecting data...")
print("Press CTRL+C when done.")

try:

    while True:

        line = ser.readline().decode().strip()

        if (
            line.startswith("READY")
            or line.startswith("temp,humidity")
            or line == ""
        ):
            continue

        values = line.split(",")

        if len(values) == 9:

            writer.writerow(values)

            print(values)

except KeyboardInterrupt:

    csv_file.close()

    print("\nSaved healthy.csv")