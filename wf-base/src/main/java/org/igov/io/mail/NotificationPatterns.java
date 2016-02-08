package org.igov.io.mail;

import org.activiti.engine.impl.util.json.JSONArray;
import org.activiti.engine.impl.util.json.JSONObject;
import org.apache.commons.mail.EmailException;
import org.springframework.beans.factory.annotation.Autowired;
import org.igov.io.GeneralConfig;
import static org.igov.service.business.action.task.core.ActionTaskService.createTable_TaskProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User: goodg_000
 * Date: 25.08.2015
 * Time: 22:54
 */
public class NotificationPatterns {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationPatterns.class);
    
    @Autowired
    GeneralConfig generalConfig;

    @Autowired
    Mail oMail;

    public void sendTaskCreatedInfoEmail(String sMailTo, String sID_Order) throws EmailException {

      /*
      String sHead = String.format("Ви подали заяву №%s на послугу через портал %s", nID_Protected,
              generalConfig.sHostCentral());

      String sBody = String.format("Ви подали заяву №%s на послугу через портал %s", nID_Protected,
              generalConfig.sHostCentral()) +
              "<br>(Ви завжди можете подивитись її статус на порталі у разділі \"Статуси\")" +
              "<br>" +
              "При надходжені Вашої заявки у систему госоргану - Вам буде додатково направлено персональний лист - повідомленя.<br>";
      */
        try{
            String sHead = String.format("Ваша заявка %s прийнята!", sID_Order);

            String sBody = String.format("Ваша заявка %s прийнята!", sID_Order) +
                    "<br>Ви завжди зможете переглянути її поточний статус у розділі <a href=\""+generalConfig.sHostCentral() + "/order/search?sID_Order=" + sID_Order+"\">\"Статуси\"</a>. Також на кожному етапі Ви будете отримувати email-повідомлення.	";

            oMail.reset();

            oMail._To(sMailTo)._Head(sHead)._Body(sBody);

            oMail.send();
            LOG.info("Send email with sID_Order={} to the sMailTo={}", sID_Order, sMailTo);
        }catch(Exception oException){
            LOG.warn("Refused: {} (sMailTo={},sID_Order={})", oException.getMessage(), sMailTo, sID_Order);
            LOG.error("FAIL:", oException);
        }
    }
    
    public void sendVerifyEmail(String sMailTo, String sToken) throws EmailException {

        try{
            //String sMailTo = sQuestion;
            String sHead = "Верификация адреса";
            String sBody = "Код подтверждения: " + sToken;
            oMail.reset();
            oMail._To(sMailTo)
                 ._Head(sHead)
                 ._Body(sBody);
            oMail.send();
            LOG.info("Send email with sToken={} to the sMailTo={}", sToken, sMailTo);
        }catch(Exception oException){
            LOG.warn("FAIL: {} (sMailTo={},sToken={})", oException.getMessage(), sMailTo, sToken);
            throw oException;
        }
    
    }    
    
            //oNotificationPatterns.sendTaskEmployeeQuestionEmail(nID_Process, saField, sBody, sToken);
            
    //nID_Process, saField, sBody, sToken
    //String sHead, String sBody, String sMailTo
    public void sendTaskEmployeeQuestionEmail(String sHead, String sCommentary, String sMailTo, String sToken, Long nID_Process, String saField, String soParams)
            throws EmailException {
        try{
            sHead = sHead == null ? "Просимо уточнити дані по Вашій заявці на iGov" : sHead;
            String sClientFIO = null;
            String sEmployerFIO = null;
            try{
                if(soParams!=null&&!"".equals(soParams.trim())){
                    JSONObject oParams = new JSONObject(soParams);
                    //Object sName=oPatams.opt("sName");
                    //String sEmployerFIO = oPatams.getString("sEmployerFIO");
                    if(oParams.has("sEmployerFIO")){
                        sEmployerFIO = oParams.getString("sEmployerFIO");
                    }
                    if(oParams.has("sClientFIO")){
                        sClientFIO = oParams.getString("sClientFIO");
                    }
                    
                    //JSONArray aField = oFields.getJSONArray("soData");
                    /*for (int i = 0; i < aField.length(); i++) {
                        JSONObject oField = aField.getJSONObject(i);
                    StringBuilder osTable = new StringBuilder();*/
                }
            }catch(Exception oException){
                LOG.warn("FAIL: {} (soParams={})", oException.getMessage(), soParams);
            }
            
            if(sClientFIO==null){
                sClientFIO = "громадянин";
            }
            String sURL = (new StringBuilder(generalConfig.sHostCentral()).append("/order/search?sID_Order=")
                    .append(generalConfig.sID_Order_ByProcess(nID_Process))
                    .append("&sToken=").append(sToken)).toString();
            
            String sText = "<b>Шановний (-а) "+sClientFIO+"!</b><br>"
            +"Державний службовець, що обробляє Вашу заявку "+(sEmployerFIO!=null?"("+sEmployerFIO+")":"")+", просить уточнити Вас дані.<br>"
            +"Коментар службовця: "+sCommentary + "<br>"
            +"<br>"
            + "Щоб уточнити ці дані, перейдіть, будь ласка, за <a href=\""+sURL+"\">цим посиланням</a> та заповніть поле.<br>"
            ;
            
            StringBuilder osBody = new StringBuilder(sText);
            osBody.append("<br/>").append(createTable_TaskProperties(saField, false)).append("<br/>");
            //osBody.append("Для уточнення - перейдіть по цьому посіланню: ").append(sURL).append("<br/>");
            
            String sBody = osBody.toString();
            oMail.reset();
            oMail._To(sMailTo)._Head(sHead)._Body(sBody);
            oMail.send();
        }catch(Exception oException){
            LOG.warn("FAIL: {} (sMailTo={},sToken={},nID_Process={},saField={})", oException.getMessage(), sMailTo, sToken,nID_Process,saField);
            throw oException;
        }
    }

    /*
    public void sendEmail(String sHead, String sBody, String recipient) throws EmailException {
        oMail.reset();
        oMail._To(recipient)._Head(sHead)._Body(sBody);
        oMail.send();
    }
    public String createEmailBody(Long nID_Process, String soData, String sBody, String sToken) throws UnsupportedEncodingException {
        StringBuilder emailBody = new StringBuilder(sBody);
        emailBody.append("<br/>").append(createTable_TaskProperties(soData)).append("<br/>");
        String link = (new StringBuilder(generalConfig.sHostCentral()).append("/order/search?sID_Order=")
                .append(generalConfig.sID_Order_ByProcess(nID_Process))
                .append("&sToken=").append(sToken)).toString();
        emailBody.append(link).append("<br/>");
        return emailBody.toString();
    }*/
    
}
