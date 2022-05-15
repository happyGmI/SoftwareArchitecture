package com.example.clickhousedemo.util;

/**
 * @Class: ClickHouseUtil
 * @Author: Wei Junwei
 * @Time: 16:12 2022/5/14
 * @XiDianUniversity
 * @Description:
 */
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.clickhouse.ClickHouseConnection;
import ru.yandex.clickhouse.ClickHouseDataSource;
import ru.yandex.clickhouse.settings.ClickHouseProperties;

import java.sql.*;
import java.util.*;

/**
 * @Description:
 * @Date 2018/11/12
 */
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
    public static void createTableSql(String sql){
        Connection connection = getConn();
        try {
            Statement statement = connection.createStatement();
            ResultSet results = statement.executeQuery(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}