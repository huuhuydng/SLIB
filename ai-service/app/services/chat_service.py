"""
RAG Chat Service
Handles RAG (Retrieval-Augmented Generation) chat with strict guardrails
"""

import logging
from typing import List, Dict, Any, Optional, Tuple
from datetime import datetime

from langchain_community.llms import Ollama
from langchain_core.prompts import PromptTemplate

from app.config.settings import get_settings
from app.services.embedding_service import get_embedding_service
from app.models.schemas import ActionType

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


# System prompt template with strict guardrails
RAG_SYSTEM_PROMPT = """Bạn là **SLIB AI Assistant** - trợ lý thông minh của hệ thống Thư viện thông minh SLIB.
Nhiệm vụ của bạn là hỗ trợ sinh viên và giảng viên tìm kiếm thông tin trong thư viện.
Hãy trả lời câu hỏi dựa trên context được cung cấp dưới đây.
Phong cách trả lời: Ngắn gọn, thân thiện và chính xác bằng tiếng Việt.

**Quy tắc tối thượng:** TUYỆT ĐỐI KHÔNG sử dụng kiến thức bên ngoài (pre-trained knowledge). KHÔNG được suy đoán.
Nếu context không chứa câu trả lời, hãy trả lời chính xác cụm từ: 'I_DO_NOT_KNOW'.

--- CONTEXT TỪ THƯ VIỆN ---
{context}
--- HẾT CONTEXT ---

Câu hỏi của người dùng: {question}

Trả lời (bằng tiếng Việt, ngắn gọn và thân thiện):"""


# Greeting patterns and responses - answered WITHOUT RAG search
GREETING_PATTERNS = {
    # Identity questions
    "bạn là ai": "Xin chào! 👋 Tôi là **SLIB AI Assistant** - trợ lý thông minh của hệ thống Thư viện SLIB. Tôi có thể giúp bạn:\n• Tìm kiếm thông tin về quy định thư viện\n• Hướng dẫn đặt chỗ ngồi\n• Trả lời các câu hỏi về dịch vụ thư viện\n\nHãy hỏi tôi bất cứ điều gì về thư viện nhé!",
    "bạn là gì": "Tôi là **SLIB AI Assistant** - chatbot hỗ trợ của thư viện thông minh SLIB. Tôi được thiết kế để giúp sinh viên và giảng viên tra cứu thông tin về thư viện một cách nhanh chóng.",
    "ai đang nói": "Tôi là **SLIB AI Assistant** - trợ lý ảo của hệ thống thư viện SLIB!",
    "giới thiệu": "Xin chào! Tôi là **SLIB AI Assistant** 🤖\n\nTôi là trợ lý thông minh của Thư viện SLIB, được tạo ra để hỗ trợ sinh viên và giảng viên tìm kiếm thông tin nhanh chóng. Hãy đặt câu hỏi cho tôi về:\n• Quy định thư viện\n• Cách đặt chỗ ngồi\n• Điểm uy tín\n• Và nhiều thông tin khác!",
    
    # Greetings
    "xin chào": "Xin chào! 👋 Tôi là SLIB AI Assistant. Tôi có thể giúp gì cho bạn hôm nay?",
    "chào bạn": "Chào bạn! 😊 Tôi sẵn sàng hỗ trợ bạn. Bạn cần tìm hiểu thông tin gì về thư viện?",
    "hello": "Hello! 👋 Tôi là SLIB AI Assistant. Bạn cần hỗ trợ gì về thư viện?",
    "hi": "Hi! Tôi là SLIB AI. Bạn muốn hỏi gì về thư viện?",
    
    # Thanks
    "cảm ơn": "Không có gì! 😊 Rất vui được hỗ trợ bạn. Nếu cần thêm thông tin, đừng ngần ngại hỏi nhé!",
    "cám ơn": "Rất vui được giúp đỡ! 🙂 Chúc bạn có trải nghiệm tốt tại thư viện!",
    "thanks": "You're welcome! 😊 Rất vui được hỗ trợ bạn!",
    
    # Help
    "giúp tôi": "Tôi có thể giúp bạn:\n• Tra cứu quy định thư viện\n• Hướng dẫn đặt chỗ ngồi\n• Giải đáp thắc mắc về điểm uy tín\n• Trả lời câu hỏi về dịch vụ thư viện\n\nBạn muốn hỏi về vấn đề gì?",
    "hỗ trợ": "Tôi sẵn sàng hỗ trợ! Bạn có thể hỏi tôi về quy định thư viện, cách đặt chỗ, điểm uy tín, v.v. Hãy đặt câu hỏi nhé!",
}


class RAGChatService:
    """
    RAG Chat Service with similarity threshold checking and guardrails
    
    Workflow:
    1. Generate embedding for user query
    2. Search PostgreSQL for similar chunks (cosine distance)
    3. Check similarity threshold (>= 0.75)
    4. If threshold met: Generate response with LLM
    5. If not met: Return ESCALATE_TO_LIBRARIAN
    6. Check LLM response for "I_DO_NOT_KNOW"
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
            f"[RAGChatService] Initialized with model={self.model}, "
            f"threshold={self.similarity_threshold}"
        )
    
    def _check_greeting(self, message: str) -> Optional[str]:
        """
        Check if message matches a greeting pattern
        Returns response if matched, None otherwise
        """
        normalized = message.lower().strip()
        # Remove punctuation for matching
        normalized = ''.join(c for c in normalized if c.isalnum() or c.isspace())
        
        for pattern, response in GREETING_PATTERNS.items():
            if pattern in normalized or normalized in pattern:
                return response
        
        return None
    
    
    def retrieve_context(
        self, 
        query: str, 
        top_k: int = None
    ) -> Tuple[List[Dict[str, Any]], float]:
        """
        Retrieve relevant context from Qdrant vector database
        
        Args:
            query: User's question
            top_k: Number of chunks to retrieve
            
        Returns:
            Tuple of (chunks list, best similarity score)
        """
        if top_k is None:
            top_k = self.max_chunks
        
        try:
            # Generate query embedding
            query_embedding = self.embedding_service.embed_text(query)
            
            # Search in Qdrant
            from app.services.qdrant_service import get_qdrant_service
            qdrant_service = get_qdrant_service()
            
            results = qdrant_service.search(
                query_vector=query_embedding,
                limit=top_k,
                score_threshold=0.0  # Get all results, filter later
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
            
            logger.info(
                f"[RAGChatService] Retrieved {len(chunks)} chunks from Qdrant, "
                f"best score: {best_score:.4f}"
            )
            
            return chunks, best_score
                
        except Exception as e:
            logger.error(f"[RAGChatService] Error retrieving context: {e}")
            return [], 0.0
    
    def generate_response(
        self, 
        query: str, 
        context_chunks: List[Dict[str, Any]]
    ) -> str:
        """
        Generate response using LLM with context
        
        Args:
            query: User's question
            context_chunks: Retrieved context chunks
            
        Returns:
            LLM generated response
        """
        try:
            # Build context string from chunks
            context_parts = []
            for chunk in context_chunks:
                source = chunk.get("source", "Unknown")
                content = chunk.get("content", "")
                context_parts.append(f"[Nguồn: {source}]\n{content}")
            
            context = "\n\n".join(context_parts)
            
            if not context:
                context = "Không có thông tin nào được tìm thấy trong cơ sở dữ liệu."
            
            # Format prompt
            prompt = self.prompt_template.format(
                context=context,
                question=query
            )
            
            logger.info(f"[RAGChatService] Generating response with LLM...")
            
            # Call LLM
            response = self.llm.invoke(prompt)
            
            logger.info(f"[RAGChatService] LLM response: {response[:100]}...")
            
            return response.strip()
            
        except Exception as e:
            logger.error(f"[RAGChatService] Error generating response: {e}")
            return self.I_DO_NOT_KNOW_MARKER
    
    def query(self, message: str) -> Dict[str, Any]:
        """
        Main RAG query method with full workflow
        
        Args:
            message: User's question
            
        Returns:
            Dict with reply, action, similarity_score, sources
        """
        logger.info(f"[RAGChatService] Processing query: {message}")
        
        # Step 0: Check for greeting/common questions (no RAG needed)
        greeting_response = self._check_greeting(message)
        if greeting_response:
            logger.info(f"[RAGChatService] Matched greeting pattern, returning direct response")
            return {
                "success": True,
                "reply": greeting_response,
                "action": ActionType.NONE,
                "similarity_score": 1.0,  # Perfect match for greetings
                "sources": []
            }
        
        # Step 1: Retrieve context
        chunks, best_score = self.retrieve_context(message)
        
        logger.info(
            f"[RAGChatService] Retrieval complete - "
            f"chunks: {len(chunks)}, best_score: {best_score:.4f}, "
            f"threshold: {self.similarity_threshold}"
        )
        
        # Step 2: Check similarity threshold
        if best_score < self.similarity_threshold:
            logger.info(
                f"[RAGChatService] Score {best_score:.4f} < threshold {self.similarity_threshold}. "
                f"Escalating to librarian."
            )
            return {
                "success": True,
                "reply": "Xin lỗi, hiện tại tôi chưa có thông tin về vấn đề này trong cơ sở dữ liệu. Bạn có thể liên hệ thủ thư để được hỗ trợ thêm.",
                "action": ActionType.ESCALATE_TO_LIBRARIAN,
                "similarity_score": best_score,
                "sources": []
            }
        
        # Step 3: Generate response with LLM
        # Filter chunks above threshold
        relevant_chunks = [c for c in chunks if c["similarity_score"] >= self.similarity_threshold]
        
        response = self.generate_response(message, relevant_chunks)
        
        # Step 4: Check for I_DO_NOT_KNOW
        if self.I_DO_NOT_KNOW_MARKER in response:
            logger.info("[RAGChatService] LLM returned I_DO_NOT_KNOW. Escalating to librarian.")
            return {
                "success": True,
                "reply": "Xin lỗi, hiện tại tôi chưa có thông tin về vấn đề này trong cơ sở dữ liệu.",
                "action": ActionType.ESCALATE_TO_LIBRARIAN,
                "similarity_score": best_score,
                "sources": []
            }
        
        # Step 5: Return successful response
        sources = [
            {
                "source": c["source"],
                "score": round(c["similarity_score"], 4)
            }
            for c in relevant_chunks[:3]  # Top 3 sources
        ]
        
        return {
            "success": True,
            "reply": response,
            "action": ActionType.NONE,
            "similarity_score": best_score,
            "sources": sources
        }
    
    def test_connection(self) -> Dict[str, Any]:
        """Test RAG service connections"""
        try:
            # Test embedding
            embed_result = self.embedding_service.test_connection()
            if not embed_result["success"]:
                return embed_result
            
            # Test LLM
            test_response = self.llm.invoke("Say 'OK' if you can hear me.")
            
            return {
                "success": True,
                "message": f"RAG service ready. Model: {self.model}",
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
