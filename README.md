# UniSphere-App 🏠✨

![Kotlin](https://img.shields.io/badge/Kotlin-0095D5?style=for-the-badge&logo=kotlin&logoColor=white) ![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpack-compose&logoColor=white) ![Ktor](https://img.shields.io/badge/Ktor-007AFF?style=for-the-badge&logo=ktor&logoColor=white) ![Dagger Hilt](https://img.shields.io/badge/Dagger%20Hilt-388E3C?style=for-the-badge&logo=dagger&logoColor=white) ![Stars](https://img.shields.io/github/stars/steppo04/Unisphere-App?style=for-the-badge&color=gold) ![Forks](https://img.shields.io/github/forks/steppo04/Unisphere-App?style=for-the-badge&color=lightgrey)

A comprehensive multi-functional Android application built with Kotlin and Jetpack Compose, designed to streamline various aspects of daily life, particularly focusing on shared living scenarios. UniSphere-App provides robust features for house management, personal finance tracking, event organization, local points of interest, and recipe discovery, all within a modern and intuitive user interface.

## 📋 Table of Contents

*   [🌟 Features](#-features)
*   [🛠️ Tech Stack](#%EF%B8%8F-tech-stack)
*   [🚀 Installation](#-installation)
*   [💡 How to Use](#-how-to-use)
*   [📂 Project Structure](#-project-structure)
*   [🌐 API Reference](#-api-reference)
*   [🤝 Contributing](#-contributing)
*   [📄 License](#-license)
*   [🔗 Important Links](#-important-links)
*   [<footer> Footer</footer>](#footer-footer)

## 🌟 Features

UniSphere-App offers a rich set of features to manage your daily life efficiently:

*   **🔒 User Authentication & Profile Management**:
    *   Secure user registration and login functionalities.
    *   Ability to update profile picture, username, and email.
    *   Personalize app theme (light, dark, system default).
    *   Password reset mechanism (backend integration currently simulated).

*   **🏡 House Management (UniHome)**:
    *   Create or join a shared house group with other users.
    *   Manage house members: invite new roommates and remove existing ones.
    *   **Splitwise-like Shared Expenses**: Add group expenses with custom splits among members, track who paid and who owes.
    *   **Cleaning Rotation**: Set up and track weekly cleaning services with assigned members and completion status.
    *   View an overview of house balances to keep track of financial standings between roommates.
    *   Option to settle direct debts between members.
    *   Admin functionality to delete a house, with validations for outstanding balances.
    *   Members can leave a house, with validations for outstanding balances.

*   **💸 Personal Finance (UniWallet)**:
    *   Record and track personal incomes and expenses.
    *   Create and manage custom transaction categories with distinct colors.
    *   Get a real-time overview of net balance, total incomes, and total expenses.
    *   **Interactive Visualizations**: Dynamic Pie Chart illustrating expense distribution by category and a Line Chart showing balance trends over the last 30 days.
    *   Advanced transaction filtering by type (income/expense), category, and amount range.
    *   Edit and delete individual transactions and categories.

*   **🗓️ Event Calendar (UniCalendar)**:
    *   Create, view, edit, and delete events with title, description, and time slots.
    *   Associate events with multiple customizable calendars, each with a unique color.
    *   Location integration for events: manually input addresses, use current device location, or select from address suggestions.
    *   View event locations on an interactive OpenStreetMap (OSMDroid) within the event detail screen.
    *   Intuitive date and time pickers for seamless event scheduling.

*   **🗺️ Interactive Map (UniMaps)**:
    *   Save personal Points of Interest (POIs) with names, addresses, and optional notes.
    *   Display all saved POIs on an interactive OpenStreetMap (OSMDroid) map.
    *   Utilize current device location or search for addresses to define new POIs.
    *   Get quick directions to any saved POI using external map applications.
    *   Delete saved POIs easily from the map or list view.

*   **🍳 Recipe Discovery (UniChef)**:
    *   Search for a vast array of recipes using the Spoonacular API.
    *   View comprehensive recipe details including ingredients, step-by-step instructions, preparation time, and servings.
    *   Mark and manage favorite recipes for quick access.
    *   Share delicious recipes with friends and family.

## 🛠️ Tech Stack

UniSphere-App is built using modern and robust technologies:

*   **Primary Language**: Kotlin
*   **UI Framework**: Jetpack Compose
*   **Backend Communication**: Ktor Client
*   **Dependency Injection**: Dagger Hilt
*   **Serialization**: Kotlinx.serialization (JSON)
*   **Local Secure Storage**: EncryptedSharedPreferences (for JWT tokens)
*   **Mapping**: OSMDroid (OpenStreetMap library), Google Location Services
*   **Image Loading**: Coil
*   **External APIs**: Spoonacular API (for recipe data)
*   **Testing**: JUnit, AndroidJUnit4

## 🚀 Installation

To get UniSphere-App up and running on your local machine, follow these steps:

### Prerequisites

*   **Android Studio**: Ensure you have the latest version installed.
*   **Kotlin Plugin**: Installed and up-to-date in Android Studio.
*   **Java Development Kit (JDK)**: Version 17 or higher.
*   **Android Device/Emulator**: An Android device or emulator running API Level 21+.

### Steps

1.  **Clone the Repository**:

    ```bash
git clone https://github.com/steppo04/Unisphere-App.git
cd Unisphere-App
    ```

2.  **Open in Android Studio**:

    *   Launch Android Studio.
    *   Select `File > Open...` and navigate to the cloned `Unisphere-App` directory.
    *   Android Studio will automatically import the project and sync Gradle files.

3.  **Configure API Keys (Optional but Recommended)**:

    *   The Spoonacular API key is currently hardcoded in `app/src/main/java/com/example/unisphere/repository/RecipeRepository.kt`.
        ```kotlin
        private val spoonacularApiKey = "e09429ff9b1c4c3ca2e6e4318890b313"
        ```
        For production or if the key expires, you may want to replace it or move it to `local.properties` for better security.

4.  **Backend URL Configuration**:

    *   The application communicates with a Ktor backend. The base URL is hardcoded in `app/src/main/java/com/example/unisphere/db/dbUtils.kt` and `app/src/main/java/com/example/unisphere/db/local/ApiClient.kt`:
        ```kotlin
        url("https://api.unisphere-db.it/")
        private const val BASE_URL = "https://api.unisphere-db.it"
        ```
        If you are running a custom backend, you will need to update this URL to point to your Ktor server instance.

5.  **Build and Run**:

    *   Connect an Android device or start an emulator.
    *   Click the `Run` button (green triangle ▶️) in Android Studio to build and install the app on your selected device/emulator.

### Permissions

*   The app requires location permissions for UniMaps and UniCalendar features (e.g., `ACCESS_FINE_LOCATION`). Ensure these are granted on your device when prompted.

## 💡 How to Use

Upon launching UniSphere-App, you will be greeted by the authentication screen.

1.  **🚪 Account Creation/Login**:
    *   **Sign Up**: If you're a new user, tap "Crea account" to register with your name, surname, username, email, and password. You can also add a profile picture.
    *   **Login**: If you already have an account, enter your email and password to access the app.

2.  **🧭 Navigation**:
    *   The app features a **Bottom Navigation Bar** that allows you to seamlessly switch between the main modules:
        *   **UniHome** 🏠: House management.
        *   **UniWallet** 💸: Personal finance.
        *   **UniCalendar** 🗓️: Event scheduling.
        *   **UniChef** 🍳: Recipe discovery.
        *   **UniMaps** 🗺️: Points of Interest.
    *   The **Top AppBar** provides context-sensitive titles and quick access to your profile settings.

3.  **👤 Profile Management**:
    *   Tap your profile icon in the top-right corner of the AppBar to go to the `Impostazioni` screen.
    *   Here, you can update your profile picture, change your username or email, and switch between light, dark, or system-default themes.

4.  **🏡 UniHome (House Management)**:
    *   **Create a House**: If you don't have a house, you can create a new one by providing a name.
    *   **Join a House**: Accept invitations from other users to join their UniHome.
    *   **Manage Members**: Invite new roommates by searching their usernames. As an admin, you can remove members.
    *   **Cleaning Rotations**: Add new cleaning services (e.g., "Kitchen", "Bathroom") and track their completion status weekly.
    *   **Shared Expenses**: Record group expenses by providing a title, total amount, and selecting members to split the cost with. The app automatically calculates individual shares.
    *   **Balances**: View a summary of who owes whom and their net amounts. You can also initiate direct settlements.

5.  **💸 UniWallet (Personal Finance)**:
    *   **Add Transaction**: Tap the `+` Floating Action Button to add new income or expense transactions.
        *   Specify title, amount, category, and date.
        *   Create new custom categories with unique colors.
    *   **Overview**: The dashboard provides a summary of your net balance, total incomes, and expenses.
    *   **Visualizations**: Explore the Pie Chart for expense breakdown and the Line Chart for your balance trend over the last month.
    *   **Filtering**: Use the filter panel to narrow down transactions by category, type (income/expense), and amount range.

6.  **🗓️ UniCalendar (Event Management)**:
    *   **Add Event**: Use the `+` Floating Action Button to create a new event. Provide a title, date, start/end times, location, and assign it to a calendar.
        *   You can create new calendar types with custom colors.
        *   For location, you can type an address, use your current GPS location, or pick from suggestions.
    *   **View Events**: The calendar displays events for the selected date. Tap on an event to view its details.
    *   **Event Details**: View event information, including a map showing the event location (if available). You can also edit or delete events from this screen.

7.  **🗺️ UniMaps (Points of Interest)**:
    *   **Add POI**: Tap the `+` Floating Action Button to add a new point of interest.
        *   Provide a name, address, and optional notes.
        *   Use your current location or type an address to find coordinates.
    *   **View POIs**: All your saved POIs are displayed as markers on the interactive OpenStreetMap. Tapping a marker shows a compact card with details.
    *   **Directions**: From the POI card, you can get directions to the location using an external map app.
    *   **Manage POIs**: Delete saved POIs from the list or detail view.

8.  **🍳 UniChef (Recipe Discovery)**:
    *   **Search Recipes**: Use the search bar to find recipes by ingredients or dish names.
    *   **Recipe Details**: Tap on any recipe to view its detailed information, including ingredients, cooking instructions, and nutritional facts.
    *   **Favorites**: Mark recipes as favorites to save them for quick access later. Access your favorite recipes from the dedicated "Ricette Preferite" card.
    *   **Share**: Share interesting recipes with others.

## 📂 Project Structure

The project follows a clean architecture pattern, organizing code into distinct layers for maintainability and scalability:

```
Unisphere-App/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── androidTest/
│       │   └── java/com/example/unisphere/ExampleInstrumentedTest.kt
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/example/unisphere/
│       │   │   ├── Application.kt               # Dagger Hilt application setup
│       │   │   ├── MainActivity.kt              # Main activity, handles navigation and theming
│       │   │   ├── db/                        # Data layer - network models, token management
│       │   │   │   ├── TokenManager.kt          # Manages secure JWT token storage
│       │   │   │   ├── dbUtils.kt               # Ktor HttpClient setup with Dagger Hilt
│       │   │   │   ├── local/ApiClient.kt       # Ktor client configuration with Auth plugin
│       │   │   │   └── network/                 # Data Transfer Objects (DTOs) for all features
│       │   │   │       ├── CalendarModels.kt
│       │   │   │       ├── HouseModels.kt
│       │   │   │       ├── PoiModels.kt
│       │   │   │       ├── RecipeModels.kt
│       │   │   │       ├── UserModels.kt
│       │   │   │       └── WalletModels.kt
│       │   │   └── repository/                # Repository layer - API abstraction
│       │   │       ├── EventRepository.kt
│       │   │       ├── HouseRepository.kt
│       │   │       ├── PoiRepository.kt
│       │   │       ├── RecipeRepository.kt
│       │   │       ├── UserRepository.kt
│       │   │       └── WalletRepository.kt
│       │   │   └── ui/                        # User Interface layer - Jetpack Compose screens, viewmodels, etc.
│       │   │       ├── composables/           # Reusable UI components
│       │   │       │   ├── AppBar.kt
│       │   │       │   ├── NavGraph.kt
│       │   │       │   ├── NavigationBar.kt
│       │   │       │   └── UniSphereComposables.kt
│       │   │       ├── screen/                # Feature-specific screens and ViewModels
│       │   │       │   ├── accessScreen/        # Login, Sign In, Reset Password
│       │   │       │   ├── calendar/            # Event management, calendar view
│       │   │       │   ├── cook/                # Recipe search, favorites, detail
│       │   │       │   ├── map/                 # POI management, map display
│       │   │       │   ├── profile/             # User profile settings
│       │   │       │   ├── uniHome/             # House management, shared expenses, cleaning
│       │   │       │   └── wallet/              # Personal finance, transactions, charts
│       │   │       ├── theme/                   # App theming (colors, typography)
│       │   │       └── utils/ImageUtils.kt      # Utility for image selection
│       │   └── res/                         # Android resources (drawables, layouts, values, etc.)
│       └── test/
│           └── java/com/example/unisphere/ExampleUnitTest.kt
├── gradle/
├── .idea/
├── settings.gradle.kts
└── README.md
```

## 🌐 API Reference

UniSphere-App interacts with custom Ktor backend services and external APIs for its functionalities.

*   **Internal Ktor Backend API**: The application communicates with a dedicated backend hosted at `https://api.unisphere-db.it/`.
    *   All user-related data (authentication, profile), house management, wallet transactions, calendar events, and Points of Interest are managed through this backend.
    *   The API endpoints and data models (DTOs) are defined in `app/src/main/java/com/example/unisphere/db/network/` and utilized by the repository classes in `app/src/main/java/com/example/unisphere/repository/`.
    *   Authentication is handled via JWT tokens, securely stored using `EncryptedSharedPreferences` and automatically attached to requests by the Ktor HttpClient setup in `dbUtils.kt` and `ApiClient.kt`.

*   **Spoonacular API**: For recipe discovery and details, the app integrates with the Spoonacular API.
    *   Base URL: `https://api.spoonacular.com/recipes`
    *   An API key is required and currently hardcoded within `RecipeRepository.kt`.
    *   Endpoints used include `/complexSearch` for searching recipes and `/{id}/information` for fetching detailed recipe information.
    *   For more details, refer to the [Spoonacular API Documentation](https://spoonacular.com/api/docs).

## 🤝 Contributing

We welcome contributions to UniSphere-App! If you're interested in improving the app, please consider the following:

1.  **Fork** the repository.
2.  **Create** a new branch (`git checkout -b feature/AmazingFeature`).
3.  **Implement** your changes.
4.  **Commit** your changes (`git commit -m 'Add some AmazingFeature'`).
5.  **Push** to the branch (`git push origin feature/AmazingFeature`).
6.  **Open** a Pull Request.

Please ensure your code adheres to Kotlin coding conventions and includes appropriate tests.

## 📄 License

No specific license information was provided in the repository. Please contact the repository owner for licensing details.

## 🔗 Important Links

*   **GitHub Repository**: [https://github.com/steppo04/Unisphere-App](https://github.com/steppo04/Unisphere-App)
*   **Backend API**: [https://api.unisphere-db.it/](https://api.unisphere-db.it/)
*   **Spoonacular API Documentation**: [https://spoonacular.com/api/docs](https://spoonacular.com/api/docs)

<footer>
<h2 align="center">Star ⭐, Fork 🍴 and Contribute 🚀!</h2>
<p align="center">
    A project by steppo04
    <br/>
    Feel free to reach out via GitHub issues or discussions.
</p>
</footer>


---
**<p align="center">Generated by [ReadmeCodeGen](https://www.readmecodegen.com/)</p>**