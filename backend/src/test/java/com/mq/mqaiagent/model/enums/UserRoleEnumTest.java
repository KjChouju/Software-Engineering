package com.mq.mqaiagent.model.enums;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class UserRoleEnumTest {

    @Test
    void getValues_returnsAllInOrder() {
        List<String> values = UserRoleEnum.getValues();
        assertEquals(List.of("user", "admin", "vip", "ban"), values);
    }

    @Test
    void getEnumByValue_valid_returnsEnum() {
        assertEquals(UserRoleEnum.USER, UserRoleEnum.getEnumByValue("user"));
        assertEquals(UserRoleEnum.ADMIN, UserRoleEnum.getEnumByValue("admin"));
        assertEquals(UserRoleEnum.VIP, UserRoleEnum.getEnumByValue("vip"));
        assertEquals(UserRoleEnum.BAN, UserRoleEnum.getEnumByValue("ban"));
    }

    @Test
    void getEnumByValue_empty_returnsNull() {
        assertNull(UserRoleEnum.getEnumByValue(null));
        assertNull(UserRoleEnum.getEnumByValue(""));
        assertNull(UserRoleEnum.getEnumByValue("   "));
    }

    @Test
    void getEnumByValue_notFound_returnsNull() {
        assertNull(UserRoleEnum.getEnumByValue("moderator"));
    }

    @Test
    void getters_returnValueAndText() {
        assertEquals("user", UserRoleEnum.USER.getValue());
        assertEquals("用户", UserRoleEnum.USER.getText());
        assertEquals("admin", UserRoleEnum.ADMIN.getValue());
        assertEquals("管理员", UserRoleEnum.ADMIN.getText());
        assertEquals("vip", UserRoleEnum.VIP.getValue());
        assertEquals("会员", UserRoleEnum.VIP.getText());
        assertEquals("ban", UserRoleEnum.BAN.getValue());
        assertEquals("被封号", UserRoleEnum.BAN.getText());
    }
}

