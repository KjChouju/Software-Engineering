package com.mq.mqaiagent.model;

import com.mq.mqaiagent.model.dto.ranking.Milestone;
import com.mq.mqaiagent.model.dto.ranking.MyRankingResponse;
import com.mq.mqaiagent.model.dto.ranking.NextRankInfo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MyRankingResponseTest {

    @Test
    void builder_setsAllFields() {
        NextRankInfo next = new NextRankInfo();
        Milestone m1 = new Milestone();
        MyRankingResponse resp = MyRankingResponse.builder()
                .isOnBoard(true)
                .myRank(5)
                .myScore(42)
                .firstRecordTime("2025-08-01")
                .totalMinutes(123)
                .totalCalories(456.7f)
                .totalUsers(1000)
                .beatPercent(87.5)
                .rankChange(2)
                .rankChangeType("up")
                .nextRankInfo(next)
                .milestones(List.of(m1))
                .encourageMessage("keep going")
                .build();
        assertTrue(resp.getIsOnBoard());
        assertEquals(5, resp.getMyRank());
        assertEquals(42, resp.getMyScore());
        assertEquals("2025-08-01", resp.getFirstRecordTime());
        assertEquals(123, resp.getTotalMinutes());
        assertEquals(456.7f, resp.getTotalCalories());
        assertEquals(1000, resp.getTotalUsers());
        assertEquals(87.5, resp.getBeatPercent());
        assertEquals(2, resp.getRankChange());
        assertEquals("up", resp.getRankChangeType());
        assertSame(next, resp.getNextRankInfo());
        assertEquals(1, resp.getMilestones().size());
        assertEquals("keep going", resp.getEncourageMessage());
    }

    @Test
    void notOnBoard_factory_setsDefaults() {
        MyRankingResponse resp = MyRankingResponse.notOnBoard(777);
        assertFalse(resp.getIsOnBoard());
        assertEquals(0, resp.getMyRank());
        assertEquals(0, resp.getMyScore());
        assertEquals(777, resp.getTotalUsers());
        assertEquals(0.0, resp.getBeatPercent());
        assertEquals("开始运动，加入排行榜吧！💪", resp.getEncourageMessage());
    }
}

