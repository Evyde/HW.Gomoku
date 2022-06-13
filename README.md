# 五子棋

[![Gobang CI](https://github.com/Evyde/hw.gobang/actions/workflows/GobangCI.yml/badge.svg)](https://github.com/Evyde/hw.gobang/actions/workflows/GobangCI.yml)


对了，我又来写五子棋了（

打算写一个前后端分离的五子棋游戏。

是JLU的Java大作业。

项目使用MVC架构。

项目基本完成。

## 现阶段截图

![五子棋界面截图/夜间模式](medias/ScreenShot1001.png)

![五子棋界面截图/夜间获胜](medias/ScreenShot1002.png)

![五子棋界面截图/日间获胜](medias/ScreenShot1003.png)

![控制台截图](medias/ScreenShot0905.png)


https://user-images.githubusercontent.com/9302540/172889849-01f7e236-21ac-4a06-bfe1-85e3eea3e08f.mp4

https://user-images.githubusercontent.com/9302540/173047555-e5f513aa-c91e-4c44-b817-ac232c8b0c26.mp4



https://user-images.githubusercontent.com/9302540/173399591-3b6a5ad3-0abc-491c-b83d-f17119320c6e.mp4




## 功能

拟实现以下功能，具体实现哪些视情况而定：

- [X] 下棋
- [X] 设置
- [ ] AI
- [ ] 登录
- [X] 对战
- [ ] 动画（算是实现了个鼠标移动的动画）
- [X] 联机

- [X] i18n
- [X] 日志
- [X] 日间/夜间模式切换

## 客户端

客户端有两个阶段：

1. Java Native GUI体验阶段
2. Web前端阶段

总体来说，先用Java写一个基本的客户端出来，看时间再进行Web端的移植。

客户端和逻辑服务端使用Socket/WebSocket通信（方便后续移植）。

客户端架构分为三个部分，皆可独立启动：
1. Message Queue Server消息服务器，用于鉴权和推送消息
2. Logic Server逻辑服务器，用于判断UI操作是否合法，并执行游戏主逻辑
3. UI Server(UI Driver)图形界面服务，用于解析服务器传来的消息并对UI进行操作
4. UI Client图形界面客户端，用来处理用户点击事件、绘制棋子等

理论上，只要实现UI Driver和UI Client，便可与其他两个服务无缝衔接，示例见上述视频，UI更改自
[@MerlynAllen](https://github.com/MerlynAllen)的[gomoku-vue](https://github.com/MerlynAllen/gomoku-vue)，只对我的协议进行了适配，
可以看到已经取得了较为不错的效果。且适配工作十分简单。

通信暂时采用WebSocket，可能不会迁移到Socket了。

关于协议的描述，详见[Model/MQProtocol.java](Client/src/main/java/jlu/evyde/gobang/Client/Model/MQProtocol.java)和
[Model/MQMessage.java](Client/src/main/java/jlu/evyde/gobang/Client/Model/MQMessage.java)。

### 消息队列服务器

#### 协议

详见`MQProtocol.java`。

消息使用类似：

```
PRODUCE
{"status": "SUCCESS", "code": 101}
END
```

的形式传送，第一行是传输头，第二行是数据，第三行是消息结束标志，目前主要使用WebSocket进行内容传送，所以一般不识别消息结束标志（不存在粘包）。

##### 传输头

在传输的内容中，消息头分为以下几大类：

- Produce（消息生产）

- Register（订阅消息服务器）

- Auth（逻辑服务器鉴权）

- Consume（消息消费（弃用，改为主动消息推送））

- End（消息尾，暂无实际作用）


消息服务器收到请求头之后，会对不同的消息执行不同的动作（详见`WebSocketMQServer.java`）。对于`Produce`类的消息，如果通过鉴权，则推送给消息来源组对应的目标组中的所有消费者。对于`Register`类的消息，如果是注册逻辑服务器的，则单独判断，否则转发到来源组对应的目标组（一般是逻辑服务器，因为MACHINE权限的客户端的消息只会被推送到逻辑服务器，而GUEST/USER权限不允许发送消息）。

##### 消息体

详见`MQMessage.java`。

一般常用的字段是`group`、`token`、`chess`和`code`，分别代表分组、临时随机密钥、棋子和操作码。

使用`Gson`库进行序列化和反序列化。

#### 权限

详见`MQProtocol.java`。

每个操作均有权限规定，而每个组都设置了权限，对操作进行了分级。例如客户端不能进行胜利的操作，观战者不能进行下棋的操作。

权限分为`GUEST`、`USER`、`MACHINE`、`SUPERVISOR`，和`RSIC-V`的权限分级类似，`GUEST`不具有任何权限，仅方便`MQServer`区分使用，`USER`只具有聊天的权限，一般是观战者组使用，`MACHINE`具有下棋、撤回、清除分数、重启和结束游戏的权限，一般将该权限分配给`GAMER`组。`SUPERVISOR`组具有所有操作的权限。高等级的用户可以执行低等级权限的所有操作。

#### 组

详见`MQProtocol.java`。

组分为访客、观战者、玩家和逻辑服务器，权限依次升高。每组都有容量限制，访客为5，观战者不限，玩家为2，逻辑服务器为1。每个组都有初始密钥，注册时需要发给消息服务器换取新的临时密钥。这些都可通过配置进行更改。

### 逻辑服务器

逻辑服务器负责处理客户端注册批准、游戏主逻辑等，详见`LogicServer.java`。

比如下棋，逻辑服务器在收到消息服务器推送的`code`为`PUT_CHESS`（101）的指令后，检查棋子是否合法，如果合法，则将密钥、组替换后，转发给消息服务器，让其转发给客户端，完成下棋指令。如果胜利，则不发送下棋指令，发送胜利的指令。

### 底层通信

详见`WebSocketCommunicator.java`和`WebSocketMQServer.java`。

采用WebSocket进行通信，但预留了接口`Communicator.java`、`CommunicatorFactory.java`、`MQBrokerServer.java`、`MQBrokerServerFactory.java`，随时可根据这些接口移植到`Socket`或其他传输层实现上。

## 服务端

暂时没有服务端的想法，有也就是负责登录、传递对手的棋路、判赢、大厅聊天之类，要加的东西有很多。

简单的可以只加一个联机功能，复杂一些带登录、大厅，再复杂就可以加数据库了。

## 许可协议

本项目采用AGPLv3协议。
