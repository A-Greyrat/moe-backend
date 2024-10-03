package com.abdecd.moebackend.business.dao.dataencrypt;

import com.abdecd.moebackend.business.common.util.SpringContextUtil;
import com.baomidou.mybatisplus.core.toolkit.AES;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@MappedJdbcTypes(JdbcType.VARCHAR)
@MappedTypes(String.class)
@SuppressWarnings("all")
public class EncryptStrHandler extends BaseTypeHandler<String> {
    private static String KEY;

    static {
        // 记得提前引用，不然static块不会触发
        // 读取 KEY
        Thread.ofVirtual().start(() -> {
            while (SpringContextUtil.getApplicationContext() == null) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                }
            }
            KEY = SpringContextUtil
                .getApplicationContext()
                .getEnvironment()
                .getProperty("moe.encrypt-str-aes-key");
            if (KEY == null || KEY.isEmpty())
                throw new RuntimeException("moe.encrypt-str-aes-key is empty");
        });
    }

    public static String encrypt(String value) {
        if (null == value) {
            return null;
        }
        while (KEY.isEmpty()) Thread.onSpinWait();
        return AES.encrypt(value, KEY);
    }

    public static String decrypt(String value) {
        if (null == value) {
            return null;
        }
        while (KEY.isEmpty()) Thread.onSpinWait();
        return AES.decrypt(value, KEY);
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        if (parameter == null) {
            ps.setString(i, null);
            return;
        }
        ps.setString(i, encrypt(parameter));
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return decrypt(rs.getString(columnName));
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return decrypt(rs.getString(columnIndex));
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return decrypt(cs.getString(columnIndex));
    }
}