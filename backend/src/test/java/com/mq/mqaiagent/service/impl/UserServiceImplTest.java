package com.mq.mqaiagent.service.impl;

import com.mq.mqaiagent.exception.BusinessException;
import com.mq.mqaiagent.manager.CosManager;
import com.mq.mqaiagent.model.entity.User;
import com.mq.mqaiagent.model.vo.LoginUserVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.DigestUtils;
import org.mockito.Mockito;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

import static com.mq.mqaiagent.constant.UserConstant.USER_LOGIN_STATE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserServiceImplTest {

    @Test
    void testGetEncryptPassword_ok() {
        UserServiceImpl service = new UserServiceImpl();
        String input = "password123";
        String expected = DigestUtils.md5DigestAsHex(("lmqicu" + input).getBytes());
        assertEquals(expected, service.getEncryptPassword(input));
    }

    @Test
    void testUserLogout_ok() {
        UserServiceImpl service = new UserServiceImpl();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute(USER_LOGIN_STATE)).thenReturn(new User());
        boolean result = service.userLogout(request);
        assertTrue(result);
        verify(session, times(1)).removeAttribute(USER_LOGIN_STATE);
    }

    @Test
    void testUserLogout_notLogged_throws() {
        UserServiceImpl service = new UserServiceImpl();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute(USER_LOGIN_STATE)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.userLogout(request));
    }

    @Test
    void testUpdateUserAvatar_ok() {
        UserServiceImpl service = Mockito.spy(new UserServiceImpl());
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);
        User loginUser = new User();
        loginUser.setId(10L);
        doReturn(loginUser).when(service).getLoginUser(eq(request));
        doReturn(true).when(service).updateById(any(User.class));
        boolean result = service.updateUserAvatar("http://x/y.png", request);
        assertTrue(result);
        verify(session, times(1)).setAttribute(eq(USER_LOGIN_STATE), any(User.class));
    }

    @Test
    void testUploadAvatar_ok() throws Exception {
        UserServiceImpl service = Mockito.spy(new UserServiceImpl());
        ReflectionTestUtils.setField(service, "bucketUrl", "http://bucket");
        CosManager cosManager = mock(CosManager.class);
        ReflectionTestUtils.setField(service, "cosManager", cosManager);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);
        User loginUser = new User();
        loginUser.setId(99L);
        doReturn(loginUser).when(service).getLoginUser(eq(request));
        doReturn(true).when(service).updateUserAvatar(anyString(), eq(request));

        MultipartFile file = mock(MultipartFile.class);
        when(file.getSize()).thenReturn(1024L);
        when(file.getOriginalFilename()).thenReturn("avatar.png");
        doAnswer(invocation -> {
            File temp = invocation.getArgument(0);
            return null;
        }).when(file).transferTo(any(File.class));

        String url = service.uploadAvatar(file, request);
        assertNotNull(url);
        assertTrue(url.startsWith("http://bucket/ai_agent_avatars/99/"));
        verify(cosManager, atLeastOnce()).putObject(anyString(), any(File.class));
    }

    @Test
    void testUserLogin_paramErrors_throw() {
        UserServiceImpl service = new UserServiceImpl();
        HttpServletRequest request = mock(HttpServletRequest.class);
        assertThrows(BusinessException.class, () -> service.userLogin("", "12345678", request));
        assertThrows(BusinessException.class, () -> service.userLogin("u", "12345678", request));
        assertThrows(BusinessException.class, () -> service.userLogin("user1", "short", request));
    }

    @Test
    void testUserLogin_userNotFound_throw() {
        UserServiceImpl service = new UserServiceImpl();
        com.mq.mqaiagent.mapper.UserMapper userMapper = mock(com.mq.mqaiagent.mapper.UserMapper.class);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "baseMapper", userMapper);
        when(userMapper.selectOne(any(com.baomidou.mybatisplus.core.conditions.query.QueryWrapper.class))).thenReturn(null);
        HttpServletRequest request = mock(HttpServletRequest.class);
        assertThrows(BusinessException.class, () -> service.userLogin("user1", "password123", request));
    }

    @Test
    void testUserRegister_paramErrors_throw() {
        UserServiceImpl service = new UserServiceImpl();
        assertThrows(BusinessException.class, () -> service.userRegister("", "12345678", "12345678"));
        assertThrows(BusinessException.class, () -> service.userRegister("usr", "12345678", "12345678"));
        assertThrows(BusinessException.class, () -> service.userRegister("user1", "short", "short"));
        assertThrows(BusinessException.class, () -> service.userRegister("user1", "12345678", "87654321"));
    }

    @Test
    void testUserRegister_duplicate_throw() {
        UserServiceImpl service = new UserServiceImpl();
        com.mq.mqaiagent.mapper.UserMapper userMapper = mock(com.mq.mqaiagent.mapper.UserMapper.class);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "baseMapper", userMapper);
        when(userMapper.selectCount(any(com.baomidou.mybatisplus.core.conditions.query.QueryWrapper.class))).thenReturn(1L);
        assertThrows(BusinessException.class, () -> service.userRegister("user1", "12345678", "12345678"));
    }

    @Test
    void testUserRegister_saveFail_throw() {
        UserServiceImpl service = Mockito.spy(new UserServiceImpl());
        com.mq.mqaiagent.mapper.UserMapper userMapper = mock(com.mq.mqaiagent.mapper.UserMapper.class);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "baseMapper", userMapper);
        when(userMapper.selectCount(any(com.baomidou.mybatisplus.core.conditions.query.QueryWrapper.class))).thenReturn(0L);
        doReturn(false).when(service).save(any(com.mq.mqaiagent.model.entity.User.class));
        assertThrows(BusinessException.class, () -> service.userRegister("user1", "12345678", "12345678"));
    }

    @Test
    void testGetLoginUser_notLogged_throw() {
        UserServiceImpl service = new UserServiceImpl();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute(USER_LOGIN_STATE)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.getLoginUser(request));
    }

    @Test
    void testGetLoginUserPermitNull_null_returnsNull() {
        UserServiceImpl service = new UserServiceImpl();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute(USER_LOGIN_STATE)).thenReturn(null);
        assertNull(service.getLoginUserPermitNull(request));
    }

    @Test
    void testGetQueryWrapper_nullRequest_throwsBusinessException() {
        UserServiceImpl service = new UserServiceImpl();
        assertThrows(BusinessException.class, () -> service.getQueryWrapper(null));
    }

    @Test
    void testGetLoginUserVO_null_returnsNull() {
        UserServiceImpl service = new UserServiceImpl();
        assertNull(service.getLoginUserVO(null));
    }

    @Test
    void testGetUserVO_null_returnsNull() {
        UserServiceImpl service = new UserServiceImpl();
        assertNull(service.getUserVO((User) null));
    }

    @Test
    void testGetUserVO_listEmpty_returnsEmptyList() {
        UserServiceImpl service = new UserServiceImpl();
        assertTrue(service.getUserVO(java.util.Collections.emptyList()).isEmpty());
    }

    @Test
    void testUserLogin_success_setsSessionAndReturnsVO() {
        UserServiceImpl service = new UserServiceImpl();
        com.mq.mqaiagent.mapper.UserMapper userMapper = mock(com.mq.mqaiagent.mapper.UserMapper.class);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "baseMapper", userMapper);
        when(userMapper.selectOne(any(com.baomidou.mybatisplus.core.conditions.query.QueryWrapper.class))).thenReturn(new User(){{
            setId(5L);
            setUserAccount("user1");
        }});
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);
        LoginUserVO vo = service.userLogin("user1", "password123", request);
        assertNotNull(vo);
        verify(session, times(1)).setAttribute(eq(USER_LOGIN_STATE), any(User.class));
    }

    @Test
    void testGetLoginUser_dbNull_throws() {
        UserServiceImpl service = Mockito.spy(new UserServiceImpl());
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);
        User u = new User();
        u.setId(10L);
        when(session.getAttribute(USER_LOGIN_STATE)).thenReturn(u);
        Mockito.doReturn(null).when(service).getById(eq(10L));
        assertThrows(BusinessException.class, () -> service.getLoginUser(request));
    }

    @Test
    void testGetLoginUser_success_returnsUser() {
        UserServiceImpl service = Mockito.spy(new UserServiceImpl());
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);
        User u = new User();
        u.setId(10L);
        when(session.getAttribute(USER_LOGIN_STATE)).thenReturn(u);
        User dbUser = new User();
        dbUser.setId(10L);
        Mockito.doReturn(dbUser).when(service).getById(eq(10L));
        User ret = service.getLoginUser(request);
        assertNotNull(ret);
        assertEquals(10L, ret.getId());
    }

    @Test
    void testGetLoginUserPermitNull_logged_returnsUser() {
        UserServiceImpl service = Mockito.spy(new UserServiceImpl());
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);
        User u = new User();
        u.setId(7L);
        when(session.getAttribute(USER_LOGIN_STATE)).thenReturn(u);
        User dbUser = new User();
        dbUser.setId(7L);
        Mockito.doReturn(dbUser).when(service).getById(eq(7L));
        User ret = service.getLoginUserPermitNull(request);
        assertNotNull(ret);
        assertEquals(7L, ret.getId());
    }

    @Test
    void testGetLoginUserVO_ok() {
        UserServiceImpl service = new UserServiceImpl();
        User user = new User();
        user.setId(3L);
        LoginUserVO vo = service.getLoginUserVO(user);
        assertNotNull(vo);
    }

    @Test
    void testGetUserVO_ok() {
        UserServiceImpl service = new UserServiceImpl();
        User user = new User();
        user.setId(4L);
        assertNotNull(service.getUserVO(user));
    }

    @Test
    void testGetUserVO_list_ok() {
        UserServiceImpl service = new UserServiceImpl();
        java.util.List<User> list = new java.util.ArrayList<>();
        User u1 = new User();
        u1.setId(1L);
        list.add(u1);
        java.util.List<com.mq.mqaiagent.model.vo.UserVO> vos = service.getUserVO(list);
        assertEquals(1, vos.size());
    }

    @Test
    void testGetQueryWrapper_fields_ok() {
        UserServiceImpl service = new UserServiceImpl();
        com.mq.mqaiagent.model.dto.user.UserQueryRequest req = new com.mq.mqaiagent.model.dto.user.UserQueryRequest();
        req.setId(1L);
        req.setUserAccount("acc");
        req.setUserName("name");
        req.setUserProfile("profile");
        req.setUserRole("admin");
        req.setSortField("id");
        req.setSortOrder(com.mq.mqaiagent.constant.CommonConstant.SORT_ORDER_ASC);
        assertNotNull(service.getQueryWrapper(req));
    }

    @Test
    void testIsAdmin_true_false() {
        UserServiceImpl service = new UserServiceImpl();
        User admin = new User();
        admin.setUserRole(com.mq.mqaiagent.model.enums.UserRoleEnum.ADMIN.getValue());
        assertTrue(service.isAdmin(admin));
        User normal = new User();
        normal.setUserRole(com.mq.mqaiagent.model.enums.UserRoleEnum.USER.getValue());
        assertFalse(service.isAdmin(normal));
    }

    @Test
    void testUploadAvatar_fileTooLarge_throws() {
        UserServiceImpl service = Mockito.spy(new UserServiceImpl());
        ReflectionTestUtils.setField(service, "bucketUrl", "http://bucket");
        CosManager cosManager = mock(CosManager.class);
        ReflectionTestUtils.setField(service, "cosManager", cosManager);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);
        User loginUser = new User();
        loginUser.setId(99L);
        doReturn(loginUser).when(service).getLoginUser(eq(request));
        MultipartFile file = mock(MultipartFile.class);
        when(file.getSize()).thenReturn(3 * 1024 * 1024L);
        when(file.getOriginalFilename()).thenReturn("avatar.png");
        assertThrows(BusinessException.class, () -> service.uploadAvatar(file, request));
    }

    @Test
    void testUploadAvatar_invalidSuffix_throws() {
        UserServiceImpl service = Mockito.spy(new UserServiceImpl());
        ReflectionTestUtils.setField(service, "bucketUrl", "http://bucket");
        CosManager cosManager = mock(CosManager.class);
        ReflectionTestUtils.setField(service, "cosManager", cosManager);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);
        User loginUser = new User();
        loginUser.setId(99L);
        doReturn(loginUser).when(service).getLoginUser(eq(request));
        MultipartFile file = mock(MultipartFile.class);
        when(file.getSize()).thenReturn(1024L);
        when(file.getOriginalFilename()).thenReturn("avatar.exe");
        assertThrows(BusinessException.class, () -> service.uploadAvatar(file, request));
    }

    @Test
    void testUploadAvatar_cosPutThrows_throws() throws Exception {
        UserServiceImpl service = Mockito.spy(new UserServiceImpl());
        ReflectionTestUtils.setField(service, "bucketUrl", "http://bucket");
        CosManager cosManager = mock(CosManager.class);
        ReflectionTestUtils.setField(service, "cosManager", cosManager);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);
        User loginUser = new User();
        loginUser.setId(99L);
        doReturn(loginUser).when(service).getLoginUser(eq(request));
        doReturn(true).when(service).updateUserAvatar(anyString(), eq(request));
        MultipartFile file = mock(MultipartFile.class);
        when(file.getSize()).thenReturn(1024L);
        when(file.getOriginalFilename()).thenReturn("avatar.png");
        doAnswer(invocation -> {
            File temp = invocation.getArgument(0);
            return null;
        }).when(file).transferTo(any(File.class));
        doThrow(new RuntimeException("fail")).when(cosManager).putObject(anyString(), any(File.class));
        assertThrows(BusinessException.class, () -> service.uploadAvatar(file, request));
    }
}
