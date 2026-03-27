#!/usr/bin/env python3

import os
import time
import digitalio
import board
from PIL import Image, ImageDraw, ImageFont, ImageSequence
import adafruit_rgb_display.ili9341 as ili9341
import RPi.GPIO as GPIO

LOGO_PATH = "logo.png"
GIF_LOADING = "loading.gif"
GIF_SUCCESS = "success.gif"
GIF_FAILED  = "failed.gif"
GIF_ERROR   = "error.gif"

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

def draw_center_text_on_image(img_draw, text, font, y_pos, color, img_width):
    w, h = get_text_size(text, font)
    x = (img_width - w) // 2
    img_draw.text((x, y_pos), text, font=font, fill=color)

def draw_multiline_text_on_image(img_draw, text, font, start_y, color, img_width):
    margin = 10
    max_width = img_width - (margin * 2)
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
        draw_center_text_on_image(img_draw, line.strip(), font, current_y, color, img_width)
        w, h = get_text_size(line, font)
        current_y += h + 5 

def draw_center_text(text, font, y_pos, color):
    draw_center_text_on_image(draw, text, font, y_pos, color, width)

def show_static_status(status_type, title, message):
    draw.rectangle((0, 0, width, height), fill=COLOR_BG_WHITE)
    
    icon_size = 80
    center_y = 60
    
    if status_type == 'success':
        text_color = COLOR_SUCCESS
        draw.ellipse((width//2 - 40, center_y-40, width//2+40, center_y+40), fill=COLOR_SUCCESS)
    elif status_type == 'denied':
        text_color = COLOR_ERROR
        draw.ellipse((width//2 - 40, center_y-40, width//2+40, center_y+40), fill=COLOR_ERROR)
    elif status_type == 'sys_error':
        text_color = COLOR_ERROR
        draw.rectangle((width//2 - 40, center_y-40, width//2+40, center_y+40), fill=COLOR_ERROR)
    else:
        text_color = COLOR_WARNING
        draw.ellipse((width//2 - 40, center_y-40, width//2+40, center_y+40), fill=COLOR_WARNING)

    draw_center_text(title, font_header, center_y + (icon_size//2) + 20, text_color)
    
    margin = 10
    max_width = width - (margin * 2)
    lines = []
    words = message.split()
    current_line = ""
    for word in words:
        test_line = current_line + word + " "
        w, h = get_text_size(test_line, font_main)
        if w <= max_width:
            current_line = test_line
        else:
            lines.append(current_line)
            current_line = word + " "
    lines.append(current_line)
    
    txt_y = center_y + (icon_size//2) + 60
    for line in lines:
        draw_center_text(line.strip(), font_main, txt_y, COLOR_TEXT_MAIN)
        w, h = get_text_size(line, font_main)
        txt_y += h + 5 

    disp.image(image.rotate(90, expand=True))

def play_gif_animation(gif_path, title, message, status_type, duration=3.0):
    if not os.path.exists(gif_path):
        show_static_status(status_type, title, message)
        time.sleep(duration)
        return

    try:
        gif = Image.open(gif_path)
        
        icon_size = 80
        center_x = width // 2
        center_y = 60
        
        if status_type == 'success':
            text_color = COLOR_SUCCESS
        elif status_type == 'denied' or status_type == 'sys_error':
            text_color = COLOR_ERROR
        else:
            text_color = COLOR_WARNING

        processed_frames = []
        for frame in ImageSequence.Iterator(gif):
            frame = frame.convert("RGBA")
            frame = frame.resize((icon_size, icon_size), Image.BICUBIC)
            
            bg_frame = Image.new("RGB", (width, height), COLOR_BG_WHITE)
            
            x = center_x - (icon_size // 2)
            y = center_y - (icon_size // 2)
            
            bg_frame.paste(frame, (x, y), frame)
            
            d = ImageDraw.Draw(bg_frame)
            draw_center_text_on_image(d, title, font_header, center_y + (icon_size//2) + 20, text_color, width)
            draw_multiline_text_on_image(d, message, font_main, center_y + (icon_size//2) + 60, COLOR_TEXT_MAIN, width)
            
            processed_frames.append(bg_frame.rotate(90, expand=True))

        if not processed_frames: return

        start_time = time.time()
        frame_idx = 0
        total_frames = len(processed_frames)
        
        while time.time() - start_time < duration:
            disp.image(processed_frames[frame_idx])
            frame_idx = (frame_idx + 1) % total_frames
                    
    except Exception:
        show_static_status(status_type, title, message)
        time.sleep(duration)

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

    text1 = "Vui lòng chạm"
    text2 = "điện thoại của bạn"
    
    draw_center_text(text1, font_main, current_y, COLOR_BRAND)
    draw_center_text(text2, font_main, current_y + 30, COLOR_BRAND)

    disp.image(image.rotate(90, expand=True))

def main_test():
    print("--- BẮT ĐẦU TEST MÀN HÌNH ---")
    
    while True:
        print("1. Standby Screen")
        set_led('BLUE')
        show_standby_screen()
        time.sleep(2)

        print("2. Test Loading (Processing)")
        set_led('YELLOW')
        beep(0.1)
        play_gif_animation(GIF_LOADING, "ĐANG ĐỌC...", "Vui lòng chờ", "processing", duration=3.0)

        print("3. Test Thành Công (Success)")
        set_led('GREEN')
        beep(0.1)
        time.sleep(0.1)
        beep(0.2)
        play_gif_animation(GIF_SUCCESS, "XIN CHÀO!", "Nguyễn Văn A\nK15 - Software", "success", duration=4.0)

        print("4. Test Thất Bại (Denied)")
        set_led('RED')
        beep(0.8)
        play_gif_animation(GIF_FAILED, "TỪ CHỐI", "Thẻ không hợp lệ\nhoặc hết hạn", "denied", duration=4.0)

        print("5. Test Lỗi Hệ Thống (Sys Error)")
        set_led('RED')
        beep(0.5)
        play_gif_animation(GIF_ERROR, "LỖI APP", "Chưa cài đặt App\nhoặc mất kết nối", "sys_error", duration=4.0)

        print("--- Hết vòng lặp, chạy lại... ---")

if __name__ == "__main__":
    try:
        main_test()
    except KeyboardInterrupt:
        GPIO.cleanup()
        print("\nTest Ended!")
