package com.hospital.service.blockchain.service;

import com.bubi.connect.ContractConnect;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hospital.service.blockchain.blockchainmessage.SubmitCompleteMessage;
import com.hospital.service.blockchain.blockchainmessage.SubmitHospitalDeclineMessage;
import com.hospital.service.blockchain.blockchainmessage.SubmitInsuranceConfirmMessage;
import com.hospital.service.blockchain.blockchainmessage.SubmitInsuranceConfirmToThirdMessgae;
import com.hospital.service.blockchain.entity.Insurance;
import com.hospital.service.blockchain.entity.Payment;
import com.hospital.service.blockchain.entity.Policy;
import com.hospital.service.blockchain.entity.Record;
import com.hospital.service.blockchain.repository.InsuranceRepository;
import com.hospital.service.blockchain.repository.PaymentRepository;
import com.hospital.service.blockchain.repository.PolicyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;

import static cn.xjfme.encrypt.test.SecurityTestAll.SM2Dec;
import static java.lang.Thread.*;

@Service
public class PaymentService {
    @Autowired
    private PolicyRepository policyRepository;
    @Autowired
    private InsuranceRepository insuranceRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Resource
    private Environment environment;


    public void updatePaymentFromBlockchain()
    {
        try {
            smellHospitalConfirm();
            handleHospitalConfirmFinal();
            handleInsuranceDecline();

        }catch (IOException e)
        {
            e.printStackTrace();
        }
    }
   //第一阶段
    public  void smellHospitalConfirm() throws IOException {
        String contract = environment.getProperty("payment.hospital.confirm.payable");
      //  String contract = "9a9f1d22b06f0054796c150043044bceca73cb29b7bd3b03b61374bf10a835dc";
        String pubKey = environment.getProperty("hospital.pubkey");
        String priKey = environment.getProperty("hospital.prikey");
        String json = ContractConnect.get(pubKey,contract);
        System.out.println(json);
        JsonObject object = new JsonParser().parse(json).getAsJsonObject();
        JsonObject result = object.get("result").getAsJsonObject();
        if(result.get("value").getAsString().equals("false")){return ;}
        String[] encValues = result.get("value").getAsString().split(",");

        for(String encValue : encValues)
        {
            String value = SM2Dec(priKey,encValue);
            System.out.println(value);
            hanldeHospitalConfirm(value);
        }
    }
    private void hanldeHospitalConfirm(String value)
    {
        JsonObject jsonObject =new JsonParser().parse(value).getAsJsonObject();
        String insuranceRawString = jsonObject.getAsJsonObject("data").get("insurance").toString();
        Insurance insurance = new Gson().fromJson(insuranceRawString,Insurance.class);
        System.out.println(insurance.toString());
        String policyRawString = jsonObject.getAsJsonObject("data").getAsJsonObject().get("policy").toString();
        Policy policy = new Gson().fromJson(policyRawString,Policy.class);
        System.out.println(policy.toString());
        String paymentRwString = jsonObject.getAsJsonObject("data").get("paymment").toString();
        Payment payment = new Gson().fromJson(paymentRwString,Payment.class);
        System.out.println(payment.toString());

        if(insuranceRepository.findByInsuranceId(insurance.getInsuranceId()) == null)
        {
            insuranceRepository.saveAndFlush(insurance);
        }

        if(policyRepository.findByInsurancePurchasingInfoId(policy.getInsurancePurchasingInfoId()) == null)
        {
            policyRepository.saveAndFlush(policy);
        }

        if(paymentRepository.findByDirectPaymentInfoId(payment.getDirectPaymentInfoId()) == null)
        {
            payment.setDirectPaymentStage(1);
            paymentRepository.saveAndFlush(payment);
        }
    }

    //第二阶段
    public void submitInsuranceConfirm(String paymentId, List<Record> records)
    {
        String contract = environment.getProperty("payment.insurance.verify");
        Payment payment = paymentRepository.findByDirectPaymentInfoId(paymentId);
        if(payment == null)
        {
            return ;
        }
        String reveiverPubkey = environment.getProperty("insurance.company.pubkey");
        String receiverPubkeyToThird = payment.getPublicKey();
        String senderPubkey = environment.getProperty("hospital.pubkey");
        String senderPrikey = environment.getProperty("hospital.prikey");
        String policyId = payment.getInsurancePurchasingInfoId();

        SubmitInsuranceConfirmMessage message = new SubmitInsuranceConfirmMessage();
        message.setPayment(payment);
        message.setPolicyId(policyId);
        message.setRecords(records);
        message.setSenderPrikey(senderPrikey);
        message.setSenderPubkey(senderPubkey);
        String source = message.toJsonString();
        System.out.println(source);

        SubmitInsuranceConfirmToThirdMessgae messgaeToThird = new SubmitInsuranceConfirmToThirdMessgae();
        messgaeToThird.setPaymentId(paymentId);
        messgaeToThird.setSenderPubkey(senderPubkey);
        messgaeToThird.setSenderPrikey(senderPrikey);
        String sourceToThird = messgaeToThird.toJsonString();
        System.out.println(sourceToThird);

        String encSource = null;
        try {
            encSource = message.encrypt(source,reveiverPubkey);
            System.out.println("encSource: "+encSource);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String encSourceToThird = null;
        try {
            encSourceToThird = messgaeToThird.encrypt(sourceToThird,receiverPubkeyToThird);
            System.out.println("encSourceToThird: "+encSourceToThird);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ContractConnect.set(reveiverPubkey,encSource,contract);

        try {
            sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        ContractConnect.set(receiverPubkeyToThird,encSourceToThird,contract);
    }

    //医院拒绝
    public void submitHospitalDecline(String paymentId)
    {
        String contract = environment.getProperty("payment.hospital.declined");
        Payment payment = paymentRepository.findByDirectPaymentInfoId(paymentId);
        payment.setDirectPaymentStage(-1);
        if(payment == null)
        {
            return ;
        }
        String reveiverPubkey = payment.getPublicKey();
        String senderPubkey = environment.getProperty("hospital.pubkey");
        String senderPrikey = environment.getProperty("hospital.prikey");

        SubmitHospitalDeclineMessage message = new SubmitHospitalDeclineMessage();
        message.setPaymentId(paymentId);
        message.setSenderPrikey(senderPrikey);
        message.setSenderPubkey(senderPubkey);
        String source = message.toJsonString();
        System.out.println(source);

        String encSource = null;
        try {
            encSource = message.encrypt(source,reveiverPubkey);
            System.out.println("encSource: "+encSource);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ContractConnect.set(reveiverPubkey,encSource,contract);

    }

    //获取医院最终同意
    public void handleHospitalConfirmFinal() throws IOException {
        String pubKey = environment.getProperty("hospital.pubkey");
        String priKey = environment.getProperty("hospital.prikey");
        String contract = environment.getProperty("payment.hospital.confirm.payment");
        String json = ContractConnect.get(pubKey, contract);
        System.out.println(json);
        JsonObject object = new JsonParser().parse(json).getAsJsonObject();
        JsonObject result = object.get("result").getAsJsonObject();
        if (result.get("value").getAsString().equals("false")) {
            return;
        }

        String[] encValues = result.get("value").getAsString().split(",");

        for (String encValue : encValues) {
            String value = SM2Dec(priKey, encValue);
            System.out.println(value);
            JsonObject jsonObject = new JsonParser().parse(value).getAsJsonObject();
            String paymentId = jsonObject.getAsJsonObject("data").get("paymentId").getAsString();
            System.out.println("paymentId: " + paymentId);
            String publicKey = jsonObject.get("senderPubkey").getAsString();
            System.out.println("senderpubkey:  " + publicKey);
            String sign = jsonObject.get("sign").getAsString();
            System.out.println("sign:  " + sign);

            //应该有个sign验证过程
            Payment payment = paymentRepository.findByDirectPaymentInfoId(paymentId);
            int stage = payment.getDirectPaymentStage();
            if(stage == 4 || stage == -1 || stage == -2 ||stage==3)
            {
                return;
            }
            payment.setDirectPaymentStage(3);
            paymentRepository.saveAndFlush(payment);
        }
    }
    //获取保险公司拒绝
    public void handleInsuranceDecline() throws IOException {
        String pubKey = environment.getProperty("hospital.pubkey");
        String priKey = environment.getProperty("hospital.prikey");
        String contract = environment.getProperty("payment.hospital.declined");
        String json = ContractConnect.get(pubKey, contract);
        System.out.println(json);
        JsonObject object = new JsonParser().parse(json).getAsJsonObject();
        JsonObject result = object.get("result").getAsJsonObject();
        if (result.get("value").getAsString().equals("false")) {
            return;
        }

        String[] encValues = result.get("value").getAsString().split(",");

        for (String encValue : encValues) {
            String value = SM2Dec(priKey, encValue);
            System.out.println(value);
            JsonObject jsonObject = new JsonParser().parse(value).getAsJsonObject();
            String paymentId = jsonObject.getAsJsonObject("data").get("paymentId").getAsString();
            System.out.println("paymentId: " + paymentId);
            String publicKey = jsonObject.get("senderPubkey").getAsString();
            System.out.println("senderpubkey:  " + publicKey);
            String sign = jsonObject.get("sign").getAsString();
            System.out.println("sign:  " + sign);

            //应该有个sign验证过程
            Payment payment = paymentRepository.findByDirectPaymentInfoId(paymentId);
            payment.setDirectPaymentStage(-2);
            paymentRepository.saveAndFlush(payment);
        }
    }

    public void submitComplete(String paymentId)
    {
        String contract = environment.getProperty("payment.complete");
        Payment payment = paymentRepository.findByDirectPaymentInfoId(paymentId);
        if(payment == null){return ;}
        String senderPubkey = environment.getProperty("hospital.pubkey");
        String senderPrikey = environment.getProperty("hospital.prikey");

        SubmitCompleteMessage message = new SubmitCompleteMessage();
        message.setPaymentId(paymentId);
        message.setSenderPrikey(senderPrikey);
        message.setSenderPubkey(senderPubkey);
        String source = message.toJsonString();
        System.out.println(source);

        String receiverPubkey = environment.getProperty("insurance.company.pubkey");
        String encSource = null;
        try {
            encSource = message.encrypt(source,receiverPubkey);
            System.out.println("encSource: "+encSource);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ContractConnect.set(receiverPubkey,encSource,contract);

        receiverPubkey = payment.getPublicKey();
        encSource = null;
        try {
            encSource = message.encrypt(source,receiverPubkey);
            System.out.println("encSource: "+encSource);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        ContractConnect.set(receiverPubkey,encSource,contract);
    }


    public static void main(String[] args) throws  Exception{
     //   smellHospitalConfirm("04dee4c9569b83964b8e89c898dc35ba3694f36721a4f5972a3e3caa93b11f84058156b74390e40b474c4bf86669587d67099e6165d868f498997538a81193e4aa","7036a322829ee67a7661b764b883221b4aff94483bbbce836fc0ea06585bc01f");
    }


}
