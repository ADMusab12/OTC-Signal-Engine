# 📊 Binary Options Trading Signals App

> **Real-time OTC binary options trading signals on Android**  
> Powered by technical indicators, candle analysis, and a fully offline market simulator.

Built with **Jetpack Compose**, **Hilt**, **Kotlin Coroutines & Flows**, and a **custom OTC market simulator** for reliable testing and demos.

---

## 🎬 App Demo

<p align="center">
  <img src="demo/app_demo.gif" width="320" alt="App Demo" />
</p>

---

## ✨ Key Features

### 📈 Signal Engine
- Real-time monitoring of multiple **OTC currency pairs**
- Technical indicator analysis:
  - EMA 9 / EMA 20 crossovers
  - RSI (14)
  - ADX (14)
  - EMA slope confirmation
- **Confidence-based signals** (0–100%)

### 🎨 UI & UX
- Clean **Material 3** design
- Smooth animated signal alerts
- Bottom-sheet **signal history**
- Pair selection & live monitoring dashboard

### 🧪 Simulator Mode (Offline)
- Fully **offline-capable**
- No API keys required
- Realistic OTC-style candle generation
- Ideal for development, testing, and demos

---

## 🎯 Why This Project Exists

OTC binary options markets—especially during weekends—use **broker-specific synthetic price feeds** that are **not publicly available** through free APIs.

Most public forex APIs only provide **weekday market data**, making them unsuitable for OTC signal development.

This project was built to:
- Demonstrate a realistic **indicator-based trading signal system**
- Provide a **fully functional offline demo** using a custom market simulator
- Serve as a modern **Jetpack Compose + Hilt reference project**
- Allow easy future integration with real broker APIs

---

## 🛠️ Tech Stack

| Category | Technology | Purpose |
|--------|------------|---------|
| UI | Jetpack Compose | Modern declarative UI |
| Architecture | MVVM + Hilt | Clean architecture & DI |
| Asynchronous | Kotlin Coroutines & Flows | Reactive real-time data |
| Data Layer | Repository Pattern | Separation of concerns |
| Indicators | Custom EMA / RSI / ADX | Lightweight, no TA libs |
| Market Data | **MarketDataSimulator** | OTC-style candle generation |
| Real API (Optional) | Alpha Vantage | Weekday forex data |
| Logging | android.util.Log | Simple debugging |

---

## 🔌 Alpha Vantage & Simulator Strategy

### Alpha Vantage (Optional)
- Base URL:  https://www.alphavantage.co/
- Clean, well-documented REST API
- Useful for **weekday forex testing**

## ⚠️ Disclaimer
This application is for **educational and demonstration purposes only**.  
It does **not** provide financial advice or guarantee trading outcomes.

---

## ⭐ If you like this project
Give it a ⭐ and feel free to fork or contribute!
