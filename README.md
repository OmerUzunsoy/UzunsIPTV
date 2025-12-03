# 📺 UzunsIPTV

Modern ve kullanıcı dostu bir **Android IPTV Player** uygulaması. Xtream Codes API ve M3U playlist desteği ile canlı TV, film (VOD) ve dizi içeriklerini izlemenizi sağlar.

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![ExoPlayer](https://img.shields.io/badge/ExoPlayer-4285F4?style=for-the-badge&logo=google&logoColor=white)
![Android TV](https://img.shields.io/badge/Android%20TV-41BDF5?style=for-the-badge&logo=android&logoColor=white)

## ✨ Özellikler

### 📡 Canlı TV (Live TV)
- Xtream Codes API desteği
- M3U playlist desteği
- Kategori bazlı kanal listesi
- Kanal favorilere ekleme
- Hızlı kanal geçişi (Hotkey desteği)

### 🎬 Filmler (VOD)
- Film kategorileri
- Film detay sayfası
- Fragman izleme (YouTube entegrasyonu)
- İzleme geçmişi takibi

### 📺 Diziler (Series)
- Dizi kategorileri
- Sezon ve bölüm listesi
- Bölüm bazlı izleme takibi

### 🎮 Video Oynatıcı
- **ExoPlayer** tabanlı güçlü oynatıcı
- Çoklu ses/altyazı desteği
- Aspect ratio (en-boy oranı) değiştirme
- Oynatma hızı kontrolü
- Kaldığı yerden devam etme
- Tam ekran deneyimi

### 📱 Uygulama Özellikleri
- 🌙 Karanlık/Aydınlık tema desteği
- 📺 Android TV uyumluluğu
- 👤 Çoklu hesap yönetimi
- 💾 Room Database ile yerel veri saklama
- 🔄 Otomatik giriş

## 🛠️ Teknolojiler

| Teknoloji | Kullanım Alanı |
|-----------|----------------|
| **Kotlin** | Ana programlama dili |
| **ExoPlayer** | Video oynatma |
| **Retrofit** | HTTP istekleri |
| **Gson** | JSON parse |
| **Glide** | Görsel yükleme |
| **Room Database** | Yerel veritabanı |
| **ViewModel & LiveData** | MVVM mimarisi |
| **YouTube Player** | Fragman oynatma |
| **Coroutines** | Asenkron işlemler |

## 📁 Proje Yapısı

```
app/src/main/java/com/uzuns/uzunsiptv/
├── 📱 Activities
│   ├── SelectionActivity.kt      # Ana seçim ekranı
│   ├── LoginActivity.kt          # Giriş ekranı
│   ├── DashboardActivity.kt      # Ana menü
│   ├── LiveTvActivity.kt         # Canlı TV listesi
│   ├── VodActivity.kt            # Film listesi
│   ├── VodDetailsActivity.kt     # Film detayları
│   ├── SeriesActivity.kt         # Dizi listesi
│   ├── SeriesDetailsActivity.kt  # Dizi detayları
│   ├── PlayerActivity.kt         # Video oynatıcı
│   ├── TrailerActivity.kt        # Fragman oynatıcı
│   ├── AccountsActivity.kt       # Hesap yönetimi
│   ├── SettingsActivity.kt       # Ayarlar
│   └── M3uActivity.kt            # M3U import
│
├── 🔌 Network
│   ├── XtreamApi.kt              # Xtream API interface
│   ├── ApiClient.kt              # Retrofit client
│   └── LoginResponse.kt          # API response modelleri
│
├── 📦 Models
│   ├── LiveModels.kt             # Canlı TV modelleri
│   ├── VodModels.kt              # Film modelleri
│   └── SeriesModels.kt           # Dizi modelleri
│
├── 🎨 Adapters
│   ├── ChannelAdapter.kt         # Kanal listesi
│   ├── CategoryAdapter.kt        # Kategori listesi
│   ├── VodAdapter.kt             # Film listesi
│   ├── SeriesAdapter.kt          # Dizi listesi
│   ├── SeasonAdapter.kt          # Sezon listesi
│   ├── EpisodeAdapter.kt         # Bölüm listesi
│   └── AccountAdapter.kt         # Hesap listesi
│
├── 🗄️ Database (data/db/)
│   ├── AppDatabase.kt            # Room database
│   ├── FavoriteChannel.kt        # Favori entity
│   ├── FavoriteDao.kt            # Favori DAO
│   ├── WatchProgress.kt          # İzleme ilerlemesi entity
│   └── WatchDao.kt               # İzleme DAO
│
└── 🛠️ Utils
    ├── ThemeHelper.kt            # Tema yönetimi
    ├── ChannelManager.kt         # Kanal yönetimi
    ├── ChannelHotkeyManager.kt   # Kanal kısayolları
    └── EpisodeManager.kt         # Bölüm yönetimi
```

## 📋 Gereksinimler

- **Min SDK:** 24 (Android 7.0 Nougat)
- **Target SDK:** 34 (Android 14)
- **Compile SDK:** 36
- **Kotlin:** 2.0+

## 🚀 Kurulum

1. Projeyi klonlayın:
```bash
git clone https://github.com/OmerUzunsoy/UzunsIPTV.git
```

2. Android Studio'da açın

3. Gradle sync yapın

4. Uygulamayı çalıştırın

## 📱 Ekran Görüntüleri

*Yakında eklenecek...*

## 🔐 Kullanım

### Xtream Codes ile Giriş
1. Uygulamayı açın
2. "Xtream Codes" seçeneğini tıklayın
3. Server URL, kullanıcı adı ve şifrenizi girin
4. Giriş yapın ve içeriklerin keyfini çıkarın!

### M3U Playlist ile Giriş
1. Uygulamayı açın
2. "M3U Playlist" seçeneğini tıklayın
3. M3U URL'nizi girin
4. Kanallarınız yüklenecektir

## 🤝 Katkıda Bulunma

1. Bu projeyi fork edin
2. Feature branch oluşturun (`git checkout -b feature/AmazingFeature`)
3. Değişikliklerinizi commit edin (`git commit -m 'Add some AmazingFeature'`)
4. Branch'i push edin (`git push origin feature/AmazingFeature`)
5. Pull Request açın

## 📄 Lisans

Bu proje kişisel kullanım amaçlıdır.

## 👤 Geliştirici

**Ömer Uzunsoy**

- GitHub: [@OmerUzunsoy](https://github.com/OmerUzunsoy)

---

⭐ Bu projeyi beğendiyseniz yıldız vermeyi unutmayın!
