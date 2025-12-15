package com.mq.mqaiagent.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mq.mqaiagent.common.ErrorCode;
import com.mq.mqaiagent.exception.BusinessException;
import com.mq.mqaiagent.model.dto.trainingPlan.TrainingPlanQueryRequest;
import com.mq.mqaiagent.model.entity.TrainingPlan;
import com.mq.mqaiagent.model.vo.TrainingPlanVO;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

public class TrainingPlanServiceImplTest {

    @Test
    void testValidTrainingPlan_addParamsBlank_throwsBusinessException() {
        TrainingPlanServiceImpl service = new TrainingPlanServiceImpl();
        TrainingPlan plan = new TrainingPlan();
        plan.setPlanName(null);
        plan.setPlanDetails(null);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.validTrainingPlan(plan, true));
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), ex.getCode());
    }

    @Test
    void testValidTrainingPlan_lengthChecks_throwsRuntimeException() {
        TrainingPlanServiceImpl service = new TrainingPlanServiceImpl();
        TrainingPlan plan = new TrainingPlan();
        plan.setPlanName("a".repeat(300));
        plan.setPlanDetails("详情");
        RuntimeException ex1 = assertThrows(RuntimeException.class, () -> service.validTrainingPlan(plan, false));
        assertTrue(ex1.getMessage().contains("计划名称过长"));

        plan.setPlanName("力量");
        plan.setPlanDetails("a".repeat(9000));
        RuntimeException ex2 = assertThrows(RuntimeException.class, () -> service.validTrainingPlan(plan, false));
        assertTrue(ex2.getMessage().contains("计划详情过长"));

        plan.setPlanDetails("详情");
        plan.setPlanType("a".repeat(300));
        RuntimeException ex3 = assertThrows(RuntimeException.class, () -> service.validTrainingPlan(plan, false));
        assertTrue(ex3.getMessage().contains("计划类型过长"));
    }

    @Test
    void testGetQueryWrapper_basic_ok() {
        TrainingPlanServiceImpl service = new TrainingPlanServiceImpl();
        TrainingPlanQueryRequest req = new TrainingPlanQueryRequest();
        req.setUserId(1L);
        req.setPlanName("力量");
        req.setPlanType("strength");
        req.setIsDefault(1);
        req.setSortField("createTime");
        req.setSortOrder("descend");
        assertNotNull(service.getQueryWrapper(req));
    }

    @Test
    void testGetTrainingPlanVO_copiesProps() {
        TrainingPlanServiceImpl service = new TrainingPlanServiceImpl();
        TrainingPlan entity = new TrainingPlan();
        entity.setId(10L);
        entity.setUserId(2L);
        entity.setPlanName("力量提升");
        entity.setPlanDetails("细节");
        TrainingPlanVO vo = service.getTrainingPlanVO(entity);
        assertNotNull(vo);
        assertEquals(entity.getId(), vo.getId());
        assertEquals(entity.getUserId(), vo.getUserId());
        assertEquals(entity.getPlanName(), vo.getPlanName());
        assertEquals(entity.getPlanDetails(), vo.getPlanDetails());
    }

    @Test
    void testGetTrainingPlanVOPage_mapsRecords() {
        TrainingPlanServiceImpl service = new TrainingPlanServiceImpl();
        Page<TrainingPlan> page = new Page<>(1, 10, 2);
        List<TrainingPlan> list = new ArrayList<>();
        TrainingPlan e1 = new TrainingPlan();
        e1.setId(1L);
        e1.setUserId(1L);
        e1.setPlanName("A");
        e1.setPlanDetails("a");
        TrainingPlan e2 = new TrainingPlan();
        e2.setId(2L);
        e2.setUserId(1L);
        e2.setPlanName("B");
        e2.setPlanDetails("b");
        list.add(e1);
        list.add(e2);
        page.setRecords(list);

        Page<TrainingPlanVO> voPage = service.getTrainingPlanVOPage(page);
        assertEquals(2, voPage.getRecords().size());
        assertEquals(2, voPage.getTotal());
    }

    @Test
    void testGetUserTrainingPlans_spyList() {
        TrainingPlanServiceImpl service = Mockito.spy(new TrainingPlanServiceImpl());
        List<TrainingPlan> fake = new ArrayList<>();
        TrainingPlan e = new TrainingPlan();
        e.setId(1L);
        e.setUserId(1L);
        e.setPlanName("A");
        e.setPlanDetails("a");
        fake.add(e);
        Mockito.doReturn(fake).when(service).list(Mockito.<com.baomidou.mybatisplus.core.conditions.Wrapper<TrainingPlan>>any());
        List<TrainingPlanVO> voList = service.getUserTrainingPlans(1L);
        assertEquals(1, voList.size());
        assertEquals("A", voList.get(0).getPlanName());
    }

    @Test
    void testGetDefaultTrainingPlans_spyList() {
        TrainingPlanServiceImpl service = Mockito.spy(new TrainingPlanServiceImpl());
        List<TrainingPlan> fake = new ArrayList<>();
        TrainingPlan e = new TrainingPlan();
        e.setId(1L);
        e.setUserId(0L);
        e.setPlanName("默认");
        e.setPlanDetails("默认详情");
        fake.add(e);
        Mockito.doReturn(fake).when(service).list(Mockito.<com.baomidou.mybatisplus.core.conditions.Wrapper<TrainingPlan>>any());
        List<TrainingPlanVO> voList = service.getDefaultTrainingPlans();
        assertEquals(1, voList.size());
        assertEquals("默认", voList.get(0).getPlanName());
    }

    @Test
    void testGetQueryWrapper_nullRequest_ok() {
        TrainingPlanServiceImpl service = new TrainingPlanServiceImpl();
        assertNotNull(service.getQueryWrapper(null));
    }

    @Test
    void testGetTrainingPlanVO_null_returnsNull() {
        TrainingPlanServiceImpl service = new TrainingPlanServiceImpl();
        assertNull(service.getTrainingPlanVO(null));
    }

    @Test
    void testGetTrainingPlanVOPage_empty_returnsEmptyPage() {
        TrainingPlanServiceImpl service = new TrainingPlanServiceImpl();
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<TrainingPlan> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10, 0);
        page.setRecords(new ArrayList<>());
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<TrainingPlanVO> voPage =
                service.getTrainingPlanVOPage(page);
        assertTrue(voPage.getRecords().isEmpty());
        assertEquals(0, voPage.getTotal());
    }

    @Test
    void testGetUserTrainingPlans_nullUser_returnsEmpty() {
        TrainingPlanServiceImpl service = new TrainingPlanServiceImpl();
        assertTrue(service.getUserTrainingPlans(null).isEmpty());
    }
}
