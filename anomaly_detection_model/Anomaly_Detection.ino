#include <Wire.h>
#include <Adafruit_Sensor.h>
#include <Adafruit_ADXL345_U.h>
#include <DHT.h>

#define DHTPIN 2
#define DHTTYPE DHT11

#define GREEN_LED 6
#define RED_LED 7
#define RELAY 8
#define BUZZER 9

DHT dht(DHTPIN, DHTTYPE);
Adafruit_ADXL345_Unified accel = Adafruit_ADXL345_Unified(12345);

// ---------- Safety Thresholds ----------
const float VIBRATION_THRESHOLD = 2.5;
const float TEMPERATURE_THRESHOLD = 40.0;

// Previous acceleration values
float lastX = 0;
float lastY = 0;
float lastZ = 0;

bool firstReading = true;

void setup()
{
    Serial.begin(9600);

    pinMode(GREEN_LED, OUTPUT);
    pinMode(RED_LED, OUTPUT);
    pinMode(RELAY, OUTPUT);
    pinMode(BUZZER, OUTPUT);

    digitalWrite(GREEN_LED, HIGH);
    digitalWrite(RED_LED, LOW);
    digitalWrite(BUZZER, LOW);

    dht.begin();

    if (!accel.begin())
    {
        Serial.println("ERROR,ADXL345_NOT_FOUND");
        while (1);
    }

    accel.setRange(ADXL345_RANGE_16_G);

    // Relay ON (change if your relay logic is reversed)
    digitalWrite(RELAY, HIGH);

    Serial.println("READY");
    Serial.println("temp,humidity,ax,ay,az,delta,tempFault,vibFault,status");
}

void loop()
{
    sensors_event_t event;
    accel.getEvent(&event);

    float temperature = dht.readTemperature();
    float humidity = dht.readHumidity();

    if (isnan(temperature) || isnan(humidity))
    {
        delay(500);
        return;
    }

    float delta = 0;

    if (!firstReading)
    {
        delta =
            abs(event.acceleration.x - lastX) +
            abs(event.acceleration.y - lastY) +
            abs(event.acceleration.z - lastZ);
    }

    firstReading = false;

    lastX = event.acceleration.x;
    lastY = event.acceleration.y;
    lastZ = event.acceleration.z;

    bool vibrationFault = (delta > VIBRATION_THRESHOLD);
    bool tempFault = (temperature > TEMPERATURE_THRESHOLD);

    bool anomaly = vibrationFault || tempFault;

    // ------------------------------------
    // Immediate Hardware Safety Response
    // ------------------------------------

    if (anomaly)
    {
        digitalWrite(GREEN_LED, LOW);
        digitalWrite(RED_LED, HIGH);
        digitalWrite(BUZZER, HIGH);

        // Stop machine
        digitalWrite(RELAY, LOW);
    }
    else
    {
        digitalWrite(GREEN_LED, HIGH);
        digitalWrite(RED_LED, LOW);
        digitalWrite(BUZZER, LOW);

        // Machine running
        digitalWrite(RELAY, HIGH);
    }

    // ------------------------------------
    // CSV Serial Output
    // ------------------------------------

    Serial.print(temperature, 2);
    Serial.print(",");

    Serial.print(humidity, 2);
    Serial.print(",");

    Serial.print(event.acceleration.x, 3);
    Serial.print(",");

    Serial.print(event.acceleration.y, 3);
    Serial.print(",");

    Serial.print(event.acceleration.z, 3);
    Serial.print(",");

    Serial.print(delta, 3);
    Serial.print(",");

    Serial.print(tempFault ? 1 : 0);
    Serial.print(",");

    Serial.print(vibrationFault ? 1 : 0);
    Serial.print(",");

    Serial.println(anomaly ? 1 : 0);

    delay(500);
}