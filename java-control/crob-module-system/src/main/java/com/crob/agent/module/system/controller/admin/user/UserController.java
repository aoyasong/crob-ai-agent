package com.crob.agent.module.system.controller.admin.user;

import com.crob.agent.framework.common.pojo.CommonResult;
import com.crob.agent.module.system.controller.admin.user.vo.UserCreateReqVO;
import com.crob.agent.module.system.controller.admin.user.vo.UserPageReqVO;
import com.crob.agent.module.system.controller.admin.user.vo.UserRespVO;
import com.crob.agent.module.system.controller.admin.user.vo.UserUpdateReqVO;
import com.crob.agent.module.system.service.user.UserService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import java.util.Collections;
import java.util.Map;
import javax.annotation.Resource;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/user")
public class UserController {

    @Resource
    private UserService userService;

    @GetMapping("/page")
    public CommonResult<Map<String, Object>> page(@Valid UserPageReqVO req) {
        IPage<UserRespVO> page = userService.page(req);
        Map<String, Object> result = Map.of(
                "list", page.getRecords(),
                "total", page.getTotal());
        return CommonResult.success(result);
    }

    @GetMapping("/get")
    public CommonResult<UserRespVO> get(@RequestParam Long id) {
        return CommonResult.success(userService.get(id));
    }

    @PostMapping("/create")
    public CommonResult<Long> create(@RequestBody @Valid UserCreateReqVO req) {
        return CommonResult.success(userService.create(req));
    }

    @PutMapping("/update")
    public CommonResult<Boolean> update(@RequestBody @Valid UserUpdateReqVO req) {
        userService.update(req);
        return CommonResult.success(true);
    }

    @GetMapping("/simple-list")
    public CommonResult<java.util.List<UserRespVO>> simpleList() {
        return CommonResult.success(userService.page(new UserPageReqVO()).getRecords());
    }

    @DeleteMapping("/delete")
    public CommonResult<Boolean> delete(@RequestParam Long id) {
        userService.delete(id);
        return CommonResult.success(true);
    }
}
