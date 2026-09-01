package vcampus.common.vo;

import java.io.Serializable;

/**
 * 医院模块管理员请求包装对象，用于同时传递登录userId和业务数据
 * 【hospital分支本地，不依赖user模块，后续合并无需修改】
 */
public class HospitalAdminReq implements Serializable {
    private static final long serialVersionUID = 1L;
    private String loginUserId;
    private Object payload;

    public HospitalAdminReq(){}
    public HospitalAdminReq(String loginUserId, Object payload){
        this.loginUserId = loginUserId;
        this.payload = payload;
    }

    public String getLoginUserId() { return loginUserId; }
    public void setLoginUserId(String loginUserId) { this.loginUserId = loginUserId; }
    public Object getPayload() { return payload; }
    public void setPayload(Object payload) { this.payload = payload; }
}
