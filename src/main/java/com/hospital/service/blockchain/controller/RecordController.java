package com.hospital.service.blockchain.controller;

import cn.xjfme.encrypt.test.SecurityTestAll;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.hospital.service.blockchain.entity.Record;
import com.hospital.service.blockchain.repository.RecordRepository;
import com.hospital.service.blockchain.response.GetMedicalRecordInfoListResponse;
import com.hospital.service.blockchain.response.ResponseCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class RecordController {
    @Autowired
    private RecordRepository recordRepository;


    @RequestMapping("/personalCenter/getMedicalRecordInfoList")
    @ResponseBody
    public String getMedicalRecordInfoList(String pubkey)
    {
        List<Record> records = recordRepository.findByPubkey(pubkey);

        GetMedicalRecordInfoListResponse response = new GetMedicalRecordInfoListResponse();
        response.setCode(ResponseCode.SUCCESS.getCode());
        response.setRecords(records);

        return response.toJsonString();
    }

    @ResponseBody
    @PostMapping("/personalCenter/authorizationMedicalRecord")
    public String authorizationMedicalRecord(@RequestBody String json)
    {
        System.out.println("-----------------"+json+"--------------");
        JsonObject jsonObject = new JsonParser().parse(json).getAsJsonObject();
        String username = jsonObject.get("publicKey").getAsString();

        //no shixian now

        Map<String,Object> map = new LinkedHashMap<>();
        map.put("code", ResponseCode.SUCCESS.getCode());
        map.put("data",null);

        return new Gson().toJson(map);
    }

    @ResponseBody
    @PostMapping("/submitMedicalRecord/submitMedicalRecord")
    public String submitMedicalRecord( @RequestBody String json)
    {
        System.out.println("-----------------"+json+"--------------");
        JsonObject jsonObject = new JsonParser().parse(json).getAsJsonObject();
        String  publicKey = jsonObject.get("publicKey").getAsString();
        JsonArray jsonArray = jsonObject.get("medicalRecordList").getAsJsonArray();
        List<Record> records = new Gson().fromJson(jsonArray,new TypeToken<List<Record>>(){}.getType());

        for (Record record : records)
        {
            record.setPubkey(publicKey);
            record.setMedicalRecordInfoId(SecurityTestAll.generateSM3HASH(record.toString()));
            System.out.println(record);
            recordRepository.saveAndFlush(record);
        }

        Map<String,Object> map = new LinkedHashMap<>();
        map.put("code",ResponseCode.SUCCESS.getCode());
        return new Gson().toJson(map);
    }

    public static void main(String[] args) {
        new RecordController().submitMedicalRecord("{\n" +
                "   \"publicKey\": \"String\",      \n" +
                "    \"medicalRecordList\":\n" +
                "    [    {\n" +
                "            \"treatmentDate\": \"String\",              \n" +
                "            \"treatmentHospital\": \"String\",          \n" +
                "            \"treatmentDoctor\": \"String\",            \n" +
                "            \"medicalRecordContent\":\"tring\"     \n" +
                "        }]\n" +
                "}");
    }
}
