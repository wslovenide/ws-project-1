package com.ws.oms.chat.netty.service;

import com.alibaba.fastjson.JSON;
import com.ws.oms.chat.netty.handler.dto.*;
import com.ws.oms.chat.netty.service.api.IChatMsgService;
import com.ws.oms.chat.netty.util.Constant;
import com.ws.oms.chat.netty.util.ZhuanYiCharUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.apache.commons.lang3.StringUtils;
import java.util.List;
import java.util.UUID;

/**
 * Description:
 *
 * @author sheng.wang
 * @version 1.0.0
 * @email sheng.wang@chinaredstar.com
 * @date: 2018-03-09 15:25
 */
public class ChatMsgService implements IChatMsgService {

    private ServiceContext serviceContext;

    public ChatMsgService(ServiceContext serviceContext){
        this.serviceContext = serviceContext;
    }

    @Override
    public void handleMessage(ChannelHandlerContext ctx, String msg) {
        BaseReq baseReq = JSON.parseObject(msg, BaseReq.class);
        switch (baseReq.getCommand()){
            case Constant.WEB_SOCKET_INIT:
                handleWebSocketInit(ctx,baseReq);
                break;

            case Constant.SEND_MESSAGE:
                handleSendMessage(ctx,msg);
                break;

            case Constant.QUERY_CHAT_HISTORY:

                break;
            default:
                ChatMsgResp resp = new ChatMsgResp();
                resp.setSuccess(false);
                resp.setSessionId(baseReq.getSessionId());
                resp.setCommand(baseReq.getCommand());
                ctx.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(resp)));
                break;
        }
    }

    private void handleWebSocketInit(ChannelHandlerContext ctx, BaseReq baseReq){
        if (StringUtils.isBlank(baseReq.getSessionId())){
            baseReq.setSessionId(UUID.randomUUID().toString());
        }
        serviceContext.attach(ctx.channel(),baseReq.getSessionId());

        // 默认查询公共房间的聊天记录
        List<ChatMsgItemResp> chatMsgList = serviceContext.getChatMsgByGroup(Constant.PUBLIC_GROUP_ID,baseReq.getSessionId());

        ChatMsgResp resp = new ChatMsgResp();
        resp.setMsg(chatMsgList);
        resp.setSessionId(baseReq.getSessionId());
        resp.setCommand(Constant.WEB_SOCKET_INIT);
        resp.setCount(serviceContext.getOnlineNumber(Constant.PUBLIC_GROUP_ID));

        ctx.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(resp)));
    }


    private void handleSendMessage(ChannelHandlerContext ctx, String msg){
        SendMessageReq msgReq = JSON.parseObject(msg, SendMessageReq.class);

        // 获取 channel 对应的 sessionId , 分发消息
        String sessionId = serviceContext.getSessionId(ctx.channel());

        String groupId = msgReq.getGroupId();
        groupId = StringUtils.isBlank(groupId) ? Constant.PUBLIC_GROUP_ID : groupId;

        ChatMsgResp resp = new ChatMsgResp();
        resp.setCommand(Constant.SEND_MESSAGE_RESP);
        resp.setSessionId(sessionId);

        ChatMsgItemResp item = new ChatMsgItemResp();
        item.setSessionId(sessionId);
        item.setGroupId(groupId);
        item.setNickName(sessionId);
        item.setMsg(ZhuanYiCharUtil.zhuanYi(msgReq.getMsg()));

        resp.setMsg(item);

        serviceContext.broadcastMessage(sessionId,groupId,resp);
        serviceContext.save(item);
    }

}
