package build.dream.platform.services;

import build.dream.common.api.ApiRest;
import build.dream.common.saas.domains.ElemeAuthorizedTenant;
import build.dream.common.saas.domains.ElemeBranchMapping;
import build.dream.common.saas.domains.Tenant;
import build.dream.common.utils.*;
import build.dream.platform.constants.Constants;
import build.dream.platform.models.eleme.CheckIsAuthorizeModel;
import build.dream.platform.models.eleme.HandleTenantAuthorizeCallbackModel;
import build.dream.platform.models.eleme.SaveElemeBranchMappingModel;
import net.sf.json.JSONObject;
import org.apache.commons.lang.Validate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Date;

@Service
public class ElemeService {
    /**
     * 处理商户授权回调
     *
     * @param handleTenantAuthorizeCallbackModel
     * @return
     * @throws IOException
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiRest handleTenantAuthorizeCallback(HandleTenantAuthorizeCallbackModel handleTenantAuthorizeCallbackModel) throws IOException {
        BigInteger tenantId = handleTenantAuthorizeCallbackModel.getTenantId();
        BigInteger branchId = handleTenantAuthorizeCallbackModel.getBranchId();
        BigInteger userId = handleTenantAuthorizeCallbackModel.getUserId();
        Integer elemeAccountType = handleTenantAuthorizeCallbackModel.getElemeAccountType();
        String code = handleTenantAuthorizeCallbackModel.getCode();

        SearchModel searchModel = new SearchModel(true);
        searchModel.addSearchCondition("id", Constants.SQL_OPERATION_SYMBOL_EQUAL, tenantId);
        Tenant tenant = DatabaseHelper.find(Tenant.class, searchModel);
        Validate.notNull(tenant, "商户不存在！");

        String appKey = ConfigurationUtils.getConfiguration(Constants.ELEME_APP_KEY);
        String appSecret = ConfigurationUtils.getConfiguration(Constants.ELEME_APP_SECRET);
        String redirectUrl = CommonUtils.getOutsideUrl(Constants.SERVICE_NAME_OUT, "eleme", "tenantAuthorizeCallback");
        JSONObject tokenJsonObject = JSONObject.fromObject(ElemeUtils.obtainTokenByCode(code, appKey, appSecret, redirectUrl));
        Date fetchTokenTime = new Date();
        ElemeAuthorizedTenant elemeAuthorizedTenant = new ElemeAuthorizedTenant();
        elemeAuthorizedTenant.setTenantId(tenantId);
        String tokenField = null;
        if (elemeAccountType == Constants.ELEME_ACCOUNT_TYPE_CHAIN_ACCOUNT) {
            tokenField = Constants.ELEME_TOKEN + "_" + tenantId;
        } else if (elemeAccountType == Constants.ELEME_ACCOUNT_TYPE_INDEPENDENT_ACCOUNT) {
            tokenField = Constants.ELEME_TOKEN + "_" + tenantId + "_" + branchId;
            elemeAuthorizedTenant.setBranchId(branchId);
        }
        elemeAuthorizedTenant.setAccessToken(tokenJsonObject.getString("access_token"));
        elemeAuthorizedTenant.setRefreshToken(tokenJsonObject.getString("refresh_token"));
        elemeAuthorizedTenant.setExpiresIn(tokenJsonObject.getInt("expires_in"));
        elemeAuthorizedTenant.setTokenType(tokenJsonObject.getString("token_type"));
        elemeAuthorizedTenant.setFetchTokenTime(fetchTokenTime);

        elemeAuthorizedTenant.setCreatedUserId(userId);
        elemeAuthorizedTenant.setUpdatedUserId(userId);
        elemeAuthorizedTenant.setUpdatedRemark("商户授权获取token");
        DatabaseHelper.insert(elemeAuthorizedTenant);

        UpdateModel updateModel = new UpdateModel(true);
        updateModel.setTableName("eleme_authorized_tenant");
        updateModel.addContentValue(ElemeAuthorizedTenant.ColumnName.DELETED, 1);
        updateModel.addContentValue(ElemeAuthorizedTenant.ColumnName.UPDATED_USER_ID, userId);
        updateModel.addContentValue(ElemeAuthorizedTenant.ColumnName.UPDATED_REMARK, "商户重新授权，删除本条记录！");
        updateModel.addSearchCondition(ElemeAuthorizedTenant.ColumnName.TENANT_ID, Constants.SQL_OPERATION_SYMBOL_EQUAL, tenantId);
        if (elemeAccountType == Constants.ELEME_ACCOUNT_TYPE_CHAIN_ACCOUNT) {

        } else if (elemeAccountType == Constants.ELEME_ACCOUNT_TYPE_INDEPENDENT_ACCOUNT) {
            updateModel.addSearchCondition(ElemeAuthorizedTenant.ColumnName.BRANCH_ID, Constants.SQL_OPERATION_SYMBOL_EQUAL, branchId);
        }
        DatabaseHelper.universalUpdate(updateModel);

        tokenJsonObject.put("fetch_time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(fetchTokenTime));
        CacheUtils.hset(Constants.KEY_ELEME_TOKENS, tokenField, tokenJsonObject.toString());

        return ApiRest.builder().message("处理商户授权成功！").successful(true).build();
    }

    /**
     * 保存门店映射
     *
     * @param saveElemeBranchMappingModel
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiRest saveElemeBranchMapping(SaveElemeBranchMappingModel saveElemeBranchMappingModel) {
        BigInteger tenantId = saveElemeBranchMappingModel.getTenantId();
        BigInteger branchId = saveElemeBranchMappingModel.getBranchId();
        BigInteger userId = saveElemeBranchMappingModel.getUserId();
        BigInteger shopId = saveElemeBranchMappingModel.getShopId();
        String updatedRemark = "门店(" + branchId + ")绑定饿了么(" + shopId + ")，清除绑定关系！";

        UpdateModel updateModel = new UpdateModel(true);
        updateModel.setTableName("eleme_branch_mapping");
        updateModel.addContentValue(ElemeBranchMapping.ColumnName.DELETED, 1);
        updateModel.addContentValue(ElemeBranchMapping.ColumnName.UPDATED_USER_ID, userId);
        updateModel.addContentValue(ElemeBranchMapping.ColumnName.UPDATED_REMARK, updatedRemark);
        updateModel.addSearchCondition(ElemeBranchMapping.ColumnName.SHOP_ID, Constants.SQL_OPERATION_SYMBOL_EQUAL, shopId);
        DatabaseHelper.universalUpdate(updateModel);

        ElemeBranchMapping elemeBranchMapping = new ElemeBranchMapping();
        elemeBranchMapping.setTenantId(tenantId);
        elemeBranchMapping.setBranchId(branchId);
        elemeBranchMapping.setShopId(shopId);

        elemeBranchMapping.setCreatedUserId(userId);
        elemeBranchMapping.setUpdatedUserId(userId);
        elemeBranchMapping.setUpdatedRemark("保存饿了么门店映射！");
        DatabaseHelper.insert(elemeBranchMapping);

        return ApiRest.builder().message("保存饿了么门店映射成功！").successful(true).build();
    }

    /**
     * 检查是否授权
     *
     * @param checkIsAuthorizeModel
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiRest checkIsAuthorize(CheckIsAuthorizeModel checkIsAuthorizeModel) {
        return ApiRest.builder().build();
    }
}
