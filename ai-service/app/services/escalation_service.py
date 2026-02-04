"""
Escalation Service - Handles AI-to-Human escalation logic
Detects when user needs human support and calls backend API
"""

import httpx
import logging
import os
from typing import Optional

logger = logging.getLogger(__name__)


# Keywords that trigger escalation to human librarian
ESCALATION_KEYWORDS = [
    # Vietnamese
    "gặp thủ thư",
    "cần người hỗ trợ",
    "muốn nói chuyện với nhân viên",
    "liên hệ thủ thư",
    "chat với người thật",
    "muốn gặp người",
    "không hiểu",
    "giúp tôi gặp",
    "cần hỗ trợ trực tiếp",
    "gọi nhân viên",
    # English
    "talk to human",
    "speak to librarian",
    "real person",
    "human support",
    "need help from staff"
]


class EscalationService:
    """Service for handling escalation from AI to human librarian"""
    
    def __init__(self):
        self.backend_url = os.getenv("BACKEND_URL", "http://localhost:8080/slib")
        self.internal_api_key = os.getenv("INTERNAL_API_KEY", "default-internal-key")
        self.client = httpx.Client(timeout=10.0)
    
    def should_escalate(self, user_message: str, ai_response: str) -> tuple[bool, Optional[str]]:
        """
        Check if conversation should be escalated to human
        
        Args:
            user_message: User's message
            ai_response: AI's generated response
            
        Returns:
            Tuple of (should_escalate, reason)
        """
        user_lower = user_message.lower()
        
        # Check for escalation keywords in user message
        for keyword in ESCALATION_KEYWORDS:
            if keyword in user_lower:
                return True, f"Người dùng yêu cầu: '{keyword}'"
        
        # Check if AI is uncertain (needs_review flag)
        if "[KHÔNG CHẮC CHẮN]" in ai_response:
            return True, "AI không chắc chắn về câu trả lời"
        
        return False, None
    
    async def escalate_conversation(
        self, 
        conversation_id: str, 
        student_id: str, 
        reason: Optional[str] = None
    ) -> dict:
        """
        Call backend API to escalate conversation to human
        
        Args:
            conversation_id: UUID of the conversation
            student_id: UUID of the student
            reason: Reason for escalation
            
        Returns:
            Response from backend
        """
        try:
            url = f"{self.backend_url}/internal/chat/escalate"
            payload = {
                "conversationId": conversation_id,
                "studentId": student_id,
                "reason": reason or "Người dùng yêu cầu nói chuyện với thủ thư"
            }
            
            response = self.client.post(
                url,
                json=payload,
                headers={
                    "Content-Type": "application/json",
                    "X-API-Key": self.internal_api_key
                }
            )
            response.raise_for_status()
            
            logger.info(f"🔔 Escalated conversation {conversation_id} to human. Reason: {reason}")
            return response.json()
            
        except Exception as e:
            logger.error(f"Error escalating conversation: {str(e)}")
            return {"success": False, "error": str(e)}
    
    async def send_ai_reply(
        self,
        conversation_id: str,
        student_id: str,
        content: str,
        attachment_url: Optional[str] = None,
        message_type: str = "TEXT"
    ) -> dict:
        """
        Send AI reply to student via backend API
        
        Args:
            conversation_id: UUID of the conversation
            student_id: UUID of the student
            content: Message content
            attachment_url: Optional attachment URL
            message_type: TEXT, IMAGE, or FILE
            
        Returns:
            Response from backend
        """
        try:
            url = f"{self.backend_url}/internal/chat/reply"
            payload = {
                "conversationId": conversation_id,
                "studentId": student_id,
                "content": content,
                "attachmentUrl": attachment_url,
                "messageType": message_type
            }
            
            response = self.client.post(
                url,
                json=payload,
                headers={
                    "Content-Type": "application/json",
                    "X-API-Key": self.internal_api_key
                }
            )
            response.raise_for_status()
            
            logger.info(f"🤖 Sent AI reply to student {student_id}")
            return response.json()
            
        except Exception as e:
            logger.error(f"Error sending AI reply: {str(e)}")
            return {"success": False, "error": str(e)}
    
    def __del__(self):
        """Cleanup HTTP client"""
        if hasattr(self, 'client'):
            self.client.close()


# Singleton instance
escalation_service = EscalationService()
