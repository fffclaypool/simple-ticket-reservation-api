# チケット予約システム REST API

Java 17、Spring Boot 3.2を使用したチケット予約システムのREST APIです。

## 必要条件

- Java 17以上
- Gradle 8.5以上（Gradle Wrapperを使用する場合は不要）

## セットアップ

```bash
cd ticket-reservation-api
```

## 起動方法

### 開発モード（H2データベース）

```bash
./gradlew bootRun
```

アプリケーションは `http://localhost:8080` で起動します。

H2データベースコンソール: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:ticketdb`
- Username: `sa`
- Password: （空欄）

### 本番モード（PostgreSQL）

```bash
./gradlew bootRun --args='--spring.profiles.active=prod'
```

環境変数でデータベース接続情報を設定してください：
- `DB_USERNAME`: PostgreSQLユーザー名（デフォルト: postgres）
- `DB_PASSWORD`: PostgreSQLパスワード（デフォルト: postgres）

## API エンドポイント

### イベント管理

| メソッド | エンドポイント | 説明 |
|---------|---------------|------|
| GET | `/api/events` | 全イベント取得 |
| GET | `/api/events/{id}` | ID指定でイベント取得 |
| GET | `/api/events/available` | 予約可能なイベント取得 |
| GET | `/api/events/search?name={name}` | イベント名で検索 |
| POST | `/api/events` | イベント作成 |
| PUT | `/api/events/{id}` | イベント更新 |
| DELETE | `/api/events/{id}` | イベント削除 |

### 予約管理

| メソッド | エンドポイント | 説明 |
|---------|---------------|------|
| GET | `/api/reservations` | 全予約取得 |
| GET | `/api/reservations/{id}` | ID指定で予約取得 |
| GET | `/api/reservations/code/{code}` | 予約コードで検索 |
| GET | `/api/reservations/email/{email}` | メールアドレスで検索 |
| GET | `/api/reservations/event/{eventId}` | イベントIDで検索 |
| POST | `/api/reservations` | 予約作成 |
| PATCH | `/api/reservations/{id}/cancel` | 予約キャンセル |

## 使用例

### イベント作成

```bash
curl -X POST http://localhost:8080/api/events \
  -H "Content-Type: application/json" \
  -d '{
    "name": "サマーコンサート2024",
    "description": "夏の野外コンサート",
    "venue": "東京ドーム",
    "eventDate": "2024-08-15T18:00:00",
    "totalSeats": 500,
    "price": 8000
  }'
```

### イベント一覧取得

```bash
curl http://localhost:8080/api/events
```

### 予約作成

```bash
curl -X POST http://localhost:8080/api/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": 1,
    "customerName": "山田太郎",
    "customerEmail": "yamada@example.com",
    "numberOfSeats": 2
  }'
```

### 予約コードで検索

```bash
curl http://localhost:8080/api/reservations/code/RES-XXXXXXXX
```

### 予約キャンセル

```bash
curl -X PATCH http://localhost:8080/api/reservations/1/cancel
```

## ヘルスチェック

Spring Boot Actuatorによるヘルスチェックエンドポイント：

```bash
curl http://localhost:8080/actuator/health
```

## ビルド

```bash
./gradlew build
```

実行可能JARファイルは `build/libs/ticket-reservation-api-0.0.1-SNAPSHOT.jar` に生成されます。

```bash
java -jar build/libs/ticket-reservation-api-0.0.1-SNAPSHOT.jar
```

## テスト

```bash
./gradlew test
```
