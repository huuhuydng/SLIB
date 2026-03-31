"""
RAG Chat Service for SLIB AI.
Handles knowledge retrieval, deterministic answers for key library settings,
and real-time statistics queries.
"""

import logging
import os
import re
import unicodedata
import hashlib
import httpx
from typing import List, Dict, Any, Optional, Tuple
from datetime import datetime
from urllib.parse import urlparse, urlunparse

from langchain_community.llms import Ollama
from langchain_core.prompts import PromptTemplate

# Import các module từ project của bạn
from app.config.settings import get_settings
from app.services.embedding_service import get_embedding_service
from app.models.schemas import ActionType
from app.services.qdrant_service import get_qdrant_service
from app.services.java_backend_client import get_java_client

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


# --- 1. CẤU HÌNH NGƯỠNG (THRESHOLDS) ---
# Điểm < 0.35: Coi là spam/gõ bậy -> Giới thiệu lại bản thân (Giống MoMo case "hdjdhdiirhfh")
SPAM_THRESHOLD = 0.35 
STRICT_GROUNDED_SCORE_THRESHOLD = 0.62

# Điểm >= 0.75 (Lấy từ settings): Mới được coi là tìm thấy thông tin
# Khoảng giữa (0.35 - 0.75): Coi là câu hỏi khó/không có data -> Xin lỗi nhẹ nhàng


# --- 2. SYSTEM PROMPT ---
RAG_SYSTEM_PROMPT = """Bạn là SLIB AI, trợ lý hỗ trợ sinh viên của thư viện SLIB.

--- NHIỆM VỤ ---
Trả lời câu hỏi của sinh viên dựa trên thông tin được cung cấp trong dữ liệu bên dưới.

--- QUY TẮC AN TOÀN (BẮT BUỘC) ---
1. Đọc kỹ toàn bộ dữ liệu được cung cấp.
2. Chỉ được khẳng định khi dữ liệu có căn cứ rõ ràng, trực tiếp.
3. Nếu thông tin KHÔNG có trong dữ liệu, dữ liệu không đủ chắc chắn, hoặc có nhiều khả năng hiểu sai, bạn phải trả lời duy nhất cụm từ: I_DO_NOT_KNOW
4. Tuyệt đối KHÔNG tự bịa ra thông tin, KHÔNG suy đoán, KHÔNG dùng kiến thức bên ngoài dữ liệu được cung cấp.
5. Không tự hợp nhất nhiều dữ liệu rời rạc thành một kết luận mới nếu dữ liệu không nói rõ như vậy.

--- PHONG CÁCH TRẢ LỜI ---
- Trả lời bằng tiếng Việt, lịch sự, rõ ràng, chuyên nghiệp.
- Ưu tiên câu ngắn, trực tiếp vào thông tin người dùng hỏi.
- Không nhắc đến các cụm như "theo thông tin trong context", "dựa trên context", "trong dữ liệu được cung cấp" trừ khi người dùng hỏi về nguồn.
- Không dùng giọng điệu quá trẻ con, quá đáng yêu hoặc thêm biểu tượng cảm xúc.
- Nếu dữ liệu có giờ giấc hoặc con số cụ thể, hãy nêu trực tiếp con số đó.
- Nếu dữ liệu có các mốc thời gian cụ thể như 07:00 hoặc 21:00, phải lặp lại đúng các mốc đó, không được tự đổi sang giờ khác.
- Nếu không chắc, không vòng vo. Chỉ trả lời: I_DO_NOT_KNOW

--- DỮ LIỆU THƯ VIỆN ---
{context}
--- HẾT DỮ LIỆU ---

{live_data}

{chat_history}

Câu hỏi: {question}

Câu trả lời của bạn:"""


# --- 3. CÁC MẪU CÂU TRẢ LỜI CỐ ĐỊNH ---

# A. Greeting Patterns (Xã giao)
GREETING_PATTERNS = {
    # Identity & Intro
    "bạn là ai": "Xin chào, tôi là **SLIB AI**, trợ lý hỗ trợ sinh viên tại thư viện SLIB. Tôi có thể giúp bạn tra cứu thông tin, hướng dẫn sử dụng và giải đáp các câu hỏi thường gặp.",
    "bạn là gì": "Tôi là **SLIB AI**, trợ lý hỗ trợ thông tin của thư viện SLIB.",
    "ai đang nói": "Tôi là **SLIB AI**, trợ lý hỗ trợ của thư viện SLIB.",
    "giới thiệu": "Xin chào, tôi là **SLIB AI**. Tôi có thể hỗ trợ bạn về giờ hoạt động, đặt chỗ, check-in, điểm uy tín và các quy định của thư viện.",
    
    # Greetings
    "xin chào": "Xin chào, tôi là SLIB AI. Bạn cần tôi hỗ trợ thông tin gì?",
    "chào bạn": "Xin chào, tôi có thể hỗ trợ bạn thông tin gì về thư viện?",
    "hello": "Xin chào, tôi là SLIB AI. Bạn cần hỗ trợ gì?",
    "hi": "Xin chào, tôi là SLIB AI. Bạn cần hỗ trợ gì?",
    
    # Thanks
    "cảm ơn": "Rất vui được hỗ trợ bạn. Nếu cần thêm thông tin, bạn cứ tiếp tục đặt câu hỏi.",
    "cám ơn": "Rất vui được hỗ trợ bạn. Nếu cần thêm thông tin, bạn cứ tiếp tục đặt câu hỏi.",
    "thanks": "Rất vui được hỗ trợ bạn. Nếu cần thêm thông tin, bạn cứ tiếp tục đặt câu hỏi.",
    
    # Help
    "giúp tôi": "Tôi có thể hỗ trợ bạn về giờ hoạt động, đặt chỗ, check-in, điểm uy tín hoặc quy định thư viện.",
    "hỗ trợ": "Tôi có thể hỗ trợ bạn về giờ hoạt động, đặt chỗ, check-in, điểm uy tín hoặc quy định thư viện.",
}

# B. Fallback Messages (Xử lý lỗi)

# Case 1: Spam/Gõ bậy (Score < SPAM_THRESHOLD)
# Giống MoMo đoạn "hdjdhdiirhfh" -> Giới thiệu lại bản thân để định hướng user
MSG_SPAM_DETECTED = "Tôi là **SLIB AI**. Tôi có thể hỗ trợ bạn về giờ hoạt động, đặt chỗ, check-in, điểm uy tín và các quy định của thư viện. Bạn hãy gửi lại câu hỏi rõ hơn nhé."

# Case 2: Không tìm thấy tin (SPAM_THRESHOLD <= Score < Similarity_Threshold)
# Giống MoMo đoạn "Slib là gì" -> Xin lỗi vì chưa có data
MSG_NO_INFO_VARIANTS = [
    "Hiện tôi chưa đủ căn cứ để trả lời chắc chắn câu hỏi này. Để tránh cung cấp sai thông tin, bạn có thể chọn Chat với Thủ thư SLIB để được hỗ trợ chính xác hơn.",
    "Tôi chưa có đủ cơ sở để trả lời câu hỏi này một cách chính xác. Bạn có thể chọn Chat với Thủ thư SLIB để được hỗ trợ đúng nghiệp vụ hơn.",
    "Thông tin hiện có chưa đủ rõ để tôi trả lời một cách chắc chắn. Nếu cần, bạn có thể chọn Chat với Thủ thư SLIB để được xác nhận chính xác hơn.",
]

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

MSG_ESCALATION_OFFER = "Bạn có thể bấm nút bên dưới để kết nối với thủ thư."

GROUNDING_STOPWORDS = {
    "thu", "vien", "slib", "toi", "ban", "em", "anh", "chi", "va", "la",
    "co", "cua", "cho", "voi", "mot", "nhung", "nay", "kia", "neu", "thi",
    "de", "duoc", "can", "hay", "giup", "ho", "tro", "thong", "tin", "gi",
    "nao", "mau", "the", "trong", "ngoai", "tren", "duoi", "toi", "den",
    "tu", "luc", "gio", "rang", "rang", "ro", "chinh", "xac", "hien", "tai",
}

BOILERPLATE_SENTENCE_PATTERNS = [
    r"ban co the chon chat voi thu thu slib",
    r"ban co the lien he thu thu",
    r"neu can ho tro them",
    r"de tranh cung cap sai thong tin",
]



class RAGChatService:
    """
    RAG Chat Service
    Workflow:
    1. Check Greeting (Xã giao)
    2. Retrieve context from document knowledge base first
    3. Fall back to other indexed sources if needed
    4. Check spam / low confidence
    5. Generate response with LLM when enough evidence exists
    """
    
    I_DO_NOT_KNOW_MARKER = "I_DO_NOT_KNOW"
    HOURS_QUERY_PATTERNS = [
        r"(giờ|gio).*(mở cửa|đóng cửa)",
        r"(mở cửa|đóng cửa).*(giờ|gio|lúc|luc|mấy|may)",
        r"mở.*(đến mấy giờ|tới mấy giờ|den may gio|toi may gio)",
        r"(mấy giờ|may gio).*(mở|đóng)",
        r"(giờ|gio).*(hoạt động|lam viec|làm việc)",
        r"thư viện.*(mở cửa|đóng cửa|giờ hoạt động|giờ làm việc)",
        r"(chủ nhật|chu nhat).*(mở|đóng|hoạt động|lam viec|làm việc)",
        r"(ngày nào|ngay nao).*(mở cửa|hoạt động|lam viec|làm việc)",
    ]
    
    def __init__(self):
        settings = get_settings()
        self.settings = settings
        self.java_client = get_java_client()
        
        self.similarity_threshold = settings.similarity_threshold
        self.max_chunks = settings.max_retrieved_chunks
        self.ollama_url = settings.ollama_url
        self.model = settings.ollama_model
        self.temperature = min(settings.default_temperature, 0.15)
        self.max_tokens = settings.default_max_tokens
        
        self.embedding_service = get_embedding_service()
        
        # Initialize LangChain Ollama LLM
        self.llm = self._build_llm()
        
        # Prompt template
        self.prompt_template = PromptTemplate(
            template=RAG_SYSTEM_PROMPT,
            input_variables=["context", "question", "live_data", "chat_history"]
        )
        
        logger.info(
            f"[RAGChatService] Initialized. Model={self.model}, "
            f"Threshold={self.similarity_threshold}, SpamThreshold={SPAM_THRESHOLD}"
        )

    def _build_llm(self) -> Ollama:
        return Ollama(
            model=self.model,
            base_url=self.ollama_url,
            temperature=self.temperature,
            num_predict=self.max_tokens
        )

    def _refresh_runtime_config(self) -> None:
        """Sync runtime model/url with the AI config managed in the Java backend."""
        try:
            config = self.java_client.get_ai_config(force_refresh=True)
        except Exception as exc:
            logger.warning(f"[RAGChatService] Cannot refresh AI config from backend: {exc}")
            return

        next_url = self._normalize_ollama_url(config.get("ollamaUrl") or self.settings.ollama_url)
        next_model = config.get("ollamaModel") or self.settings.ollama_model
        next_temperature = min(config.get("temperature", self.settings.default_temperature), 0.15)
        next_max_tokens = config.get("maxTokens", self.settings.default_max_tokens)

        if (
            next_url == self.ollama_url
            and next_model == self.model
            and next_temperature == self.temperature
            and next_max_tokens == self.max_tokens
        ):
            return

        self.ollama_url = next_url
        self.model = next_model
        self.temperature = next_temperature
        self.max_tokens = next_max_tokens
        self.llm = self._build_llm()

        logger.info(
            "[RAGChatService] Refreshed runtime config: model=%s url=%s",
            self.model,
            self.ollama_url,
        )

    def _normalize_ollama_url(self, raw_url: str) -> str:
        normalized = (raw_url or self.settings.ollama_url).strip()
        if not normalized:
            return self.settings.ollama_url

        parsed = urlparse(normalized)
        if not os.path.exists("/.dockerenv"):
            return normalized

        if parsed.hostname not in {"localhost", "127.0.0.1"}:
            return normalized

        env_url = (self.settings.ollama_url or "").strip()
        env_parsed = urlparse(env_url) if env_url else None
        if env_parsed and env_parsed.hostname not in {"", None, "localhost", "127.0.0.1"}:
            return env_url

        port = parsed.port or 11434
        replacement_netloc = f"host.docker.internal:{port}"
        return urlunparse(parsed._replace(netloc=replacement_netloc))

    def _model_matches(self, available_model: str) -> bool:
        requested = (self.model or "").strip()
        available = (available_model or "").strip()
        if not requested or not available:
            return False

        requested_base = requested.split(":")[0]
        available_base = available.split(":")[0]

        return (
            available == requested
            or available == f"{requested}:latest"
            or available.startswith(f"{requested}:")
            or requested_base == available_base
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

    def _build_no_info_reply(self, query: str) -> str:
        normalized = self._normalize_for_match(query)
        if not normalized:
            return MSG_NO_INFO_VARIANTS[0]

        digest = hashlib.md5(normalized.encode("utf-8")).digest()
        variant_index = int.from_bytes(digest[:2], "big") % len(MSG_NO_INFO_VARIANTS)
        return MSG_NO_INFO_VARIANTS[variant_index]

    def _strip_accents(self, text: str) -> str:
        normalized = unicodedata.normalize("NFD", text or "")
        return "".join(char for char in normalized if unicodedata.category(char) != "Mn")

    def _normalize_for_match(self, text: str) -> str:
        lowered = self._strip_accents((text or "").lower())
        lowered = re.sub(r"[*_`>#-]+", " ", lowered)
        lowered = re.sub(r"[^a-z0-9:%/\s]", " ", lowered)
        return re.sub(r"\s+", " ", lowered).strip()

    def _clean_response_tone(self, response: str) -> str:
        cleaned = response or ""
        cleanup_patterns = [
            r"\b[Tt]heo thông tin (?:trong|trên) [Cc]ontext[:,]?\s*",
            r"\b[Tt]heo thông tin (?:trong|trên) context[:,]?\s*",
            r"\b[Dd]ựa trên [Cc]ontext[:,]?\s*",
            r"\b[Dd]ựa trên context[:,]?\s*",
            r"\b[Tt]rong [Cc]ontext[:,]?\s*",
            r"\b[Tt]rong context[:,]?\s*",
            r"\b[Tt]heo [Cc]ontext[:,]?\s*",
            r"\b[Tt]heo context[:,]?\s*",
            r"\b[Dd]ựa trên dữ liệu được cung cấp[:,]?\s*",
        ]

        for pattern in cleanup_patterns:
            cleaned = re.sub(pattern, "", cleaned, flags=re.IGNORECASE)

        cleaned = re.sub(r"\s+", " ", cleaned).strip(" .,:;")
        if not cleaned:
            return ""

        return cleaned[:1].upper() + cleaned[1:]

    def _extract_fact_tokens(self, text: str) -> List[str]:
        normalized = self._normalize_for_match(text)
        patterns = [
            r"\b\d{1,2}:\d{2}\b",
            r"\b\d+\s*(?:%|gio|phut|ngay|tuan|thang|nam|diem|ghe|cho|nguoi|luot)\b",
            r"\bthu\s*(?:hai|ba|tu|nam|sau|bay)\b",
            r"\bchu nhat\b",
        ]

        tokens = []
        for pattern in patterns:
            tokens.extend(re.findall(pattern, normalized, flags=re.IGNORECASE))
        return list(dict.fromkeys(token.strip() for token in tokens if token.strip()))

    def _extract_keywords(self, text: str) -> List[str]:
        normalized = self._normalize_for_match(text)
        words = []
        for token in normalized.split():
            if len(token) < 4:
                continue
            if token in GROUNDING_STOPWORDS:
                continue
            if token.isdigit():
                continue
            words.append(token)
        return list(dict.fromkeys(words))

    def _is_boilerplate_sentence(self, sentence: str) -> bool:
        normalized = self._normalize_for_match(sentence)
        return any(re.search(pattern, normalized) for pattern in BOILERPLATE_SENTENCE_PATTERNS)

    def _verify_response_grounding(
        self,
        query: str,
        response: str,
        context_chunks: List[Dict[str, Any]],
        live_data: str = "",
    ) -> Tuple[bool, str]:
        evidence_parts = [chunk.get("content", "") for chunk in context_chunks if chunk.get("content")]
        if live_data:
            evidence_parts.append(live_data)

        evidence_text = self._normalize_for_match("\n".join(evidence_parts))
        normalized_response = self._normalize_for_match(response)
        if not evidence_text or not normalized_response:
            return False, "Không có đủ evidence để kiểm chứng câu trả lời"

        response_fact_tokens = self._extract_fact_tokens(response)
        missing_fact_tokens = [token for token in response_fact_tokens if token not in evidence_text]
        if missing_fact_tokens:
            return False, f"Câu trả lời chứa chi tiết không có trong evidence: {', '.join(missing_fact_tokens[:3])}"

        sentences = [s.strip() for s in re.split(r"[.!?\n]+", response) if s.strip()]
        for sentence in sentences:
            if self._is_boilerplate_sentence(sentence):
                continue

            fact_tokens = self._extract_fact_tokens(sentence)
            if fact_tokens:
                continue

            keywords = self._extract_keywords(sentence)
            if not keywords:
                continue

            matched_keywords = [keyword for keyword in keywords if keyword in evidence_text]
            min_required = 2 if len(keywords) >= 3 else 1
            if len(matched_keywords) < min_required:
                return False, f"Câu trả lời chưa bám đủ evidence cho ý: '{sentence}'"

        return True, "Grounded"

    def _is_library_hours_query(self, message: str) -> bool:
        normalized = message.lower().strip()
        return any(re.search(pattern, normalized) for pattern in self.HOURS_QUERY_PATTERNS)

    def _rerank_chunks(self, query: str, chunks: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        if not chunks:
            return chunks

        normalized_query = query.lower().strip()
        is_hours_query = self._is_library_hours_query(query)

        def rank(chunk: Dict[str, Any]) -> tuple[float, float]:
            content = chunk.get("content", "").lower()
            source = chunk.get("source", "").lower()
            bonus = 0.0

            if is_hours_query:
                if "mở cửa" in content or "đóng cửa" in content or "giờ hoạt động" in content:
                    bonus += 0.25
                if re.search(r"\b\d{1,2}:\d{2}\b", content):
                    bonus += 0.20
                if "thứ hai" in content or "chủ nhật" in content:
                    bonus += 0.10
                if "01_gioi_thieu_thu_vien" in source or "08_cau_hoi_thuong_gap" in source:
                    bonus += 0.15

            keyword_hits = 0
            for token in normalized_query.split():
                if len(token) < 3:
                    continue
                if token in content:
                    keyword_hits += 1
            bonus += min(keyword_hits * 0.03, 0.15)

            return (chunk.get("similarity_score", 0.0) + bonus, chunk.get("similarity_score", 0.0))

        return sorted(chunks, key=rank, reverse=True)

    def _try_answer_hours_from_chunks(self, query: str, chunks: List[Dict[str, Any]]) -> Optional[str]:
        if not self._is_library_hours_query(query) or not chunks:
            return None

        combined_text = "\n".join(chunk.get("content", "") for chunk in chunks[:5])
        normalized_query = query.lower().strip()

        open_match = re.search(r"giờ mở cửa:\s*(\d{1,2}:\d{2})", combined_text, re.IGNORECASE)
        close_match = re.search(r"giờ đóng cửa:\s*(\d{1,2}:\d{2})", combined_text, re.IGNORECASE)
        monday_to_saturday = bool(re.search(r"thứ hai đến thứ bảy", combined_text, re.IGNORECASE))
        sunday_closed = bool(re.search(r"chủ nhật:\s*đóng cửa", combined_text, re.IGNORECASE))

        asks_sunday = "chủ nhật" in normalized_query or "chu nhat" in normalized_query
        asks_close_time = bool(re.search(r"(đóng cửa|đến mấy giờ|tới mấy giờ|may gio dong|dong cua)", normalized_query))
        asks_open_time = bool(re.search(r"(mở cửa|từ mấy giờ|may gio mo|mo cua)", normalized_query))

        if asks_sunday and sunday_closed:
            response = "Thư viện không mở cửa vào Chủ Nhật."
            if open_match and close_match and monday_to_saturday:
                response += f" Thư viện hoạt động từ Thứ Hai đến Thứ Bảy, từ {open_match.group(1)} đến {close_match.group(1)}."
            return response

        if asks_close_time and close_match:
            response = f"Thư viện mở cửa đến {close_match.group(1)}."
            if monday_to_saturday:
                response += " Thư viện hoạt động từ Thứ Hai đến Thứ Bảy."
            return response

        if asks_open_time and open_match:
            response = f"Thư viện mở cửa từ {open_match.group(1)}."
            if monday_to_saturday:
                response += " Thư viện hoạt động từ Thứ Hai đến Thứ Bảy."
            return response

        if open_match and close_match:
            response = f"Thư viện hoạt động từ {open_match.group(1)} đến {close_match.group(1)}."
            if monday_to_saturday:
                response += " Thư viện hoạt động từ Thứ Hai đến Thứ Bảy."
            if sunday_closed:
                response += " Chủ Nhật thư viện đóng cửa."
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
        r'đặt\s*(tối đa|bao nhiêu|mấy)',
        r'bao nhiêu\s*lượt',
        r'(điểm uy tín|uy tín|uy tin)',
        r'(bị trừ|trừ|cộng)\s*bao nhiêu\s*điểm',
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
    
    def retrieve_context(
        self,
        query: str,
        top_k: int = None,
        category: Optional[str] = None
    ) -> Tuple[List[Dict[str, Any]], float]:
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
                category=category,
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
            
            chunks = self._rerank_chunks(query, chunks)

            logger.info(f"[Retrieve] Found {len(chunks)} chunks. Best score: {best_score:.4f}")
            return chunks, best_score
                
        except Exception as e:
            logger.error(f"[RAGChatService] Error retrieving context: {e}")
            return [], 0.0
    
    def generate_response(self, query: str, context_chunks: List[Dict[str, Any]], live_data: str = "", chat_history: List[Dict[str, str]] = None) -> str:
        """Generate response using LLM with context, optional live data, and chat history"""
        try:
            self._refresh_runtime_config()
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
        Main RAG query method
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

        preferred_top_k = max(self.max_chunks, 12) if self._is_library_hours_query(message) else self.max_chunks

        # 2. Retrieve Context from document knowledge base first
        chunks, best_score = self.retrieve_context(
            message,
            top_k=preferred_top_k,
            category="knowledge_base"
        )

        # 3. Fall back to all indexed sources only when document KB is too weak
        if best_score < self.similarity_threshold:
            fallback_chunks, fallback_score = self.retrieve_context(
                message,
                top_k=preferred_top_k
            )
            if fallback_score > best_score:
                chunks, best_score = fallback_chunks, fallback_score
        
        # 4. Check Spam/Gibberish (Score cực thấp)
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

        grounded_hours_response = self._try_answer_hours_from_chunks(message, chunks)
        if grounded_hours_response:
            return {
                "success": True,
                "reply": grounded_hours_response,
                "action": ActionType.NONE,
                "similarity_score": best_score,
                "sources": [
                    {"source": c["source"], "score": round(c["similarity_score"], 4)}
                    for c in chunks[:3]
                ]
            }

        # 5. Check realtime query BEFORE low confidence check
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

        # 5a. Check Low Confidence (Score lửng lơ)
        if best_score < self.similarity_threshold:
            # Nếu là câu hỏi realtime hoặc có history -> vẫn generate
            if (is_realtime and live_data) or chat_history:
                logger.info(f"[Query] Low score but realtime/follow-up — using live data + history to answer")
                response = self.generate_response(message, chunks[:3], live_data=live_data, chat_history=chat_history)
                response = self._clean_response_tone(response)

                if self.I_DO_NOT_KNOW_MARKER not in response:
                    is_grounded, grounding_reason = self._verify_response_grounding(message, response, chunks[:3], live_data=live_data)
                    if is_grounded:
                        return {
                            "success": True,
                            "reply": response,
                            "action": ActionType.NONE,
                            "similarity_score": best_score,
                            "sources": [{"source": "Dữ liệu thời gian thực", "score": 1.0}]
                        }

                    logger.info(f"[Query] Rejected low-confidence answer after grounding check: {grounding_reason}")
                    
                    return {
                        "success": True,
                        "reply": self._build_no_info_reply(message),
                        "action": ActionType.ESCALATE_TO_LIBRARIAN,
                        "similarity_score": best_score,
                        "sources": []
                    }

            # Không phải realtime hoặc LLM trả IDK -> escalate
            logger.info(f"[Query] Low confidence ({best_score:.4f} < {self.similarity_threshold}). Escalating.")
            return {
                "success": True,
                "reply": self._build_no_info_reply(message),
                "action": ActionType.ESCALATE_TO_LIBRARIAN,
                "similarity_score": best_score,
                "sources": []
            }

        # 6. Generate Response (Score cao)
        relevant_chunks = [c for c in chunks if c["similarity_score"] >= self.similarity_threshold]
        if self._is_library_hours_query(message):
            focused_chunks = [
                c for c in relevant_chunks
                if (
                    "mở cửa" in c.get("content", "").lower()
                    or "đóng cửa" in c.get("content", "").lower()
                    or "giờ hoạt động" in c.get("content", "").lower()
                )
            ]
            if focused_chunks:
                relevant_chunks = focused_chunks[:3]

        response = self.generate_response(message, relevant_chunks, live_data=live_data, chat_history=chat_history)
        response = self._clean_response_tone(response)

        # 7. Check I_DO_NOT_KNOW (LLM fallback)
        if self.I_DO_NOT_KNOW_MARKER in response:
            logger.info("[Query] LLM returned I_DO_NOT_KNOW.")
            return {
                "success": True,
                "reply": self._build_no_info_reply(message),
                "action": ActionType.ESCALATE_TO_LIBRARIAN,
                "similarity_score": best_score,
                "sources": []
            }

        is_grounded, grounding_reason = self._verify_response_grounding(message, response, relevant_chunks, live_data=live_data)
        if not is_grounded:
            logger.info(f"[Query] Rejected answer after grounding check: {grounding_reason}")
            return {
                "success": True,
                "reply": self._build_no_info_reply(message),
                "action": ActionType.ESCALATE_TO_LIBRARIAN,
                "similarity_score": min(best_score, STRICT_GROUNDED_SCORE_THRESHOLD),
                "sources": []
            }

        # 8. Success
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
        
        # 2. Retrieve from document knowledge base first
        preferred_top_k = max(self.max_chunks, 12) if self._is_library_hours_query(message) else self.max_chunks
        chunks, best_score = self.retrieve_context(
            message,
            top_k=preferred_top_k,
            category="knowledge_base"
        )
        debug["retrieval"]["primary_category"] = "knowledge_base"

        # 3. Fall back to all indexed sources only when document KB is too weak
        if best_score < self.similarity_threshold:
            fallback_chunks, fallback_score = self.retrieve_context(
                message,
                top_k=preferred_top_k
            )
            if fallback_score > best_score:
                chunks, best_score = fallback_chunks, fallback_score
                debug["retrieval"]["fell_back_to_all_sources"] = True

        # 4. Retrieve
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
            
        # 4. Check Spam
        if best_score < SPAM_THRESHOLD:
            debug["retrieval"]["is_spam"] = True
            debug["generation"]["action_reason"] = f"Spam detected (Score {best_score:.4f} < {SPAM_THRESHOLD})"
            return {
                "success": True,
                "reply": MSG_SPAM_DETECTED,
                "action": ActionType.NONE,
                "debug": debug
            }

        grounded_hours_response = self._try_answer_hours_from_chunks(message, chunks)
        if grounded_hours_response:
            debug["generation"]["action_reason"] = "Answered from retrieved knowledge-base chunk"
            return {
                "success": True,
                "reply": grounded_hours_response,
                "action": ActionType.NONE,
                "debug": debug
            }
            
        # 5. Check realtime query BEFORE low confidence
        is_realtime = self._detect_realtime_query(message)
        live_data = ""
        if is_realtime:
            live_data = self._fetch_live_context()
            debug["generation"]["is_realtime_query"] = True

        # 5a. Check Low Confidence
        if best_score < self.similarity_threshold:
            # If realtime query -> try with live data
            if is_realtime and live_data:
                debug["generation"]["used_llm"] = True
                debug["generation"]["action_reason"] = "Low score but realtime query — answered with live data"
                response = self.generate_response(message, chunks[:3], live_data=live_data)
                response = self._clean_response_tone(response)
                if self.I_DO_NOT_KNOW_MARKER not in response:
                    is_grounded, grounding_reason = self._verify_response_grounding(message, response, chunks[:3], live_data=live_data)
                    if is_grounded:
                        return {
                            "success": True,
                            "reply": response,
                            "action": ActionType.NONE,
                            "debug": debug
                        }

                    debug["generation"]["action_reason"] = f"Rejected after grounding check: {grounding_reason}"
                    return {
                        "success": True,
                        "reply": self._build_no_info_reply(message),
                        "action": ActionType.ESCALATE_TO_LIBRARIAN,
                        "debug": debug
                    }

            debug["generation"]["action_reason"] = f"Score {best_score:.4f} < Threshold {self.similarity_threshold}"
            return {
                "success": True,
                "reply": self._build_no_info_reply(message),
                "action": ActionType.ESCALATE_TO_LIBRARIAN,
                "debug": debug
            }

        # 6. Generate with LLM
        relevant_chunks = [c for c in chunks if c["similarity_score"] >= self.similarity_threshold]
        if self._is_library_hours_query(message):
            focused_chunks = [
                c for c in relevant_chunks
                if (
                    "mở cửa" in c.get("content", "").lower()
                    or "đóng cửa" in c.get("content", "").lower()
                    or "giờ hoạt động" in c.get("content", "").lower()
                )
            ]
            if focused_chunks:
                relevant_chunks = focused_chunks[:3]
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
        response = self._clean_response_tone(response)

        # 7. Check IDK
        if self.I_DO_NOT_KNOW_MARKER in response:
            debug["generation"]["llm_returned_idk"] = True
            debug["generation"]["action_reason"] = "LLM returned I_DO_NOT_KNOW marker"
            return {
                "success": True,
                "reply": self._build_no_info_reply(message),
                "action": ActionType.ESCALATE_TO_LIBRARIAN,
                "debug": debug
            }

        is_grounded, grounding_reason = self._verify_response_grounding(message, response, relevant_chunks, live_data=live_data)
        if not is_grounded:
            debug["generation"]["action_reason"] = f"Rejected after grounding check: {grounding_reason}"
            return {
                "success": True,
                "reply": self._build_no_info_reply(message),
                "action": ActionType.ESCALATE_TO_LIBRARIAN,
                "debug": debug
            }

        # 8. Success
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
            self._refresh_runtime_config()

            # Test embedding
            embed_result = self.embedding_service.test_connection()
            if not embed_result["success"]:
                return embed_result

            qdrant_service = get_qdrant_service()
            qdrant_service.client.get_collection(qdrant_service.collection_name)

            tags_response = httpx.get(f"{self.ollama_url}/api/tags", timeout=15.0)
            tags_response.raise_for_status()
            tags_data = tags_response.json()
            available_models = [
                model.get("name", "")
                for model in tags_data.get("models", [])
                if model.get("name")
            ]

            if available_models and not any(self._model_matches(model) for model in available_models):
                return {
                    "success": False,
                    "message": (
                        f"Không tìm thấy model đang cấu hình ({self.model}) trên Ollama. "
                        "Vui lòng kiểm tra lại cấu hình model trong AI Config."
                    ),
                    "model": self.model,
                    "available_models": available_models,
                }

            return {
                "success": True,
                "message": f"RAG ready. Model: {self.model}",
                "embedding_dims": embed_result.get("dimensions", 768),
                "model": self.model,
                "available_models": available_models,
            }
            
        except Exception as e:
            return {
                "success": False,
                "message": f"RAG service error: {str(e)}",
                "model": self.model,
            }


# Singleton instance
_rag_chat_service = None

def get_rag_chat_service() -> RAGChatService:
    """Get singleton RAGChatService instance"""
    global _rag_chat_service
    if _rag_chat_service is None:
        _rag_chat_service = RAGChatService()
    return _rag_chat_service
