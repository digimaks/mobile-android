# Digimaks

An Android implementation of a European Digital Identity Wallet for Latvia.
The project uses Kotlin, Jetpack Compose, and the [EUDI Wallet Core
SDK](https://github.com/eu-digital-identity-wallet/eudi-lib-android-wallet-core).

## Prerequisites

- Android Studio with Android SDK Platform 36
- JDK 17
- Git
- Node.js and pnpm, when building the embedded WebView application

The project includes the Gradle wrapper; no separate Gradle installation is
required.

## Local configuration

The repository contains no live service endpoints or OAuth client IDs. Copy the
example configuration and supply values for an environment you control:

```bash
cp environment.properties.example environment.properties
```

On Windows PowerShell, use:

```powershell
Copy-Item environment.properties.example environment.properties
```

`environment.properties` is ignored by Git. It defines configuration for the
`local`, `demo`, `dev`, `staging`, and `prod` flavors. Without it, builds use
non-routable `example.invalid` values.

## Build

Open the project root in Android Studio, allow Gradle sync to complete, choose a
build variant such as `localDebug`, then run it on a device.

From the command line:

```bash
./gradlew :app:assembleLocalDebug
```

On Windows, use `gradlew.bat` instead.

## Project structure

| Area | Purpose |
| --- | --- |
| `app` | Android application and release configuration |
| `assembly-logic` | Application composition and dependency injection |
| `auth-logic`, `business-logic`, `core-logic` | Domain, authentication, and wallet-core integration |
| `network-logic`, `storage-logic`, `analytics-logic` | Infrastructure services |
| `resources-logic`, `ui-logic`, `web-bridge` | Shared resources, UI, and WebView bridge |
| `features/` | User-facing wallet features |
| `build-logic` | Shared Gradle convention plugins |

## Build flavors

The available flavors are `local`, `demo`, `dev`, `staging`, and `prod`. They
all use the corresponding values from `environment.properties`.

## License

SPDX identifiers in comment-capable source files identify the applicable
license. This project is licensed under the [EUPL-1.2](LICENSE).

## Security

Please report vulnerabilities according to the [security policy](SECURITY.md).
