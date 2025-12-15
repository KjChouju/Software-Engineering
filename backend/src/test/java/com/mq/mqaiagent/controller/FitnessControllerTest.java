package com.mq.mqaiagent.controller;

import com.mq.mqaiagent.exception.GlobalExceptionHandler;
import com.mq.mqaiagent.model.dto.exerciseLog.ExerciseLogAddRequest;
import com.mq.mqaiagent.model.dto.fitnessData.FitnessDataAddRequest;
import com.mq.mqaiagent.model.dto.trainingPlan.TrainingPlanAddRequest;
import com.mq.mqaiagent.model.entity.User;
import com.mq.mqaiagent.model.entity.ExerciseLog;
import com.mq.mqaiagent.model.entity.FitnessData;
import com.mq.mqaiagent.model.entity.FitnessGoal;
import com.mq.mqaiagent.model.entity.TrainingPlan;
import com.mq.mqaiagent.model.vo.ExerciseLogVO;
import com.mq.mqaiagent.model.vo.FitnessDataVO;
import com.mq.mqaiagent.model.vo.FitnessGoalVO;
import com.mq.mqaiagent.model.vo.TrainingPlanVO;
import com.mq.mqaiagent.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FitnessController 关键分支与边界覆盖测试
 */
class FitnessControllerTest {

    private MockMvc mockMvc;
    private FitnessController fitnessController;

    private FitnessDataService fitnessDataService;
    private ExerciseLogService exerciseLogService;
    private TrainingPlanService trainingPlanService;
    private FitnessGoalService fitnessGoalService;
    private UserService userService;
    private RankingService rankingService;

    @BeforeEach
    void setUp() {
        fitnessController = new FitnessController();
        fitnessDataService = Mockito.mock(FitnessDataService.class);
        exerciseLogService = Mockito.mock(ExerciseLogService.class);
        trainingPlanService = Mockito.mock(TrainingPlanService.class);
        fitnessGoalService = Mockito.mock(FitnessGoalService.class);
        userService = Mockito.mock(UserService.class);
        rankingService = Mockito.mock(RankingService.class);

        setPrivateField(fitnessController, "fitnessDataService", fitnessDataService);
        setPrivateField(fitnessController, "exerciseLogService", exerciseLogService);
        setPrivateField(fitnessController, "trainingPlanService", trainingPlanService);
        setPrivateField(fitnessController, "fitnessGoalService", fitnessGoalService);
        setPrivateField(fitnessController, "userService", userService);
        setPrivateField(fitnessController, "rankingService", rankingService);

        mockMvc = MockMvcBuilders.standaloneSetup(fitnessController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testAddFitnessData_paramsNull() throws Exception {
        try {
            fitnessController.addFitnessData(null, new MockHttpServletRequest());
            org.junit.jupiter.api.Assertions.fail("expected BusinessException");
        } catch (com.mq.mqaiagent.exception.BusinessException e) {
            org.junit.jupiter.api.Assertions.assertEquals(com.mq.mqaiagent.common.ErrorCode.PARAMS_ERROR.getCode(), e.getCode());
        }
    }

    @Test
    void testAddFitnessData_success() throws Exception {
        User user = new User();
        user.setId(10L);
        when(userService.getLoginUser(any())).thenReturn(user);
        when(fitnessDataService.calculateBMI(any(), any())).thenReturn(22.5f);
        when(fitnessDataService.save(any())).thenReturn(true);

        String body = "{\"height\":170,\"weight\":65}";
        mockMvc.perform(post("/fitness/data/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));
    }

    @Test
    void testDeleteFitnessData_success() throws Exception {
        User user = new User();
        user.setId(100L);
        when(userService.getLoginUser(any())).thenReturn(user);
        FitnessData fd = new FitnessData();
        fd.setUserId(100L);
        when(fitnessDataService.getById(eq(123L))).thenReturn(fd);
        when(fitnessDataService.removeById(eq(123L))).thenReturn(true);

        String body = "{\"id\":123}";
        mockMvc.perform(post("/fitness/data/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));
    }

    @Test
    void testDeleteFitnessData_noAuth() throws Exception {
        User user = new User();
        user.setId(100L);
        when(userService.getLoginUser(any())).thenReturn(user);
        FitnessData fd = new FitnessData();
        fd.setUserId(999L);
        when(fitnessDataService.getById(eq(124L))).thenReturn(fd);
        when(userService.isAdmin(any())).thenReturn(false);
        String body = "{\"id\":124}";
        mockMvc.perform(post("/fitness/data/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("无权限")));
    }

    @Test
    void testDeleteFitnessData_adminAllowed() throws Exception {
        User user = new User();
        user.setId(100L);
        when(userService.getLoginUser(any())).thenReturn(user);
        FitnessData fd = new FitnessData();
        fd.setUserId(999L);
        when(fitnessDataService.getById(eq(125L))).thenReturn(fd);
        when(userService.isAdmin(any())).thenReturn(true);
        when(fitnessDataService.removeById(eq(125L))).thenReturn(true);
        String body = "{\"id\":125}";
        mockMvc.perform(post("/fitness/data/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));
    }

    @Test
    void testUpdateFitnessData_success() throws Exception {
        FitnessData old = new FitnessData();
        old.setId(200L);
        when(fitnessDataService.getById(eq(200L))).thenReturn(old);
        when(fitnessDataService.calculateBMI(any(), any())).thenReturn(23.0f);
        when(fitnessDataService.updateById(any())).thenReturn(true);

        String body = "{\"id\":200,\"height\":175,\"weight\":70}";
        mockMvc.perform(post("/fitness/data/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));
    }

    @Test
    void testDeleteFitnessData_paramInvalidId() throws Exception {
        String body = "{\"id\":0}";
        mockMvc.perform(post("/fitness/data/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":")));
    }

    @Test
    void testDeleteFitnessData_notFound() throws Exception {
        User user = new User();
        user.setId(100L);
        when(userService.getLoginUser(any())).thenReturn(user);
        when(fitnessDataService.getById(eq(999L))).thenReturn(null);

        String body = "{\"id\":999}";
        mockMvc.perform(post("/fitness/data/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":")));
    }

    @Test
    void testUpdateFitnessData_paramInvalid() throws Exception {
        String body = "{\"id\":0,\"height\":175,\"weight\":70}";
        mockMvc.perform(post("/fitness/data/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":")));
    }

    @Test
    void testGetFitnessDataById_success() throws Exception {
        FitnessData fd = new FitnessData();
        fd.setId(300L);
        when(fitnessDataService.getById(eq(300L))).thenReturn(fd);
        when(fitnessDataService.getFitnessDataVO(eq(fd))).thenReturn(new FitnessDataVO());

        mockMvc.perform(get("/fitness/data/get")
                        .param("id", "300"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));
    }

    @Test
    void testGetFitnessDataById_paramInvalid() throws Exception {
        mockMvc.perform(get("/fitness/data/get")
                        .param("id", "0"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":")));
    }

    @Test
    void testGetFitnessDataById_notFound() throws Exception {
        when(fitnessDataService.getById(eq(888L))).thenReturn(null);
        mockMvc.perform(get("/fitness/data/get")
                        .param("id", "888"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":")));
    }

    @Test
    void testListFitnessDataByPage_success() throws Exception {
        when(fitnessDataService.page(any(), any())).thenReturn(org.mockito.Mockito.mock(com.baomidou.mybatisplus.extension.plugins.pagination.Page.class));
        when(fitnessDataService.getFitnessDataVOPage(any())).thenReturn(org.mockito.Mockito.mock(com.baomidou.mybatisplus.extension.plugins.pagination.Page.class));

        String body = "{\"current\":1,\"pageSize\":10}";
        mockMvc.perform(post("/fitness/data/list/page")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));
    }

    @Test
    void testListMyFitnessDataByPage_paramsNull() {
        try {
            fitnessController.listMyFitnessDataByPage(null, new MockHttpServletRequest());
            org.junit.jupiter.api.Assertions.fail("expected BusinessException");
        } catch (com.mq.mqaiagent.exception.BusinessException e) {
            org.junit.jupiter.api.Assertions.assertEquals(com.mq.mqaiagent.common.ErrorCode.PARAMS_ERROR.getCode(), e.getCode());
        }
    }

    @Test
    void testListMyFitnessDataByPage_success() throws Exception {
        User user = new User();
        user.setId(11L);
        when(userService.getLoginUser(any())).thenReturn(user);
        when(fitnessDataService.getFitnessDataVOPage(any())).thenReturn(org.mockito.Mockito.mock(com.baomidou.mybatisplus.extension.plugins.pagination.Page.class));
        when(fitnessDataService.page(any(), any())).thenReturn(org.mockito.Mockito.mock(com.baomidou.mybatisplus.extension.plugins.pagination.Page.class));

        String body = "{\"current\":1,\"pageSize\":10}";
        mockMvc.perform(post("/fitness/data/my/list/page")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));
    }

    @Test
    void testGetFitnessTrends_success() throws Exception {
        User user = new User();
        user.setId(12L);
        when(userService.getLoginUser(any())).thenReturn(user);
        when(fitnessDataService.getFitnessTrends(eq(12L), anyInt())).thenReturn(List.of(new FitnessDataVO()));

        mockMvc.perform(get("/fitness/data/trends")
                        .param("days", "30"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));
    }

    @Test
    void testAddExerciseLog_paramsNull() throws Exception {
        try {
            fitnessController.addExerciseLog(null, new MockHttpServletRequest());
            org.junit.jupiter.api.Assertions.fail("expected BusinessException");
        } catch (com.mq.mqaiagent.exception.BusinessException e) {
            org.junit.jupiter.api.Assertions.assertEquals(com.mq.mqaiagent.common.ErrorCode.PARAMS_ERROR.getCode(), e.getCode());
        }
    }

    @Test
    void testAddExerciseLog_success() throws Exception {
        User user = new User();
        user.setId(20L);
        when(userService.getLoginUser(any())).thenReturn(user);
        when(exerciseLogService.save(any())).thenReturn(true);

        String body = "{\"duration\":30,\"caloriesBurned\":200}";
        mockMvc.perform(post("/fitness/exercise/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));
    }

    @Test
    void testAddExerciseLog_withDate() throws Exception {
        User user = new User();
        user.setId(20L);
        when(userService.getLoginUser(any())).thenReturn(user);
        when(exerciseLogService.save(any())).thenReturn(true);
        String body = "{\"duration\":30,\"caloriesBurned\":200,\"dateRecorded\":\"2025-12-01T00:00:00.000+00:00\"}";
        mockMvc.perform(post("/fitness/exercise/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));
    }

    @Test
    void testAddExerciseLog_operationFail() throws Exception {
        User user = new User();
        user.setId(20L);
        when(userService.getLoginUser(any())).thenReturn(user);
        when(exerciseLogService.save(any())).thenReturn(false);

        String body = "{\"duration\":30,\"caloriesBurned\":200}";
        mockMvc.perform(post("/fitness/exercise/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":")));
    }

    @Test
    void testDeleteExerciseLog_success() throws Exception {
        User user = new User();
        user.setId(120L);
        when(userService.getLoginUser(any())).thenReturn(user);
        ExerciseLog log = new ExerciseLog();
        log.setUserId(120L);
        when(exerciseLogService.getById(eq(321L))).thenReturn(log);
        when(exerciseLogService.removeById(eq(321L))).thenReturn(true);

        String body = "{\"id\":321}";
        mockMvc.perform(post("/fitness/exercise/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));
    }

    @Test
    void testDeleteExerciseLog_noAuth() throws Exception {
        User user = new User();
        user.setId(120L);
        when(userService.getLoginUser(any())).thenReturn(user);
        ExerciseLog log = new ExerciseLog();
        log.setUserId(999L);
        when(exerciseLogService.getById(eq(322L))).thenReturn(log);
        when(userService.isAdmin(any())).thenReturn(false);
        String body = "{\"id\":322}";
        mockMvc.perform(post("/fitness/exercise/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("无权限")));
    }

    @Test
    void testDeleteExerciseLog_adminAllowed() throws Exception {
        User user = new User();
        user.setId(120L);
        when(userService.getLoginUser(any())).thenReturn(user);
        ExerciseLog log = new ExerciseLog();
        log.setUserId(999L);
        when(exerciseLogService.getById(eq(323L))).thenReturn(log);
        when(userService.isAdmin(any())).thenReturn(true);
        when(exerciseLogService.removeById(eq(323L))).thenReturn(true);
        String body = "{\"id\":323}";
        mockMvc.perform(post("/fitness/exercise/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));
    }

    @Test
    void testDeleteExerciseLog_paramInvalid() throws Exception {
        String body = "{\"id\":0}";
        mockMvc.perform(post("/fitness/exercise/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":")));
    }

    @Test
    void testDeleteExerciseLog_notFound() throws Exception {
        User user = new User();
        user.setId(120L);
        when(userService.getLoginUser(any())).thenReturn(user);
        when(exerciseLogService.getById(eq(9999L))).thenReturn(null);

        String body = "{\"id\":9999}";
        mockMvc.perform(post("/fitness/exercise/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":")));
    }

    @Test
    void testGetExerciseStats_success() throws Exception {
        User user = new User();
        user.setId(21L);
        when(userService.getLoginUser(any())).thenReturn(user);
        when(exerciseLogService.getExerciseStats(eq(21L), anyInt())).thenReturn(List.of(new ExerciseLogVO()));

        mockMvc.perform(get("/fitness/exercise/stats")
                        .param("days", "7"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));
    }

    @Test
    void testListMyExerciseLogByPage_paramsNull() {
        try {
            fitnessController.listMyExerciseLogByPage(null, new MockHttpServletRequest());
            org.junit.jupiter.api.Assertions.fail("expected BusinessException");
        } catch (com.mq.mqaiagent.exception.BusinessException e) {
            org.junit.jupiter.api.Assertions.assertEquals(com.mq.mqaiagent.common.ErrorCode.PARAMS_ERROR.getCode(), e.getCode());
        }
    }

    @Test
    void testListMyExerciseLogByPage_success() throws Exception {
        User user = new User();
        user.setId(22L);
        when(userService.getLoginUser(any())).thenReturn(user);
        when(exerciseLogService.page(any(), any())).thenReturn(org.mockito.Mockito.mock(com.baomidou.mybatisplus.extension.plugins.pagination.Page.class));
        when(exerciseLogService.getExerciseLogVOPage(any())).thenReturn(org.mockito.Mockito.mock(com.baomidou.mybatisplus.extension.plugins.pagination.Page.class));

        String body = "{\"current\":1,\"pageSize\":10}";
        mockMvc.perform(post("/fitness/exercise/my/list/page")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));
    }

    @Test
    void testGetTotalCaloriesBurned_success() throws Exception {
        User user = new User();
        user.setId(23L);
        when(userService.getLoginUser(any())).thenReturn(user);
        when(exerciseLogService.getTotalCaloriesBurned(eq(23L), anyInt())).thenReturn(1234.5f);

        mockMvc.perform(get("/fitness/exercise/calories")
                        .param("days", "30"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));
    }

    @Test
    void testAddTrainingPlan_paramsNull() throws Exception {
        try {
            fitnessController.addTrainingPlan(null, new MockHttpServletRequest());
            org.junit.jupiter.api.Assertions.fail("expected BusinessException");
        } catch (com.mq.mqaiagent.exception.BusinessException e) {
            org.junit.jupiter.api.Assertions.assertEquals(com.mq.mqaiagent.common.ErrorCode.PARAMS_ERROR.getCode(), e.getCode());
        }
    }

    @Test
    void testAddTrainingPlan_success() throws Exception {
        User user = new User();
        user.setId(30L);
        when(userService.getLoginUser(any())).thenReturn(user);
        when(trainingPlanService.save(any())).thenReturn(true);

        String body = "{\"title\":\"计划A\",\"description\":\"描述\"}";
        mockMvc.perform(post("/fitness/plan/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));
    }

    @Test
    void testAddFitnessData_operationFail() throws Exception {
        User user = new User();
        user.setId(10L);
        when(userService.getLoginUser(any())).thenReturn(user);
        when(fitnessDataService.calculateBMI(any(), any())).thenReturn(22.5f);
        when(fitnessDataService.save(any())).thenReturn(false);
        String body = "{\"height\":170,\"weight\":65}";
        mockMvc.perform(post("/fitness/data/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("操作失败")));
    }

    @Test
    void testAddFitnessData_withDate() throws Exception {
        User user = new User();
        user.setId(10L);
        when(userService.getLoginUser(any())).thenReturn(user);
        when(fitnessDataService.calculateBMI(any(), any())).thenReturn(22.5f);
        when(fitnessDataService.save(any())).thenReturn(true);
        String body = "{\"height\":170,\"weight\":65,\"dateRecorded\":\"2025-12-01T00:00:00.000+00:00\"}";
        mockMvc.perform(post("/fitness/data/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));
    }

    @Test
    void testAddTrainingPlan_operationFail() throws Exception {
        User user = new User();
        user.setId(30L);
        when(userService.getLoginUser(any())).thenReturn(user);
        when(trainingPlanService.save(any())).thenReturn(false);

        String body = "{\"title\":\"计划A\",\"description\":\"描述\"}";
        mockMvc.perform(post("/fitness/plan/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":")));
    }

    @Test
    void testDeleteTrainingPlan_success() throws Exception {
        User user = new User();
        user.setId(130L);
        when(userService.getLoginUser(any())).thenReturn(user);
        TrainingPlan tp = new TrainingPlan();
        tp.setUserId(130L);
        when(trainingPlanService.getById(eq(444L))).thenReturn(tp);
        when(trainingPlanService.removeById(eq(444L))).thenReturn(true);

        String body = "{\"id\":444}";
        mockMvc.perform(post("/fitness/plan/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));
    }

    @Test
    void testDeleteTrainingPlan_noAuth() throws Exception {
        User user = new User();
        user.setId(130L);
        when(userService.getLoginUser(any())).thenReturn(user);
        TrainingPlan tp = new TrainingPlan();
        tp.setUserId(999L);
        when(trainingPlanService.getById(eq(445L))).thenReturn(tp);
        when(userService.isAdmin(any())).thenReturn(false);
        String body = "{\"id\":445}";
        mockMvc.perform(post("/fitness/plan/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("无权限")));
    }

    @Test
    void testDeleteTrainingPlan_adminAllowed() throws Exception {
        User user = new User();
        user.setId(130L);
        when(userService.getLoginUser(any())).thenReturn(user);
        TrainingPlan tp = new TrainingPlan();
        tp.setUserId(999L);
        when(trainingPlanService.getById(eq(446L))).thenReturn(tp);
        when(userService.isAdmin(any())).thenReturn(true);
        when(trainingPlanService.removeById(eq(446L))).thenReturn(true);
        String body = "{\"id\":446}";
        mockMvc.perform(post("/fitness/plan/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));
    }

    @Test
    void testDeleteTrainingPlan_paramInvalid() throws Exception {
        String body = "{\"id\":0}";
        mockMvc.perform(post("/fitness/plan/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":")));
    }

    @Test
    void testDeleteTrainingPlan_notFound() throws Exception {
        User user = new User();
        user.setId(130L);
        when(userService.getLoginUser(any())).thenReturn(user);
        when(trainingPlanService.getById(eq(7777L))).thenReturn(null);

        String body = "{\"id\":7777}";
        mockMvc.perform(post("/fitness/plan/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":")));
    }

    @Test
    void testListMyTrainingPlanByPage_paramsNull() {
        try {
            fitnessController.listMyTrainingPlanByPage(null, new MockHttpServletRequest());
            org.junit.jupiter.api.Assertions.fail("expected BusinessException");
        } catch (com.mq.mqaiagent.exception.BusinessException e) {
            org.junit.jupiter.api.Assertions.assertEquals(com.mq.mqaiagent.common.ErrorCode.PARAMS_ERROR.getCode(), e.getCode());
        }
    }

    @Test
    void testListMyTrainingPlanByPage_success() throws Exception {
        User user = new User();
        user.setId(31L);
        when(userService.getLoginUser(any())).thenReturn(user);
        when(trainingPlanService.page(any(), any())).thenReturn(org.mockito.Mockito.mock(com.baomidou.mybatisplus.extension.plugins.pagination.Page.class));
        when(trainingPlanService.getTrainingPlanVOPage(any())).thenReturn(org.mockito.Mockito.mock(com.baomidou.mybatisplus.extension.plugins.pagination.Page.class));

        String body = "{\"current\":1,\"pageSize\":10}";
        mockMvc.perform(post("/fitness/plan/my/list/page")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));
    }

    @Test
    void testGetDefaultTrainingPlans_success() throws Exception {
        when(trainingPlanService.getDefaultTrainingPlans()).thenReturn(List.of(new TrainingPlanVO()));
        mockMvc.perform(get("/fitness/plan/default"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));
    }

    @Test
    void testAddFitnessGoal_success() throws Exception {
        User user = new User();
        user.setId(40L);
        when(userService.getLoginUser(any())).thenReturn(user);
        when(fitnessGoalService.save(any())).thenReturn(true);

        String body = "{\"title\":\"目标A\",\"description\":\"描述\"}";
        mockMvc.perform(post("/fitness/goal/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));
    }

    @Test
    void testAddFitnessGoal_operationFail() throws Exception {
        User user = new User();
        user.setId(40L);
        when(userService.getLoginUser(any())).thenReturn(user);
        when(fitnessGoalService.save(any())).thenReturn(false);

        String body = "{\"title\":\"目标A\",\"description\":\"描述\"}";
        mockMvc.perform(post("/fitness/goal/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":")));
    }

    @Test
    void testDeleteFitnessGoal_success() throws Exception {
        User user = new User();
        user.setId(41L);
        when(userService.getLoginUser(any())).thenReturn(user);
        FitnessGoal goal = new FitnessGoal();
        goal.setUserId(41L);
        when(fitnessGoalService.getById(eq(555L))).thenReturn(goal);
        when(fitnessGoalService.removeById(eq(555L))).thenReturn(true);

        String body = "{\"id\":555}";
        mockMvc.perform(post("/fitness/goal/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));
    }

    @Test
    void testUpdateFitnessGoal_success() throws Exception {
        User user = new User();
        user.setId(42L);
        when(userService.getLoginUser(any())).thenReturn(user);
        FitnessGoal old = new FitnessGoal();
        old.setUserId(42L);
        when(fitnessGoalService.getById(eq(666L))).thenReturn(old);
        when(fitnessGoalService.updateById(any())).thenReturn(true);

        String body = "{\"id\":666,\"title\":\"更新\",\"description\":\"x\"}";
        mockMvc.perform(post("/fitness/goal/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));
    }

    @Test
    void testUpdateFitnessGoal_noAuth() throws Exception {
        User user = new User();
        user.setId(42L);
        when(userService.getLoginUser(any())).thenReturn(user);
        FitnessGoal old = new FitnessGoal();
        old.setUserId(999L);
        when(fitnessGoalService.getById(eq(667L))).thenReturn(old);
        when(userService.isAdmin(any())).thenReturn(false);
        String body = "{\"id\":667,\"title\":\"更新\"}";
        mockMvc.perform(post("/fitness/goal/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("无权限")));
    }

    @Test
    void testUpdateFitnessGoal_adminAllowed() throws Exception {
        User user = new User();
        user.setId(42L);
        when(userService.getLoginUser(any())).thenReturn(user);
        FitnessGoal old = new FitnessGoal();
        old.setUserId(999L);
        when(fitnessGoalService.getById(eq(668L))).thenReturn(old);
        when(userService.isAdmin(any())).thenReturn(true);
        when(fitnessGoalService.updateById(any())).thenReturn(true);
        String body = "{\"id\":668,\"title\":\"更新\"}";
        mockMvc.perform(post("/fitness/goal/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));
    }

    @Test
    void testUpdateFitnessGoal_paramInvalid() throws Exception {
        String body = "{\"id\":0,\"title\":\"x\"}";
        mockMvc.perform(post("/fitness/goal/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":")));
    }

    @Test
    void testUpdateFitnessGoal_notFound() throws Exception {
        User user = new User();
        user.setId(42L);
        when(userService.getLoginUser(any())).thenReturn(user);
        when(fitnessGoalService.getById(eq(999L))).thenReturn(null);
        String body = "{\"id\":999,\"title\":\"更新\"}";
        mockMvc.perform(post("/fitness/goal/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":")));
    }

    @Test
    void testListMyFitnessGoalByPage_paramsNull() {
        try {
            fitnessController.listMyFitnessGoalByPage(null, new MockHttpServletRequest());
            org.junit.jupiter.api.Assertions.fail("expected BusinessException");
        } catch (com.mq.mqaiagent.exception.BusinessException e) {
            org.junit.jupiter.api.Assertions.assertEquals(com.mq.mqaiagent.common.ErrorCode.PARAMS_ERROR.getCode(), e.getCode());
        }
    }

    @Test
    void testListMyFitnessGoalByPage_success() throws Exception {
        User user = new User();
        user.setId(43L);
        when(userService.getLoginUser(any())).thenReturn(user);
        when(fitnessGoalService.page(any(), any())).thenReturn(org.mockito.Mockito.mock(com.baomidou.mybatisplus.extension.plugins.pagination.Page.class));
        when(fitnessGoalService.getFitnessGoalVOPage(any())).thenReturn(org.mockito.Mockito.mock(com.baomidou.mybatisplus.extension.plugins.pagination.Page.class));

        String body = "{\"current\":1,\"pageSize\":10}";
        mockMvc.perform(post("/fitness/goal/my/list/page")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));
    }

    @Test
    void testGetActiveFitnessGoals_success() throws Exception {
        User user = new User();
        user.setId(44L);
        when(userService.getLoginUser(any())).thenReturn(user);
        when(fitnessGoalService.getActiveFitnessGoals(eq(44L))).thenReturn(List.of(new FitnessGoalVO()));

        mockMvc.perform(get("/fitness/goal/active"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));
    }

    @Test
    void testCalculateBMI_success() throws Exception {
        String body = "{\"weight\":70,\"height\":175}";
        mockMvc.perform(post("/fitness/bmi/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));
    }

    @Test
    void testCalculateBMI_paramNulls() throws Exception {
        String body1 = "{\"weight\":null,\"height\":175}";
        mockMvc.perform(post("/fitness/bmi/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body1))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":")));

        String body2 = "{\"weight\":70,\"height\":null}";
        mockMvc.perform(post("/fitness/bmi/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body2))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":")));
    }

    @Test
    void testCalculateBMI_paramNonPositive() throws Exception {
        String body1 = "{\"weight\":0,\"height\":175}";
        mockMvc.perform(post("/fitness/bmi/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body1))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":")));

        String body2 = "{\"weight\":70,\"height\":0}";
        mockMvc.perform(post("/fitness/bmi/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body2))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":")));
    }

    @Test
    void testCalculateBMI_generateHealthAdviceCategories() throws Exception {
        String underweight = "{\"weight\":50,\"height\":175}";
        mockMvc.perform(post("/fitness/bmi/calculate").contentType(MediaType.APPLICATION_JSON).content(underweight))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));

        String normal = "{\"weight\":65,\"height\":175}";
        mockMvc.perform(post("/fitness/bmi/calculate").contentType(MediaType.APPLICATION_JSON).content(normal))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));

        String overweight = "{\"weight\":80,\"height\":175}";
        mockMvc.perform(post("/fitness/bmi/calculate").contentType(MediaType.APPLICATION_JSON).content(overweight))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));

        String obese = "{\"weight\":95,\"height\":175}";
        mockMvc.perform(post("/fitness/bmi/calculate").contentType(MediaType.APPLICATION_JSON).content(obese))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));
    }

    @Test
    void testGetRankingList_success() throws Exception {
        when(rankingService.getRankingList(eq("week"), any(), any())).thenReturn(org.mockito.Mockito.mock(com.mq.mqaiagent.model.dto.ranking.RankingListResponse.class));
        String body = "{\"rankingType\":\"week\",\"current\":1,\"pageSize\":10}";
        mockMvc.perform(post("/fitness/ranking/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));
    }

    @Test
    void testGetRankingList_invalidType() throws Exception {
        String body = "{\"rankingType\":\"year\",\"current\":1,\"pageSize\":10}";
        mockMvc.perform(post("/fitness/ranking/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":")));
    }

    @Test
    void testGetRankingList_missingType() throws Exception {
        String body = "{\"current\":1,\"pageSize\":10}";
        mockMvc.perform(post("/fitness/ranking/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":")));
    }

    @Test
    void testGetMyRanking_success() throws Exception {
        User user = new User();
        user.setId(50L);
        when(userService.getLoginUser(any())).thenReturn(user);
        when(rankingService.getMyRanking(eq(50L), eq("week"))).thenReturn(org.mockito.Mockito.mock(com.mq.mqaiagent.model.dto.ranking.MyRankingResponse.class));
        mockMvc.perform(get("/fitness/ranking/my")
                        .param("rankingType", "week"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));
    }

    @Test
    void testGetMyRanking_invalidType() throws Exception {
        mockMvc.perform(get("/fitness/ranking/my")
                        .param("rankingType", "year"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":")));
    }

    @Test
    void testRefreshRanking_success() throws Exception {
        when(rankingService.refreshRanking(eq("week"))).thenReturn(true);
        mockMvc.perform(post("/fitness/ranking/refresh")
                        .param("rankingType", "week"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));
    }

    @Test
    void testRefreshRanking_invalidType() throws Exception {
        mockMvc.perform(post("/fitness/ranking/refresh")
                        .param("rankingType", "year"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":")));
    }

    private void setPrivateField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set private field: " + fieldName, e);
        }
    }
}
