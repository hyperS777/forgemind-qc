document.addEventListener('DOMContentLoaded', () => {
    let mediaRecorder;
    let audioChunks = [];
    let audioBlob = null;
    let isRecording = false;

    // UI Elements
    const recordBtn = document.getElementById('recordBtn');
    const statusIndicator = document.getElementById('statusIndicator');
    const instructionText = document.getElementById('instructionText');
    const audioPlayerContainer = document.getElementById('audioPlayerContainer');
    const audioPlayback = document.getElementById('audioPlayback');
    const actionButtons = document.getElementById('actionButtons');
    const discardBtn = document.getElementById('discardBtn');
    const processBtn = document.getElementById('processBtn');
    const resultPanel = document.getElementById('resultPanel');
    const loader = document.getElementById('loader');
    const transcriptBox = document.getElementById('transcriptBox');
    const errorBox = document.getElementById('errorBox');

    // Initialize MediaRecorder
    async function initAudio() {
        try {
            const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
            mediaRecorder = new MediaRecorder(stream);

            mediaRecorder.ondataavailable = (event) => {
                if (event.data.size > 0) {
                    audioChunks.push(event.data);
                }
            };

            mediaRecorder.onstop = () => {
                // Determine mime type and extension
                const mimeType = mediaRecorder.mimeType || 'audio/webm';
                audioBlob = new Blob(audioChunks, { type: mimeType });
                const audioUrl = URL.createObjectURL(audioBlob);
                
                audioPlayback.src = audioUrl;
                audioPlayerContainer.classList.remove('hidden');
                actionButtons.classList.remove('hidden');
                instructionText.innerText = "Audio recorded successfully";
                statusIndicator.innerText = "Recorded";
                
                // Clear chunks for next recording
                audioChunks = [];
            };

        } catch (err) {
            console.error("Error accessing microphone:", err);
            instructionText.innerText = "Microphone access denied.";
            instructionText.style.color = "#ff8a8e";
        }
    }

    // Toggle recording state
    recordBtn.addEventListener('click', () => {
        if (!mediaRecorder) {
            initAudio().then(() => {
                if (mediaRecorder) toggleRecording();
            });
            return;
        }
        toggleRecording();
    });

    function toggleRecording() {
        if (!isRecording) {
            // Start recording
            audioBlob = null;
            audioChunks = [];
            audioPlayerContainer.classList.add('hidden');
            actionButtons.classList.add('hidden');
            resultPanel.classList.add('hidden');
            
            mediaRecorder.start();
            isRecording = true;
            
            recordBtn.classList.add('recording');
            statusIndicator.classList.add('recording');
            statusIndicator.innerText = "Recording...";
            instructionText.innerText = "Tap to stop recording";
        } else {
            // Stop recording
            mediaRecorder.stop();
            isRecording = false;
            
            recordBtn.classList.remove('recording');
            statusIndicator.classList.remove('recording');
        }
    }

    // Discard recording
    discardBtn.addEventListener('click', () => {
        audioBlob = null;
        audioPlayerContainer.classList.add('hidden');
        actionButtons.classList.add('hidden');
        resultPanel.classList.add('hidden');
        instructionText.innerText = "Tap to start recording";
        statusIndicator.innerText = "Ready";
    });

    // Process with Sarvam AI
    processBtn.addEventListener('click', async () => {
        if (!audioBlob) return;

        // UI updates
        actionButtons.classList.add('hidden');
        resultPanel.classList.remove('hidden');
        loader.classList.remove('hidden');
        transcriptBox.classList.add('hidden');
        const payloadBox = document.getElementById('payloadBox');
        if (payloadBox) payloadBox.classList.add('hidden');
        errorBox.classList.add('hidden');
        statusIndicator.innerText = "Processing...";

        // Create form data
        const formData = new FormData();
        // Sarvam often prefers standard formats, we send as wav/webm
        const ext = audioBlob.type.includes('webm') ? 'webm' : 'wav';
        formData.append('audio', audioBlob, `recording.${ext}`);

        try {
            const response = await fetch('/process-audio', {
                method: 'POST',
                body: formData
            });

            const data = await response.json();

            loader.classList.add('hidden');

            if (response.ok && data.status === 'success') {
                transcriptBox.innerText = data.transcript;
                transcriptBox.classList.remove('hidden');
                
                // Show payload
                if (data.full_response) {
                    const payloadBox = document.getElementById('payloadBox');
                    const payloadContent = document.getElementById('payloadContent');
                    if (payloadBox && payloadContent) {
                        payloadContent.innerText = JSON.stringify(data.full_response, null, 2);
                        payloadBox.classList.remove('hidden');
                    }
                }

                statusIndicator.innerText = "Completed";
            } else {
                throw new Error(data.error || "Unknown error occurred");
            }
        } catch (err) {
            loader.classList.add('hidden');
            errorBox.innerText = err.message || "Failed to process audio.";
            errorBox.classList.remove('hidden');
            actionButtons.classList.remove('hidden'); // allow retry
            statusIndicator.innerText = "Error";
            console.error(err);
        }
    });

    // Initialize on load
    initAudio();
});
