package com.mq.mqaiagent.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mq.mqaiagent.common.ErrorCode;
import com.mq.mqaiagent.exception.BusinessException;
import com.mq.mqaiagent.model.dto.fitnessData.FitnessDataQueryRequest;
import com.mq.mqaiagent.model.entity.FitnessData;
import com.mq.mqaiagent.model.vo.FitnessDataVO;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

public class FitnessDataServiceImplTest {

    @Test
    void testValidFitnessData_addParamsNull_throwsBusinessException() {
        FitnessDataServiceImpl service = new FitnessDataServiceImpl();
        FitnessData data = new FitnessData();
        data.setWeight(null);
        data.setHeight(null);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.validFitnessData(data, true));
        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), ex.getCode());
    }

    @Test
    void testValidFitnessData_invalidRange_throwsRuntimeException() {
        FitnessDataServiceImpl service = new FitnessDataServiceImpl();
        FitnessData data = new FitnessData();
        data.setWeight(-1.0f);
        data.setHeight(170.0f);
        RuntimeException ex1 = assertThrows(RuntimeException.class, () -> service.validFitnessData(data, false));
        assertTrue(ex1.getMessage().contains("体重数据不合理"));

        data.setWeight(60.0f);
        data.setHeight(0.0f);
        RuntimeException ex2 = assertThrows(RuntimeException.class, () -> service.validFitnessData(data, false));
        assertTrue(ex2.getMessage().contains("身高数据不合理"));

        data.setWeight(60.0f);
        data.setHeight(170.0f);
        data.setBodyFat(120.0f);
        RuntimeException ex3 = assertThrows(RuntimeException.class, () -> service.validFitnessData(data, false));
        assertTrue(ex3.getMessage().contains("体脂率数据不合理"));
    }

    @Test
    void testGetQueryWrapper_basic_ok() {
        FitnessDataServiceImpl service = new FitnessDataServiceImpl();
        FitnessDataQueryRequest req = new FitnessDataQueryRequest();
        req.setUserId(1L);
        req.setStartDate(new Date(System.currentTimeMillis() - 86400000L));
        req.setEndDate(new Date());
        req.setSortField("dateRecorded");
        req.setSortOrder("ascend");
        assertNotNull(service.getQueryWrapper(req));
    }

    @Test
    void testGetFitnessDataVO_copiesProps() {
        FitnessDataServiceImpl service = new FitnessDataServiceImpl();
        FitnessData entity = new FitnessData();
        entity.setId(10L);
        entity.setUserId(2L);
        entity.setWeight(65.5f);
        entity.setHeight(172.0f);
        FitnessDataVO vo = service.getFitnessDataVO(entity);
        assertNotNull(vo);
        assertEquals(entity.getId(), vo.getId());
        assertEquals(entity.getUserId(), vo.getUserId());
        assertEquals(entity.getWeight(), vo.getWeight());
        assertEquals(entity.getHeight(), vo.getHeight());
    }

    @Test
    void testGetFitnessDataVOPage_mapsRecords() {
        FitnessDataServiceImpl service = new FitnessDataServiceImpl();
        Page<FitnessData> page = new Page<>(1, 10, 2);
        List<FitnessData> list = new ArrayList<>();
        FitnessData e1 = new FitnessData();
        e1.setId(1L);
        e1.setUserId(1L);
        e1.setWeight(60.0f);
        e1.setHeight(170.0f);
        FitnessData e2 = new FitnessData();
        e2.setId(2L);
        e2.setUserId(1L);
        e2.setWeight(61.0f);
        e2.setHeight(170.0f);
        list.add(e1);
        list.add(e2);
        page.setRecords(list);

        Page<FitnessDataVO> voPage = service.getFitnessDataVOPage(page);
        assertEquals(2, voPage.getRecords().size());
        assertEquals(2, voPage.getTotal());
    }

    @Test
    void testCalculateBMI() {
        FitnessDataServiceImpl service = new FitnessDataServiceImpl();
        Float bmi = service.calculateBMI(60.0f, 170.0f);
        assertNotNull(bmi);
        assertEquals(20.76f, Math.round(bmi * 100) / 100.0f);

        assertNull(service.calculateBMI(null, 170.0f));
        assertNull(service.calculateBMI(60.0f, null));
        assertNull(service.calculateBMI(-1.0f, 170.0f));
        assertNull(service.calculateBMI(60.0f, 0.0f));
    }

    @Test
    void testGetFitnessTrends_spyList() {
        FitnessDataServiceImpl service = Mockito.spy(new FitnessDataServiceImpl());
        List<FitnessData> fake = new ArrayList<>();
        FitnessData e = new FitnessData();
        e.setId(1L);
        e.setUserId(1L);
        e.setWeight(60.0f);
        e.setHeight(170.0f);
        e.setDateRecorded(new Date());
        fake.add(e);
        Mockito.doReturn(fake).when(service).list(Mockito.<com.baomidou.mybatisplus.core.conditions.Wrapper<FitnessData>>any());
        List<FitnessDataVO> voList = service.getFitnessTrends(1L, 7);
        assertEquals(1, voList.size());
        assertEquals(60.0f, voList.get(0).getWeight());
    }

    @Test
    void testGetQueryWrapper_nullRequest_ok() {
        FitnessDataServiceImpl service = new FitnessDataServiceImpl();
        assertNotNull(service.getQueryWrapper(null));
    }

    @Test
    void testGetFitnessDataVO_null_returnsNull() {
        FitnessDataServiceImpl service = new FitnessDataServiceImpl();
        assertNull(service.getFitnessDataVO(null));
    }

    @Test
    void testGetFitnessDataVOPage_empty_returnsEmptyPage() {
        FitnessDataServiceImpl service = new FitnessDataServiceImpl();
        Page<FitnessData> page = new Page<>(1, 10, 0);
        page.setRecords(new ArrayList<>());
        Page<FitnessDataVO> voPage = service.getFitnessDataVOPage(page);
        assertTrue(voPage.getRecords().isEmpty());
        assertEquals(0, voPage.getTotal());
    }

    @Test
    void testGetFitnessTrends_invalidArgs_returnsEmpty() {
        FitnessDataServiceImpl service = new FitnessDataServiceImpl();
        assertTrue(service.getFitnessTrends(null, 7).isEmpty());
        assertTrue(service.getFitnessTrends(1L, null).isEmpty());
        assertTrue(service.getFitnessTrends(1L, 0).isEmpty());
        assertTrue(service.getFitnessTrends(1L, -1).isEmpty());
    }
}
