# 📋 Przykłady Konfiguracji - Gotowe do Skopiowania

## 📗 Konfiguracja dla 100 graczy (SQLite)

```yaml
# ═══════════════════════════════════════════════════════════════
# KONFIGURACJA BAZY DANYCH
# ═══════════════════════════════════════════════════════════════
database:
  type: "sqlite"
```

**✅ To wszystko! SQLite jest skonfigurowany automatycznie.**

---

## 📘 Konfiguracja dla 200 graczy (MySQL)

```yaml
# ═══════════════════════════════════════════════════════════════
# KONFIGURACJA BAZY DANYCH - 200 GRACZY
# ═══════════════════════════════════════════════════════════════
database:
  type: "mysql"
  mysql:
    host: "localhost"
    port: 3306
    database: "funnymisc"
    username: "funnymisc_user"
    password: "ZMIEN_TO_HASLO"
    use-ssl: false
    pool-size: 25
    min-idle: 5
```

### MySQL Setup:
```sql
CREATE DATABASE funnymisc CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'funnymisc_user'@'localhost' IDENTIFIED BY 'ZMIEN_TO_HASLO';
GRANT ALL PRIVILEGES ON funnymisc.* TO 'funnymisc_user'@'localhost';
FLUSH PRIVILEGES;
```

### my.cnf optimization:
```ini
[mysqld]
max_connections = 100
innodb_buffer_pool_size = 256M
query_cache_size = 64M
thread_cache_size = 16
```

---

## 📙 Konfiguracja dla 300 graczy (MySQL)

```yaml
# ═══════════════════════════════════════════════════════════════
# KONFIGURACJA BAZY DANYCH - 300 GRACZY
# ═══════════════════════════════════════════════════════════════
database:
  type: "mysql"
  mysql:
    host: "localhost"
    port: 3306
    database: "funnymisc"
    username: "funnymisc_user"
    password: "BARDZO_SILNE_HASLO_123"
    use-ssl: false
    pool-size: 35
    min-idle: 7
```

### MySQL Setup:
```sql
CREATE DATABASE funnymisc CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'funnymisc_user'@'localhost' IDENTIFIED BY 'BARDZO_SILNE_HASLO_123';
GRANT ALL PRIVILEGES ON funnymisc.* TO 'funnymisc_user'@'localhost';
FLUSH PRIVILEGES;
```

### my.cnf optimization:
```ini
[mysqld]
max_connections = 150
innodb_buffer_pool_size = 512M
query_cache_size = 128M
thread_cache_size = 32
innodb_log_file_size = 128M
innodb_flush_log_at_trx_commit = 2
```

---

## 📕 Konfiguracja dla 400 graczy (MySQL Dedykowany)

```yaml
# ═══════════════════════════════════════════════════════════════
# KONFIGURACJA BAZY DANYCH - 400 GRACZY
# ═══════════════════════════════════════════════════════════════
database:
  type: "mysql"
  mysql:
    host: "mysql.twoja-domena.pl"  # Dedykowany serwer
    port: 3306
    database: "funnymisc"
    username: "funnymisc_user"
    password: "ULTRA_SILNE_HASLO_2024!"
    use-ssl: true  # SSL dla zdalnego połączenia
    pool-size: 45
    min-idle: 9
```

### MySQL Setup:
```sql
CREATE DATABASE funnymisc CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'funnymisc_user'@'%' IDENTIFIED BY 'ULTRA_SILNE_HASLO_2024!';
GRANT ALL PRIVILEGES ON funnymisc.* TO 'funnymisc_user'@'%';
FLUSH PRIVILEGES;
```

### my.cnf optimization (Dedykowany serwer 4GB RAM):
```ini
[mysqld]
max_connections = 200
innodb_buffer_pool_size = 1G
query_cache_size = 256M
thread_cache_size = 64
innodb_log_file_size = 256M
innodb_flush_log_at_trx_commit = 2
innodb_flush_method = O_DIRECT
innodb_io_capacity = 2000
```

---

## 📓 Konfiguracja dla 500 graczy (MySQL High-End)

```yaml
# ═══════════════════════════════════════════════════════════════
# KONFIGURACJA BAZY DANYCH - 500 GRACZY (HIGH-END)
# ═══════════════════════════════════════════════════════════════
database:
  type: "mysql"
  mysql:
    host: "10.0.0.5"  # Lokalny MySQL w sieci
    port: 3306
    database: "funnymisc"
    username: "funnymisc_prod"
    password: "ENTERPRISE_GRADE_PASSWORD_2024!"
    use-ssl: false  # Lokalny - nie potrzebny SSL
    pool-size: 60
    min-idle: 12
```

### MySQL Setup:
```sql
CREATE DATABASE funnymisc CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'funnymisc_prod'@'%' IDENTIFIED BY 'ENTERPRISE_GRADE_PASSWORD_2024!';
GRANT ALL PRIVILEGES ON funnymisc.* TO 'funnymisc_prod'@'%';
FLUSH PRIVILEGES;
```

### my.cnf optimization (Dedykowany serwer 8GB RAM):
```ini
[mysqld]
# Basic
max_connections = 250
port = 3306
bind-address = 0.0.0.0

# InnoDB
innodb_buffer_pool_size = 2G
innodb_buffer_pool_instances = 8
innodb_log_file_size = 512M
innodb_flush_log_at_trx_commit = 2
innodb_flush_method = O_DIRECT
innodb_io_capacity = 4000
innodb_io_capacity_max = 8000
innodb_read_io_threads = 8
innodb_write_io_threads = 8

# Query Cache
query_cache_type = 1
query_cache_size = 512M
query_cache_limit = 2M

# Thread
thread_cache_size = 128
max_allowed_packet = 64M

# Table Cache
table_open_cache = 4000
table_definition_cache = 2000

# Temp Tables
tmp_table_size = 64M
max_heap_table_size = 64M

# Logging (Production)
slow_query_log = 1
slow_query_log_file = /var/log/mysql/slow-query.log
long_query_time = 2
```

---

## 📔 Konfiguracja dla 500+ graczy (MySQL Cluster)

```yaml
# ═══════════════════════════════════════════════════════════════
# KONFIGURACJA BAZY DANYCH - 500+ GRACZY (CLUSTER)
# ═══════════════════════════════════════════════════════════════
database:
  type: "mysql"
  mysql:
    host: "proxysql.internal"  # ProxySQL Load Balancer
    port: 6033
    database: "funnymisc"
    username: "funnymisc_cluster"
    password: "CLUSTER_PASSWORD_ULTRA_SECURE_2024!"
    use-ssl: true
    pool-size: 75
    min-idle: 15
```

### ProxySQL Configuration:
```sql
-- Admin interface: 6032
-- MySQL interface: 6033

INSERT INTO mysql_servers(hostgroup_id, hostname, port) VALUES (1, '10.0.0.10', 3306); -- Master
INSERT INTO mysql_servers(hostgroup_id, hostname, port) VALUES (2, '10.0.0.11', 3306); -- Slave 1
INSERT INTO mysql_servers(hostgroup_id, hostname, port) VALUES (2, '10.0.0.12', 3306); -- Slave 2

INSERT INTO mysql_users(username, password, default_hostgroup) VALUES ('funnymisc_cluster', 'CLUSTER_PASSWORD_ULTRA_SECURE_2024!', 1);

INSERT INTO mysql_query_rules(rule_id, active, match_pattern, destination_hostgroup) VALUES (1, 1, '^SELECT.*FOR UPDATE$', 1);
INSERT INTO mysql_query_rules(rule_id, active, match_pattern, destination_hostgroup) VALUES (2, 1, '^SELECT', 2);

LOAD MYSQL SERVERS TO RUNTIME;
LOAD MYSQL USERS TO RUNTIME;
LOAD MYSQL QUERY RULES TO RUNTIME;
```

**⚠️ Ta konfiguracja wymaga profesjonalnego DBA i enterprise infrastructure!**

---

## 🔐 Security Best Practices

### 1. Silne hasła:
```bash
# Generuj silne hasło (Linux/Mac)
openssl rand -base64 32

# Lub (Windows PowerShell)
-join ((33..126) | Get-Random -Count 32 | % {[char]$_})
```

### 2. Dedykowany użytkownik (nie root):
```sql
-- ✅ Dobrze
CREATE USER 'funnymisc_user'@'localhost' IDENTIFIED BY 'silne_haslo';
GRANT ALL PRIVILEGES ON funnymisc.* TO 'funnymisc_user'@'localhost';

-- ❌ Źle (nigdy nie używaj root w produkcji!)
username: "root"
```

### 3. Ograniczenie dostępu:
```sql
-- Tylko localhost
CREATE USER 'funnymisc'@'localhost' IDENTIFIED BY 'haslo';

-- Konkretny IP
CREATE USER 'funnymisc'@'192.168.1.100' IDENTIFIED BY 'haslo';

-- Wszystkie hosty (tylko dla dedykowanych serwerów z firewall!)
CREATE USER 'funnymisc'@'%' IDENTIFIED BY 'haslo';
```

### 4. Firewall:
```bash
# UFW (Ubuntu)
sudo ufw allow from 192.168.1.100 to any port 3306

# iptables
sudo iptables -A INPUT -p tcp -s 192.168.1.100 --dport 3306 -j ACCEPT
```

---

## 📊 Monitoring Commands

### Sprawdzenie połączeń:
```sql
-- Obecne połączenia
SHOW STATUS LIKE 'Threads_connected';

-- Maksymalne użyte
SHOW STATUS LIKE 'Max_used_connections';

-- Lista aktywnych połączeń
SHOW PROCESSLIST;

-- Szczegółowe info o połączeniach
SELECT * FROM information_schema.processlist;
```

### Sprawdzenie wydajności:
```sql
-- Statystyki InnoDB
SHOW ENGINE INNODB STATUS\G

-- Statystyki query cache
SHOW STATUS LIKE 'Qcache%';

-- Slow queries
SHOW STATUS LIKE 'Slow_queries';
```

---

## ✅ Checklist przed deploymentem

- [ ] Zmieniłem domyślne hasło
- [ ] Utworzyłem dedykowanego użytkownika (nie root)
- [ ] Skonfigurowałem firewall
- [ ] Ustawiłem odpowiedni pool-size
- [ ] Zoptymalizowałem MySQL (my.cnf)
- [ ] Przetestowałem na serwerze testowym
- [ ] Wykonałem backup
- [ ] Skonfigurowałem automatyczne backupy
- [ ] Włączyłem monitoring
- [ ] Sprawdziłem max_connections w MySQL

---

## 🆘 Quick Troubleshooting

### Connection refused:
```bash
# Sprawdź czy MySQL działa
sudo systemctl status mysql

# Start MySQL
sudo systemctl start mysql

# Sprawdź port
netstat -an | grep 3306
```

### Access denied:
```sql
-- Sprawdź użytkowników
SELECT user, host FROM mysql.user;

-- Reset hasła
ALTER USER 'funnymisc_user'@'localhost' IDENTIFIED BY 'nowe_haslo';
FLUSH PRIVILEGES;
```

### Too many connections:
```sql
-- Zwiększ max_connections
SET GLOBAL max_connections = 200;

-- Lub w my.cnf
[mysqld]
max_connections = 200
```

---

**Tip:** Zawsze testuj konfigurację na serwerze testowym przed wdrożeniem na produkcję!

---

## 📦 Konfiguracja Backupów (v1.0.5+)

### Minimalna (Domyślna - Działa out-of-the-box)
```yaml
backup:
  enabled: true
  auto-backup:
    on-death: true
    on-logout: false
  max-backups:
    default: 5
```

### Zalecana dla Małych Serwerów (50-100 graczy)
```yaml
backup:
  enabled: true
  
  auto-backup:
    on-death: true
    on-logout: false  # Wyłączone - może powodować lagi
    on-error: true
  
  max-backups:
    default: 5
    vip: 10
    svip: 20
    admin: 50
  
  cleanup:
    enabled: true
    retention-days: 30
    check-interval: 12  # Co 12h
    
    inactive-players:
      enabled: true
      days-offline: 14  # 2 tygodnie
      keep-latest: true
  
  security:
    require-confirmation: true
    restore-cooldown: 60
    single-use: true
  
  compression:
    enabled: true
```

### Zalecana dla Dużych Serwerów (200+ graczy)
```yaml
backup:
  enabled: true
  
  auto-backup:
    on-death: true
    on-logout: false  # NIGDY nie włączaj na dużych serwerach!
    on-error: true
  
  max-backups:
    default: 5
    vip: 15
    svip: 30
    admin: 100
  
  cleanup:
    enabled: true
    retention-days: 20  # Krócej - oszczędność miejsca
    check-interval: 6   # Co 6h
    
    inactive-players:
      enabled: true
      days-offline: 7   # Tydzień
      keep-latest: true
      min-backups-to-keep: 1
  
  security:
    require-confirmation: true
    restore-cooldown: 120  # 2 minuty
    single-use: true
  
  compression:
    enabled: true
  
  deduplication:
    enabled: true
    min-interval: 60  # Min 60s między backupami
  
  save-extra-data:
    xp: true
    enderchest: false  # Wyłączone - oszczędność miejsca
  
  debug: false
```

### Przykład Personalizacji Wiadomości
```yaml
backup:
  # ... inne opcje ...
  
  # Wiadomości można łatwo dostosować:
  gui:
    title: "<gradient:#FFD700:#FF8C00>Backupy:</gradient> <yellow><player></yellow>"
    
    item:
      active:
        name: "<rainbow><date></rainbow>"
        click: "<green>👁 Kliknij aby zobaczyć</green>"
        shift: "<yellow>⚡ Shift+Klik aby przywrócić</yellow>"
```

---

**Ostatnia aktualizacja:** 2026-01-20
