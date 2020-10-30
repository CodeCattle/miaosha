package com.seckillproject.error;

/**
 * @author Zhang Yifei
 * @date 2019/12/5 14:31
 * @description 错误类型枚举类
 */
public enum EmBusinessError implements CommonError {
    //通用错误类型10001
    PARAMETER_VALIDATION_ERROR(10001,"参数不合法"),
    UNKNOWN_ERROR(10002,"未知错误"),
    //20000开头为用户信息相关错误定义
    USER_NOT_EXIST(20001,"用户不存在"),
    USER_LOGIN_FAIL(20002,"用户手机号或密码不正确"),
    USER_NOT_LOGIN(20003, "用户还未登录"),
    //3000开头为交易信息错误
    STOCK_NOT_ENOUGH(30001, "库存不足"),
    MQ_FOUND_FAIL(30003, "库存异步消息失败"),
    RATELIMIT(30004,"活动太火爆，请稍后在重试");

    private int errCode;
    private String errMsg;

    /**
     * @author wang wei
     * @param errCode: 错误码
     * @param errMsg: 错误信息
     * @return null
     * @description 这是一个private的构造器，供上面定义的枚举具体类使用
     */
    private EmBusinessError(int errCode, String errMsg) {
        this.errCode = errCode;
        this.errMsg = errMsg;
    }

    @Override
    public int getErrCode() {
        return this.errCode;
    }

    @Override
    public String getErrMsg() {
        return this.errMsg;
    }

    @Override
    public CommonError setErMsg(String errMsg) {
        this.errMsg = errMsg;
        return this;
    }
}
