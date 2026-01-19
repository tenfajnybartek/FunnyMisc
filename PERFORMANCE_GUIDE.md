# 📊 Przewodnik Wydajnościowy FunnyMisc

## 🎯 Rekomendacje dla różnych wielkości serwerów

### 📗 Mały Serwer (50-100 graczy)

#### SQLite (Rekomendowane)
```yaml
database:
  type: "sqlite"
```

**Parametry:**
- Pool Size: 5 (automatycznie)
- WAL Mode: Włączony automatycznie
- Idealny dla tej wielkości

**Zalety:**
- ✅ Zero konfiguracji
- ✅ Automatyczne backupy
- ✅ Wystarczająca wydajność
- ✅ Brak kosztów dodatkowych

---

### 📘 Średni Serwer (100-200 graczy)

#### MySQL (Rekomendowane)
```yaml
database:
  type: "mysql"
  mysql:
    host: "localhost"
    port: 3306
    database: "funnymisc"
    username: "root"
    password: "twoje_haslo"
    use-ssl: false
    pool-size: 25
    min-idle: 5
```

**Wymagania MySQL:**
```sql
-- my.cnf / my.ini
max_connections = 100
innodb_buffer_pool_size = 256M
query_cache_size = 64M
thread_cache_size = 16
```

**Wydajność:**
- ⚡ ~200-500 zapytań/sekundę
- ⏱️ <5ms średni czas odpowiedzi
- 💾 ~50MB RAM (plugin)
- 🔄 Async operacje

---

### 📙 Duży Serwer (200-300 graczy)

#### MySQL (Wymagane)
```yaml
database:
  type: "mysql"
  mysql:
    host: "localhost"
    port: 3306
    database: "funnymisc"
    username: "funnymisc_user"
    password: "silne_haslo"
    use-ssl: false
    pool-size: 35
    min-idle: 7
```

**Wymagania MySQL:**
```sql
-- my.cnf / my.ini
max_connections = 150
innodb_buffer_pool_size = 512M
query_cache_size = 128M
thread_cache_size = 32
innodb_log_file_size = 128M
innodb_flush_log_at_trx_commit = 2
```

**Wydajność:**
- ⚡ ~500-1000 zapytań/sekundę
- ⏱️ <10ms średni czas odpowiedzi
- 💾 ~100MB RAM (plugin)
- 🔄 Pełna obsługa async

**Zalecenia:**
- 🖥️ Dedykowany serwer MySQL (nie shared hosting)
- 💾 SSD dla MySQL data directory
- 📊 Monitorowanie przez MySQL Workbench
- 🔐 Osobny użytkownik MySQL (nie root)

---

### 📕 Bardzo Duży Serwer (300-400 graczy)

#### MySQL (Wymagane + Optymalizacje)
```yaml
database:
  type: "mysql"
  mysql:
    host: "mysql.twojadomena.pl"  # Dedykowany serwer
    port: 3306
    database: "funnymisc"
    username: "funnymisc_user"
    password: "bardzo_silne_haslo"
    use-ssl: true  # Jeśli zdalny serwer
    pool-size: 45
    min-idle: 9
```

**Wymagania MySQL:**
```sql
-- my.cnf / my.ini
max_connections = 200
innodb_buffer_pool_size = 1G
query_cache_size = 256M
thread_cache_size = 64
innodb_log_file_size = 256M
innodb_flush_log_at_trx_commit = 2
innodb_flush_method = O_DIRECT
innodb_io_capacity = 2000
```

**Wydajność:**
- ⚡ ~1000-2000 zapytań/sekundę
- ⏱️ <15ms średni czas odpowiedzi
- 💾 ~150MB RAM (plugin)
- 🔄 Zaawansowane async + batch operations

**Zalecenia:**
- 🖥️ **WYMAGANY** dedykowany serwer MySQL
- 💾 **WYMAGANY** SSD NVMe
- 🌐 Preferowane: lokalny MySQL (nie zdalny)
- 📊 Monitoring 24/7
- 🔧 Tunning MySQL przez DBA
- 💰 Budget: ~50-100 PLN/m na MySQL VPS

---

### 📓 Ekstremalny Serwer (400-500 graczy)

#### MySQL (Zaawansowana Konfiguracja)
```yaml
database:
  type: "mysql"
  mysql:
    host: "10.0.0.5"  # Lokalny serwer w sieci
    port: 3306
    database: "funnymisc"
    username: "funnymisc_user"
    password: "ultra_silne_haslo_128bit"
    use-ssl: false  # Lokalny - nie trzeba
    pool-size: 60
    min-idle: 12
```

**Wymagania MySQL:**
```sql
-- my.cnf / my.ini
max_connections = 250
innodb_buffer_pool_size = 2G
innodb_buffer_pool_instances = 8
query_cache_size = 512M
thread_cache_size = 128
innodb_log_file_size = 512M
innodb_flush_log_at_trx_commit = 2
innodb_flush_method = O_DIRECT
innodb_io_capacity = 4000
innodb_io_capacity_max = 8000
innodb_read_io_threads = 8
innodb_write_io_threads = 8
table_open_cache = 4000
tmp_table_size = 64M
max_heap_table_size = 64M
```

**Sprzęt:**
- **CPU:** 4+ rdzenie (dedykowane dla MySQL)
- **RAM:** 4GB+ (tylko MySQL)
- **Dysk:** SSD NVMe (min 500MB/s write)
- **Sieć:** 1Gbps (jeśli zdalny)

**Wydajność:**
- ⚡ ~2000-3000 zapytań/sekundę
- ⏱️ <20ms średni czas odpowiedzi
- 💾 ~200MB RAM (plugin)
- 🔄 Pełna optymalizacja async

**Zalecenia:**
- 🖥️ **WYMAGANY** dedykowany serwer MySQL (min 4GB RAM)
- 💾 **WYMAGANY** SSD NVMe
- 🌐 **WYMAGANE:** lokalny MySQL w tej samej sieci
- 📊 **WYMAGANY:** Professional monitoring (Grafana + Prometheus)
- 🔧 **WYMAGANY:** DBA do tuningu
- 💰 Budget: ~150-300 PLN/m na MySQL VPS
- ⚡ Rozważ MySQL Cluster dla HA

---

### 📔 Mega Serwer (500+ graczy)

#### MySQL Cluster / ProxySQL
```yaml
database:
  type: "mysql"
  mysql:
    host: "proxysql.internal"  # ProxySQL load balancer
    port: 6033
    database: "funnymisc"
    username: "funnymisc_user"
    password: "enterprise_grade_password"
    use-ssl: true
    pool-size: 75
    min-idle: 15
```

**Architektura:**
```
Plugin (75 połączeń)
    ↓
ProxySQL (Load Balancer)
    ↓
MySQL Master-Slave Replication
    ├─ Master (Write)
    ├─ Slave 1 (Read)
    └─ Slave 2 (Read)
```

**Wymagania:**
- **MySQL Master:** 8GB RAM, 8 cores, NVMe
- **MySQL Slave 1:** 4GB RAM, 4 cores, NVMe
- **MySQL Slave 2:** 4GB RAM, 4 cores, NVMe
- **ProxySQL:** 2GB RAM, 2 cores, SSD

**Wydajność:**
- ⚡ ~5000+ zapytań/sekundę
- ⏱️ <25ms średni czas odpowiedzi
- 💾 ~250MB RAM (plugin)
- 🔄 Enterprise-grade async

**Zalecenia:**
- 👨‍💼 **WYMAGANY:** Profesjonalny DBA
- 🏢 **WYMAGANE:** Enterprise hosting
- 📊 **WYMAGANE:** 24/7 monitoring + alerting
- 🔐 **WYMAGANE:** SSL/TLS + firewall
- 💰 Budget: ~500-1000 PLN/m
- ☁️ Rozważ: AWS RDS, Google Cloud SQL, Azure Database

---

## 🔧 Optymalizacje dodatkowe

### 1. Backup automatyczny (wszystkie wielkości)

```bash
# Cron job - codziennie o 3:00
0 3 * * * mysqldump -u root -p funnymisc > /backups/funnymisc-$(date +\%Y\%m\%d).sql
```

### 2. Monitoring wydajności

**Logi FunnyMisc:**
- Sprawdzaj: `[FunnyMisc] Błąd pobierania połączenia`
- Jeśli występują - zwiększ `pool-size`

**MySQL:**
```sql
-- Sprawdź obecne użycie połączeń
SHOW STATUS LIKE 'Threads_connected';
SHOW STATUS LIKE 'Max_used_connections';

-- Powinno być: Max_used_connections < max_connections × 0.8
```

### 3. Czyszczenie starych danych (opcjonalne)

```sql
-- Usuń graczy którzy nie byli online 365 dni
-- (wymaga dodatkowego pola last_seen w bazie)
-- Możliwe do dodania w przyszłości
```

---

## 📈 Tabela porównawcza

| Wielkość | Gracze | Baza | Pool Size | MySQL RAM | MySQL CPU | Koszt/m |
|----------|--------|------|-----------|-----------|-----------|---------|
| 📗 Mały | 50-100 | SQLite | 5 | - | - | 0 PLN |
| 📘 Średni | 100-200 | MySQL | 25 | 256MB | 2 cores | 0-30 PLN |
| 📙 Duży | 200-300 | MySQL | 35 | 512MB | 4 cores | 50-100 PLN |
| 📕 Bardzo Duży | 300-400 | MySQL | 45 | 1GB | 4 cores | 100-200 PLN |
| 📓 Ekstremalny | 400-500 | MySQL | 60 | 2GB | 8 cores | 150-300 PLN |
| 📔 Mega | 500+ | MySQL HA | 75+ | 4GB+ | 16+ cores | 500+ PLN |

---

## ⚠️ Troubleshooting

### Problem: "Connection timeout after 10000ms"

**Przyczyna:** Za mało połączeń w poolu

**Rozwiązanie:**
1. Zwiększ `pool-size` o 10-15
2. Zrestartuj serwer
3. Monitoruj logi

---

### Problem: Duże opóźnienia (lag)

**Przyczyna:** Baza danych jest przeciążona

**Rozwiązanie:**
1. Sprawdź `max_connections` w MySQL
2. Zwiększ `innodb_buffer_pool_size`
3. Rozważ upgrade serwera MySQL
4. Sprawdź czy dysk nie jest pełny

---

### Problem: Crash serwera MySQL

**Przyczyna:** Za mało RAM lub źle skonfigurowana baza

**Rozwiązanie:**
1. Zmniejsz `innodb_buffer_pool_size` do 50% RAM
2. Sprawdź logi MySQL: `/var/log/mysql/error.log`
3. Rozważ upgrade RAM
4. Skontaktuj się z hostingiem

---

## 🎓 Best Practices

1. ✅ **Zawsze testuj** na testowym serwerze przed produkcją
2. ✅ **Regularnie backupuj** bazę danych (dziennie minimum)
3. ✅ **Monitoruj logi** - sprawdzaj errors codziennie
4. ✅ **Aktualizuj MySQL** - zawsze najnowsza stabilna wersja
5. ✅ **Używaj SSL** jeśli MySQL jest zdalny
6. ✅ **Nie używaj root** - stwórz dedykowanego usera
7. ✅ **Ogranicz dostęp** - firewall tylko dla IP serwera MC
8. ✅ **Dokumentuj zmiany** - zapisuj co i kiedy zmieniałeś

---

## 📞 Wsparcie

Jeśli masz problemy wydajnościowe:

1. Sprawdź logi: `plugins/FunnyMisc/logs/`
2. Sprawdź MySQL logi: `/var/log/mysql/error.log`
3. Uruchom: `SHOW PROCESSLIST;` w MySQL
4. Sprawdź dostępną pamięć RAM: `free -h`
5. Sprawdź obciążenie CPU: `top` lub `htop`

---

**Ostatnia aktualizacja:** 2026-01-19
**Wersja pluginu:** 1.0.0-SNAPSHOT
**Autor:** tenfajnybartek
