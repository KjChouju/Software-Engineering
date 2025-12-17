package com.mq.mqaiagent.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SqlUtilsTest {

    @Test
    void testValidSortField_blank_returnsFalse() {
        assertFalse(SqlUtils.validSortField(""));
        assertFalse(SqlUtils.validSortField("   "));
        assertFalse(SqlUtils.validSortField(null));
    }

    @Test
    void testValidSortField_containsIllegal_returnsFalse() {
        assertFalse(SqlUtils.validSortField("id = 1"));
        assertFalse(SqlUtils.validSortField("name)"));
        assertFalse(SqlUtils.validSortField("(createTime"));
        assertFalse(SqlUtils.validSortField("user name"));
    }

    @Test
    void testValidSortField_valid_returnsTrue() {
        assertTrue(SqlUtils.validSortField("createTime"));
        assertTrue(SqlUtils.validSortField("userName"));
        assertTrue(SqlUtils.validSortField("id"));
    }
}

