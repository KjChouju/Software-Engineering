package com.mq.mqaiagent.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mq.mqaiagent.common.ErrorCode;
import com.mq.mqaiagent.exception.BusinessException;
import com.mq.mqaiagent.model.dto.exerciseLog.ExerciseLogQueryRequest;
import com.mq.mqaiagent.model.entity.ExerciseLog;
import com.mq.mqaiagent.model.vo.ExerciseLogVO;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import com.mq.mqaiagent.mapper.ExerciseLogMapper;
import com.mq.mqaiagent.service.RankingService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

public class ExerciseLogServiceImplTest {

    @Test
    void testValidExerciseLog_addParamsNull_throwsBusinessException() {
        ExerciseLogServiceImpl service = new ExerciseLogServiceImpl();
        ExerciseLog log = new ExerciseLog();
        log.setExerciseType(null);
        log.setDuration(null);
        log.setCaloriesBurned(null);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.validExerciseLog(log, true));
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), ex.getCode());
    }

    @Test
    void testValidExerciseLog_invalidRange_throwsRuntimeException() {
        ExerciseLogServiceImpl service = new ExerciseLogServiceImpl();
        ExerciseLog log = new ExerciseLog();
        log.setExerciseType("跑步");
        log.setDuration(-1);
        log.setCaloriesBurned(100.0f);
        RuntimeException ex1 = assertThrows(RuntimeException.class, () -> service.validExerciseLog(log, false));
        assertTrue(ex1.getMessage().contains("运动时长不合理"));

        log.setDuration(30);
        log.setCaloriesBurned(20000.0f);
        RuntimeException ex2 = assertThrows(RuntimeException.class, () -> service.validExerciseLog(log, false));
        assertTrue(ex2.getMessage().contains("卡路里消耗数据不合理"));
    }

    @Test
    void testGetQueryWrapper_basic_ok() {
        ExerciseLogServiceImpl service = new ExerciseLogServiceImpl();
        ExerciseLogQueryRequest req = new ExerciseLogQueryRequest();
        req.setUserId(1L);
        req.setExerciseType("跑步");
        req.setStartDate(new Date(System.currentTimeMillis() - 86400000L));
        req.setEndDate(new Date());
        req.setSortField("dateRecorded");
        req.setSortOrder("descend");
        assertNotNull(service.getQueryWrapper(req));
    }

    @Test
    void testGetExerciseLogVO_copiesProps() {
        ExerciseLogServiceImpl service = new ExerciseLogServiceImpl();
        ExerciseLog entity = new ExerciseLog();
        entity.setId(10L);
        entity.setUserId(2L);
        entity.setExerciseType("跑步");
        entity.setDuration(50);
        entity.setCaloriesBurned(400.0f);
        ExerciseLogVO vo = service.getExerciseLogVO(entity);
        assertNotNull(vo);
        assertEquals(entity.getId(), vo.getId());
        assertEquals(entity.getUserId(), vo.getUserId());
        assertEquals(entity.getExerciseType(), vo.getExerciseType());
        assertEquals(entity.getDuration(), vo.getDuration());
        assertEquals(entity.getCaloriesBurned(), vo.getCaloriesBurned());
    }

    @Test
    void testGetExerciseLogVOPage_mapsRecords() {
        ExerciseLogServiceImpl service = new ExerciseLogServiceImpl();
        Page<ExerciseLog> page = new Page<>(1, 10, 2);
        List<ExerciseLog> list = new ArrayList<>();
        ExerciseLog e1 = new ExerciseLog();
        e1.setId(1L);
        e1.setUserId(1L);
        e1.setExerciseType("跑步");
        e1.setDuration(30);
        e1.setCaloriesBurned(250.0f);
        ExerciseLog e2 = new ExerciseLog();
        e2.setId(2L);
        e2.setUserId(1L);
        e2.setExerciseType("游泳");
        e2.setDuration(45);
        e2.setCaloriesBurned(350.0f);
        list.add(e1);
        list.add(e2);
        page.setRecords(list);

        Page<ExerciseLogVO> voPage = service.getExerciseLogVOPage(page);
        assertEquals(2, voPage.getRecords().size());
        assertEquals(2, voPage.getTotal());
    }

    @Test
    void testGetExerciseStats_spyList() {
        ExerciseLogServiceImpl service = Mockito.spy(new ExerciseLogServiceImpl());
        List<ExerciseLog> fake = new ArrayList<>();
        ExerciseLog e = new ExerciseLog();
        e.setId(1L);
        e.setUserId(1L);
        e.setExerciseType("跑步");
        e.setDuration(30);
        e.setCaloriesBurned(250.0f);
        e.setDateRecorded(new Date());
        fake.add(e);
        Mockito.doReturn(fake).when(service).list(Mockito.<com.baomidou.mybatisplus.core.conditions.Wrapper<ExerciseLog>>any());
        List<ExerciseLogVO> voList = service.getExerciseStats(1L, 7);
        assertEquals(1, voList.size());
        assertEquals("跑步", voList.get(0).getExerciseType());
    }

    @Test
    void testGetTotalCaloriesBurned_spyList() {
        ExerciseLogServiceImpl service = Mockito.spy(new ExerciseLogServiceImpl());
        List<ExerciseLog> fake = new ArrayList<>();
        ExerciseLog e1 = new ExerciseLog();
        e1.setCaloriesBurned(100.0f);
        ExerciseLog e2 = new ExerciseLog();
        e2.setCaloriesBurned(200.0f);
        fake.add(e1);
        fake.add(e2);
        Mockito.doReturn(fake).when(service).list(Mockito.<com.baomidou.mybatisplus.core.conditions.Wrapper<ExerciseLog>>any());
        Float total = service.getTotalCaloriesBurned(1L, 7);
        assertEquals(300.0f, total);
    }

    @Test
    void testGetQueryWrapper_nullRequest_ok() {
        ExerciseLogServiceImpl service = new ExerciseLogServiceImpl();
        assertNotNull(service.getQueryWrapper(null));
    }

    @Test
    void testGetExerciseStats_invalidArgs_returnsEmpty() {
        ExerciseLogServiceImpl service = new ExerciseLogServiceImpl();
        assertTrue(service.getExerciseStats(null, 7).isEmpty());
        assertTrue(service.getExerciseStats(1L, null).isEmpty());
        assertTrue(service.getExerciseStats(1L, 0).isEmpty());
        assertTrue(service.getExerciseStats(1L, -1).isEmpty());
    }

    @Test
    void testGetTotalCaloriesBurned_invalidArgs_returnsZero() {
        ExerciseLogServiceImpl service = new ExerciseLogServiceImpl();
        assertEquals(0.0f, service.getTotalCaloriesBurned(null, 7));
        assertEquals(0.0f, service.getTotalCaloriesBurned(1L, null));
        assertEquals(0.0f, service.getTotalCaloriesBurned(1L, 0));
        assertEquals(0.0f, service.getTotalCaloriesBurned(1L, -1));
    }

    @Test
    void testSave_setsPeriodDates_andAsyncRanking_noThrow() throws Exception {
        ExerciseLogServiceImpl service = Mockito.spy(new ExerciseLogServiceImpl());
        ExerciseLogMapper mapper = Mockito.mock(ExerciseLogMapper.class);
        RankingService rankingService = Mockito.mock(RankingService.class);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        ReflectionTestUtils.setField(service, "rankingService", rankingService);

        Mockito.when(mapper.insert(Mockito.any(ExerciseLog.class))).thenReturn(1);
        Mockito.doThrow(new RuntimeException("ranking fail")).when(rankingService)
                .updateRankingAfterAdd(Mockito.anyLong(), Mockito.any(Date.class), Mockito.any(java.time.LocalDate.class), Mockito.any(java.time.LocalDate.class));

        ExerciseLog entity = new ExerciseLog();
        entity.setUserId(1L);
        entity.setDateRecorded(new Date());

        boolean result = service.save(entity);
        assertTrue(result);
        assertNotNull(entity.getWeekStartDate());
        assertNotNull(entity.getMonthStartDate());
        TimeUnit.MILLISECONDS.sleep(50);
    }

    @Test
    void testSave_superSaveFail_returnsFalse() {
        ExerciseLogServiceImpl service = Mockito.spy(new ExerciseLogServiceImpl());
        ExerciseLogMapper mapper = Mockito.mock(ExerciseLogMapper.class);
        RankingService rankingService = Mockito.mock(RankingService.class);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        ReflectionTestUtils.setField(service, "rankingService", rankingService);
        Mockito.when(mapper.insert(Mockito.any(ExerciseLog.class))).thenReturn(0);

        ExerciseLog entity = new ExerciseLog();
        entity.setUserId(1L);
        entity.setDateRecorded(new Date());
        assertFalse(service.save(entity));
    }

    @Test
    void testRemoveById_noRecord_returnsFalse() {
        ExerciseLogServiceImpl service = Mockito.spy(new ExerciseLogServiceImpl());
        ExerciseLogMapper mapper = Mockito.mock(ExerciseLogMapper.class);
        RankingService rankingService = Mockito.mock(RankingService.class);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        ReflectionTestUtils.setField(service, "rankingService", rankingService);

        Mockito.when(mapper.selectById(Mockito.eq(1L))).thenReturn(null);
        assertFalse(service.removeById(1L));
    }

    @Test
    void testRemoveById_ok_asyncRanking_noThrow() throws Exception {
        ExerciseLogServiceImpl service = Mockito.spy(new ExerciseLogServiceImpl());
        ExerciseLogMapper mapper = Mockito.mock(ExerciseLogMapper.class);
        RankingService rankingService = Mockito.mock(RankingService.class);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        ReflectionTestUtils.setField(service, "rankingService", rankingService);

        ExerciseLog exerciseLog = new ExerciseLog();
        exerciseLog.setId(2L);
        exerciseLog.setUserId(1L);
        exerciseLog.setWeekStartDate(new java.sql.Date(System.currentTimeMillis()));
        exerciseLog.setMonthStartDate(new java.sql.Date(System.currentTimeMillis()));
        Mockito.when(mapper.selectById(Mockito.eq(2L))).thenReturn(exerciseLog);
        Mockito.when(mapper.deleteById(Mockito.eq(2L))).thenReturn(1);
        Mockito.doThrow(new RuntimeException("ranking delete fail")).when(rankingService)
                .updateRankingAfterDelete(Mockito.anyLong(), Mockito.any(java.sql.Date.class), Mockito.any(java.sql.Date.class));

        assertTrue(service.removeById(2L));
        TimeUnit.MILLISECONDS.sleep(50);
    }
}
