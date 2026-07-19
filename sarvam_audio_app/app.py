import os
import tempfile
import csv
import numpy as np
import librosa
import ai_edge_litert.interpreter as tflite
from flask import Flask, render_template, request, jsonify
from flask_cors import CORS

app = Flask(__name__)
CORS(app)  # Enable CORS for all routes

# Load YAMNet class map
def load_class_map(class_map_csv="yamnet_class_map.csv"):
    class_names = []
    if os.path.exists(class_map_csv):
        with open(class_map_csv, 'r') as csvfile:
            reader = csv.DictReader(csvfile)
            for row in reader:
                class_names.append(row['display_name'])
    return class_names

class_names = load_class_map()

# Load TFLite Model
interpreter = None
if os.path.exists("yamnet.tflite"):
    interpreter = tflite.Interpreter(model_path="yamnet.tflite")
    interpreter.allocate_tensors()
    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()
else:
    print("WARNING: yamnet.tflite not found. Run download_yamnet.py first.")

@app.route("/")
def index():
    """Serve the frontend."""
    return render_template("index.html")

@app.route("/process-audio", methods=["POST"])
def process_audio():
    """Receive audio from frontend, run through YAMNet, return predictions."""
    if "audio" not in request.files:
        return jsonify({"error": "No audio file provided"}), 400

    if not interpreter:
        return jsonify({"error": "YAMNet model not loaded on server."}), 500

    audio_file = request.files["audio"]
    
    # Save the file temporarily
    with tempfile.NamedTemporaryFile(delete=False, suffix=".wav") as temp_audio:
        temp_path = temp_audio.name
        
    audio_file.save(temp_path)

    try:
        # 1. Load and resample audio to 16kHz mono (required by YAMNet)
        waveform, sr = librosa.load(temp_path, sr=16000, mono=True)
        waveform = np.array(waveform, dtype=np.float32)
        
        # 2. Prepare for LiteRT
        interpreter.resize_tensor_input(input_details[0]['index'], waveform.shape)
        interpreter.allocate_tensors()
        
        interpreter.set_tensor(input_details[0]['index'], waveform)
        interpreter.invoke()
        
        scores = interpreter.get_tensor(output_details[0]['index'])
        
        # Average the scores over the frames if multiple frames
        if len(scores.shape) > 1:
            scores = np.mean(scores, axis=0)
        else:
            scores = scores[0]
            
        # Get top 5 classes
        top_N = 5
        top_indices = np.argsort(scores)[::-1][:top_N]
        
        predictions = []
        for i in top_indices:
            class_name = class_names[i] if i < len(class_names) else f"Class {i}"
            predictions.append({
                "class": class_name,
                "score": float(scores[i])
            })

        return jsonify({
            "status": "success",
            "predictions": predictions,
            "full_response": predictions
        })

    except Exception as e:
        import traceback
        traceback.print_exc()
        return jsonify({"status": "error", "error": str(e)}), 500
    finally:
        # Clean up temporary file
        if os.path.exists(temp_path):
            os.remove(temp_path)

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)
