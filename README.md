## 🐛 Known Issues

This application is still under active development and has several known issues:

- YouTube audio playback may be inconsistent on some devices
- Background playback may stop unexpectedly in some scenarios
- UI responsiveness issues on some lower-end devices
- Spotify integration is not fully functional yet
- Occasional crashes when extracting audio from certain YouTube videos

These issues are being actively worked on and will be fixed in future updates. Feel free to report any bugs you encounter by opening an issue in the repository.# Virtual Realm Music Player

## 📱 Overview

Virtual Realm Music Player is an **open source** Android music player application that allows users to search and play music from multiple sources with a focus on YouTube, with Spotify integration planned for the future. Built following clean architecture principles, this app focuses on functionality and a modular design that makes it easily extensible.

The app features a custom audio extraction system that enables playing YouTube videos as audio tracks. With background playback support and offline caching capabilities, Virtual Realm Music Player aims to provide a practical solution for music enthusiasts.

> **Note:** This project is still under active development and may contain bugs. Contributions and feedback are welcome!

## ✨ Features

- **YouTube Music Player**: Search and play audio from YouTube videos
- **Background Playback**: Continue listening even when the app is in the background
- **Offline Caching**: Access recently played tracks even without an internet connection
- **Favorites Management**: Add tracks to favorites for quick access
- **Playlist Support**: Create and manage playlists for continuous playback
- **Media Controls**: Basic media controls including play, pause, skip, and seek functionality
- **Media Notification**: Control playback from the notification shade

> **Coming Soon:**
> - Spotify integration (currently in development)
> - Enhanced UI/UX
> - Bug fixes and performance improvements

## 🛠️ Technologies Used

- **Kotlin**: Full Kotlin codebase
- **Clean Architecture**: Domain-driven design with use cases for business logic
- **MVVM Architecture**: For separation of concerns
- **Coroutines & Flow**: For asynchronous programming
- **Hilt**: For dependency injection
- **Room**: For local database storage
- **Retrofit & OkHttp**: For API communication
- **WebView**: For YouTube audio extraction
- **MediaPlayer**: For audio playback
- **Foreground Service**: For background music playback

## 📋 Requirements

- Android SDK 23+ (targetSdk 34)
- Kotlin 2.1.20+
- JDK 17
- Gradle 8.10.2+
- YouTube API Key

## 🚀 Getting Started

### Prerequisites

1. Android Studio (Latest Version)
2. YouTube API Key (Get it from [Google Cloud Console](https://console.cloud.google.com/))
    - Enable the YouTube Data API v3
    - Create credentials for an Android application

### Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/lisvindanu/VirtualRealmMusicPlayer.git
   ```

2. Create a `local.properties` file in the root project directory and add your API key:
   ```properties
   youtube.api.key=YOUR_YOUTUBE_API_KEY
   ```

3. Open the project in Android Studio and sync Gradle.

4. Make sure you have JDK 17 configured in Android Studio.

5. Build and run the application on your device or emulator (API 23 or higher).

### Key Features to Try

1. **YouTube Integration**: Search for any song on YouTube and play it directly in the app.
2. **Background Playback**: Play music in the background with media notification controls.
3. **Favorites Management**: Add songs to your favorites for quick access.

> **Note on Spotify Integration:**  
> Spotify integration code is included but not fully functional yet as it requires hosting a backend service for proper OAuth authentication flow. This feature will be completed in future updates.

## 📲 Installation

You can either:
- Build and install the app from Android Studio
- Download the latest APK from the [Releases](https://github.com/yourusername/VirtualRealmMusicPlayer/releases) section

## 🏗️ Project Structure

The project follows clean architecture principles with the following layers:

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/virtualrealm/virtualrealmmusicplayer/
│   │   │   ├── data/                   # Data layer
│   │   │   │   ├── local/              # Local storage
│   │   │   │   │   ├── cache/          # Local cache management
│   │   │   │   │   │   └── LocalCacheManager.kt
│   │   │   │   │   ├── dao/            # Data Access Objects
│   │   │   │   │   │   └── MusicDao.kt
│   │   │   │   │   ├── database/       # Room database setup
│   │   │   │   │   │   └── MusicDatabase.kt
│   │   │   │   │   ├── entity/         # Database entities
│   │   │   │   │   │   └── MusicEntity.kt
│   │   │   │   │   └── preferences/    # DataStore preferences
│   │   │   │   │       └── AuthPreferences.kt
│   │   │   │   ├── remote/             # Remote data sources
│   │   │   │   │   ├── api/            # API interfaces
│   │   │   │   │   │   ├── SpotifyApi.kt
│   │   │   │   │   │   └── YouTubeApi.kt
│   │   │   │   │   ├── dto/            # Data Transfer Objects
│   │   │   │   │   │   ├── SpotifyDto.kt
│   │   │   │   │   │   ├── YouTubeDto.kt
│   │   │   │   │   │   └── YouTubeVideoDetailsDto.kt
│   │   │   │   │   └── service/        # Network services
│   │   │   │   │       ├── AuthAuthenticator.kt
│   │   │   │   │       ├── MusicService.kt
│   │   │   │   │       ├── NetworkBoundResource.kt
│   │   │   │   │       └── TokenInterceptor.kt
│   │   │   │   ├── repository/         # Repository implementations
│   │   │   │   │   ├── AuthRepositoryImpl.kt
│   │   │   │   │   └── MusicRepositoryImpl.kt
│   │   │   │   └── util/               # Data utilities
│   │   │   │       ├── ApiResponseHandler.kt
│   │   │   │       └── NetworkConnectivityHelper.kt
│   │   │   ├── di/                     # Dependency injection modules
│   │   │   │   ├── AppModule.kt
│   │   │   │   ├── DatabaseModule.kt
│   │   │   │   ├── NetworkModule.kt
│   │   │   │   └── RepositoryModule.kt
│   │   │   ├── domain/                 # Domain layer
│   │   │   │   ├── model/              # Domain models
│   │   │   │   │   ├── AuthState.kt
│   │   │   │   │   ├── Music.kt
│   │   │   │   │   └── Resource.kt
│   │   │   │   ├── repository/         # Repository interfaces
│   │   │   │   │   ├── AuthRepository.kt
│   │   │   │   │   └── MusicRepository.kt
│   │   │   │   └── usecase/            # Use cases
│   │   │   │       ├── auth/           # Authentication use cases
│   │   │   │       │   ├── ExchangeSpotifyCodeUseCase.kt
│   │   │   │       │   ├── GetAuthStateUseCase.kt
│   │   │   │       │   ├── LogoutUseCase.kt
│   │   │   │       │   └── RefreshSpotifyTokenUseCase.kt
│   │   │   │       └── music/          # Music-related use cases
│   │   │   │           ├── GetFavoritesUseCase.kt
│   │   │   │           ├── SearchMusicUseCase.kt
│   │   │   │           └── ToggleFavoriteUseCase.kt
│   │   │   ├── service/                # Background services
│   │   │   │   ├── MusicExtractionService.kt
│   │   │   │   ├── MusicService.kt
│   │   │   │   ├── YoutubeAudioHelper.kt
│   │   │   │   └── YouTubeAudioPlayer.kt
│   │   │   ├── ui/                     # UI layer (Jetpack Compose)
│   │   │   │   ├── auth/               # Authentication screens
│   │   │   │   │   ├── AuthViewModel.kt
│   │   │   │   │   ├── LoginScreen.kt
│   │   │   │   │   └── SpotifyAuthActivity.kt
│   │   │   │   ├── common/             # Common UI components
│   │   │   │   │   ├── EmptyState.kt
│   │   │   │   │   ├── ErrorState.kt
│   │   │   │   │   ├── LoadingState.kt
│   │   │   │   │   ├── MusicItem.kt
│   │   │   │   │   └── SourceTag.kt
│   │   │   │   ├── home/               # Home screen
│   │   │   │   │   ├── HomeScreen.kt
│   │   │   │   │   └── HomeViewModel.kt
│   │   │   │   ├── main/               # Main screen and navigation
│   │   │   │   │   ├── BottomNavItem.kt
│   │   │   │   │   ├── MainViewModel.kt
│   │   │   │   │   ├── MusicAppNavHost.kt
│   │   │   │   │   └── Screen.kt
│   │   │   │   ├── player/             # Player screen
│   │   │   │   │   ├── MusicViewModel.kt
│   │   │   │   │   ├── PlayerScreen.kt
│   │   │   │   │   └── PlayerViewModel.kt
│   │   │   │   ├── playlist/           # Playlist screen
│   │   │   │   │   └── PlaylistScreen.kt
│   │   │   │   ├── search/             # Search screen
│   │   │   │   │   ├── SearchScreen.kt
│   │   │   │   │   └── SearchViewModel.kt
│   │   │   │   └── theme/              # Theme definitions
│   │   │   │       ├── Color.kt
│   │   │   │       ├── Theme.kt
│   │   │   │       └── Type.kt
│   │   │   └── util/                   # Utility classes
│   │   │       ├── ApiCredentials.kt
│   │   │       ├── AppConfig.kt
│   │   │       ├── Constants.kt
│   │   │       ├── DateTimeUtils.kt
│   │   │       ├── Extensions.kt
│   │   │       └── ResourceState.kt
│   │   └── res/                        # Resources
│   │       ├── drawable/
│   │       │   ├── ic_launcher_background.xml
│   │       │   ├── ic_launcher_foreground.xml
│   │       │   ├── ic_music_note.xml
│   │       │   ├── ic_next.xml
│   │       │   ├── ic_pause.xml
│   │       │   ├── ic_play.xml
│   │       │   ├── ic_previous.xml
│   │       │   ├── ic_spotify.xml
│   │       │   ├── ic_youtube.xml
│   │       │   └── placeholder_album.xml
│   │       ├── values/
│   │       │   ├── colors.xml
│   │       │   ├── strings.xml
│   │       │   └── themes.xml
│   │       ├── xml/
│   │       │   ├── backup_rules.xml
│   │       │   └── data_extraction_rules.xml
│   │       └── mipmap-anydpi-v26/
│   │           ├── ic_launcher.xml
│   │           └── ic_launcher_round.xml
│   └── test/                           # Unit tests
│       ├── java/com/virtualrealm/virtualrealmmusicplayer/
│       │   └── ExampleUnitTest.kt
│       └── androidTest/java/com/virtualrealm/virtualrealmmusicplayer/
│           └── ExampleInstrumentedTest.kt
├── build.gradle.kts                    # App build configuration
├── proguard-rules.pro                  # ProGuard rules
├── .gitignore                          # App-level gitignore
├── AndroidManifest.xml                 # Android manifest file
├── README.md                           # Project documentation
├── gradle.properties                   # Gradle properties
├── settings.gradle.kts                 # Gradle settings
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties
├── gradlew                             # Gradle wrapper script
└── gradlew.bat                         # Gradle wrapper script for Windows
```
```

## 💻 Development

### Architecture

The app follows MVVM architecture with Clean Architecture principles:

1. **UI Layer (Presentation)**
   - **Activities**: MainActivity, SpotifyAuthActivity
   - **Composables**: Screen layouts (HomeScreen, SearchScreen, PlayerScreen, etc.)
   - **ViewModels**: State management and business logic (AuthViewModel, SearchViewModel, etc.)

2. **Domain Layer**
   - **Use Cases**: Business logic units (SearchMusicUseCase, ToggleFavoriteUseCase, etc.)
   - **Models**: Business entities (Music, AuthState, etc.)
   - **Repository Interfaces**: Abstraction for data operations (MusicRepository, AuthRepository)

3. **Data Layer**
   - **Repository Implementations**: Concrete implementations of domain repositories
   - **Remote Data Sources**: API clients (SpotifyApi, YouTubeApi)
   - **Local Data Sources**: Database and preferences (MusicDao, AuthPreferences)
   - **DTO/Entities**: Data models for network and database

4. **Service Layer**
   - **Background Services**: Music playback and extraction (MusicService)
   - **Media Handling**: Playback control and audio extraction

### Key Implementation Notes

- **State Management**: Using Kotlin Flow for reactive state updates
- **Dependency Injection**: Hilt for dependency injection throughout the app
- **Local Database**: Room for caching and offline access
- **Network Communication**: Retrofit with OkHttp for API requests
- **WebView Integration**: Custom implementation for YouTube audio extraction
- **Foreground Service**: For background audio playback with notification controls

### Testing

Run the unit tests with:
```bash
./gradlew test
```

Run the instrumented tests with:
```bash
./gradlew connectedAndroidTest
```

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

```
MIT License

Copyright (c) 2025 Virtual Realm

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## 💬 Disclaimer

This application is intended for educational and personal use only. The developers of this application are not responsible for any misuse of this software or violations of terms of service of YouTube, Spotify, or any other third-party services. Users should be aware that extracting audio from YouTube videos may violate YouTube's Terms of Service in some scenarios.

## 🤝 Contributing

Contributions are welcome! This is an open source project that's still in development. Feel free to:

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

If you're interested in collaborating on this project or have any questions, feel free to reach out via email at anaphygon@protonmail.com.