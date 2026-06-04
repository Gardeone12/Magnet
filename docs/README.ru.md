# Magnet

Язык: [English](../README.md) | Русский

Magnet — Paper-плагин для Minecraft, который добавляет переносной магнит и стационарные магнитные ядра. Переносной магнит притягивает выпавшие металлические предметы, пока игрок держит его в руке. Стационарные ядра — это структуры 2x2x2, которые притягивают предметы к своему центру.

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

- Minecraft / Paper: `1.21.11`
- Java: `21`
- Kotlin JVM: `2.4.0-RC`

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

Для команды сейчас не настроено отдельное permission-требование.

## Переносной Магнит

Переносной магнит — это аметистовый осколок, помеченный через `PersistentDataContainer`, поэтому плагин отличает его от обычного предмета. На Paper/Leaf `1.21.11` основной способ отображения — ключ модели `magnit:portable_magnet`. Также плагин записывает custom model data `9001001` как fallback для паков, где ещё есть override аметистового осколка.

Каждые 2 тика плагин проверяет игроков онлайн. Если игрок держит переносной магнит, поддерживаемые выпавшие предметы в радиусе 7 блоков притягиваются к игроку.

## Настройка Ресурспака

Пример ресурспака лежит в `docs/resource-pack` и использует тот же namespace, что и плагин: `magnit`. Пак с путями `assets/magnet/...` не совпадёт с `magnit:portable_magnet`, из-за чего предмет может отображаться как missing texture.

Для Minecraft `1.21.11` в ресурспаке должны быть эти файлы:

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

Положи текстуру в `assets/magnit/textures/item/portable_magnet.png`. Новые магниты получают эту модель сразу; старые магниты обновятся, когда игрок возьмёт их в руку.

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

Команда `/magnet debug item` показывает base material, маркер `PersistentDataContainer`, item model key, custom model data, ожидаемый model key и версию плагина.

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

Если Gradle установлен в системе:

```powershell
gradle build
```

Если в проекте есть Gradle Wrapper:

```powershell
.\gradlew.bat build
```

Готовый `.jar` появится в папке:

```text
build/libs/Magnit-0.1.2.jar
```

## Установка

1. Соберите плагин.
2. Скопируйте `build/libs/Magnit-0.1.2.jar` в папку `plugins` Paper-сервера.
3. Запустите или перезапустите сервер.
4. В игре выполните `/magnet give`.
5. Для стационарного магнита постройте структуру 2x2x2 и выполните `/magnet core create <id>`.

## Локальный Тестовый Сервер

Проект использует `xyz.jpenilla.run-paper`, поэтому локальный Paper-сервер можно запустить командой:

```powershell
gradle runServer
```

Если в проекте есть Gradle Wrapper:

```powershell
.\gradlew.bat runServer
```

## Структура Проекта

```text
src/main/kotlin/ru/garde/magnet/
  CoreMaterialRegistry.kt
  MagnetCoreManager.kt
  MagnetPlugin.kt
  MagnetPluginBootsTrap.kt
  MagnetStructureBreakListener.kt

src/main/resources/
  paper-plugin.yml
  config.yml
  lang/
    en.yml
    ru.yml

schematic/
  Magnet.litematic
```

## Лицензия

Проект распространяется под лицензией GNU General Public License v3.0 only.

Copyright (C) 2026 Garde1 / Gardeone12.

Подробности см. в файле [LICENSE](../LICENSE).
