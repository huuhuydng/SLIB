"""
Knowledge Base Service
Provides context information for AI responses
"""

from typing import List, Dict


class KnowledgeBaseService:
    """
    Simulated Knowledge Base Service
    In production, this would connect to a database
    """
    
    def __init__(self):
        # Simulated knowledge base data
        self._knowledge: List[Dict] = [
            {
                "title": "Giờ mở cửa",
                "content": "Thư viện mở cửa từ 7:30 sáng đến 21:00 tối các ngày trong tuần (Thứ 2 - Thứ 7). Chủ nhật nghỉ.",
                "type": "INFO"
            },
            {
                "title": "Quy định mượn sách",
                "content": "Sinh viên được mượn tối đa 5 cuốn sách trong 14 ngày. Gia hạn tối đa 2 lần.",
                "type": "RULES"
            },
            {
                "title": "Đặt chỗ ngồi",
                "content": "Sinh viên có thể đặt chỗ ngồi qua app SLIB. Mỗi lần đặt tối đa 4 tiếng.",
                "type": "GUIDE"
            }
        ]
    
    def build_knowledge_context(self) -> str:
        """Build knowledge context string for AI prompt"""
        if not self._knowledge:
            return ""
        
        context = "\n--- KIẾN THỨC THƯ VIỆN ---\n"
        for item in self._knowledge:
            context += f"[{item['type']}] {item['title']}: {item['content']}\n"
        context += "--- HẾT KIẾN THỨC ---\n\n"
        
        return context
    
    def get_all_knowledge(self) -> List[Dict]:
        """Get all knowledge items"""
        return self._knowledge
    
    def add_knowledge(self, title: str, content: str, knowledge_type: str = "INFO"):
        """Add new knowledge item"""
        self._knowledge.append({
            "title": title,
            "content": content,
            "type": knowledge_type
        })


# Singleton instance
knowledge_base_service = KnowledgeBaseService()
