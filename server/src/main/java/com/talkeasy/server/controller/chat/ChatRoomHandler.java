package com.talkeasy.server.controller.chat;

import com.google.gson.Gson;
import com.talkeasy.server.domain.chat.ChatRoomDetail;
import com.talkeasy.server.dto.chat.ChatReadDto;
import com.talkeasy.server.dto.chat.MakeChatRoomDto;
import com.talkeasy.server.service.chat.ChatReadService;
import com.talkeasy.server.service.chat.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;


@Slf4j
@RequiredArgsConstructor
@Component
public class ChatRoomHandler {

    private final ChatService chatService;
    private final ChatReadService chatReadService;

    //서버로 들어오는 채팅값
    @RabbitListener(queues = "chat.queue")
    public void chatControl(Message message) {
        // Json : String -> Object : ChatDto

        log.info(" message : {}", message);
        System.out.println("hihihihhihi");
        ChatRoomDetail chat = chatService.convertChat(message);
        if(chat.getRoomId().equals("0")) {
            return;
        }
//        //비즈니스 로직 (mongodb 저장, 채팅 보내기)
        String roomId = chatService.saveChat(chat);
        chat.setRoomId(roomId);

        chatService.doChat(chat, message);

    }


    @RabbitListener(queues = "read.queue")
    public void readMessage(Message message) {

        log.info(" read : {}", message);
        System.out.println("readread");
        ChatReadDto chat = chatReadService.convertChat(message);
        if(chat.getRoomId().equals("0")) {
            return;
        }

        chatReadService.readMessage(chat);

    }

    @RabbitListener(queues = "room.queue")
    public void makeRoom(Message message) {
        log.info("room message : {}", message);

        String str = new String(message.getBody());
        MakeChatRoomDto dto = new Gson().fromJson(str, MakeChatRoomDto.class);
        chatService.createRoom(dto.getUser1(), dto.getUser2());
    }

}