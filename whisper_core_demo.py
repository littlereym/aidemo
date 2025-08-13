#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
OpenAI Whisper 語音轉文字核心程式示例
展示 Python 如何處理語音生成文字
"""

import os
import sys
from pathlib import Path

import numpy as np
import torch
import whisper


def whisper_core_demo(audio_file_path):
    """
    展示 Whisper 核心語音轉文字流程
    """
    print("=" * 60)
    print("🎤 OpenAI Whisper Python 語音處理核心程式")
    print("=" * 60)
    
    # 第 1 步：載入 Whisper 模型
    print("\n📥 第 1 步：載入 Whisper AI 模型...")
    model = whisper.load_model("base")
    print(f"✅ 模型載入成功：{model}")
    print(f"🧠 模型維度：{model.dims}")
    
    # 第 2 步：音訊檔案預處理
    print("\n🔊 第 2 步：音訊檔案載入與預處理...")
    if not os.path.exists(audio_file_path):
        print(f"❌ 檔案不存在：{audio_file_path}")
        return
    
    # 使用 Whisper 的音訊載入函數
    audio = whisper.load_audio(audio_file_path)
    print(f"📊 音訊資料形狀：{audio.shape}")
    print(f"🎵 音訊長度：{len(audio) / whisper.audio.SAMPLE_RATE:.2f} 秒")
    print(f"⚡ 取樣率：{whisper.audio.SAMPLE_RATE} Hz")
    
    # 第 3 步：音訊正規化與填充
    print("\n🔧 第 3 步：音訊正規化處理...")
    audio = whisper.pad_or_trim(audio)
    print(f"📏 處理後音訊長度：{len(audio) / whisper.audio.SAMPLE_RATE:.2f} 秒")
    
    # 第 4 步：生成梅爾頻譜圖
    print("\n📈 第 4 步：生成梅爾頻譜圖...")
    mel = whisper.log_mel_spectrogram(audio, model.dims.n_mels)
    print(f"🌊 梅爾頻譜形狀：{mel.shape}")
    
    # 第 5 步：語言檢測
    print("\n🌍 第 5 步：自動語言檢測...")
    mel = mel.to(model.device)
    _, probs = model.detect_language(mel)
    detected_lang = max(probs, key=probs.get)
    confidence = probs[detected_lang]
    print(f"🎯 檢測到語言：{detected_lang} (信心度：{confidence:.2%})")
    
    # 第 6 步：語音轉文字 (核心步驟)
    print("\n🚀 第 6 步：執行語音轉文字...")
    options = whisper.DecodingOptions(
        language="zh",  # 指定中文
        fp16=False      # CPU 使用 FP32
    )
    
    result = whisper.decode(model, mel, options)
    
    # 第 7 步：結果輸出
    print("\n📝 第 7 步：語音識別結果")
    print("-" * 40)
    print(f"🎯 識別文字：{result.text}")
    print(f"📊 平均概率：{result.avg_logprob:.4f}")
    print(f"🔢 無語音概率：{result.no_speech_prob:.4f}")
    print(f"⚡ 溫度：{result.temperature}")
    
    # 使用完整的 transcribe 函數 (更高級的功能)
    print("\n🌟 使用完整 transcribe 函數:")
    full_result = whisper.transcribe(model, audio_file_path, language="zh")
    
    print(f"📋 完整識別文字：")
    print(full_result["text"])
    
    if "segments" in full_result:
        print(f"\n⏱️ 時間軸資訊：")
        for i, segment in enumerate(full_result["segments"][:3]):  # 只顯示前3個片段
            print(f"  {i+1}. [{segment['start']:.1f}s - {segment['end']:.1f}s] {segment['text']}")
    
    return full_result

def show_whisper_architecture():
    """
    展示 Whisper 的神經網路架構
    """
    print("\n" + "=" * 60)
    print("🧠 Whisper AI 神經網路架構")
    print("=" * 60)
    
    model = whisper.load_model("base")
    
    print(f"🏗️ 模型類型：{type(model)}")
    print(f"📊 參數數量：{sum(p.numel() for p in model.parameters()):,}")
    print(f"🎯 詞彙表大小：{model.dims.n_vocab:,}")
    print(f"🔤 文字長度：{model.dims.n_text_ctx}")
    print(f"🎵 音訊長度：{model.dims.n_audio_ctx}")
    print(f"🌊 梅爾頻率數：{model.dims.n_mels}")
    
    print("\n📋 模型結構組件：")
    for name, module in model.named_children():
        print(f"  • {name}: {type(module).__name__}")

def main():
    """主程式"""
    print("🎯 Python Whisper 語音處理程式示範")
    
    # 檢查是否有音訊檔案參數
    if len(sys.argv) > 1:
        audio_file = sys.argv[1]
    else:
        # 使用預設的測試檔案
        audio_dir = Path("uploads/audio")
        if audio_dir.exists():
            audio_files = list(audio_dir.glob("*.mp3")) + list(audio_dir.glob("*.wav"))
            if audio_files:
                audio_file = str(audio_files[0])
                print(f"📁 使用預設音訊檔案：{audio_file}")
            else:
                print("❌ 找不到音訊檔案")
                return
        else:
            print("❌ uploads/audio 目錄不存在")
            return
    
    # 執行語音轉文字示範
    result = whisper_core_demo(audio_file)
    
    # 顯示架構資訊
    show_whisper_architecture()
    
    print("\n" + "=" * 60)
    print("✅ Python Whisper 語音處理程式示範完成")
    print("=" * 60)

if __name__ == "__main__":
    main()
