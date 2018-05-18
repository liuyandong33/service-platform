package build.dream.platform.services;

import build.dream.common.api.ApiRest;
import build.dream.common.saas.domains.Tenant;
import build.dream.common.utils.SearchModel;
import build.dream.platform.constants.Constants;
import build.dream.platform.mappers.TenantGoodsMapper;
import build.dream.platform.models.tenant.FindAllGoodsInfosModel;
import build.dream.platform.models.tenant.FindGoodsInfoModel;
import build.dream.platform.models.tenant.ObtainTenantInfoModel;
import build.dream.platform.utils.DatabaseHelper;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TenantService {
    @Autowired
    private TenantGoodsMapper tenantGoodsMapper;

    @Transactional(readOnly = true)
    public ApiRest obtainTenantInfo(ObtainTenantInfoModel obtainTenantInfoModel) {
        BigInteger tenantId = obtainTenantInfoModel.getTenantId();
        String tenantCode = obtainTenantInfoModel.getTenantCode();

        SearchModel searchModel = new SearchModel(true);
        if (tenantId != null) {
            searchModel.addSearchCondition("id", Constants.SQL_OPERATION_SYMBOL_EQUALS, tenantId);
        }
        if (StringUtils.isNotBlank(tenantCode)) {
            searchModel.addSearchCondition("code", Constants.SQL_OPERATION_SYMBOL_EQUALS, tenantCode);
        }
        Tenant tenant = DatabaseHelper.find(Tenant.class, searchModel);

        ApiRest apiRest = new ApiRest();
        apiRest.setClassName(Tenant.class.getName());
        apiRest.setData(tenant);
        apiRest.setMessage("查询商户信息成功！");
        apiRest.setSuccessful(true);
        return apiRest;
    }

    @Transactional(readOnly = true)
    public ApiRest findAllGoodsInfos(FindAllGoodsInfosModel findAllGoodsInfosModel) {
        List<Map<String, Object>> goodsInfos = tenantGoodsMapper.findAllGoodsInfos(findAllGoodsInfosModel.getTenantId(), findAllGoodsInfosModel.getBranchId());
        ApiRest apiRest = new ApiRest();
        apiRest.setData(goodsInfos);
        apiRest.setMessage("查询产品购买信息成功！");
        apiRest.setSuccessful(true);
        return apiRest;
    }

    @Transactional(readOnly = true)
    public ApiRest findGoodsInfo(FindGoodsInfoModel findGoodsInfoModel) {
        Map<String, Object> goodsInfo = tenantGoodsMapper.findGoodsInfo(findGoodsInfoModel.getTenantId(), findGoodsInfoModel.getBranchId(), findGoodsInfoModel.getGoodsId());
        Validate.notEmpty(goodsInfo, "未检索到产品购买信息！");
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("goods", goodsInfo);
        data.put("currentTime", new Date());
        ApiRest apiRest = new ApiRest();
        apiRest.setData(data);
        apiRest.setMessage("查询产品购买信息成功！");
        return apiRest;
    }
}
