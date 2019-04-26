package com.hospital.service.blockchain.blockchainmessage;

import cn.xjfme.encrypt.test.SecurityTestAll;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class SubmitCompleteMessage {
    private String paymentId;
    private String senderPubkey;
    private String senderPrikey;

    public String toJsonString()
    {
        Map<String ,Object> data = new LinkedHashMap<>();
        data.put("paymentId",paymentId);

        Map<String,Object> map = new LinkedHashMap<>();
        map.put("sign","no shixian now");
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
    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
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
}
