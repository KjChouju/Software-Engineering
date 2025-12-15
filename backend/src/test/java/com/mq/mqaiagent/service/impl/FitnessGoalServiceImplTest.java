package com.mq.mqaiagent.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mq.mqaiagent.common.ErrorCode;
import com.mq.mqaiagent.exception.BusinessException;
import com.mq.mqaiagent.model.dto.fitnessGoal.FitnessGoalQueryRequest;
import com.mq.mqaiagent.model.entity.FitnessGoal;
import com.mq.mqaiagent.model.vo.FitnessGoalVO;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

public class FitnessGoalServiceImplTest {

    @Test
    void testValidFitnessGoal_addParamsBlank_throwsBusinessException() {
        FitnessGoalServiceImpl service = new FitnessGoalServiceImpl();
        FitnessGoal goal = new FitnessGoal();
        goal.setGoalType(null);
        goal.setTargetValue(null);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.validFitnessGoal(goal, true));
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), ex.getCode());
    }

    @Test
    void testValidFitnessGoal_lengthChecks_throwsRuntimeException() {
        FitnessGoalServiceImpl service = new FitnessGoalServiceImpl();
        FitnessGoal goal = new FitnessGoal();
        goal.setGoalType("a".repeat(300));
        goal.setTargetValue("10kg");
        RuntimeException ex1 = assertThrows(RuntimeException.class, () -> service.validFitnessGoal(goal, false));
        assertTrue(ex1.getMessage().contains("目标类型过长"));

        goal.setGoalType("减脂");
        goal.setTargetValue("a".repeat(300));
        RuntimeException ex2 = assertThrows(RuntimeException.class, () -> service.validFitnessGoal(goal, false));
        assertTrue(ex2.getMessage().contains("目标值过长"));

        goal.setTargetValue("10kg");
        goal.setProgress("a".repeat(9000));
        RuntimeException ex3 = assertThrows(RuntimeException.class, () -> service.validFitnessGoal(goal, false));
        assertTrue(ex3.getMessage().contains("进度记录过长"));
    }

    @Test
    void testGetQueryWrapper_basic_ok() {
        FitnessGoalServiceImpl service = new FitnessGoalServiceImpl();
        FitnessGoalQueryRequest req = new FitnessGoalQueryRequest();
        req.setUserId(1L);
        req.setGoalType("减脂");
        req.setIsAchieved(0);
        req.setSortField("createTime");
        req.setSortOrder("descend");
        assertNotNull(service.getQueryWrapper(req));
    }

    @Test
    void testGetFitnessGoalVO_copiesProps() {
        FitnessGoalServiceImpl service = new FitnessGoalServiceImpl();
        FitnessGoal entity = new FitnessGoal();
        entity.setId(10L);
        entity.setUserId(2L);
        entity.setGoalType("减脂");
        entity.setTargetValue("10kg");
        FitnessGoalVO vo = service.getFitnessGoalVO(entity);
        assertNotNull(vo);
        assertEquals(entity.getId(), vo.getId());
        assertEquals(entity.getUserId(), vo.getUserId());
        assertEquals(entity.getGoalType(), vo.getGoalType());
        assertEquals(entity.getTargetValue(), vo.getTargetValue());
    }

    @Test
    void testGetFitnessGoalVOPage_mapsRecords() {
        FitnessGoalServiceImpl service = new FitnessGoalServiceImpl();
        Page<FitnessGoal> page = new Page<>(1, 10, 2);
        List<FitnessGoal> list = new ArrayList<>();
        FitnessGoal e1 = new FitnessGoal();
        e1.setId(1L);
        e1.setUserId(1L);
        e1.setGoalType("增肌");
        e1.setTargetValue("5kg");
        FitnessGoal e2 = new FitnessGoal();
        e2.setId(2L);
        e2.setUserId(1L);
        e2.setGoalType("减脂");
        e2.setTargetValue("8kg");
        list.add(e1);
        list.add(e2);
        page.setRecords(list);

        Page<FitnessGoalVO> voPage = service.getFitnessGoalVOPage(page);
        assertEquals(2, voPage.getRecords().size());
        assertEquals(2, voPage.getTotal());
    }

    @Test
    void testGetUserFitnessGoals_spyList() {
        FitnessGoalServiceImpl service = Mockito.spy(new FitnessGoalServiceImpl());
        List<FitnessGoal> fake = new ArrayList<>();
        FitnessGoal e = new FitnessGoal();
        e.setId(1L);
        e.setUserId(1L);
        e.setGoalType("增肌");
        e.setTargetValue("5kg");
        fake.add(e);
        Mockito.doReturn(fake).when(service).list(Mockito.<com.baomidou.mybatisplus.core.conditions.Wrapper<FitnessGoal>>any());
        List<FitnessGoalVO> voList = service.getUserFitnessGoals(1L);
        assertEquals(1, voList.size());
        assertEquals("增肌", voList.get(0).getGoalType());
    }

    @Test
    void testGetActiveFitnessGoals_spyList() {
        FitnessGoalServiceImpl service = Mockito.spy(new FitnessGoalServiceImpl());
        List<FitnessGoal> fake = new ArrayList<>();
        FitnessGoal e = new FitnessGoal();
        e.setId(1L);
        e.setUserId(1L);
        e.setGoalType("减脂");
        e.setTargetValue("8kg");
        e.setIsAchieved(0);
        e.setEndDate(new Date(System.currentTimeMillis() + 86400000L));
        fake.add(e);
        Mockito.doReturn(fake).when(service).list(Mockito.<com.baomidou.mybatisplus.core.conditions.Wrapper<FitnessGoal>>any());
        List<FitnessGoalVO> voList = service.getActiveFitnessGoals(1L);
        assertEquals(1, voList.size());
        assertEquals("减脂", voList.get(0).getGoalType());
    }

    @Test
    void testGetQueryWrapper_nullRequest_ok() {
        FitnessGoalServiceImpl service = new FitnessGoalServiceImpl();
        assertNotNull(service.getQueryWrapper(null));
    }

    @Test
    void testGetFitnessGoalVO_null_returnsNull() {
        FitnessGoalServiceImpl service = new FitnessGoalServiceImpl();
        assertNull(service.getFitnessGoalVO(null));
    }

    @Test
    void testGetFitnessGoalVOPage_empty_returnsEmptyPage() {
        FitnessGoalServiceImpl service = new FitnessGoalServiceImpl();
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<FitnessGoal> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10, 0);
        page.setRecords(new ArrayList<>());
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<FitnessGoalVO> voPage =
                service.getFitnessGoalVOPage(page);
        assertTrue(voPage.getRecords().isEmpty());
        assertEquals(0, voPage.getTotal());
    }

    @Test
    void testGetUserFitnessGoals_nullUser_returnsEmpty() {
        FitnessGoalServiceImpl service = new FitnessGoalServiceImpl();
        assertTrue(service.getUserFitnessGoals(null).isEmpty());
    }

    @Test
    void testGetActiveFitnessGoals_nullUser_returnsEmpty() {
        FitnessGoalServiceImpl service = new FitnessGoalServiceImpl();
        assertTrue(service.getActiveFitnessGoals(null).isEmpty());
    }
}
