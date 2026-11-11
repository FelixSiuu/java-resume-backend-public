# resume

個人履歷／作品集後端 API，供管理後台與公開作品站使用。

技術棧：Java 17、Spring Boot 3.5、MyBatis-Plus、MySQL、Redis、Flyway、JWT（HttpOnly Cookie）+ XSRF。

---

## 本地啟動

### 前置需求

- JDK 17+
- Docker / Docker Compose（MySQL、Redis）
- 本專案已含 Maven Wrapper（`./mvnw`）

### 步驟

1. 複製環境變數範本：

```bash
cp .env.example .env
```

依需要修改 `.env`（至少確認 `SPRING_JWT_SECRET` 長度 ≥ 32）。

2. 啟動依賴服務：

```bash
docker compose up -d
```

會啟動 MySQL（`3306`）與 Redis（`6379`）。

3. 啟動應用（預設 `dev` profile，埠號 `8080`）：

```bash
./mvnw spring-boot:run
```

4. 驗證：

- Swagger UI（僅 dev）：http://localhost:8080/swagger-ui.html
- 或呼叫公開介面，例如：`GET /api/v1/public/{username}/about`

資料庫 schema 由 Flyway 在啟動時自動遷移（`src/main/resources/db/migration`）。

---

## 環境變數與設定

根目錄 `.env` 會被 `application.yaml` 以 `optional:file:.env` 載入。可參考 `.env.example`：

| 變數 | 說明 |
|------|------|
| `MYSQL_ROOT_PASSWORD` | Compose 啟動 MySQL 用的 root 密碼 |
| `MYSQL_DATABASE` | 自動建立的資料庫名稱 |
| `SPRING_DB_USERNAME` / `SPRING_DB_PWD` | 應用連線 DB 的帳密 |
| `SPRING_DATABASE` | 應用使用的資料庫名稱 |
| `SPRING_JWT_SECRET` | JWT 簽名密鑰（至少 32 字元） |

環境設定檔：

- 開發：`src/main/resources/application-dev.yaml`（預設 active）
- 正式：`src/main/resources/application-prod.yaml`（例如 `java -jar … --spring.profiles.active=prod`）

開發環境 Cookie／TTL 相關（`jwt.cookie`）摘要：

- `ACCESS_TOKEN`：短期 access JWT（如 30 分鐘）
- `REFRESH_TOKEN`：refresh（rememberMe 長／短 TTL 不同）
- `XSRF_TOKEN`：CSRF 雙 Cookie 比對用
- CORS 允許來源：`http://localhost:3000`、`http://localhost:5173`（見 `app.cors.allowed-origins`）

正式環境請在 `application-prod.yaml` 對齊 cookie 名稱、TTL，並將 CORS 設為實際前端網域；API 需走 **HTTPS**（`Secure` Cookie）。

---

## 主要 API

基礎路徑皆為 `/api/v1`。

### 認證 `/api/v1/auth`

| 方法 | 路徑 | 說明 |
|------|------|------|
| `POST` | `/auth/register` | 註冊 |
| `GET` | `/auth/captcha` | 圖形驗證碼（寫入 Session） |
| `POST` | `/auth/login` | 登入（可帶 `rememberMe`） |
| `POST` | `/auth/refresh` | 用 refresh Cookie 換新 access／refresh／xsrf |
| `POST` | `/auth/logout` | 登出並撤銷 refresh、清除 Cookie |

### 需登入（Access Cookie）

| 前綴 | 說明 |
|------|------|
| `/api/v1/myInfo/**` | 關於我、技能、學歷、經歷、聯絡、專案、頭像等 CRUD |
| `/api/v1/resume` | 履歷預覽／PDF 匯出 |

### 公開（無需登入）

| 前綴 | 說明 |
|------|------|
| `/api/v1/public/{username}/**` | 依使用者名稱讀取公開履歷資料 |

前端請求需帶 Cookie 時請使用 `credentials: 'include'`（或 axios `withCredentials: true`）。

---

## 認證與安全行為

本專案**不以 JSON body 回傳 access／refresh JWT**，改以瀏覽器 Cookie 持有憑證。

### Cookie

| Cookie | HttpOnly | 用途 |
|--------|----------|------|
| `ACCESS_TOKEN` | 是 | 短效 JWT，守衛 `/myInfo` 等受保護介面 |
| `REFRESH_TOKEN` | 是 | 不透明 refresh；hash 存 DB；`Path` 為 `/api/v1/auth` |
| `XSRF_TOKEN` | 否* | 與請求頭比對，防 CSRF |

\*跨站（例如 Vercel → 獨立 API 網域）時，前端通常**無法**用 `document.cookie` 讀取 API 網域的 Cookie。登入／refresh 成功後請使用回應 body 中的 `xsrfToken`，並在後續變更類請求帶上對應 header（如 `Xsrf-Token`）。

跨站 Cookie 需 `Secure` + `SameSite=None`；正式環境 API 必須 HTTPS。

### Access + Refresh 與 rememberMe

- **登入**一律寫入 refresh 紀錄（DB 存 SHA-256 hash，明文只在 Cookie）。
- `rememberMe: true`：refresh Cookie 設較長 `Max-Age`；絕對過期時間 `expires_at` 在登入時寫死。
- `rememberMe: false`：refresh Cookie **不設 Max-Age**（瀏覽器 session）；DB 仍有較短的 `expires_at`。關掉瀏覽器後通常無法再 refresh。
- Access 過期後，前端呼叫 `POST /api/v1/auth/refresh`：校驗未撤銷且未超過 `expires_at`，再輪替 refresh 並下發新的 access／xsrf。`expires_at` **不會**因 refresh 而延長。
- 登出會撤銷當前 refresh（若有）並清除相關 Cookie。

### CORS

僅允許設定檔中的來源，且 `allowCredentials: true`。不可使用 `Access-Control-Allow-Origin: *` 搭配 Cookie。
