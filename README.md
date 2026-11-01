# Tradewind — профессии, статистика и скупщик для Paper

Оригинальный плагин (не форк и не копия чужих исходников) с механикой профессий в стиле
ReallyWorld: пять профессий, регионы WorldGuard, зарплата через Vault, уровни, бусты,
GUI-скупщик, авто-скупка, магазин уникальных крафтов, защита от эксплойтов, PlaceholderAPI и TAB.

Целевые линейки: **Paper 1.21.x** и **Paper 26.1.x**. Folia не поддерживается — используется
только обычный Bukkit Scheduler.

## Быстрый старт

```bash
./gradlew build
```

Один jar — ровно под вашу линейку:

| Артефакт | Для сервера | Байткод | API компиляции |
|---|---|---|---|
| `core/build/libs/Tradewind-1.0.0-SNAPSHOT-paper-1.21.jar` | Paper 1.21.x | Java 21 (major 65) | `paper-api:1.21.1-R0.1-SNAPSHOT` |
| `paper-26/build/libs/Tradewind-1.0.0-SNAPSHOT-paper-26.1.jar` | Paper 26.1.x | Java 25 (major 69) | `paper-api:26.1.2.build.74-stable` |

Ставится **один** jar: модули содержат одинаковый `plugin.yml` и один класс
`com.tradewind.TradewindPlugin`, поэтому вместе они конфликтуют по имени плагина.

Минимальная настройка после установки:

1. `plugins/Tradewind/config.yml` — включить профессии, при желании поменять имена регионов.
2. `plugins/Tradewind/prices.yml` — цены скупщика (по умолчанию заполнено 66 предметами).
3. Создать регионы WorldGuard с именами из конфига (`tw_mine`, `tw_farm`, `tw_wood`, `tw_fish`,
   `tw_snow`) — награда начисляется только внутри региона профессии.
4. Игрок выбирает профессию: `/prof mine`.

## Возможности по этапам

| Этап | Что реализовано | Где |
|---|---|---|
| 1 | каркас: сборка, `plugin.yml`, конфиги с версиями, `/tradewind reload`, проверка зависимостей | `bootstrap/`, `config/`, `TradewindPlugin` |
| 2 | добыча по профессиям внутри регионов WorldGuard (`mine`, `farm`, `wood`, `fish`, `snow`) | `profession/`, `listener/`, `region/` |
| 3 | SQLite (по умолчанию) или MySQL/MariaDB, статистика, зарплата через Vault, топы | `storage/`, `profession/ProfessionService` |
| 4 | GUI скупщика: оценка инвентаря, продажа всего/в руке, атомарная сделка | `buyer/`, `gui/BuyerGui` |
| 5 | уровни профессий, прогресс, множитель зарплаты, награды за уровни | `config/LevelCurve`, `/prof` |
| 6 | PlaceholderAPI-расширение и плейсхолдеры TAB | `integration/` |
| 7 | бусты денег/опыта с покупкой и продлением, авто-скупка | `boost/`, `gui/BoostGui`, `/autosell` |
| 8 | магазин уникальных крафтов с PDC-метками и требованиями по уровню | `shop/`, `gui/ShopGui`, `shop.yml` |
| 9 | защита от эксплойтов: лимиты, place-break, креатив, повторная продажа, атомарность | `anti/`, `profession/`, `buyer/` |
| 10 | документация, юнит-тесты, проверка на реальных серверах обеих линеек | этот файл, `docs/` |

## Совместимость 1.21.x / 26.1.x

Два модуля — это не две кодовые базы, а **две точки сборки одних и тех же исходников**:

* `:core` компилирует `core/src/main/java` + `core/src/platform121/java` под Java 21 против
  **нижней границы** линейки 1.21 (`paper-api:1.21.1`). Компиляция против нижней границы, а не
  против последней версии, гарантирует отсутствие в jar API, которого нет на 1.21.1.
* `:paper-26` компилирует те же общие исходники, подменяя только `com.tradewind.platform.PlatformSupport`,
  под Java 25 против `paper-api:26.1.2`. Любая правка общих классов сразу проверяется
  компилятором на обеих линейках.

**`api-version: '1.21'`** — Paper трактует его как нижнюю границу: значение `1.21` грузится и на
1.21.x, и на 26.x, тогда как `26.1` запретил бы загрузку на 1.21.x. Допустимый диапазон значений —
1.13…26.3 ([docs.papermc.io](https://docs.papermc.io/paper/dev/plugin-yml/)), то есть одно значение
покрывает обе цели. Ограничение: `paper-26` собран против 26.1.2; 26.2/26.3 не заявлены.

Третьи стороны скомпилированы против Java 21-совместимых версий API (WorldGuard 7.0.17,
WorldEdit 7.4.2), потому что 7.0.18+ публикуются как Java 25-байткод. API регионов стабилен,
на сервере может стоять более новая WorldGuard.

## Дерево проекта

```
.
├── settings.gradle.kts        репозитории, toolchain-resolver, модули
├── build.gradle.kts           общие настройки подпроектов
├── gradle.properties
├── gradlew, gradlew.bat, gradle/wrapper/
├── core/                      Java 21, paper-api 1.21.1
│   ├── build.gradle.kts       sqlite-jdbc упакован в jar
│   └── src/
│       ├── main/java/com/tradewind/
│       │   ├── TradewindPlugin.java
│       │   ├── anti/          ActionLimiter, PlacedBlockTracker
│       │   ├── boost/         BoostService
│       │   ├── bootstrap/     PluginContext, Dependency, DependencyReport, PluginStartupException
│       │   ├── buyer/         BuyerService
│       │   ├── command/       TradewindCommand, ProfessionCommand, BuyerCommand,
│       │   │                  AutoSellCommand, BoostCommand, ShopCommand
│       │   ├── config/        MainConfig, Messages, YamlFile, LevelCurve, PriceIndex,
│       │   │                  ShopCatalog, BoostDefinition, StorageSettings, ...
│       │   ├── economy/       EconomyService, VaultEconomyService, UnavailableEconomy
│       │   ├── gui/           Gui, GuiListener, BuyerGui, ShopGui, BoostGui
│       │   ├── integration/   PlaceholderHook (PAPI), TabHook (TAB)
│       │   ├── item/          CustomItems (PDC-метка магазина)
│       │   ├── listener/      BlockListener, FishingListener, SessionListener
│       │   ├── model/         Profession, PlayerData, ProfessionStats, ActiveBoost, ...
│       │   ├── platform/      Platform, Platforms, ServerLine
│       │   ├── profession/    ProfessionService, Reward, BlockOutcome
│       │   ├── region/        RegionService, WorldGuardRegions, DisabledRegions
│       │   ├── service/       LeaderboardService, ProfessionSelectionService
│       │   ├── shop/          ShopService
│       │   ├── storage/       Storage, JdbcStorage, PlayerRepository, DbExecutor, PlayerSnapshot
│       │   ├── text/          Text (MiniMessage + legacy + {placeholders})
│       │   └── util/          Numbers, TimeUtil
│       ├── main/resources/    plugin.yml, config.yml, messages.yml, prices.yml,
│       │                      boosts.yml, shop.yml
│       ├── platform121/java/  PlatformSupport для 1.21
│       └── test/java/         TextTest, LevelCurveTest, FormattingTest, JdbcStorageTest
└── paper-26/
    ├── build.gradle.kts
    └── src/main/java/com/tradewind/platform/PlatformSupport.java
```

Всего 75 Java-файлов; полный код каждого файла — в репозитории.

## Зависимости

| Плагин | Тип | Поведение, если нет |
|---|---|---|
| Vault + провайдер экономики | обязательный | `fail-fast: true` — плагин выключается с понятным сообщением; `false` — экономика недоступна, остальное работает |
| PlaceholderAPI | обязательный | то же; расширение `tradewind` не регистрируется |
| WorldGuard | обязательный | то же; награды профессий не начисляются (нет регионов) |
| TAB | мягкий | в консоль уходит предупреждение, функция отключена |

Все четыре объявлены как `softdepend`: с `depend` сервер молча не загрузил бы Tradewind, и
никакого «понятного сообщения» администратор бы не увидел. Проверка идёт в `DependencyReport`,
а классы, ссылающиеся на Vault/WorldGuard/TAB, загружаются только после успешной проверки и
дополнительно обёрнуты в `catch (LinkageError)` — `NoClassDefFoundError` исключён.

## Команды и права

| Команда | Право | Назначение |
|---|---|---|
| `/tradewind help` (`/tw`) | `tradewind.use` (все) | справка, сборка и версия сервера |
| `/tradewind status` | `tradewind.admin.status` (op) | линейка, зависимости, экономика, версии схем |
| `/tradewind reload` | `tradewind.admin.reload` (op) | перечитать конфиги, пересчитать зависимости |
| `/tradewind top [проф.]` | `tradewind.use` | топ профессии по действиям |
| `/tradewind stats [игрок]` | `tradewind.admin.stats` (op) | статистика игрока по всем профессиям |
| `/tradewind boost <игрок> <money\|xp> [30m]` | `tradewind.admin.boost` (op) | выдать буст без оплаты |
| `/prof [профессия]` | `tradewind.profession` (все) | статистика и выбор профессии |
| `/buyer` (`/sell`) | `tradewind.buyer` (все) | GUI скупщика |
| `/autosell [on\|off]` | `tradewind.autosell` (все) | авто-скупка |
| `/boost` | `tradewind.boost` (все) | GUI бустов |
| `/shop` (`/crafts`) | `tradewind.shop` (все) | магазин уникальных крафтов |
| — | `tradewind.bypass` (false) | не получать награды (тесты, персонал) |

## Конфигурация

### `config.yml` (version 2)

* `general.debug` — подробный лог (включая причины пропуска наград).
* `dependencies.fail-fast` / `warn-on-missing-optional` — режим при отсутствии зависимостей.
* `economy.currency-name`, `economy.round-to` (0–4 знака).
* `storage` — `sqlite` (файл `tradewind.db`) или `mysql`/`mariadb`; `flush-interval-seconds`.
  Драйвер MariaDB упакован и работает с MySQL; смена типа применяется после перезапуска.
* `professions.allow-change`, `change-cooldown`; у каждой профессии — `enabled`, `display`,
  `region`, `salary-per-action`, `xp-per-action`, `blocks` (пусто = безопасный набор:
  камень/руды, брёвна по тегу, зрелые культуры, снег/лёд).
* `anti-exploit` — см. ниже.
* `levels` — `max-level`, `base-xp`, `exponent`, `salary-per-level`, `rewards`.
* `buyer` — `enabled`, `rows`, `title`, `cooldown`, `autosell-default`.

### `messages.yml` (version 1)

Все тексты; MiniMessage и legacy `&` можно смешивать в одной строке, `{ключ}` подставляется
плагином. Ваш файл никогда не перезаписывается ради новых ключей: если ключа в нём ещё нет,
берётся значение из jar (в консоль уходит одна строка об этом), поэтому комментарии и правки
остаются нетронутыми. При несовпадении версии схемы файл пересоздаётся, а старая версия
сохраняется как `*.bak-vN`.

### `prices.yml` (version 1)

`items: <MATERIAL>: <цена за штуку>`. Используется и скупщиком, и авто-скупкой. Предметы вне
списка не принимаются, а предметы из магазина не продаются никогда (PDC-метка).

### `boosts.yml` (version 1)

`types.money` / `types.xp`: `display`, `multiplier`, `duration` (`30m`, `2h`, `90s`), `price`,
`permission`, `material` (иконка). Покупка продлевает активный буст того же типа.

### `shop.yml` (version 1)

`items.<id>`: `material`, `amount`, `display`, `lore`, `price`, `profession`, `required-level`,
`enchantments` (современные ключи `efficiency`, поддерживаются и legacy `DIG_SPEED`),
`unbreakable`. Выданный предмет помечается `tradewind:shop_item`.

## Данные и хранилище

Все операции идут на отдельном потоке `Tradewind-DB`; серверный поток никогда не ждёт SQL.
Профили кэшируются в памяти, грязные записи сбрасываются раз в `flush-interval-seconds`,
на выходе игрока и при выключении сервера.

```
tw_players(uuid PK, name, profession, autosell, updated_at)
tw_stats(uuid, profession, actions, money, xp, level, PK(uuid, profession))
tw_boosts(uuid, type, multiplier, expires, PK(uuid, type))
```

SQLite: `ON CONFLICT … DO UPDATE`, MySQL/MariaDB: `ON DUPLICATE KEY UPDATE`; запись профиля
транзакционная (игрок + все профессии + бусты). Топы читаются с `ORDER BY actions DESC LIMIT ?`
и кэшируются на 5 минут.

## Защита от эксплойтов

* **Регион обязателен**: награда только в регионе профессии, совпадающей с выбранной у игрока.
* **Place-break**: блоки, поставленные игроком, не оплачиваются (TTL 15 минут). Для `farm`
  вместо этого проверяется зрелость культуры — иначе фермер не смог бы собирать собственный урожай.
* **Лимиты**: не чаще одного начисления в 75 мс и не более 8 в секунду на игрока.
* **Креатив/наблюдение** не оплачиваются; право `tradewind.bypass` снимает игрока с начислений.
* **Атомарная продажа**: предметы изымаются, баланс начисляется, Vault-операция в одну сторону;
  при ошибке инвентарь восстанавливается из резервной копии.
* **Повторная продажа** предметов магазина невозможна (PDC-метка), скупщик их просто не видит.
* **Пауза скупщика** 1.5 с и не более 20 продаж в минуту.

## PlaceholderAPI

Идентификатор расширения — `tradewind`.

| Плейсхолдер | Значение |
|---|---|
| `%tradewind_profession%` | название выбранной профессии |
| `%tradewind_level%`, `%tradewind_xp%`, `%tradewind_xp_next%`, `%tradewind_progress%` | уровень и опыт выбранной профессии |
| `%tradewind_actions%`, `%tradewind_money%` | действия и заработок выбранной профессии |
| `%tradewind_<prof>_level\|xp\|actions\|money\|progress%` | то же по конкретной профессии |
| `%tradewind_total_money%`, `%tradewind_total_actions%` | суммы по всем профессиям |
| `%tradewind_autosell%` | `вкл` / `выкл` |
| `%tradewind_boost_money%`, `%tradewind_boost_money_time%`, `%tradewind_boost_xp%`, `%tradewind_boost_xp_time%` | множитель и остаток буста |
| `%tradewind_top_<prof>_name\|actions\|money\|level_<N>%` | место N в топе |

## TAB

При наличии TAB плагин регистрирует `%tradewind_profession%`, `%tradewind_level%`,
`%tradewind_actions%`, `%tradewind_money%`, `%tradewind_boost%` (обновление 500 мс). Готовый
фрагмент scoreboard — `docs/tab-scoreboard.yml`.

## Допущения и вопросы ко мне

1. **Имя/пакет** `Tradewind` / `com.tradewind`. Меняется дешевле сейчас, чем позже.
2. **Одна активная профессия.** Игрок выбирает `mine|farm|wood|fish|snow`; награды только за неё.
   Если нужны все пять одновременно — это правка `ProfessionService.resolveProfession`.
3. **Смена профессии** по умолчанию разрешена раз в час; кулдаун хранится в памяти и сбрасывается
   перезапуском (в БД колонки нет). Если нужен «вечный» кулдаун — добавлю колонку.
4. **Нижняя граница 1.21.1**, не 1.21.0.
5. **Экономика — один общий баланс Vault**, без мировых кошельков.
6. **Регионы**: одно имя на профессию. Нужны ли несколько регионов на профессию или регионы по мирам?
7. **Скупщик** — GUI по команде. Нужен ли NPC/житель?
8. **Авто-скупка** платит за дропы и не выдаёт предметы; непродаваемые дропы падают на землю.
9. **Уровни**: порог = `base-xp * level^exponent`, множитель зарплаты линейный. Формулу можно
   заменить на таблицу, если нужна точная копия конкретного сервера.

## Что проверить на сервере 1.21.x и 26.1.x

1. **1.21.x**: jar `paper-1.21` на 1.21.1 и на последней 1.21.x — убедиться, что нет расхождений API.
2. **26.1.x**: jar `paper-26.1` на 26.1.2 — Paper принимает `api-version: '1.21'`, `/tw status`
   показывает `✔ соответствует серверу`.
3. **Vault**: с EssentialsX `/tw status` показывает backend и баланс; без Vault в консоли нет
   `NoClassDefFoundError`, есть читаемое сообщение.
4. **WorldGuard**: создать регионы `tw_mine` и т.д.; добыча внутри даёт награду, снаружи — нет.
5. **Права**: обычный игрок не видит `status`/`reload` (в том числе в tab-complete).
6. **Схемы конфигов**: поставить `version: 1` → `/tw reload` создаёт `config.yml.bak-v1` и
   дополняет файл; `version: 99` — файл не трогается.
7. **MySQL**: переключить `storage.type`, проверить запись/чтение и топы на реальной БД.
8. **TAB**: вставить scoreboard из `docs/tab-scoreboard.yml`, проверить обновление значений.
9. **Нагрузка**: 100+ игроков в регионе — смотреть TPS и размер очереди записи.

## Чек-лист этапов

- [x] 1. Каркас: сборка, `plugin.yml`, конфиги с версиями, `/tw reload`, проверка зависимостей
- [x] 2. Лесорубка и регионы WorldGuard, события добычи по пяти профессиям
- [x] 3. Хранилище (SQLite/MySQL/MariaDB), статистика, зарплата через Vault, топы
- [x] 4. GUI скупщика: оценка, продажа всего/в руке, атомарная сделка
- [x] 5. Уровни, прогресс, множитель зарплаты, награды за уровни
- [x] 6. PlaceholderAPI-расширение и плейсхолдеры TAB
- [x] 7. Бусты денег/опыта и авто-скупка
- [x] 8. Магазин уникальных крафтов с PDC-метками
- [x] 9. Защита от эксплойтов: регион, лимиты, place-break, креатив, повторная продажа
- [x] 10. Документация, 21 юнит-тест, проверка на Paper 1.21.1 и 26.1.2

Подробный журнал проверки с логами и цифрами — `docs/verification.md`.

