package com.java110.common.listener.auditMessage;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.common.dao.IAuditMessageServiceDao;
import com.java110.core.annotation.Java110Listener;
import com.java110.core.context.DataFlowContext;
import com.java110.entity.center.Business;
import com.java110.utils.constant.BusinessTypeConstant;
import com.java110.utils.constant.ResponseConstant;
import com.java110.utils.constant.StatusConstant;
import com.java110.utils.exception.ListenerExecuteException;
import com.java110.utils.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 修改审核原因信息 侦听
 *
 * 处理节点
 * 1、businessAuditMessage:{} 审核原因基本信息节点
 * 2、businessAuditMessageAttr:[{}] 审核原因属性信息节点
 * 3、businessAuditMessagePhoto:[{}] 审核原因照片信息节点
 * 4、businessAuditMessageCerdentials:[{}] 审核原因证件信息节点
 * 协议地址 ：https://github.com/java110/MicroCommunity/wiki/%E4%BF%AE%E6%94%B9%E5%95%86%E6%88%B7%E4%BF%A1%E6%81%AF-%E5%8D%8F%E8%AE%AE
 * Created by wuxw on 2018/5/18.
 */
@Java110Listener("updateAuditMessageInfoListener")
@Transactional
public class UpdateAuditMessageInfoListener extends AbstractAuditMessageBusinessServiceDataFlowListener {

    private static Logger logger = LoggerFactory.getLogger(UpdateAuditMessageInfoListener.class);
    @Autowired
    private IAuditMessageServiceDao auditMessageServiceDaoImpl;

    @Override
    public int getOrder() {
        return 2;
    }

    @Override
    public String getBusinessTypeCd() {
        return BusinessTypeConstant.BUSINESS_TYPE_UPDATE_AUDIT_MESSAGE;
    }

    /**
     * business过程
     * @param dataFlowContext 上下文对象
     * @param business 业务对象
     */
    @Override
    protected void doSaveBusiness(DataFlowContext dataFlowContext, Business business) {

        JSONObject data = business.getDatas();

        Assert.notEmpty(data,"没有datas 节点，或没有子节点需要处理");

        //处理 businessAuditMessage 节点
        if(data.containsKey("businessAuditMessage")){
            //处理 businessAuditMessage 节点
            if(data.containsKey("businessAuditMessage")){
                Object _obj = data.get("businessAuditMessage");
                JSONArray businessAuditMessages = null;
                if(_obj instanceof JSONObject){
                    businessAuditMessages = new JSONArray();
                    businessAuditMessages.add(_obj);
                }else {
                    businessAuditMessages = (JSONArray)_obj;
                }
                //JSONObject businessAuditMessage = data.getJSONObject("businessAuditMessage");
                for (int _auditMessageIndex = 0; _auditMessageIndex < businessAuditMessages.size();_auditMessageIndex++) {
                    JSONObject businessAuditMessage = businessAuditMessages.getJSONObject(_auditMessageIndex);
                    doBusinessAuditMessage(business, businessAuditMessage);
                    if(_obj instanceof JSONObject) {
                        dataFlowContext.addParamOut("auditMessageId", businessAuditMessage.getString("auditMessageId"));
                    }
                }
            }
        }
    }


    /**
     * business to instance 过程
     * @param dataFlowContext 数据对象
     * @param business 当前业务对象
     */
    @Override
    protected void doBusinessToInstance(DataFlowContext dataFlowContext, Business business) {

        JSONObject data = business.getDatas();

        Map info = new HashMap();
        info.put("bId",business.getbId());
        info.put("operate",StatusConstant.OPERATE_ADD);

        //审核原因信息
        List<Map> businessAuditMessageInfos = auditMessageServiceDaoImpl.getBusinessAuditMessageInfo(info);
        if( businessAuditMessageInfos != null && businessAuditMessageInfos.size() >0) {
            for (int _auditMessageIndex = 0; _auditMessageIndex < businessAuditMessageInfos.size();_auditMessageIndex++) {
                Map businessAuditMessageInfo = businessAuditMessageInfos.get(_auditMessageIndex);
                flushBusinessAuditMessageInfo(businessAuditMessageInfo,StatusConstant.STATUS_CD_VALID);
                auditMessageServiceDaoImpl.updateAuditMessageInfoInstance(businessAuditMessageInfo);
                if(businessAuditMessageInfo.size() == 1) {
                    dataFlowContext.addParamOut("auditMessageId", businessAuditMessageInfo.get("audit_message_id"));
                }
            }
        }

    }

    /**
     * 撤单
     * @param dataFlowContext 数据对象
     * @param business 当前业务对象
     */
    @Override
    protected void doRecover(DataFlowContext dataFlowContext, Business business) {

        String bId = business.getbId();
        //Assert.hasLength(bId,"请求报文中没有包含 bId");
        Map info = new HashMap();
        info.put("bId",bId);
        info.put("statusCd",StatusConstant.STATUS_CD_VALID);
        Map delInfo = new HashMap();
        delInfo.put("bId",business.getbId());
        delInfo.put("operate",StatusConstant.OPERATE_DEL);
        //审核原因信息
        List<Map> auditMessageInfo = auditMessageServiceDaoImpl.getAuditMessageInfo(info);
        if(auditMessageInfo != null && auditMessageInfo.size() > 0){

            //审核原因信息
            List<Map> businessAuditMessageInfos = auditMessageServiceDaoImpl.getBusinessAuditMessageInfo(delInfo);
            //除非程序出错了，这里不会为空
            if(businessAuditMessageInfos == null || businessAuditMessageInfos.size() == 0){
                throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_INNER_ERROR,"撤单失败（auditMessage），程序内部异常,请检查！ "+delInfo);
            }
            for (int _auditMessageIndex = 0; _auditMessageIndex < businessAuditMessageInfos.size();_auditMessageIndex++) {
                Map businessAuditMessageInfo = businessAuditMessageInfos.get(_auditMessageIndex);
                flushBusinessAuditMessageInfo(businessAuditMessageInfo,StatusConstant.STATUS_CD_VALID);
                auditMessageServiceDaoImpl.updateAuditMessageInfoInstance(businessAuditMessageInfo);
            }
        }

    }



    /**
     * 处理 businessAuditMessage 节点
     * @param business 总的数据节点
     * @param businessAuditMessage 审核原因节点
     */
    private void doBusinessAuditMessage(Business business,JSONObject businessAuditMessage){

        Assert.jsonObjectHaveKey(businessAuditMessage,"auditMessageId","businessAuditMessage 节点下没有包含 auditMessageId 节点");

        if(businessAuditMessage.getString("auditMessageId").startsWith("-")){
            throw new ListenerExecuteException(ResponseConstant.RESULT_PARAM_ERROR,"auditMessageId 错误，不能自动生成（必须已经存在的auditMessageId）"+businessAuditMessage);
        }
        //自动保存DEL
        autoSaveDelBusinessAuditMessage(business,businessAuditMessage);

        businessAuditMessage.put("bId",business.getbId());
        businessAuditMessage.put("operate", StatusConstant.OPERATE_ADD);
        //保存审核原因信息
        auditMessageServiceDaoImpl.saveBusinessAuditMessageInfo(businessAuditMessage);

    }




    public IAuditMessageServiceDao getAuditMessageServiceDaoImpl() {
        return auditMessageServiceDaoImpl;
    }

    public void setAuditMessageServiceDaoImpl(IAuditMessageServiceDao auditMessageServiceDaoImpl) {
        this.auditMessageServiceDaoImpl = auditMessageServiceDaoImpl;
    }



}