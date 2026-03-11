package com.example.demo.service;

import com.example.demo.dto.TaskResultDTO;

/**
 * @author eddy
 */
public interface ResultSenderService {

    /**
     * 发送测试结果
     *
     * @param result 测试结果
     */
    void sendResult(TaskResultDTO result);

}
