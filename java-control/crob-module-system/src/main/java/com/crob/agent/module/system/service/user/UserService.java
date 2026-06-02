package com.crob.agent.module.system.service.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crob.agent.module.system.controller.admin.user.vo.UserCreateReqVO;
import com.crob.agent.module.system.controller.admin.user.vo.UserPageReqVO;
import com.crob.agent.module.system.controller.admin.user.vo.UserRespVO;
import com.crob.agent.module.system.controller.admin.user.vo.UserUpdateReqVO;
import com.crob.agent.module.system.dal.dataobject.SysUserDO;
import com.crob.agent.module.system.dal.mysql.SysUserMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Resource
    private SysUserMapper userMapper;

    @Resource
    private PasswordEncoder passwordEncoder;

    public IPage<UserRespVO> page(UserPageReqVO req) {
        Page<SysUserDO> page = new Page<>(req.getPageNo(), req.getPageSize());
        LambdaQueryWrapper<SysUserDO> wrapper = new LambdaQueryWrapper<>();
        if (req.getUsername() != null && !req.getUsername().isEmpty()) {
            wrapper.like(SysUserDO::getUsername, req.getUsername());
        }
        if (req.getNickname() != null && !req.getNickname().isEmpty()) {
            wrapper.like(SysUserDO::getNickname, req.getNickname());
        }
        if (req.getStatus() != null) {
            wrapper.eq(SysUserDO::getStatus, req.getStatus());
        }
        IPage<SysUserDO> doPage = userMapper.selectPage(page, wrapper);
        List<UserRespVO> voList = doPage.getRecords().stream()
                .map(this::toRespVO)
                .collect(Collectors.toList());
        Page<UserRespVO> voPage = new Page<>(req.getPageNo(), req.getPageSize());
        voPage.setRecords(voList);
        voPage.setTotal(doPage.getTotal());
        return voPage;
    }

    public UserRespVO get(Long id) {
        SysUserDO user = userMapper.selectById(id);
        return user != null ? toRespVO(user) : null;
    }

    public Long create(UserCreateReqVO req) {
        SysUserDO user = new SysUserDO();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setNickname(req.getNickname());
        user.setDeptId(req.getDeptId());
        user.setEmail(req.getEmail());
        user.setMobile(req.getMobile());
        user.setSex(req.getSex());
        user.setAvatar(req.getAvatar());
        user.setStatus(req.getStatus() != null ? req.getStatus() : 0);
        user.setRemark(req.getRemark());
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);
        return user.getId();
    }

    public void update(UserUpdateReqVO req) {
        SysUserDO user = new SysUserDO();
        user.setId(req.getId());
        if (req.getUsername() != null) {
            user.setUsername(req.getUsername());
        }
        if (req.getPassword() != null && !req.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(req.getPassword()));
        }
        user.setNickname(req.getNickname());
        user.setDeptId(req.getDeptId());
        user.setEmail(req.getEmail());
        user.setMobile(req.getMobile());
        user.setSex(req.getSex());
        user.setAvatar(req.getAvatar());
        user.setStatus(req.getStatus());
        user.setRemark(req.getRemark());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    public void delete(Long id) {
        userMapper.deleteById(id);
    }

    private UserRespVO toRespVO(SysUserDO user) {
        UserRespVO vo = new UserRespVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setDeptId(user.getDeptId());
        vo.setEmail(user.getEmail());
        vo.setMobile(user.getMobile());
        vo.setSex(user.getSex());
        vo.setAvatar(user.getAvatar());
        vo.setStatus(user.getStatus());
        vo.setRemark(user.getRemark());
        vo.setLoginIp(user.getLoginIp());
        vo.setLoginDate(user.getLoginDate());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}
