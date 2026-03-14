"""
RAG Chat Service - SLIB AI (MoMo Style Edition 🌸)
Handles RAG (Retrieval-Augmented Generation) chat with strict guardrails
and "Cute & Polite" persona logic.
Supports real-time statistics queries (density, capacity, peak hours).
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

{live_data}

{chat_history}

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
            input_variables=["context", "question", "live_data", "chat_history"]
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
    
    # --- Realtime detection: Regex patterns (gom nhóm, không cần liệt kê từng keyword) ---
    REALTIME_REGEX_PATTERNS = [
        # Density words: đông, vắng, trống, kín, đầy, full + context
        r'(đông|vắng|trống|kín|đầy|full)',
        # Availability: còn/hết + chỗ/ghế
        r'(còn|hết|con|het)\s*(chỗ|ghế|cho|ghe)',
        # Quantity: bao nhiêu/mấy + người/chỗ/ghế
        r'(bao nhiêu|mấy|bao nhieu|may)\s*(người|chỗ|ghế|nguoi|cho|ghe)',
        # Stats terms
        r'(công suất|cong suat|tỉ lệ|ti le|mật độ|mat do|lấp đầy|lap day)',
        # Peak hours phrasing
        r'(giờ|gio|lúc|luc|khung)\s*.{0,5}(cao điểm|cao diem|đông|dong|vắng|vang)',
        r'(đông|dong|vắng|vang)\s*.{0,5}(giờ|gio|lúc|luc|khung|khi)',
        # Time-of-day: sáng/chiều/tối + nay/mai, or buổi + sáng/chiều/tối
        r'(sáng|chiều|tối|sang|chieu|toi)\s*(nay|mai|qua)',
        r'buổi\s*(sáng|chiều|tối|sang|chieu|toi)',
        # Relative time
        r'(hôm nay|hom nay|hôm qua|hom qua|ngày mai|ngay mai|bây giờ|bay gio|hiện tại|hien tai)',
        # Weekday / week
        r'thứ\s*(hai|ba|tư|tu|năm|nam|sáu|sau|bảy|bay)',
        r'(tuần|tuan|cuối tuần|cuoi tuan|chủ nhật|chu nhat)\s*(này|nay|sau|trước|truoc)?',
        # Specific time: "2 giờ", "14h", "9:00"
        r'\d+\s*(giờ|gio|h\b|:00)',
        # "nên đến/đi lúc nào"
        r'nên\s*(đến|đi|den|di)\s*(lúc|luc|khi)',
    ]

    # Semantic similarity: mẫu câu realtime để so cosine similarity
    REALTIME_REFERENCE_QUERIES = [
        "thư viện đông không",
        "còn chỗ trống không",
        "giờ nào vắng nhất",
        "bây giờ có đông không",
        "mấy giờ nên đến thư viện",
        "hôm nay thư viện có đông không",
        "khung giờ cao điểm là khi nào",
        "tỉ lệ lấp đầy bao nhiêu",
    ]
    SEMANTIC_REALTIME_THRESHOLD = 0.72  # Cosine similarity >= 0.72 -> realtime query
    _realtime_embeddings = None  # Cache, compute once

    # Negative patterns: câu hỏi KHÔNG phải realtime dù có từ gần nghĩa
    REALTIME_NEGATIVE_PATTERNS = [
        r'mở cửa.*(mấy|lúc|giờ)',   # hỏi giờ mở cửa -> KB
        r'(mấy|lúc|giờ).*mở cửa',
        r'đóng cửa.*(mấy|lúc|giờ)',  # hỏi giờ đóng cửa -> KB
        r'(mấy|lúc|giờ).*đóng cửa',
        r'quy định',                   # hỏi quy định -> KB
        r'(cách|hướng dẫn)\s*(đặt|đăng|check)',  # hướng dẫn sử dụng -> KB
    ]

    def _get_realtime_embeddings(self):
        """Lazy-load embeddings for reference realtime queries (computed once)"""
        if self._realtime_embeddings is None:
            try:
                self._realtime_embeddings = self.embedding_service.embed_texts(
                    self.REALTIME_REFERENCE_QUERIES
                )
                logger.info(f"[Realtime] Cached {len(self._realtime_embeddings)} reference embeddings")
            except Exception as e:
                logger.error(f"[Realtime] Error computing reference embeddings: {e}")
                self._realtime_embeddings = []
        return self._realtime_embeddings

    def _cosine_similarity(self, a: list, b: list) -> float:
        """Compute cosine similarity between two vectors"""
        dot = sum(x * y for x, y in zip(a, b))
        norm_a = sum(x * x for x in a) ** 0.5
        norm_b = sum(x * x for x in b) ** 0.5
        if norm_a == 0 or norm_b == 0:
            return 0.0
        return dot / (norm_a * norm_b)

    def _detect_realtime_query(self, message: str) -> bool:
        """
        Check if message is asking about real-time statistics.
        Uses 2-layer detection:
          1. Regex patterns (fast, covers common phrasings)
          2. Semantic similarity to reference queries (catches novel phrasings)
        Negative patterns exclude known non-realtime queries.
        """
        normalized = message.lower().strip()

        # Layer 0: Negative patterns (exclude known non-realtime)
        for neg_pattern in self.REALTIME_NEGATIVE_PATTERNS:
            if re.search(neg_pattern, normalized):
                logger.info(f"[Realtime] Negative match: '{neg_pattern}' -> NOT realtime")
                return False

        # Layer 1: Regex patterns
        for pattern in self.REALTIME_REGEX_PATTERNS:
            if re.search(pattern, normalized):
                logger.info(f"[Realtime] Regex match: '{pattern}' in '{message}'")
                return True

        # Layer 2: Semantic similarity (only if regex missed)
        try:
            ref_embeddings = self._get_realtime_embeddings()
            if ref_embeddings:
                query_embedding = self.embedding_service.embed_text(message)
                best_sim = 0.0
                best_ref = ""
                for i, ref_emb in enumerate(ref_embeddings):
                    sim = self._cosine_similarity(query_embedding, ref_emb)
                    if sim > best_sim:
                        best_sim = sim
                        best_ref = self.REALTIME_REFERENCE_QUERIES[i]

                if best_sim >= self.SEMANTIC_REALTIME_THRESHOLD:
                    logger.info(f"[Realtime] Semantic match: sim={best_sim:.3f} ref='{best_ref}' for '{message}'")
                    return True
                else:
                    logger.debug(f"[Realtime] Semantic no match: best_sim={best_sim:.3f} for '{message}'")
        except Exception as e:
            logger.warning(f"[Realtime] Semantic check failed: {e}")

        return False

    def _fetch_live_context(self) -> str:
        """Fetch real-time data from analytics endpoints and format as context"""
        live_parts = []

        try:
            from app.core.database import engine
            from sqlalchemy import text

            now = datetime.now()

            with engine.connect() as conn:
                # 1. Realtime capacity
                seats_result = conn.execute(text("SELECT COUNT(*) FROM seats WHERE is_active = true"))
                total_seats = seats_result.fetchone()[0] or 0

                active_result = conn.execute(text("""
                    SELECT COUNT(*) FROM reservations
                    WHERE status IN ('CONFIRMED', 'BOOKED')
                      AND start_time <= :now AND end_time >= :now
                """), {"now": now})
                active_bookings = active_result.fetchone()[0] or 0

                occupancy_rate = (active_bookings / total_seats * 100) if total_seats > 0 else 0
                available_seats = total_seats - active_bookings

                if occupancy_rate >= 90:
                    status = "rất đông, gần kín chỗ"
                elif occupancy_rate >= 70:
                    status = "khá đông"
                elif occupancy_rate >= 50:
                    status = "bình thường"
                else:
                    status = "vắng, còn nhiều chỗ trống"

                live_parts.append(
                    f"THỐNG KÊ THỜI GIAN THỰC (cập nhật lúc {now.strftime('%H:%M %d/%m/%Y')}):\n"
                    f"- Tổng ghế: {total_seats}, đang sử dụng: {active_bookings}, còn trống: {available_seats}\n"
                    f"- Tỉ lệ lấp đầy: {occupancy_rate:.0f}%\n"
                    f"- Trạng thái hiện tại: {status}"
                )

                # 2. Zone breakdown
                zones_result = conn.execute(text("""
                    SELECT
                        z.zone_name,
                        COUNT(s.seat_id) as total_seats,
                        COALESCE((
                            SELECT COUNT(*) FROM reservations r
                            JOIN seats s2 ON r.seat_id = s2.seat_id
                            WHERE s2.zone_id = z.zone_id
                              AND r.status IN ('CONFIRMED', 'BOOKED')
                              AND r.start_time <= :now AND r.end_time >= :now
                        ), 0) as occupied
                    FROM zones z
                    LEFT JOIN seats s ON s.zone_id = z.zone_id AND s.is_active = true
                    GROUP BY z.zone_id, z.zone_name
                    ORDER BY z.zone_id
                """), {"now": now})

                zone_info = []
                for row in zones_result:
                    zone_name, zone_total, zone_occupied = row
                    zone_free = zone_total - zone_occupied
                    zone_occ = (zone_occupied / zone_total * 100) if zone_total > 0 else 0
                    zone_info.append(f"  + {zone_name}: {zone_free}/{zone_total} ghế trống ({zone_occ:.0f}% lấp đầy)")

                if zone_info:
                    live_parts.append("CHI TIẾT THEO KHU VỰC:\n" + "\n".join(zone_info))

                # 3. Hourly pattern (last 14 days average)
                hourly_result = conn.execute(text("""
                    SELECT
                        EXTRACT(HOUR FROM check_in_time) as hour,
                        COUNT(*) as count,
                        COUNT(DISTINCT DATE(check_in_time)) as num_days
                    FROM access_logs
                    WHERE check_in_time >= NOW() - INTERVAL '14 days'
                    GROUP BY EXTRACT(HOUR FROM check_in_time)
                    ORDER BY hour
                """))

                hourly_data = []
                peak_hour = None
                peak_avg = 0
                quiet_hour = None
                quiet_avg = float('inf')

                for row in hourly_result:
                    hour = int(row[0])
                    num_days = row[2] or 1
                    avg = row[1] / num_days
                    hourly_data.append((hour, avg))
                    if avg > peak_avg:
                        peak_avg = avg
                        peak_hour = hour
                    if avg < quiet_avg and 7 <= hour <= 20:
                        quiet_avg = avg
                        quiet_hour = hour

                if hourly_data:
                    hour_lines = []
                    for hour, avg in hourly_data:
                        if 7 <= hour <= 21:
                            level = "rất đông" if avg > peak_avg * 0.8 else "đông" if avg > peak_avg * 0.5 else "vắng"
                            hour_lines.append(f"  + {hour}:00 - {hour+1}:00: trung bình {avg:.0f} người ({level})")

                    live_parts.append(
                        f"MẬT ĐỘ TRUNG BÌNH THEO GIỜ (14 ngày qua):\n"
                        + "\n".join(hour_lines)
                        + f"\n- Giờ cao điểm nhất: {peak_hour}:00 (~{peak_avg:.0f} người)"
                        + f"\n- Giờ vắng nhất: {quiet_hour}:00 (~{quiet_avg:.0f} người)"
                    )

                # 4. Upcoming 1h bookings
                from datetime import timedelta
                next_hour = now + timedelta(hours=1)
                upcoming_result = conn.execute(text("""
                    SELECT COUNT(*) FROM reservations
                    WHERE status IN ('CONFIRMED', 'BOOKED')
                      AND start_time BETWEEN :now AND :next_hour
                """), {"now": now, "next_hour": next_hour})
                upcoming = upcoming_result.fetchone()[0] or 0

                live_parts.append(f"DỰ BÁO:\n- Trong 1 giờ tới có thêm {upcoming} lượt đặt chỗ sắp bắt đầu")

        except Exception as e:
            logger.error(f"[Realtime] Error fetching live data: {e}")
            live_parts.append("(Không thể lấy dữ liệu thống kê thời gian thực lúc này)")

        return "\n\n".join(live_parts)

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
    
    def generate_response(self, query: str, context_chunks: List[Dict[str, Any]], live_data: str = "", chat_history: List[Dict[str, str]] = None) -> str:
        """Generate response using LLM with context, optional live data, and chat history"""
        try:
            context_parts = []
            for chunk in context_chunks:
                source = chunk.get("source", "Tài liệu thư viện")
                content = chunk.get("content", "")
                context_parts.append(f"--- Nguồn: {source} ---\n{content}")

            context = "\n\n".join(context_parts)

            if not context:
                context = "Empty context."

            # Format live_data section
            live_data_section = ""
            if live_data:
                live_data_section = f"--- DỮ LIỆU THỜI GIAN THỰC ---\n{live_data}\n--- HẾT DỮ LIỆU THỜI GIAN THỰC ---\n\nHãy sử dụng dữ liệu thời gian thực ở trên để trả lời câu hỏi về tình trạng thư viện hiện tại."

            # Format chat history section
            history_section = ""
            if chat_history:
                history_lines = []
                for msg in chat_history[-6:]:  # Last 3 pairs max
                    role = "Sinh viên" if msg["role"] == "user" else "SLIB AI"
                    history_lines.append(f"{role}: {msg['content']}")
                history_section = "--- LỊCH SỬ HỘI THOẠI GẦN ĐÂY ---\n" + "\n".join(history_lines) + "\n--- HẾT LỊCH SỬ ---\n\nHãy dựa vào lịch sử hội thoại để hiểu ngữ cảnh câu hỏi hiện tại. Nếu câu hỏi là follow-up (ví dụ: 'ngày mai thì sao', 'còn buổi chiều?'), hãy trả lời dựa trên chủ đề trước đó."

            prompt = self.prompt_template.format(
                context=context,
                question=query,
                live_data=live_data_section,
                chat_history=history_section
            )

            logger.info(f"[Gen] Invoking LLM... (live_data={'yes' if live_data else 'no'})")
            response = self.llm.invoke(prompt)
            return response.strip()
            
        except Exception as e:
            logger.error(f"[RAGChatService] Error generating response: {e}")
            return self.I_DO_NOT_KNOW_MARKER
    
    def query(self, message: str, chat_history: List[Dict[str, str]] = None) -> Dict[str, Any]:
        """
        Main RAG query method with MoMo-style logic
        chat_history: list of {"role": "user"|"assistant", "content": "..."}
        """
        logger.info(f"[Query] User: {message} (history: {len(chat_history) if chat_history else 0} msgs)")
        
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

        # 4. Check realtime query BEFORE low confidence check
        is_realtime = self._detect_realtime_query(message)

        # Also check if this is a follow-up to a realtime conversation
        if not is_realtime and chat_history:
            # If recent history had realtime context and this looks like a follow-up
            recent_msgs = [m["content"] for m in (chat_history or [])[-4:]]
            for prev_msg in recent_msgs:
                if self._detect_realtime_query(prev_msg):
                    is_realtime = True
                    logger.info("[Query] Follow-up to realtime conversation detected")
                    break

        live_data = ""
        if is_realtime:
            logger.info("[Query] Realtime query detected, fetching live data...")
            live_data = self._fetch_live_context()

        # 4a. Check Low Confidence (Score lửng lơ)
        if best_score < self.similarity_threshold:
            # Nếu là câu hỏi realtime hoặc có history -> vẫn generate
            if (is_realtime and live_data) or chat_history:
                logger.info(f"[Query] Low score but realtime/follow-up — using live data + history to answer")
                response = self.generate_response(message, chunks[:3], live_data=live_data, chat_history=chat_history)

                if self.I_DO_NOT_KNOW_MARKER not in response:
                    return {
                        "success": True,
                        "reply": response,
                        "action": ActionType.NONE,
                        "similarity_score": best_score,
                        "sources": [{"source": "Dữ liệu thời gian thực", "score": 1.0}]
                    }

            # Không phải realtime hoặc LLM trả IDK -> escalate
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
        response = self.generate_response(message, relevant_chunks, live_data=live_data, chat_history=chat_history)

        # 6. Check I_DO_NOT_KNOW (LLM fallback)
        if self.I_DO_NOT_KNOW_MARKER in response:
            logger.info("[Query] LLM returned I_DO_NOT_KNOW.")
            return {
                "success": True,
                "reply": MSG_NO_INFO,
                "action": ActionType.ESCALATE_TO_LIBRARIAN,
                "similarity_score": best_score,
                "sources": []
            }

        # 7. Success
        sources = [
            {"source": c["source"], "score": round(c["similarity_score"], 4)}
            for c in relevant_chunks[:3]
        ]
        if is_realtime:
            sources.append({"source": "Dữ liệu thời gian thực", "score": 1.0})

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
            
        # 4. Check realtime query BEFORE low confidence
        is_realtime = self._detect_realtime_query(message)
        live_data = ""
        if is_realtime:
            live_data = self._fetch_live_context()
            debug["generation"]["is_realtime_query"] = True

        # 4a. Check Low Confidence
        if best_score < self.similarity_threshold:
            # If realtime query -> try with live data
            if is_realtime and live_data:
                debug["generation"]["used_llm"] = True
                debug["generation"]["action_reason"] = "Low score but realtime query — answered with live data"
                response = self.generate_response(message, chunks[:3], live_data=live_data)
                if self.I_DO_NOT_KNOW_MARKER not in response:
                    return {
                        "success": True,
                        "reply": response,
                        "action": ActionType.NONE,
                        "debug": debug
                    }

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
        debug["generation"]["used_chunks"] = [
            {
                "rank": chunks.index(c) + 1,
                "score": round(c.get("similarity_score", 0), 4),
                "source": c.get("source", "Unknown")
            }
            for c in relevant_chunks
        ]

        response = self.generate_response(message, relevant_chunks, live_data=live_data)

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
