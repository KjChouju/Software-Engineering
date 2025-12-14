package com.mq.mqaiagent.controller;

import com.mq.mqaiagent.exception.GlobalExceptionHandler;
import com.mq.mqaiagent.model.entity.User;
import com.mq.mqaiagent.model.vo.LoginUserVO;
import com.mq.mqaiagent.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * UserController 行为与分支覆盖测试
 */
class UserControllerTest {

    private MockMvc mockMvc;
    private UserController userController;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userController = new UserController();
        userService = Mockito.mock(UserService.class);
        setPrivateField(userController, "userService", userService);
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testUserLogin_paramsBlank() throws Exception {
        String body = "{\"userAccount\":\"\",\"userPassword\":\"\"}";
        mockMvc.perform(post("/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("请求参数错误")));
    }

    @Test
    void testUserLogin_success() throws Exception {
        LoginUserVO vo = new LoginUserVO();
        vo.setUserName("tom");
        when(userService.userLogin(eq("user"), eq("password"), any())).thenReturn(vo);

        String body = "{\"userAccount\":\"user\",\"userPassword\":\"password\"}";
        mockMvc.perform(post("/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("tom")));
    }

    @Test
    void testUserLogin_shortAccount_error() throws Exception {
        when(userService.userLogin(eq("abc"), eq("password123"), any()))
                .thenThrow(new com.mq.mqaiagent.exception.BusinessException(com.mq.mqaiagent.common.ErrorCode.PARAMS_ERROR, "账号错误"));
        String body = "{\"userAccount\":\"abc\",\"userPassword\":\"password123\"}";
        mockMvc.perform(post("/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("账号错误")));
    }

    @Test
    void testUserLogin_shortPassword_error() throws Exception {
        when(userService.userLogin(eq("validuser"), eq("short"), any()))
                .thenThrow(new com.mq.mqaiagent.exception.BusinessException(com.mq.mqaiagent.common.ErrorCode.PARAMS_ERROR, "密码错误"));
        String body = "{\"userAccount\":\"validuser\",\"userPassword\":\"short\"}";
        mockMvc.perform(post("/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("密码错误")));
    }

    @Test
    void testUserLogin_mismatch_error() throws Exception {
        when(userService.userLogin(eq("validuser"), eq("password123"), any()))
                .thenThrow(new com.mq.mqaiagent.exception.BusinessException(com.mq.mqaiagent.common.ErrorCode.PARAMS_ERROR, "用户不存在或密码错误"));
        String body = "{\"userAccount\":\"validuser\",\"userPassword\":\"password123\"}";
        mockMvc.perform(post("/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("用户不存在或密码错误")));
    }

    @Test
    void testUserLogin_nullRequestBody_error() {
        jakarta.servlet.http.HttpServletRequest http = new MockHttpServletRequest();
        try {
            userController.userLogin(null, http);
            org.junit.jupiter.api.Assertions.fail("expected BusinessException");
        } catch (com.mq.mqaiagent.exception.BusinessException e) {
            org.junit.jupiter.api.Assertions.assertEquals(com.mq.mqaiagent.common.ErrorCode.PARAMS_ERROR.getCode(), e.getCode());
        }
    }

    @Test
    void testUserLogout_success() throws Exception {
        when(userService.userLogout(any())).thenReturn(true);
        mockMvc.perform(post("/user/logout"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));
    }

    @Test
    void testUserLogout_requestNull_error() {
        try {
            userController.userLogout(null);
            org.junit.jupiter.api.Assertions.fail("expected BusinessException");
        } catch (com.mq.mqaiagent.exception.BusinessException e) {
            org.junit.jupiter.api.Assertions.assertEquals(com.mq.mqaiagent.common.ErrorCode.PARAMS_ERROR.getCode(), e.getCode());
        }
    }

    @Test
    void testDeleteUser_paramsError_nullBody() throws Exception {
        try {
            userController.deleteUser(null, new MockHttpServletRequest());
            org.junit.jupiter.api.Assertions.fail("expected BusinessException");
        } catch (com.mq.mqaiagent.exception.BusinessException e) {
            org.junit.jupiter.api.Assertions.assertEquals(com.mq.mqaiagent.common.ErrorCode.PARAMS_ERROR.getCode(), e.getCode());
        }
    }

    @Test
    void testDeleteUser_paramsError_idInvalid() throws Exception {
        String body = "{\"id\":0}";
        mockMvc.perform(post("/user/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("请求参数错误")));
    }

    @Test
    void testDeleteUser_success() throws Exception {
        when(userService.removeById(eq(9L))).thenReturn(true);
        String body = "{\"id\":9}";
        mockMvc.perform(post("/user/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));
    }

    @Test
    void testUpdateUser_paramsError_nullBody() throws Exception {
        try {
            userController.updateUser(null, new MockHttpServletRequest());
            org.junit.jupiter.api.Assertions.fail("expected BusinessException");
        } catch (com.mq.mqaiagent.exception.BusinessException e) {
            org.junit.jupiter.api.Assertions.assertEquals(com.mq.mqaiagent.common.ErrorCode.PARAMS_ERROR.getCode(), e.getCode());
        }
    }

    @Test
    void testUpdateUser_paramsError_idNull() throws Exception {
        String body = "{\"userName\":\"a\"}";
        mockMvc.perform(post("/user/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("请求参数错误")));
    }

    @Test
    void testUpdateUser_operationError() throws Exception {
        when(userService.updateById(any())).thenReturn(false);
        String body = "{\"id\":2,\"userName\":\"b\"}";
        mockMvc.perform(post("/user/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("操作失败")));
    }

    @Test
    void testUpdateUser_success() throws Exception {
        when(userService.updateById(any())).thenReturn(true);
        String body = "{\"id\":3,\"userName\":\"c\"}";
        mockMvc.perform(post("/user/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));
    }

    @Test
    void testGetUserById_idInvalid() throws Exception {
        mockMvc.perform(get("/user/get")
                        .param("id", "0"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("请求参数错误")));
    }

    @Test
    void testGetUserById_notFound() throws Exception {
        when(userService.getById(eq(100L))).thenReturn(null);
        mockMvc.perform(get("/user/get")
                        .param("id", "100"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("请求数据不存在")));
    }

    @Test
    void testGetUserById_success() throws Exception {
        User u = new User();
        u.setId(101L);
        when(userService.getById(eq(101L))).thenReturn(u);
        mockMvc.perform(get("/user/get")
                        .param("id", "101"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));
    }

    @Test
    void testUpdateMyUser_paramsNull() throws Exception {
        try {
            userController.updateMyUser(null, new MockHttpServletRequest());
            org.junit.jupiter.api.Assertions.fail("expected BusinessException");
        } catch (com.mq.mqaiagent.exception.BusinessException e) {
            org.junit.jupiter.api.Assertions.assertEquals(com.mq.mqaiagent.common.ErrorCode.PARAMS_ERROR.getCode(), e.getCode());
        }
    }

    @Test
    void testUpdateMyUser_success() throws Exception {
        User login = new User();
        login.setId(8L);
        when(userService.getLoginUser(any())).thenReturn(login);
        when(userService.updateById(any())).thenReturn(true);
        String body = "{\"userName\":\"z\"}";
        mockMvc.perform(post("/user/update/my")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));
    }

    @Test
    void testUserRegister_paramsNull_error() {
        try {
            userController.userRegister(null);
            org.junit.jupiter.api.Assertions.fail("expected BusinessException");
        } catch (com.mq.mqaiagent.exception.BusinessException e) {
            org.junit.jupiter.api.Assertions.assertEquals(com.mq.mqaiagent.common.ErrorCode.PARAMS_ERROR.getCode(), e.getCode());
        }
    }

    @Test
    void testUserRegister_blankFields_returnsEmpty() throws Exception {
        String body = "{\"userAccount\":\"\",\"userPassword\":\"\",\"checkPassword\":\"\"}";
        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void testUserRegister_success() throws Exception {
        when(userService.userRegister(eq("user1"), eq("password123"), eq("password123"))).thenReturn(66L);
        String body = "{\"userAccount\":\"user1\",\"userPassword\":\"password123\",\"checkPassword\":\"password123\"}";
        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));
    }

    @Test
    void testAddUser_paramsNull_error() throws Exception {
        try {
            userController.addUser(null, new MockHttpServletRequest());
            org.junit.jupiter.api.Assertions.fail("expected BusinessException");
        } catch (com.mq.mqaiagent.exception.BusinessException e) {
            org.junit.jupiter.api.Assertions.assertEquals(com.mq.mqaiagent.common.ErrorCode.PARAMS_ERROR.getCode(), e.getCode());
        }
    }

    @Test
    void testAddUser_success() throws Exception {
        when(userService.getEncryptPassword(anyString())).thenReturn("enc");
        when(userService.save(any())).thenReturn(true);
        String body = "{\"userName\":\"u\"}";
        mockMvc.perform(post("/user/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));
    }

    @Test
    void testGetUserVOById_idInvalid_error() throws Exception {
        mockMvc.perform(get("/user/get/vo")
                        .param("id", "0"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("请求参数错误")));
    }

    @Test
    void testGetUserVOById_success() throws Exception {
        User u = new User();
        u.setId(5L);
        com.mq.mqaiagent.model.vo.UserVO vo = new com.mq.mqaiagent.model.vo.UserVO();
        when(userService.getById(eq(5L))).thenReturn(u);
        when(userService.getUserVO(eq(u))).thenReturn(vo);
        mockMvc.perform(get("/user/get/vo")
                        .param("id", "5"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));
    }

    @Test
    void testListUserByPage_success() throws Exception {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.mq.mqaiagent.model.entity.User> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10, 0);
        when(userService.page(any(), any())).thenReturn(page);
        String body = "{\"current\":1,\"pageSize\":10}";
        mockMvc.perform(post("/user/list/page")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));
    }

    @Test
    void testListUserVOByPage_paramsNull_error() throws Exception {
        try {
            userController.listUserVOByPage(null, new MockHttpServletRequest());
            org.junit.jupiter.api.Assertions.fail("expected BusinessException");
        } catch (com.mq.mqaiagent.exception.BusinessException e) {
            org.junit.jupiter.api.Assertions.assertEquals(com.mq.mqaiagent.common.ErrorCode.PARAMS_ERROR.getCode(), e.getCode());
        }
    }

    @Test
    void testListUserVOByPage_sizeTooLarge_error() throws Exception {
        String body = "{\"current\":1,\"pageSize\":21}";
        mockMvc.perform(post("/user/list/page/vo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("请求参数错误")));
    }

    @Test
    void testUploadAvatar_fileNull_error() {
        try {
            userController.uploadAvatar(null, new MockHttpServletRequest());
            org.junit.jupiter.api.Assertions.fail("expected BusinessException");
        } catch (com.mq.mqaiagent.exception.BusinessException e) {
            org.junit.jupiter.api.Assertions.assertEquals(com.mq.mqaiagent.common.ErrorCode.PARAMS_ERROR.getCode(), e.getCode());
        }
    }

    @Test
    void testUploadAvatar_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", "x".getBytes());
        when(userService.uploadAvatar(any(), any())).thenReturn("http://a");
        mockMvc.perform(multipart("/user/avatar/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));
    }

    @Test
    void testUpdateAvatar_blank_error() throws Exception {
        mockMvc.perform(post("/user/avatar/update").param("avatarUrl", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("头像URL不能为空")));
    }

    @Test
    void testUpdateAvatar_operationError() throws Exception {
        when(userService.updateUserAvatar(eq("http://x"), any())).thenReturn(false);
        mockMvc.perform(post("/user/avatar/update").param("avatarUrl", "http://x"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("更新头像失败")));
    }

    @Test
    void testUpdateAvatar_success() throws Exception {
        when(userService.updateUserAvatar(eq("http://x"), any())).thenReturn(true);
        mockMvc.perform(post("/user/avatar/update").param("avatarUrl", "http://x"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")));
    }
    @Test
    void testGetLoginUser_success() throws Exception {
        User u = new User();
        u.setId(1L);
        when(userService.getLoginUser(any())).thenReturn(u);
        LoginUserVO vo = new LoginUserVO();
        vo.setUserName("jack");
        when(userService.getLoginUserVO(eq(u))).thenReturn(vo);
        mockMvc.perform(get("/user/get/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":0")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("jack")));
    }

    @Test
    void testGetLoginUser_notLoggedIn() throws Exception {
        when(userService.getLoginUser(any()))
                .thenThrow(new com.mq.mqaiagent.exception.BusinessException(com.mq.mqaiagent.common.ErrorCode.NOT_LOGIN_ERROR));
        mockMvc.perform(get("/user/get/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"code\":")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("未登录")));
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
