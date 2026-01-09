"""
FocusMotherFocus API Server
FastAPI backend for cross-platform mobile and desktop clients
"""
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from routers import monitoring, agreements, avatar

app = FastAPI(
    title="FocusMotherFocus API",
    description="AI Productivity Counselor API",
    version="1.0.0"
)

# CORS middleware for mobile clients
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Configure appropriately for production
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include routers
app.include_router(monitoring.router, prefix="/api/v1/monitoring", tags=["monitoring"])
app.include_router(agreements.router, prefix="/api/v1/agreements", tags=["agreements"])
app.include_router(avatar.router, prefix="/api/v1/avatar", tags=["avatar"])

@app.get("/")
async def root():
    return {
        "message": "FocusMotherFocus API",
        "version": "1.0.0",
        "status": "running"
    }

@app.get("/health")
async def health_check():
    return {"status": "healthy"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
