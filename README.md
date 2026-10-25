# Çalışma Saati Takip Uygulaması

Native Android (Kotlin + Jetpack Compose) ile geliştirilmiş, Firebase Auth + Firestore + Room (offline-first) mimarisine sahip çalışma saati yoklama uygulaması.

## Özellikler

1. **Google Hesabı ile Giriş** - Firebase Authentication kullanarak güvenli giriş
2. **Tek Butonla Giriş/Çıkış** - Ana ekrandan büyük butonla anlık kayıt
3. **Geriye Dönük Kayıt** - Geçmiş tarih/saat için manuel giriş
4. **Günlük/Aylık İstatistikler** - Takvimsel bazda çalışma süreleri ve grafikler
5. **Offline-First** - İnternet yoksa yerel (Room) depolama, gelince Firebase'e senkronizasyon
6. **Arka Plan Senkronizasyonu** - WorkManager ile periyodik otomatik senkronizasyon

## Teknoloji Stack'i

- **UI**: Jetpack Compose + Material3
- **Architecture**: MVVM + Repository Pattern + Clean Architecture
- **DI**: Hilt
- **Local DB**: Room Database
- **Remote DB**: Firebase Firestore
- **Auth**: Firebase Authentication (Google Sign-In)
- **Background Sync**: WorkManager
- **Charts**: Vico Compose (placeholder - entegrasyon gerekli)
- **Serialization**: Kotlinx Serialization
- **Date/Time**: Kotlinx DateTime

## Kurulum

### 1. Firebase Projesi Oluşturun

1. [Firebase Console](https://console.firebase.google.com/) adresine gidin
2. Yeni proje oluşturun
3. **Authentication** > **Sign-in method** > **Google**'u etkinleştirin
4. **Firestore Database** > **Create database** > Test modunda başlatın
5. **Project Settings** > **General** > **Android app** ekleyin
   - Package name: `com.workhourtracker`
   - `google-services.json` indirin ve `app/` klasörüne koyun

### 2. SHA-1 Fingerprint Ekleyin

```bash
# Debug keystore için
./gradlew signingReport
```

Firebase Console > Project Settings > SHA certificate fingerprints'e ekleyin.

### 3. google-services.json Yerleştirin

İndirdiğiniz `google-services.json` dosyasını `app/` klasörüne kopyalayın.

### 4. Strings.xml Güncelleyin

`app/src/main/res/values/strings.xml` dosyasındaki placeholder değerleri Firebase Console'dan aldığınız değerlerle güncelleyin:
- `default_web_client_id` (Web client ID)
- Diğer Firebase config değerleri

### 5. Derleyin ve Çalıştırın

```bash
./gradlew assembleDebug
```

## Proje Yapısı

```
app/src/main/java/com/workhourtracker/
├── data/
│   ├── local/           # Room Database, DAO
│   ├── remote/          # Firestore, Auth Repository
│   └── repository/      # Repository Implementation, SyncWorker
├── domain/
│   ├── model/           # Data models (AttendanceRecord, Stats, UserProfile)
│   └── usecase/         # Business logic (gelecekte eklenebilir)
├── presentation/
│   ├── navigation/      # Navigation Graph
│   ├── ui/
│   │   ├── components/  # Reusable UI components
│   │   └── screens/     # Auth, Main, Stats, Settings screens
│   └── viewmodel/       # ViewModels
├── di/                  # Hilt Modules
└── util/                # NetworkMonitor, SyncScheduler, Converters
```

## Offline-First Mimarisi

```
┌─────────────────┐     ┌─────────────────┐
│   Room DB       │────▶│  SyncWorker     │────▶│  Firestore    │
│   (Local)       │     │  (WorkManager)  │     │  (Remote)     │
└─────────────────┘     └─────────────────┘     └─────────────────┘
        │                       │                       │
        ▼                       ▼                       ▼
   Anlık yazma           Periyodik/           Gerçek zamanlı
   Okuma (Flow)          Manuel tetikleme     Yedekleme
```

- Tüm yazma işlemleri önce Room'a yapılır
- `isSynced` flag'i ile senkronizasyon durumu takip edilir
- WorkManager her 15 dakikada bir (bağlantı varsa) senkronize eder
- Ağ değişimleri `NetworkMonitor` ile dinlenir

## Ekranlar

1. **Auth Screen** - Google ile giriş
2. **Main Screen** - Büyük giriş/çıkış butonu, günlük kayıtlar, geriye dönük ekleme
3. **Stats Screen** - Aylık özet kartlar, grafik (placeholder), günlük detay listesi
4. **Settings Screen** - Kullanıcı bilgisi, manuel senkronizasyon, veri yönetimi, çıkış

## Geliştirme Notları

### Grafik Entegrasyonu (Vico)
`StatsScreen.kt` içinde grafik placeholder vardır. Gerçek grafik için:

```kotlin
// Vico Compose kullanımı örneği
BarChart(
    data = stats.dailyBreakdown.map { (date, stat) ->
        BarChartData(
            label = date.dayOfMonth.toString(),
            value = stat.totalMinutes.toFloat()
        )
    }
)
```

### Geriye Dönük Giriş Dialog'u
`RetroactiveEntryDialog.kt` içinde tarih/saat picker placeholder vardır. Gerçek implementasyon için `DatePickerDialog` ve `TimePickerDialog` kullanılmalıdır.

### Google Sign-In Activity Result
`AuthRepository.kt` içinde `signInWithGoogle` fonksiyonu `Activity` parametresi alır. `MainActivity` içinde `registerForActivityResult` ile handle edilmelidir.

## Lisans

MIT License