# SoftwareArchitecture
西电2022软件体系结构实时数仓ClickHouse上机实验
## ClickHouse

### 实验一

通过阅读官方文档在服务器上进行安装配置

```bash
# CentOS通过yum进行安装clickhouse-server,clickhouse-client
yum install -y yum-utils
yum-config-manager --add-repo https://packages.clickhouse.com/rpm/clickhouse.repo
yum install -y clickhouse-server clickhouse-client
# 启动clickhouse服务端，启动在8123端口
systemctl restart clickhouse-server
systemctl status clickhouse-server
# 进入clickhouse客户端
clickhouse-client
```

检查服务是否已经启动

```bash
curl http://localhost:8123/
# 返回ok则说明配置正确
```

### 实验二

##### 2.1 上传employee.csv文件，并插入clickhose中

进入clickhouse-client

```sql
# 建表
CREATE TABLE employee (
    id String,
    name String,
    bonus String,
    department String
) ENGINE = MergeTree ORDER BY(id);
```

使用以下命令导入*.csv到clickhouse中

```bash
clickhouse-client --query "INSERT INTO employee FORMAT CSVWithNames" < employee.csv
```

再次进入clickhouse-client，输入指令打印整个employee表

```sql
SELECT *
FROM employee
```

打印输出如下

```bash
SELECT *
FROM employee

Query id: 3c873e4d-932e-4082-8e3e-9ec37aca9a0d

┌─id─┬─name──┬─bonus─┬─department─┐
│ 1  │ Joe   │ 8500  │ IT         │
│ 2  │ Henry │ 8000  │ Sales      │
│ 3  │ Sam   │ 6000  │ Sales      │
│ 4  │ Max   │ 9000  │ IT         │
│ 5  │ Janet │ 6900  │ IT         │
│ 6  │ Randy │ 8500  │ IT         │
│ 7  │ Will  │ 7000  │ IT         │
└────┴───────┴───────┴────────────┘

7 rows in set. Elapsed: 0.002 sec. 
```

##### 2.2 使用Java连接clickhouse

使用非本地连接clickhouse需要进行配置文件/etc/lib/clickhouse-server/config.xml的更改，在vim中使用/listen_host进行搜索，找到该行配置进行解除注释，云服务器上还需要对安全组进行放开。

```xml
 <listen_host>::</listen_host>
```

建一个maven项目，导入相关依赖

```xml
<dependency>
	<groupId>ru.yandex.clickhouse</groupId>
	<artifactId>clickhouse-jdbc</artifactId>
	<version>0.1.40</version>
</dependency>
```

application.yml

```yml
clickhouse:
  address: jdbc:clickhouse://117.33.*.*:8123
#  username: default
#  password: xxx
# 无用户名密码登录
  username:
  password:
  db: default
  socketTimeout: 600000
```

工具类：ClickHouseUtil.java

```java 
@Slf4j
@Component
public class ClickHouseUtil {

    private static String clickhouseAddress;

    private static String clickhouseUsername;

    private static String clickhousePassword;

    private static String clickhouseDB;

    private static Integer clickhouseSocketTimeout;

    @Value("${clickhouse.address}")
    public  void setClickhouseAddress(String address) {
        ClickHouseUtil.clickhouseAddress = address;
    }
    @Value("${clickhouse.username}")
    public  void setClickhouseUsername(String username) {
        ClickHouseUtil.clickhouseUsername = username;
    }
    @Value("${clickhouse.password}")
    public  void setClickhousePassword(String password) {
        ClickHouseUtil.clickhousePassword = password;
    }
    @Value("${clickhouse.db}")
    public  void setClickhouseDB(String db) {
        ClickHouseUtil.clickhouseDB = db;
    }
    @Value("${clickhouse.socketTimeout}")
    public  void setClickhouseSocketTimeout(Integer socketTimeout) {
        ClickHouseUtil.clickhouseSocketTimeout = socketTimeout;
    }

    public static Connection getConn() {

        ClickHouseConnection conn = null;
        ClickHouseProperties properties = new ClickHouseProperties();
        properties.setUser(clickhouseUsername);
        properties.setPassword(clickhousePassword);
        properties.setDatabase(clickhouseDB);
        properties.setSocketTimeout(clickhouseSocketTimeout);
        ClickHouseDataSource clickHouseDataSource = new ClickHouseDataSource(clickhouseAddress,properties);
        try {
            conn = clickHouseDataSource.getConnection();
            return conn;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static List<JSONObject> exeSql(String sql){
        log.info("cliockhouse 执行sql：" + sql);
        Connection connection = getConn();
        try {
            Statement statement = connection.createStatement();
            ResultSet results = statement.executeQuery(sql);
            ResultSetMetaData rsmd = results.getMetaData();
            List<JSONObject> list = new ArrayList();
            while(results.next()){
                JSONObject row = new JSONObject();
                for(int i = 1;i<=rsmd.getColumnCount();i++){
                    row.put(rsmd.getColumnName(i),results.getString(rsmd.getColumnName(i)));
                }
                list.add(row);
            }

            return list;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
```

Test

```Java
@SpringBootTest
class ClickhouseDemoApplicationTests {
    @Test
    void contextLoads() {
        String sql="\n" +
                "CREATE TABLE library (\n" +
                "    id UInt16,\n" +
                "    visit_date Date,\n" +
                "    people UInt16\n" +
                ") ENGINE = MergeTree ORDER BY(id);";
        // 直接使用exeSql建表会发生空指针异常，需要对exeSql做少量修改
        ClickHouseUtil.createTableSql(sql);
    }
}

```

##### 2.3 使用http方式查询library表

```bash
# 其中%20为空格
curl http://117.33.*.*:8123/?query=SELECT%20*%20FROM%20library
```

输出

```bash
1       2017/1/1        10
2       2017/1/2        109
3       2017/1/3        150
4       2017/1/4        99
5       2017/1/5        145
6       2017/1/6        1455
7       2017/1/7        199
8       2017/1/8        188
```

### 实验三

##### 3.1 查询employee表的每个部门的奖金最高的前2人

```sql
select id, name, bonus, department from (
    select 
		id, name, bonus, department,
		dense_rank() OVER w AS dense_rank
	from employee
	WINDOW w AS (partition by department ORDER BY bonus desc)
	SETTINGS allow_experimental_window_functions = 1
) WHERE dense_rank < 3
```

打印输出如下

```bash
SELECT
    id,
    name,
    bonus,
    department
FROM
(
    SELECT
        id,
        name,
        bonus,
        department,
        dense_rank() OVER w AS dense_rank
    FROM employee
    WINDOW w AS (PARTITION BY department ORDER BY bonus DESC)
    SETTINGS allow_experimental_window_functions = 1
)
WHERE dense_rank < 3

Query id: ff921957-f1b1-4e82-81bd-24649949cdeb

┌─id─┬─name──┬─bonus─┬─department─┐
│ 4  │ Max   │ 9000  │ IT         │
│ 1  │ Joe   │ 8500  │ IT         │
│ 6  │ Randy │ 8500  │ IT         │
│ 2  │ Henry │ 8000  │ Sales      │
│ 3  │ Sam   │ 6000  │ Sales      │
└────┴───────┴───────┴────────────┘

5 rows in set. Elapsed: 0.003 sec.
```

##### 3.2 查询图书馆浏览次数大于99人，而且id连续的行数不止3行的数据

```sql
SELECT DISTINCT t1.*
FROM library AS t1, library AS t2, library AS t3
WHERE 
(t1.people >= 100) 
AND 
(t2.people >= 100) 
AND 
(t3.people >= 100)
AND 
(
    (((t1.id - t2.id) = 1) AND ((t1.id - t3.id) = 2) AND ((t2.id - t3.id) = 1)) 
OR 
	(((t2.id - t1.id) = 1) AND ((t2.id - t3.id) = 2) AND ((t1.id - t3.id) = 1)) 
OR 
	(((t3.id - t2.id) = 1) AND ((t2.id - t1.id) = 1) AND ((t3.id - t1.id) = 2))
)
```

打印输出如下

```bash
SELECT DISTINCT t1.*
FROM library AS t1, library AS t2, library AS t3
WHERE (t1.people >= 100) AND (t2.people >= 100) AND (t3.people >= 100) AND ((((t1.id - t2.id) = 1) AND ((t1.id - t3.id) = 2) AND ((t2.id - t3.id) = 1)) OR (((t2.id - t1.id) = 1) AND ((t2.id - t3.id) = 2) AND ((t1.id - t3.id) = 1)) OR (((t3.id - t2.id) = 1) AND ((t2.id - t1.id) = 1) AND ((t3.id - t1.id) = 2)))
ORDER BY t1.id ASC

Query id: ff8c02eb-4702-4e4b-a845-7b536dc346f1

┌─t1.id─┬─t1.visit_date─┬─t1.people─┐
│     5 │    2017-01-05 │       145 │
│     6 │    2017-01-06 │      1455 │
│     7 │    2017-01-07 │       199 │
│     8 │    2017-01-08 │       188 │
└───────┴───────────────┴───────────┘

4 rows in set. Elapsed: 0.025 sec.
```

### 实验四

[(125条消息) 【ClickHouse内核】对于分区、索引、标记和压缩数据的协同工作_Night_ZW的博客-CSDN博客](https://blog.csdn.net/Night_ZW/article/details/112845684?spm=1001.2014.3001.5501)

[(125条消息) 【ClickHouse内核】MergeTree数据物理存储结构_Night_ZW的博客-CSDN博客](https://blog.csdn.net/Night_ZW/article/details/112845281?spm=1001.2014.3001.5501)

- checksum.txt 检验和

- columns.txt 存放列信息

> columns format version: 1
> 4 columns:
> `id` String
> `name` String
> `bonus` String
> `department` String

- count.txt 存放行数量信息
- *.bin 存放压缩后的数据

[(125条消息) 【ClickHouse内核】MergeTree物理存储之bin文件解析_Night_ZW的博客-CSDN博客](https://blog.csdn.net/Night_ZW/article/details/112845552?spm=1001.2014.3001.5501)

- *.mrk

[(125条消息) 【ClickHouse内核】MergeTree物理存储之mrk文件解析_Night_ZW的博客-CSDN博客_mrk文件](https://blog.csdn.net/Night_ZW/article/details/112845620?spm=1001.2014.3001.5501)

- primary.idx 索引信息

插入数据前

只有all_1_1_0文件夹

插入数据后

新增了一个all_2_2_0文件夹

存储的表信息在/var/lib/clickhouse/data
通过软链接映射到/var/lib/clickhouse/store中，可以看到具体的文件存储形式
