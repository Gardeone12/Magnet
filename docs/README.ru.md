# Magnet

Язык: [English](../README.md) | Русский

Magnet — Paper/Spigot-плагин для Minecraft, который добавляет переносной магнит и стационарные магнитные ядра. Переносной магнит притягивает выпавшие металлические предметы, пока игрок держит его в руке. Стационарные ядра — это структуры 2x2x2, которые притягивают предметы к своему центру.

## Возможности

- Переносной магнит выдаётся командой `/magnet give`.
- Переносной магнит работает из основной руки и из off-hand.
- Стационарные магнитные ядра 2x2x2 с настраиваемыми радиусом и силой.
- Радиус и сила ядра рассчитываются по восьми блокам материалов ядра.
- Профили материалов ядра настраиваются в `config.yml` и могут изменяться в игре.
- Вокруг ядра сканируется магнитный каркас с critical и optional блоками.
- Если сломать блок ядра или critical-блок каркаса, ядро отключится до ремонта или пересканирования.
- Диагностика ядра показывает состояние мира, чанков, каркаса, блоков ядра и ближайших магнитных предметов.
- Магнитные ядра сохраняются в `cores.yml` в папке данных плагина.
- Встроены английские и русские сообщения.
- `schematic/Magnet.litematic` содержит пример магнитной структуры.

## Требования

- Поддерживаемый диапазон Minecraft: `1.16.5 - 1.21.x`
- Bytecode плагина: Java 8
- JDK для сборки: Java 21
- Compile API: `org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT`
- Gradle Wrapper: 9.4.1

Для запуска используй Java, которую требует сам сервер. Современный Paper 1.20.5+ требует Java 21, Paper 1.18-1.20.4 обычно использует Java 17, а старым версиям нужен соответствующий legacy runtime.

## Совместимость

Плагин собирается в один universal jar:

`build/libs/Magnet-Universal-1.16.5-1.21.x.jar`

Общий код компилируется против Spigot API 1.16.5. Новые item-model API определяются через reflection, сообщения отправляются legacy-строками Bukkit, материалы разрешаются по имени с fallback, а команда объявлена в обычном `plugin.yml`.

В этом проекте 25 июня 2026 года выполнены runtime smoke-тесты:

- Paper 1.16.5 build 794
- Paper 1.21.11 build 132
- `/magnet`, `/magnet reload`, безопасная обработка player-only команд из консоли и `/magnet core list`
- чистое выключение плагина на обеих версиях

Код рассчитан на Paper/Spigot 1.16.5-1.21.x. Промежуточные версии, Spigot и Leaf перед production-развёртыванием нужно отдельно проверить на целевом сервере.

Поведение совместимости:

- Переносной магнит определяется через `PersistentDataContainer`, доступный в 1.16.5.
- На Minecraft 1.17+ используется `AMETHYST_SHARD`, на 1.16.5 — fallback `COMPASS`.
- Современные серверы используют item model/custom model data component API, если они доступны.
- Старые серверы используют integer custom model data `9001001`.
- Материалы, отсутствующие в текущей версии Minecraft, безопасно пропускаются.
- Версия ниже 1.16.5 отклоняется с понятной ошибкой, если сервер доходит до загрузки main-класса плагина.

## Команды

| Команда | Описание |
| --- | --- |
| `/magnet` | Показать помощь |
| `/magnet give` | Выдать игроку переносной магнит |
| `/magnet reload` | Перезагрузить конфиг плагина и магнитные ядра |
| `/magnet debug item` | Показать диагностику модели предмета в основной руке |
| `/magnet core create <id> [radius] [strength]` | Создать ядро по структуре 2x2x2, на которую смотрит игрок |
| `/magnet core createat <id> <x> <y> <z> [radius] [strength]` | Создать ядро по координатам в текущем мире игрока |
| `/magnet core createat <id> <world> <x> <y> <z> [radius] [strength]` | Создать ядро по координатам из консоли или в другом мире |
| `/magnet core remove <id>` | Удалить сохранённое ядро |
| `/magnet core list` | Показать сохранённые ядра и их состояние |
| `/magnet core info <id>` | Показать диагностику ядра |
| `/magnet core rescan <id>` | Пересканировать каркас вокруг ядра |
| `/magnet core refresh <id>` | Пересчитать ядро после замены материалов |
| `/magnet core override <id> <true\|false>` | Включить или выключить ручные radius и strength |
| `/magnet core set <id> <radius\|strength> <value>` | Изменить радиус или силу ядра и включить override |
| `/magnet core reload` | Перезагрузить ядра и настройки из конфига |
| `/magnet profile list` | Показать профили материалов ядра |
| `/magnet profile info <material>` | Показать один профиль материала |
| `/magnet profile set <material> <radius\|strength\|priority> <value>` | Изменить профиль материала и пересчитать подходящие ядра |
| `/magnet profile reload` | Перезагрузить профили материалов из конфига |

Все команды используют permission `magnet.use`. По умолчанию оно равно `true`, поэтому прежнее свободное использование не ломается; владелец сервера может отозвать его через permissions-плагин.

## Переносной Магнит

Переносной магнит помечается через `PersistentDataContainer`. На Minecraft 1.17+ его базовый материал — `AMETHYST_SHARD`, а на 1.16.5 используется fallback `COMPASS`. Современные item-model API применяются при наличии, иначе записывается custom model data `9001001`.

Каждые 2 тика плагин проверяет игроков онлайн. Если игрок держит переносной магнит, поддерживаемые выпавшие предметы в радиусе 7 блоков притягиваются к игроку.

## Настройка Ресурспака

Пример ресурспака лежит в `docs/resource-pack` и использует тот же namespace, что и плагин: `magnit`. Пак с путями `assets/magnet/...` не совпадёт с `magnit:portable_magnet`, из-за чего предмет может отображаться как missing texture.

Для Minecraft `1.21.x` ресурспак с поддержкой и современного item model пути, и fallback через custom model data должен включать эти файлы:

```text
pack.mcmeta
assets/magnit/items/portable_magnet.json
assets/magnit/models/item/portable_magnet.json
assets/magnit/textures/item/portable_magnet.png
assets/minecraft/models/item/amethyst_shard.json
```

`assets/magnit/items/portable_magnet.json`:

```json
{
  "model": {
    "type": "minecraft:model",
    "model": "magnit:item/portable_magnet"
  }
}
```

`assets/magnit/models/item/portable_magnet.json`:

```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "magnit:item/portable_magnet"
  }
}
```

Положи текстуру в `assets/magnit/textures/item/portable_magnet.png`. Новые магниты получают эту модель сразу; старые магниты обновятся, когда игрок возьмёт их в руку. Для 1.16.5 добавь аналогичный override custom model data для `minecraft:compass`, потому что аметистового осколка в этой версии нет.

В `server.properties` параметр `resource-pack` должен быть прямой ссылкой на итоговый `.zip`. `resource-pack-prompt` должен быть JSON text component. Обычный текст вроде `Для отображения портативного магнита нужен ресурспак Magnet.` вызывает `MalformedJsonException` на Leaf/Paper.

Пример с русским prompt:

```properties
resource-pack=PASTE_DIRECT_DOWNLOAD_LINK_HERE
resource-pack-id=7bb7e1e4-c4e6-42b4-9c8e-8f1e9c8a6f02
resource-pack-prompt={"text":"Для отображения портативного магнита нужен ресурспак Magnet.","color":"aqua"}
resource-pack-sha1=f19f038ec8b744579cc692e5b7a9e41d6df0e8fb
require-resource-pack=false
```

Английский вариант prompt:

```properties
resource-pack-prompt={"text":"This server uses a resource pack to display the Portable Magnet texture.","color":"aqua"}
```

Если пересобираешь zip ресурспака, обнови `resource-pack-sha1` на SHA-1 именно этого zip-файла.

Команда `/magnet debug item` показывает base material, маркер `PersistentDataContainer`, item model key, custom model data, ожидаемый model key, лучший доступный visual path и версию плагина.

С полной силой притягиваются:

- железные слитки, самородки, блоки, сырое железо и железная руда;
- железные инструменты, оружие и броня;
- кольчужная броня;
- наковальни, железные решётки, железные двери и люки;
- рельсы и вагонетки;
- вёдра, ножницы, компасы и похожие металлические предметы.

С пониженной силой притягиваются:

- незеритовые слитки, обломки и блоки;
- незеритовые инструменты, оружие и броня.

## Магнитные Ядра

Стационарное магнитное ядро создаётся из полной 2x2x2 структуры настроенных материалов ядра. Посмотри на один из восьми блоков и выполни:

```text
/magnet core create <id>
```

Плагин вычислит центр ядра, просканирует окружающий каркас, сохранит ядро и начнёт притягивать поддерживаемые выпавшие предметы к центру.

Сила ядра берётся из профилей материалов:

- радиус и сила усредняются по восьми блокам ядра;
- отображаемый профиль выбирается по количеству блоков профиля, затем по priority;
- необязательные аргументы radius и strength включают ручной override;
- `/magnet core set` тоже включает ручной override;
- выключение override пересчитывает значения по текущим блокам ядра.

ID ядра может содержать только строчные латинские буквы, цифры, подчёркивания и дефисы.

## Материалы Ядра И Каркас

В `config.yml` важны два раздела:

- `core-materials` задаёт, какие блоки могут образовывать ядро 2x2x2, а также profile, base radius, base strength и priority каждого материала.
- `frame-materials` задаёт, какие блоки считаются окружающим магнитным каркасом.

Среди материалов ядра по умолчанию есть варианты меди, железо, золото, редстоун, лазурит, алмаз, изумруд, аметист, призмарин, обсидиан, незерит, магнетит, маяк и якорь возрождения.

Сканер каркаса ищет блоки вокруг ядра в пределах настроенных радиусов. Угловые блоки каркаса могут считаться optional через `ignore-frame-corners`. Если сломать блок ядра или critical-блок каркаса, ядро помечается повреждённым и перестаёт притягивать предметы, пока структуру не починят и не выполнят refresh или rescan.

## Локализация

Английский язык используется по умолчанию. Русский доступен как дополнительная локализация.

Плагин выбирает язык сообщений по языку клиента игрока. Неподдерживаемые языки используют английский fallback.

## Сборка

Все артефакты собираются одной командой:

```powershell
.\gradlew.bat clean build
```

Результат — один shaded universal jar:

```text
build/libs/Magnet-Universal-1.16.5-1.21.x.jar
```

Kotlin runtime включён внутрь, bytecode соответствует Java 8 (class-file major version 52).

## Установка

1. Выполни `.\gradlew.bat clean build`.
2. Скопируй `build/libs/Magnet-Universal-1.16.5-1.21.x.jar` в папку `plugins` сервера.
3. Запусти или перезапусти сервер.
4. Проверь `/magnet` и `/magnet give`.
5. Для стационарного магнита построй ядро 2x2x2 и выполни `/magnet core create <id>`.

## Локальный Тестовый Сервер

Тестовые серверы используют отдельные папки внутри `run/`:

```powershell
.\gradlew.bat runLegacyServer
.\gradlew.bat runModernServer
```

Можно выбрать конкретную версию Paper:

```powershell
.\gradlew.bat runServer '-PrunMinecraftVersion=1.16.5'
.\gradlew.bat runServer '-PrunMinecraftVersion=1.20.4'
.\gradlew.bat runServer '-PrunMinecraftVersion=1.21.11'
```

Legacy-задача включает Paper-флаг для локального smoke-теста на Java 21. Для production используй Java, рекомендованную для выбранной версии Minecraft/Paper.

## Структура Проекта

```text
src/main/kotlin/ru/garde/magnet/
  MagnetPlugin.kt
  command/
  compat/
  config/
  core/
  message/
  portable/
  resourcepack/

src/main/resources/
  plugin.yml
  config.yml
  lang/
    en.yml
    ru.yml

schematic/
  Magnet.litematic
```

Для contributor-oriented описания структуры см. [ARCHITECTURE.md](ARCHITECTURE.md).

## Известные ограничения

- Runtime smoke-тесты выполнены на Paper 1.16.5 и Paper 1.21.11; вся промежуточная матрица, Spigot и Leaf в этой рабочей среде не запускались.
- В Minecraft 1.16.5 нет аметиста, меди, глубинного сланца и других поздних материалов. Такие записи конфига пропускаются; доступные профили железа, золота, редстоуна, обсидиана, незерита, магнетита, маяка и якоря возрождения продолжают работать.
- Ресурспак для 1.16.5 должен использовать fallback-модель компаса; современный пак может использовать аметистовый осколок и item-model путь.
- Выдачу предмета и физику притяжения всё ещё нужно проверить игроком в игре; консольный smoke-тест проверяет маршрутизацию команд и отсутствие крашей.

## Лицензия

Проект распространяется под лицензией GNU General Public License v3.0 only.

Copyright (C) 2026 Garde1 / Gardeone12.

Подробности см. в файле [LICENSE](../LICENSE).
