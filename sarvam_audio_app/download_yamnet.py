import requests

def download_file(url, filename):
    print(f"Downloading {filename}...")
    headers = {'User-Agent': 'Mozilla/5.0'}
    response = requests.get(url, headers=headers, allow_redirects=True)
    if response.status_code == 200:
        with open(filename, 'wb') as f:
            f.write(response.content)
        print(f"Successfully downloaded {filename}")
    else:
        print(f"Failed to download {filename}: {response.status_code}")

if __name__ == "__main__":
    download_file(
        "https://raw.githubusercontent.com/tulasiram58827/yamnet_tflite/main/yamnet.tflite",
        "yamnet.tflite"
    )
    download_file(
        "https://raw.githubusercontent.com/tensorflow/models/master/research/audioset/yamnet/yamnet_class_map.csv",
        "yamnet_class_map.csv"
    )
