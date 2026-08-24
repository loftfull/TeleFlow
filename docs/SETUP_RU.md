# TeleFlow: первый запуск сборки APK

## 1. Создайте отдельный GitHub-репозиторий

Рекомендуемое имя: `loftfull/TeleFlow`.

Репозиторий нужен отдельный: TeleFlow является самостоятельным GPL-проектом и не должен смешиваться с вашими существующими проектами.

## 2. Загрузите bootstrap

Поместите содержимое этого архива в корень репозитория и отправьте ветку `teleflow/bootstrap` или `main`.

## 3. Получите собственные Telegram API credentials

Откройте официальный раздел Telegram **API development tools** на `my.telegram.org`, создайте приложение и получите:

- `api_id` — число;
- `api_hash` — 32-символьная hexadecimal-строка.

Не присылайте `api_hash` в чат и не коммитьте его в GitHub.

## 4. Добавьте GitHub Secrets

В репозитории откройте:

`Settings → Secrets and variables → Actions → New repository secret`

Создайте два секрета:

- `TELEGRAM_API_ID` = ваш `api_id`;
- `TELEGRAM_API_HASH` = ваш `api_hash`.

## 5. Запустите сборку

Откройте:

`Actions → Build TeleFlow Android APK → Run workflow`

Workflow остановится с ошибкой, если credentials отсутствуют, имеют неверный формат, совпадают с официальными Telegram credentials либо upstream Telegram изменился так, что безопасный patch больше нельзя применить.

## 6. Получите APK

После успешной сборки откройте завершённый workflow-run и скачайте artifact:

`teleflow-android-debug`

Внутри находится `app.apk`.

Это development APK. Для публичного распространения нужен отдельный TeleFlow signing key, собственная иконка/полный branding pass и публикация соответствующего исходного кода.

## Текущий функциональный уровень

Первая сборка является базой продукта: официальный Telegram Android + отдельная идентичность TeleFlow + extension-layer. Smart Folders и Drive уже зарезервированы как независимые модули в архитектуре, но их пользовательские экраны и серверная логика относятся к следующему вертикальному срезу.
