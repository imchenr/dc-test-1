package com.example.demo.core;

import com.alibaba.fastjson2.JSON;
import com.example.demo.dto.ReportDTO;
import com.example.demo.dto.StressSampleResultDTO;
import com.example.demo.dto.ApiCaseDTO;
import com.example.demo.service.ResultSenderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.jmeter.assertions.AssertionResult;
import org.apache.jmeter.reporters.ResultCollector;
import org.apache.jmeter.reporters.Summariser;
import org.apache.jmeter.samplers.SampleEvent;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.visualizers.SamplingStatCalculator;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author eddy
 */
@Slf4j
public class EngineSampleCollector extends ResultCollector {

    private final Map<String, SamplingStatCalculator> calculatorMap = new HashMap<>();
    private ResultSenderService resultSenderService;
    private ReportDTO reportDTO;
    private ApiCaseDTO apiCaseDTO;

    public EngineSampleCollector() {
        super();
    }

    public EngineSampleCollector(ApiCaseDTO apiCaseDTO, Summariser summariser, ResultSenderService resultSenderService, ReportDTO reportDTO) {
        super(summariser);
        this.apiCaseDTO = apiCaseDTO;
        this.resultSenderService = resultSenderService;
        this.reportDTO = reportDTO;
    }

    @Override
    public void sampleOccurred(SampleEvent event) {
        super.sampleOccurred(event);
        SampleResult result = event.getResult();
        String sampleLabel = result.getSampleLabel();

        //针对不同的请求，实例化不同的计算器，存储到map，才可以区分不同的请求
        SamplingStatCalculator calculator = calculatorMap.get(sampleLabel);
        if (calculator == null) {
            calculator = new SamplingStatCalculator(result.getSampleLabel());
            calculator.addSample(result);
            calculatorMap.put(sampleLabel, calculator);
        } else {
            //如果计算器存在，就添加更新采样器结果
            calculator.addSample(result);
        }

        //封装采样器结果数据
        StressSampleResultDTO sampleResultInfoDTO = new StressSampleResultDTO();
        //测试报告id
        sampleResultInfoDTO.setReportId(reportDTO.getId());
        // 设置时间戳
        sampleResultInfoDTO.setSampleTime(result.getTimeStamp());
        // 设置请求标签
        sampleResultInfoDTO.setSamplerLabel(result.getSampleLabel());
        // 设置样本计数
        sampleResultInfoDTO.setSamplerCount(calculator.getCount());
        // 设置平均时间
        sampleResultInfoDTO.setMeanTime(calculator.getMean());
        // 设置最小时间
        sampleResultInfoDTO.setMinTime(calculator.getMin().intValue());
        // 设置最大时间
        sampleResultInfoDTO.setMaxTime(calculator.getMax().intValue());

        // 设置错误百分比
        sampleResultInfoDTO.setErrorPercentage(calculator.getErrorPercentage());
        // 设置错误计数
        sampleResultInfoDTO.setErrorCount(calculator.getErrorCount());
        // 设置请求速率
        sampleResultInfoDTO.setRequestRate(calculator.getRate());
        // 设置接收数据大小
        sampleResultInfoDTO.setReceiveKBPerSecond(calculator.getKBPerSecond());
        // 设置发送数据大小
        sampleResultInfoDTO.setSentKBPerSecond(calculator.getSentKBPerSecond());


        //设置请求路径参数
        sampleResultInfoDTO.setRequestLocation(event.getResult().getUrlAsString());
        // 设置请求头
        sampleResultInfoDTO.setRequestHeader(event.getResult().getRequestHeaders());
        // 设置请求体
        sampleResultInfoDTO.setRequestBody(event.getResult().getSamplerData());
        // 设置响应码
        sampleResultInfoDTO.setResponseCode(event.getResult().getResponseCode());
        // 设置响应头
        sampleResultInfoDTO.setResponseHeader(event.getResult().getResponseHeaders());
        // 设置响应数据
        sampleResultInfoDTO.setResponseData(event.getResult().getResponseDataAsString());

        AssertionResult[] assertionResults = event.getResult().getAssertionResults();
        StringBuilder assertMsg = new StringBuilder();
        if (Objects.nonNull(assertionResults)) {
            for (AssertionResult assertionResult : assertionResults) {
                assertMsg.append("name=").append(assertionResult.getName())
                        .append(",msg=").append(assertionResult.getFailureMessage()).append(",");
            }
        }
        sampleResultInfoDTO.setAssertInfo(assertMsg.toString());
        //序列化为json对象
        String sampleResultInfoJson = JSON.toJSONString(sampleResultInfoDTO);
        log.error(sampleResultInfoJson);
        // TODO 发送测试结果
//        CaseInfoDTO caseInfoDTO = new CaseInfoDTO(apiCaseDTO.getId(), apiCaseDTO.getModuleId(), apiCaseDTO.getName());
//        resultSenderService.sendResult(caseInfoDTO, sampleResultInfoJson);

    }
}
