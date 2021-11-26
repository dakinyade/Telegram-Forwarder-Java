package com.dnainvest.main;

import com.dnainvest.client.CreateClient;
import com.dnainvest.handlers.DefaultHandler;
import com.dnainvest.handlers.ErrorHandler;
import it.tdlight.common.TelegramClient;
import it.tdlight.jni.TdApi;

public class Main {
    private static TelegramClient client;

    public static void main(String[] args) {
        client = CreateClient.getClient();
        client.send(new TdApi.SetName("..", "11"), new DefaultHandler(), new ErrorHandler());
        for (int i = 0; i < 10; i++) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.out.println(i+1);
        }
    }
}
