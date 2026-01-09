"""
Avatar Router
Endpoints for avatar counselor interactions
"""
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import Optional

router = APIRouter()

class InterventionRequest(BaseModel):
    user_id: str
    behavior_type: str
    severity: str
    context: Optional[dict] = None

class CounselorResponse(BaseModel):
    message: str
    audio_url: Optional[str] = None
    action_required: bool = False

@router.post("/intervention")
async def trigger_intervention(request: InterventionRequest):
    """Trigger avatar counselor intervention"""
    # TODO: Implement using backend/shared core logic
    return {
        "success": True,
        "response": {
            "message": "I notice you've been scrolling for a while. How much longer do you need?",
            "audio_url": None,
            "action_required": True
        }
    }

@router.post("/negotiate")
async def negotiate_time(user_id: str, requested_minutes: int, activity: str):
    """Negotiate time agreement with counselor"""
    return {
        "success": True,
        "agreement_id": "agreement_123",
        "approved_minutes": requested_minutes,
        "message": f"Okay, you have {requested_minutes} minutes for {activity}"
    }

@router.get("/status")
async def get_avatar_status():
    """Get current avatar counselor status"""
    return {
        "active": True,
        "voice_enabled": True,
        "current_mode": "monitoring"
    }
