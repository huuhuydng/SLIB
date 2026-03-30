"""
Admin authentication helpers for protected AI service routes.
"""

from __future__ import annotations

import base64
import hashlib
import hmac
import json
import time
from typing import Any, Dict

from fastapi import Depends, Header, HTTPException, status

from app.config.settings import get_settings


def _decode_base64url(value: str) -> bytes:
    padding = "=" * (-len(value) % 4)
    return base64.urlsafe_b64decode(value + padding)


def _verify_hs256_jwt(token: str, secret: str) -> Dict[str, Any]:
    try:
        header_segment, payload_segment, signature_segment = token.split(".")
    except ValueError as exc:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Token không hợp lệ") from exc

    signing_input = f"{header_segment}.{payload_segment}".encode("utf-8")
    expected_signature = hmac.new(
        secret.encode("utf-8"),
        signing_input,
        hashlib.sha256,
    ).digest()

    actual_signature = _decode_base64url(signature_segment)
    if not hmac.compare_digest(expected_signature, actual_signature):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Token không hợp lệ")

    try:
        header = json.loads(_decode_base64url(header_segment))
        payload = json.loads(_decode_base64url(payload_segment))
    except (json.JSONDecodeError, ValueError) as exc:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Token không hợp lệ") from exc

    if header.get("alg") != "HS256":
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Thuật toán token không được hỗ trợ")

    exp = payload.get("exp")
    if exp is None or float(exp) <= time.time():
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Token đã hết hạn")

    return payload


def require_admin_access(
    authorization: str | None = Header(default=None),
    x_internal_api_key: str | None = Header(default=None),
):
    settings = get_settings()

    if settings.internal_api_key and x_internal_api_key == settings.internal_api_key:
        return {"auth_type": "internal"}

    if authorization is None or not authorization.startswith("Bearer "):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Thiếu thông tin xác thực")

    if not settings.jwt_secret:
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail="JWT secret chưa được cấu hình")

    token = authorization[7:].strip()
    payload = _verify_hs256_jwt(token, settings.jwt_secret)

    if payload.get("type") != "access":
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Loại token không hợp lệ")

    role = payload.get("role")
    if role not in {"ADMIN", "LIBRARIAN"}:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Bạn không có quyền truy cập")

    return payload


AdminAccess = Depends(require_admin_access)
