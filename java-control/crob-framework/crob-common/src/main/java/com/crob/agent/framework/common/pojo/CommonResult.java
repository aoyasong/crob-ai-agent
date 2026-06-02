package com.crob.agent.framework.common.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import lombok.Data;
import org.springframework.util.Assert;

@Data
public class CommonResult<T> implements Serializable {

    private Integer code;
    private String msg;
    private T data;

    public static <T> CommonResult<T> success(T data) {
        CommonResult<T> result = new CommonResult<>();
        result.code = 0;
        result.msg = "ok";
        result.data = data;
        return result;
    }

    public static <T> CommonResult<T> error(Integer code, String msg) {
        Assert.isTrue(!Integer.valueOf(0).equals(code), "code must not be 0");
        CommonResult<T> result = new CommonResult<>();
        result.code = code;
        result.msg = msg;
        return result;
    }

    @JsonIgnore
    public boolean isSuccess() {
        return Integer.valueOf(0).equals(code);
    }
}
