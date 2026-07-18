from fastapi import FastAPI
from routers.analyze import router as analyze_router

app = FastAPI(
    title="ForgeMind Backend"
)

app.include_router(analyze_router)

@app.get("/")
def root():
    return {
        "message": "ForgeMind Backend Running"
    }

@app.get("/health")
def health():
    return {
        "status": "healthy"
    }