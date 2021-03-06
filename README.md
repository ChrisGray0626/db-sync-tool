## 同步流程

```mermaid
graph LR
SrcDB --> Reader --> ValueFilter --> FieldMap --> Writer --> DstDB
```

## 执行器（Executor）

### 读取执行器（Reader）

- 统一的连接方法
- 统一的读取方法（Timed）

#### 数据过滤（Filter）

- 编写过滤规则
- 拼接SQL语句
- 方法read执行时进行条件查询（Timed）

#### MySQLReader

##### 二进制日志（binlog）

1. 在my.ini中配置开启binLog服务

    ```bash
    #开启binlog日志
    log_bin=ON
    #binlog日志的基本文件名
    log_bin_basename=/var/lib/mysql/mysql-bin
    #binlog文件的索引文件，管理所有binlog文件
    log_bin_index=/var/lib/mysql/mysql-bin.index
    #配置serverid
    server-id=1
    ```

1. 使用开源框架[mysql-binlog-connector-java]([shyiko/mysql-binlog-connector-java: MySQL Binary Log connector (github.com)](https://github.com/shyiko/mysql-binlog-connector-java))监听binlog

##### 数据格式

- TableMapEventData：包含变化的库名和表名
- WriteRowsEventData：包含新增数据内容（不含字段名），默认格式为List<Serializable[]>
- 不同事件类型对应EventData的不同EventType

#### PostgreSQLReader

##### 逻辑解码（Logical Decoding）

1. 配置wal_level = logical

1. 创建逻辑复制槽（Replication Slot），使用解码插件 （当前使用test_decoding）

   ```sql
   SELECT * FROM pg_create_logical_replication_slot('test_slot', 'test_decoding');
   ```

1. 从逻辑复制槽中获取变更数据

   ```sql
   -- 查看并消费
   pg_logical_slot_get_changes(slot_name name, ...)
   -- 只查看不消费
   pg_logical_slot_peek_changes(slot_name name, ...)
   ```

1. 定时轮询逻辑复制槽

##### 数据格式

- table：包含模式、表名、事件类型、数据内容（含字段名与字段类型），且均在一个字段data中
- 格式Text的数据自带单引号

##### 注意事项

- 如需修改逻辑复制槽名称，请使用PostgreSQLReader的方法setLogicalReplicationSlotName设置名称。

#### SQLServerReader

##### 变更数据捕获（Change Data Capture）

1. 启动 SQL Server 代理服务
1. 启动数据库CDC服务

    ```sql
    USE 'DBName'
    GO
    EXECUTE sys.sp_cdc_enable_db;
    GO
    -- 检查是否成功
    SELECT is_cdc_enabled FROM sys.databases WHERE NAME = 'DBName'
    ```

1. 启动库表CDC服务

    ```sql
    EXEC sys.sp_cdc_enable_table 
        @source_schema= 'dbo',
           @source_name = 'tableName',
           @role_name = N'cdc_Admin',
           @capture_instance = DEFAULT, -- 新建cdc表名称
           @supports_net_changes = 1,
        @index_name = NULL,
        @captured_column_list = NULL, -- 需要捕获的字段列表
        @filegroup_name = DEFAULT

    -- 检查是否成功
    SELECT name, is_tracked_by_cdc FROM sys.tables WHERE OBJECT_ID= OBJECT_ID('dbo.tableName')

    -- 禁用
    EXEC sys.sp_cdc_disable_table  
    @source_schema = N'dbo',  
    @source_name   = N'tableName',  
    @capture_instance = N'dbo_tableName'  
    ```

1. 查看捕获数据

    ```sql
    SELECT * FROM cdc.dbo_tableName_CT
    
    -- 查询间隔时间interval（分钟）内的捕获数据
    DECLARE @bglsn VARBINARY(10)=sys.fn_cdc_map_time_to_lsn('smallest greater than or equal',DATEADD(mi,-delayTime, GETDATE()));
    DECLARE @edlsn VARBINARY(10)=sys.fn_cdc_map_time_to_lsn('largest less than or equal',GETDATE());
    SELECT * FROM cdc.dbo_ + tableName + _CT WHERE [__$start_lsn] BETWEEN @bglsn AND @edlsn);
    ```

1. 定时轮询CDC库表

##### 数据格式

- dbo_tableName_CT：包含表名、事件类型、数据内容（含字段名）
- 表名直接为CDC表名的一部分
- 事件类型由字段__$operation表示
- 数据字段直接为CDC表字段的一部分

##### 注意事项

- 如果库表有字段更新需要禁用并重新启动CDC服务，否则将无法记录新增字段并且出错。

- 因为JDK版本不推荐使用旧的TLSV1.0的协议，所以默认删除TLS10的支持。

    > The server selected protocol version TLS10 is not accepted by client preferences [TLS12]"

    jre\lib\security文件夹下，编辑java.security文件，其中找到 jdk.tls.disabledAlgorithms配置项，将TLSv1, TLSv1.1, 3DES_EDE_CBC删除。

- CDC文件中字段内容末尾默认有大量空格。

### 写入执行器（Writer）

- 统一的连接方法
- 统一的写入方法

#### PostgreSQLWriter

## 数据过滤（ValueFilter）

- 过滤规则

### 数据过滤管理器（ValueFilterManager）

- 规则配置
- 语句拼接
- 管理运行
- 数据过滤在读取时完成

## 同步数据集（SyncData）

- 多线程安全：方法write加同步锁

### 监听器（SyncDataListener）

- 专门的监听触发方法trigger，调用后将调用监听器的方法doSet。

#### 数据格式

- Map<String, String> data

## 字段映射（FieldMap）

- 源字段
- 目标字段
- 映射规则
- 字段映射

### 字段映射管理器（FieldMapManager）

- 规则配置
- 格式检查
- 管理运行

### 映射检查

- 字段类型
- 字段包含
- 规则语法

### 规则语法

- 字段均使用大括号表示
- 目标字段在等号左侧
- 源字段在等号右侧

```properties
{name}={first_name} {last_name}
```

## 作业（Job）

### 作业管理器（JobManager）

- 多线程作业

### 监听同步数据集

1. 注册监听器（registerListener）
1. 监听同步数据集的数据变化
1. 重写监听器的方法doSet

```java
syncData.registerListener(event -> {
    writer.write(syncData);
});
```

## 配置信息（Conf）

#### 数据库配置（conf.properties)

```properties
conf.dbType = MYSQL
conf.hostname = localhost
conf.port = 3306
conf.dbName = test
conf.user = root
conf.password = 123456
conf.syncJobConfTableName = sync_job_conf
conf.fieldMapConfTableName = field_map_conf
conf.valueFilterConfTableName = value_filter_conf
```

### 配置规范

#### 作业类型（JobType）

- REAL（实时）
- TIMED（定时）

#### 数据库类型（DBType）

- MYSQL
- POSTGRESQL
- SQLSERVER

#### 事件类型（EventType）

- INSERT
- INCREMENTAL（增量）
- TOTAL（全量）

## 日志（Log）

### 存储方式（MySQL）

```sql
create table log(
	id int(4) PRIMARY KEY AUTO_INCREMENT,
	create_time datetime,
	logs VARCHAR(255)
)
```

### log4j配置

```properties
log4j.appender.Database=org.apache.log4j.jdbc.JDBCAppender
log4j.appender.Database.driver=com.mysql.cj.jdbc.Driver
log4j.appender.Database.URL=jdbc:mysql://localhost:3306/test?useUnicode=true&characterEncoding=UTF-8
log4j.appender.Database.user=root
log4j.appender.Database.password=123456
log4j.appender.Database.sql=insert into log (create_time, log) VALUES ("%d{yyyy-MM-dd hh:mm:ss}", "%c %m%n")
log4j.appender.Database.Threshold = ERROR
log4j.appender.Database.layout=org.apache.log4j.PatternLayout
```

## 注意事项

- Writer/Reader必须与实现类放在同一package下，否则无法动态加载实现类。
