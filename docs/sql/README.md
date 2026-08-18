# 本地数据库初始化

## 文件

| 文件 | 说明 |
| --- | --- |
| `museum_book_schema.sql` | 最终表结构（与当前 Entity 对齐） |
| `museum_book_seed_base.sql` | 头像 / 文物 / 消息模版 / 演示管理员 `admin`/`admin123` |
| `museum_book_seed_loadtest.sql` | 压测场馆+排期+80 用户/游客（由脚本生成） |
| `generate_loadtest_seed.py` | 按「今天」生成 loadtest seed 与 `docs/jmeter/mobiles.csv` |

根目录 `database_export-*.json` **不是** MySQL dump，勿用于导入。

## 导入（Windows）

```bat
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -uroot -p123456 -e "CREATE DATABASE IF NOT EXISTS museum_book DEFAULT CHARACTER SET utf8mb4;"
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -uroot -p123456 museum_book < docs\sql\museum_book_schema.sql
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -uroot -p123456 museum_book < docs\sql\museum_book_seed_base.sql
python docs\sql\generate_loadtest_seed.py
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -uroot -p123456 museum_book < docs\sql\museum_book_seed_loadtest.sql
```

密码以 `ZDYZ/src/main/resources/application.yml` 为准（当前 `root` / `123456`）。

换日压测前请重新跑 `generate_loadtest_seed.py` 再导入 loadtest seed，保证 `DAY` / `TIME_MARK` 落在今天起 7 天内。
