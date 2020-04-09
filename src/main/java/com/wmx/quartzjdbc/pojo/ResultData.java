package com.wmx.quartzjdbc.pojo;


import com.wmx.quartzjdbc.enums.ResultCode;

import java.io.Serializable;

/**
 * 页面返回值对象，用于封装返回数据。
 * ResultData 对象中的属性需要提供 setter、getter 方法，控制层的 @ResponseBody 注解会自动将 ResultData 对象转为 json 格式数据返回给页面
 *
 * @author wangmaoxiong
 */
public class ResultData implements Serializable {
    private Integer code;
    private String message;
    private Object data;

    public ResultData(ResultCode resultCode, Object data) {
        this.code = resultCode.getCode();
        this.message = resultCode.getMessage();
        this.data = data;
    }

    public ResultData(Integer code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "ResultData{" +
                "code=" + code +
                ", message='" + message + '\'' +
                ", data=" + data +
                '}';
    }
}
