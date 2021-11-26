package com.dnainvest.client;

import com.dnainvest.handlers.DefaultHandler;
import com.dnainvest.handlers.ErrorHandler;
import com.dnainvest.misc.PropertiesHandler;
import it.tdlight.common.Init;
import it.tdlight.common.ResultHandler;
import it.tdlight.common.TelegramClient;
import it.tdlight.common.utils.CantLoadLibrary;
import it.tdlight.jni.TdApi;
import it.tdlight.tdlight.ClientManager;

import java.io.IOError;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CreateClient {

    private static TelegramClient client = null;
    private static final Lock authorizationLock = new ReentrantLock();
    private static final Condition gotAuthorization = authorizationLock.newCondition();
    private static volatile boolean havaAuthorization = false;


    public static TelegramClient getClient() {
        try {
            Init.start();
        } catch (CantLoadLibrary cantLoadLibrary) {
            cantLoadLibrary.printStackTrace();
        }

        client = ClientManager.create();
        // client.initialize(new RegHandler(), new ErrorHandler(), new ErrorHandler());
        client.initialize(CreateClient::onUpdate, CreateClient::onUpdateError, CreateClient::onError);
        //TdApi.Object objectx = DefaultHandler.result(client.execute(new TdApi.GetChats));
        client.execute(new TdApi.SetLogVerbosityLevel(0));
        //disable TDlib log
        if (client.execute(new TdApi.SetLogStream(new TdApi.LogStreamFile("tdlib.log", 1 << 27, false))) instanceof TdApi.Error) {
            throw new IOError(new IOException("write access to the current directory is required"));
        }

        //test Client.execute
        TdApi.Object object = DefaultHandler.result(client.execute(new TdApi.GetTextEntities("@telegram /test_command https://telegram.org telegram.me @gif @test")));
        System.out.println(object.toString());

        //await authoriztion
        authorizationLock.lock();
        try {
            while (!havaAuthorization) {
                try {
                    gotAuthorization.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } finally {
            authorizationLock.unlock();
        }

        //if authorized
        if (havaAuthorization)
            return client;
        else
            return null;

    }

    private static class RegHandler implements ResultHandler {
        @Override
        public void onResult(TdApi.Object object) {
            if (object.getConstructor() == TdApi.UpdateAuthorizationState.CONSTRUCTOR) {
                onAuthorizationStateUpdated(((TdApi.UpdateAuthorizationState) object).authorizationState);
            }
        }

        private static void onAuthorizationStateUpdated(TdApi.AuthorizationState authorizationState) {
            PropertiesHandler propertiesHandler = new PropertiesHandler();

            switch (authorizationState.getConstructor()) {
                case TdApi.AuthorizationStateWaitTdlibParameters.CONSTRUCTOR: {
                    TdApi.TdlibParameters parameters = new TdApi.TdlibParameters();
                    parameters.databaseDirectory = "tdlib";
                    parameters.useMessageDatabase = true;
                    parameters.useSecretChats = true;
                    parameters.apiId = propertiesHandler.getProperty("apiId", 0);
                    parameters.apiHash = propertiesHandler.getProperty("apiHash");
                    parameters.systemLanguageCode = propertiesHandler.getProperty("systemLanguageCode");
                    parameters.deviceModel = "Desktop";
                    parameters.applicationVersion = "1.0";
                    parameters.enableStorageOptimizer = true;
                    client.send(new TdApi.SetTdlibParameters(parameters), new AuthorizationRequstHandler());
                }

                case TdApi.AuthorizationStateWaitEncryptionKey.CONSTRUCTOR:
                    client.send(new TdApi.CheckDatabaseEncryptionKey(), new AuthorizationRequstHandler());
                case TdApi.AuthorizationStateWaitPhoneNumber.CONSTRUCTOR: {
                    String phoneNumber = getString("Please enter phone number ");
                    client.send(new TdApi.SetAuthenticationPhoneNumber(phoneNumber, null), new AuthorizationRequstHandler());
                    break;
                }
                case TdApi.AuthorizationStateWaitOtherDeviceConfirmation.CONSTRUCTOR: {
                    String link = ((TdApi.AuthorizationStateWaitOtherDeviceConfirmation) authorizationState).link;
                    System.out.println("Please confirm this login link on anther device: " + link);
                    break;
                }

                case TdApi.AuthorizationStateWaitCode.CONSTRUCTOR: {
                    String code = getString("Please enter authentication code: ");
                    client.send(new TdApi.CheckAuthenticationCode(code), new AuthorizationRequstHandler());
                    break;
                }
                case TdApi.AuthorizationStateWaitRegistration.CONSTRUCTOR: {
                    String firstName = getString("Please enter first name: ");
                    String lastName = getString("Please enter Last name ");
                    client.send(new TdApi.RegisterUser(firstName, lastName), new AuthorizationRequstHandler());
                    break;
                }

                case TdApi.AuthorizationStateWaitPassword.CONSTRUCTOR: {
                    String password = getString("Please enter password");
                    client.send(new TdApi.CheckAuthenticationPassword(password), new AuthorizationRequstHandler());
                    break;
                }
                case TdApi.AuthorizationStateReady.CONSTRUCTOR: {
                    havaAuthorization = true;
                    authorizationLock.lock();
                    try {
                        gotAuthorization.signal();
                    } finally {
                        authorizationLock.unlock();
                    }
                    break;
                }
                default:
                    System.out.println("Unsupported Authorization State " + authorizationState);
            }

        }

        private static String getString(String str) {
            String consoleString = null;
            Scanner sc;
            do {
                System.out.println(str);
                sc = new Scanner(System.in);
                consoleString = sc.nextLine();
                consoleString = consoleString.trim();
                if (consoleString.length() < 1) {
                    consoleString = null;
                    continue;
                } else
                    break;
            } while (consoleString == null);
            return consoleString;
        }
    }

    private static class AuthorizationRequstHandler implements ResultHandler {
        @Override
        public void onResult(TdApi.Object object) {
            switch (object.getConstructor()) {
                case TdApi.Error.CONSTRUCTOR:
                    System.err.println("Receive an error: " + object);
                    RegHandler.onAuthorizationStateUpdated(null);// repeat last action
                    break;

                case TdApi.Ok.CONSTRUCTOR:
                    //Result is already receieved through updateAuthorizationState , nothing to do
                    break;

                default:
                    System.err.println("Recieve wrong response from TDLib:" + object);
            }
        }
    }

    private static void onUpdate(TdApi.Object object) {
        TdApi.Update update = (TdApi.Update) object;
        System.out.println("Received update: " + update);
    }

    private static void onUpdateError(Throwable exception) {
        System.out.println("Received an error from updates:");
        exception.printStackTrace();
    }

    private static void onError(Throwable exception) {
        System.out.println("Received an error:");
        exception.printStackTrace();
    }

}
