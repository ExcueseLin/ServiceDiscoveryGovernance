package cn.sunrain.SDG.client.http;

public class SDGHttpException extends RuntimeException{
    public SDGHttpException(String message) {
        super(message);
    }

    public SDGHttpException(String message, Throwable cause) {
        super(message, cause);
    }
}
