import pandas as pd
from sklearn.ensemble import IsolationForest
import joblib

df = pd.read_csv("healthy.csv")

X = df[
    [
        "temperature",
        "humidity",
        "ax",
        "ay",
        "az",
        "delta"
    ]
]

model = IsolationForest(
    contamination=0.03,
    random_state=42
)

model.fit(X)

joblib.dump(
    model,
    "forgemind_model.pkl"
)

print("Training Complete")