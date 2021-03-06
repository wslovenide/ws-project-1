// var serverUrl = "47.75.15.228:8888";
var serverUrl = "localhost:8888";
var webSocketUrl = serverUrl + "/websocket/chat";
var ws;
function initWebsocket() {
    if (window.WebSocket){
        ws = new WebSocket("ws:" + webSocketUrl);
        ws.onopen = function (event) {
            var item = localStorage.getItem("sessionId");
            var msg = {msgType:"3",sessionId:item || ''};
            ws.send(JSON.stringify(msg));
        };
        ws.onmessage = function (event) {
            var jsonMsg = JSON.parse(event.data);
            if (jsonMsg.msgType == "1"){
                chatMessage(jsonMsg);
            }else if (jsonMsg.msgType == "3"){
                initSessionId(jsonMsg);
            }else {
                onlineOfflineNotifyMessage(jsonMsg);
            }
        };
        ws.onclose = function (event) {
            alert("连接已断开,确认后重新连接!");
        };
    }else {
        alert("您的浏览器不支持websocket,请升级!");
    }
}

function chatMessage(jsonMsg) {
    var label = jsonMsg.self ? "rightMessageLabel" : "leftMessageLabel";

    var chatData = "<div class='"+label+"'>";
    chatData += "<span class='msgClass'>" + jsonMsg.msg + "</span><br/>";
    chatData += "<span class='nickNameClass'>" + jsonMsg.nickName + "</span>";
    chatData += "<span class='dateTimeClass'>" + jsonMsg.dateTime + "</span>";
    chatData += "</div>";

    var messageContent =  document.getElementById('messageContent');
    messageContent.innerHTML +=  chatData;

    messageContent.scrollTop = messageContent.scrollHeight;
}

function onlineOfflineNotifyMessage(jsonMsg) {
    $("#titleText").text("聊天室(在线" + jsonMsg.msg + ")");
}

function initSessionId(jsonMsg) {
    if (jsonMsg.msg && jsonMsg.msg.length > 0){
        jsonMsg.msg.forEach(chatMessage);
    }
    localStorage.setItem("sessionId",jsonMsg.sessionId);
}

function sendMessage() {
    if (ws){
        var messageObj = $("#message");
        if (messageObj.val() == ''){
            return;
        }
        var msg = {msg:messageObj.val(),msgType:"1"};
        ws.send(JSON.stringify(msg));
        messageObj.val('');
    }
}
document.onkeydown = function(event){
    var e = event || window.event;
    if(e && e.keyCode == 13){
        sendMessage();
    }
};
initWebsocket();