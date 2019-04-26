package com.hospital.service.blockchain.blockchainmessage;

import cn.xjfme.encrypt.test.SecurityTestAll;
import com.google.gson.Gson;
import com.hospital.service.blockchain.entity.Payment;
import com.hospital.service.blockchain.entity.Record;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SubmitInsuranceConfirmMessage {
    private String policyId;
    private Payment payment;
    private List<Record> records;
    private String senderPubkey;
    private String senderPrikey;

    public String toJsonString()
    {

        Map<String,Object> data =new LinkedHashMap<>();
        data.put("policyId",policyId);
        data.put("payment",payment);
        data.put("records",records);

        Map<String,Object> map = new LinkedHashMap<>();
        map.put("sign","no shi xian now");
        map.put("senderPubkey",senderPubkey);
        map.put("data",data);
        return new Gson().toJson(map);
    }
    public String encrypt(String source ,String senderPubkey) throws IOException {
        String SM2Enc = SecurityTestAll.SM2Enc(senderPubkey, source);
        System.out.println("加密:");
        System.out.println("密文:" + SM2Enc);
        return SM2Enc;
    }

    public List<Record> getRecords() {
        return records;
    }

    public void setRecords(List<Record> records) {
        this.records = records;
    }

    public String getSenderPubkey() {
        return senderPubkey;
    }

    public void setSenderPubkey(String senderPubkey) {
        this.senderPubkey = senderPubkey;
    }

    public String getSenderPrikey() {
        return senderPrikey;
    }

    public void setSenderPrikey(String senderPrikey) {
        this.senderPrikey = senderPrikey;
    }

    public String getPolicyId() {
        return policyId;
    }

    public void setPolicyId(String policyId) {
        this.policyId = policyId;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }
}
