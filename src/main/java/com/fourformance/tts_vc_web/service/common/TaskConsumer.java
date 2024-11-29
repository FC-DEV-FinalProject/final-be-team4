package com.fourformance.tts_vc_web.service.common;

import com.fourformance.tts_vc_web.common.config.TaskConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class TaskConsumer {

    @RabbitListener(queues = TaskConfig.TTS_QUEUE)
    public void handleTTSTask(String message) {
        System.out.println("TTS audio task : " + message);

        // 작업 상태 update


        // TTS 생성 로직
        try{


        }catch(Exception e){
            // 실패 상태로 업데이트
            System.out.println("TTS 처리 실패"+e);
        }
    }

    @RabbitListener(queues = TaskConfig.VC_QUEUE)
    public void handleVCTask(String message) {
        System.out.println("VC audio task : " + message);
        // 처리 로직 구현
        try{

        }catch(Exception e){
            // 실패 상태로 업데이트
            System.out.println("TTS 처리 실패"+e);
        }
    }

    @RabbitListener(queues = TaskConfig.CONCAT_QUEUE)
    public void handleConcatTask(String message) {
        System.out.println("Concat audio task : " + message);
        // 처리 로직 구현
        // TTS 생성 로직 구현
        try{

        }catch(Exception e){
            // 실패 상태로 업데이트
            System.out.println("TTS 처리 실패"+e);
        }
    }
}
