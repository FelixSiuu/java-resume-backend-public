# resume

個人履歷／作品集後端 API，供管理後台與公開作品站使用。

技術棧：Java 17、Spring Boot 3.5、MyBatis-Plus、MySQL、Redis、Flyway、JWT（HttpOnly Cookie）+ XSRF、Spring Mail（SMTP 郵箱驗證／忘記密碼）。

版本：`2.3.0`

---

## 本地啟動

### 前置需求

- JDK 17+
- Docker / Docker Compose（MySQL、Redis）
- 本專案已含 Maven Wrapper（`./mvnw`）
- 可寄信的 SMTP（開發可用 Gmail App Password 等）

### 步驟

1. 複製環境變數範本：

```bash
cp .env.example .env
```

依需要修改 `.env`（至少確認 `SPRING_JWT_SECRET` 長度 ≥ 32，並填好郵件相關變數）。

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
| `SPRING_MAIL_HOST` | SMTP 主機（如 `smtp.gmail.com`） |
| `SPRING_MAIL_PORT` | SMTP 埠（預設 `587`） |
| `SPRING_MAIL_USERNAME` | SMTP 登入帳號 |
| `SPRING_MAIL_PASSWORD` | SMTP 密碼／App Password |
| `SPRING_MAIL_FROM` | 信件 From（建議與登入帳號一致） |
| `APP_FRONTEND_BASE_URL` | 前端基底網址（驗證信／重設密碼連結用，如 `https://web-profolio-admin.vercel.app`） |

環境設定檔：

- 開發：`src/main/resources/application-dev.yaml`（預設 active）
- 正式：`src/main/resources/application-prod.yaml`（例如 `java -jar … --spring.profiles.active=prod`）

開發環境 Cookie／TTL 相關（`jwt.cookie`）摘要：

- `ACCESS_TOKEN`：短期 access JWT（如 30 分鐘）
- `REFRESH_TOKEN`：refresh（rememberMe 長／短 TTL 不同）
- `XSRF_TOKEN`：CSRF 雙 Cookie 比對用
- CORS 允許來源：`http://localhost:3000`、`http://localhost:5173`（見 `app.cors.allowed-origins`）

正式環境請在 `application-prod.yaml` 對齊 cookie 名稱、TTL，並將 CORS 設為實際前端網域；API 需走 **HTTPS**（`Secure` Cookie）。郵件與 `APP_FRONTEND_BASE_URL` 亦須在正式環境設定。

---

## 主要 API

基礎路徑皆為 `/api/v1`。

### 認證 `/api/v1/auth`

| 方法 | 路徑 | 說明 |
|------|------|------|
| `POST` | `/auth/register` | 註冊（必填 `username`、`password`、`email`；寄驗證信） |
| `GET` | `/auth/captcha` | 圖形驗證碼（寫入 Session；需 `credentials: 'include'`） |
| `POST` | `/auth/login` | 登入（可帶 `rememberMe`；未驗證 email 仍可登入） |
| `POST` | `/auth/refresh` | 用 refresh Cookie 換新 access／refresh／xsrf |
| `POST` | `/auth/logout` | 登出並撤銷 refresh、清除 Cookie |
| `POST` | `/auth/verify-email` | 驗證郵箱（body：`{ "token" }`；免登入） |
| `POST` | `/auth/forgot-password` | 忘記密碼發信（body：`email`、`captcha`；免登入；60 秒冷卻） |
| `POST` | `/auth/validate-reset-token` | 開頁校驗重設 token 是否仍有效（body：`{ "token" }`；免登入；不消耗 token） |
| `POST` | `/auth/reset-password` | 重設密碼（body：`token`、`password`；免登入） |

### 帳戶 `/api/v1/account`（需登入）

| 方法 | 路徑 | 說明 |
|------|------|------|
| `PUT` | `/account/change-password` | 修改密碼（需舊密碼；成功後撤銷 refresh 並清 Cookie） |
| `PUT` | `/account/update-email` | 綁定／改綁／重寄驗證信 |
| `GET` | `/account/email` | 查詢郵箱綁定狀態（`email`、`verified`、`nextStepMessage`） |
| `DELETE` | `/account` | 軟刪帳戶（寬限期後排程硬刪；成功後清 Cookie／撤銷 refresh） |

### 需登入（Access Cookie）

| 前綴 | 說明 |
|------|------|
| `/api/v1/myInfo/**` | 關於我、技能、學歷、經歷、聯絡、專案、頭像等 CRUD |
| `/api/v1/account/**` | 帳戶設定（改密、郵箱、刪除） |
| `/api/v1/resume` | 履歷預覽／PDF 匯出 |

### 公開（無需登入）

| 前綴 | 說明 |
|------|------|
| `/api/v1/public/{username}/**` | 依使用者名稱讀取公開履歷資料 |

前端請求需帶 Cookie 時請使用 `credentials: 'include'`（或 axios `withCredentials: true`）。

---

## 郵箱綁定與驗證

註冊與補綁都會寫入 pending email（`email_verified_at = null`），並以 SMTP 寄出驗證信。信內連結指向**前端**：

`{APP_FRONTEND_BASE_URL}/verify-email?token={rawToken}`

前端從 query 取 `token` 後呼叫 `POST /api/v1/auth/verify-email`。後端以 SHA-256(token) 查庫；token 綁定特定 `user_id`，且須與當前 `user.email` 一致才標記已驗證。

### `PUT /account/update-email` 行為摘要

| 情況 | 行為 |
|------|------|
| 同 email + 已驗證 | 拒絕 |
| 同 email + 未驗證 | 只重寄驗證信 |
| 不同 email（含已驗證後改綁） | 覆蓋信箱、清空驗證狀態、寄新驗證信 |

每次寄信前會作廢該用戶尚未使用的舊 **同 purpose** token。郵箱驗證／重設密碼 raw token 約 15 分鐘有效、單次使用；表 `email_token` 以 `purpose` 區分 `VERIFY`／`RESET`。

郵箱狀態請用 `GET /account/email` 查詢，勿只依賴登入當下的 `LoginVo`。

---

## 忘記密碼與重設

未登入時透過**已驗證郵箱**重設密碼。身分由信箱內一次性 `RESET` token 證明（DB 只存 SHA-256 hash，綁定 `user_id`）；客戶端不可指定要改哪個用戶。

與已登入 `PUT /account/change-password`（需 Access Cookie + 舊密碼）分開。

### 前端流程

```text
GET  /auth/captcha
POST /auth/forgot-password          { email, captcha }
        ↓ 郵件連結
開啟   {APP_FRONTEND_BASE_URL}/reset-password?token={rawToken}
POST /auth/validate-reset-token     { token }     ← 開頁先校驗，再顯示表單
POST /auth/reset-password           { token, password }
```

1. `GET /auth/captcha`（需 Session Cookie／`credentials: 'include'`），再 `POST /auth/forgot-password`：`{ "email", "captcha" }`。
2. 僅當該 email **已驗證且帳戶未軟刪** 時才寄重設信；無論是否寄出，成功訊息固定（避免探測帳號是否存在）。
3. 信內連結：

`{APP_FRONTEND_BASE_URL}/reset-password?token={rawToken}`

4. 開頁先 `POST /auth/validate-reset-token`：`{ "token" }`。通過再顯示密碼表單；失敗則引導重新申請。此接口**不**標記 token 已用、不回傳 userId／email。
5. 再 `POST /auth/reset-password`：`{ "token", "password" }`（規則同改密：8–20 位、須同時含字母與數字；與舊密碼相同則拒絕）。
6. 後端查 `purpose=RESET` 且未過期、未使用的 token → 確認用戶存在且未軟刪 → 更新密碼 → 標記 token 已用 → 撤銷該用戶所有 refresh 並清除相關 Cookie。

### Token 與 `email_token`

- 與郵箱驗證共用表 `email_token`，以 `purpose` 區分：`VERIFY`（驗證信）／`RESET`（重設密碼）。
- Raw token 僅出現在郵件／前端 URL；庫內為 hash。約 **15 分鐘**有效、**單次使用**。
- 寄出新信前會作廢該用戶尚未使用的**同 purpose**舊 token。

### `POST /auth/forgot-password` 冷卻

- 同一 email（正規化為小寫）**60 秒**內不可再次進入發信流程；無論上次有無實際寄信都寫入冷卻。
- 剩餘秒數來自 **Redis TTL**（非 `email_token.create_time`）。
- 冷卻中：`isSuccess: false`，data 為 `CoolDownVo`（欄位 `cd` = 剩餘秒數）。
- 成功：`isSuccess: true`，亦帶 `cd: 60`，方便前端倒數。
- Captcha 錯誤不寫入、不延長冷卻。

### 與已登入改密的差異

| | 忘記密碼 | 已登入改密 |
|--|----------|------------|
| 端點 | `forgot-password` → `validate-reset-token` → `reset-password` | `PUT /account/change-password` |
| 身分證明 | 信箱內一次性 `RESET` token | Access Cookie + 舊密碼 |
| 登入態 | 不需要 | 需要 |
| 成功後 | 撤銷 refresh、清 Cookie | 同左 |

---

## 認證與安全行為

本專案**不以 JSON body 回傳 access／refresh JWT**，改以瀏覽器 Cookie 持有憑證。

### Captcha（Session）

- `GET /auth/captcha` 將驗證碼寫入伺服器 Session（瀏覽器持有 `JSESSIONID`；跨站需 `credentials: 'include'`）。
- `login`、`forgot-password` 在 **captcha 比對成功後立即** `session.removeAttribute`，與後續業務成敗無關；同一碼不可重用，每次請求需重新取圖。

### Cookie

| Cookie | HttpOnly | 用途 |
|--------|----------|------|
| `ACCESS_TOKEN` | 是 | 短效 JWT，守衛 `/myInfo`、`/account` 等受保護介面 |
| `REFRESH_TOKEN` | 是 | 不透明 refresh；hash 存 DB；`Path` 為 `/api/v1/auth` |
| `XSRF_TOKEN` | 否* | 與請求頭比對，防 CSRF |

\*跨站（例如 Vercel → 獨立 API 網域）時，前端通常**無法**用 `document.cookie` 讀取 API 網域的 Cookie。登入／refresh 成功後請使用回應 body 中的 `xsrfToken`，並在後續變更類請求帶上對應 header（如 `Xsrf-Token`）。

跨站 Cookie 需 `Secure` + `SameSite=None`；正式環境 API 必須 HTTPS。

下列路徑不經 Auth／XSRF 攔截：`register`、`login`、`captcha`、`logout`、`refresh`、`verify-email`、`forgot-password`、`validate-reset-token`、`reset-password`，以及 `/api/v1/public/**`。

### Access + Refresh 與 rememberMe

- **登入**一律寫入 refresh 紀錄（DB 存 SHA-256 hash，明文只在 Cookie）。
- `rememberMe: true`：refresh Cookie 設較長 `Max-Age`；絕對過期時間 `expires_at` 在登入時寫死。
- `rememberMe: false`：refresh Cookie **不設 Max-Age**（瀏覽器 session）；DB 仍有較短的 `expires_at`。關掉瀏覽器後通常無法再 refresh。
- Access 過期後，前端呼叫 `POST /api/v1/auth/refresh`：校驗未撤銷且未超過 `expires_at`，再輪替 refresh 並下發新的 access／xsrf。`expires_at` **不會**因 refresh 而延長。
- 登出、改密、**重設密碼**、刪除帳戶會撤銷 refresh（若適用）並清除相關 Cookie。

### 帳戶軟刪

`DELETE /account` 為軟刪（改寫 username、清空 email，設 `deleted_at`）。約 **30 天**寬限期後由排程硬刪關聯資料（外鍵 CASCADE）。

### CORS

僅允許設定檔中的來源，且 `allowCredentials: true`。不可使用 `Access-Control-Allow-Origin: *` 搭配 Cookie。
