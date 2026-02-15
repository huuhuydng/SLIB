"""
RAG Chat Service - SLIB AI (MoMo Style Edition 🌸)
Handles RAG (Retrieval-Augmented Generation) chat with strict guardrails
and "Cute & Polite" persona logic.
"""

import logging
import re
from typing import List, Dict, Any, Optional, Tuple
from datetime import datetime

from langchain_community.llms import Ollama
from langchain_core.prompts import PromptTemplate

# Import các module từ project của bạn
from app.config.settings import get_settings
from app.services.embedding_service import get_embedding_service
from app.models.schemas import ActionType
from app.services.qdrant_service import get_qdrant_service

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


# --- 1. CẤU HÌNH NGƯỠNG (THRESHOLDS) ---
# Điểm < 0.35: Coi là spam/gõ bậy -> Giới thiệu lại bản thân (Giống MoMo case "hdjdhdiirhfh")
SPAM_THRESHOLD = 0.35 

# Điểm >= 0.75 (Lấy từ settings): Mới được coi là tìm thấy thông tin
# Khoảng giữa (0.35 - 0.75): Coi là câu hỏi khó/không có data -> Xin lỗi nhẹ nhàng


# --- 2. SYSTEM PROMPT (STYLE MOMO) ---
RAG_SYSTEM_PROMPT = """Bạn là **SLIB AI** - trợ thủ đắc lực và siêu dễ thương của Thư viện SLIB 🌸

--- NHIỆM VỤ ---
Trả lời câu hỏi của sinh viên dựa trên thông tin được cung cấp trong phần CONTEXT.

--- QUY TẮC AN TOÀN (BẮT BUỘC) ---
1. Đọc kỹ phần CONTEXT bên dưới.
2. Nếu thông tin KHÔNG có trong CONTEXT, bạn phải trả lời duy nhất cụm từ: I_DO_NOT_KNOW
3. Tuyệt đối KHÔNG tự bịa ra thông tin hoặc sử dụng kiến thức bên ngoài context.

--- PHONG CÁCH TRẢ LỜI (MOMO STYLE) ---
- Bắt buộc bắt đầu câu trả lời bằng: **"Dạ,"**
- Xưng hô: "mình" (SLIB AI) và "bạn".
- Giọng điệu: Lễ phép, nhẹ nhàng, nhiệt tình.
- Ví dụ: "Dạ, theo quy định thì...", "Dạ, hiện tại thư viện có..."

--- CONTEXT TỪ THƯ VIỆN ---
{context}
--- HẾT CONTEXT ---

Câu hỏi: {question}

Câu trả lời của bạn:"""


# --- 3. CÁC MẪU CÂU TRẢ LỜI CỐ ĐỊNH ---

# A. Greeting Patterns (Xã giao)
GREETING_PATTERNS = {
    # Identity & Intro
    "bạn là ai": "Dạ, mình là **SLIB AI** - Trợ lý ảo hỗ trợ sinh viên tại Thư viện SLIB ạ! Mình có thể giúp bạn tra cứu sách, xem quy định hoặc hướng dẫn đặt phòng... Bạn cần mình hỗ trợ gì hông nè?",
    "bạn là gì": "Dạ, mình là **SLIB AI** - chatbot của thư viện SLIB đó ạ! Mình được tạo ra để giúp bạn tìm thông tin thư viện nhanh gọn lẹ nè!",
    "ai đang nói": "Dạ, là mình nè - **SLIB AI**, trợ lý ảo của thư viện SLIB!",
    "giới thiệu": "Dạ, chào bạn! Mình là **SLIB AI**.\n\nMình ở đây để giải đáp mọi thắc mắc về Thư viện SLIB một cách nhanh nhất. Đừng ngại hỏi mình nha!",
    
    # Greetings
    "xin chào": "Dạ, chào bạn ạ! SLIB AI đã sẵn sàng. Hôm nay bạn cần tìm thông tin gì nè?",
    "chào bạn": "Dạ, chào bạn nha! Mình sẵn sàng hỗ trợ bạn rồi đây. Cần mình giúp gì không?",
    "hello": "Dạ, hello bạn! SLIB AI nghe nè!",
    "hi": "Dạ, hi bạn! Rất vui được gặp bạn. Cần mình giúp gì cứ nói nha!",
    
    # Thanks
    "cảm ơn": "Dạ, không có chi đâu ạ! Giúp được bạn là niềm vui của mình mà. Cần gì thêm cứ ới mình nha!",
    "cám ơn": "Dạ, hông có gì nè! Chúc bạn học tập thật tốt tại thư viện nhé!",
    "thanks": "Dạ, you're welcome! Chúc bạn một ngày học tập thật hiệu quả!",
    
    # Help
    "giúp tôi": "Dạ, mình sẵn sàng nè! Bạn muốn hỏi về quy định, đặt chỗ hay điểm uy tín? Nói cho mình biết đi!",
    "hỗ trợ": "Dạ, mình ở đây để hỗ trợ bạn mà! Cứ hỏi thoải mái nhé, mình sẽ cố gắng trả lời nhanh nhất có thể!",
}

# B. Fallback Messages (Xử lý lỗi)

# Case 1: Spam/Gõ bậy (Score < SPAM_THRESHOLD)
# Giống MoMo đoạn "hdjdhdiirhfh" -> Giới thiệu lại bản thân để định hướng user
MSG_SPAM_DETECTED = "Dạ, mình là **SLIB AI** - Trợ lý thư viện đây ạ!\n\nMình có thể giúp bạn giải quyết các vấn đề liên quan đến Thư viện SLIB (như quy định, giờ mở cửa, tài liệu...). Bạn cần hỗ trợ gì hông ạ?"

# Case 2: Không tìm thấy tin (SPAM_THRESHOLD <= Score < Similarity_Threshold)
# Giống MoMo đoạn "Slib là gì" -> Xin lỗi vì chưa có data
MSG_NO_INFO = "Dạ, mình chưa có thông tin về vấn đề này trong hệ thống thư viện ạ.\n\nBạn có thể mô tả rõ hơn hoặc hỏi về vấn đề khác được không ạ? Nếu cần gấp, mình sẽ nhờ thủ thư hỗ trợ bạn nha!"

# C. Escalation Patterns (User muốn gặp thủ thư)
ESCALATION_PATTERNS = [
    # Gặp thủ thư
    "gặp thủ thư", "gap thu thu",
    "nói chuyện với thủ thư", "noi chuyen voi thu thu",
    "liên hệ thủ thư", "lien he thu thu",
    "chat với thủ thư", "chat voi thu thu",
    "hỏi thủ thư", "hoi thu thu",
    # Gặp người/nhân viên
    "gặp người", "gap nguoi",
    "gặp nhân viên", "gap nhan vien",
    "nói chuyện với người", "noi chuyen voi nguoi",
    # Muốn được hỗ trợ trực tiếp
    "cần hỗ trợ trực tiếp", "can ho tro truc tiep",
    "muốn được hỗ trợ", "muon duoc ho tro",
    "cho tôi gặp", "cho em gặp",
    "chuyển cho người", "chuyen cho nguoi",
]

MSG_ESCALATION_OFFER = "Dạ, để được gặp thủ thư, bạn vui lòng bấm vào nút bên dưới ạ!"



class RAGChatService:
    """
    RAG Chat Service
    Workflow:
    1. Check Greeting (Xã giao)
    2. Retrieve Context (Tìm kiếm)
    3. Check Spam (Score quá thấp -> Re-intro)
    4. Check Low Confidence (Score trung bình -> Xin lỗi)
    5. Generate Answer (Score cao -> Trả lời cute)
    """
    
    I_DO_NOT_KNOW_MARKER = "I_DO_NOT_KNOW"
    
    def __init__(self):
        settings = get_settings()
        
        self.similarity_threshold = settings.similarity_threshold
        self.max_chunks = settings.max_retrieved_chunks
        self.ollama_url = settings.ollama_url
        self.model = settings.ollama_model
        
        self.embedding_service = get_embedding_service()
        
        # Initialize LangChain Ollama LLM
        self.llm = Ollama(
            model=self.model,
            base_url=self.ollama_url,
            temperature=settings.default_temperature,
            num_predict=settings.default_max_tokens
        )
        
        # Prompt template
        self.prompt_template = PromptTemplate(
            template=RAG_SYSTEM_PROMPT,
            input_variables=["context", "question"]
        )
        
        logger.info(
            f"[RAGChatService] Initialized. Model={self.model}, "
            f"Threshold={self.similarity_threshold}, SpamThreshold={SPAM_THRESHOLD}"
        )
    
    def _check_greeting(self, message: str) -> Optional[str]:
        """Check if message matches a greeting pattern"""
        normalized = message.lower().strip()
        # Remove punctuation
        normalized = ''.join(c for c in normalized if c.isalnum() or c.isspace())
        
        words = set(normalized.split())
        
        for pattern, response in GREETING_PATTERNS.items():
            if ' ' not in pattern:
                if pattern in words:
                    return response
            else:
                if pattern in normalized:
                    return response
        return None
    
    def _check_escalation(self, message: str) -> bool:
        """Check if user wants to talk to librarian directly"""
        normalized = message.lower().strip()
        # Remove punctuation
        normalized = ''.join(c for c in normalized if c.isalnum() or c.isspace())
        
        for pattern in ESCALATION_PATTERNS:
            if pattern in normalized:
                logger.info(f"[Escalation] Detected pattern: '{pattern}'")
                return True
        return False
    
    def retrieve_context(self, query: str, top_k: int = None) -> Tuple[List[Dict[str, Any]], float]:
        """Retrieve relevant context from Qdrant vector database"""
        if top_k is None:
            top_k = self.max_chunks
        
        try:
            # Generate query embedding
            query_embedding = self.embedding_service.embed_text(query)
            
            # Search in Qdrant
            qdrant_service = get_qdrant_service()
            
            # Lấy kết quả thô, chưa lọc score_threshold vội để xử lý logic spam
            results = qdrant_service.search(
                query_vector=query_embedding,
                limit=top_k,
                score_threshold=0.0
            )
            
            chunks = []
            best_score = 0.0
            
            for result in results:
                score = float(result.get("score", 0))
                chunks.append({
                    "id": result.get("id"),
                    "content": result.get("content", ""),
                    "source": result.get("source", ""),
                    "category": result.get("category", ""),
                    "similarity_score": score
                })
                if score > best_score:
                    best_score = score
            
            logger.info(f"[Retrieve] Found {len(chunks)} chunks. Best score: {best_score:.4f}")
            return chunks, best_score
                
        except Exception as e:
            logger.error(f"[RAGChatService] Error retrieving context: {e}")
            return [], 0.0
    
    def generate_response(self, query: str, context_chunks: List[Dict[str, Any]]) -> str:
        """Generate response using LLM with context"""
        try:
            context_parts = []
            for chunk in context_chunks:
                source = chunk.get("source", "Tài liệu thư viện")
                content = chunk.get("content", "")
                # Format rõ ràng với dấu gạch ngang
                context_parts.append(f"--- Nguồn: {source} ---\n{content}")
            
            context = "\n\n".join(context_parts)
            
            if not context:
                context = "Empty context."
            
            prompt = self.prompt_template.format(
                context=context,
                question=query
            )
            
            logger.info(f"[Gen] Invoking LLM...")
            response = self.llm.invoke(prompt)
            return response.strip()
            
        except Exception as e:
            logger.error(f"[RAGChatService] Error generating response: {e}")
            return self.I_DO_NOT_KNOW_MARKER
    
    def query(self, message: str) -> Dict[str, Any]:
        """
        Main RAG query method with MoMo-style logic
        """
        logger.info(f"[Query] User: {message}")
        
        # 0. Escalation Check - User muốn gặp thủ thư
        if self._check_escalation(message):
            return {
                "success": True,
                "reply": MSG_ESCALATION_OFFER,
                "action": ActionType.SHOW_ESCALATION_BUTTON,
                "similarity_score": 1.0,
                "sources": [],
                "needs_review": True,
                "escalated": True
            }
        
        # 1. Greeting Check
        greeting_response = self._check_greeting(message)
        if greeting_response:
            return {
                "success": True,
                "reply": greeting_response,
                "action": ActionType.NONE,
                "similarity_score": 1.0,
                "sources": []
            }
        
        # 2. Retrieve Context
        chunks, best_score = self.retrieve_context(message)
        
        # 3. Check Spam/Gibberish (Score cực thấp)
        # Ví dụ: "hdjdhdiirhfh" -> Score ~ 0.2 -> Trả lời giới thiệu bản thân
        if best_score < SPAM_THRESHOLD:
            logger.info(f"[Query] Spam detected (Score {best_score:.4f} < {SPAM_THRESHOLD})")
            return {
                "success": True,
                "reply": MSG_SPAM_DETECTED,
                "action": ActionType.NONE, # Không cần escalate spam
                "similarity_score": best_score,
                "sources": []
            }

        # 4. Check Low Confidence (Score lửng lơ)
        # Ví dụ: "Slib là gì" -> Score ~ 0.5 -> Xin lỗi
        if best_score < self.similarity_threshold:
            logger.info(f"[Query] Low confidence ({best_score:.4f} < {self.similarity_threshold}). Escalating.")
            return {
                "success": True,
                "reply": MSG_NO_INFO,
                "action": ActionType.ESCALATE_TO_LIBRARIAN,
                "similarity_score": best_score,
                "sources": []
            }
        
        # 5. Generate Response (Score cao)
        relevant_chunks = [c for c in chunks if c["similarity_score"] >= self.similarity_threshold]
        response = self.generate_response(message, relevant_chunks)
        
        # 6. Check I_DO_NOT_KNOW (LLM fallback)
        if self.I_DO_NOT_KNOW_MARKER in response:
            logger.info("[Query] LLM returned I_DO_NOT_KNOW.")
            return {
                "success": True,
                "reply": MSG_NO_INFO, # Dùng chung câu xin lỗi dễ thương
                "action": ActionType.ESCALATE_TO_LIBRARIAN,
                "similarity_score": best_score,
                "sources": []
            }
        
        # 7. Success
        sources = [
            {"source": c["source"], "score": round(c["similarity_score"], 4)}
            for c in relevant_chunks[:3]
        ]
        
        return {
            "success": True,
            "reply": response,
            "action": ActionType.NONE,
            "similarity_score": best_score,
            "sources": sources
        }
    
    # --- DEBUG HELPERS (FULL VERSION) ---
    def _normalize_query(self, message: str) -> str:
        normalized = message.lower().strip()
        return ''.join(c for c in normalized if c.isalnum() or c.isspace())
    
    def query_with_debug(self, message: str) -> Dict[str, Any]:
        """
        RAG query with detailed debug information for Admin Dashboard.
        Logic MUST mirror query() exactly but populating the debug dict.
        """
        logger.info(f"[Debug] Processing: {message}")
        
        debug = {
            "query_analysis": {
                "original": message,
                "normalized": self._normalize_query(message),
                "is_greeting": False,
                "greeting_pattern": None
            },
            "retrieval": {
                "spam_threshold": SPAM_THRESHOLD,
                "similarity_threshold": self.similarity_threshold,
                "best_score": 0.0,
                "passed_threshold": False,
                "is_spam": False,
                "chunks_found": 0,
                "chunks": []
            },
            "generation": {
                "used_llm": False,
                "llm_returned_idk": False,
                "action_reason": ""
            }
        }
        
        # 1. Check Greeting
        greeting_response = self._check_greeting(message)
        if greeting_response:
            debug["query_analysis"]["is_greeting"] = True
            debug["generation"]["action_reason"] = "Matched greeting pattern"
            return {
                "success": True, 
                "reply": greeting_response, 
                "action": ActionType.NONE, 
                "debug": debug
            }
        
        # 2. Retrieve
        chunks, best_score = self.retrieve_context(message)
        debug["retrieval"]["best_score"] = round(best_score, 4)
        debug["retrieval"]["chunks_found"] = len(chunks)
        debug["retrieval"]["passed_threshold"] = best_score >= self.similarity_threshold
        
        # Add detailed chunks info for debug view
        for i, chunk in enumerate(chunks[:5]):
            full_content = chunk.get("content", "")
            debug["retrieval"]["chunks"].append({
                "rank": i + 1,
                "score": round(chunk.get("similarity_score", 0), 4),
                "content": full_content[:200] + "..." if len(full_content) > 200 else full_content,
                "full_content": full_content,  # Full content for expandable view
                "source": chunk.get("source", "Unknown"),
            })
            
        # 3. Check Spam
        if best_score < SPAM_THRESHOLD:
            debug["retrieval"]["is_spam"] = True
            debug["generation"]["action_reason"] = f"Spam detected (Score {best_score:.4f} < {SPAM_THRESHOLD})"
            return {
                "success": True,
                "reply": MSG_SPAM_DETECTED,
                "action": ActionType.NONE,
                "debug": debug
            }
            
        # 4. Check Low Confidence
        if best_score < self.similarity_threshold:
            debug["generation"]["action_reason"] = f"Score {best_score:.4f} < Threshold {self.similarity_threshold}"
            return {
                "success": True,
                "reply": MSG_NO_INFO,
                "action": ActionType.ESCALATE_TO_LIBRARIAN,
                "debug": debug
            }
        
        # 5. Generate with LLM
        relevant_chunks = [c for c in chunks if c["similarity_score"] >= self.similarity_threshold]
        debug["generation"]["used_llm"] = True
        debug["generation"]["used_chunks_count"] = len(relevant_chunks)
        # Track which chunks are actually used (by rank, 1-indexed)
        debug["generation"]["used_chunks"] = [
            {
                "rank": chunks.index(c) + 1,
                "score": round(c.get("similarity_score", 0), 4),
                "source": c.get("source", "Unknown")
            }
            for c in relevant_chunks
        ]
        
        response = self.generate_response(message, relevant_chunks)
        
        # 6. Check IDK
        if self.I_DO_NOT_KNOW_MARKER in response:
            debug["generation"]["llm_returned_idk"] = True
            debug["generation"]["action_reason"] = "LLM returned I_DO_NOT_KNOW marker"
            return {
                "success": True,
                "reply": MSG_NO_INFO,
                "action": ActionType.ESCALATE_TO_LIBRARIAN,
                "debug": debug
            }
        
        # 7. Success
        debug["generation"]["action_reason"] = "Successfully generated response"
        return {
            "success": True,
            "reply": response,
            "action": ActionType.NONE,
            "debug": debug
        }
    
    def test_connection(self) -> Dict[str, Any]:
        """Test RAG service connections"""
        try:
            # Test embedding
            embed_result = self.embedding_service.test_connection()
            if not embed_result["success"]:
                return embed_result
            
            # Test LLM
            test_response = self.llm.invoke("Say 'OK'")
            
            return {
                "success": True,
                "message": f"RAG ready. Model: {self.model}",
                "embedding_dims": embed_result.get("dimensions", 768)
            }
            
        except Exception as e:
            return {
                "success": False,
                "message": f"RAG service error: {str(e)}"
            }


# Singleton instance
_rag_chat_service = None

def get_rag_chat_service() -> RAGChatService:
    """Get singleton RAGChatService instance"""
    global _rag_chat_service
    if _rag_chat_service is None:
        _rag_chat_service = RAGChatService()
    return _rag_chat_service
