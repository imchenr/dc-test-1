package com.example.demo.core;

import cn.hutool.core.collection.CollectionUtil;
import com.example.demo.dto.*;
import com.example.demo.dto.EnvironmentDTO;
import com.example.demo.dto.ApiCaseDTO;
import com.example.demo.enums.AssertActionEnum;
import com.example.demo.enums.AssertSourceEnum;
import com.example.demo.service.impl.RedisResultSenderServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.assertions.ResponseAssertion;
import org.apache.jmeter.assertions.gui.AssertionGui;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.config.gui.ArgumentsPanel;
import org.apache.jmeter.control.LoopController;
import org.apache.jmeter.control.gui.TestPlanGui;
import org.apache.jmeter.extractor.JSR223PostProcessor;
import org.apache.jmeter.modifiers.JSR223PreProcessor;
import org.apache.jmeter.protocol.http.control.Header;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.protocol.http.control.gui.HttpTestSampleGui;
import org.apache.jmeter.protocol.http.gui.HTTPArgumentsPanel;
import org.apache.jmeter.protocol.http.gui.HeaderPanel;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.protocol.java.sampler.JSR223Sampler;
import org.apache.jmeter.testbeans.gui.TestBeanGUI;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestPlan;
import org.apache.jmeter.testelement.property.StringProperty;
import org.apache.jmeter.timers.ConstantTimer;
import org.apache.jmeter.timers.gui.ConstantTimerGui;
import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jmeter.threads.gui.ThreadGroupGui;
import org.apache.jorphan.collections.HashTree;
import org.apache.jorphan.collections.ListedHashTree;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;

public class JMeterSimpleEngine extends BaseJMeterEngine {

    private final EnvironmentDTO environmentDTO;

    public JMeterSimpleEngine(EnvironmentDTO environmentDTO, ApiCaseDTO apiCaseDTO, ReportDTO reportDTO, ApplicationContext applicationContext) {
        this.environmentDTO = environmentDTO;
        super.apiCaseDTO = apiCaseDTO;
        super.reportDTO = reportDTO;
        super.applicationContext = applicationContext;
    }

    @Override
    public void assembleTestPlan() {
        // 获取压测结果收集器
        EngineSampleCollector engineSampleCollector = super.getEngineSampleCollector(applicationContext.getBean(RedisResultSenderServiceImpl.class));

        // 组装测试计划

        // 创建hashTree
        ListedHashTree testHashTree = new ListedHashTree();

        // 创建测试计划
        TestPlan testPlan = createTestPlan();

        // 创建线程组
        ThreadGroup threadGroup = createTheadGroup();

        // 创建循环控制器
        LoopController loopController = createLoopController(1);
        threadGroup.setSamplerController(loopController);

        // 组装到测试计划里面
        HashTree threadGroupHashTree = testHashTree.add(testPlan, threadGroup);

        // 请求头和参数等（环境级别）
        List<EnvironmentHttpInfoDTO> envHttpInfos = environmentDTO.getHttpInfos();
        for (EnvironmentHttpInfoDTO envHttpInfoDTO : envHttpInfos) {
            HeaderManager headerManager = createHeaderManager(envHttpInfoDTO.getHeaders());
            if (headerManager != null) {
                threadGroupHashTree.add(headerManager);
            }
        }

        // 按照步骤列表进行编排
        for (ApiCaseStepDTO apiCaseStepDTO : apiCaseDTO.getSteps()) {
            assembleComponent(threadGroupHashTree, apiCaseStepDTO);
        }

        // 结果收集器添加到线程组下面
        threadGroupHashTree.add(engineSampleCollector);

        super.setTestPlanHashTree(testHashTree);
    }

    private void assembleComponent(HashTree hashTree, ApiCaseStepDTO step) {
        if (step instanceof ApiCaseHttpStepDTO currentStep) {
            // HTTP请求
            // 创建采样器
            HTTPSamplerProxy httpSamplerProxy = createHttpSamplerProxy(currentStep);
            // 将http采样器添加到线程组下面，add() 返回该采样器的子树，后续的请求头/断言直接挂在上面
            HashTree httpSamplerHashTree = hashTree.add(httpSamplerProxy);
            List<ParamDTO> header = currentStep.getHeader();
            // 创建请求头列表
            HeaderManager headerManager = createHeaderManager(header);
            if (headerManager != null) {
                httpSamplerHashTree.add(headerManager);
            }

            List<ProcessorDTO> preProcessor = currentStep.getPreProcessor();
            // 创建前置处理器列表
            List<TestElement> preProcessorList = createPreProcessorList(preProcessor);
            if (preProcessorList != null) {
                httpSamplerHashTree.add(preProcessorList);
            }

            List<ProcessorDTO> postProcessor = currentStep.getPostProcessor();
            // 创建后置处理器列表
            List<TestElement> postProcessorList = createPostProcessorList(postProcessor);
            if (postProcessorList != null) {
                httpSamplerHashTree.add(postProcessorList);
            }

            List<AssertionDTO> assertion = currentStep.getAssertion();
            // 创建断言列表
            List<ResponseAssertion> responseAssertionList = createResponseAssertionList(assertion);
            if (responseAssertionList != null) {
                httpSamplerHashTree.add(responseAssertionList);
            }
        } else if (step instanceof ApiCaseScriptStepDTO currentStep) {
            // 脚本请求 JSR223 Sampler
            JSR223Sampler scriptSampler = createScriptSampler(currentStep);
            hashTree.add(scriptSampler);
        }
        // TODO 其他类型的组件
    }

    private List<TestElement> createPreProcessorList(List<ProcessorDTO> processors) {
        if (CollectionUtil.isEmpty(processors)) {
            return null;
        }
        List<TestElement> result = new ArrayList<>(processors.size());
        for (ProcessorDTO dto : processors) {
            switch (dto.getProcessorType()) {
                case SCRIPT -> {
                    ScriptProcessorDTO scriptDTO = dto.getScript();
                    JSR223PreProcessor pre = new JSR223PreProcessor();
                    pre.setProperty(TestElement.TEST_CLASS, JSR223PreProcessor.class.getName());
                    pre.setProperty(TestElement.GUI_CLASS, TestBeanGUI.class.getName());
                    pre.setEnabled(true);
                    pre.setName("PreProcessor-Script");
                    pre.setProperty("scriptLanguage", StringUtils.lowerCase(scriptDTO.getLanguage().name()));
                    pre.setProperty("script", scriptDTO.getContent() != null ? scriptDTO.getContent() : "");
                    result.add(pre);
                }
                case TIMER -> {
                    TimerProcessorDTO timerDTO = dto.getTimer();
                    ConstantTimer timer = new ConstantTimer();
                    timer.setProperty(TestElement.TEST_CLASS, ConstantTimer.class.getName());
                    timer.setProperty(TestElement.GUI_CLASS, ConstantTimerGui.class.getName());
                    timer.setEnabled(true);
                    timer.setName("PreProcessor-Timer");
                    timer.setDelay(String.valueOf(timerDTO.getWaitTime()));
                    result.add(timer);
                }
                default -> throw new RuntimeException("前置处理器不支持的类型: " + dto.getProcessorType());
            }
        }
        return result;
    }

    private List<TestElement> createPostProcessorList(List<ProcessorDTO> processors) {
        if (CollectionUtil.isEmpty(processors)) {
            return null;
        }
        List<TestElement> result = new ArrayList<>(processors.size());
        for (ProcessorDTO dto : processors) {
            switch (dto.getProcessorType()) {
                case SCRIPT -> {
                    ScriptProcessorDTO scriptDTO = dto.getScript();
                    JSR223PostProcessor post = new JSR223PostProcessor();
                    post.setProperty(TestElement.TEST_CLASS, JSR223PostProcessor.class.getName());
                    post.setProperty(TestElement.GUI_CLASS, TestBeanGUI.class.getName());
                    post.setEnabled(true);
                    post.setName("PostProcessor-Script");
                    post.setProperty("scriptLanguage", StringUtils.lowerCase(scriptDTO.getLanguage().name()));
                    post.setProperty("script", scriptDTO.getContent() != null ? scriptDTO.getContent() : "");
                    result.add(post);
                }
                default -> throw new RuntimeException("后置处理器不支持的类型: " + dto.getProcessorType());
            }
        }
        return result;
    }

    private JSR223Sampler createScriptSampler(ApiCaseScriptStepDTO step) {
        JSR223Sampler sampler = new JSR223Sampler();
        sampler.setProperty(TestElement.TEST_CLASS, JSR223Sampler.class.getName());
        sampler.setProperty(TestElement.GUI_CLASS, TestBeanGUI.class.getName());
        sampler.setEnabled(true);
        sampler.setName(step.getName());
        // 脚本语言映射
        String language = StringUtils.lowerCase(step.getLanguage().name());
        sampler.setProperty("scriptLanguage", language);
        sampler.setProperty("script", step.getContent() != null ? step.getContent() : "");
        return sampler;
    }

    private List<ResponseAssertion> createResponseAssertionList(List<AssertionDTO> assertion) {
        if (CollectionUtil.isEmpty(assertion)) {
            return null;
        }
        // 创建list存储ResponseAssertion
        List<ResponseAssertion> responseAssertionList = new ArrayList<>(assertion.size());
        for (AssertionDTO assertionDTO : assertion) {
            // 创建响应断言对象
            ResponseAssertion responseAssertion = new ResponseAssertion();
            responseAssertion.setProperty(TestElement.GUI_CLASS, AssertionGui.class.getName());
            responseAssertion.setProperty(TestElement.TEST_CLASS, ResponseAssertion.class.getName());
            responseAssertion.setName(assertionDTO.getName());
            responseAssertion.setAssumeSuccess(false);
            // 获取断言规则
            AssertActionEnum assertActionEnum = assertionDTO.getAction();
            // 匹配规则 包括，匹配
            switch (assertActionEnum) {
                case CONTAIN -> responseAssertion.setToContainsType();
                case EQUAL -> responseAssertion.setToEqualsType();
                default -> throw new RuntimeException("不支持的断言规则");
            }
            // 断言字段类型来源，响应头，响应体
            AssertSourceEnum fieldSourceEnum = assertionDTO.getSource();
            switch (fieldSourceEnum) {
                case RESPONSE_CODE -> responseAssertion.setTestFieldResponseCode();
                case RESPONSE_HEADER -> responseAssertion.setTestFieldResponseHeaders();
                case RESPONSE_DATA -> responseAssertion.setTestFieldResponseData();
                default -> throw new RuntimeException("不支持的断言字段来源");
            }
            // 增加用户期望的值
            responseAssertion.addTestString(assertionDTO.getValue());
            responseAssertionList.add(responseAssertion);
        }
        return responseAssertionList;
    }

    private HTTPSamplerProxy createHttpSamplerProxy(ApiCaseHttpStepDTO step) {
        // 设置HTTP请求的名称、协议、域名、端口、路径和方法
        HTTPSamplerProxy httpSampler = new HTTPSamplerProxy();
        httpSampler.setProperty(TestElement.TEST_CLASS, HTTPSamplerProxy.class.getName());
        httpSampler.setProperty(TestElement.GUI_CLASS, HttpTestSampleGui.class.getName());
        httpSampler.setEnabled(true);

        httpSampler.setName(step.getName());
        httpSampler.setProperty("HTTPSampler.path", step.getPath());
        httpSampler.setMethod(step.getMethod().name());

        httpSampler.setAutoRedirects(false);
        httpSampler.setUseKeepAlive(true);
        httpSampler.setFollowRedirects(true);
        httpSampler.setPostBodyRaw(true);

        // 处理请求参数
        if (HttpMethod.GET.equals(step.getMethod()) && CollectionUtil.isNotEmpty(step.getQuery())) {
            List<ParamDTO> keyValueList = step.getQuery();
            for (ParamDTO param : keyValueList) {
                httpSampler.addArgument(param.getName(), param.getValue());
            }
        } else {
            Arguments arguments = createArguments();
            httpSampler.setArguments(arguments);
        }
        return httpSampler;
    }

    private Arguments createArguments() {
        Arguments argumentManager = new Arguments();
        argumentManager.setProperty(TestElement.TEST_CLASS, Arguments.class.getName());
        argumentManager.setProperty(TestElement.GUI_CLASS, HTTPArgumentsPanel.class.getName());
//
//        //类型，当前没用，后续可以根据content-type进行判断
//        String bodyType = apiCaseDTO.getBodyType();
//
//        HTTPArgument httpArgument = new HTTPArgument();
//        httpArgument.setValue(apiCaseDTO.getBody());
//        httpArgument.setAlwaysEncoded(false);
//        argumentManager.addArgument(httpArgument);

        return argumentManager;
    }

    private HeaderManager createHeaderManager(List<ParamDTO> headers) {
        if (CollectionUtil.isEmpty(headers)) {
            return null;
        }
        HeaderManager headerManager = new HeaderManager();
        headerManager.setProperty(TestElement.TEST_CLASS, HeaderManager.class.getName());
        headerManager.setProperty(TestElement.GUI_CLASS, HeaderPanel.class.getName());
        headerManager.setEnabled(true);
        headerManager.setName("Headers");
        headers.forEach(keyValueConfig -> {
            headerManager.add(new Header(keyValueConfig.getName(), keyValueConfig.getValue()));
        });
        return headerManager;
    }

    /**
     * 创建线程组
     *
     * @return
     */
    private org.apache.jmeter.threads.ThreadGroup createTheadGroup() {
        // 将线程组配置转换为DTO对象
        org.apache.jmeter.threads.ThreadGroup threadGroup = new org.apache.jmeter.threads.ThreadGroup();
        threadGroup.setProperty(TestElement.TEST_CLASS, org.apache.jmeter.threads.ThreadGroup.class.getName());
        threadGroup.setProperty(TestElement.GUI_CLASS, ThreadGroupGui.class.getName());
        // 设置线程组名称、线程数、梯度上升等属性
        threadGroup.setName("ThreadGroup");
        threadGroup.setNumThreads(1);
        threadGroup.setRampUp(1);
        threadGroup.setIsSameUserOnNextIteration(true);
        threadGroup.setScheduler(false);
        threadGroup.setEnabled(true);
        threadGroup.setProperty(new StringProperty(ThreadGroup.ON_SAMPLE_ERROR, "continue"));
        threadGroup.setScheduler(false);
        return threadGroup;
    }

    private TestPlan createTestPlan() {
        TestPlan testPlan = new TestPlan(apiCaseDTO.getName());
        testPlan.setProperty(TestElement.TEST_CLASS, TestPlan.class.getName());
        testPlan.setProperty(TestElement.GUI_CLASS, TestPlanGui.class.getName());
        testPlan.setUserDefinedVariables((Arguments) new ArgumentsPanel().createTestElement());
        testPlan.setSerialized(true);
        testPlan.setTearDownOnShutdown(true);
        return testPlan;
    }

    /**
     * 创建循环控制器
     * @param loopCount
     * @return
     */
    private LoopController createLoopController(Integer loopCount) {
        // 创建一个 LoopController 对象
        LoopController loopController = new LoopController();
        // 设置测试类的属性为 LoopController 类的名称
        loopController.setProperty(TestElement.TEST_CLASS, LoopController.class.getName());
        // 设置图形用户界面类的属性为 LoopController 类的名称
        loopController.setProperty(TestElement.GUI_CLASS, LoopController.class.getName());
        // 设置循环次数
        loopController.setLoops(loopCount);
        // 设置为第一次循环
        loopController.setFirst(true);
        // 启用 LoopController
        loopController.setEnabled(true);
        // 初始化 LoopController
        loopController.initialize();
        // 返回 LoopController 对象
        return loopController;
    }

}
