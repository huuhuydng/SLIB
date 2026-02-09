#!/usr/bin/env python3
"""
Script tao anh avatar AI cho user
Su dung API de sinh anh mat sinh vien
"""

import os
import requests
from pathlib import Path

AVATARS_DIR = Path("avatars")

def create_placeholder_avatars(student_ids):
    """Tao anh placeholder cho user"""
    AVATARS_DIR.mkdir(exist_ok=True)
    
    for student_id in student_ids:
        # Su dung placeholder service
        # Ban co the thay the bang API sinh anh AI thuc te
        avatar_path = AVATARS_DIR / f"{student_id}.png"
        
        if not avatar_path.exists():
            # Tao placeholder image
            print(f"Tao avatar cho {student_id}...")
            # Placeholder: su dung UI Avatars hoac DiceBear
            url = f"https://api.dicebear.com/7.x/avataaars/png?seed={student_id}&size=200"
            try:
                response = requests.get(url, timeout=10)
                if response.status_code == 200:
                    with open(avatar_path, 'wb') as f:
                        f.write(response.content)
                    print(f"  -> Da luu {avatar_path}")
            except Exception as e:
                print(f"  -> Loi: {e}")

if __name__ == "__main__":
    # Doc danh sach student_id tu file Excel
    import openpyxl
    wb = openpyxl.load_workbook("slib_user_import_2700.xlsx")
    ws = wb.active
    
    student_ids = []
    for row in ws.iter_rows(min_row=2, max_col=2, values_only=True):
        if row[1]:
            student_ids.append(row[1])
    
    print(f"Tim thay {len(student_ids)} user")
    create_placeholder_avatars(student_ids)
    print("Hoan thanh!")
