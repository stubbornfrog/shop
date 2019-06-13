package com.macro.mall.portal.controller;


import com.macro.mall.dto.OmsOrderDetail;
import com.macro.mall.dto.OmsOrderQueryParam;
import com.macro.mall.model.OmsOrder;
import com.macro.mall.model.OmsOrderItem;
import com.macro.mall.model.UmsMember;
import com.macro.mall.portal.config.WxAppletProperties;
import com.macro.mall.portal.service.OmsOrderService;
import com.macro.mall.portal.service.OmsPortalOrderService;
import com.macro.mall.portal.service.UmsMemberService;
import com.macro.mall.portal.util.*;
import com.macro.mall.portal.util.applet.WechatRefundApiResult;
import com.macro.mall.portal.util.applet.WechatUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 作者: @author Harmon <br>
 * 时间: 2017-08-11 08:32<br>
 * 描述: ApiIndexController <br>
 */
@Api(tags = "商户支付")
@RestController
@RequestMapping("/api/pay")
public class PayController extends ApiBaseAction {
    private static Logger LOGGER = LoggerFactory.getLogger(PayController.class);

    @Autowired
    private OmsPortalOrderService portalOrderService;
    @Autowired
    private OmsOrderService orderService;
    @Autowired
    private UmsMemberService umsMemberService;
    @Autowired
    private WxAppletProperties wxAppletProperties;
    /**
     */
    @ApiOperation(value = "跳转")
    @PostMapping("index")
    public Object index() {
        //
        return toResponsSuccess("");
    }

    /**
     * 获取支付的请求参数
     */
    @ApiOperation(value = "获取支付的请求参数")
    @GetMapping("prepay")
    public Object payPrepay( @RequestParam(value = "id", required = false, defaultValue = "0") Long id) {
        UmsMember user = umsMemberService.getCurrentMember();
        //
        OmsOrderDetail orderInfo = orderService.detail(id);

        if (null == orderInfo) {
            return toResponsObject(400, "订单已取消", "");
        }

        if (orderInfo.getStatus() != 0) {
            return toResponsObject(400, "订单已支付，请不要重复操作", "");
        }

        String nonceStr = CharUtil.getRandomString(32);

        //https://pay.weixin.qq.com/wiki/doc/api/wxa/wxa_api.php?chapter=7_7&index=3
        Map<Object, Object> resultObj = new TreeMap();

        try {
            Map<Object, Object> parame = new TreeMap<Object, Object>();
            parame.put("appid", wxAppletProperties.getAppId());
            // 商家账号。
            parame.put("mch_id", wxAppletProperties.getMchId());
            String randomStr = CharUtil.getRandomNum(18).toUpperCase();
            // 随机字符串
            parame.put("nonce_str", randomStr);
            // 商户订单编号
            parame.put("out_trade_no", orderInfo.getOrderSn());

            // 商品描述
            parame.put("body", "超市-支付");
            //订单的商品
            List<OmsOrderItem> orderGoods = orderInfo.getOrderItemList();
            if (null != orderGoods) {
                String body = "超市-";
                for (OmsOrderItem goodsVo : orderGoods) {
                    body = body + goodsVo.getProductName() + "、";
                }
                if (body.length() > 0) {
                    body = body.substring(0, body.length() - 1);
                }
                // 商品描述
                parame.put("body", body);
            }
            //支付金额
            parame.put("total_fee", orderInfo.getPayAmount().multiply(new BigDecimal(100)).intValue());
            // 回调地址
            parame.put("notify_url", wxAppletProperties.getNotifyUrl());
            // 交易类型APP
            parame.put("trade_type", wxAppletProperties.getTradeType());
            parame.put("spbill_create_ip", getClientIp());
            parame.put("openid", user.getWeixinOpenid());
            String sign = WechatUtil.arraySign(parame, wxAppletProperties.getPaySignKey());
            // 数字签证
            parame.put("sign", sign);

            String xml = MapUtils.convertMap2Xml(parame);
            logger.info("xml:" + xml);
            Map<String, Object> resultUn = XmlUtil.xmlStrToMap(WechatUtil.requestOnce(wxAppletProperties.getUniformorder(), xml));
            // 响应报文
            String return_code = MapUtils.getString("return_code", resultUn);
            String return_msg = MapUtils.getString("return_msg", resultUn);
            //
            if (return_code.equalsIgnoreCase("FAIL")) {
                return toResponsFail("支付失败," + return_msg);
            } else if (return_code.equalsIgnoreCase("SUCCESS")) {
                // 返回数据
                String result_code = MapUtils.getString("result_code", resultUn);
                String err_code_des = MapUtils.getString("err_code_des", resultUn);
                if (result_code.equalsIgnoreCase("FAIL")) {
                    return toResponsFail("支付失败," + err_code_des);
                } else if (result_code.equalsIgnoreCase("SUCCESS")) {
                    String prepay_id = MapUtils.getString("prepay_id", resultUn);
                    // 先生成paySign 参考https://pay.weixin.qq.com/wiki/doc/api/wxa/wxa_api.php?chapter=7_7&index=5
                    resultObj.put("appId", wxAppletProperties.getAppId());
                    resultObj.put("timeStamp", DateUtils.timeToStr(System.currentTimeMillis() / 1000, DateUtils.DATE_TIME_PATTERN));
                    resultObj.put("nonceStr", nonceStr);
                    resultObj.put("package", "prepay_id=" + prepay_id);
                    resultObj.put("signType", "MD5");
                    String paySign = WechatUtil.arraySign(resultObj, wxAppletProperties.getPaySignKey());
                    resultObj.put("paySign", paySign);
                    // 业务处理
                    orderInfo.setPrepayId(prepay_id);
                    // 付款中
                    orderInfo.setStatus(1);
                    orderService.updateAll(orderInfo);
                    return toResponsObject(200, "微信统一订单下单成功", resultObj);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return toResponsFail("下单失败,error=" + e.getMessage());
        }
        return toResponsFail("下单失败");
    }

    /**
     * 微信查询订单状态
     */
    @ApiOperation(value = "查询订单状态")
    @GetMapping("query")
    public Object orderQuery( @RequestParam(value = "id", required = false, defaultValue = "0") Long id) {
        UmsMember user = umsMemberService.getCurrentMember();
        //
        OmsOrderDetail orderDetail = orderService.detail(id);
        if (id == null) {
            return toResponsFail("订单不存在");
        }
        Map<Object, Object> parame = new TreeMap<Object, Object>();
        parame.put("appid", wxAppletProperties.getAppId());
        // 商家账号。
        parame.put("mch_id", wxAppletProperties.getMchId());
        String randomStr = CharUtil.getRandomNum(18).toUpperCase();
        // 随机字符串
        parame.put("nonce_str", randomStr);
        // 商户订单编号
        parame.put("out_trade_no", orderDetail.getOrderSn());

        String sign = WechatUtil.arraySign(parame, wxAppletProperties.getPaySignKey());
        // 数字签证
        parame.put("sign", sign);

        String xml = MapUtils.convertMap2Xml(parame);
        logger.info("xml:" + xml);
        Map<String, Object> resultUn = null;
        try {
            resultUn = XmlUtil.xmlStrToMap(WechatUtil.requestOnce(wxAppletProperties.getOrderquery(), xml));
        } catch (Exception e) {
            e.printStackTrace();
            return toResponsFail("查询失败,error=" + e.getMessage());
        }
        // 响应报文
        String return_code = MapUtils.getString("return_code", resultUn);
        String return_msg = MapUtils.getString("return_msg", resultUn);

        if (!"SUCCESS".equals(return_code)) {
            return toResponsFail("查询失败,error=" + return_msg);
        }

        String trade_state = MapUtils.getString("trade_state", resultUn);
        if ("SUCCESS".equals(trade_state)) {
            // 更改订单状态
            // 业务处理
            OmsOrder orderInfo = new OmsOrder();
            orderInfo.setId(id);
            orderInfo.setStatus(2);
            orderInfo.setConfirmStatus(1);
            orderInfo.setPaymentTime(new Date());
            orderService.updateAll(orderInfo);
            return toResponsMsgSuccess("支付成功");
        } else if ("USERPAYING".equals(trade_state)) {
            // 重新查询 正在支付中
           /* Integer num = (Integer) J2CacheUtils.get(J2CacheUtils.SHOP_CACHE_NAME, "queryRepeatNum" + id + "");
            if (num == null) {
                J2CacheUtils.put(J2CacheUtils.SHOP_CACHE_NAME, "queryRepeatNum" + id + "", 1);
                this.orderQuery(id);
            } else if (num <= 3) {
                J2CacheUtils.remove(J2CacheUtils.SHOP_CACHE_NAME, "queryRepeatNum" + id);
                this.orderQuery(id);
            } else {
                return toResponsFail("查询失败,error=" + trade_state);
            }*/

        } else {
            // 失败
            return toResponsFail("查询失败,error=" + trade_state);
        }

        return toResponsFail("查询失败，未知错误");
    }

    /**
     * 微信订单回调接口
     *
     * @return
     */
    @ApiOperation(value = "微信订单回调接口")
    @RequestMapping(value = "/notify", method = RequestMethod.POST, produces = "text/html;charset=UTF-8")
    @ResponseBody
    public void notify(HttpServletRequest request, HttpServletResponse response) {
        try {
            request.setCharacterEncoding("UTF-8");
            response.setCharacterEncoding("UTF-8");
            response.setContentType("text/html;charset=UTF-8");
            response.setHeader("Access-Control-Allow-Origin", "*");
            InputStream in = request.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            out.close();
            in.close();
            //xml数据
            String reponseXml = new String(out.toByteArray(), "utf-8");

            WechatRefundApiResult result = (WechatRefundApiResult) XmlUtil.xmlStrToBean(reponseXml, WechatRefundApiResult.class);
            String result_code = result.getResult_code();
            if (result_code.equalsIgnoreCase("FAIL")) {
                //订单编号
                String out_trade_no = result.getOut_trade_no();
                logger.error("订单" + out_trade_no + "支付失败");
                response.getWriter().write(setXml("SUCCESS", "OK"));
            } else if (result_code.equalsIgnoreCase("SUCCESS")) {
                //订单编号
                String out_trade_no = result.getOut_trade_no();
                logger.error("订单" + out_trade_no + "支付成功");
                // 业务处理
                OmsOrderQueryParam param = new OmsOrderQueryParam();
                param.setOrderSn(out_trade_no);
                List<OmsOrder> list = orderService.list(param,1,1);
                OmsOrder orderInfo = list.get(0);
                orderInfo.setStatus(2);
                orderInfo.setPaymentTime(new Date());
                orderService.updateAll(orderInfo);
                response.getWriter().write(setXml("SUCCESS", "OK"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    /**
     * 订单退款请求
     */
    /*@ApiOperation(value = "订单退款请求")
    @PostMapping("refund")
    public Object refund(Integer orderId) {
        //
        OrderVo orderInfo = orderService.queryObject(orderId);

        if (null == orderInfo) {
            return toResponsObject(400, "订单已取消", "");
        }

        if (orderInfo.getOrder_status() == 401 || orderInfo.getOrder_status() == 402) {
            return toResponsObject(400, "订单已退款", "");
        }

        WechatRefundApiResult result = WechatUtil.wxRefund(orderInfo.getId().toString(),
                10.01, 10.01);
        if (result.getResult_code().equals("SUCCESS")) {
            if (orderInfo.getOrder_status() == 201) {
                orderInfo.setOrder_status(401);
            } else if (orderInfo.getOrder_status() == 300) {
                orderInfo.setOrder_status(402);
            }
            orderService.updateAll(orderInfo);
            return toResponsObject(400, "成功退款", "");
        } else {
            return toResponsObject(400, "退款失败", "");
        }
    }*/


    //返回微信服务
    public static String setXml(String return_code, String return_msg) {
        return "<xml><return_code><![CDATA[" + return_code + "]]></return_code><return_msg><![CDATA[" + return_msg + "]]></return_msg></xml>";
    }

    //模拟微信回调接口
    public static String callbakcXml(String orderNum) {
        return "<xml><appid><![CDATA[wx2421b1c4370ec43b]]></appid><attach><![CDATA[支付测试]]></attach><bank_type><![CDATA[CFT]]></bank_type><fee_type><![CDATA[CNY]]></fee_type> <is_subscribe><![CDATA[Y]]></is_subscribe><mch_id><![CDATA[10000100]]></mch_id><nonce_str><![CDATA[5d2b6c2a8db53831f7eda20af46e531c]]></nonce_str><openid><![CDATA[oUpF8uMEb4qRXf22hE3X68TekukE]]></openid> <out_trade_no><![CDATA[" + orderNum + "]]></out_trade_no>  <result_code><![CDATA[SUCCESS]]></result_code> <return_code><![CDATA[SUCCESS]]></return_code><sign><![CDATA[B552ED6B279343CB493C5DD0D78AB241]]></sign><sub_mch_id><![CDATA[10000100]]></sub_mch_id> <time_end><![CDATA[20140903131540]]></time_end><total_fee>1</total_fee><trade_type><![CDATA[JSAPI]]></trade_type><transaction_id><![CDATA[1004400740201409030005092168]]></transaction_id></xml>";
    }
}