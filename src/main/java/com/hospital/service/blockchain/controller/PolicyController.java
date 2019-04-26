package com.hospital.service.blockchain.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hospital.service.blockchain.entity.Policy;
import com.hospital.service.blockchain.repository.PolicyRepository;
import com.hospital.service.blockchain.response.GetInsurancePurchasingInfoListResponse;
import com.hospital.service.blockchain.response.ResponseCode;
import com.hospital.service.blockchain.service.PolicyService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
public class PolicyController {
    @Autowired
    private PolicyRepository policyRepository;
    @Autowired
    private PolicyService policyService;

    @RequestMapping("/insurancePurchasingProcess/getInsurancePurchasingInfoList")
    @ResponseBody
    public String getInsurancePurchasingInfoList()
    {


        List<Policy> policies = policyRepository.findAll();
        GetInsurancePurchasingInfoListResponse getInsurancePurchasingInfoListResponse = new GetInsurancePurchasingInfoListResponse();
        getInsurancePurchasingInfoListResponse.setCode(ResponseCode.SUCCESS.getCode());
        getInsurancePurchasingInfoListResponse.setPolicies(policies);
        return getInsurancePurchasingInfoListResponse.getJsonStrig();
    }

    @ResponseBody
    @RequestMapping("/insurancePurchasingDetail/getInsurancePurchasingInfo")
    public String getInsurancePurchasingInfo(String insurancePurchasingInfoId)
    {
        Policy policy = policyRepository.findByInsurancePurchasingInfoId(insurancePurchasingInfoId);

        Map<String,Object> map = new LinkedHashMap<>();
        map.put("code", ResponseCode.SUCCESS.getCode());
        map.put("data",policy);
        return new Gson().toJson(map);
    }


}
