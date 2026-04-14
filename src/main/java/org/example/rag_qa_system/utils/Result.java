package org.example.rag_qa_system.utils;

import lombok.Data;

/**
 * 统一返回结果类
 */
@Data
public class Result {

    private int code;
    private String message;
    private Object data;

    public Result() {
    }

    public Result(int code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static Result success(Object data) {
        return new Result(200, "success", data);
    }

    public static Result success(String message) {
        return new Result(200, message, null);
    }

    public static Result error(String message) {
        return new Result(500, message, null);
    }
}