#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
簡單的語音識別 API 測試工具
用於測試不同啟動方式的語音識別功能
"""

import json
import os
import sys
from pathlib import Path

import requests


def test_speech_recognition():
    """測試語音識別 API"""
    
    # API 端點
    url = "http://localhost:8080/speech/api/speech-to-text"
    
    # 查找測試音頻檔案
    test_files = [
        "uploads/audio/audio_1754882213026.mp3",
        "uploads/audio/audio_1754641308230.mp3"
    ]
    
    audio_file_path = None
    for test_file in test_files:
        if os.path.exists(test_file):
            audio_file_path = test_file
            break
    
    if not audio_file_path:
        print("❌ 找不到測試音頻檔案")
        # 列出可用的音頻檔案
        audio_dir = Path("uploads/audio")
        if audio_dir.exists():
            print("📂 可用的音頻檔案:")
            for file in audio_dir.glob("*.mp3"):
                print(f"   - {file}")
                if not audio_file_path:  # 使用第一個找到的
                    audio_file_path = str(file)
        
        if not audio_file_path:
            print("❌ 沒有可用的測試音頻檔案")
            return False
    
    print(f"🎵 使用測試檔案: {audio_file_path}")
    print(f"📊 檔案大小: {os.path.getsize(audio_file_path):,} bytes")
    
    try:
        # 發送 API 請求
        print(f"🚀 發送語音識別請求到: {url}")
        
        with open(audio_file_path, 'rb') as f:
            files = {
                'audioFile': (os.path.basename(audio_file_path), f, 'audio/mpeg')
            }
            
            response = requests.post(url, files=files, timeout=120)
        
        print(f"📡 HTTP 狀態: {response.status_code}")
        
        if response.status_code == 200:
            try:
                result = response.json()
                print("\n✅ 語音識別成功!")
                print("=" * 50)
                
                # 顯示結果
                data = result.get('data', {})
                print(f"🤖 引擎: {data.get('engine', 'N/A')}")
                print(f"🎯 成功: {result.get('success', 'N/A')}")
                print(f"⏱️ 處理時間: {data.get('processingTime', 'N/A')}")
                print(f"📊 信心度: {data.get('confidence', 'N/A')}")
                
                recognized_text = data.get('recognizedText', 'N/A')
                print(f"📝 識別文字:")
                print(f"   {recognized_text}")
                
                return True
                
            except json.JSONDecodeError:
                print(f"❌ 無法解析 JSON 響應:")
                print(f"   {response.text}")
                return False
        else:
            print(f"❌ 請求失敗: {response.status_code}")
            print(f"📄 響應內容: {response.text}")
            return False
            
    except requests.exceptions.RequestException as e:
        print(f"❌ 網路請求錯誤: {str(e)}")
        return False
    except Exception as e:
        print(f"❌ 發生未預期的錯誤: {str(e)}")
        return False

def check_server_status():
    """檢查伺服器狀態"""
    try:
        response = requests.get("http://localhost:8080", timeout=5)
        if response.status_code == 200:
            print("✅ 伺服器正常運行")
            return True
        else:
            print(f"⚠️ 伺服器響應異常: {response.status_code}")
            return False
    except Exception as e:
        print(f"❌ 無法連接到伺服器: {str(e)}")
        return False

def main():
    print("=" * 60)
    print("🧪 語音識別功能測試工具")
    print("=" * 60)
    
    # 檢查伺服器狀態
    print("\n🔍 檢查伺服器狀態...")
    if not check_server_status():
        print("❌ 伺服器不可用，請先啟動應用程式")
        return
    
    # 測試語音識別
    print("\n🎤 測試語音識別功能...")
    success = test_speech_recognition()
    
    print("\n" + "=" * 60)
    if success:
        print("🎉 測試通過 - 語音識別功能正常!")
    else:
        print("❌ 測試失敗 - 語音識別功能有問題")
    print("=" * 60)

if __name__ == "__main__":
    main()
