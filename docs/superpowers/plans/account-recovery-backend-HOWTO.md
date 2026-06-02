# Как применить патч восстановления аккаунта в rcq-server-ref

## Что делает патч

Добавляет три эндпоинта в `app/routers/auth.py` и одно поле в `app/models/user.py`:

| Эндпоинт | Auth | Описание |
|----------|------|----------|
| `POST /auth/key-backup` | JWT | Сохраняет зашифрованный blob приватных ключей |
| `POST /auth/recover/challenge` | — | Выдаёт одноразовый nonce для UIN (TTL 5 мин, Redis) |
| `POST /auth/recover/verify` | — | Проверяет Ed25519 подпись → возвращает JWT + blob |

Схема: добавляется колонка `encrypted_key_blob TEXT NULL` в таблицу `users`.

---

## Применить патч (шаг за шагом)

```bash
# 1. Клонируй бэкенд
git clone git@github.com:rcq-messenger/rcq-server-ref.git
cd rcq-server-ref

# 2. Создай ветку
git checkout -b feat/account-recovery

# 3. Скачай и примени патч
curl -L \
  https://raw.githubusercontent.com/darxfame/rcq-android/phase-1-core-messaging/docs/superpowers/plans/account-recovery-backend.patch \
  | git am

# 4. Проверь что применилось
git log --oneline -1
git diff HEAD~1 --stat

# 5. Пушни ветку
git push origin feat/account-recovery
```

---

## Создать Pull Request через gh CLI

```bash
gh pr create \
  --repo rcq-messenger/rcq-server-ref \
  --head feat/account-recovery \
  --base main \
  --title "feat: account recovery via encrypted key backup + Ed25519 challenge-response" \
  --body "$(cat <<'EOF'
## Problem

Account recovery is impossible today — \`POST /auth/register\` always allocates a new UIN,
there is no re-authentication path for an existing identity.

## Solution

Zero-knowledge challenge-response recovery using the user's own Ed25519 signing key.
The server never sees any private key material.

**Registration (client-side):**
1. Generate 24-word BIP39 mnemonic
2. Derive AES-256 key via PBKDF2-SHA256 (600k iterations, salt = UIN bytes)
3. AES-256-GCM encrypt private key bundle → \`encrypted_blob\`
4. Upload blob via \`POST /auth/key-backup\` (JWT required, one-time)

**Recovery (new device):**
1. \`POST /auth/recover/challenge { uin }\` → nonce (TTL 5 min)
2. Derive AES key from mnemonic, decrypt signing key
3. Ed25519-sign nonce → \`POST /auth/recover/verify { uin, challenge, signature }\`
4. Server verifies vs stored public \`signing_key\` → returns \`{ token, encrypted_blob }\`

## Files changed
- \`app/models/user.py\` — add \`encrypted_key_blob TEXT NULL\`
- \`app/routers/auth.py\` — add 3 endpoints + Pydantic models

## No new dependencies
\`cryptography\` already available via \`python-jose[cryptography]\`.
\`redis.asyncio\` already used via \`app.core.redis\`.

Full plan: https://github.com/darxfame/rcq-android/blob/phase-1-core-messaging/docs/superpowers/plans/2026-05-28-account-recovery.md
EOF
)"
```

---

## Создать Issue вручную (если нет gh или push-доступа)

Открой: https://github.com/rcq-messenger/rcq-server-ref/issues/new

**Заголовок:**
```
feat: account recovery via encrypted key backup + Ed25519 challenge-response
```

**Тело** — скопируй блок из секции PR выше (между `EOF`).

---

## Файлы в этой папке

| Файл | Описание |
|------|----------|
| `account-recovery-backend.patch` | Готовый git-патч для rcq-server-ref |
| `2026-05-28-account-recovery.md` | Полный план реализации (бэкенд + Android) |
| `account-recovery-backend-HOWTO.md` | Этот файл — инструкция по применению |
