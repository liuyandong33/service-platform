package build.dream.platform.controllers;

import build.dream.common.api.ApiRest;
import build.dream.common.utils.GsonUtils;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.common.utils.BinaryUtil;
import com.aliyun.oss.model.MatchMode;
import com.aliyun.oss.model.PolicyConditions;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping(value = "/oss")
public class OssController {
    @RequestMapping(value = "/obtainPolicy")
    @ResponseBody
    public String obtainPolicy() {
        ApiRest apiRest = null;
        try {
            String endpoint = "oss-cn-qingdao.aliyuncs.com";
            String accessId = "LTAIzWtJmkzU0Uex";
            String accessKey = "6XIiGAie3fEPoUIhQpZMsXuPa80bwT";
            String bucket = "image-check-local";
            String dir = "user-dir";
            String host = "http://" + bucket + "." + endpoint;
            OSSClient client = new OSSClient(endpoint, accessId, accessKey);
            long expireTime = 30;
            long expireEndTime = System.currentTimeMillis() + expireTime * 1000;
            Date expiration = new Date(expireEndTime);
            PolicyConditions policyConditions = new PolicyConditions();
            policyConditions.addConditionItem(PolicyConditions.COND_CONTENT_LENGTH_RANGE, 0, 1048576000);
            policyConditions.addConditionItem(MatchMode.StartWith, PolicyConditions.COND_KEY, dir);

            String postPolicy = client.generatePostPolicy(expiration, policyConditions);
            byte[] binaryData = postPolicy.getBytes("utf-8");
            String encodedPolicy = BinaryUtil.toBase64String(binaryData);
            String postSignature = client.calculatePostSignature(postPolicy);

            Map<String, String> respMap = new LinkedHashMap<String, String>();
            respMap.put("accessid", accessId);
            respMap.put("policy", encodedPolicy);
            respMap.put("signature", postSignature);
            respMap.put("dir", dir);
            respMap.put("host", host);
            respMap.put("expire", String.valueOf(expireEndTime / 1000));
            respMap.put("callback", "http://check-local.smartpos.top/zd1/ct3/proxy/doGet");

            apiRest = new ApiRest();
            apiRest.setData(respMap);
            apiRest.setSuccessful(true);
        } catch (Exception e) {
            apiRest = new ApiRest(e);
        }
        return GsonUtils.toJson(apiRest);
    }

    @RequestMapping(value = "/index")
    public ModelAndView index() {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("demo/index");
        return modelAndView;
    }
}
