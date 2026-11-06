# Unique Relics

**Unique Relics** — заготовка мода для Minecraft Java Edition 1.20.1 на Fabric.

## Текущий статус

Сейчас в репозитории находится базовая структура проекта:
- Fabric Loom и Gradle Wrapper;
- Java 17;
- главный класс мода;
- метаданные Fabric и файлы локализации;
- GitHub Actions для автоматической сборки.

**Игровые механики пока не реализованы.** Предметы, регистрация реликвий, редкости, надписи, шаблоны и интерфейс будут добавляться отдельно.

## Окружение

| Компонент | Версия |
| --- | --- |
| Minecraft | 1.20.1 |
| Fabric Loader | 0.19.5 |
| Fabric API | 0.92.12+1.20.1 |
| Java | 17 |
| Gradle Wrapper | 8.12 |
| Fabric Loom | 1.9.2 |

## Сборка

Требуется установленная Java 17.

**Windows:**
```bat
gradlew.bat build --no-daemon
```

**Linux / macOS:**
```bash
chmod +x gradlew
./gradlew build --no-daemon
```

Готовый JAR появится в `build/libs/`.

## Идентификаторы

- Mod ID: `unique_relics`
- Java package: `com.mortylovelly.uniquerelics`
- Maven group: `com.mortylovelly.uniquerelics`

## Совместимость

Проект нацелен на Fabric 1.20.1. При добавлении будущих функций интерфейс кузнечного стола и данные сторонних модов будут затрагиваться только при необходимости, с отдельной проверкой совместимости.
