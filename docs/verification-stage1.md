# Журнал проверки этапа 1

Окружение: Gradle 9.8.2, Temurin JDK 21.0.13 и 25.0.5, Paper `1.21.1-133` и `26.1.2-74`,
`paper-api:1.21.1-R0.1-SNAPSHOT` и `paper-api:26.1.2.build.74-stable`.

## Сборка

```
./gradlew build  -> BUILD SUCCESSFUL
core:test        -> tests=6 failures=0 errors=0
core jar         -> 17 классов, major 65 (Java 21), plugin.yml version=1.0.0-SNAPSHOT
paper-26 jar     -> 17 классов, major 69 (Java 25), тот же plugin.yml
```

## Paper 1.21.1-133, fail-fast: true

```
[Tradewind] Loading server plugin Tradewind v1.0.0-SNAPSHOT
[Tradewind] Tradewind v1.0.0-SNAPSHOT: сервер 1.21.1 [1.21.x], сборка Paper 1.21.x, Java 21,
            профессии [mine, farm, wood, fish, snow]
[Tradewind] Нет обязательных зависимостей: Vault, PlaceholderAPI, WorldGuard
[Tradewind] Мягкие зависимости не найдены: TAB (не критично).
[Tradewind] Экономика недоступна: Vault или его провайдер не найдены.
[Tradewind] Tradewind выключен: отсутствуют обязательные зависимости — Vault, PlaceholderAPI,
            WorldGuard. Установите их или включите dependencies.fail-fast=false в config.yml
            для запуска в урезанном режиме.
[Tradewind] Disabling Tradewind v1.0.0-SNAPSHOT
```

Созданы `plugins/Tradewind/config.yml` и `plugins/Tradewind/messages.yml`.

## Paper 1.21.1-133, fail-fast: false

```
[Tradewind] Плагин запущен в урезанном режиме: отсутствуют Vault, PlaceholderAPI, WorldGuard.
[Tradewind] —— Tradewind v1.0.0-SNAPSHOT ——
[Tradewind] /tradewind help — справка
[Tradewind] /tradewind status — состояние зависимостей и сборки
[Tradewind] /tradewind reload — перезагрузить конфиги и сообщения
[Tradewind] Сборка: Paper 1.21.x, Java 21, сервер: 1.21.1
[Tradewind] —— Состояние Tradewind ——
[Tradewind] Версия: 1.0.0-SNAPSHOT
[Tradewind] Сервер: 1.21.1 (1.21.x)
[Tradewind] Сборка: Paper 1.21.x, Java 21 ✔ соответствует серверу
[Tradewind] ✘ Vault — обязателен: экономика, баланс, сдача предметов и зарплата
[Tradewind] ✘ PlaceholderAPI — обязателен: плейсхолдеры в текстах и scoreboard
[Tradewind] ✘ WorldGuard — обязателен: регионы профессий
[Tradewind] • TAB — необязателен: scoreboard-профили игроков
[Tradewind] Экономика недоступна: Vault не найден или провайдер не зарегистрирован.
[Tradewind] Схемы файлов: config v1, messages v1
[Tradewind] Конфиги и сообщения перезагружены. Зависимости: нет обязательных: Vault,
            PlaceholderAPI, WorldGuard; нет мягких: TAB.
[Tradewind] Неизвестная подкоманда bogus. Используйте /tradewind help.
```

## Paper 26.1.2-74, JDK 25, jar paper-26.1

```
[Tradewind] Tradewind v1.0.0-SNAPSHOT: сервер 26.1.2 [26.1.x], сборка Paper 26.1.x, Java 25,
            профессии [mine, farm, wood, fish, snow]
[Tradewind] Сервер: 26.1.2 (26.1.x)
[Tradewind] Сборка: Paper 26.1.x, Java 25 ✔ соответствует серверу
[Tradewind] Схемы файлов: config v1, messages v1
[Tradewind] Конфиги и сообщения перезагружены. ...
```

`api-version: '1.21'` принят сервером 26.1.2 без предупреждений.

## Что осталось непроверенным

* Paper 1.21.0, 1.21.11, 26.2, 26.3 — не запускались.
* Vault, PlaceholderAPI, WorldGuard, TAB — не устанавливались; проверен путь «нет зависимости»
  и изоляция Vault-класса от загрузки. Успешный путь Vault — на стороне заказчика.
* Права обычного игрока и tab-complete проверены по коду, а не в клиенте.
* Обновление схем (`version: 0` / `version: 99`) на сервере не прогонялось.

