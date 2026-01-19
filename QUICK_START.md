# ⚡ Quick Start - Konfiguracja dla różnych wielkości serwerów

## 📗 Mały Serwer (50-100 graczy)

### Nic nie zmieniaj! 
Plugin jest już skonfigurowany dla tej wielkości.

```yaml
# config.yml - domyślna konfiguracja
database:
  type: "sqlite"  # ✅ Wystarczające dla 100 graczy
```

**✅ Gotowe do użycia!**

---

## 📘 Średni Serwer (100-200 graczy)

### Zmień na MySQL:

**1. Zainstaluj MySQL:**
```bash
# Ubuntu/Debian
sudo apt install mysql-server

# Lub użyj hostingu z MySQL
```

**2. Utwórz bazę:**
```sql
CREATE DATABASE funnymisc;
CREATE USER 'funnymisc_user'@'localhost' IDENTIFIED BY 'twoje_silne_haslo';
GRANT ALL PRIVILEGES ON funnymisc.* TO 'funnymisc_user'@'localhost';
FLUSH PRIVILEGES;
```

**3. Zmień config.yml:**
```yaml
database:
  type: "mysql"
  mysql:
    host: "localhost"
    port: 3306
    database: "funnymisc"
    username: "funnymisc_user"
    password: "twoje_silne_haslo"
    use-ssl: false
    pool-size: 25     # ✅ Dla 200 graczy
    min-idle: 5
```

**4. Restart serwera:**
```bash
/stop
# Start ponownie
```

**✅ Gotowe!**

---

## 📙 Duży Serwer (200-300 graczy)

### To samo co wyżej, ale zwiększ pool-size:

```yaml
database:
  type: "mysql"
  mysql:
    # ... pozostałe ustawienia ...
    pool-size: 35     # ✅ Dla 300 graczy
    min-idle: 7
```

### Dodatkowo zoptymalizuj MySQL:

**Edytuj `/etc/mysql/my.cnf` (lub `my.ini` na Windows):**
```ini
[mysqld]
max_connections = 150
innodb_buffer_pool_size = 512M
query_cache_size = 128M
thread_cache_size = 32
```

**Restart MySQL:**
```bash
sudo systemctl restart mysql
```

**✅ Gotowe!**

---

## 📕 Bardzo Duży Serwer (300-400 graczy)

### Pool size:
```yaml
database:
  mysql:
    pool-size: 45     # ✅ Dla 400 graczy
    min-idle: 9
```

### MySQL optimization:
```ini
[mysqld]
max_connections = 200
innodb_buffer_pool_size = 1G
query_cache_size = 256M
thread_cache_size = 64
innodb_log_file_size = 256M
```

**⚠️ Wymagany dedykowany serwer MySQL (min 2GB RAM)**

**✅ Gotowe!**

---

## 📓 Ekstremalny Serwer (400-500 graczy)

### Pool size:
```yaml
database:
  mysql:
    pool-size: 60     # ✅ Dla 500 graczy
    min-idle: 12
```

### MySQL optimization:
```ini
[mysqld]
max_connections = 250
innodb_buffer_pool_size = 2G
innodb_buffer_pool_instances = 8
query_cache_size = 512M
thread_cache_size = 128
innodb_log_file_size = 512M
innodb_io_capacity = 4000
```

**⚠️ Wymagany dedykowany serwer MySQL (min 4GB RAM, SSD NVMe)**

**✅ Gotowe!**

---

## 📔 Mega Serwer (500+ graczy)

### Pool size:
```yaml
database:
  mysql:
    host: "twoj-mysql-serwer.pl"  # Dedykowany serwer
    pool-size: 75     # ✅ Dla 500+ graczy
    min-idle: 15
    use-ssl: true     # Jeśli zdalny
```

### Rozważ MySQL Cluster:
- Master-Slave Replication
- ProxySQL jako load balancer
- Dedykowany DBA

**⚠️ Wymagane: Enterprise hosting + Professional DBA**

**📖 Zobacz pełny przewodnik:** [PERFORMANCE_GUIDE.md](PERFORMANCE_GUIDE.md)

---

## ✅ Checklist

### Przed uruchomieniem sprawdź:

- [ ] Wybrany odpowiedni typ bazy (`sqlite` lub `mysql`)
- [ ] Ustawiony poprawny `pool-size` dla liczby graczy
- [ ] MySQL ma `max_connections >= pool-size × 3`
- [ ] Serwer ma wystarczająco RAM
- [ ] Wykonany backup przed zmianami
- [ ] Przetestowane na serwerze testowym

---

## 🆘 Szybka pomoc

### Plugin nie startuje:
```bash
# 1. Sprawdź logi
tail -f logs/latest.log

# 2. Sprawdź config
cat plugins/FunnyMisc/config.yml

# 3. Test MySQL connection
mysql -u funnymisc_user -p funnymisc
```

### Connection timeout:
```yaml
# Zwiększ pool-size o 10-15
pool-size: 35  # było: 25
```

### Lagi:
```sql
-- Sprawdź połączenia MySQL
SHOW STATUS LIKE 'Threads_connected';
SHOW STATUS LIKE 'Max_used_connections';

-- Max_used powinno być < max_connections × 0.8
```

---

## 📞 Support

Problemy? Sprawdź:
1. [PERFORMANCE_GUIDE.md](PERFORMANCE_GUIDE.md) - Szczegółowy przewodnik
2. [README.md](README.md) - Pełna dokumentacja
3. Logi: `plugins/FunnyMisc/logs/`

---

**Ostatnia aktualizacja:** 2026-01-19
