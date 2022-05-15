package com.example.clickhousedemo;

import com.example.clickhousedemo.util.ClickHouseUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

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
        ClickHouseUtil.createTableSql(sql);
    }

}
