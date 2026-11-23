# Smart Assistant («Штучний помічник»)

Android-застосунок на **Kotlin**, який слухає голосові команди й виконує прості завдання.
Мови інтерфейсу, розпізнавання та озвучення: 🇬🇧 English · 🇺🇦 Українська · 🇸🇰 Slovenčina.

## Що вміє

| Команда (приклад, укр.)          | Дія                                              |
|----------------------------------|--------------------------------------------------|
| «Привіт»                         | вітання                                          |
| «Котра година?»                  | називає поточний час                             |
| «Яка сьогодні дата?»             | називає дату                                     |
| «Який заряд батареї?»            | рівень заряду й чи йде зарядка                   |
| «Відкрий камеру»                 | запускає застосунок камери                       |
| «Постав таймер на 5 хвилин»      | системний таймер (також «на 2 години»)           |
| «Знайди смішних котів»           | веб-пошук                                        |
| «Допомога»                       | перелік можливостей                              |

Англійські та словацькі фрази — у `res/values/commands.xml` і `res/values-sk/commands.xml`.

## Дві версії проєкту

| Каталог        | Що це                                                                                      |
|----------------|--------------------------------------------------------------------------------------------|
| `spaghetti/`   | Базовий прототип: усе в одному `MainActivity.kt`, глобальні змінні, hardcode, хаотичні логи |
| `final/`       | Фінальний варіант: шари, i18n, Timber, обробка помилок, тести                               |

Кожен каталог — **окремий, самодостатній Gradle-проєкт** (відкривайте в Android Studio саме його, а не корінь репозиторію).
Перелік «файл ↔ файл» і розбір відмінностей — у [`docs/SPAGHETTI_VS_FINAL.md`](docs/SPAGHETTI_VS_FINAL.md).
Повна схема файлів — у [`docs/REPOSITORY_STRUCTURE.md`](docs/REPOSITORY_STRUCTURE.md).

## Структура репозиторію (скорочено)

```
smart-assistant/
├── .gitignore  .gitattributes  README.md
├── docs/                         ← документація
│   ├── REPOSITORY_STRUCTURE.md      повна схема файлів
│   └── SPAGHETTI_VS_FINAL.md        порівняння двох версій
├── spaghetti/                    ← версія 1: «спагеті»
│   └── app/src/main/{AndroidManifest.xml, java/.../MainActivity.kt, res/layout/}
└── final/                        ← версія 2: фінальна
    └── app/src/
        ├── main/java/com/example/smartassistant/
        │   ├── SmartAssistantApp.kt      Application: налаштування логера
        │   ├── logging/                  ReleaseTree
        │   ├── i18n/                     StringProvider, LanguageManager
        │   ├── command/                  Command, CommandParser, CommandExecutor
        │   ├── voice/                    VoiceRecognizer, SpeechSynthesizer, VoiceError
        │   └── ui/                       MainActivity, MainViewModel, ChatAdapter, ChatModels
        ├── main/res/                     layout, drawable, values{,-uk,-sk}, xml/locales_config
        └── test/                         CommandParserTest
```

## Архітектура фінальної версії

```
 мікрофон ─► VoiceRecognizer ─► MainActivity ─► MainViewModel ─► CommandParser ─► Command
                                     ▲                │                              │
                 SpeechSynthesizer ◄─┘  (speech)      └──────► CommandExecutor ◄─────┘
                 (озвучення)                                   (Intent'и, батарея, час)
                                     ▲
                       StateFlow<UiState> (чат + статус)
```

* **UI** (`ui/`) не знає про логіку команд, **ViewModel** не знає про мікрофон і динаміки.
* **Парсер** залежить лише від інтерфейсу `StringProvider` → тестується звичайним JUnit без емулятора.
* Команди — `sealed interface`: компілятор змушує обробити кожну нову команду в `when`.

## Логування (Timber)

Налаштовується в одному місці — `SmartAssistantApp.initLogging()`:

| Збірка  | Дерево               | Що потрапляє в Logcat                         |
|---------|----------------------|-----------------------------------------------|
| debug   | `Timber.DebugTree`   | усі рівні, тег = ім’я класу                   |
| release | `logging/ReleaseTree`| лише `WARN` і `ERROR` (+ місце для Crashlytics)|

Політика рівнів: `v` — деталі розбору; `d` — потік подій (старт розпізнавання, розпізнаний текст);
`i` — значущі події (команда, зміна мови); `w` — відновлювані проблеми (немає TTS-голосу, помилка розпізнавання);
`e` — збої (TTS не ініціалізовано, немає служби розпізнавання).
Розпізнані фрази — персональні дані, тому в release-збірці вони **не логуються**.

Використання: `Timber.d("Recognized: '%s'", text)`, `Timber.w(exception, "Message")`. Не використовуйте `println`/`Log.*` напряму.

## Інтернаціоналізація (i18n)

* Усі рядки — у ресурсах: `values/` (English, за замовчуванням), `values-uk/`, `values-sk/`.
* **Plurals** для граматично правильних форм («1 хвилину / 2 хвилини / 5 хвилин»).
* **Ключові фрази команд** теж локалізовані (`commands.xml`) — нова мова не потребує змін у Kotlin-коді.
* Мови розпізнавання мовлення й озвучення, формати дати/часу беруться з `R.string.locale_tag`.
* Перемикач мови в застосунку працює через **Per-app language API** (`AppCompatDelegate.setApplicationLocales`) +
  `locales_config.xml` (системні налаштування Android 13+) + `AppLocalesMetadataHolderService` (збереження вибору на Android 8–12).
* Назви мов показуються їхніми власними мовами (`translatable="false"`).

**Як додати мову (наприклад, польську):**
1. `res/values-pl/strings.xml` і `res/values-pl/commands.xml` (скопіюйте з `values/` і перекладіть; `locale_tag` = `pl-PL`);
2. `<locale android:name="pl"/>` у `res/xml/locales_config.xml`;
3. рядок `language_pl` у `values/language_names.xml` та запис у `LanguageManager.supported`.

## Збірка та запуск

Потрібно: Android Studio (поточна стабільна), JDK 17, Android SDK 35.

```bash
# відкрийте каталог final/ (або spaghetti/) як проєкт в Android Studio, або з терміналу:
cd final
gradle wrapper          # один раз: створює gradle-wrapper.jar (його не включено в репозиторій)
./gradlew :app:testDebugUnitTest
./gradlew :app:installDebug
```

Розпізнавання мовлення потребує сервісу (Google app / Speech Services) на пристрої; на емуляторі використовуйте образ із Google Play.
Дозвіл мікрофона запитується під час першого натискання на кнопку.

## Робота з Git

* Гілка `main`; повідомлення комітів — за [Conventional Commits](https://www.conventionalcommits.org/):
  `feat(scope)`, `fix(scope)`, `test(scope)`, `docs`, `chore`. Scope `spaghetti` / `final` вказує, яку версію змінено.
* `.gitignore` виключає `build/`, `.gradle/`, `.idea/`, `local.properties`, ключі підпису (`*.jks`, `keystore.properties`), `*.apk/*.aab`.
* `.gitattributes` нормалізує кінці рядків (LF).

Публікація у віддалений репозиторій:

```bash
git remote add origin https://github.com/<user>/smart-assistant.git
git push -u origin main
```

## Відомі обмеження

* Числа розпізнаються цифрами («5 хвилин»); словесні («п’ять хвилин») залежать від того, як їх видасть система розпізнавання.
* Привітальне повідомлення й попередні репліки чату не перекладаються заднім числом після зміни мови.
* Якщо на пристрої немає голосу TTS для вибраної мови — відповіді лишаються текстовими (у логах є `WARN`).
