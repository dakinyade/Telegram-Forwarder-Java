package com.dnainvest.handlers;

import it.tdlight.common.ResultHandler;
import it.tdlight.jni.TdApi;

public class DefaultHandler implements ResultHandler {

    public static TdApi.Object result(TdApi.Object object) {
        return object;
    }

    @Override
    public void onResult(TdApi.Object object) {
        System.out.println(object.toString());
    }
}
