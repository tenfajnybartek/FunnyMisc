# 🧪 KOMPLETNY PRZEWODNIK TESTOWANIA SYSTEMU BACKUPÓW

**Data**: 2026-01-20  
**Wersja**: v1.0.5  
**Naprawiony**: Duplikat klucza `backup` w config.yml ✅

---

## 🔧 Krok 1: Przygotowanie

### 1.0. Weryfikacja plugin.yml ⚠️
**WAŻNE**: Przed buildem sprawdź czy plugin.yml nie ma błędów!

```bash
# Sprawdź czy nie ma literówek w plugin.yml
# Szczególnie linijka po 'aliases: [fm, fmisc]'
# NIE MOŻE BYĆ: samotnej litery 'y' lub innych śmieci!
```

**Najczęstsze błędy**:
- ❌ Samotna litera (np. `y` zamiast `depozyt`)
- ❌ Złe wcięcia (YAML wymaga 2 spacji, nie 3!)
- ❌ Brak `:` po nazwie komendy

### 1.1. Build Projektu
```powershell
cd C:\Users\Bartek\IdeaProjects\FunnyMisc
.\gradlew.bat clean build --no-daemon
```

**Oczekiwany wynik**: `BUILD SUCCESSFUL`

**Jeśli BUILD FAILED**:
- Sprawdź plugin.yml (linia 13-22)
- Sprawdź config.yml (brak duplikatów `backup:`)
- Uruchom ponownie: `.\gradlew.bat clean build --no-daemon`

### 1.2. Skopiuj JAR na Serwer
```powershell
Copy-Item .\build\libs\FunnyMisc-1.0.5-SNAPSHOT.jar D:\Serwer\plugins\
```

### 1.3. Start Serwera
```bash
# Uruchom serwer Minecraft
# Sprawdź logi:
```

**Oczekiwane w logach**:
```
[INFO]: [FunnyMisc] Enabling FunnyMisc v1.0.5-SNAPSHOT
[INFO]: [FunnyMisc] Połączono z bazą danych: SQLITE
[INFO]: [FunnyMisc] Tabele bazy danych zostały utworzone
[INFO]: [FunnyMisc] FunnyMisc plugin został włączony!
```

**NIE POWINNO BYĆ** ❌:
```
ERROR: PlayerDeathEvent may only be triggered synchronously ← NAPRAWIONE ✅
ERROR: Missing message: backup.gui.* ← NAPRAWIONE ✅
```

---

## ⚠️ NAPRAWIONE PROBLEMY (v1.0.5):

Poniższe problemy zostały już naprawione w tej wersji:

1. ✅ **Async setHealth error** - Przywracanie jest teraz sync
2. ✅ **Brak armoru** - Armor jest poprawnie przywracany
3. ✅ **Missing messages** - Wszystkie klucze zaktualizowane do `backup.messages.*`
4. ✅ **Wyciąganie przycisku Powrót** - Preview ma teraz custom holder
5. ✅ **Duplikat backup w config** - Usunięty
6. ✅ **Literówka 'y' w plugin.yml** - Usunięta

**Status**: Wszystkie znane problemy rozwiązane! ✨

---

## 🧪 TESTY FUNKCJONALNE

### ✅ TEST 1: Automatyczny Backup przy Śmierci

**Cel**: Sprawdzenie czy backup tworzy się automatycznie przy śmierci gracza

**Kroki**:
1. Zaloguj się do gry
2. **WAŻNE**: Załóż zbroję!
   ```
   /give @s diamond_helmet
   /give @s diamond_chestplate
   /give @s diamond_leggings
   /give @s diamond_boots
   ```
3. Dodaj itemy do eq: `/give @s diamond 64`
4. Zginij: `/kill`
5. Sprawdź backup: `/backup <twoj_nick>`

**Oczekiwany wynik**:
- ✅ GUI otwiera się
- ✅ Widzisz 1 backup z datą i czasem śmierci
- ✅ Powód: "Śmierć" (DEATH)
- ✅ Item = CHEST (aktywny backup)

**Screenshoty do zrobienia**:
- [ ] GUI z backupem
- [ ] Lore backupu (data, powód)

---

### ✅ TEST 2: Preview Backupu

**Cel**: Sprawdzenie czy można podejrzeć zawartość backupu

**Kroki**:
1. Otwórz GUI backupów: `/backup <twoj_nick>`
2. **Kliknij LPM** (lewy przycisk myszy) na backup

**Oczekiwany wynik**:
- ✅ Otwiera się nowe GUI (54 sloty)
- ✅ Tytuł: "Preview: DD.MM.YYYY HH:MM:SS"
- ✅ Widzisz itemy które miałeś przed śmiercią
- ✅ Sloty 0-35: Główny inwentarz (diamenty)
- ✅ **Sloty 36-39: ZBROJA (helmet, chest, legs, boots)** ⚠️ WAŻNE!
- ✅ Slot 40: Offhand
- ✅ Slot 49: Przycisk "← Powrót" (BARRIER)
- ✅ Przycisk "Powrót" **NIE da się wyciągnąć** (event cancelled)

**Screenshoty do zrobienia**:
- [ ] GUI Preview z itemami
- [ ] Zbroja w slotach 36-39 (WIDOCZNA!)
- [ ] Przycisk powrotu

---

### ✅ TEST 3: Przywracanie Backupu (Shift+Klik)

**Cel**: Sprawdzenie czy można przywrócić backup

**Kroki**:
1. Otwórz GUI backupów: `/backup <twoj_nick>`
2. **Shift+Klik LPM** (shift + lewy przycisk) na backup
3. Poczekaj 5 sekund
4. **Shift+Klik LPM** ponownie (potwierdzenie)

**Oczekiwany wynik**:
- ✅ Po pierwszym kliknięciu: Wiadomość "⚠ Kliknij ponownie w ciągu 5 sekund..."
- ✅ Po drugim kliknięciu: 
  - Wiadomość "✔ Przywrócono backup z..."
  - **Inwentarz przywrócony (diamenty WRACAJĄ)**
  - **ZBROJA przywrócona (helmet, chest, legs, boots WRACAJĄ!)** ⚠️ KRYTYCZNE!
  - GUI się zamyka
- ✅ Ponowne otwarcie GUI: Backup ma **zieloną** ikonę (GREEN_GLASS_PANE)
- ✅ Lore pokazuje "Przywrócony przez: <twoj_nick>"

**Screenshoty do zrobienia**:
- [ ] Wiadomość potwierdzenia
- [ ] Wiadomość sukcesu
- [ ] GUI z przywróconym backupem (zielona ikona)
- [ ] **Gracz ma zbroję na sobie** (F5 screenshot)

---

### ✅ TEST 4: Single-Use (Anty-Duping)

**Cel**: Sprawdzenie czy backup może być użyty tylko raz

**Kroki**:
1. Spróbuj przywrócić ten sam backup ponownie
2. **Shift+Klik LPM** 2x na już przywrócony backup

**Oczekiwany wynik**:
- ✅ Wiadomość: "✘ Ten backup został już przywrócony wcześniej!"
- ✅ Inwentarz się NIE zmienia

**Screenshoty do zrobienia**:
- [ ] Wiadomość błędu

---

### ✅ TEST 5: Cooldown

**Cel**: Sprawdzenie czy jest cooldown między przywróceniami

**Kroki**:
1. Zginij ponownie (`/kill`) - utworzy się nowy backup
2. Przywróć backup (Shift+Klik 2x)
3. Od razu zginij i spróbuj przywrócić kolejny backup

**Oczekiwany wynik**:
- ✅ Jeśli próbujesz w ciągu 60s: Wiadomość o cooldownie
- ✅ Po 60s: Backup można przywrócić

**Screenshoty do zrobienia**:
- [ ] Wiadomość o cooldownie (jeśli działa)

---

### ✅ TEST 6: Paginacja (Wiele Backupów)

**Cel**: Sprawdzenie czy paginacja działa dla 45+ backupów

**Kroki**:
1. Stwórz 50 backupów: 
```
for ($i=1; $i -le 50; $i++) { 
    /kill
    Start-Sleep -Seconds 2
}
```
2. Otwórz GUI: `/backup <twoj_nick>`
3. Sprawdź strzałki nawigacji

**Oczekiwany wynik**:
- ✅ Strona 1: 45 backupów
- ✅ Slot 53: Strzałka "Następna strona →" (żółta)
- ✅ Kliknięcie: Przejście na stronę 2
- ✅ Strona 2: 5 backupów
- ✅ Slot 45: Strzałka "← Poprzednia strona"

**Screenshoty do zrobienia**:
- [ ] Strona 1 z 45 backupami
- [ ] Przyciski nawigacji
- [ ] Strona 2 z pozostałymi

---

### ✅ TEST 7: Limity Per Gracz

**Cel**: Sprawdzenie czy limity działają (default: 5)

**Kroki**:
1. Upewnij się że masz uprawnienie `funnymisc.backup.limit.default`
2. Stwórz 10 backupów (zginij 10 razy)
3. Sprawdź `/backup <twoj_nick>`

**Oczekiwany wynik**:
- ✅ Maksymalnie **5 backupów** (najstarsze usunięte)
- ✅ Backupy są sortowane od najnowszych

**Testuj także VIP/SVIP/Admin**:
```
# Daj sobie uprawnienie
/lp user <nick> permission set funnymisc.backup.limit.vip true

# Sprawdź limit (powinno być 15)
```

**Screenshoty do zrobienia**:
- [ ] GUI z 5 backupami (limit default)
- [ ] GUI z 15 backupami (limit VIP)

---

### ✅ TEST 8: Manualny Backup

**Cel**: Sprawdzenie czy można tworzyć manualne backupy

**Kroki**:
1. Jako admin: `/backup create <nick>`

**Oczekiwany wynik**:
- ✅ Wiadomość: "✔ Utworzono manualny backup dla <nick>"
- ✅ W GUI: Nowy backup z powodem "MANUAL"

**Uprawnienia**:
```
funnymisc.backup.create
```

**Screenshoty do zrobienia**:
- [ ] Wiadomość sukcesu
- [ ] GUI z backupem MANUAL

---

### ✅ TEST 9: Info o Backupie (Tekstowo)

**Cel**: Sprawdzenie komendy `/backup info <id>`

**Kroki**:
1. Znajdź ID backupu (hover nad backupem w GUI - ID w nazwie)
2. `/backup info <id>`

**Oczekiwany wynik**:
```
════════════════════════════════════════
Backup #123

Gracz: tenfajnybartek
Data: 20.01.2026 15:30:45
Powód: Śmierć
Przywrócony: NIE

Lokalizacja: world (300, 64, 400)
XP: Poziom 10 (50%)
Zdrowie: 20/20
Jedzenie: 20/20
Tryb gry: CREATIVE
Efekty: 0
════════════════════════════════════════
```

**Screenshoty do zrobienia**:
- [ ] Output komendy `/backup info`

---

### ✅ TEST 10: Cleanup

**Cel**: Sprawdzenie czy czyszczenie działa

**Kroki**:
1. Zmień w config.yml: `retention-days: 0` (testowo)
2. `/funnymisc reload`
3. `/backup cleanup`

**Oczekiwany wynik**:
- ✅ Wiadomość: "✔ Usunięto X starych backupów..."
- ✅ GUI jest pusty lub ma tylko najnowsze

**Przywróć po teście**:
```yaml
retention-days: 30
```

**Screenshoty do zrobienia**:
- [ ] Wiadomość o czyszczeniu

---

### ✅ TEST 11: Metadata (XP, Lokalizacja, Efekty)

**Cel**: Sprawdzenie czy metadata jest zapisywana

**Kroki**:
1. Daj sobie XP: `/xp add @s 100 levels`
2. Daj sobie efekty: `/effect give @s speed 999 1`
3. Zginij: `/kill`
4. Sprawdź backup (PPM - preview lub `/backup info <id>`)

**Oczekiwany wynik**:
- ✅ Lore backupu pokazuje XP: "Poziom 100"
- ✅ `/backup info` pokazuje efekty: "Speed 1"
- ✅ Po przywróceniu: XP i efekty wracają!

**Screenshoty do zrobienia**:
- [ ] Lore z XP
- [ ] Info z efektami
- [ ] Przywrócony XP

---

### ✅ TEST 12: Kompresja

**Cel**: Sprawdzenie czy GZIP działa (68% oszczędności)

**Kroki**:
1. Sprawdź bazę danych:

**SQLite**:
```bash
cd plugins/FunnyMisc
sqlite3 database.db

SELECT id, LENGTH(inventory_data) as size_bytes, 
       LENGTH(inventory_data)/1024.0 as size_kb
FROM player_backups 
ORDER BY id DESC LIMIT 5;
```

**Oczekiwany wynik**:
- ✅ Rozmiar ~200-500 bytes (zamiast ~1500 bez kompresji)

**Screenshoty do zrobienia**:
- [ ] Screenshot z SQL query

---

### ✅ TEST 13: Enderchest

**Cel**: Sprawdzenie czy enderchest jest zapisywany

**Kroki**:
1. Włącz w config: `save-extra-data.enderchest: true`
2. `/funnymisc reload`
3. Włóż itemy do enderchesta
4. Zginij
5. Sprawdź bazę czy `enderchest_data` nie jest NULL

**Oczekiwany wynik**:
- ✅ Kolumna `enderchest_data` ma dane
- ✅ Po przywróceniu: Enderchest wraca!

**Screenshoty do zrobienia**:
- [ ] SQL z enderchest_data

---

### ✅ TEST 14: Deduplikacja

**Cel**: Sprawdzenie czy nie tworzy duplikatów

**Kroki**:
1. Włącz: `deduplication.enabled: true`
2. Zginij 2 razy w ciągu 30s **bez zmiany ekwipunku**
3. Sprawdź GUI

**Oczekiwany wynik**:
- ✅ Tylko **1 backup** (nie dwa!)
- ✅ W logach: "Backup nie utworzony - inwentarz identyczny"

**Screenshoty do zrobienia**:
- [ ] Logi z deduplikacją
- [ ] GUI z 1 backupem

---

## 📊 TESTY WYDAJNOŚCIOWE

### ⚡ TEST 15: Wydajność - Tworzenie Backupu

**Cel**: Zmierzyć czas tworzenia backupu

**Kroki**:
1. Włącz `debug: true` w config
2. Zginij
3. Sprawdź logi

**Oczekiwany wynik**:
```
[DEBUG] Backup utworzony w 15ms
```

- ✅ Czas < 50ms (async!)

**Screenshoty do zrobienia**:
- [ ] Logi z czasem

---

### ⚡ TEST 16: Wydajność - Przywracanie

**Cel**: Zmierzyć czas przywracania

**Kroki**:
1. Przywróć backup
2. Sprawdź logi

**Oczekiwany wynik**:
```
[DEBUG] Backup przywrócony w 25ms
```

- ✅ Czas < 100ms

**Screenshoty do zrobienia**:
- [ ] Logi z czasem

---

### ⚡ TEST 17: Lag Test - 50 Graczy Jednocześnie

**Cel**: Sprawdzić czy serwer lubi się przy wielu backupach

**Kroki**:
1. Symuluj 50 graczy ginących jednocześnie:
```bash
# Bot plugin lub skrypt
for i in 1..50; simulate death
```

2. Sprawdź TPS: `/tps`

**Oczekiwany wynik**:
- ✅ TPS >= 19.5 (brak lagów!)
- ✅ Async queue obsługuje bez problemu

**Screenshoty do zrobienia**:
- [ ] TPS podczas testu

---

## 🛡️ TESTY BEZPIECZEŃSTWA

### 🔒 TEST 18: Permissions

**Cel**: Sprawdzić czy permissions działają

**Kroki**:
1. Usuń wszystkie uprawnienia backup
2. Spróbuj:
   - `/backup <nick>` 
   - `/backup create`
   - Shift+Klik przywracanie

**Oczekiwany wynik**:
- ✅ Każda akcja: "Nie masz uprawnień!"

**Screenshoty do zrobienia**:
- [ ] Wiadomości o braku uprawnień

---

### 🔒 TEST 19: Anty-Duping Bypass

**Cel**: Sprawdzić czy da się ominąć single-use

**Kroki**:
1. Daj uprawnienie: `funnymisc.backup.bypass.single-use`
2. Przywróć backup 2 razy

**Oczekiwany wynik**:
- ✅ Backup można użyć wielokrotnie (tylko z permem!)

**Screenshoty do zrobienia**:
- [ ] Drugi raz przywrócony backup

---

## 🗄️ TESTY BAZY DANYCH

### 💾 TEST 20: SQLite - Weryfikacja Tabel

**Cel**: Sprawdzić strukturę bazy

**Kroki**:
```bash
cd plugins/FunnyMisc
sqlite3 database.db

.tables
# Powinny być: player_backups, player_activity

.schema player_backups
```

**Oczekiwany wynik**:
```sql
CREATE TABLE player_backups (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    uuid VARCHAR(36) NOT NULL,
    player_name VARCHAR(16) NOT NULL,
    timestamp BIGINT NOT NULL,
    reason TINYINT NOT NULL,
    inventory_data TEXT NOT NULL,
    armor_data TEXT NOT NULL,
    enderchest_data TEXT,
    metadata TEXT,
    is_restored BOOLEAN DEFAULT 0,
    restored_by VARCHAR(36),
    restored_at BIGINT
);
```

**Screenshoty do zrobienia**:
- [ ] Output `.schema`

---

### 💾 TEST 21: MySQL Migration (Opcjonalnie)

**Cel**: Test migracji SQLite → MySQL

**Kroki**:
1. Backup SQLite: `cp database.db database.db.backup`
2. Zmień config na MySQL
3. Restart serwera
4. Sprawdź czy tabele utworzone

**Oczekiwany wynik**:
- ✅ Tabele `player_backups` i `player_activity` w MySQL
- ✅ Stare backupy w SQLite (nie migrują auto!)

---

## 📝 CHECKLIST FINALNY

Po wykonaniu wszystkich testów, wypełnij:

### Funkcjonalność:
- [ ] ✅ Backup przy śmierci
- [ ] ✅ Preview działa
- [ ] ✅ Przywracanie działa
- [ ] ✅ Single-use (anty-duping)
- [ ] ✅ Cooldown
- [ ] ✅ Paginacja (45+ backupów)
- [ ] ✅ Limity per gracz
- [ ] ✅ Manualny backup
- [ ] ✅ Info o backupie
- [ ] ✅ Cleanup
- [ ] ✅ Metadata (XP, efekty)
- [ ] ✅ Kompresja GZIP
- [ ] ✅ Enderchest
- [ ] ✅ Deduplikacja

### Wydajność:
- [ ] ✅ Czas tworzenia < 50ms
- [ ] ✅ Czas przywracania < 100ms
- [ ] ✅ Brak lagów przy 50 graczach
- [ ] ✅ TPS >= 19.5

### Bezpieczeństwo:
- [ ] ✅ Permissions działają
- [ ] ✅ Anty-duping działa
- [ ] ✅ Bypass permission działa

### Baza Danych:
- [ ] ✅ Tabele utworzone
- [ ] ✅ Kompresja działa
- [ ] ✅ Indeksy istnieją

---

## 🐛 Błędy do Sprawdzenia

Sprawdź logi pod kątem:
```
❌ WARN: Connection pool exhausted
❌ ERROR: Failed to create backup
❌ ERROR: Failed to restore backup
❌ NullPointerException
❌ SQLException
```

**Jeśli widzisz błędy**: Skopiuj do raportu i opisz kroki reprodukcji!

---

## 📊 Raport Testów (Template)

```markdown
# Raport Testów - System Backupów v1.0.5

**Data**: 2026-01-20
**Tester**: <Nick>
**Serwer**: Minecraft 1.21.4 + Paper

## Wyniki:
| Test | Status | Uwagi |
|------|--------|-------|
| TEST 1: Auto backup | ✅ PASS | - |
| TEST 2: Preview | ✅ PASS | - |
| TEST 3: Przywracanie | ✅ PASS | - |
| ... | ... | ... |

## Znalezione Błędy:
1. [Opisz błąd jeśli znaleziony]

## Wydajność:
- Czas tworzenia: XYms
- Czas przywracania: XYms
- TPS podczas testu: XX.X

## Werdykt:
✅ GOTOWE DO PRODUKCJI / ❌ WYMAGA POPRAWEK
```

---

**Powodzenia w testowaniu!** 🚀

**Data utworzenia**: 2026-01-20
**Status**: ✅ Config naprawiony, gotowe do testów
