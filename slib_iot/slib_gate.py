#!/usr/bin/env python3

import os
import time
import threading
import logging
import requests
import digitalio
import board
from PIL import Image, ImageDraw, ImageFont
import adafruit_rgb_display.ili9341 as ili9341
import RPi.GPIO as GPIO
from smartcard.System import readers
from smartcard.util import toHexString

# Load .env file if python-dotenv is available
try:
    from dotenv import load_dotenv
    load_dotenv()
except ImportError:
    pass  # python-dotenv not installed, use os.environ only

logging.basicConfig(level=logging.INFO, format='%(asctime)s %(levelname)s %(message)s')
logger = logging.getLogger("slib_gate")

LOGO_PATH = "logo.png"
BUZZER_PIN = 17
LED_R = 27
LED_G = 22
LED_B = 23
BAUDRATE = 24000000

COLOR_BG_WHITE = (255, 255, 255)
COLOR_SUCCESS  = "#2ecc71"
COLOR_ERROR    = "#e74c3c"
COLOR_WARNING  = "#f39c12"
COLOR_TEXT_MAIN = "#2c3e50"
COLOR_TEXT_SUB  = "#7f8c8d"
COLOR_BRAND     = "#FF751F"

# ============================================================
# Configuration: loaded from environment variables with fallback
# ============================================================
SLIB_API_BASE = os.environ.get("SLIB_API_URL", "https://api.slibsystem.site")
SLIB_GATE_ID = os.environ.get("SLIB_GATE_ID", "GATE_01")
SLIB_API_KEY = os.environ.get("SLIB_GATE_API_KEY", "").strip()
SLIB_HEARTBEAT_INTERVAL = int(os.environ.get("SLIB_HEARTBEAT_INTERVAL", "30"))
COOLDOWN_SECONDS = int(os.environ.get("SLIB_COOLDOWN_SECONDS", "30"))

API_URL = f"{SLIB_API_BASE}/slib/hce/checkin"
HEARTBEAT_URL = f"{SLIB_API_BASE}/slib/hce/stations/{SLIB_GATE_ID}/heartbeat"

# Cooldown tracking: prevents duplicate scans of the same token
last_scanned_token = None
last_scanned_time = 0

SELECT_APDU = [0x00, 0xA4, 0x04, 0x00, 0x07, 0xF0, 0x39, 0x41, 0x48, 0x14, 0x81, 0x00]

GPIO.setmode(GPIO.BCM)
GPIO.setwarnings(False)

GPIO.setup(BUZZER_PIN, GPIO.OUT)
buzzer_pwm = GPIO.PWM(BUZZER_PIN, 2500)
buzzer_pwm.stop()

GPIO.setup([LED_R, LED_G, LED_B], GPIO.OUT)

cs_pin = digitalio.DigitalInOut(board.CE0)
dc_pin = digitalio.DigitalInOut(board.D24)
reset_pin = digitalio.DigitalInOut(board.D25)

spi = board.SPI()

disp = ili9341.ILI9341(
    spi,
    rotation=0,
    cs=cs_pin,
    dc=dc_pin,
    rst=reset_pin,
    baudrate=BAUDRATE,
)

width = 320
height = 240

image = Image.new("RGB", (width, height))
draw = ImageDraw.Draw(image)

try:
    font_header = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 28)
    font_main   = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 22)
    font_sub    = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 16)
except IOError:
    font_header = ImageFont.load_default()
    font_main = ImageFont.load_default()
    font_sub = ImageFont.load_default()

def set_led(color):
    GPIO.output(LED_R, GPIO.LOW)
    GPIO.output(LED_G, GPIO.LOW)
    GPIO.output(LED_B, GPIO.LOW)

    if color == 'RED':
        GPIO.output(LED_R, GPIO.HIGH)
    elif color == 'GREEN':
        GPIO.output(LED_G, GPIO.HIGH)
    elif color == 'BLUE':
        GPIO.output(LED_B, GPIO.HIGH)
    elif color == 'YELLOW':
        GPIO.output(LED_R, GPIO.HIGH)
        GPIO.output(LED_G, GPIO.HIGH)

def beep(duration):
    buzzer_pwm.start(50)
    time.sleep(duration)
    buzzer_pwm.stop()

def get_text_size(text, font):
    try:
        bbox = draw.textbbox((0, 0), text, font=font)
        return bbox[2] - bbox[0], bbox[3] - bbox[1]
    except:
        return draw.textsize(text, font=font)

def draw_center_text(text, font, y_pos, color):
    w, h = get_text_size(text, font)
    x = (width - w) // 2
    draw.text((x, y_pos), text, font=font, fill=color)

def draw_multiline_center_text(text, font, start_y, color):
    margin = 10
    max_width = width - (margin * 2)
    lines = []
    words = text.split()
    
    current_line = ""
    for word in words:
        test_line = current_line + word + " "
        w, h = get_text_size(test_line, font)
        if w <= max_width:
            current_line = test_line
        else:
            lines.append(current_line)
            current_line = word + " "
    lines.append(current_line)
    
    current_y = start_y
    for line in lines:
        draw_center_text(line.strip(), font, current_y, color)
        w, h = get_text_size(line, font)
        current_y += h + 5 

def draw_icon(icon_type, x, y, size):
    if icon_type == 'check':
        draw.ellipse((x, y, x+size, y+size), fill=COLOR_SUCCESS)
        start_line1 = (x + size*0.25, y + size*0.5)
        end_line1   = (x + size*0.45, y + size*0.7)
        end_line2   = (x + size*0.75, y + size*0.3)
        draw.line([start_line1, end_line1], fill="white", width=5)
        draw.line([end_line1, end_line2], fill="white", width=5)
        
    elif icon_type == 'cross':
        draw.ellipse((x, y, x+size, y+size), fill=COLOR_ERROR)
        offset = size * 0.3
        draw.line((x+offset, y+offset, x+size-offset, y+size-offset), fill="white", width=5)
        draw.line((x+size-offset, y+offset, x+offset, y+size-offset), fill="white", width=5)

    elif icon_type == 'wait':
        draw.ellipse((x, y, x+size, y+size), fill=COLOR_WARNING)
        dot_r = size * 0.1
        cy = y + size/2
        cx = x + size/2
        draw.ellipse((cx-20-dot_r, cy-dot_r, cx-20+dot_r, cy+dot_r), fill="white")
        draw.ellipse((cx-dot_r, cy-dot_r, cx+dot_r, cy+dot_r), fill="white")
        draw.ellipse((cx+20-dot_r, cy-dot_r, cx+20+dot_r, cy+dot_r), fill="white")

def show_standby_screen():
    draw.rectangle((0, 0, width, height), fill=COLOR_BG_WHITE)
    
    current_y = 20

    if os.path.exists(LOGO_PATH):
        try:
            logo_img = Image.open(LOGO_PATH).convert("RGBA")
            max_h = 100
            ratio = max_h / float(logo_img.size[1])
            new_w = int(float(logo_img.size[0]) * ratio)
            new_h = int(float(logo_img.size[1]) * ratio)
            
            logo_img = logo_img.resize((new_w, new_h), Image.BICUBIC)
            logo_x = (width - new_w) // 2
            
            image.paste(logo_img, (logo_x, current_y), logo_img)
            current_y += new_h + 30 
        except Exception:
            current_y = 60 

    text1 = "CHẠM ĐIỆN THOẠI"
    text2 = "ĐỂ CHECK-IN"
    
    draw_center_text(text1, font_header, current_y, COLOR_BRAND)
    draw_center_text(text2, font_main, current_y + 35, COLOR_TEXT_MAIN)

    disp.image(image.rotate(90, expand=True))

def show_status_screen(status_type, title, message):
    draw.rectangle((0, 0, width, height), fill=COLOR_BG_WHITE)
    
    icon_size = 70
    icon_x = (width - icon_size) // 2
    icon_y = 15
    
    if status_type == 'success':
        draw_icon('check', icon_x, icon_y, icon_size)
        text_color = COLOR_SUCCESS
    elif status_type == 'error':
        draw_icon('cross', icon_x, icon_y, icon_size)
        text_color = COLOR_ERROR
    else:
        draw_icon('wait', icon_x, icon_y, icon_size)
        text_color = COLOR_WARNING
        
    draw_center_text(title, font_header, icon_y + icon_size + 15, text_color)
    
    draw_multiline_center_text(message, font_main, icon_y + icon_size + 55, COLOR_TEXT_MAIN)

    disp.image(image.rotate(90, expand=True))

# ============================================================
# Heartbeat thread: sends periodic heartbeat to backend
# ============================================================
def heartbeat_worker():
    """Background thread that sends heartbeat to backend every N seconds."""
    while True:
        if not SLIB_API_KEY:
            logger.error("SLIB_GATE_API_KEY is missing. Heartbeat skipped until the gateway is configured.")
            time.sleep(SLIB_HEARTBEAT_INTERVAL)
            continue
        try:
            headers = {
                "Content-Type": "application/json",
                "X-API-KEY": SLIB_API_KEY
            }
            resp = requests.post(HEARTBEAT_URL, headers=headers, timeout=5)
            if resp.status_code == 200:
                logger.info("Heartbeat OK - %s", SLIB_GATE_ID)
            else:
                logger.warning("Heartbeat error %s: %s", resp.status_code, resp.text)
        except Exception as e:
            logger.error("Heartbeat failed: %s", e)
        time.sleep(SLIB_HEARTBEAT_INTERVAL)


def send_to_backend(token):
    if not SLIB_API_KEY:
        logger.error("SLIB_GATE_API_KEY is missing. Rejecting gateway request.")
        return False, {"message": "Thiếu cấu hình API key"}
    try:
        payload = {"token": token, "gateId": SLIB_GATE_ID}
        
        headers = {
            "Content-Type": "application/json",
            "X-API-KEY": SLIB_API_KEY
        }
        
        response = requests.post(API_URL, json=payload, headers=headers, timeout=5)
        
        if response.status_code == 200:
            data = response.json()
            logger.info("Backend response: type=%s, message=%s", data.get('type'), data.get('message'))
            return True, data
        
        # Parse error response for station-specific messages
        try:
            err_data = response.json()
            logger.warning("Backend error %s: %s", response.status_code, err_data.get('message'))
            return False, err_data
        except Exception:
            return False, {"message": "Server Error"}
    except Exception as e:
        logger.error("Backend connection failed: %s", e)
        return False, {"message": "Mất kết nối"}

def main():
    logger.info("--- SLIB GATEWAY UI UPGRADED ---")
    logger.info("Gate ID: %s", SLIB_GATE_ID)
    logger.info("API URL: %s", API_URL)
    logger.info("Heartbeat URL: %s", HEARTBEAT_URL)
    logger.info("Heartbeat Interval: %ss", SLIB_HEARTBEAT_INTERVAL)
    
    # Start heartbeat thread (daemon=True so it dies with main process)
    hb_thread = threading.Thread(target=heartbeat_worker, daemon=True)
    hb_thread.start()
    logger.info("Heartbeat thread started")
    
    beep(0.1)
    set_led('BLUE')
    
    show_standby_screen()

    while True:
        try:
            r = readers()
            if len(r) == 0:
                time.sleep(1)
                continue

            reader = r[0]
            connection = reader.createConnection()
            
            try:
                connection.connect()
                
                response, sw1, sw2 = connection.transmit(SELECT_APDU)
                status = (sw1 << 8) | sw2

                if status == 0x9000:
                    set_led('YELLOW')
                    show_status_screen('processing', "ĐANG XỬ LÝ...", "Vui lòng giữ thẻ")
                    beep(0.1)

                    token = bytes(response).decode('utf-8', errors='ignore').strip().replace('\x00', '')
                    
                    if token:
                        # Cooldown check: skip if same token was scanned recently
                        global last_scanned_token, last_scanned_time
                        now = time.time()
                        if token == last_scanned_token and (now - last_scanned_time) < COOLDOWN_SECONDS:
                            remaining = int(COOLDOWN_SECONDS - (now - last_scanned_time))
                            logger.info("Cooldown active for the last token, wait %ss", remaining)
                            show_status_screen('error', "VUI LÒNG CHỜ", f"Thẻ đã quét, chờ {remaining} giây")
                            beep(0.3)
                            time.sleep(3)
                            set_led('BLUE')
                            show_standby_screen()
                            continue
                        
                        success, data = send_to_backend(token)
                        
                        if success:
                            set_led('GREEN')
                            
                            req_type = data.get("type", "UNKNOWN")
                            message = data.get("message", "Thành công")

                            title = "XIN CHÀO!"
                            if req_type == "CHECK_OUT":
                                title = "TẠM BIỆT!"
                            
                            show_status_screen('success', title, message)
                            beep(0.1) 
                            time.sleep(0.1)
                            beep(0.2)
                            
                            # Update cooldown tracker on successful scan
                            last_scanned_token = token
                            last_scanned_time = time.time()
                        else:
                            set_led('RED')
                            err_msg = data.get("message", "Lỗi xác thực")
                            show_status_screen('error', "THẤT BẠI", err_msg)
                            beep(0.8)
                    else:
                        set_led('RED')
                        show_status_screen('error', "LỖI", "Token rỗng")
                        beep(0.5)

                    # Display result for 5 seconds (enough time for user to remove phone)
                    time.sleep(5)
                    
                    set_led('BLUE')
                    show_standby_screen()

                else:
                    set_led('RED')
                    show_status_screen('error', "LỖI APP", "Chưa cài đặt\nhệ thống SLIB hoặc chưa đăng nhập hệ thống")
                    beep(0.8)
                    time.sleep(3)
                    set_led('BLUE')
                    show_standby_screen()
            
            except Exception:
                pass

        except Exception as e:
            logger.error("System loop error: %s", e)
        
        time.sleep(0.1)

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        GPIO.cleanup()
        logger.info("Goodbye!")
