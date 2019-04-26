package com.hospital.service.blockchain.controller;


import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hospital.service.blockchain.entity.Payment;
import com.hospital.service.blockchain.entity.Record;
import com.hospital.service.blockchain.repository.PaymentRepository;
import com.hospital.service.blockchain.repository.RecordRepository;
import com.hospital.service.blockchain.response.GetDirectPaymentInfoIdReponse;
import com.hospital.service.blockchain.response.GetDirectPaymentInfoListResponse;
import com.hospital.service.blockchain.response.ResponseCode;
import com.hospital.service.blockchain.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class PaymentController {
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private RecordRepository recordRepository;

    @RequestMapping("/directPaymentProcess/getDirectPaymentInfoList")
    @ResponseBody
    public String GetDirectPaymentInfoList()
    {
        paymentService.updatePaymentFromBlockchain();
        GetDirectPaymentInfoListResponse response =new GetDirectPaymentInfoListResponse();
        response.setCode(ResponseCode.SUCCESS.getCode());
        List<Payment> payments = paymentRepository.findAll();
        response.setPayments(payments);
        return response.toJsonString();
    }

    @RequestMapping("/directPaymentDetail/getDirectPaymentInfo")
    @ResponseBody
    public String GetDirectPaymentInfo(String directPaymentInfoId )
    {
        paymentService.updatePaymentFromBlockchain();
        Payment payment = paymentRepository.findByDirectPaymentInfoId(directPaymentInfoId);
        GetDirectPaymentInfoIdReponse reponse = new GetDirectPaymentInfoIdReponse();
        reponse.setPayment(payment);
        return  reponse.toJSonString();
    }

    @ResponseBody
    @PostMapping("/directPaymentDetail/hospitalConfirmPayment")
    public String hospitalConfirmPayment(@RequestBody String json)
    {
        System.out.println("-----------------"+json+"--------------");
        JsonObject jsonObject = new JsonParser().parse(json).getAsJsonObject();
        String directPaymentInfoId = jsonObject.get("directPaymentInfoId").getAsString();
        Payment payment = paymentRepository.findByDirectPaymentInfoId(directPaymentInfoId);
        payment.setDirectPaymentStage(4);
        paymentRepository.saveAndFlush(payment);

        paymentService.submitComplete(directPaymentInfoId);

        Map<String,Object> map = new LinkedHashMap<>();
        map.put("code",ResponseCode.SUCCESS.getCode());
        return new Gson().toJson(map);
    }

    @ResponseBody
    @RequestMapping("/directPaymentDetail/hospitalConfirmPayable")
    public  String hospitalConfirmPayable(@RequestBody String json)
    {
        System.out.println("-----------------"+json+"--------------");
        JsonObject jsonObject = new JsonParser().parse(json).getAsJsonObject();
        String directPaymentInfoId = jsonObject.get("directPaymentInfoId").getAsString();
        Integer payable = jsonObject.get("payable").getAsInt();

        Payment payment = paymentRepository.findByDirectPaymentInfoId(directPaymentInfoId);
        if(payable == 0)
        {
            payment.setDirectPaymentStage(-1);
            paymentRepository.saveAndFlush(payment);
            paymentService.submitHospitalDecline(directPaymentInfoId);
        }
        else if(payable == 1)
        {
            payment.setDirectPaymentStage(2);
            paymentRepository.saveAndFlush(payment);
            paymentService.submitInsuranceConfirm(directPaymentInfoId,recordRepository.findByPubkey(payment.getPublicKey()));
        }
        Map<String,Object> map = new LinkedHashMap<>();
        map.put("code",ResponseCode.SUCCESS.getCode());
        return new Gson().toJson(map);
    }

    @ResponseBody
    @RequestMapping("/testconfim")
    public String smellHospitalConfirm()
    {
        try {
            paymentService.smellHospitalConfirm();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "200";
    }

    @RequestMapping("/testDec")
    @ResponseBody
    public String submitTestDecline(String paymentId)
    {
        paymentService.submitHospitalDecline(paymentId);
        return "200";
    }

    @RequestMapping("/submitIns")
    @ResponseBody
    public String submitInsuranceConfirm(String paymentId)
    {
        List<Record> records = recordRepository.findByPubkey("042e36e1a0b8329167533551183630e7c24deab17aed97f58e392a34a7f33efe97a64b3c73dbc75583c48d34dbb8b045cfabd8fc21fc40599c22a3c6dcbf132faf");
        paymentService.submitInsuranceConfirm(paymentId,records);
        return "200";
    }

    @RequestMapping("/testFin")
    @ResponseBody
    public String submitTestFinal()
    {
        try {
            paymentService.handleHospitalConfirmFinal();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "200";
    }

    @RequestMapping("/submitComp")
    @ResponseBody
    public String submitComplete(String paymentId)
    {
      //  List<Record> records = recordRepository.findByPubkey("042e36e1a0b8329167533551183630e7c24deab17aed97f58e392a34a7f33efe97a64b3c73dbc75583c48d34dbb8b045cfabd8fc21fc40599c22a3c6dcbf132faf");
        paymentService.submitComplete(paymentId);
        return "200";
    }

}
